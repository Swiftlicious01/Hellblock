package com.swiftlicious.hellblock.protection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.world.HellblockWorld;

/**
 * Internal implementation of {@link IslandProtection} using Bukkit-based
 * protection logic.
 * <p>
 * This fallback is used when no external protection plugins (like WorldGuard)
 * are available or enabled in the configuration.
 *
 * <p>
 * It operates using {@link org.bukkit.util.Vector} for island protection
 * boundaries.
 */
public class DefaultProtection implements IslandProtection<Vector> {

	protected final HellblockPlugin instance;

	public DefaultProtection(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public CompletableFuture<Void> protectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData) {
		String playerName = ownerData.getName();
		UUID ownerUUID = ownerData.getUUID();

		try {
			instance.debug("protectHellblock: Starting protection setup for " + playerName + " in world '"
					+ world.worldName() + "'");

			final HellblockData hellblockData = ownerData.getHellblockData();
			int islandId = hellblockData.getIslandId();
			if (islandId <= 0) {
				throw new IllegalStateException("protectHellblock: Invalid island ID for player: " + playerName);
			}

			BoundingBox boundingBox;

			// Option 1: Per-player world → full world
			if (instance.getConfigManager().perPlayerWorlds()) {
				instance.debug("protectHellblock: Using full-world bounding box (per-player world mode).");

				// Fetch the configured default protection radius from upgrades
				double radius = instance.getUpgradeManager().getDefaultValue(IslandUpgradeType.PROTECTION_RANGE)
						.intValue();

				double minY = world.bukkitWorld().getMinHeight();
				double maxY = world.bukkitWorld().getMaxHeight();

				boundingBox = new BoundingBox(-radius, minY, -radius, radius, maxY, radius);

				// Option 2: Reset → preserved bounding box
			} else if (hellblockData.getPreservedBoundingBox() != null) {
				boundingBox = hellblockData.getPreservedBoundingBox();
				instance.debug("protectHellblock: Using preserved bounding box from previous island reset.");
			}

			// Option 3: Shared world → derive bounding box from spiral logic
			else {
				boundingBox = instance.getPlacementDetector().computeSpiralBoundingBoxForIsland(islandId, world);
				instance.debug("protectHellblock: Computed bounding box from spiral placement.");
			}

			hellblockData.setBoundingBox(boundingBox);
			instance.getPlacementDetector().cacheIslandBoundingBox(islandId, boundingBox);

			instance.debug("protectHellblock: Final bounding box for " + playerName + " → Min[" + boundingBox.getMinX()
					+ ", " + boundingBox.getMinY() + ", " + boundingBox.getMinZ() + "], Max[" + boundingBox.getMaxX()
					+ ", " + boundingBox.getMaxY() + ", " + boundingBox.getMaxZ() + "]");

			updateHellblockMessages(world, ownerUUID);
			instance.debug("protectHellblock: Updated protection messages for " + playerName);

			// Wait for lock status to complete before returning
			return instance.getProtectionManager().changeLockStatus(world, ownerUUID)
					.thenRun(() -> instance.debug("protectHellblock: Lock status updated for " + playerName));
		} catch (Exception ex) {
			instance.getPluginLogger().severe("protectHellblock: Failed to protect " + playerName + "'s island!", ex);
			return CompletableFuture.failedFuture(ex);
		}
	}

	@Override
	public CompletableFuture<Void> unprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		final CompletableFuture<Void> unprotection = new CompletableFuture<>();

