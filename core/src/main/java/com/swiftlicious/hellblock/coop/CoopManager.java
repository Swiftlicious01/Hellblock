package com.swiftlicious.hellblock.coop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.commands.CommandConfig;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
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
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;
import com.swiftlicious.hellblock.world.WorldManager;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Handles all cooperative (co-op) functionality and player relationship logic
 * for Hellblock islands.
 *
 * <p>
 * This manager is responsible for maintaining the structure and rules of
 * cooperative play, including inviting players, managing party membership,
 * trust access, bans, and ownership transfers. It also ensures consistent
 * behavior through visitor enforcement and cache management.
 *
 * <p>
 * <b>Main responsibilities include:</b>
 * <ul>
 * <li>Sending and managing coop invitations</li>
 * <li>Adding and removing players from a party</li>
 * <li>Handling island owner transfers (voluntary or forced)</li>
 * <li>Enforcing bans, trusted access, and locked island state</li>
 * <li>Identifying owners of locations or blocks for permission checks</li>
 * <li>Auto-kicking unauthorized visitors periodically</li>
 * <li>Maintaining and caching island owner data to reduce I/O</li>
 * <li>Validating home locations for safe teleportation</li>
 * </ul>
 *
 * <p>
 * Many operations are asynchronous and leverage scheduled tasks, completable
 * futures, and database-safe access patterns. This helps keep gameplay smooth
 * and responsive even under heavy server load.
 */
