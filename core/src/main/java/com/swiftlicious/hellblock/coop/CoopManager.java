package com.swiftlicious.hellblock.coop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.events.coop.CoopInviteRejectEvent;
import com.swiftlicious.hellblock.events.coop.CoopInviteSendEvent;
import com.swiftlicious.hellblock.events.coop.CoopJoinEvent;
import com.swiftlicious.hellblock.events.coop.CoopKickEvent;
import com.swiftlicious.hellblock.events.coop.CoopLeaveEvent;
import com.swiftlicious.hellblock.events.coop.CoopOwnershipTransferEvent;
import com.swiftlicious.hellblock.events.coop.CoopTrustAddEvent;
import com.swiftlicious.hellblock.events.coop.CoopTrustRemoveEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public class CoopManager implements Reloadable {

	protected final HellblockPlugin instance;

	// Cached island owners
	private volatile Set<UUID> cachedIslandOwners = Collections.emptySet();
	// Last time the cache was refreshed
	private volatile long lastOwnersRefresh = 0L;
	// Refresh interval (e.g., 5 minutes)
	private static final long OWNERS_CACHE_TTL = 5 * 60 * 1000L;

	private SchedulerTask enforceTask = null;

	public CoopManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		this.getCachedIslandOwners();
		this.startEnforcementTask();
	}

	@Override
	public void unload() {
		// Clear cache on reload
		cachedIslandOwners.clear();
		if (enforceTask != null && !enforceTask.isCancelled()) {
			enforceTask.cancel();
			enforceTask = null;
		}
		lastOwnersRefresh = 0L;
	}

	public void startEnforcementTask() {
		enforceTask = instance.getScheduler().sync().runRepeating(() -> {
			// 1. Enforce bans for online players
			instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer).filter(Objects::nonNull)
					.forEach(this::enforceIslandBanIfNeeded);

			// 2. Enforce visitor kicks on locked islands
			instance.getCoopManager().getCachedIslandOwners().thenAccept(ownerUUIDs -> {
				for (UUID ownerUUID : ownerUUIDs) {
					instance.getCoopManager().kickVisitorsIfLocked(ownerUUID).exceptionally(ex -> {
						instance.getPluginLogger().warn("Failed to kick visitors for locked island " + ownerUUID, ex);
						return null;
					});
				}
			});
		}, 20 * 10L, 20 * 10L, LocationUtils.getAnyLocationInstance()); // 10s delay and repeat
	}

	public void sendInvite(@NotNull UserData onlineUser, @NotNull UserData playerToInvite) {
		final Player owner = requireOnline(onlineUser)
				.orElseThrow(() -> new IllegalStateException("Owner must be online to send an invite."));

		final Sender ownerAudience = instance.getSenderFactory().wrap(owner);
		final UUID targetId = playerToInvite.getUUID();

		if (onlineUser.getHellblockData().isAbandoned()) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
			return;
		}

		if (owner.getUniqueId().equals(targetId)) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_SELF);
			return;
		}

		if (onlineUser.getHellblockData().getParty().contains(targetId)) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_IN_PARTY,
					Component.text(playerToInvite.getName()));
			return;
		}

		if (onlineUser.getHellblockData().getBanned().contains(targetId)) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_BANNED_FROM_INVITE);
			return;
		}

		if (playerToInvite.getHellblockData().hasInvite(owner.getUniqueId())) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXISTS);
			return;
		}

		final CoopInviteSendEvent inviteEvent = new CoopInviteSendEvent(onlineUser, playerToInvite);
		Bukkit.getPluginManager().callEvent(inviteEvent);
		if (inviteEvent.isCancelled()) {
			return;
		}

		playerToInvite.getHellblockData().sendInvitation(owner.getUniqueId());

		send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_SENT, Component.text(playerToInvite.getName()));
		// Send live or mailbox message
		if (playerToInvite.isOnline()) {
			final Player targetPlayer = playerToInvite.getPlayer();
			final Sender playerAudience = instance.getSenderFactory().wrap(targetPlayer);
			send(playerAudience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_RECEIVED, Component.text(owner.getName()),
					Component.text(instance.getFormattedCooldown(
							playerToInvite.getHellblockData().getInvitations().get(owner.getUniqueId()))));
		} else {
			// Offline player — queue mailbox notification
			instance.getMailboxManager().queue(playerToInvite.getUUID(),
					new MailboxEntry("message.hellblock.coop.invite.offline", List.of(Component.text(owner.getName())),
							Set.of(MailboxFlag.NOTIFY_PARTY)));
		}
	}

	public void rejectInvite(@NotNull UUID ownerID, @NotNull UserData rejectingPlayer) {
		final Player player = requireOnline(rejectingPlayer)
				.orElseThrow(() -> new IllegalStateException("Rejecting player must be online."));

		final Sender audience = instance.getSenderFactory().wrap(player);

		if (!rejectingPlayer.getHellblockData().hasInvite(ownerID)) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
			return;
		}

		final CoopInviteRejectEvent rejectEvent = new CoopInviteRejectEvent(ownerID, rejectingPlayer);
		Bukkit.getPluginManager().callEvent(rejectEvent);

		rejectingPlayer.getHellblockData().removeInvitation(ownerID);

		Optional.ofNullable(Bukkit.getPlayer(ownerID)).map(ownerPlayer -> instance.getSenderFactory().wrap(ownerPlayer))
				.ifPresent(ownerAudience -> send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_REJECTED_TO_OWNER,
						Component.text(player.getName())));

		final OfflinePlayer owner = resolvePlayer(ownerID);
		final String username = owner.hasPlayedBefore() && owner.getName() != null ? owner.getName() : "???";
		send(audience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_REJECTED, Component.text(username));
	}

	public void listInvitations(@NotNull UserData onlineUser, int page) {
		final Player player = requireOnline(onlineUser)
				.orElseThrow(() -> new IllegalStateException("Player must be online to view invites."));

		final Sender audience = instance.getSenderFactory().wrap(player);

		final Map<UUID, Long> rawInvites = onlineUser.getHellblockData().getInvitations();

		// Order by expiry soonest to latest
		// Filter only valid (non-expired) invitations
		List<Map.Entry<UUID, Long>> invites = rawInvites.entrySet().stream()
				.filter(entry -> entry.getValue() > System.currentTimeMillis())
				.sorted((a, b) -> Long.compare(a.getValue(), b.getValue())).toList();

		if (invites.isEmpty()) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES);
			return;
		}

		int totalPages = (int) Math.ceil(invites.size() / 10.0);
		if (page < 1 || page > totalPages) {
			send(audience, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT, Component.text(page),
					Component.text(totalPages));
			return;
		}

		send(audience, MessageConstants.MSG_HELLBLOCK_COOP_INVITATION_HEADER.arguments(Component.text(page),
				Component.text(totalPages)));

		int start = (page - 1) * 10;
		int end = Math.min(start + 10, invites.size());

		for (int i = start; i < end; i++) {
			Map.Entry<UUID, Long> entry = invites.get(i);
			UUID ownerUUID = entry.getKey();
			long expiration = entry.getValue();

			final OfflinePlayer owner = resolvePlayer(ownerUUID);
			final String username = owner.hasPlayedBefore() && owner.getName() != null ? owner.getName() : "???";

			String remaining = instance.getFormattedCooldown((expiration - System.currentTimeMillis()) / 1000);

			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_INVITATION_LIST, Component.text(username),
					Component.text(remaining));
		}
	}

	public void addMemberToHellblock(@NotNull UUID ownerID, @NotNull UserData playerToAdd) {
		final Player player = requireOnline(playerToAdd)
				.orElseThrow(() -> new IllegalStateException("Player must be online to join a Hellblock."));

		final Sender audience = instance.getSenderFactory().wrap(player);

		if (playerToAdd.getHellblockData().hasHellblock()) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS);
			return;
		}

		if (!playerToAdd.getHellblockData().hasInvite(ownerID)) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
			return;
		}

		instance.getStorageManager().getOfflineUserData(ownerID, instance.getConfigManager().lockData())
				.thenAccept(optionalOwner -> {
					if (optionalOwner.isEmpty()) {
						return;
					}
					final UserData ownerData = optionalOwner.get();

					if (ownerData.getHellblockData().isAbandoned()) {
						send(audience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_ABANDONED);
						return;
					}

					final Set<UUID> party = ownerData.getHellblockData().getParty();
					if (party.size() >= getMaxPartySize(ownerData)) {
						send(audience, MessageConstants.MSG_HELLBLOCK_COOP_PARTY_FULL);
						return;
					}

					if (party.contains(player.getUniqueId())) {
						send(audience, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_JOINED_PARTY);
						return;
					}

					final World bukkitWorld = getWorldFor(ownerData)
							.orElseThrow(() -> new IllegalStateException("Hellblock world not found."));

					final CoopJoinEvent joinEvent = new CoopJoinEvent(ownerID, playerToAdd);
					Bukkit.getPluginManager().callEvent(joinEvent);

					instance.getProtectionManager().getIslandProtection().addMemberToHellblockBounds(bukkitWorld,
							ownerData.getUUID(), player.getUniqueId());

					playerToAdd.getHellblockData().setHasHellblock(true);
					playerToAdd.getHellblockData().setOwnerUUID(ownerID);
					ownerData.getHellblockData().addToParty(player.getUniqueId());

					if (playerToAdd.getHellblockData().getTrusted().contains(ownerID)) {
						playerToAdd.getHellblockData().removeTrustPermission(ownerID);
					}

					makeHomeLocationSafe(ownerData, playerToAdd);

					if (ownerData.isOnline()) {
						final Sender ownerAudience = instance.getSenderFactory().wrap(Bukkit.getPlayer(ownerID));
						send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_ADDED_TO_PARTY,
								Component.text(player.getName()));
					} else {
						instance.getMailboxManager().queue(ownerData.getUUID(),
								new MailboxEntry("message.hellblock.coop.added.offline",
										List.of(Component.text(player.getName())), Set.of(MailboxFlag.NOTIFY_OWNER)));
					}

					send(audience, MessageConstants.MSG_HELLBLOCK_COOP_JOINED_PARTY,
							Component.text(ownerData.getName()));
				});
	}

	public void removeMemberFromHellblock(@NotNull UserData onlineUser, @NotNull String input, @NotNull UUID memberId) {
		final Player owner = requireOnline(onlineUser)
				.orElseThrow(() -> new IllegalStateException("Owner must be online to remove a member."));

		final Sender ownerAudience = instance.getSenderFactory().wrap(owner);

		if (!onlineUser.getHellblockData().hasHellblock()) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
			return;
		}

		instance.getStorageManager().getOfflineUserData(memberId, instance.getConfigManager().lockData())
				.thenAccept(optionalMember -> {
					if (optionalMember.isEmpty()) {
						return;
					}
					final UserData member = optionalMember.get();

					if (Objects.equals(owner.getUniqueId(), member.getHellblockData().getOwnerUUID())) {
						send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NO_KICK_SELF);
						return;
					}

					if (!member.getHellblockData().hasHellblock()
							|| !onlineUser.getHellblockData().getParty().contains(memberId)) {
						send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY,
								Component.text(input));
						return;
					}

					final World bukkitWorld = getWorldFor(member)
							.orElseThrow(() -> new IllegalStateException("Hellblock world not found."));

					final CoopKickEvent kickEvent = new CoopKickEvent(onlineUser, member);
					Bukkit.getPluginManager().callEvent(kickEvent);
					if (kickEvent.isCancelled()) {
						return;
					}

					instance.getProtectionManager().getIslandProtection().removeMemberFromHellblockBounds(bukkitWorld,
							owner.getUniqueId(), memberId);

					member.getHellblockData().setHasHellblock(false);
					member.getHellblockData().setOwnerUUID(null);
					onlineUser.getHellblockData().kickFromParty(memberId);

					send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_PARTY_KICKED, Component.text(input));

					if (member.isOnline()) {
						final Player kickedPlayer = member.getPlayer();
						instance.getHellblockHandler().teleportToSpawn(kickedPlayer, true);

						final Sender kickedAudience = instance.getSenderFactory().wrap(kickedPlayer);
						send(kickedAudience, MessageConstants.MSG_HELLBLOCK_COOP_REMOVED_FROM_PARTY,
								Component.text(owner.getName()));
					} else {
						instance.getMailboxManager().queue(member.getUUID(),
								new MailboxEntry("message.hellblock.coop.kicked.offline",
										List.of(Component.text(owner.getName())),
										Set.of(MailboxFlag.NOTIFY_PARTY, MailboxFlag.UNSAFE_LOCATION)));
					}
				});
	}

	public void leaveHellblockParty(@NotNull UserData leavingPlayer) {
		final Player player = requireOnline(leavingPlayer)
				.orElseThrow(() -> new IllegalStateException("Player must be online to leave party."));

		final Sender audience = instance.getSenderFactory().wrap(player);

		if (!leavingPlayer.getHellblockData().hasHellblock()) {
			send(audience, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
			return;
		}

		final UUID ownerId = leavingPlayer.getHellblockData().getOwnerUUID();
		if (ownerId == null) {
			throw new IllegalStateException("Hellblock owner UUID missing.");
		}

		if (ownerId.equals(player.getUniqueId())) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_OWNER_NO_LEAVE);
			return;
		}

		instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
				.thenAccept(optionalOwner -> {
					if (optionalOwner.isEmpty()) {
						return;
					}
					final UserData owner = optionalOwner.get();

					if (!owner.getHellblockData().getParty().contains(player.getUniqueId())) {
						send(audience, MessageConstants.MSG_HELLBLOCK_COOP_NOT_IN_PARTY);
						return;
					}

					final World bukkitWorld = getWorldFor(owner)
							.orElseThrow(() -> new IllegalStateException("Hellblock world not found."));

					final CoopLeaveEvent leaveEvent = new CoopLeaveEvent(leavingPlayer, ownerId);
					Bukkit.getPluginManager().callEvent(leaveEvent);

					instance.getProtectionManager().getIslandProtection().removeMemberFromHellblockBounds(bukkitWorld,
							ownerId, player.getUniqueId());

					leavingPlayer.getHellblockData().setHasHellblock(false);
					leavingPlayer.getHellblockData().setOwnerUUID(null);
					owner.getHellblockData().kickFromParty(player.getUniqueId());

					instance.getHellblockHandler().teleportToSpawn(player, true);
					send(audience, MessageConstants.MSG_HELLBLOCK_COOP_PARTY_LEFT, Component.text(owner.getName()));

					if (owner.isOnline()) {
						final Sender ownerAudience = instance.getSenderFactory().wrap(Bukkit.getPlayer(ownerId));
						send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_LEFT_PARTY,
								Component.text(player.getName()));
					} else {
						instance.getMailboxManager().queue(owner.getUUID(),
								new MailboxEntry("message.hellblock.coop.member.left.offline",
										List.of(Component.text(player.getName())), Set.of(MailboxFlag.NOTIFY_OWNER)));
					}
				});
	}

	public void transferOwnershipOfHellblock(@NotNull UserData currentOwnerData, @NotNull UserData newOwnerData,
			boolean forcedByAdmin) {
		final Player currentOwnerPlayer = currentOwnerData.getPlayer();
		final Player newOwnerPlayer = newOwnerData.getPlayer();

		final Sender currentOwnerAudience = currentOwnerPlayer != null
				? instance.getSenderFactory().wrap(currentOwnerPlayer)
				: null; // offline safe
		final Sender newOwnerAudience = newOwnerPlayer != null ? instance.getSenderFactory().wrap(newOwnerPlayer)
				: null;

		// --- Global config restriction: overrides everything ---
		if (!instance.getConfigManager().transferIslands()) {
			if (currentOwnerAudience != null) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED);
			}
			if (newOwnerAudience != null) {
				send(newOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED);
			}
			return;
		}

		// --- Validation (skipped if forcedByAdmin = true) ---
		if (!forcedByAdmin) {
			if (currentOwnerPlayer == null) {
				throw new IllegalStateException("Owner must be online to transfer ownership.");
			}

			if (!currentOwnerData.getHellblockData().hasHellblock()) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			if (currentOwnerData.getHellblockData().isAbandoned()) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
				return;
			}

			final UUID currentOwnerId = currentOwnerData.getHellblockData().getOwnerUUID();
			if (currentOwnerId == null || !currentOwnerId.equals(currentOwnerPlayer.getUniqueId())) {
				send(currentOwnerAudience, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
				return;
			}

			if (currentOwnerPlayer.getUniqueId().equals(newOwnerPlayer.getUniqueId())) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NO_TRANSFER_SELF);
				return;
			}

			if (!newOwnerData.getHellblockData().hasHellblock()
					|| !currentOwnerData.getHellblockData().getParty().contains(newOwnerPlayer.getUniqueId())) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY,
						Component.text(newOwnerPlayer.getName()));
				return;
			}

			if (currentOwnerId.equals(newOwnerPlayer.getUniqueId())) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_OWNER_OF_ISLAND,
						Component.text(newOwnerPlayer.getName()));
				return;
			}
		}

		final CoopOwnershipTransferEvent ownerTransferEvent = new CoopOwnershipTransferEvent(currentOwnerData,
				newOwnerData, forcedByAdmin);
		Bukkit.getPluginManager().callEvent(ownerTransferEvent);

		// --- Ownership transfer ---
		newOwnerData.getHellblockData().transferHellblockData(currentOwnerData);
		newOwnerData.getHellblockData().setTransferCooldown(forcedByAdmin ? 0L : TimeUnit.SECONDS.toDays(86400));
		newOwnerData.getHellblockData().setOwnerUUID(newOwnerData.getUUID());
		newOwnerData.getHellblockData().kickFromParty(newOwnerData.getUUID());

		newOwnerData.getHellblockData().getParty().forEach(partyId -> instance.getStorageManager()
				.getOfflineUserData(partyId, instance.getConfigManager().lockData()).thenAccept(optParty -> optParty
						.ifPresent(u -> u.getHellblockData().setOwnerUUID(newOwnerData.getUUID()))));

		newOwnerData.getHellblockData().addToParty(currentOwnerData.getUUID());

		final World bukkitWorld = getWorldFor(newOwnerData)
				.orElseThrow(() -> new IllegalStateException("Hellblock world not found."));
		instance.getProtectionManager().getIslandProtection().reprotectHellblock(bukkitWorld, currentOwnerData,
				newOwnerData);

		currentOwnerData.getHellblockData().resetHellblockData();
		currentOwnerData.getHellblockData().setHasHellblock(true);
		currentOwnerData.getHellblockData().setID(0);
		currentOwnerData.getHellblockData().setTrusted(new HashSet<>());
		currentOwnerData.getHellblockData().setResetCooldown(0L);
		currentOwnerData.getHellblockData().setOwnerUUID(newOwnerData.getUUID());

		// --- Messaging ---
		if (forcedByAdmin) {
			if (currentOwnerAudience != null) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_LOST,
						Component.text(newOwnerData.getName()));
			}
			if (newOwnerAudience != null) {
				send(newOwnerAudience, MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_GAINED,
						Component.text(currentOwnerData.getName()));
			}
		} else {
			send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NEW_OWNER_SET,
					Component.text(newOwnerData.getName()));
			if (newOwnerPlayer != null) {
				send(newOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_OWNER_TRANSFER_SUCCESS,
						Component.text(currentOwnerData.getName()));
			} else {
				instance.getMailboxManager().queue(newOwnerData.getUUID(),
						new MailboxEntry("message.hellblock.coop.owner.transfer.offline",
								List.of(Component.text(currentOwnerPlayer.getName())),
								Set.of(MailboxFlag.NOTIFY_OWNER)));
			}
		}

		Set<UUID> party = newOwnerData.getHellblockData().getParty();
		party.stream().map(Bukkit::getPlayer).forEach(member -> {
			if (member != null && member.isOnline()) {
				Sender sender = instance.getSenderFactory().wrap(member);
				sender.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_OWNER_TRANSFER_NOTIFY_PARTY
								.arguments(AdventureHelper.miniMessage(newOwnerData.getName())).build()));
			} else {
				instance.getMailboxManager().queue(newOwnerData.getUUID(),
						new MailboxEntry("message.hellblock.coop.owner.transfer.party",
								List.of(Component.text(newOwnerData.getName())), Set.of(MailboxFlag.NOTIFY_PARTY)));
			}
		});
	}

	public CompletableFuture<Set<UUID>> getVisitors(@NotNull UUID ownerId) {
		return instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
				.thenCombine(getAllCoopMembers(ownerId), (ownerDataOpt, coopMembers) -> {
					if (ownerDataOpt.isEmpty())
						return Set.of();

					final HellblockData data = ownerDataOpt.get().getHellblockData();
					final BoundingBox bounds = data.getBoundingBox();
					final World ownerWorld = data.getHellblockLocation().getWorld();

					if (bounds == null || ownerWorld == null) {
						return Set.of();
					}

					return instance.getStorageManager().getOnlineUsers().stream().filter(UserData::isOnline)
							.map(UserData::getPlayer).filter(Objects::nonNull)
							.filter(p -> p.getWorld().equals(ownerWorld)) // World match
							.filter(p -> bounds.contains(p.getBoundingBox())) // Inside island
							.filter(p -> !coopMembers.contains(p.getUniqueId())) // Not a coop member
							.map(Player::getUniqueId).collect(Collectors.toSet());
				});
	}

	public CompletableFuture<@Nullable UUID> getHellblockOwnerOfVisitingIsland(@NotNull Player player) {
		if (player.getLocation() == null) {
			return CompletableFuture.completedFuture(null);
		}

		final Set<UUID> allUsers = instance.getStorageManager().getDataSource().getUniqueUsers();
		final List<CompletableFuture<@Nullable UUID>> futures = new ArrayList<>();

		for (UUID uuid : allUsers) {
			final CompletableFuture<@Nullable UUID> owner = instance.getStorageManager()
					.getOfflineUserData(uuid, instance.getConfigManager().lockData()).thenApply(result -> {
						if (result.isEmpty()) {
							return null;
						}
						final UserData offlineUser = result.get();
						final BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
						if (bounds != null && bounds.contains(player.getBoundingBox())) {
							return offlineUser.getHellblockData().getOwnerUUID();
						}
						return null;
					});
			futures.add(owner);
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(
				v -> futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).findFirst().orElse(null));
	}

	public CompletableFuture<@Nullable UUID> getHellblockOwnerOfBlock(@NotNull Block block) {
		final World world = block.getWorld();

		// not a hellblock world
		if (!instance.getHellblockHandler().isInCorrectWorld(world)) {
			return CompletableFuture.completedFuture(null);
		}

		// check all island owners
		return getCachedIslandOwners().thenCompose(owners -> {
			if (owners == null || owners.isEmpty()) {
				return CompletableFuture.completedFuture(null);
			}

			// chain futures for each owner
			final List<CompletableFuture<@Nullable UUID>> futures = new ArrayList<>();
			owners.forEach(ownerUUID -> {
				final CompletableFuture<@Nullable UUID> check = instance.getProtectionManager()
						.getHellblockBlocks(world, ownerUUID)
						.thenApply(blocks -> blocks.contains(block) ? ownerUUID : null);
				futures.add(check);
			});

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> futures.stream()
					.map(CompletableFuture::join).filter(Objects::nonNull).findFirst().orElse(null));
		});
	}

	/**
	 * Returns a cached set of all island owners. Refreshes from storage if the
	 * cache has expired.
	 */
	public CompletableFuture<Collection<UUID>> getCachedIslandOwners() {
		long now = System.currentTimeMillis();

		// If cache is still valid, return it immediately
		if (now - lastOwnersRefresh < OWNERS_CACHE_TTL && !cachedIslandOwners.isEmpty()) {
			return CompletableFuture.completedFuture(cachedIslandOwners);
		}

		// Otherwise refresh from database
		return getAllIslandOwners().thenApply(owners -> {
			cachedIslandOwners = new HashSet<>(owners);
			lastOwnersRefresh = now;
			return cachedIslandOwners;
		});
	}

	public CompletableFuture<List<UserData>> getCachedIslandOwnerData() {
		return getCachedIslandOwners().thenCompose(ownerUUIDs -> {
			List<CompletableFuture<Optional<UserData>>> futures = ownerUUIDs.stream().map(uuid -> instance
					.getStorageManager().getOfflineUserData(uuid, instance.getConfigManager().lockData())).toList();

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> futures.stream()
					.map(CompletableFuture::join).filter(Optional::isPresent).map(Optional::get).toList());
		});
	}

	public CompletableFuture<Collection<UUID>> getAllIslandOwners() {
		final Set<UUID> allUsers = instance.getStorageManager().getDataSource().getUniqueUsers();
		if (allUsers.isEmpty()) {
			return CompletableFuture.completedFuture(Collections.emptySet());
		}

		final List<CompletableFuture<UUID>> futures = new ArrayList<>();

		for (UUID uuid : allUsers) {
			final CompletableFuture<UUID> ownerFuture = instance.getStorageManager()
					.getOfflineUserData(uuid, instance.getConfigManager().lockData()).thenApply(result -> {
						if (result.isEmpty()) {
							return null;
						}

						final UserData offlineUser = result.get();
						final UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();

						// Only return UUIDs that are true island owners
						if (ownerUUID != null && ownerUUID.equals(uuid)) {
							return ownerUUID;
						}
						return null;
					});

			futures.add(ownerFuture);
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> futures.stream()
				.map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toSet()));
	}

	public CompletableFuture<Collection<UUID>> getAllCoopMembers(@NotNull UUID ownerId) {
		return instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
				.thenApply(optionalUserData -> {
					if (optionalUserData.isEmpty()) {
						return Collections.emptySet();
					}

					final UserData userData = optionalUserData.get();
					final HellblockData hb = userData.getHellblockData();

					// Always include owner + members
					final Set<UUID> members = new HashSet<>(hb.getPartyPlusOwner());
					members.add(ownerId);

					return members;
				});
	}

	public CompletableFuture<Optional<UUID>> getIslandOwner(@NotNull UUID playerId) {
		return instance.getStorageManager().getOfflineUserData(playerId, instance.getConfigManager().lockData())
				.thenApply(optionalUserData -> {
					if (optionalUserData.isEmpty()) {
						return Optional.empty();
					}
					final UserData userData = optionalUserData.get();
					final HellblockData hb = userData.getHellblockData();
					return Optional.ofNullable(hb.getOwnerUUID());
				});
	}

	public CompletableFuture<Boolean> checkIfVisitorsAreWelcome(@NotNull Player player, @NotNull UUID id) {
		return instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenApply(result -> {
					if (result.isEmpty()) {
						return false;
					}
					final UserData offlineUser = result.get();
					final HellblockData data = offlineUser.getHellblockData();

					return !data.isLocked()
							|| (data.getOwnerUUID() != null
									&& Objects.equals(data.getOwnerUUID(), player.getUniqueId()))
							|| (data.getParty().contains(player.getUniqueId()))
							|| (data.getTrusted().contains(player.getUniqueId()))
							|| player.hasPermission("hellblock.bypass.lock") || player.hasPermission("hellblock.admin")
							|| player.isOp();
				});
	}

	public CompletableFuture<Void> kickVisitorsIfLocked(@NotNull UUID id) {
		return instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenCompose(result -> {
					if (result.isEmpty()) {
						return CompletableFuture.completedFuture(null);
					}

					final UserData offlineUser = result.get();
					if (offlineUser.getHellblockData().getOwnerUUID() != null
							&& !offlineUser.getHellblockData().isOwner(offlineUser.getHellblockData().getOwnerUUID())) {
						return CompletableFuture.completedFuture(null);
					}
					if (!offlineUser.getHellblockData().isLocked()) {
						return CompletableFuture.completedFuture(null);
					}

					return getVisitors(offlineUser.getUUID()).thenCompose(visitors -> {
						final List<CompletableFuture<Void>> tasks = new ArrayList<>();

						for (UUID visitor : visitors) {
							final Optional<UserData> opt = instance.getStorageManager().getOnlineUser(visitor);
							if (opt.isEmpty() || !opt.get().isOnline() || opt.get().getPlayer() == null) {
								continue;
							}
							final Player player = opt.get().getPlayer();
							final CompletableFuture<Void> task = checkIfVisitorsAreWelcome(player,
									offlineUser.getUUID()).thenAccept(status -> {
										if (!status) {
											instance.debug("Kicked visitor %s from locked island of %s"
													.formatted(player.getName(), offlineUser.getName()));
											handleVisitorKick(opt.get(), player);
										}
									});
							tasks.add(task);
						}
						return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
					});
				});
	}

	private void handleVisitorKick(UserData onlineVisitor, Player player) {
		if (onlineVisitor.getHellblockData().hasHellblock()) {
			final UUID owner = onlineVisitor.getHellblockData().getOwnerUUID();
			if (owner == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			instance.getStorageManager().getOfflineUserData(owner, instance.getConfigManager().lockData())
					.thenAccept(ownerData -> ownerData
							.ifPresent(visitorOwner -> makeHomeLocationSafe(visitorOwner, onlineVisitor)));
		} else {
			instance.getHellblockHandler().teleportToSpawn(player, true);
		}
		instance.getSenderFactory().wrap(player).sendMessage(
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LOCKED_ENTRY.build()));
	}

	public CompletableFuture<Boolean> addTrustAccess(@NotNull UserData onlineUser, @NotNull String input,
			@NotNull UUID id) {
		if (!onlineUser.isOnline() || onlineUser.getPlayer() == null) {
			throw new NullPointerException("Player object returned null, please report this to the developer.");
		}

		final Optional<HellblockWorld<?>> world = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(onlineUser.getHellblockData().getID()));

		if (world.isEmpty()) {
			throw new NullPointerException(
					"World returned null, please try to regenerate the world before reporting this issue.");
		}

		final World bukkitWorld = world.get().bukkitWorld();

		return instance.getProtectionManager().getIslandProtection()
				.getMembersOfHellblockBounds(bukkitWorld, onlineUser.getUUID()).thenApply(trusted -> {
					if (trusted.contains(id)) {
						final CoopTrustAddEvent trustAddEvent = new CoopTrustAddEvent(onlineUser, id);
						Bukkit.getPluginManager().callEvent(trustAddEvent);
						instance.getSenderFactory().wrap(onlineUser.getPlayer())
								.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_TRUSTED
												.arguments(AdventureHelper.miniMessage(input)).build()));
						return false;
					}
					return trusted.add(id);
				});
	}

	public CompletableFuture<Boolean> removeTrustAccess(@NotNull UserData onlineUser, @NotNull String input,
			@NotNull UUID id) {
		if (!onlineUser.isOnline() || onlineUser.getPlayer() == null) {
			throw new NullPointerException("Player object returned null, please report this to the developer.");
		}

		final Optional<HellblockWorld<?>> world = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(onlineUser.getHellblockData().getID()));

		if (world.isEmpty()) {
			throw new NullPointerException(
					"World returned null, please try to regenerate the world before reporting this issue.");
		}

		final World bukkitWorld = world.get().bukkitWorld();

		return instance.getProtectionManager().getIslandProtection()
				.getMembersOfHellblockBounds(bukkitWorld, onlineUser.getUUID()).thenApply(trusted -> {
					if (!trusted.contains(id)) {
						final CoopTrustRemoveEvent trustRemoveEvent = new CoopTrustRemoveEvent(onlineUser, id);
						Bukkit.getPluginManager().callEvent(trustRemoveEvent);
						instance.getSenderFactory().wrap(onlineUser.getPlayer()).sendMessage(
								instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_TRUSTED
										.arguments(AdventureHelper.miniMessage(input)).build()));
						return false;
					}
					return trusted.remove(id);
				});
	}

	public CompletableFuture<Boolean> trackBannedPlayer(@NotNull UUID bannedFromUUID, @NotNull UUID playerUUID) {
		return instance.getStorageManager().getOfflineUserData(bannedFromUUID, instance.getConfigManager().lockData())
				.thenApply(result -> {
					if (result.isEmpty()) {
						return false;
					}
					final UserData offlineUser = result.get();
					final BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
					final Set<UUID> banned = offlineUser.getHellblockData().getBanned();

					final Player player = Bukkit.getPlayer(playerUUID);
					if (bounds == null || player == null) {
						return false;
					}

					return bounds.contains(player.getBoundingBox()) && banned.contains(playerUUID);
				});
	}

	public void enforceIslandBanIfNeeded(Player player) {
		UUID playerId = player.getUniqueId();
		World world = player.getWorld();

		BoundingBox playerBox = player.getBoundingBox();

		getCachedIslandOwners().thenAccept(ownerIds -> {
			for (UUID ownerId : ownerIds) {
				// Load island owner data (even if offline)
				instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
						.thenAccept(optOwner -> {
							if (optOwner.isEmpty())
								return;

							UserData islandOwner = optOwner.get();
							HellblockData data = islandOwner.getHellblockData();
							if (data == null || data.getBoundingBox() == null || data.getHellblockLocation() == null)
								return;

							if (!world.getName().equals(data.getHellblockLocation().getWorld().getName()))
								return;
							if (!data.getBoundingBox().contains(playerBox))
								return;
							if (!data.getBanned().contains(playerId))
								return;

							instance.debug("Banned player " + player.getName() + " was in island of "
									+ islandOwner.getName() + " — teleporting out.");

							// Get banned player's UserData
							Optional<UserData> userDataOpt = instance.getStorageManager().getOnlineUser(playerId);
							if (userDataOpt.isEmpty())
								return;

							UserData bannedUser = userDataOpt.get();
							HellblockData bannedData = bannedUser.getHellblockData();

							if (bannedData == null || bannedData.getHomeLocation() == null) {
								// No valid home location — send to spawn
								instance.getScheduler().executeSync(
										() -> instance.getHellblockHandler().teleportToSpawn(player, true));
								return;
							}

							UUID bannedOwnerUUID = bannedData.getOwnerUUID();

							if (bannedOwnerUUID == null) {
								instance.getPluginLogger()
										.warn("Owner UUID was null for banned player " + player.getName());
								instance.getScheduler().executeSync(
										() -> instance.getHellblockHandler().teleportToSpawn(player, true));
								return;
							}

							// Load the owner's UserData (used in makeHomeLocationSafe)
							instance.getStorageManager()
									.getOfflineUserData(bannedOwnerUUID, instance.getConfigManager().lockData())
									.thenAccept(ownerOpt -> {
										if (ownerOpt.isEmpty()) {
											instance.getPluginLogger().warn(
													"Could not load owner data for banned player " + player.getName());
											instance.getScheduler().executeSync(
													() -> instance.getHellblockHandler().teleportToSpawn(player, true));
											return;
										}

										UserData bannedOwnerUser = ownerOpt.get();

										// Ensure the home location is safe (and teleport)
										// Ensure world is loaded
										instance.getWorldManager().ensureHellblockWorldLoaded(bannedData.getID())
												.thenCompose(loadedWorld -> instance.getCoopManager()
														.makeHomeLocationSafe(bannedOwnerUser, bannedUser)
														.thenRun(() -> instance.getScheduler()
																.executeSync(() -> instance.getSenderFactory()
																		.wrap(player)
																		.sendMessage(instance.getTranslationManager()
																				.render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY
																						.build())))))
												.exceptionally(ex -> {
													instance.getPluginLogger()
															.warn("Failed to load world or make safe for banned player "
																	+ player.getName(), ex);
													instance.getScheduler().executeSync(() -> instance
															.getHellblockHandler().teleportToSpawn(player, true));
													return null;
												});
									});
						});
				// Only check one island (player can't be in multiple)
				break;
			}
		});
	}

	public CompletableFuture<Boolean> makeHomeLocationSafe(@NotNull UserData offlineUser,
			@NotNull UserData onlineUser) {
		if (!onlineUser.isOnline() || onlineUser.getPlayer() == null) {
			throw new NullPointerException("Player object returned null, please report this to the developer.");
		}

		return LocationUtils.isSafeLocationAsync(offlineUser.getHellblockData().getHomeLocation()).thenCompose(safe -> {
			if (safe) {
				return CompletableFuture.completedFuture(null);
			}

			instance.getSenderFactory().wrap(onlineUser.getPlayer()).sendMessage(instance.getTranslationManager()
					.render(MessageConstants.MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK.build()));

			return instance.getHellblockHandler().locateBedrock(offlineUser.getUUID()).thenCompose(bedrock -> {
				offlineUser.getHellblockData().setHomeLocation(bedrock);
				return ChunkUtils.teleportAsync(onlineUser.getPlayer(), bedrock, TeleportCause.PLUGIN);
			});
		});
	}

	public int getMaxPartySize(@NotNull UserData offlineUser) {
		// Must have a hellblock
		if (!offlineUser.getHellblockData().hasHellblock()) {
			return instance.getUpgradeManager().getDefaultValue(IslandUpgradeType.PARTY_SIZE).intValue();
		}

		final UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();

		// Only the island owner determines max party size
		if (ownerUUID == null || !ownerUUID.equals(offlineUser.getUUID())) {
			return instance.getUpgradeManager().getDefaultValue(IslandUpgradeType.PARTY_SIZE).intValue();
		}

		// Get island’s current tier for "party-size"
		int currentTier = offlineUser.getHellblockData().getUpgradeLevel(IslandUpgradeType.PARTY_SIZE);

		// Use upgrades system to get effective party size
		return instance.getUpgradeManager().getEffectiveValue(currentTier, IslandUpgradeType.PARTY_SIZE).intValue();
	}

	private OfflinePlayer resolvePlayer(UUID id) {
		return Optional.ofNullable(Bukkit.getPlayer(id)).map(p -> (OfflinePlayer) p)
				.orElse(Bukkit.getOfflinePlayer(id));
	}

	private Optional<Player> requireOnline(UserData user) {
		return Optional.ofNullable(user.getPlayer());
	}

	private Optional<World> getWorldFor(UserData user) {
		return instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(user.getHellblockData().getID()))
				.map(HellblockWorld::bukkitWorld);
	}

	private void send(Sender audience, TranslatableComponent.Builder msg, Component... args) {
		audience.sendMessage(instance.getTranslationManager().render(msg.arguments(args).build()));
	}
}