		try {
			instance.debug("unprotectHellblock: Starting unprotection for island UUID=" + ownerId + " in world '"
					+ world.worldName() + "'");

			instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						try {
							if (result.isEmpty()) {
								instance.getPluginLogger().warn("unprotectHellblock: No UserData found for UUID: "
										+ ownerId + ". Skipping unprotection.");
								unprotection.complete(null);
								return;
							}

							final UserData offlineUser = result.get();
							final HellblockData data = offlineUser.getHellblockData();

							BoundingBox boundingBox = data.getBoundingBox();
							if (boundingBox == null) {
								instance.getPluginLogger().warn("unprotectHellblock: No bounding box set for UUID: "
										+ ownerId + ". Skipping entity cleanup.");
								unprotection.complete(null);
								return;
							}

							instance.debug("unprotectHellblock: Clearing entities in bounding box for UUID=" + ownerId
									+ " → [MinX=" + boundingBox.getMinX() + ", MinY=" + boundingBox.getMinY()
									+ ", MinZ=" + boundingBox.getMinZ() + ", MaxX=" + boundingBox.getMaxX() + ", MaxY="
									+ boundingBox.getMaxY() + ", MaxZ=" + boundingBox.getMaxZ() + "]");

							instance.getProtectionManager().clearHellblockEntities(world.bukkitWorld(), boundingBox);

							instance.debug("unprotectHellblock: Entity cleanup complete for UUID=" + ownerId);
							unprotection.complete(null);

						} catch (Exception ex) {
							instance.getPluginLogger().severe(
									"unprotectHellblock: Error while unprotecting hellblock for UUID: " + ownerId, ex);
							unprotection.completeExceptionally(ex);
						}
					});
		} catch (Exception ex) {
			instance.getPluginLogger().severe("unprotectHellblock: Failed to start unprotection for UUID: " + ownerId,
					ex);
			unprotection.completeExceptionally(ex);
		}

		return unprotection;
	}

	@Override
	public CompletableFuture<Void> reprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull UserData transfereeData) {
		final CompletableFuture<Void> reprotection = new CompletableFuture<>();
		String ownerName = ownerData.getName();
		String transfereeName = transfereeData.getName();
		UUID transfereeUUID = transfereeData.getUUID();

		try {
			instance.debug("reprotectHellblock: Starting reprotection from owner " + ownerName + " to transferee "
					+ transfereeName + " in world '" + world.worldName() + "'");

			Location l1 = getProtectionVectorUpperCorner(ownerData).toLocation(world.bukkitWorld());
			Location l2 = getProtectionVectorLowerCorner(ownerData).toLocation(world.bukkitWorld());

			double minX = Math.min(l1.getX(), l2.getX());
			double minY = Math.min(l1.getY(), l2.getY());
			double minZ = Math.min(l1.getZ(), l2.getZ());
			double maxX = Math.max(l1.getX(), l2.getX());
			double maxY = Math.max(l1.getY(), l2.getY());
			double maxZ = Math.max(l1.getZ(), l2.getZ());

			BoundingBox boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
			transfereeData.getHellblockData().setBoundingBox(boundingBox);
			instance.getPlacementDetector().cacheIslandBoundingBox(transfereeData.getHellblockData().getIslandId(),
					transfereeData.getHellblockData().getBoundingBox());

			instance.debug("reprotectHellblock: Calculated bounding box for " + transfereeName + " → Min[" + minX + ", "
					+ minY + ", " + minZ + "], Max[" + maxX + ", " + maxY + ", " + maxZ + "]");

			updateHellblockMessages(world, transfereeUUID);
			instance.debug("reprotectHellblock: Updated protection messages for transferee " + transfereeName);

			instance.getProtectionManager().changeLockStatus(world, transfereeUUID);
			instance.debug("reprotectHellblock: Lock status updated for transferee " + transfereeName);

			reprotection.complete(null);
		} catch (Exception ex) {
			instance.getPluginLogger()
					.severe("reprotectHellblock: Failed to reprotect island for transferee " + transfereeName, ex);
			reprotection.completeExceptionally(ex);
		}
		return reprotection;
	}

	@Override
	public void updateHellblockMessages(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		instance.debug("Updating hellblock messages for UUID: " + ownerId);

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> result.ifPresent(ownerData -> {
					if (ownerData == null) {
						instance.getPluginLogger().warn(
								"Failed to retrieve player's username to update hellblock entry/farewell messages.");
						return;
					}

					instance.debug("Retrieved user data for message update: " + ownerData.getName());
					updateMessagesForUser(ownerData, ownerData.getName());
				}));
	}

	/**
	 * Updates the greeting and farewell messages for a user's island based on their
	 * current name and island state.
	 *
	 * @param ownerData   The {@link UserData} of the island owner.
	 * @param updatedName The most up-to-date player name.
	 */
	private void updateMessagesForUser(UserData ownerData, String updatedName) {
		final boolean abandoned = ownerData.getHellblockData().isAbandoned();

		// Greeting flag
		createAndSetMessageFlag(ownerData, HellblockFlag.FlagType.GREET_MESSAGE,
				instance.getConfigManager().entryMessageEnabled(),
				abandoned ? MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key()
						: MessageConstants.HELLBLOCK_ENTRY_MESSAGE.arguments(AdventureHelper.miniMessageToComponent(updatedName))
								.build().key());

		// Farewell flag
		createAndSetMessageFlag(ownerData, HellblockFlag.FlagType.FAREWELL_MESSAGE,
				instance.getConfigManager().farewellMessageEnabled(),
				abandoned ? MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key()
						: MessageConstants.HELLBLOCK_FAREWELL_MESSAGE
								.arguments(AdventureHelper.miniMessageToComponent(updatedName)).build().key());
	}

	/**
	 * Creates and applies a message-based protection flag for an island (e.g.,
	 * greeting or farewell message).
	 *
	 * @param ownerData  The {@link UserData} containing the Hellblock data.
	 * @param type       The {@link HellblockFlag.FlagType} to update (greet or
	 *                   farewell).
	 * @param enabled    Whether the flag should be enabled or disabled.
	 * @param messageKey The translated message key or raw message to apply.
	 */
	private void createAndSetMessageFlag(UserData ownerData, HellblockFlag.FlagType type, boolean enabled,
			String messageKey) {

		instance.debug(
				"Creating message flag for type: " + type + ", enabled: " + enabled + ", messageKey: " + messageKey);

		final HellblockFlag flag = new HellblockFlag(type,
				enabled ? HellblockFlag.AccessType.ALLOW : HellblockFlag.AccessType.DENY);

		flag.setData(enabled ? instance.getTranslationManager()
				.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(messageKey)) : null);

		// Apply to the island's protection data
		HellblockData hellblockData = ownerData.getHellblockData();
		hellblockData.setProtectionData(
				type, enabled
						? instance.getTranslationManager()
								.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(messageKey))
						: null);
	}

	@Override
	public void abandonIsland(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		instance.debug("Attempting to abandon island for UUID: " + ownerId);

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("No UserData found for UUID: " + ownerId + " (abandonIsland)");
						return;
					}

					final UserData offlineUser = result.get();
					final HellblockData data = offlineUser.getHellblockData();

					if (data.isAbandoned()) {
						instance.debug(
								"Island already marked as abandoned. Updating protection flags for UUID: " + ownerId);
						data.setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.PVP, HellblockFlag.AccessType.ALLOW));
						data.setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.BUILD, HellblockFlag.AccessType.ALLOW));
						data.setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.ENTRY, HellblockFlag.AccessType.ALLOW));
						data.setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING, HellblockFlag.AccessType.DENY));
					}
				});
	}

	@Override
	public void restoreFlags(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		instance.debug("Restoring protection flags for UUID: " + ownerId);

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("No UserData found for UUID: " + ownerId + " (restoreFlags)");
						return;
					}

					final UserData offlineUser = result.get();
					final HellblockData data = offlineUser.getHellblockData();

					if (!data.isAbandoned()) {
						instance.debug("Island is not abandoned. Restoring default flags for UUID: " + ownerId);
						data.setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.PVP, HellblockFlag.AccessType.DENY));
						data.setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.BUILD, HellblockFlag.AccessType.DENY));
						data.setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.ENTRY, HellblockFlag.AccessType.ALLOW));
						data.setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING, HellblockFlag.AccessType.ALLOW));
					}
				});
	}

	@Override
	public CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull HellblockWorld<?> world,
			@NotNull UUID ownerId) {
		instance.debug("Fetching island members for UUID: " + ownerId);

		final CompletableFuture<Set<UUID>> members = new CompletableFuture<>();

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("No UserData found for UUID: " + ownerId + " (getMembersOfHellblockBounds)");
						members.complete(Set.of());
						return;
					}

					Set<UUID> islandMembers = result.get().getHellblockData().getIslandMembers();
					instance.debug("Found " + islandMembers.size() + " member" + (islandMembers.size() == 1 ? "" : "s")
							+ " for island UUID: " + ownerId);
					members.complete(islandMembers);
				});

		return members;
	}

	@Override
	@NotNull
	public Vector getProtectionVectorUpperCorner(@NotNull UserData ownerData) {
		final BoundingBox bounds = ownerData.getHellblockData().getBoundingBox();

		if (bounds == null) {
			throw new IllegalStateException("Bounding box not set. Did you call expandBoundingBox()?");
		}

		instance.debug("Upper corner vector for UUID " + ownerData.getUUID() + ": " + bounds.getMaxX() + ", "
				+ bounds.getMaxY() + ", " + bounds.getMaxZ());

		return new Vector(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
	}

	@Override
	@NotNull
	public Vector getProtectionVectorLowerCorner(@NotNull UserData ownerData) {
		final BoundingBox bounds = ownerData.getHellblockData().getBoundingBox();

		if (bounds == null) {
			throw new IllegalStateException("Bounding box not set. Did you call expandBoundingBox()?");
		}

		instance.debug("Lower corner vector for UUID " + ownerData.getUUID() + ": " + bounds.getMinX() + ", "
				+ bounds.getMinY() + ", " + bounds.getMinZ());

		return new Vector(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
	}
}