public class CoopManager implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	// Cached island owners
	private volatile Set<UUID> cachedIslandOwners = Collections.emptySet();
	// Last time the cache was refreshed
	private volatile long lastOwnersRefresh = 0L;
	// Refresh interval (e.g., 5 minutes)
	private static final long OWNERS_CACHE_TTL = 5 * 60 * 1000L;

	private final Set<UUID> nonOwnersCache = ConcurrentHashMap.newKeySet();
	// Tracks the last number of resolved owners to prevent repeated logging
	private volatile int lastResolvedOwnerCount = -1; // -1 ensures first run always logs
	private long lastOwnerCountChangeTime = System.currentTimeMillis();
	private boolean loggedStableOwnerCount = false;
	private int lastDebuggedOwnerCount = -1;

	private volatile CompletableFuture<Set<UUID>> ongoingRefresh = null;

	private final Map<UUID, UUID> lastVisitorIslandOwner = new ConcurrentHashMap<>();
	private final Map<UUID, Location> lastVisitorCheckLocation = new ConcurrentHashMap<>();
	private final Map<UUID, Long> lastVisitorCheckTime = new HashMap<>();

	// Sentinel UUID used to represent "no owner" in ConcurrentHashMap caches
	private static final UUID NO_OWNER = new UUID(0L, 0L);

	private static final double MIN_DISTANCE_FOR_RECHECK = 1.5; // blocks
	private static final long CHECK_COOLDOWN_MS = 750; // ms between checks

	// Caches for recent ownership lookups to avoid redundant async calls
	private final Map<PosWithWorld, OwnerCache> ownerCache = new ConcurrentHashMap<>();
	private final Map<UUID, ProximityCache> proximityCache = new ConcurrentHashMap<>();
	private final Map<String, Long> debugCooldowns = new ConcurrentHashMap<>();

	// Configurable constants
	private static final long OWNERSHIP_CHECK_COOLDOWN_MS = 1000; // 1 second
	private static final double OWNERSHIP_CHECK_DISTANCE_SQUARED = 1.0; // within 1 block

	private final Map<UUID, Boolean> lastLockedStateCache = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> lastVisitorCountCache = new ConcurrentHashMap<>();

	private SchedulerTask enforceTask = null;
	private final Map<UUID, EnforcementState> lastEnforcementState = new ConcurrentHashMap<>();

	public CoopManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		this.getCachedIslandOwners();
		this.startEnforcementTask();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		// Clear cache on reload
		cachedIslandOwners.clear();
		nonOwnersCache.clear();
		debugCooldowns.clear();
		lastVisitorIslandOwner.clear();
		lastVisitorCheckLocation.clear();
		lastVisitorCheckTime.clear();
		ownerCache.clear();
		proximityCache.clear();
		lastLockedStateCache.clear();
		lastVisitorCountCache.clear();
		ongoingRefresh = null;
		lastOwnerCountChangeTime = System.currentTimeMillis();
		lastResolvedOwnerCount = -1;
		loggedStableOwnerCount = false;
		if (enforceTask != null && !enforceTask.isCancelled()) {
			enforceTask.cancel();
			enforceTask = null;
		}
		lastEnforcementState.clear();
		lastOwnersRefresh = 0L;
	}

	private record OwnerCache(UUID owner, long time) {
	}

	private record ProximityCache(Location loc, long time) {
	}

	/**
	 * Invalidates the cached ownership data for a specific island owner.
	 * <p>
	 * This is typically called when an island owner loses their island (e.g.
	 * abandoned or transferred).
	 *
	 * @param ownerId The UUID of the island owner to remove from the cache.
	 * @return true if the owner was previously cached and was removed; false
	 *         otherwise.
	 */
	public boolean invalidateCachedOwnerData(@NotNull UUID ownerId) {
		return cachedIslandOwners.remove(ownerId);
	}

	/**
	 * Validates the cached ownership data for a specific island owner.
	 * <p>
	 * This is typically called when an island owner creates their island (e.g.
	 * created or transferred).
	 *
	 * @param ownerId The UUID of the island owner to remove from the cache.
	 * @return true if the owner was previously cached and was added; false
	 *         otherwise.
	 */
	public boolean validateCachedOwnerData(@NotNull UUID ownerId) {
		return cachedIslandOwners.add(ownerId);
	}

	/**
	 * Invalidates the cached set of island owners.
	 * <p>
	 * This method clears the current cache and resets the last refresh timestamp,
	 * forcing the next call to {@code getCachedIslandOwners()} to fetch fresh data
	 * from storage. It should be called whenever a user creates an island or a
	 * change in ownership occurs to ensure cache consistency.
	 */
	public void invalidateIslandOwnersCache() {
		cachedIslandOwners = Collections.emptySet();
		lastOwnersRefresh = 0L; // Forces a refresh on next call
	}

	public void invalidateVisitingIsland(UUID visitorUUID) {
		lastVisitorIslandOwner.remove(visitorUUID);
		lastVisitorCheckLocation.remove(visitorUUID);
	}

	/**
	 * Removes a UUID from the non-owners cache set.
	 * <p>
	 * This is useful when a player's status may have changed (e.g., they became an
	 * island owner), and you want the system to re-evaluate their ownership status
	 * in the next check.
	 *
	 * @param uuid The UUID to remove from the non-owners cache.
	 * @return true if the UUID was present and removed, false if it was not in the
	 *         cache.
	 */
	public boolean removeNonOwnerUUID(@NotNull UUID uuid) {
		if (uuid == null) {
			throw new IllegalArgumentException("UUID must not be null");
		}

		boolean removed = nonOwnersCache.remove(uuid);

		if (removed) {
			instance.debug("Removed UUID " + uuid + " from non-owners cache — will be re-evaluated.");
		} else {
			instance.debug("UUID " + uuid + " was not in non-owners cache — no action taken.");
		}

		return removed;
	}

	/**
	 * Replaces an existing UUID in the non-owners cache with a new UUID.
	 * <p>
	 * This is useful when a player's data has changed (e.g., they became or stopped
	 * being an island owner), and you want to update the cache accordingly.
	 *
	 * @param oldUUID The UUID currently in the cache that should be removed.
	 * @param newUUID The new UUID to add to the cache.
	 */
	public void replaceNonOwnerUUID(@NotNull UUID oldUUID, @NotNull UUID newUUID) {
		if (oldUUID == null || newUUID == null) {
			throw new IllegalArgumentException("UUIDs must not be null");
		}

		// Remove the old UUID if present
		nonOwnersCache.remove(oldUUID);

		// Add the new UUID to the cache
		nonOwnersCache.add(newUUID);

		instance.debug("Replaced cached non-owner UUID: " + oldUUID + " → " + newUUID);
	}

	/**
	 * Starts a repeating task to enforce island access rules:
	 * <ul>
	 * <li>Removes banned players from islands they are not allowed to access.</li>
	 * <li>Kicks non-coop visitors from islands that are locked.</li>
	 * </ul>
	 * 
	 * <p>
	 * This runs every 10 seconds and helps ensure real-time enforcement even if
	 * changes happen outside normal entry logic (e.g. teleportation, permission
	 * edits).
	 */
	public void startEnforcementTask() {
		enforceTask = instance.getScheduler().sync().runRepeating(() -> {
			// --- 1. Enforce bans on online players ---
			// If a player is banned from any island and is currently within its bounds,
			// they are removed.
			instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer).filter(Objects::nonNull)
					.forEach(this::enforceIslandBanIfNeeded);

			// --- 2. Enforce visitor restrictions on locked islands ---
			// For every cached island owner, check if their island is locked, and remove
			// unauthorized visitors.
			getCachedIslandOwners().thenAccept(ownerUUIDs -> {
				for (UUID ownerUUID : ownerUUIDs) {
					kickVisitorsIfLocked(ownerUUID).exceptionally(ex -> {
						instance.getPluginLogger().warn("Failed to kick visitors for locked island " + ownerUUID, ex);
						return null;
					});
				}
			});
		}, 20 * 10L, 20 * 10L, LocationUtils.getAnyLocationInstance()); // 10s delay and repeat
	}

	/**
	 * Sends a cooperative (co-op) invitation from an island owner to another
	 * player.
	 *
	 * <p>
	 * Performs checks to ensure the owner can send an invite, verifies the target
	 * player isn't already a party member, banned, or already invited, and
	 * dispatches the invite both visually and via mailbox if the player is offline.
	 *
	 * @param ownerData      The UserData of the island owner sending the invite.
	 * @param playerToInvite The UserData of the player being invited.
	 */
	public CompletableFuture<Boolean> sendInvite(@NotNull UserData ownerData, @NotNull UserData playerToInvite) {
		Optional<Player> ownerOpt = requireOnline(ownerData);
		if (ownerOpt.isEmpty()) {
			return CompletableFuture.completedFuture(false);
		}

		final Player owner = ownerOpt.get();
		final Sender ownerAudience = instance.getSenderFactory().wrap(owner);
		final UUID targetId = playerToInvite.getUUID();

		instance.debug("Attempting to send coop invite from " + owner.getName() + " to " + playerToInvite.getName());

		// Prevent invite from abandoned islands
		if (ownerData.getHellblockData().isAbandoned()) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
			return CompletableFuture.completedFuture(false);
		}

		// Prevent self-invites
		if (owner.getUniqueId().equals(targetId)) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_SELF);
			return CompletableFuture.completedFuture(false);
		}

		// Already in the party
		if (ownerData.getHellblockData().getPartyMembers().contains(targetId)) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_IN_PARTY,
					AdventureHelper.miniMessageToComponent(playerToInvite.getName()));
			return CompletableFuture.completedFuture(false);
		}

		// Banned players cannot be invited
		if (ownerData.getHellblockData().getBannedMembers().contains(targetId)) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_BANNED_FROM_INVITE);
			return CompletableFuture.completedFuture(false);
		}

		// Prevent duplicate invites
		if (playerToInvite.getHellblockData().hasInvite(owner.getUniqueId())) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXISTS);
			return CompletableFuture.completedFuture(false);
		}

		// Fire event for plugins to cancel invite if needed
		final CoopInviteSendEvent inviteEvent = new CoopInviteSendEvent(ownerData, playerToInvite);
		if (EventUtils.fireAndCheckCancel(inviteEvent)) {
			return CompletableFuture.completedFuture(false);
		}

		// Store the invitation in target's data
		playerToInvite.getHellblockData().sendInvitation(owner.getUniqueId());

		instance.debug("Coop invite sent from " + owner.getName() + " to " + playerToInvite.getName());

		send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_SENT,
				AdventureHelper.miniMessageToComponent(playerToInvite.getName()));

		// Player is online → send interactive message
		if (playerToInvite.isOnline()) {
			final Player targetPlayer = playerToInvite.getPlayer();
			final Sender playerAudience = instance.getSenderFactory().wrap(targetPlayer);

			String ownerName = owner.getName();
			String expiresIn = instance.getCooldownManager()
					.getFormattedCooldown(playerToInvite.getHellblockData().getInvitations().get(owner.getUniqueId()));

			YamlDocument config = instance.getConfigManager().loadConfig(HellblockCommandManager.commandsFile);
			CommandConfig<?> acceptConfig = instance.getCommandManager().getCommandConfig(config, "coop_accept");
			CommandConfig<?> rejectConfig = instance.getCommandManager().getCommandConfig(config, "coop_reject");

			List<String> acceptUsages = acceptConfig.getUsages();
			List<String> rejectUsages = rejectConfig.getUsages();

			if (acceptUsages.isEmpty()) {
				return CompletableFuture.failedFuture(
						new IllegalStateException("No usages defined for 'coop_accept' command in commands.yml"));
			}

			if (rejectUsages.isEmpty()) {
				return CompletableFuture.failedFuture(
						new IllegalStateException("No usages defined for 'coop_reject' command in commands.yml"));
			}

			String acceptCommand = acceptUsages.get(0);
			String rejectCommand = rejectUsages.get(0);

			// Header: "X invited you to their hellblock!"
			Component header = MessageConstants.MSG_HELLBLOCK_COOP_INVITE_RECEIVED
					.arguments(AdventureHelper.miniMessageToComponent(ownerName)).build();

			// Accept button
			Component accept = MessageConstants.BTN_HELLBLOCK_COOP_INVITE_ACCEPT
					.clickEvent(ClickEvent.runCommand(acceptCommand + Component.space() + ownerName))
					.hoverEvent(HoverEvent.showText(MessageConstants.BTN_HELLBLOCK_COOP_INVITE_ACCEPT_HOVER.build()))
					.build();

			// Decline button
			Component decline = MessageConstants.BTN_HELLBLOCK_COOP_INVITE_DECLINE
					.clickEvent(ClickEvent.runCommand(rejectCommand + Component.space() + ownerName))
					.hoverEvent(HoverEvent.showText(MessageConstants.BTN_HELLBLOCK_COOP_INVITE_DECLINE_HOVER.build()))
					.build();

			// Expiration message
			Component expires = MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXPIRES
					.arguments(AdventureHelper.miniMessageToComponent(expiresIn)).build();

			Component fullMessage = Component.join(JoinConfiguration.builder().separator(Component.space()).build(),
					List.of(header, accept, decline, expires));

			playerAudience.sendMessage(instance.getTranslationManager().render(fullMessage));
			instance.debug("Invite UI sent to online player " + targetPlayer.getName());

		} else {
			// Offline → send mailbox entry
			instance.getMailboxManager().queue(playerToInvite.getUUID(),
					new MailboxEntry("message.hellblock.coop.invite.offline",
							List.of(AdventureHelper.miniMessageToComponent(owner.getName())),
							Set.of(MailboxFlag.NOTIFY_PARTY)));

			instance.debug("Offline coop invite queued in mailbox for " + playerToInvite.getUUID());
		}

		return CompletableFuture.completedFuture(true);
	}

	/**
	 * Rejects a co-op invitation sent by another island owner.
	 *
	 * <p>
	 * This removes the invitation from the rejecting player's data, notifies the
	 * inviter (if online), and optionally shows confirmation to the rejecting
	 * player.
	 *
	 * @param ownerId         The UUID of the island owner who sent the invite.
	 * @param rejectingPlayer The player rejecting the invitation.
	 */
	public void rejectInvite(@NotNull UUID ownerId, @NotNull UserData rejectingPlayer) {
		final Player player = requireOnline(rejectingPlayer)
				.orElseThrow(() -> new IllegalStateException("Rejecting player must be online."));

		final Sender audience = instance.getSenderFactory().wrap(player);

		instance.debug("Player " + player.getName() + " is attempting to reject coop invite from " + ownerId);

		// No invite found from this owner
		if (!rejectingPlayer.getHellblockData().hasInvite(ownerId)) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
			return;
		}

		// Fire plugin event for invite rejection
		final CoopInviteRejectEvent rejectEvent = new CoopInviteRejectEvent(ownerId, rejectingPlayer);
		EventUtils.fireAndForget(rejectEvent);

		// Remove the invite
		rejectingPlayer.getHellblockData().removeInvitation(ownerId);
		instance.debug("Coop invite from " + ownerId + " rejected by " + player.getName());

		// Notify the inviter (if online)
		Optional.ofNullable(Bukkit.getPlayer(ownerId)).map(ownerPlayer -> instance.getSenderFactory().wrap(ownerPlayer))
				.ifPresent(ownerAudience -> send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_REJECTED_TO_OWNER,
						AdventureHelper.miniMessageToComponent(player.getName())));

		// Show confirmation to rejecting player
		final OfflinePlayer owner = resolvePlayer(ownerId);
		final String username = owner.hasPlayedBefore() && owner.getName() != null ? owner.getName()
				: instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());

		send(audience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_REJECTED,
				AdventureHelper.miniMessageToComponent(username));
	}

	/**
	 * Displays a paginated list of active co-op invitations sent to the player.
	 *
	 * <p>
	 * This method filters out expired invitations, paginates them, and builds
	 * interactive components for accepting or declining each invite. If the player
	 * has no invitations, a message is shown instead.
	 *
	 * @param playerData The UserData of the player viewing their invites.
	 * @param page       The page number to display (1-based).
	 */
	public void listInvitations(@NotNull UserData playerData, int page) {
		final Player player = requireOnline(playerData)
				.orElseThrow(() -> new IllegalStateException("Player must be online to view invites."));

		final Sender audience = instance.getSenderFactory().wrap(player);

		instance.debug("Listing coop invites for " + player.getName() + " (page " + page + ")");

		final Map<UUID, Long> rawInvites = playerData.getHellblockData().getInvitations();

		// Filter and sort valid (non-expired) invites by expiration time
		List<Map.Entry<UUID, Long>> invites = rawInvites.entrySet().stream()
				.filter(entry -> entry.getValue() > System.currentTimeMillis()).sorted(Map.Entry.comparingByValue())
				.toList();

		if (invites.isEmpty()) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES);
			return;
		}

		// Pagination calculations
		int totalPages = (int) Math.ceil(invites.size() / 10.0);
		if (page < 1 || page > totalPages) {
			send(audience, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
					AdventureHelper.miniMessageToComponent(String.valueOf(page)),
					AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
			return;
		}

		// Send pagination header
		send(audience,
				MessageConstants.MSG_HELLBLOCK_COOP_INVITATION_HEADER.arguments(
						AdventureHelper.miniMessageToComponent(String.valueOf(page)),
						AdventureHelper.miniMessageToComponent(String.valueOf(totalPages))));

		int start = (page - 1) * 10;
		int end = Math.min(start + 10, invites.size());

		// Load commands
		YamlDocument config = instance.getConfigManager().loadConfig(HellblockCommandManager.commandsFile);
		CommandConfig<?> acceptConfig = instance.getCommandManager().getCommandConfig(config, "coop_accept");
		CommandConfig<?> rejectConfig = instance.getCommandManager().getCommandConfig(config, "coop_reject");

		List<String> acceptUsages = acceptConfig.getUsages();
		List<String> rejectUsages = rejectConfig.getUsages();

		if (acceptUsages.isEmpty()) {
			throw new IllegalStateException("No usages defined for 'coop_accept' command in commands.yml");
		}

		if (rejectUsages.isEmpty()) {
			throw new IllegalStateException("No usages defined for 'coop_reject' command in commands.yml");
		}

		String acceptCommand = acceptUsages.get(0);
		String rejectCommand = rejectUsages.get(0);

		// Render each invite
		for (int i = start; i < end; i++) {
			Map.Entry<UUID, Long> entry = invites.get(i);
			UUID ownerUUID = entry.getKey();
			long expiration = entry.getValue();

			final OfflinePlayer owner = resolvePlayer(ownerUUID);
			final String username = (owner.hasPlayedBefore() && owner.getName() != null) ? owner.getName()
					: instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());

			final String remaining = instance.getCooldownManager()
					.getFormattedCooldown((expiration - System.currentTimeMillis()) / 1000);

			// Prefix: "- <username>"
			Component prefix = MessageConstants.MSG_HELLBLOCK_COOP_INVITE_ENTRY
					.arguments(AdventureHelper.miniMessageToComponent(username)).build();

			// Accept button
			Component accept = MessageConstants.BTN_HELLBLOCK_COOP_INVITE_ACCEPT
					.clickEvent(ClickEvent.runCommand(acceptCommand + Component.space() + username))
					.hoverEvent(HoverEvent.showText(MessageConstants.BTN_HELLBLOCK_COOP_INVITE_ACCEPT_HOVER.build()))
					.build();

			// Decline button
			Component decline = MessageConstants.BTN_HELLBLOCK_COOP_INVITE_DECLINE
					.clickEvent(ClickEvent.runCommand(rejectCommand + Component.space() + username))
					.hoverEvent(HoverEvent.showText(MessageConstants.BTN_HELLBLOCK_COOP_INVITE_DECLINE_HOVER.build()))
					.build();

			// Expiration info
			Component expires = MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXPIRES
					.arguments(AdventureHelper.miniMessageToComponent(remaining)).build();

			// Combine into final line
			Component fullEntry = Component.join(JoinConfiguration.builder().separator(Component.space()).build(),
					List.of(prefix, accept, decline, expires));

			audience.sendMessage(instance.getTranslationManager().render(fullEntry));
		}
		instance.debug("Displayed invite entries from " + invites.size() + " player" + (invites.size() == 1 ? "" : "s")
				+ " to " + player.getName());
	}

	/**
	 * Adds a player to the specified owner's Hellblock party, if eligible.
	 *
	 * <p>
	 * Performs all validation steps including checking for: - existing island
	 * ownership - valid invite - party capacity - trust conflict resolution
	 *
	 * @param ownerId     The UUID of the island owner.
	 * @param playerToAdd The player being added to the island.
	 */
	public CompletableFuture<Boolean> addMemberToHellblock(@NotNull UUID ownerId, @NotNull UserData playerToAdd) {
		Optional<Player> playerOpt = requireOnline(playerToAdd);
		if (playerOpt.isEmpty()) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("addMemberToHellblock: Player must be online to join a Hellblock."));
		}
		final Player player = playerOpt.get();
		final Sender audience = instance.getSenderFactory().wrap(player);

		instance.debug(
				"addMemberToHellblock: Attempting to add player " + player.getName() + " to party owned by " + ownerId);

		if (playerToAdd.getHellblockData().hasHellblock()) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS);
			return CompletableFuture.completedFuture(false);
		}

		if (!playerToAdd.getHellblockData().hasInvite(ownerId)) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
			return CompletableFuture.completedFuture(false);
		}

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("addMemberToHellblock: Owner data not found for " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();
			final HellblockData hellblockData = ownerData.getHellblockData();

			if (hellblockData.isAbandoned()) {
				send(audience, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_ABANDONED);
				return CompletableFuture.completedFuture(false);
			}

			final Set<UUID> party = hellblockData.getPartyMembers();
			if (party.size() >= getMaxPartySize(ownerData)) {
				send(audience, MessageConstants.MSG_HELLBLOCK_COOP_PARTY_FULL);
				return CompletableFuture.completedFuture(false);
			}

			if (party.contains(player.getUniqueId())) {
				send(audience, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_JOINED_PARTY);
				return CompletableFuture.completedFuture(false);
			}

			Optional<HellblockWorld<?>> worldOpt = getWorldFor(ownerData);
			if (worldOpt.isEmpty()) {
				return CompletableFuture
						.failedFuture(new IllegalStateException("addMemberToHellblock: Hellblock world not found."));
			}
			final HellblockWorld<?> world = worldOpt.get();

			// Add member to protection first
			return instance.getProtectionManager().getIslandProtection()
					.addMemberToHellblockBounds(world, ownerData.getUUID(), player.getUniqueId()).thenCompose(added -> {
						if (!added) {
							return CompletableFuture.failedFuture(new IllegalStateException(
									"addMemberToHellblock: Failed to update island protection for new member "
											+ player.getName()));
						}

						// Update in-memory party data
						playerToAdd.getHellblockData().setHasHellblock(true);
						playerToAdd.getHellblockData().setOwnerUUID(ownerId);
						ownerData.getHellblockData().addToParty(player.getUniqueId());

						// Fire event
						EventUtils.fireAndForget(new CoopJoinEvent(ownerId, playerToAdd));

						// Remove from trust list if present
						if (ownerData.getHellblockData().getTrustedMembers().contains(playerToAdd.getUUID())) {
							ownerData.getHellblockData().removeTrustPermission(playerToAdd.getUUID());
						}

						// Make home safe and respond accordingly
						return makeHomeLocationSafe(ownerData, playerToAdd).thenCompose(safetyResult -> {
							return switch (safetyResult) {
							case ALREADY_SAFE -> {
								instance.debug("addMemberToHellblock: Home is already safe for " + player.getName());
								yield instance.getHellblockHandler().teleportPlayerToHome(playerToAdd,
										ownerData.getHellblockData().getHomeLocation());
							}
							case FIXED_AND_TELEPORTED -> {
								instance.debug("addMemberToHellblock: Home fixed and teleport complete for "
										+ player.getName());
								yield CompletableFuture.completedFuture(true);
							}
							case FAILED_TO_FIX -> {
								instance.getPluginLogger()
										.warn("addMemberToHellblock: Failed to fix home for " + player.getName());
								yield instance.getScheduler().callSync(() -> {
									return CompletableFuture.completedFuture(
											instance.getHellblockHandler().teleportToSpawn(player, true)); // Cancel
																											// flow
								});
							}
							};
						});
					}).thenCompose(shouldContinue -> {
						if (!shouldContinue) {
							return CompletableFuture.completedFuture(false);
						}

						instance.getBorderHandler().startBorderTask(player.getUniqueId());

						if (ownerData.isOnline()) {
							final Sender ownerAudience = instance.getSenderFactory().wrap(Bukkit.getPlayer(ownerId));
							send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_ADDED_TO_PARTY,
									AdventureHelper.miniMessageToComponent(player.getName()));
						} else {
							instance.getMailboxManager().queue(ownerData.getUUID(),
									new MailboxEntry("message.hellblock.coop.added.offline",
											List.of(AdventureHelper.miniMessageToComponent(player.getName())),
											Set.of(MailboxFlag.NOTIFY_OWNER)));
						}

						send(audience, MessageConstants.MSG_HELLBLOCK_COOP_JOINED_PARTY,
								AdventureHelper.miniMessageToComponent(ownerData.getName()));

						instance.debug("addMemberToHellblock: Player " + player.getName() + " successfully added to "
								+ ownerData.getName() + "'s party.");

						return instance.getStorageManager().saveUserData(playerToAdd, true);
					});
		}).handle((result, ex) -> {
			return instance.getStorageManager().unlockUserData(ownerId).thenApply(unused -> {
				if (ex != null) {
					instance.getPluginLogger().warn("addMemberToHellblock: Failed to add player "
							+ playerToAdd.getName() + " to hellblock: " + ex.getMessage(), ex);
					return false;
				}
				return result != null && result;
			});
		}).thenCompose(Function.identity());
	}

	/**
	 * Removes a member from the specified owner's Hellblock party.
	 *
	 * <p>
	 * This method performs validation, permission updates, teleportation, and
	 * messaging.
	 *
	 * @param ownerData The owner removing the member.
	 * @param input     The input string used to identify the member.
	 * @param memberId  The UUID of the member to remove.
	 */
	public CompletableFuture<Boolean> removeMemberFromHellblock(@NotNull UserData ownerData, @NotNull String input,
			@NotNull UUID memberId) {
		Optional<Player> ownerOpt = requireOnline(ownerData);
		if (ownerOpt.isEmpty()) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("removeMemberFromHellblock: Owner must be online to remove a member."));
		}
		final Player owner = ownerOpt.get();
		final Sender ownerAudience = instance.getSenderFactory().wrap(owner);

		instance.debug("removeMemberFromHellblock: Attempting to remove member " + memberId + " from " + owner.getName()
				+ "'s party");

		if (!ownerData.getHellblockData().hasHellblock()) {
			send(ownerAudience, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
			return CompletableFuture.completedFuture(false);
		}

		return instance.getStorageManager().getCachedUserDataWithFallback(memberId, true).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("removeMemberFromHellblock: Could not find member data for UUID " + memberId);
				return CompletableFuture.completedFuture(false);
			}

			final UserData memberData = optData.get();

			if (Objects.equals(owner.getUniqueId(), memberData.getHellblockData().getOwnerUUID())) {
				send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NO_KICK_SELF);
				return CompletableFuture.completedFuture(false);
			}

			if (!memberData.getHellblockData().hasHellblock()
					|| !ownerData.getHellblockData().getPartyMembers().contains(memberId)) {
				send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY,
						AdventureHelper.miniMessageToComponent(input));
				return CompletableFuture.completedFuture(false);
			}

			Optional<HellblockWorld<?>> worldOpt = getWorldFor(memberData);
			if (worldOpt.isEmpty()) {
				return CompletableFuture.failedFuture(
						new IllegalStateException("removeMemberFromHellblock: Hellblock world not found."));
			}
			final HellblockWorld<?> world = worldOpt.get();

			final CoopKickEvent kickEvent = new CoopKickEvent(ownerData, memberData);
			if (EventUtils.fireAndCheckCancel(kickEvent)) {
				return CompletableFuture.completedFuture(false);
			}

			return instance.getProtectionManager().getIslandProtection()
					.removeMemberFromHellblockBounds(world, owner.getUniqueId(), memberId).thenCompose(removed -> {
						if (!removed) {
							return CompletableFuture.failedFuture(new IllegalStateException(
									"removeMemberFromHellblock: Failed to remove member " + memberData.getName()
											+ " from protection in " + owner.getName() + "'s hellblock"));
						}

						ownerData.getHellblockData().kickFromParty(memberId);
						memberData.getHellblockData().resetHellblockData();

						send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_PARTY_KICKED,
								AdventureHelper.miniMessageToComponent(input));

						if (memberData.isOnline()) {
							final Player kickedPlayer = memberData.getPlayer();
							instance.getBorderHandler().stopBorderTask(kickedPlayer.getUniqueId());
							kickedPlayer.closeInventory();
							instance.getHellblockHandler().teleportToSpawn(kickedPlayer, true);

							final Sender kickedAudience = instance.getSenderFactory().wrap(kickedPlayer);
							send(kickedAudience, MessageConstants.MSG_HELLBLOCK_COOP_REMOVED_FROM_PARTY,
									AdventureHelper.miniMessageToComponent(owner.getName()));
						} else {
							instance.getMailboxManager().queue(memberData.getUUID(),
									new MailboxEntry("message.hellblock.coop.kicked.offline",
											List.of(AdventureHelper.miniMessageToComponent(owner.getName())),
											Set.of(MailboxFlag.NOTIFY_PARTY, MailboxFlag.UNSAFE_LOCATION)));
						}

						instance.debug("removeMemberFromHellblock: Successfully removed " + memberId + " from "
								+ owner.getName() + "'s party.");
						return instance.getStorageManager().saveUserData(memberData, true);
					});
		}).handle((result, ex) -> {
			return instance.getStorageManager().unlockUserData(memberId).thenApply(unused -> {
				if (ex != null) {
					instance.getPluginLogger()
							.warn("removeMemberFromHellblock: Failed to remove player from coop party (" + input + "): "
									+ ex.getMessage(), ex);
					return false;
				}
				return result != null && result;
			});
		}).thenCompose(Function.identity());
	}

	/**
	 * Allows a player to voluntarily leave a Hellblock party they are a member of.
	 *
	 * <p>
	 * Resets their island data and moves them back to spawn. Owner cannot leave
	 * their own island.
	 *
	 * @param leavingPlayer The player requesting to leave the party.
	 */
	public CompletableFuture<Boolean> leaveHellblockParty(@NotNull UserData leavingPlayer) {
		Optional<Player> playerOpt = requireOnline(leavingPlayer);
		if (playerOpt.isEmpty()) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("leaveHellblockParty: Player must be online to leave party."));
		}
		final Player player = playerOpt.get();

		final Sender audience = instance.getSenderFactory().wrap(player);

		instance.debug("leaveHellblockParty: Player " + player.getName() + " is attempting to leave their party.");

		if (!leavingPlayer.getHellblockData().hasHellblock()) {
			send(audience, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
			return CompletableFuture.completedFuture(false);
		}

		final UUID ownerId = leavingPlayer.getHellblockData().getOwnerUUID();
		if (ownerId == null) {
			instance.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
					+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
			return CompletableFuture.failedFuture(new IllegalStateException(
					"Owner reference was null. This should never happen — please report to the developer."));
		}

		if (ownerId.equals(player.getUniqueId())) {
			send(audience, MessageConstants.MSG_HELLBLOCK_COOP_OWNER_NO_LEAVE);
			return CompletableFuture.completedFuture(false);
		}

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("leaveHellblockParty: Owner data not found for UUID " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();

			if (!ownerData.getHellblockData().getPartyMembers().contains(player.getUniqueId())) {
				send(audience, MessageConstants.MSG_HELLBLOCK_COOP_NOT_IN_PARTY);
				return CompletableFuture.completedFuture(false);
			}

			Optional<HellblockWorld<?>> worldOpt = getWorldFor(ownerData);
			if (worldOpt.isEmpty()) {
				return CompletableFuture
						.failedFuture(new IllegalStateException("leaveHellblockParty: Hellblock world not found."));
			}
			final HellblockWorld<?> world = worldOpt.get();

			return instance.getProtectionManager().getIslandProtection()
					.removeMemberFromHellblockBounds(world, ownerId, player.getUniqueId()).thenCompose(removed -> {
						if (!removed) {
							return CompletableFuture.failedFuture(
									new IllegalStateException("leaveHellblockParty: Failed to remove member "
											+ leavingPlayer.getName() + " from hellblock owned by " + ownerId));
						}

						// Proceed only after protection is updated
						ownerData.getHellblockData().kickFromParty(player.getUniqueId());
						leavingPlayer.getHellblockData().resetHellblockData();

						final CoopLeaveEvent leaveEvent = new CoopLeaveEvent(leavingPlayer, ownerId);
						EventUtils.fireAndForget(leaveEvent);

						instance.getBorderHandler().stopBorderTask(leavingPlayer.getUUID());
						player.closeInventory();
						instance.getHellblockHandler().teleportToSpawn(player, true);

						send(audience, MessageConstants.MSG_HELLBLOCK_COOP_PARTY_LEFT,
								AdventureHelper.miniMessageToComponent(ownerData.getName()));

						if (ownerData.isOnline()) {
							final Sender ownerAudience = instance.getSenderFactory().wrap(Bukkit.getPlayer(ownerId));
							send(ownerAudience, MessageConstants.MSG_HELLBLOCK_COOP_LEFT_PARTY,
									AdventureHelper.miniMessageToComponent(player.getName()));
						} else {
							instance.getMailboxManager().queue(ownerData.getUUID(),
									new MailboxEntry("message.hellblock.coop.member.left.offline",
											List.of(AdventureHelper.miniMessageToComponent(player.getName())),
											Set.of(MailboxFlag.NOTIFY_OWNER)));
						}

						instance.debug("leaveHellblockParty: Player " + player.getName()
								+ " has successfully left the party owned by " + ownerData.getName());

						return instance.getStorageManager().saveUserData(ownerData, true);
					});
		}).handle((result, ex) -> {
			return instance.getStorageManager().unlockUserData(ownerId).thenApply(unused -> {
				if (ex != null) {
					instance.getPluginLogger()
							.warn("leaveHellblockParty: Failed to leave coop party (Could not read player "
									+ leavingPlayer.getName() + "'s data): " + ex.getMessage(), ex);
					return false;
				}
				return result != null && result;
			});
		}).thenCompose(Function.identity());
	}

	/**
	 * Transfers ownership of a Hellblock island from the current owner to a new
	 * owner.
	 *
	 * <p>
	 * This includes all relevant island and cache data transfer, validation checks,
	 * cooldown enforcement, and user messaging.
	 *
	 * @param currentOwnerData The user currently owning the Hellblock island.
	 * @param newOwnerData     The user to receive ownership of the island.
	 * @param forcedByAdmin    Whether the transfer was forced by an admin (skips
	 *                         all checks).
	 * @return true if transfer was successful; false otherwise.
	 */
	public CompletableFuture<Boolean> transferOwnershipOfHellblock(@NotNull UserData currentOwnerData,
			@NotNull UserData newOwnerData, boolean forcedByAdmin) {
		Player currentOwnerPlayer = currentOwnerData.getPlayer();
		final Player newOwnerPlayer = newOwnerData.getPlayer();

		final Sender currentOwnerAudience = currentOwnerPlayer != null
				? instance.getSenderFactory().wrap(currentOwnerPlayer)
				: null;
		final Sender newOwnerAudience = newOwnerPlayer != null ? instance.getSenderFactory().wrap(newOwnerPlayer)
				: null;

		instance.debug("transferOwnershipOfHellblock: Starting ownership transfer from %s to %s. Forced by admin: %s"
				.formatted(currentOwnerData.getName(), newOwnerData.getName(), forcedByAdmin));

		// Check global config if transfer is allowed
		if (!instance.getConfigManager().transferIslands() && !forcedByAdmin) {
			instance.debug("transferOwnershipOfHellblock: Transfer blocked - global config prevents island transfers.");
			if (currentOwnerAudience != null) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED);
			}
			if (newOwnerAudience != null) {
				send(newOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED);
			}
			return CompletableFuture.completedFuture(false);
		}

		// --- VALIDATION ---
		if (!forcedByAdmin) {
			Optional<Player> currentOpt = requireOnline(currentOwnerData);
			if (currentOpt.isEmpty()) {
				return CompletableFuture.completedFuture(false);
			}
			currentOwnerPlayer = currentOpt.get();

			UUID currentOwnerId = currentOwnerPlayer.getUniqueId();

			if (!currentOwnerData.getHellblockData().hasHellblock()) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return CompletableFuture.completedFuture(false);
			}

			if (currentOwnerData.getHellblockData().isAbandoned()) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
				return CompletableFuture.completedFuture(false);
			}

			if (!currentOwnerId.equals(currentOwnerData.getHellblockData().getOwnerUUID())) {
				send(currentOwnerAudience, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
				return CompletableFuture.completedFuture(false);
			}

			if (currentOwnerId.equals(newOwnerData.getUUID())) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NO_TRANSFER_SELF);
				return CompletableFuture.completedFuture(false);
			}

			if (!newOwnerData.getHellblockData().hasHellblock()
					|| !currentOwnerData.getHellblockData().getPartyMembers().contains(newOwnerData.getUUID())) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY,
						AdventureHelper.miniMessageToComponent(newOwnerData.getName()));
				return CompletableFuture.completedFuture(false);
			}

			if (currentOwnerData.getHellblockData().getOwnerUUID().equals(newOwnerData.getUUID())) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_OWNER_OF_ISLAND,
						AdventureHelper.miniMessageToComponent(newOwnerData.getName()));
				return CompletableFuture.completedFuture(false);
			}

			if (currentOwnerData.getHellblockData().getTransferCooldown() > 0) {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_TRANSFER_ON_COOLDOWN
						.arguments(AdventureHelper.miniMessageToComponent(instance.getCooldownManager()
								.getFormattedCooldown(currentOwnerData.getHellblockData().getTransferCooldown()))));
				return CompletableFuture.completedFuture(false);
			}
		}

		final Optional<HellblockWorld<?>> optWorld = getWorldFor(newOwnerData);
		if (optWorld.isEmpty()) {
			return CompletableFuture.completedFuture(false);
		}
		final HellblockWorld<?> world = optWorld.get();

		// --- TRANSFER LOGIC ---
		instance.debug("transferOwnershipOfHellblock: Transferring Hellblock data...");
		newOwnerData.getHellblockData().transferHellblockData(currentOwnerData);
		newOwnerData.getChallengeData().clearChallengeMeta();
		newOwnerData.getHellblockData().setTransferCooldown(forcedByAdmin ? 0L : TimeUnit.DAYS.toSeconds(1));
		newOwnerData.getHellblockData().setOwnerUUID(newOwnerData.getUUID());
		newOwnerData.getHellblockData().kickFromParty(newOwnerData.getUUID());

		List<CompletableFuture<Boolean>> futures = new ArrayList<>();

		for (UUID partyId : newOwnerData.getHellblockData().getPartyMembers()) {
			CompletableFuture<Boolean> partyFuture = instance.getStorageManager()
					.getCachedUserDataWithFallback(partyId, true).thenCompose(optPartyData -> {
						if (optPartyData.isEmpty())
							return CompletableFuture.completedFuture(false);
						UserData partyData = optPartyData.get();
						partyData.getHellblockData().setOwnerUUID(newOwnerData.getUUID());
						partyData.getChallengeData().clearChallengeMeta();
						return instance.getStorageManager().saveUserData(partyData, true);
					}).handle((result, ex) -> {
						return instance.getStorageManager().unlockUserData(partyId).thenApply(unused -> {
							if (ex != null) {
								instance.getPluginLogger()
										.warn("transferOwnershipOfHellblock: Failed to update party member " + partyId
												+ ": " + ex.getMessage(), ex);
								return false;
							}
							return result != null && result;
						});
					}).thenCompose(Function.identity());

			futures.add(partyFuture);
		}

		// Wait for reprotect to complete
		futures.add(instance.getProtectionManager().getIslandProtection()
				.reprotectHellblock(world, currentOwnerData, newOwnerData).thenApply(x -> true).exceptionally(ex -> {
					instance.getPluginLogger()
							.warn("transferOwnershipOfHellblock: Failed to reprotect hellblock: " + ex.getMessage());
					return false;
				}));

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
			boolean success = futures.stream().map(CompletableFuture::join).allMatch(Boolean::booleanValue);
			if (!success) {
				instance.getPluginLogger()
						.warn("transferOwnershipOfHellblock: One or more steps failed during ownership transfer from '"
								+ currentOwnerData.getName() + "' (" + currentOwnerData.getUUID() + ") to '"
								+ newOwnerData.getName() + "' (" + newOwnerData.getUUID() + "). "
								+ "This may indicate a failure in party member updates or island reprotection.");
			}

			instance.debug(
					"transferOwnershipOfHellblock: Updated all party members for new owner " + newOwnerData.getName());

			newOwnerData.getHellblockData().addToParty(currentOwnerData.getUUID());

			instance.getStorageManager().getDataSource()
					.invalidateIslandCache(currentOwnerData.getHellblockData().getIslandId());
			instance.getProtectionManager()
					.invalidateIslandChunkCache(currentOwnerData.getHellblockData().getIslandId());
			instance.getProtectionManager().cancelBlockScan(currentOwnerData.getUUID());

			int currentIslandId = currentOwnerData.getHellblockData().getIslandId();

			currentOwnerData.getHellblockData().resetHellblockData();
			currentOwnerData.getHellblockData().setHasHellblock(true);
			currentOwnerData.getHellblockData().setIslandId(0);
			currentOwnerData.getChallengeData().clearChallengeMeta();
			currentOwnerData.getHellblockData().setResetCooldown(0L);
			currentOwnerData.getHellblockData().setOwnerUUID(newOwnerData.getUUID());

			// --- Location Data Transfer ---
			Map<String, Map<String, Integer>> placedBlocksCopy = new HashMap<>();
			currentOwnerData.getLocationCacheData().getPlacedBlocks()
					.forEach((k, v) -> placedBlocksCopy.put(k, new HashMap<>(v)));
			newOwnerData.getLocationCacheData().setPlacedBlocks(placedBlocksCopy);
			currentOwnerData.getLocationCacheData().setPlacedBlocks(new HashMap<>());

			Map<Integer, List<String>> pistonLocationsCopy = new HashMap<>();
			currentOwnerData.getLocationCacheData().getPistonLocationsByIsland()
					.forEach((k, v) -> pistonLocationsCopy.put(k, new ArrayList<>(v)));
			newOwnerData.getLocationCacheData().setPistonLocationsByIsland(pistonLocationsCopy);
			currentOwnerData.getLocationCacheData().setPistonLocationsByIsland(new HashMap<>());

			// --- Post-processing ---
			instance.getHellblockHandler().invalidateHellblockIDCache();
			instance.getUpgradeManager().revalidateUpgradeCache(currentIslandId, newOwnerData.getHellblockData());
			invalidateCachedOwnerData(currentOwnerData.getUUID());
			validateCachedOwnerData(newOwnerData.getUUID());
			instance.getHopperHandler().invalidateHopperWarningCache(currentIslandId);

			replaceNonOwnerUUID(currentOwnerData.getUUID(), newOwnerData.getUUID());

			// --- MESSAGES ---
			instance.debug("transferOwnershipOfHellblock: Sending messages for ownership transfer (forced: %s)..."
					.formatted(forcedByAdmin));
			if (forcedByAdmin) {
				if (currentOwnerAudience != null) {
					send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_LOST,
							AdventureHelper.miniMessageToComponent(newOwnerData.getName()));
				} else {
					instance.getMailboxManager().queue(currentOwnerData.getUUID(),
							new MailboxEntry("message.hellblock.coop.owner.transfer.offline",
									List.of(AdventureHelper.miniMessageToComponent(newOwnerData.getName())),
									Set.of(MailboxFlag.NOTIFY_OWNER)));
				}
				if (newOwnerAudience != null) {
					send(newOwnerAudience, MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_GAINED,
							AdventureHelper.miniMessageToComponent(currentOwnerData.getName()));
				} else {
					instance.getMailboxManager().queue(newOwnerData.getUUID(),
							new MailboxEntry("message.hellblock.coop.owner.transfer.offline",
									List.of(AdventureHelper.miniMessageToComponent(currentOwnerData.getName())),
									Set.of(MailboxFlag.NOTIFY_OWNER)));
				}
			} else {
				send(currentOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_NEW_OWNER_SET,
						AdventureHelper.miniMessageToComponent(newOwnerData.getName()));
				if (newOwnerPlayer != null) {
					send(newOwnerAudience, MessageConstants.MSG_HELLBLOCK_COOP_OWNER_TRANSFER_SUCCESS,
							AdventureHelper.miniMessageToComponent(currentOwnerData.getName()));
				} else {
					instance.getMailboxManager().queue(newOwnerData.getUUID(),
							new MailboxEntry("message.hellblock.coop.owner.transfer.offline",
									List.of(AdventureHelper.miniMessageToComponent(currentOwnerData.getName())),
									Set.of(MailboxFlag.NOTIFY_OWNER)));
				}
			}

			// --- Notify Party ---
			Set<UUID> party = newOwnerData.getHellblockData().getPartyMembers();
			party.stream().map(Bukkit::getPlayer).forEach(member -> {
				if (member != null && member.isOnline()) {
					Sender sender = instance.getSenderFactory().wrap(member);
					sender.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_OWNER_TRANSFER_NOTIFY_PARTY
									.arguments(AdventureHelper.miniMessageToComponent(newOwnerData.getName()))
									.build()));
				} else {
					instance.getMailboxManager().queue(member.getUniqueId(),
							new MailboxEntry("message.hellblock.coop.owner.transfer.party",
									List.of(AdventureHelper.miniMessageToComponent(newOwnerData.getName())),
									Set.of(MailboxFlag.NOTIFY_PARTY)));
				}
			});

			EventUtils.fireAndForget(new CoopOwnershipTransferEvent(currentOwnerData, newOwnerData, forcedByAdmin));

			instance.debug("transferOwnershipOfHellblock: Ownership transfer complete from %s to %s"
					.formatted(currentOwnerData.getName(), newOwnerData.getName()));
			return true;
		}).exceptionally(ex -> {
			instance.getPluginLogger()
					.severe("transferOwnershipOfHellblock: Ownership transfer failed: " + ex.getMessage(), ex);
			return false;
		});
	}

	/**
	 * Returns a list of online players currently on the island owned by the given
	 * UUID.
	 * 
	 * @param ownerId UUID of the island owner
	 * @return List of online {@link Player} objects within the owner's island
	 *         bounds
	 */
	@NotNull
	public List<Player> getIslandOnlinePlayers(@NotNull UUID ownerId) {
		Optional<UserData> userData = instance.getStorageManager().getCachedUserData(ownerId);
		if (userData.isEmpty()) {
			return List.of();
		}

		HellblockData data = userData.get().getHellblockData();
		BoundingBox box = data.getBoundingBox();
		if (box == null) {
			return List.of();
		}

		List<Player> players = instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer)
				.filter(Objects::nonNull).filter(Player::isOnline).filter(p -> box.contains(p.getLocation().toVector()))
				.toList();

		instance.debug("Found " + players.size() + " online player" + (players.size() == 1 ? "" : "s")
				+ " inside island for ownerId: " + ownerId);
		return players;
	}

	/**
	 * Gets a set of UUIDs representing visitors on the island, excluding the owner
	 * and coop members.
	 * 
	 * @param ownerId UUID of the island owner
	 * @return CompletableFuture containing set of UUIDs of non-coop online visitors
	 */
	@NotNull
	public CompletableFuture<Set<UUID>> getVisitors(@NotNull UUID ownerId) {
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false)
				.thenCombine(getAllCoopMembers(ownerId), (optData, coopMembers) -> {
					if (optData.isEmpty()) {
						return Set.of();
					}

					final UserData userData = optData.get();
					final HellblockData data = userData.getHellblockData();
					final BoundingBox bounds = data.getBoundingBox();
					final Location hellblockLocation = data.getHellblockLocation();

					if (bounds == null || hellblockLocation == null || hellblockLocation.getWorld() == null) {
						return Set.of();
					}

					final World world = hellblockLocation.getWorld();

					// Track online visitors
					Set<UUID> onlineVisitors = instance.getStorageManager().getOnlineUsers().stream()
							.filter(UserData::isOnline).map(UserData::getPlayer).filter(Objects::nonNull)
							.filter(p -> !coopMembers.contains(p.getUniqueId()))
							.filter(p -> p.getWorld().getUID().equals(world.getUID()))
							.filter(p -> bounds.contains(p.getLocation().toVector())).map(Player::getUniqueId)
							.collect(Collectors.toSet());

					// Include offline visitors not in the party and not already counted as online
					Set<UUID> allVisitors = new HashSet<>(onlineVisitors);
					for (UUID offlineVisitor : data.getOfflineVisitors()) {
						if (!coopMembers.contains(offlineVisitor) && !onlineVisitors.contains(offlineVisitor)) {
							allVisitors.add(offlineVisitor);
						}
					}

					instance.debug("getVisitors: Found " + allVisitors.size() + " visitor"
							+ (allVisitors.size() == 1 ? "" : "s") + " (online + offline) on island for ownerId: "
							+ ownerId);

					return allVisitors;
				});
	}

	/**
	 * Gets the UUID of the island owner whose island the player is currently
	 * visiting.
	 * 
	 * @param visitor Player to check
	 * @return CompletableFuture containing the UUID of the owner, or null if not on
	 *         any island
	 */
	@Nullable
	public CompletableFuture<UUID> getHellblockOwnerOfVisitingIsland(@NotNull Player visitor) {
		Location currentLocation = visitor.getLocation();
		if (currentLocation == null) {
			return CompletableFuture.completedFuture(null);
		}

		World world = currentLocation.getWorld();
		if (world == null || !instance.getHellblockHandler().isInCorrectWorld(world)) {
			return CompletableFuture.completedFuture(null);
		}

		// Quick skip if too soon or moved insignificantly
		UUID visitorId = visitor.getUniqueId();
		Location lastChecked = lastVisitorCheckLocation.get(visitorId);
		Long lastCheckTime = lastVisitorCheckTime.get(visitorId);
		long now = System.currentTimeMillis();

		boolean tooSoon = lastCheckTime != null && now - lastCheckTime < CHECK_COOLDOWN_MS;
		boolean tooClose = lastChecked != null && lastChecked.getWorld() != null
				&& lastChecked.getWorld().getUID().equals(world.getUID())
				&& lastChecked.distanceSquared(currentLocation) < (MIN_DISTANCE_FOR_RECHECK * MIN_DISTANCE_FOR_RECHECK);

		if (tooSoon || tooClose) {
			return CompletableFuture.completedFuture(null); // skip redundant check
		}

		// Update last check caches
		lastVisitorCheckLocation.put(visitorId, currentLocation.clone());
		lastVisitorCheckTime.put(visitorId, now);

		instance.debug("Checking ownership of visitor " + visitor.getName() + "'s current location at " + "[world="
				+ world.getName() + ", x=" + currentLocation.getBlockX() + ", y=" + currentLocation.getBlockY() + ", z="
				+ currentLocation.getBlockZ() + "]");

		// Reuse the unified method — no duplication
		return getHellblockOwner(currentLocation).thenApply(ownerUUID -> {
			debugVisitorResolutionIfChanged(visitorId, ownerUUID);
			return ownerUUID;
		});
	}

	private void debugVisitorResolutionIfChanged(@NotNull UUID visitorId, @Nullable UUID newOwnerId) {
		boolean hadPreviousEntry = lastVisitorIslandOwner.containsKey(visitorId);
		UUID previousOwner = lastVisitorIslandOwner.get(visitorId);
		if (!hadPreviousEntry || !Objects.equals(previousOwner, newOwnerId)) {
			if (newOwnerId != null) {
				lastVisitorIslandOwner.put(visitorId, newOwnerId);
				instance.debug("Visitor " + visitorId + " is now in island owned by: " + newOwnerId);
			} else {
				lastVisitorIslandOwner.remove(visitorId);
				instance.debug("Visitor " + visitorId + " is not in any island");
			}
		}
	}

	@Nullable
	private CompletableFuture<UUID> getHellblockOwner(@NotNull Object target) {
		Location location;
		HellblockWorld<?> world = null;

		// --- Normalize input into (Location, HellblockWorld) ---
		if (target instanceof Block block) {
			location = block.getLocation();
			world = instance.getWorldManager().getWorld(block.getWorld()).orElse(null);
		} else if (target instanceof Location loc) {
			location = loc;
			if (loc.getWorld() == null)
				return CompletableFuture.completedFuture(null);
			world = instance.getWorldManager().getWorld(loc.getWorld()).orElse(null);
		} else {
			throw new IllegalArgumentException("Unsupported type for getHellblockOwner: " + target.getClass());
		}

		if (world == null || !instance.getHellblockHandler().isInCorrectWorld(world.bukkitWorld())) {
			return CompletableFuture.completedFuture(null);
		}

		final PosWithWorld key = PosWithWorld.from(location);
		final long now = System.currentTimeMillis();

		// --- Owner cache check ---
		OwnerCache cached = ownerCache.get(key);
		if (cached != null && now - cached.time < OWNERSHIP_CHECK_COOLDOWN_MS) {
			UUID cachedOwner = cached.owner.equals(NO_OWNER) ? null : cached.owner;
			if (canDebug("skip_check", key, 3000))
				instance.debug("Skipping async ownership check for " + key + " (cached owner: " + cachedOwner + ")");
			return CompletableFuture.completedFuture(cachedOwner);
		}

		// --- Load bounding boxes for geometric filtering ---
		Map<Integer, BoundingBox> islandBoxes = instance.getPlacementDetector().getCachedBoundingBoxes();
		if (islandBoxes == null || islandBoxes.isEmpty())
			return CompletableFuture.completedFuture(null);

		// --- Begin async owner resolution ---
		return getCachedIslandOwners().thenCompose(owners -> {
			if (owners == null || owners.isEmpty())
				return CompletableFuture.completedFuture(null);

			List<CompletableFuture<UUID>> futures = new ArrayList<>();

			for (UUID ownerUUID : owners) {
				if (shouldSkip(ownerUUID))
					continue;

				// --- Proximity skip check ---
				ProximityCache prox = proximityCache.get(ownerUUID);
				if (prox != null && now - prox.time < OWNERSHIP_CHECK_COOLDOWN_MS
						&& prox.loc.getWorld().getUID().equals(location.getWorld().getUID())
						&& prox.loc.distanceSquared(location) < OWNERSHIP_CHECK_DISTANCE_SQUARED) {
					continue;
				}
				proximityCache.put(ownerUUID, new ProximityCache(location.clone(), now));

				// --- Async user data + bounding box check ---
				CompletableFuture<UUID> check = instance.getStorageManager()
						.getCachedUserDataWithFallback(ownerUUID, false).thenApply(optData -> {
							if (optData.isEmpty())
								return null;

							HellblockData data = optData.get().getHellblockData();
							BoundingBox bounds = islandBoxes.get(data.getIslandId());
							if (bounds == null)
								return null;

							boolean inside = bounds.clone().expand(0.01).contains(location.toVector());
							if (inside) {
								if (canDebug("resolved_match", key, 10000))
									instance.debug("Resolved " + key + " → " + ownerUUID + " (islandId="
											+ data.getIslandId() + ")");
								return ownerUUID;
							} else {
								if (canDebug("bounds_miss", key, 10000))
									instance.debug("Owner " + ownerUUID + " bounds miss at " + key);
								return null;
							}
						});
				futures.add(check);
			}

			// --- Only log once if we're actually checking ---
			if (!futures.isEmpty()) {
				if (canDebug("checking_count", key, 3000)) {
					instance.debug("Checking ownership against " + futures.size() + " active island owner"
							+ (futures.size() == 1 ? "" : "s"));
				}
			}

			if (futures.isEmpty()) {
				ownerCache.put(key, new OwnerCache(NO_OWNER, now));
				if (canDebug("no_checks", key, 5000))
					instance.debug("Skipped ownership check for " + key + " — no valid owners to evaluate.");
				return CompletableFuture.completedFuture(null);
			}

			// --- Await and resolve ---
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
				UUID result = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).findFirst()
						.orElse(null);

				ownerCache.put(key, new OwnerCache(result == null ? NO_OWNER : result, now));

				if (result != null) {
					if (canDebug("resolved_owner", key, 5000))
						instance.debug("Resolved island owner: " + result + " for " + key);
				} else {
					if (canDebug("no_owner", key, 10000))
						instance.debug("Couldn't resolve to an island owner for " + key);
				}

				return result;
			});
		});
	}

	/**
	 * Asynchronously determines the owner UUID of a given block position within a
	 * Hellblock world.
	 * <p>
	 * This method iterates through all known island owners and checks if the
	 * specified {@link Pos3} position exists in their protected block set (as
	 * managed by the {@code ProtectionManager}).
	 * <p>
	 * This is useful for resolving ownership of {@link CustomBlockState}-based
	 * blocks, where no native Bukkit block is available.
	 *
	 * @param pos   the position of the block in question
	 * @param world the {@link HellblockWorld} in which the block exists
	 * @return a {@link CompletableFuture} resolving to the owning {@link UUID}, or
	 *         {@code null} if no owner is found
	 */
	@Nullable
	public CompletableFuture<UUID> getHellblockOwnerOfBlock(@NotNull Pos3 pos, @NotNull HellblockWorld<?> world) {
		if (world.bukkitWorld() == null || !instance.getHellblockHandler().isInCorrectWorld(world.bukkitWorld())) {
			return CompletableFuture.completedFuture(null);
		}

		Location location = pos.toLocation(world.bukkitWorld());
		return getHellblockOwner(location);
	}

	/**
	 * Gets the UUID of the island owner for a specific block.
	 * 
	 * @param block Block to check
	 * @return CompletableFuture containing owner UUID if the block belongs to an
	 *         island, otherwise null
	 */
	@Nullable
	public CompletableFuture<UUID> getHellblockOwnerOfBlock(@NotNull Block block) {
		return getHellblockOwner(block);
	}

	/**
	 * Gets the UUID of the island owner based on a location.
	 * 
	 * @param location Location to check
	 * @return CompletableFuture containing the owner's UUID or null if none found
	 */
	@Nullable
	public CompletableFuture<UUID> getHellblockOwnerOfLocation(@NotNull Location location) {
		return getHellblockOwner(location);
	}

	/**
	 * Determines if the given owner should be skipped due to ongoing operations.
	 */
	private boolean shouldSkip(@NotNull UUID ownerUUID) {
		return instance.getIslandGenerator().isAnimating(ownerUUID)
				|| instance.getHellblockHandler().creationProcessing(ownerUUID)
				|| instance.getHellblockHandler().resetProcessing(ownerUUID)
				|| instance.getIslandGenerator().isGenerating(ownerUUID)
				|| instance.getIslandChoiceGUIManager().isGeneratingIsland(ownerUUID)
				|| instance.getSchematicGUIManager().isGeneratingSchematic(ownerUUID);
	}

	/**
	 * Prevents excessive debug spam for the same region within a short interval.
	 */
	private boolean canDebug(String category, Object key, long cooldownMs) {
		String composite = category + ":" + key;
		long now = System.currentTimeMillis();
		Long last = debugCooldowns.get(composite);
		if (last != null && now - last < cooldownMs)
			return false;
		debugCooldowns.put(composite, now);
		return true;
	}

	/**
	 * Gets a cached set of all island owners. Refreshes from storage if the cache
	 * is expired.
	 * 
	 * @return CompletableFuture containing a collection of all island owner UUIDs
	 */
	@NotNull
	public CompletableFuture<Collection<UUID>> getCachedIslandOwners() {
		long now = System.currentTimeMillis();

		if (now - lastOwnersRefresh < OWNERS_CACHE_TTL && !cachedIslandOwners.isEmpty()) {
			if (cachedIslandOwners.size() != lastDebuggedOwnerCount) {
				lastDebuggedOwnerCount = cachedIslandOwners.size();
				if (cachedIslandOwners.isEmpty()) {
					instance.debug("Cache hit for island owners: none found in cache.");
				} else {
					instance.debug("Cache hit for island owners. Count: " + cachedIslandOwners.size() + ".");
				}
			}
			return CompletableFuture.completedFuture(cachedIslandOwners);
		}

		synchronized (this) {
			if (ongoingRefresh != null) {
				return ongoingRefresh.thenApply(Function.identity());
			}

			CompletableFuture<Set<UUID>> internalFuture = getAllIslandOwners().thenApply(owners -> {
				cachedIslandOwners = new HashSet<>(owners);
				lastOwnersRefresh = System.currentTimeMillis();

				boolean changed = owners.size() != lastDebuggedOwnerCount;
				lastDebuggedOwnerCount = owners.size();

				if (changed || cachedIslandOwners.isEmpty()) {
					if (owners.isEmpty()) {
						instance.debug("Fetched and cached no new island owners.");
					} else {
						instance.debug("Fetched and cached " + owners.size() + " new island owner"
								+ (owners.size() == 1 ? "" : "s") + ".");
					}
				}

				return cachedIslandOwners;
			}).whenComplete((result, ex) -> ongoingRefresh = null);

			ongoingRefresh = internalFuture;

			return internalFuture.thenApply(Function.identity());
		}
	}

	/**
	 * Gets the {@link UserData} of the island owner associated with a given island
	 * ID.
	 * 
	 * @param islandId ID of the island
	 * @return CompletableFuture containing an Optional of the owner's UserData
	 */
	@NotNull
	public CompletableFuture<Optional<UserData>> getOwnerUserDataByIslandId(int islandId) {
		return getCachedIslandOwnerData().thenApply(allOwners -> {
			Optional<UserData> result = allOwners.stream().filter(Objects::nonNull)
					.filter(user -> user.getHellblockData().getIslandId() == islandId).findFirst();
			instance.debug("Owner matched for islandId " + islandId + ": "
					+ result.map(data -> data.getHellblockData().getOwnerUUID()).orElse(null));
			return result;
		});
	}

	/**
	 * Retrieves a cached list of all island owner {@link UserData} instances.
	 * 
	 * @return CompletableFuture with a list of UserData of all island owners
	 */
	@NotNull
	public CompletableFuture<List<UserData>> getCachedIslandOwnerData() {
		return getCachedIslandOwners().thenCompose(ownerUUIDs -> {
			List<CompletableFuture<Optional<UserData>>> futures = ownerUUIDs.stream()
					.map(uuid -> instance.getStorageManager().getCachedUserDataWithFallback(uuid, false)).toList();
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
				List<UserData> result = futures.stream().map(CompletableFuture::join).filter(Optional::isPresent)
						.map(Optional::get).toList();
				return result;
			});
		});
	}

	/**
	 * Fetches all true island owners by checking the data source and filtering
	 * accordingly.
	 * 
	 * @return CompletableFuture containing a collection of all true island owner
	 *         UUIDs
	 */
	@NotNull
	public CompletableFuture<Collection<UUID>> getAllIslandOwners() {
		final Set<UUID> allUsers = instance.getStorageManager().getDataSource().getUniqueUsers();

		if (allUsers.isEmpty()) {
			return CompletableFuture.completedFuture(Collections.emptySet());
		}

		final List<CompletableFuture<UUID>> futures = new ArrayList<>();
		for (UUID uuid : allUsers) {
			// Skip if already known as non-owner
			if (nonOwnersCache.contains(uuid)) {
				continue;
			}

			final CompletableFuture<UUID> ownerFuture = instance.getStorageManager()
					.getCachedUserDataWithFallback(uuid, false).thenApply(optData -> {
						if (optData.isEmpty()) {
							nonOwnersCache.add(uuid); // Cache even failed loads
							return null;
						}

						final UserData userData = optData.get();
						final UUID ownerUUID = userData.getHellblockData().getOwnerUUID();

						boolean isOwner = ownerUUID != null && userData.getHellblockData().hasHellblock()
								&& uuid.equals(ownerUUID);

						if (!isOwner) {
							instance.debug("User: " + uuid
									+ " is NOT an island owner — caching and skipping in future checks.");
							nonOwnersCache.add(uuid); // Cache non-owner
						} else {
							instance.debug("User: " + uuid + " is an island owner.");
						}

						return isOwner ? uuid : null;
					});
			futures.add(ownerFuture);
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
			Set<UUID> owners = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull)
					.collect(Collectors.toSet());

			int currentOwnerCount = owners.size();
			long now = System.currentTimeMillis();

			if (currentOwnerCount != lastResolvedOwnerCount) {
				instance.debug(
						"Resolved " + currentOwnerCount + " true island owner" + (currentOwnerCount == 1 ? "" : "s"));
				lastResolvedOwnerCount = currentOwnerCount;
				lastOwnerCountChangeTime = now;
				loggedStableOwnerCount = false; // reset logging state
			} else {
				long timeSinceLastChange = now - lastOwnerCountChangeTime;

				if (!loggedStableOwnerCount && timeSinceLastChange >= TimeUnit.MINUTES.toMillis(5)) {
					instance.debug("Island owner count has remained at " + currentOwnerCount + " for 5+ minutes.");
					loggedStableOwnerCount = true; // only log once per stable period
				}
			}
			return owners;
		});
	}

	/**
	 * Gets all coop members (and the owner) of the specified island owner.
	 * 
	 * @param ownerId UUID of the island owner
	 * @return CompletableFuture with a collection of UUIDs representing the party
	 */
	@NotNull
	public CompletableFuture<Collection<UUID>> getAllCoopMembers(@NotNull UUID ownerId) {
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenApply(optData -> {
			if (optData.isEmpty()) {
				return Collections.emptySet();
			}

			final UserData userData = optData.get();
			final HellblockData hellblockData = userData.getHellblockData();

			final Set<UUID> members = hellblockData.getPartyPlusOwner();
			instance.debug("Found " + members.size() + " party member" + (members.size() == 1 ? "" : "s")
					+ " for ownerId: " + ownerId);
			return members;
		});
	}

	/**
	 * Attempts to find the island owner at the given location using cached user
	 * data. This is a fast, synchronous operation that does not hit the database.
	 *
	 * @param location The location to check.
	 * @return Optional containing the UserData of the island owner, or empty if
	 *         none found.
	 */
	@NotNull
	public Optional<UserData> getIslandOwnerAt(@NotNull Location location) {
		final World world = location.getWorld();
		if (world == null || !instance.getHellblockHandler().isInCorrectWorld(world)) {
			return Optional.empty();
		}

		for (UUID ownerUUID : cachedIslandOwners) {
			if (shouldSkip(ownerUUID)) {
				continue;
			}

			Optional<UserData> ownerDataOpt = instance.getStorageManager().getCachedUserData(ownerUUID);
			if (ownerDataOpt.isEmpty()) {
				continue;
			}

			UserData ownerData = ownerDataOpt.get();
			HellblockData hellblockData = ownerData.getHellblockData();

			String formattedWorldName = instance.getWorldManager().getHellblockWorldFormat(hellblockData.getIslandId());
			Optional<HellblockWorld<?>> worldWrapperOpt = instance.getWorldManager().getWorld(formattedWorldName);

			if (worldWrapperOpt.isEmpty() || worldWrapperOpt.get().bukkitWorld() == null) {
				continue;
			}

			UUID expectedWorldId = worldWrapperOpt.get().bukkitWorld().getUID();
			if (!world.getUID().equals(expectedWorldId)) {
				continue;
			}

			BoundingBox bounds = hellblockData.getBoundingBox();
			if (bounds != null && bounds.contains(location.toVector())) {
				instance.debug("Found matching owner: " + ownerUUID + " at location: " + location);
				return Optional.of(ownerData);
			}
		}

		instance.debug("No island owner found at location: " + location);
		return Optional.empty();
	}

	/**
	 * Attempts to resolve the {@link UserData} of the island owner based on a given
	 * world name.
	 * <p>
	 * This method is intended for plugin-managed worlds that follow the naming
	 * convention {@code hellblock_world_<islandId>}. It extracts the island ID from
	 * the world name and matches it against the cached island owners'
	 * {@link HellblockData#getIslandId()}.
	 * <p>
	 * This is useful for systems that need to reverse-lookup the owner of a world
	 * during tasks like world purging, validation, or offline management.
	 *
	 * @param worldName the name of the world (e.g. {@code hellblock_world_123})
	 * @return an {@link Optional} containing the {@link UserData} of the matching
	 *         owner, or {@link Optional#empty()} if not found or if the name is
	 *         invalid
	 */
	@NotNull
	public Optional<UserData> getCachedIslandOwnerDataNow(@NotNull String worldName) {
		if (!worldName.startsWith(WorldManager.WORLD_PREFIX)) {
			return Optional.empty();
		}

		// Extract the island ID from the world name
		String idPart = worldName.substring(WorldManager.WORLD_PREFIX.length());
		int islandId;
		try {
			islandId = Integer.parseInt(idPart);
		} catch (NumberFormatException e) {
			instance.getPluginLogger().warn("Invalid world name format for island: " + worldName);
			return Optional.empty();
		}

		// Search the cache
		for (UUID ownerUUID : cachedIslandOwners) {
			Optional<UserData> userDataOpt = instance.getStorageManager().getCachedUserData(ownerUUID);
			if (userDataOpt.isEmpty())
				continue;

			UserData userData = userDataOpt.get();
			HellblockData hellblockData = userData.getHellblockData();

			if (hellblockData.getIslandId() == islandId) {
				return Optional.of(userData);
			}
		}

		return Optional.empty();
	}

	/**
	 * Checks whether a given player is part of the specified island (owner or coop
	 * member).
	 *
	 * @param ownerId  UUID of the island owner.
	 * @param playerId UUID of the player to check.
	 * @return True if the player is part of the island (owner or coop), false
	 *         otherwise.
	 */
	public boolean isIslandMember(@NotNull UUID ownerId, @NotNull UUID playerId) {
		if (ownerId.equals(playerId)) {
			instance.debug("Player is the owner, instant return as coop member");
			return true;
		}

		Optional<UserData> ownerData = instance.getStorageManager().getCachedUserData(ownerId);
		if (ownerData.isEmpty()) {
			return false;
		}

		HellblockData islandData = ownerData.get().getHellblockData();
		Set<UUID> coopMembers = islandData.getPartyPlusOwner();
		boolean isMember = coopMembers.contains(playerId);
		instance.debug("Player is " + (isMember ? "" : "not ") + "a coop member");
		return isMember;
	}

	/**
	 * Gets the island owner UUID for the specified player.
	 *
	 * @param playerId UUID of the player whose island owner is to be retrieved.
	 * @return CompletableFuture containing an Optional with the owner's UUID if
	 *         found.
	 */
	@Nullable
	public CompletableFuture<Optional<UUID>> getIslandOwner(@NotNull UUID playerId) {
		return instance.getStorageManager().getCachedUserDataWithFallback(playerId, false).thenApply(optData -> {
			if (optData.isEmpty()) {
				return Optional.empty();
			}

			final UserData userData = optData.get();
			final HellblockData hellblockData = userData.getHellblockData();
			UUID ownerUUID = hellblockData.getOwnerUUID();
			if (ownerUUID != null)
				instance.debug(
						"Island owner for player " + playerId + " is " + (ownerUUID != null ? ownerUUID : "null"));
			return Optional.ofNullable(ownerUUID);
		});
	}

	/**
	 * Checks whether the specified player is allowed to visit the island owned by
	 * the given UUID.
	 *
	 * @param visitor Player to check.
	 * @param ownerId UUID of the island owner.
	 * @return CompletableFuture containing true if the player is allowed, false
	 *         otherwise.
	 */
	@NotNull
	public CompletableFuture<Boolean> checkIfVisitorsAreWelcome(@NotNull Player visitor, @NotNull UUID ownerId) {
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenApply(optData -> {
			if (optData.isEmpty()) {
				return false;
			}

			final UserData userData = optData.get();
			final HellblockData data = userData.getHellblockData();

			boolean allowed = !data.isLocked()
					|| (data.getOwnerUUID() != null && Objects.equals(data.getOwnerUUID(), visitor.getUniqueId()))
					|| data.getPartyMembers().contains(visitor.getUniqueId())
					|| data.getTrustedMembers().contains(visitor.getUniqueId())
					|| visitor.hasPermission("hellblock.bypass.lock") || visitor.hasPermission("hellblock.admin")
					|| visitor.isOp();

			instance.debug(
					"Visitor permission for player " + visitor.getName() + " to island of " + ownerId + ": " + allowed);
			return allowed;
		});
	}

	/**
	 * Kicks all online visitors from the island if it is locked.
	 *
	 * @param ownerId UUID of the island owner.
	 * @return CompletableFuture that completes when all visitors have been checked
	 *         and possibly kicked.
	 */
	public CompletableFuture<Void> kickVisitorsIfLocked(@NotNull UUID ownerId) {
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).handle((optData, ex) -> {
			if (ex != null) {
				instance.getPluginLogger()
						.warn("Failed to load owner data for kickVisitorsIfLocked: " + ex.getMessage(), ex);
				return Optional.<UserData>empty();
			}
			return optData;
		}).thenCompose(optData -> {
			if (optData.isEmpty()) {
				return CompletableFuture.completedFuture(null);
			}

			final UserData ownerData = optData.get();
			final HellblockData data = ownerData.getHellblockData();

			if (data.getOwnerUUID() != null && !data.isOwner(data.getOwnerUUID())) {
				instance.debug("User is not the actual island owner: " + ownerId);
				return CompletableFuture.completedFuture(null);
			}

			boolean wasLocked = lastLockedStateCache.getOrDefault(ownerId, false);
			boolean isLocked = data.isLocked();
			lastLockedStateCache.put(ownerId, isLocked);

			if (!isLocked) {
				if (wasLocked) {
					instance.debug("Island is no longer locked; no visitors will be kicked");
				}
				return CompletableFuture.completedFuture(null);
			}

			return getVisitors(ownerData.getUUID()).thenCompose(visitors -> {
				final int visitorCount = visitors.size();
				final int previousCount = lastVisitorCountCache.getOrDefault(ownerId, -1);
				if (visitorCount != previousCount) {
					instance.debug("Found " + visitorCount + " visitor" + (visitorCount == 1 ? "" : "s")
							+ " on locked island of owner: " + ownerData.getUUID());
					lastVisitorCountCache.put(ownerId, visitorCount);
				}

				AtomicInteger kicked = new AtomicInteger(0);
				AtomicInteger allowed = new AtomicInteger(0);
				AtomicInteger skipped = new AtomicInteger(0);

				List<CompletableFuture<Boolean>> tasks = new ArrayList<>();

				for (UUID visitor : visitors) {
					Optional<UserData> dataOpt = instance.getStorageManager().getOnlineUser(visitor);
					if (dataOpt.isEmpty() || !dataOpt.get().isOnline() || dataOpt.get().getPlayer() == null) {
						skipped.incrementAndGet();
						continue;
					}

					final UserData userData = dataOpt.get();
					final Player player = userData.getPlayer();

					CompletableFuture<Boolean> task = checkIfVisitorsAreWelcome(player, ownerData.getUUID())
							.thenCompose(status -> {
								if (!status) {
									return handleVisitorKick(userData).thenApply(result -> {
										if (result) {
											kicked.incrementAndGet();
										}
										return result;
									});
								} else {
									allowed.incrementAndGet();
									return CompletableFuture.completedFuture(false);
								}
							});
					tasks.add(task);
				}

				return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).thenRun(() -> {
					instance.debug("Visitor enforcement summary for islandId=" + data.getIslandId() + " — Kicked: "
							+ kicked.get() + ", Bypassed: " + allowed.get() + ", Skipped (offline/invalid): "
							+ skipped.get());
				});
			});
		});
	}

	/**
	 * Handles kicking a visitor from a locked island. If the visitor is on their
	 * own island, they are sent to a safe location. Otherwise, they are teleported
	 * to spawn.
	 *
	 * @param visitorData The user data of the visitor to kick.
	 */
	private CompletableFuture<Boolean> handleVisitorKick(@NotNull UserData visitorData) {
		if (visitorData.getHellblockData().hasHellblock()) {
			final UUID ownerUUID = visitorData.getHellblockData().getOwnerUUID();
			if (ownerUUID == null) {
				instance.getPluginLogger().severe("Hellblock owner UUID was null for player " + visitorData.getName()
						+ " (" + visitorData.getUUID() + "). This indicates corrupted data.");
				return CompletableFuture.failedFuture(new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer."));
			}

			instance.debug("Visitor has a hellblock, retrieving owner's UserData: " + ownerUUID);

			return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optData -> {
				if (optData.isEmpty()) {
					instance.debug("Owner data not found for: " + ownerUUID);
					return CompletableFuture.completedFuture(false);
				}

				final UserData ownerData = optData.get();

				// Await the result of makeHomeLocationSafe
				return makeHomeLocationSafe(ownerData, visitorData).thenCompose(result -> {
					switch (result) {
					case ALREADY_SAFE:
						instance.debug(
								"Visitor's home is already safe. Teleporting manually: " + visitorData.getName());
						return instance.getHellblockHandler().teleportPlayerToHome(visitorData,
								ownerData.getHellblockData().getHomeLocation());
					case FIXED_AND_TELEPORTED:
						instance.debug("Home was fixed and visitor already teleported: " + visitorData.getName());
						return CompletableFuture.completedFuture(true);
					case FAILED_TO_FIX:
						instance.getPluginLogger().warn("Failed to fix home location for visitor: "
								+ visitorData.getName() + " on island of " + ownerUUID);
						Optional<Player> playerOpt = requireOnline(visitorData);
						if (playerOpt.isEmpty()) {
							return CompletableFuture.completedFuture(false);
						}
						Player player = playerOpt.get();
						return CompletableFuture
								.completedFuture(instance.getHellblockHandler().teleportToSpawn(player, true));
					}
					return CompletableFuture.completedFuture(false); // Fallback safety
				});
			}).handle((res, ex) -> {
				if (ex != null) {
					instance.getPluginLogger().warn("Failed to handle visitor kick for " + visitorData.getName(), ex);
				}
				return instance.getStorageManager().unlockUserData(ownerUUID).thenApply(ignored -> res != null && res);
			}).thenCompose(Function.identity()).thenApply(success -> {
				instance.getSenderFactory().wrap(visitorData.getPlayer()).sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LOCKED_ENTRY.build()));
				return success;
			});
		} else {
			instance.debug("Visitor has no hellblock, teleporting to spawn");
			requireOnline(visitorData)
					.ifPresent(player -> instance.getHellblockHandler().teleportToSpawn(player, true));

			instance.getSenderFactory().wrap(visitorData.getPlayer()).sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LOCKED_ENTRY.build()));

			return CompletableFuture.completedFuture(true);
		}
	}

	/**
	 * Attempts to add a player to the trusted access list of an island.
	 *
	 * @param ownerData The UserData of the island owner.
	 * @param input     The player name input for feedback messages.
	 * @param memberId  UUID of the player to trust.
	 * @return CompletableFuture that completes with true if added successfully,
	 *         false otherwise.
	 */
	@NotNull
	public CompletableFuture<Boolean> addTrustAccess(@NotNull UserData ownerData, @NotNull String input,
			@NotNull UUID memberId) {
		instance.debug("addTrustAccess called. Owner: " + ownerData.getName() + ", Trusting: " + memberId);

		Optional<Player> playerOpt = requireOnline(ownerData);
		if (playerOpt.isEmpty()) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("addTrustAccess: Owner must be online to add trust access."));
		}
		final Player player = playerOpt.get();

		final Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(
				instance.getWorldManager().getHellblockWorldFormat(ownerData.getHellblockData().getIslandId()));

		if (world.isEmpty() || world.get().bukkitWorld() == null) {
			return CompletableFuture.failedFuture(new NullPointerException(
					"addTrustAccess: World returned null, please try to regenerate the world before reporting this issue."));
		}

		if (ownerData.getHellblockData().getTrustedMembers().contains(memberId)) {
			instance.debug("addTrustAccess: Player already trusted: " + memberId);
			instance.getSenderFactory().wrap(player).sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_TRUSTED
							.arguments(AdventureHelper.miniMessageToComponent(input)).build()));
			return CompletableFuture.completedFuture(false);
		}

		return instance.getProtectionManager().getIslandProtection()
				.addMemberToHellblockBounds(world.get(), ownerData.getUUID(), memberId).thenApply(added -> {
					if (!added) {
						throw new IllegalStateException("addTrustAccess: Failed to trust player " + input
								+ " under owner " + ownerData.getName());
					}

					instance.debug("addTrustAccess: Adding player to trusted list: " + memberId);
					EventUtils.fireAndForget(new CoopTrustAddEvent(ownerData, memberId));
					return ownerData.getHellblockData().addTrustPermission(memberId);
				});
	}

	/**
	 * Removes a player from the trusted access list of an island.
	 *
	 * @param ownerData The UserData of the island owner.
	 * @param input     The player name input for feedback messages.
	 * @param memberId  UUID of the player to untrust.
	 * @return CompletableFuture that completes with true if removed successfully,
	 *         false otherwise.
	 */
	@NotNull
	public CompletableFuture<Boolean> removeTrustAccess(@NotNull UserData ownerData, @NotNull String input,
			@NotNull UUID memberId) {
		instance.debug(
				"removeTrustAccess called. Owner: " + ownerData.getName() + ", Removing trust from: " + memberId);

		Optional<Player> playerOpt = requireOnline(ownerData);
		if (playerOpt.isEmpty()) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("removeTrustAccess: Owner must be online to remove trust access."));
		}
		final Player player = playerOpt.get();

		final Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(
				instance.getWorldManager().getHellblockWorldFormat(ownerData.getHellblockData().getIslandId()));

		if (world.isEmpty() || world.get().bukkitWorld() == null) {
			return CompletableFuture.failedFuture(new NullPointerException(
					"removeTrustAccess: World returned null, please try to regenerate the world before reporting this issue."));
		}

		if (!ownerData.getHellblockData().getTrustedMembers().contains(memberId)) {
			instance.debug("removeTrustAccess: Player not trusted: " + memberId);
			instance.getSenderFactory().wrap(player)
					.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_TRUSTED
							.arguments(AdventureHelper.miniMessageToComponent(input)).build()));
			return CompletableFuture.completedFuture(false);
		}

		return instance.getProtectionManager().getIslandProtection()
				.removeMemberFromHellblockBounds(world.get(), ownerData.getUUID(), memberId).thenApply(removed -> {
					if (!removed) {
						throw new IllegalStateException("removeTrustAccess: Failed to untrust player " + input
								+ " under owner " + ownerData.getName());
					}

					instance.debug("removeTrustAccess: Removing player from trusted list: " + memberId);
					EventUtils.fireAndForget(new CoopTrustRemoveEvent(ownerData, memberId));
					return ownerData.getHellblockData().removeTrustPermission(memberId);
				});
	}

	/**
	 * Checks if a player is banned from a specific island and is currently inside
	 * the island boundaries.
	 *
	 * @param bannedOwnerId UUID of the island owner.
	 * @param targetId      UUID of the player to check.
	 * @param location      Location of the player.
	 * @return CompletableFuture containing true if the player is banned and inside
	 *         the island.
	 */
	@NotNull
	public CompletableFuture<Boolean> isPlayerBannedInLocation(@NotNull UUID bannedOwnerId, @NotNull UUID targetId,
			@NotNull Location location) {
		return instance.getStorageManager().getCachedUserDataWithFallback(bannedOwnerId, false).thenApply(optData -> {
			if (optData.isEmpty()) {
				return false;
			}

			final UserData ownerData = optData.get();
			final HellblockData data = ownerData.getHellblockData();
			final BoundingBox bounds = data.getBoundingBox();
			final Set<UUID> banned = data.getBannedMembers();

			if (bounds == null) {
				return false;
			}

			final Player target = Bukkit.getPlayer(targetId);
			final boolean isBypassing = target != null && (target.isOp() || target.hasPermission("hellblock.admin")
					|| target.hasPermission("hellblock.bypass.interact"));

			boolean resultFlag = !isBypassing && bounds.contains(location.toVector()) && banned.contains(targetId);

			instance.debug("Player banned: " + banned.contains(targetId) + ", In bounds: "
					+ bounds.contains(location.toVector()) + ", Bypassing: " + isBypassing + " => Result: "
					+ resultFlag);

			return resultFlag;
		});
	}

	/**
	 * Checks if a player is banned from the island they are currently standing in
	 * and teleports them away (to home or spawn) if so. This method handles:
	 * 
	 * <ul>
	 * <li>Verifying if the player is within a banned island</li>
	 * <li>Skipping admin/bypass permissions</li>
	 * <li>Attempting to safely teleport them home</li>
	 * <li>Falling back to spawn if no valid home exists</li>
	 * </ul>
	 *
	 * @param player The player to check and possibly enforce a ban on.
	 */
	private void enforceIslandBanIfNeeded(@NotNull Player player) {
		UUID playerId = player.getUniqueId();
		World world = player.getWorld();

		getCachedIslandOwners().thenAccept(ownerIds -> {
			for (UUID ownerId : ownerIds) {
				instance.debug("Checking island of owner: " + ownerId);

				instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenAccept(optData -> {
					if (optData.isEmpty())
						return;

					UserData userData = optData.get();
					HellblockData hellblockData = userData.getHellblockData();

					if (hellblockData.getBoundingBox() == null || hellblockData.getHellblockLocation() == null
							|| hellblockData.getHellblockLocation().getWorld() == null) {
						return;
					}

					boolean inParty = hellblockData.getPartyPlusOwner().contains(playerId);
					boolean isBanned = hellblockData.getBannedMembers().contains(playerId);
					boolean insideIsland = world.getUID()
							.equals(hellblockData.getHellblockLocation().getWorld().getUID())
							&& hellblockData.getBoundingBox().contains(player.getLocation().toVector());

					EnforcementState currentState = new EnforcementState(ownerId, inParty, isBanned);
					EnforcementState lastState = lastEnforcementState.get(playerId);

					if (!Objects.equals(currentState, lastState)) {
						lastEnforcementState.put(playerId, currentState);
						instance.debug("Enforcement change for " + player.getName() + ": " + "IslandID="
								+ hellblockData.getIslandId() + ", InParty=" + inParty + ", IsBanned=" + isBanned
								+ ", Inside=" + insideIsland);
					}

					if (inParty || player.hasPermission("hellblock.admin")
							|| player.hasPermission("hellblock.bypass.interact") || player.isOp())
						return;

					if (!insideIsland || !isBanned)
						return;

					instance.debug("Banned player " + player.getName() + " found in island of " + userData.getName()
							+ " — initiating removal.");

					Optional<UserData> userDataOpt = instance.getStorageManager().getOnlineUser(playerId);
					if (userDataOpt.isEmpty())
						return;

					UserData bannedUserData = userDataOpt.get();
					HellblockData bannedHellblockData = bannedUserData.getHellblockData();

					if (bannedHellblockData.getHomeLocation() == null) {
						instance.debug("No valid home location for " + player.getName() + ", teleporting to spawn.");
						instance.getScheduler()
								.executeSync(() -> instance.getHellblockHandler().teleportToSpawn(player, true));
						return;
					}

					UUID bannedOwnerUUID = bannedHellblockData.getOwnerUUID();
					if (bannedOwnerUUID == null) {
						instance.getPluginLogger()
								.warn("Island Owner UUID was null for banned player " + player.getName());
						instance.getScheduler()
								.executeSync(() -> instance.getHellblockHandler().teleportToSpawn(player, true));
						return;
					}

					instance.getStorageManager().getCachedUserDataWithFallback(bannedOwnerUUID, true)
							.handle((optOwner, ex) -> {
								if (ex != null) {
									instance.getPluginLogger().warn("Failed to load owner data for banned player "
											+ player.getName() + ": " + ex.getMessage(), ex);
									return instance.getScheduler().callSync(() -> {
										return CompletableFuture.completedFuture(
												instance.getHellblockHandler().teleportToSpawn(player, true));
									});
								}

								if (optOwner.isEmpty()) {
									instance.getPluginLogger()
											.warn("Owner data missing for banned player " + player.getName());
									return instance.getScheduler().callSync(() -> {
										return CompletableFuture.completedFuture(
												instance.getHellblockHandler().teleportToSpawn(player, true));
									});
								}

								UserData bannedOwnerUserData = optOwner.get();
								instance.debug("Attempting to ensure safe home for " + player.getName());

								return instance.getWorldManager()
										.ensureHellblockWorldLoaded(bannedHellblockData.getIslandId())
										.thenCompose(
												loadedWorld -> makeHomeLocationSafe(bannedOwnerUserData, bannedUserData)
														.thenCompose(result -> {
															switch (result) {
															case ALREADY_SAFE:
																return instance.getHellblockHandler()
																		.teleportPlayerToHome(bannedUserData,
																				bannedOwnerUserData.getHellblockData()
																						.getHomeLocation());
															case FIXED_AND_TELEPORTED:
																return CompletableFuture.completedFuture(true);
															case FAILED_TO_FIX:
																instance.getPluginLogger().warn(
																		"Failed to fix home location for banned player: "
																				+ player.getName());
																return instance.getScheduler().callSync(() -> {
																	return CompletableFuture.completedFuture(
																			instance.getHellblockHandler()
																					.teleportToSpawn(player, true));
																});
															}
															return CompletableFuture.completedFuture(false); // fallback
														})
														.thenRun(() -> instance.getScheduler()
																.executeSync(() -> instance.getSenderFactory()
																		.wrap(player)
																		.sendMessage(instance.getTranslationManager()
																				.render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY
																						.build())))))
										.exceptionally(error -> {
											instance.getPluginLogger()
													.warn("Failed to load world or make safe for banned player "
															+ player.getName(), error);
											instance.getScheduler().executeSync(
													() -> instance.getHellblockHandler().teleportToSpawn(player, true));
											return null;
										});
							}).handle((result, ex) -> {
								return instance.getStorageManager().unlockUserData(bannedOwnerUUID).thenRun(() -> {
									if (ex != null) {
										instance.getPluginLogger()
												.warn("Error unlocking data for banned owner " + bannedOwnerUUID, ex);
									}
								});
							}).thenCompose(Function.identity());
				});

				// Only need to check one island — player cannot be in multiple simultaneously
				break;
			}
		});
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		lastEnforcementState.remove(event.getPlayer().getUniqueId());
	}

	/**
	 * Ensures a player's home location is safe for teleportation. If it's unsafe,
	 * attempts to locate and reset the home to the nearest bedrock block.
	 *
	 * @param ownerData  The UserData of the island owner.
	 * @param playerData The UserData of the player being teleported.
	 * @return CompletableFuture that completes with false if no safe location could
	 *         be found, null if already safe, or true if teleport was successful.
	 */
	@NotNull
	public CompletableFuture<SafeHomeResult> makeHomeLocationSafe(@NotNull UserData ownerData,
			@NotNull UserData playerData) {
		Optional<Player> playerOpt = requireOnline(playerData);
		if (playerOpt.isEmpty()) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("Player must be online to make home location safe for teleportation."));
		}
		final Player player = playerOpt.get();

		Location homeLocation = ownerData.getHellblockData().getHomeLocation();
		if (homeLocation == null) {
			throw new IllegalStateException(
					"Home location returned null when attempting to make safe for teleportation.");
		}

		instance.debug("makeHomeLocationSafe: Checking if home location is safe: " + homeLocation);

		return LocationUtils.isSafeLocationAsync(homeLocation, player).thenCompose(safe -> {
			if (safe) {
				instance.debug("makeHomeLocationSafe: Home location is already safe for player: " + player.getName());
				return CompletableFuture.completedFuture(SafeHomeResult.ALREADY_SAFE);
			}

			instance.debug(
					"makeHomeLocationSafe: Home location is unsafe, notifying player and locating nearest bedrock");
			instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
					.render(MessageConstants.MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK.build()));

			return instance.getHellblockHandler().locateNearestBedrock(ownerData).thenCompose(bedrock -> {
				if (bedrock == null) {
					instance.debug("makeHomeLocationSafe: No nearby bedrock found for owner: " + ownerData.getUUID());
					return CompletableFuture.completedFuture(SafeHomeResult.FAILED_TO_FIX);
				}

				instance.debug("makeHomeLocationSafe: Nearest bedrock found: " + Pos3.from(bedrock)
						+ ", updating home and teleporting player: " + player.getName());
				ownerData.getHellblockData().setHomeLocation(bedrock);
				return ChunkUtils.teleportAsync(player, bedrock, TeleportCause.PLUGIN)
						.thenApply(v -> SafeHomeResult.FIXED_AND_TELEPORTED);
			});
		});
	}

	/**
	 * Gets the maximum allowed party size for an island based on upgrade level.
	 *
	 * @param ownerData The UserData of the island owner.
	 * @return The maximum party size allowed.
	 */
	public int getMaxPartySize(@NotNull UserData ownerData) {
		if (!ownerData.getHellblockData().hasHellblock()) {
			instance.debug("User has no hellblock. Returning default party size.");
			return instance.getUpgradeManager().getDefaultValue(IslandUpgradeType.PARTY_SIZE).intValue();
		}

		final UUID ownerUUID = ownerData.getHellblockData().getOwnerUUID();
		if (ownerUUID == null || !ownerUUID.equals(ownerData.getUUID())) {
			instance.debug("User is not the island owner. Returning default party size.");
			return instance.getUpgradeManager().getDefaultValue(IslandUpgradeType.PARTY_SIZE).intValue();
		}

		int currentTier = ownerData.getHellblockData().getUpgradeLevel(IslandUpgradeType.PARTY_SIZE);
		int effectiveValue = instance.getUpgradeManager().getEffectiveValue(currentTier, IslandUpgradeType.PARTY_SIZE)
				.intValue();

		instance.debug("User has upgrade tier " + currentTier + ". Effective party size: " + effectiveValue);
		return effectiveValue;
	}

	/**
	 * Resolves a player to an {@link OfflinePlayer}, prioritizing online state.
	 *
	 * @param playerId UUID of the player to resolve.
	 * @return An {@link OfflinePlayer} instance representing the player.
	 */
	@Nullable
	private OfflinePlayer resolvePlayer(@NotNull UUID playerId) {
		return Optional.ofNullable(Bukkit.getPlayer(playerId)).map(p -> (OfflinePlayer) p)
				.orElse(Bukkit.getOfflinePlayer(playerId));
	}

	/**
	 * Checks if the player is currently online.
	 *
	 * @param userData The user data of the player.
	 * @return Optional containing the online Player, or empty if offline.
	 */
	@Nullable
	private Optional<Player> requireOnline(@NotNull UserData userData) {
		return Optional.ofNullable(userData.getPlayer());
	}

	/**
	 * Gets the Bukkit {@link HellblockWorld} associated with a user's hellblock
	 * island.
	 *
	 * @param userData The UserData of the island owner.
	 * @return Optional containing the Bukkit World, or empty if not found.
	 */
	@NotNull
	private Optional<HellblockWorld<?>> getWorldFor(@NotNull UserData userData) {
		int islandId = userData.getHellblockData().getIslandId();
		return instance.getWorldManager().getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));
	}

	/**
	 * Sends a localized message to the specified audience.
	 *
	 * @param audience The target audience (e.g., player or console).
	 * @param msg      The translatable message builder.
	 * @param args     Additional arguments for the message.
	 */
	private void send(@NotNull Sender audience, @NotNull TranslatableComponent.Builder msg, Component... args) {
		audience.sendMessage(instance.getTranslationManager().render(msg.arguments(args).build()));
	}

	public record PosWithWorld(@NotNull String world, int x, int y, int z) {
		public static PosWithWorld from(@NotNull Location loc) {
			Objects.requireNonNull(loc.getWorld(), "Cannot get position with world of null world");
			return new PosWithWorld(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		}

		public static PosWithWorld from(@NotNull Block block) {
			return from(block.getLocation());
		}

		public static PosWithWorld from(@NotNull Pos3 pos, @NotNull String world) {
			return new PosWithWorld(world, pos.x(), pos.y(), pos.z());
		}
	}

	private static class EnforcementState {
		final UUID islandOwner;
		final boolean isInParty;
		final boolean isBanned;

		EnforcementState(UUID islandOwner, boolean isInParty, boolean isBanned) {
			this.islandOwner = islandOwner;
			this.isInParty = isInParty;
			this.isBanned = isBanned;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof EnforcementState other))
				return false;
			return Objects.equals(islandOwner, other.islandOwner) && isInParty == other.isInParty
					&& isBanned == other.isBanned;
		}

		@Override
		public int hashCode() {
			return Objects.hash(islandOwner, isInParty, isBanned);
		}
	}

	public enum SafeHomeResult {
		ALREADY_SAFE, FIXED_AND_TELEPORTED, FAILED_TO_FIX;
	}
}