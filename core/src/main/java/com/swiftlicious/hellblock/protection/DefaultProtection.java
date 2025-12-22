package com.swiftlicious.hellblock.protection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

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
		final String playerName = ownerData.getName();
		final UUID ownerUUID = ownerData.getUUID();

		try {
			instance.debug("protectHellblock: Starting protection setup for " + playerName + " in world '"
					+ world.worldName() + "'");

			final HellblockData hellblockData = ownerData.getHellblockData();
			int islandId = hellblockData.getIslandId();
			if (islandId <= 0) {
				return CompletableFuture.failedFuture(
						new IllegalStateException("protectHellblock: Invalid island ID for player: " + playerName));
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

				// Option 3: Shared world → derive bounding box from spiral logic
			} else {
				boundingBox = instance.getPlacementDetector().computeSpiralBoundingBoxForIsland(islandId, world);
				instance.debug("protectHellblock: Computed bounding box from spiral placement.");
			}

			hellblockData.setBoundingBox(boundingBox);
			instance.getPlacementDetector().cacheIslandBoundingBox(islandId, boundingBox);

			instance.debug("protectHellblock: Final bounding box for " + playerName + " → Min[" + boundingBox.getMinX()
					+ ", " + boundingBox.getMinY() + ", " + boundingBox.getMinZ() + "], Max[" + boundingBox.getMaxX()
					+ ", " + boundingBox.getMaxY() + ", " + boundingBox.getMaxZ() + "]");

			// Wait for both protection messages and lock status to complete
			return updateHellblockMessages(world, ownerUUID).thenCompose(v -> {
				instance.debug("protectHellblock: Updated protection messages for " + playerName);
				return instance.getProtectionManager().changeLockStatus(world, ownerUUID);
			}).thenRun(() -> instance.debug("protectHellblock: Lock status updated for " + playerName))
					.exceptionally(ex -> {
						instance.getPluginLogger().severe("protectHellblock: Failed during async protection steps for "
								+ playerName + "'s island!", ex);
						throw new CompletionException(ex);
					});

		} catch (Exception ex) {
			instance.getPluginLogger().severe("protectHellblock: Failed to protect " + playerName + "'s island!", ex);
			return CompletableFuture.failedFuture(ex);
		}
	}

	@Override
	public CompletableFuture<Void> unprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		instance.debug("unprotectHellblock: Starting unprotection for island UUID=" + ownerId + " in world '"
				+ world.worldName() + "'");

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenAccept(optData -> {
			if (optData.isEmpty()) {
				instance.getPluginLogger().warn(
						"unprotectHellblock: No UserData found for UUID: " + ownerId + ". Skipping unprotection.");
				return;
			}

			final UserData ownerData = optData.get();
			final HellblockData data = ownerData.getHellblockData();
			final BoundingBox boundingBox = data.getBoundingBox();

			if (boundingBox == null) {
				instance.getPluginLogger().warn(
						"unprotectHellblock: No bounding box set for UUID: " + ownerId + ". Skipping entity cleanup.");
				return;
			}

			instance.debug("unprotectHellblock: Clearing entities in bounding box for UUID=" + ownerId + " → [MinX="
					+ boundingBox.getMinX() + ", MinY=" + boundingBox.getMinY() + ", MinZ=" + boundingBox.getMinZ()
					+ ", MaxX=" + boundingBox.getMaxX() + ", MaxY=" + boundingBox.getMaxY() + ", MaxZ="
					+ boundingBox.getMaxZ() + "]");

			instance.getProtectionManager().clearHellblockEntities(world.bukkitWorld(), boundingBox);
			instance.debug("unprotectHellblock: Entity cleanup complete for UUID=" + ownerId);
		}).exceptionally(ex -> {
			instance.getPluginLogger().severe("unprotectHellblock: Error during unprotection for UUID: " + ownerId, ex);
			return null; // Void-returning exceptionally block
		});
	}

	@Override
	public CompletableFuture<Void> reprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull UserData transfereeData) {
		final String ownerName = ownerData.getName();
		final String transfereeName = transfereeData.getName();
		final UUID transfereeUUID = transfereeData.getUUID();

		try {
			instance.debug("reprotectHellblock: Starting reprotection from owner " + ownerName + " to transferee "
					+ transfereeName + " in world '" + world.worldName() + "'");

			Location minLoc = getProtectionVectorUpperCorner(ownerData).toLocation(world.bukkitWorld());
			Location maxLoc = getProtectionVectorLowerCorner(ownerData).toLocation(world.bukkitWorld());

			double minX = Math.min(minLoc.getX(), maxLoc.getX());
			double minY = Math.min(minLoc.getY(), maxLoc.getY());
			double minZ = Math.min(minLoc.getZ(), maxLoc.getZ());
			double maxX = Math.max(minLoc.getX(), maxLoc.getX());
			double maxY = Math.max(minLoc.getY(), maxLoc.getY());
			double maxZ = Math.max(minLoc.getZ(), maxLoc.getZ());

			BoundingBox boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
			transfereeData.getHellblockData().setBoundingBox(boundingBox);

			instance.getPlacementDetector().cacheIslandBoundingBox(transfereeData.getHellblockData().getIslandId(),
					boundingBox);

			instance.debug("reprotectHellblock: Calculated bounding box for " + transfereeName + " → Min[" + minX + ", "
					+ minY + ", " + minZ + "], Max[" + maxX + ", " + maxY + ", " + maxZ + "]");

			// Compose futures properly
			return updateHellblockMessages(world, transfereeUUID).thenCompose(v -> {
				instance.debug("reprotectHellblock: Updated protection messages for transferee " + transfereeName);
				return instance.getProtectionManager().changeLockStatus(world, transfereeUUID);
			}).thenAccept(v -> {
				instance.debug("reprotectHellblock: Lock status updated for transferee " + transfereeName);
			}).exceptionally(ex -> {
				instance.getPluginLogger()
						.severe("reprotectHellblock: Failed to reprotect island for transferee " + transfereeName, ex);
				throw new CompletionException(ex); // propagate
			});

		} catch (Exception ex) {
			instance.getPluginLogger().severe(
					"reprotectHellblock: Unexpected failure before async steps for transferee " + transfereeName, ex);
			return CompletableFuture.failedFuture(ex);
		}
	}

	@Override
	public CompletableFuture<Boolean> updateHellblockMessages(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		instance.debug("updateHellblockMessages: Updating hellblock messages for UUID: " + ownerId);

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("updateHellblockMessages: No UserData found for UUID: " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();

			instance.debug("updateHellblockMessages: Retrieved user data for message update: " + ownerData.getName());
			updateMessagesForUser(ownerData, ownerData.getName());
			return instance.getStorageManager().saveUserData(ownerData, true);
		}).handle((result, ex) -> {
			if (ex != null) {
				instance.getPluginLogger()
						.warn("updateHellblockMessages: Failed to update entry/farwell messages for UUID " + ownerId
								+ ": " + ex.getMessage());
			}
			return instance.getStorageManager().unlockUserData(ownerId).thenApply(x -> true);
		}).thenCompose(Function.identity());
	}

	/**
	 * Updates the greeting and farewell messages for a user's island based on their
	 * current name and island state.
	 *
	 * @param ownerData   The {@link UserData} of the island owner.
	 * @param updatedName The most up-to-date player name.
	 */
	private void updateMessagesForUser(@NotNull UserData ownerData, @NotNull String updatedName) {
		final boolean abandoned = ownerData.getHellblockData().isAbandoned();

		// Greeting flag
		createAndSetMessageFlag(ownerData, HellblockFlag.FlagType.GREET_MESSAGE,
				instance.getConfigManager().entryMessageEnabled(),
				abandoned ? MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key()
						: MessageConstants.HELLBLOCK_ENTRY_MESSAGE
								.arguments(AdventureHelper.miniMessageToComponent(updatedName)).build().key());

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
	private void createAndSetMessageFlag(@NotNull UserData ownerData, @NotNull HellblockFlag.FlagType type,
			boolean enabled, @NotNull String messageKey) {
		instance.debug("createAndSetMessageFlag: Creating message flag for type: " + type + ", enabled: " + enabled
				+ ", messageKey: " + messageKey);

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
	public CompletableFuture<Boolean> abandonIsland(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		instance.debug("abandonIsland: Attempting to abandon island for UUID: " + ownerId);

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("abandonIsland: No UserData found for UUID: " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();
			final HellblockData data = ownerData.getHellblockData();

			if (!data.isAbandoned()) {
				instance.debug("abandonIsland: Island was not marked for abandonment for UUID: " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			data.setProtectionValue(new HellblockFlag(HellblockFlag.FlagType.PVP, HellblockFlag.AccessType.ALLOW));
			data.setProtectionValue(new HellblockFlag(HellblockFlag.FlagType.BUILD, HellblockFlag.AccessType.ALLOW));
			if (data.isLocked())
				data.setProtectionValue(
						new HellblockFlag(HellblockFlag.FlagType.ENTRY, HellblockFlag.AccessType.ALLOW));
			data.setProtectionValue(
					new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING, HellblockFlag.AccessType.DENY));

			instance.debug("abandonIsland: Island marked as abandoned. Updating protection flags for UUID: " + ownerId);

			return instance.getStorageManager().saveUserData(ownerData, true);
		}).handle((result, ex) -> {
			if (ex != null) {
				instance.getPluginLogger()
						.warn("abandonIsland: Failed to abandon island for UUID " + ownerId + ": " + ex.getMessage());
			}
			return instance.getStorageManager().unlockUserData(ownerId).thenApply(x -> true);
		}).thenCompose(Function.identity());
	}

	@Override
	public CompletableFuture<Boolean> restoreFlags(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		instance.debug("restoreFlags: Restoring protection flags for UUID: " + ownerId);

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("restoreFlags: No UserData found for UUID: " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();
			final HellblockData data = ownerData.getHellblockData();

			if (data.isAbandoned()) {
				instance.debug("restoreFlags: Island was found marked as abandoned for UUID: " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			data.setProtectionValue(new HellblockFlag(HellblockFlag.FlagType.PVP, HellblockFlag.AccessType.DENY));
			data.setProtectionValue(new HellblockFlag(HellblockFlag.FlagType.BUILD, HellblockFlag.AccessType.DENY));
			if (data.isLocked())
				data.setProtectionValue(new HellblockFlag(HellblockFlag.FlagType.ENTRY, HellblockFlag.AccessType.DENY));
			data.setProtectionValue(
					new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING, HellblockFlag.AccessType.ALLOW));

			instance.debug("restoreFlags: Island is abandoned, marking as restored. Restoring default flags for UUID: "
					+ ownerId);

			// Save changes
			return instance.getStorageManager().saveUserData(ownerData, true);
		}).handle((result, ex) -> {
			if (ex != null) {
				instance.getPluginLogger()
						.warn("restoreFlags: Failed to restore flags for UUID " + ownerId + ": " + ex.getMessage());
			}
			return instance.getStorageManager().unlockUserData(ownerId).thenApply(x -> true);
		}).thenCompose(Function.identity());
	}

	@Override
	public CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull HellblockWorld<?> world,
			@NotNull UUID ownerId) {
		instance.debug("getMembersOfHellblockBounds: Fetching island members for UUID: " + ownerId);

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenApply(optData -> {
			if (optData.isEmpty()) {
				instance.debug("getMembersOfHellblockBounds: No UserData found for UUID: " + ownerId);
				return Set.of();
			}

			Set<UUID> islandMembers = optData.get().getHellblockData().getIslandMembers();
			instance.debug("Found " + islandMembers.size() + " member" + (islandMembers.size() == 1 ? "" : "s")
					+ " for island UUID: " + ownerId);
			return islandMembers;
		});
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