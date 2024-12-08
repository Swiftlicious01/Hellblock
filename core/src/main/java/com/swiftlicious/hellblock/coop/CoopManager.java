package com.swiftlicious.hellblock.coop;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public class CoopManager {

	protected final HellblockPlugin instance;

	public CoopManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void sendInvite(@NotNull UserData onlineUser, @NotNull UserData playerToInvite) {
		Player owner = onlineUser.getPlayer();
		Player player = playerToInvite.getPlayer();
		if (owner != null) {
			Audience ownerAudience = instance.getSenderFactory().getAudience(owner);
			if (player != null) {
				Audience audience = instance.getSenderFactory().getAudience(player);
				if (onlineUser.getHellblockData().isAbandoned()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
					return;
				}
				if (playerToInvite.getHellblockData().hasHellblock()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_HAS_HELLBLOCK.build()));
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_SELF.build()));
					return;
				}
				if (onlineUser.getHellblockData().getParty().contains(player.getUniqueId())) {
					ownerAudience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_IN_PARTY
									.arguments(Component.text(player.getName())).build()));
					return;
				}
				if (onlineUser.getHellblockData().getBanned().contains(player.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_BANNED_FROM_INVITE.build()));
					return;
				}
				if (playerToInvite.getHellblockData().hasInvite(owner.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXISTS.build()));
					return;
				}

				playerToInvite.getHellblockData().sendInvitation(owner.getUniqueId());
				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_INVITE_SENT
								.arguments(Component.text(player.getName())).build()));
				audience.sendMessage(
						instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_COOP_INVITE_RECEIVED
										.arguments(Component.text(owner.getName()),
												Component.text(instance.getFormattedCooldown(playerToInvite
														.getHellblockData().getInvitations().get(owner.getUniqueId()))))
										.build()));
			} else {
				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE.build()));
				return;
			}
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void rejectInvite(@NotNull UUID ownerID, @NotNull UserData rejectingPlayer) {
		Player player = rejectingPlayer.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (rejectingPlayer.getHellblockData().hasHellblock()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS.build()));
				return;
			}
			if (!rejectingPlayer.getHellblockData().hasInvite(ownerID)) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND.build()));
				return;
			}

			rejectingPlayer.getHellblockData().removeInvitation(ownerID);
			if (Bukkit.getPlayer(ownerID) != null) {
				Audience ownerAudience = instance.getSenderFactory().getAudience(Bukkit.getPlayer(ownerID));
				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_REJECTED_TO_OWNER
								.arguments(Component.text(player.getName())).build()));
			}
			OfflinePlayer owner;
			if (Bukkit.getPlayer(ownerID) != null) {
				owner = Bukkit.getPlayer(ownerID);
			} else {
				owner = Bukkit.getOfflinePlayer(ownerID);
			}
			String username = owner.hasPlayedBefore() && owner.getName() != null ? owner.getName() : "???";
			audience.sendMessage(instance.getTranslationManager().render(
					MessageConstants.MSG_HELLBLOCK_COOP_INVITE_REJECTED.arguments(Component.text(username)).build()));
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void listInvitations(@NotNull UserData onlineUser) {
		Player player = onlineUser.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (onlineUser.getHellblockData().hasHellblock()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS.build()));
				return;
			}
			if (onlineUser.getHellblockData().getInvitations() != null
					&& onlineUser.getHellblockData().getInvitations().isEmpty()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES.build()));
				return;
			}

			for (Map.Entry<UUID, Long> invites : onlineUser.getHellblockData().getInvitations().entrySet()) {
				UUID invitee = invites.getKey();
				OfflinePlayer owner;
				if (Bukkit.getPlayer(invitee) != null) {
					owner = Bukkit.getPlayer(invitee);
				} else {
					owner = Bukkit.getOfflinePlayer(invitee);
				}
				String username = owner.hasPlayedBefore() && owner.getName() != null ? owner.getName() : "???";
				long expirationLeft = invites.getValue().longValue();
				audience.sendMessage(
						instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_COOP_INVITATION_LIST
										.arguments(Component.text(username),
												Component.text(instance.getFormattedCooldown(expirationLeft)))
										.build()));
			}

		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void addMemberToHellblock(@NotNull UUID ownerID, @NotNull UserData playerToAdd) {
		Player player = playerToAdd.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (playerToAdd.getHellblockData().hasHellblock()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS.build()));
				return;
			}
			if (!playerToAdd.getHellblockData().hasInvite(ownerID)) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND.build()));
				return;
			}
			instance.getStorageManager().getOfflineUserData(ownerID, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						if (offlineUser.getHellblockData().isAbandoned()) {
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_INVITE_ABANDONED.build()));
							return;
						}

						Set<UUID> party = offlineUser.getHellblockData().getParty();
						if (party.size() >= getMaxPartySize(offlineUser)) {
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_PARTY_FULL.build()));
							return;
						}
						if (party.contains(player.getUniqueId())) {
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_JOINED_PARTY.build()));
							return;
						}

						Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(instance
								.getWorldManager().getHellblockWorldFormat(offlineUser.getHellblockData().getID()));
						if (world.isEmpty() || world.get() == null)
							throw new NullPointerException(
									"World returned null, please try to regenerate the world before reporting this issue.");
						World bukkitWorld = world.get().bukkitWorld();

						instance.getProtectionManager().getIslandProtection().addMemberToHellblockBounds(bukkitWorld,
								offlineUser.getUUID(), player.getUniqueId());

						playerToAdd.getHellblockData().clearInvitations();
						playerToAdd.getHellblockData().setHasHellblock(true);
						playerToAdd.getHellblockData().setOwnerUUID(ownerID);
						offlineUser.getHellblockData().addToParty(player.getUniqueId());
						if (playerToAdd.getHellblockData().getTrusted().contains(ownerID)) {
							playerToAdd.getHellblockData().removeTrustPermission(ownerID);
						}
						makeHomeLocationSafe(offlineUser, playerToAdd);
						if (offlineUser.isOnline()) {
							Audience ownerAudience = instance.getSenderFactory()
									.getAudience(Bukkit.getPlayer(offlineUser.getUUID()));
							ownerAudience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_ADDED_TO_PARTY
											.arguments(Component.text(player.getName())).build()));
						}
						audience.sendMessage(
								instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_JOINED_PARTY
										.arguments(Component.text(offlineUser.getName())).build()));
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void removeMemberFromHellblock(@NotNull UserData onlineUser, @NotNull String input, @NotNull UUID id) {
		Player owner = onlineUser.getPlayer();
		if (owner != null) {
			Audience ownerAudience = instance.getSenderFactory().getAudience(owner);
			if (!onlineUser.getHellblockData().hasHellblock()) {
				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
				return;
			}
			instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						if (offlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}

						if (owner.getUniqueId().equals(offlineUser.getHellblockData().getOwnerUUID())) {
							ownerAudience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_KICK_SELF.build()));
							return;
						}
						if (!offlineUser.getHellblockData().hasHellblock()) {
							ownerAudience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY
											.arguments(Component.text(input)).build()));
							return;
						}

						Set<UUID> party = onlineUser.getHellblockData().getParty();
						if (!party.contains(id)) {
							ownerAudience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY
											.arguments(Component.text(input)).build()));
							return;
						}

						Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(instance
								.getWorldManager().getHellblockWorldFormat(offlineUser.getHellblockData().getID()));
						if (world.isEmpty() || world.get() == null)
							throw new NullPointerException(
									"World returned null, please try to regenerate the world before reporting this issue.");
						World bukkitWorld = world.get().bukkitWorld();

						instance.getProtectionManager().getIslandProtection()
								.removeMemberFromHellblockBounds(bukkitWorld, owner.getUniqueId(), id);

						offlineUser.getHellblockData().setHasHellblock(false);
						offlineUser.getHellblockData().setOwnerUUID(null);
						onlineUser.getHellblockData().kickFromParty(id);
						ownerAudience.sendMessage(
								instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_PARTY_KICKED
										.arguments(Component.text(input)).build()));
						if (offlineUser.isOnline()) {
							instance.getHellblockHandler().teleportToSpawn(Bukkit.getPlayer(offlineUser.getUUID()),
									true);
							Audience audience = instance.getSenderFactory()
									.getAudience(Bukkit.getPlayer(offlineUser.getUUID()));
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_REMOVED_FROM_PARTY
											.arguments(Component.text(owner.getName())).build()));
						}
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void leaveHellblockParty(@NotNull UserData leavingPlayer) {
		Player player = leavingPlayer.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (!leavingPlayer.getHellblockData().hasHellblock()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
				return;
			}
			if (leavingPlayer.getHellblockData().getOwnerUUID() == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			if (leavingPlayer.getHellblockData().getOwnerUUID() != null
					&& leavingPlayer.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_OWNER_NO_LEAVE.build()));
				return;
			}
			instance.getStorageManager().getOfflineUserData(leavingPlayer.getHellblockData().getOwnerUUID(),
					instance.getConfigManager().lockData()).thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						Set<UUID> party = offlineUser.getHellblockData().getParty();

						party = offlineUser.getHellblockData().getParty();
						if (!party.contains(player.getUniqueId())) {
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_IN_PARTY.build()));
							return;
						}

						Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(instance
								.getWorldManager().getHellblockWorldFormat(offlineUser.getHellblockData().getID()));
						if (world.isEmpty() || world.get() == null)
							throw new NullPointerException(
									"World returned null, please try to regenerate the world before reporting this issue.");
						World bukkitWorld = world.get().bukkitWorld();

						instance.getProtectionManager().getIslandProtection().removeMemberFromHellblockBounds(
								bukkitWorld, offlineUser.getUUID(), leavingPlayer.getUUID());

						leavingPlayer.getHellblockData().setHasHellblock(false);
						leavingPlayer.getHellblockData().setOwnerUUID(null);
						offlineUser.getHellblockData().kickFromParty(player.getUniqueId());
						instance.getHellblockHandler().teleportToSpawn(player, true);
						audience.sendMessage(
								instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_PARTY_LEFT
										.arguments(Component.text(offlineUser.getName())).build()));
						if (offlineUser.isOnline()) {
							Audience ownerAudience = instance.getSenderFactory()
									.getAudience(Bukkit.getPlayer(offlineUser.getUUID()));
							ownerAudience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_LEFT_PARTY
											.arguments(Component.text(player.getName())).build()));
						}
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void transferOwnershipOfHellblock(@NotNull UserData onlineUser, @NotNull UserData playerToTransfer) {
		Player owner = onlineUser.getPlayer();
		Player player = playerToTransfer.getPlayer();
		if (owner != null) {
			Audience ownerAudience = instance.getSenderFactory().getAudience(owner);
			if (player != null) {
				Audience audience = instance.getSenderFactory().getAudience(player);
				if (!instance.getConfigManager().transferIslands()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED.build()));
					return;
				}
				if (!onlineUser.getHellblockData().hasHellblock()) {
					ownerAudience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
					return;
				}
				if (onlineUser.getHellblockData().isAbandoned()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
					return;
				}
				if (onlineUser.getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (!onlineUser.getHellblockData().getOwnerUUID().equals(owner.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_TRANSFER_SELF.build()));
					return;
				}
				if (!playerToTransfer.getHellblockData().hasHellblock()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY
									.arguments(Component.text(player.getName())).build()));
					return;
				}

				Set<UUID> party = onlineUser.getHellblockData().getParty();
				if (!party.contains(player.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY
									.arguments(Component.text(player.getName())).build()));
					return;
				}
				if (onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_OWNER_OF_ISLAND
									.arguments(Component.text(player.getName())).build()));
					return;
				}

				playerToTransfer.getHellblockData().transferHellblockData(onlineUser);
				playerToTransfer.getHellblockData().setTransferCooldown(86400L);
				playerToTransfer.getHellblockData().setOwnerUUID(player.getUniqueId());
				playerToTransfer.getHellblockData().kickFromParty(player.getUniqueId());
				for (UUID partyData : playerToTransfer.getHellblockData().getParty()) {
					instance.getStorageManager().getOfflineUserData(partyData, instance.getConfigManager().lockData())
							.thenAccept((result) -> {
								if (result.isEmpty())
									return;
								UserData offlineParty = result.get();
								offlineParty.getHellblockData().setOwnerUUID(player.getUniqueId());
							});
				}
				playerToTransfer.getHellblockData().addToParty(owner.getUniqueId());
				onlineUser.getHellblockData().resetHellblockData();
				onlineUser.getHellblockData().setHasHellblock(true);
				onlineUser.getHellblockData().setID(0);
				onlineUser.getHellblockData().setTrusted(new HashSet<>());
				onlineUser.getHellblockData().setResetCooldown(0L);
				onlineUser.getHellblockData().setOwnerUUID(player.getUniqueId());

				Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(instance.getWorldManager()
						.getHellblockWorldFormat(playerToTransfer.getHellblockData().getID()));
				if (world.isEmpty() || world.get() == null)
					throw new NullPointerException(
							"World returned null, please try to regenerate the world before reporting this issue.");
				World bukkitWorld = world.get().bukkitWorld();

				instance.getProtectionManager().getIslandProtection().reprotectHellblock(bukkitWorld, onlineUser,
						playerToTransfer);

				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_NEW_OWNER_SET
								.arguments(Component.text(player.getName())).build()));
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_OWNER_TRANSFER_SUCCESS
								.arguments(Component.text(owner.getName())).build()));
			} else {
				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE.build()));
				return;
			}
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public CompletableFuture<Set<UUID>> getVisitors(@NotNull UUID id) {
		CompletableFuture<Set<UUID>> future = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
					if (bounds == null)
						future.complete(new HashSet<>());
					Set<UUID> visitors = new HashSet<>();
					for (UserData user : instance.getStorageManager().getOnlineUsers()) {
						if (!user.isOnline() || user.getPlayer() == null)
							continue;
						Player player = user.getPlayer();
						if (bounds.contains(player.getBoundingBox())) {
							visitors.add(player.getUniqueId());
						}
					}
					future.complete(visitors);
				});
		return future;
	}

	public @Nullable CompletableFuture<UUID> getHellblockOwnerOfVisitingIsland(@NotNull Player player) {
		CompletableFuture<UUID> future = new CompletableFuture<>();
		if (player.getLocation() == null)
			future.complete(null);
		for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers()) {
			instance.getStorageManager().getOfflineUserData(playerData, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
						if (bounds != null) {
							if (bounds.contains(player.getBoundingBox())) {
								UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();
								if (ownerUUID != null) {
									future.complete(ownerUUID);
								}
							}
						}
					});
		}
		return future;
	}

	public CompletableFuture<Boolean> checkIfVisitorIsWelcome(@NotNull Player player, @NotNull UUID id) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					UUID owner = offlineUser.getHellblockData().getOwnerUUID();
					Set<UUID> members = offlineUser.getHellblockData().getParty();
					Set<UUID> trusted = offlineUser.getHellblockData().getTrusted();
					future.complete((owner != null && owner.equals(player.getUniqueId())
							|| (members != null && members.contains(player.getUniqueId()))
							|| (trusted != null && trusted.contains(player.getUniqueId()))
							|| player.hasPermission("hellblock.bypass.lock") || player.hasPermission("hellblock.admin")
							|| player.isOp()));
				});
		return future;
	}

	public CompletableFuture<Void> kickVisitorsIfLocked(@NotNull UUID id) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					if (offlineUser.getHellblockData().isLocked()) {
						getVisitors(offlineUser.getUUID()).thenAccept(visitors -> {
							if (!visitors.isEmpty()) {
								for (UUID visitor : visitors) {
									Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(visitor);
									if (onlineUser.isEmpty())
										continue;
									if (onlineUser.get().isOnline() && onlineUser.get().getPlayer() != null) {
										checkIfVisitorIsWelcome(onlineUser.get().getPlayer(), offlineUser.getUUID())
												.thenAccept((status) -> {
													if (!status) {
														if (onlineUser.get().getHellblockData().hasHellblock()) {
															if (onlineUser.get().getHellblockData()
																	.getOwnerUUID() == null)
																throw new NullPointerException(
																		"Owner reference returned null, please report this to the developer.");

															instance.getStorageManager()
																	.getOfflineUserData(
																			onlineUser.get().getHellblockData()
																					.getOwnerUUID(),
																			instance.getConfigManager().lockData())
																	.thenAccept((owner) -> {
																		if (owner.isEmpty())
																			return;
																		UserData visitorOwner = owner.get();
																		makeHomeLocationSafe(visitorOwner,
																				onlineUser.get());
																	});
														} else {
															instance.getHellblockHandler().teleportToSpawn(
																	onlineUser.get().getPlayer(), true);
														}
														Audience audience = instance.getSenderFactory()
																.getAudience(onlineUser.get().getPlayer());
														audience.sendMessage(instance.getTranslationManager().render(
																MessageConstants.MSG_HELLBLOCK_LOCKED_ENTRY.build()));
													}
												});
									}
								}
							}
						});
					}
				});
		return future;
	}

	public CompletableFuture<Boolean> addTrustAccess(@NotNull UserData onlineUser, @NotNull String input,
			@NotNull UUID id) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		if (!onlineUser.isOnline())
			throw new NullPointerException("Player object returned null, please report this to the developer.");

		Optional<HellblockWorld<?>> world = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(onlineUser.getHellblockData().getID()));
		if (world.isEmpty() || world.get() == null)
			throw new NullPointerException(
					"World returned null, please try to regenerate the world before reporting this issue.");
		World bukkitWorld = world.get().bukkitWorld();

		instance.getProtectionManager().getIslandProtection()
				.getMembersOfHellblockBounds(bukkitWorld, onlineUser.getUUID(), id).thenAccept(trusted -> {
					if (trusted.contains(id)) {
						Audience audience = instance.getSenderFactory().getAudience(onlineUser.getPlayer());
						audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_TRUSTED
										.arguments(AdventureHelper.miniMessage(input)).build()));
						return;
					}

					future.complete(trusted.add(id));
				});
		return future;
	}

	public CompletableFuture<Boolean> removeTrustAccess(@NotNull UserData onlineUser, @NotNull String input,
			@NotNull UUID id) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		if (!onlineUser.isOnline())
			throw new NullPointerException("Player object returned null, please report this to the developer.");

		Optional<HellblockWorld<?>> world = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(onlineUser.getHellblockData().getID()));
		if (world.isEmpty() || world.get() == null)
			throw new NullPointerException(
					"World returned null, please try to regenerate the world before reporting this issue.");
		World bukkitWorld = world.get().bukkitWorld();

		instance.getProtectionManager().getIslandProtection()
				.getMembersOfHellblockBounds(bukkitWorld, onlineUser.getUUID(), id).thenAccept(trusted -> {
					if (!trusted.contains(id)) {
						Audience audience = instance.getSenderFactory().getAudience(onlineUser.getPlayer());
						audience.sendMessage(
								instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_TRUSTED
										.arguments(AdventureHelper.miniMessage(input)).build()));
						return;
					}

					future.complete(trusted.remove(id));
				});
		return future;
	}

	public CompletableFuture<Boolean> trackBannedPlayer(@NotNull UUID bannedFromUUID, @NotNull UUID playerUUID) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserData(bannedFromUUID, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
					Set<UUID> banned = offlineUser.getHellblockData().getBanned();
					future.complete((bounds != null && Bukkit.getPlayer(playerUUID) != null
							&& bounds.contains(Bukkit.getPlayer(playerUUID).getBoundingBox()))
							&& (banned != null && banned.contains(playerUUID)));

				});
		return future;
	}

	public CompletableFuture<Void> makeHomeLocationSafe(@NotNull UserData offlineUser, @NotNull UserData onlineUser) {
		return CompletableFuture.runAsync(() -> {
			if (!onlineUser.isOnline()) {
				throw new NullPointerException("Player object returned null, please report this to the developer.");
			}
			LocationUtils.isSafeLocationAsync(offlineUser.getHellblockData().getHomeLocation()).thenAccept((safe) -> {
				if (!safe.booleanValue()) {
					Audience audience = instance.getSenderFactory().getAudience(onlineUser.getPlayer());
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK.build()));
					instance.getHellblockHandler().locateBedrock(offlineUser.getUUID()).thenAccept((bedrock) -> {
						offlineUser.getHellblockData().setHomeLocation(bedrock);
					}).thenRunAsync(() -> {
						ChunkUtils.teleportAsync(onlineUser.getPlayer(),
								offlineUser.getHellblockData().getHomeLocation(), TeleportCause.PLUGIN);
					});
				}
			});
		});
	}

	public int getMaxPartySize(@NotNull UserData offlineUser) {
		if (!offlineUser.getHellblockData().hasHellblock() || offlineUser.getHellblockData().getParty() == null)
			return -1;

		if (offlineUser.getHellblockData().getOwnerUUID() == null
				|| (offlineUser.getHellblockData().getOwnerUUID() != null
						&& !offlineUser.getHellblockData().getOwnerUUID().equals(offlineUser.getUUID())))
			return -1;

		if (offlineUser.getPlayer().isOp() || offlineUser.getPlayer().hasPermission("hellcoop.size.*")) {
			return instance.getConfigManager().partySize();
		}

		AtomicInteger maxParty = new AtomicInteger(1);
		for (int i = 1; i <= instance.getConfigManager().partySize(); i++) {
			if (offlineUser.getPlayer().hasPermission("hellcoop.size." + i)) {
				maxParty.incrementAndGet();
			} else {
				break;
			}
		}
		return maxParty.get();
	}
}