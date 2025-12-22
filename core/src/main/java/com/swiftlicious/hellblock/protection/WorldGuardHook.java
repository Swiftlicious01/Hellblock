package com.swiftlicious.hellblock.protection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.world.HellblockWorld;

/**
 * WorldGuard-based implementation of {@link IslandProtection} for managing
 * island protection.
 * <p>
 * Integrates with WorldGuard (7.0 or higher) to define and enforce region rules
 * using WorldGuard APIs.
 *
 * <p>
 * It uses {@link com.sk89q.worldedit.math.BlockVector3} to represent protection
 * boundary corners.
 */
public class WorldGuardHook implements IslandProtection<BlockVector3> {

	private final WorldGuardPlatform worldGuardPlatform;

	protected final HellblockPlugin instance;

	public WorldGuardHook(HellblockPlugin plugin) {
		instance = plugin;
		this.worldGuardPlatform = WorldGuard.getInstance().getPlatform();
	}

	/**
	 * Returns the instance of the WorldGuard platform used by this protection hook.
	 * <p>
	 * This may return {@code null} if WorldGuard is not available or failed to
	 * initialize.
	 *
	 * @return the {@link WorldGuardPlatform} instance, or {@code null} if
	 *         unavailable
	 */
	@Nullable
	private WorldGuardPlatform getWorldGuardPlatform() {
		return this.worldGuardPlatform;
	}

	/**
	 * Checks if WorldGuard is available and functioning correctly.
	 * <p>
	 * Attempts to retrieve the WorldGuard platform instance. If this fails due to a
	 * missing or incompatible WorldGuard version, logs a severe error and returns
	 * {@code false}.
	 *
	 * @return {@code true} if WorldGuard is initialized and supported,
	 *         {@code false} otherwise
	 */
	public static boolean isWorking() {
		try {
			return WorldGuard.getInstance().getPlatform() != null;
		} catch (Throwable t) {
			HellblockPlugin.getInstance().getPluginLogger().severe(
					"WorldGuard threw an error during initializing, make sure it's updated and API compatible (Must be 7.0 or higher)",
					t);
			return false;
		}
	}

	@Override
	public CompletableFuture<Void> protectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData) {
		final String playerName = ownerData.getName();
		final UUID ownerUUID = ownerData.getUUID();

		if (getWorldGuardPlatform() == null) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("protectHellblock: WorldGuard platform is not initialized."));
		}

		instance.debug("protectHellblock: Starting WorldGuard protection setup for " + playerName + " in world '"
				+ world.worldName() + "'");

		try {
			final RegionContainer container = getWorldGuardPlatform().getRegionContainer();
			final RegionManager regionManager = container.get(BukkitAdapter.adapt(world.bukkitWorld()));
			if (regionManager == null) {
				return CompletableFuture.failedFuture(new IllegalStateException(
						"protectHellblock: No WorldGuard RegionManager for world: " + world.worldName()));
			}

			final HellblockData hellblockData = ownerData.getHellblockData();
			int islandId = hellblockData.getIslandId();
			if (islandId <= 0) {
				return CompletableFuture.failedFuture(
						new IllegalStateException("protectHellblock: Invalid island ID for player: " + playerName));
			}

			BoundingBox boundingBox;

			if (instance.getConfigManager().perPlayerWorlds()) {
				instance.debug("protectHellblock: Using full-world bounding box (per-player world mode).");

				double radius = instance.getUpgradeManager().getDefaultValue(IslandUpgradeType.PROTECTION_RANGE)
						.intValue();
				double minY = world.bukkitWorld().getMinHeight();
				double maxY = world.bukkitWorld().getMaxHeight();

				boundingBox = new BoundingBox(-radius, minY, -radius, radius, maxY, radius);
			} else if (hellblockData.getPreservedBoundingBox() != null) {
				boundingBox = hellblockData.getPreservedBoundingBox();
				instance.debug("protectHellblock: Using preserved bounding box from previous island reset.");
			} else {
				boundingBox = instance.getPlacementDetector().computeSpiralBoundingBoxForIsland(islandId, world);
				instance.debug("protectHellblock: Computed bounding box from spiral placement.");
			}

			hellblockData.setBoundingBox(boundingBox);
			instance.getPlacementDetector().cacheIslandBoundingBox(islandId, boundingBox);

			instance.debug("protectHellblock: Final bounding box for " + playerName + " → Min[" + boundingBox.getMinX()
					+ ", " + boundingBox.getMinY() + ", " + boundingBox.getMinZ() + "], Max[" + boundingBox.getMaxX()
					+ ", " + boundingBox.getMaxY() + ", " + boundingBox.getMaxZ() + "]");

			return instance.getScheduler().callSync(() -> {
				BlockVector3 min = BlockVector3.at(Math.floor(boundingBox.getMinX()), Math.floor(boundingBox.getMinY()),
						Math.floor(boundingBox.getMinZ()));
				BlockVector3 max = BlockVector3.at(Math.ceil(boundingBox.getMaxX()), Math.ceil(boundingBox.getMaxY()),
						Math.ceil(boundingBox.getMaxZ()));

				String regionId = "%s_%s".formatted(ownerUUID, islandId);
				ProtectedRegion region = new ProtectedCuboidRegion(regionId, min, max);

				try {
					region.setParent(regionManager.getRegion(ProtectedRegion.GLOBAL_REGION));
				} catch (Exception ex) {
					instance.getPluginLogger()
							.warn("protectHellblock: Failed to set WorldGuard parent region for " + regionId, ex);
				}

				DefaultDomain owners = new DefaultDomain();
				owners.addPlayer(ownerUUID);
				region.setOwners(owners);
				region.setPriority(100);

				ApplicableRegionSet existingRegions = regionManager.getApplicableRegions(min);
				for (ProtectedRegion regionCheck : existingRegions) {
					if (!regionCheck.getId().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION)) {
						instance.debug(
								"protectHellblock: Removing overlapping WorldGuard region: " + regionCheck.getId());
						regionManager.removeRegion(regionCheck.getId());
					}
				}

				regionManager.addRegion(region);
				try {
					regionManager.save();
					instance.debug(
							"protectHellblock: WorldGuard region '" + regionId + "' saved for island ID: " + islandId);
				} catch (Throwable t) {
					instance.getPluginLogger()
							.severe("protectHellblock: Failed to save WorldGuard RegionManager for " + playerName, t);
					throw new CompletionException(t);
				}
				return null;
			}).thenCompose(v -> updateHellblockMessages(world, ownerUUID)).thenCompose(v -> {
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
		if (getWorldGuardPlatform() == null) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("unprotectHellblock: WorldGuard platform is not initialized."));
		}

		instance.debug("unprotectHellblock: Starting WorldGuard unprotection for island owner: " + ownerId
				+ " in world '" + world.worldName() + "'");

		final RegionContainer regionContainer = getWorldGuardPlatform().getRegionContainer();
		final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world.bukkitWorld()));

		if (regionManager == null) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					"unprotectHellblock: No WorldGuard RegionManager for world: " + world.worldName()));
		}

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("unprotectHellblock: No user data found for " + ownerId + ". Skipping unprotection.");
				return CompletableFuture.completedFuture(null);
			}

			final UserData userData = optData.get();
			final HellblockData data = userData.getHellblockData();
			final BoundingBox boundingBox = data.getBoundingBox();

			if (boundingBox == null) {
				instance.getPluginLogger()
						.warn("unprotectHellblock: No bounding box set for " + ownerId + ". Skipping entity cleanup.");
			} else {
				instance.debug("unprotectHellblock: Clearing entities in bounding box for " + ownerId + " → [MinX="
						+ boundingBox.getMinX() + ", MinY=" + boundingBox.getMinY() + ", MinZ=" + boundingBox.getMinZ()
						+ ", MaxX=" + boundingBox.getMaxX() + ", MaxY=" + boundingBox.getMaxY() + ", MaxZ="
						+ boundingBox.getMaxZ() + "]");
				instance.getProtectionManager().clearHellblockEntities(world.bukkitWorld(), boundingBox);
				instance.debug("unprotectHellblock: Entity cleanup complete for " + ownerId);
			}

			return instance.getScheduler().callSyncImmediate(() -> {
				final String regionName = "%s_%s".formatted(ownerId, data.getIslandId());
				final ProtectedRegion region = regionManager.getRegion(regionName);

				if (region == null) {
					instance.getPluginLogger().severe("unprotectHellblock: WorldGuard region '" + regionName
							+ "' not found during unprotection.");
					return CompletableFuture.<Void>completedFuture(null);
				}

				regionManager.removeRegion(regionName, RemovalStrategy.UNSET_PARENT_IN_CHILDREN);

				try {
					regionManager.save();
					instance.debug("unprotectHellblock: WorldGuard region '" + regionName + "' successfully removed.");
				} catch (Exception ex) {
					instance.getPluginLogger()
							.severe("unprotectHellblock: Failed to save WorldGuard RegionManager after removing region "
									+ regionName, ex);
					throw new CompletionException(ex);
				}

				return CompletableFuture.<Void>completedFuture(null);
			}).thenCompose(Function.identity());
		}).exceptionally(ex -> {
			instance.getPluginLogger().severe("unprotectHellblock: Failed to unprotect island for " + ownerId, ex);
			throw new CompletionException(ex);
		});
	}

	@Override
	public CompletableFuture<Void> reprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull UserData transfereeData) {
		final String ownerName = ownerData.getName();
		final String transfereeName = transfereeData.getName();
		final UUID transfereeUUID = transfereeData.getUUID();

		if (getWorldGuardPlatform() == null) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("reprotectHellblock: WorldGuard platform is not initialized."));
		}

		instance.debug("reprotectHellblock: Starting reprotection from owner " + ownerName + " to transferee "
				+ transfereeName + " in world '" + world.worldName() + "'");

		final RegionContainer container = getWorldGuardPlatform().getRegionContainer();
		final RegionManager regionManager = container.get(BukkitAdapter.adapt(world.bukkitWorld()));
		if (regionManager == null) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					"reprotectHellblock: No WorldGuard RegionManager for world: " + world.worldName()));
		}

		return instance.getScheduler().callSyncImmediate(() -> {
			final ProtectedRegion region = getRegion(world, ownerData.getUUID(),
					ownerData.getHellblockData().getIslandId());
			if (region == null) {
				return CompletableFuture.failedFuture(new IllegalStateException(
						"reprotectHellblock: Could not get WorldGuard region for owner: " + ownerName));
			}

			instance.debug("reprotectHellblock: Found existing WorldGuard region: " + region.getId());

			final DefaultDomain owners = region.getOwners();
			final DefaultDomain members = region.getMembers();

			if (!owners.getUniqueIds().contains(transfereeUUID)) {
				owners.getUniqueIds().add(transfereeUUID);
				instance.debug(
						"reprotectHellblock: Added transferee " + transfereeUUID + " to WorldGuard region owners.");
			}

			owners.getUniqueIds().remove(ownerData.getUUID());
			instance.debug(
					"reprotectHellblock: Removed old owner " + ownerData.getUUID() + " from WorldGuard region owners.");

			if (!members.getUniqueIds().contains(ownerData.getUUID())) {
				members.getUniqueIds().add(ownerData.getUUID());
				instance.debug("reprotectHellblock: Added previous owner " + ownerData.getUUID()
						+ " as a WorldGuard region member.");
			}

			members.getUniqueIds().remove(transfereeUUID);

			final String newRegionId = "%s_%s".formatted(transfereeUUID,
					transfereeData.getHellblockData().getIslandId());
			final ProtectedRegion newRegion = new ProtectedCuboidRegion(newRegionId,
					getProtectionVectorUpperCorner(ownerData), getProtectionVectorLowerCorner(ownerData));

			instance.debug("reprotectHellblock: Created new WorldGuard region with ID: " + newRegionId);

			newRegion.setOwners(owners);
			newRegion.setMembers(members);
			newRegion.setFlags(region.getFlags());
			newRegion.setPriority(region.getPriority());

			try {
				newRegion.setParent(region.getParent());
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("reprotectHellblock: Failed to set parent for new WorldGuard region " + newRegionId, ex);
			}

			regionManager.addRegion(newRegion);
			regionManager.removeRegion(region.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);

			try {
				regionManager.save();
				instance.debug("reprotectHellblock: WorldGuard RegionManager saved after replacing region '"
						+ region.getId() + "' with '" + newRegionId + "'");
			} catch (Exception ex) {
				instance.getPluginLogger().severe(
						"reprotectHellblock: Failed to save WorldGuard RegionManager after region transfer", ex);
				throw new CompletionException(ex);
			}

			BoundingBox boundingBox = new BoundingBox(newRegion.getMinimumPoint().x(), newRegion.getMinimumPoint().y(),
					newRegion.getMinimumPoint().z(), newRegion.getMaximumPoint().x(), newRegion.getMaximumPoint().y(),
					newRegion.getMaximumPoint().z());

			transfereeData.getHellblockData().setBoundingBox(boundingBox);
			instance.getPlacementDetector().cacheIslandBoundingBox(transfereeData.getHellblockData().getIslandId(),
					boundingBox);

			instance.debug("reprotectHellblock: Updated bounding box for transferee " + transfereeUUID);

			return null;
		}).thenCompose(v -> updateHellblockMessages(world, transfereeUUID).thenRun(() -> instance
				.debug("reprotectHellblock: Updated protection messages for transferee " + transfereeName)))
				.thenCompose(v -> instance.getProtectionManager().changeLockStatus(world, transfereeUUID)
						.thenRun(() -> instance
								.debug("reprotectHellblock: Lock status updated for transferee " + transfereeName)))
				.exceptionally(ex -> {
					instance.getPluginLogger().severe(
							"reprotectHellblock: Failed to reprotect island for transferee " + transfereeName, ex);
					throw new CompletionException(ex);
				});
	}

	@Override
	public CompletableFuture<Boolean> updateHellblockMessages(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		instance.debug("updateHellblockMessages: Updating hellblock messages for UUID: " + ownerId);
		// First, try to apply flags for online users
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(ownerId);
		if (onlineUser.isPresent()) {
			final UserData ownerData = onlineUser.get();
			final ProtectedRegion region = getRegion(world, ownerId, ownerData.getHellblockData().getIslandId());
			if (region == null) {
				instance.getPluginLogger()
						.severe("updateHellblockMessages: No WorldGuard region found for online user: " + ownerId);
				return CompletableFuture.completedFuture(false);
			}
			instance.debug("updateHellblockMessages: Applying WorldGuard message flags for online user: " + ownerId);
			applyEntryAndFarewellFlags(ownerData, region);
			return CompletableFuture.completedFuture(true);
		}

		// Fallback: lookup offline user
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenApply(optData -> {
			if (optData.isEmpty()) {
				instance.debug("updateHellblockMessages: No UserData found for UUID: " + ownerId);
				return false;
			}

			final UserData ownerData = optData.get();
			final ProtectedRegion region = getRegion(world, ownerId, ownerData.getHellblockData().getIslandId());
			if (region == null) {
				instance.getPluginLogger()
						.severe("updateHellblockMessages: No WorldGuard region found for offline user: " + ownerId);
				return false;
			}

			instance.debug("updateHellblockMessages: Applying WorldGuard message flags for offline user: " + ownerId);
			applyEntryAndFarewellFlags(ownerData, region);
			return instance.getStorageManager().saveUserData(ownerData, true).thenApply(v -> true);
		}).handle((result, ex) -> {
			if (ex != null) {
				instance.getPluginLogger()
						.warn("updateHellblockMessages: Failed to update WorldGuard message flags for " + ownerId, ex);
			}
			return instance.getStorageManager().unlockUserData(ownerId).thenApply(x -> true);
		}).thenCompose(Function.identity());
	}

	/**
	 * Applies greeting and farewell WorldGuard flags to a player's protected
	 * region, based on their name and abandonment status.
	 * <p>
	 * If abandoned, different messages will be applied. If greeting/farewell
	 * messages are disabled in the config, the respective flags will be cleared.
	 *
	 * @param ownerData the user whose region flags are being updated
	 * @param region    the WorldGuard region associated with the user's island
	 */
	private void applyEntryAndFarewellFlags(@NotNull UserData ownerData, @NotNull ProtectedRegion region) {
		final String name = ownerData.getName();
		final boolean abandoned = ownerData.getHellblockData().isAbandoned();

		applyMessageFlag(ownerData, region, HellblockFlag.FlagType.GREET_MESSAGE,
				instance.getConfigManager().entryMessageEnabled(),
				abandoned ? MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key()
						: MessageConstants.HELLBLOCK_ENTRY_MESSAGE
								.arguments(AdventureHelper.miniMessageToComponent(name)).build().key(),
				"greet");

		applyMessageFlag(ownerData, region, HellblockFlag.FlagType.FAREWELL_MESSAGE,
				instance.getConfigManager().farewellMessageEnabled(),
				abandoned ? MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key()
						: MessageConstants.HELLBLOCK_FAREWELL_MESSAGE
								.arguments(AdventureHelper.miniMessageToComponent(name)).build().key(),
				"farewell");
	}

	/**
	 * Applies a message flag (greeting or farewell) to a region and updates
	 * protection data.
	 *
	 * @param ownerData  The user data of the region owner.
	 * @param region     The WorldGuard protected region.
	 * @param type       The type of message flag (GREET or FAREWELL).
	 * @param enabled    Whether the message is enabled.
	 * @param messageKey The translation key for the message.
	 * @param debugLabel The label to use for debugging output.
	 */
	private void applyMessageFlag(@NotNull UserData ownerData, @NotNull ProtectedRegion region,
			@NotNull HellblockFlag.FlagType type, boolean enabled, @NotNull String messageKey,
			@NotNull String debugLabel) {

		final StringFlag flag = convertToWorldGuardStringFlag(type);
		final String translatedMessage = enabled ? instance.getTranslationManager()
				.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(messageKey))
				.replace("<arg:0>", ownerData.getName()) : null;

		// Apply to region
		region.setFlag(flag, translatedMessage);
		instance.debug((enabled ? "Set" : "Cleared") + " " + debugLabel + " message WorldGuard flag for "
				+ ownerData.getUUID() + ": " + (translatedMessage != null ? translatedMessage : "null"));

		// Also store in protection data
		HellblockData hellblockData = ownerData.getHellblockData();
		hellblockData.setProtectionData(type, translatedMessage);
	}

	@Override
	public CompletableFuture<Boolean> lockHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData) {
		return instance.getScheduler().callSync(() -> {
			final ProtectedRegion region = getRegion(world, ownerData.getUUID(),
					ownerData.getHellblockData().getIslandId());

			if (region == null) {
				instance.getPluginLogger()
						.warn("lockHellblock: WorldGuard region not found when attempting to lock island for owner "
								+ ownerData.getUUID());
				return CompletableFuture.completedFuture(false);
			}

			final StateFlag entryFlag = convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY);
			region.setFlag(entryFlag, (!ownerData.getHellblockData().isLocked() ? null : StateFlag.State.DENY));

			instance.debug("lockHellblock: Set ENTRY WorldGuard flag for " + ownerData.getUUID() + " to "
					+ (ownerData.getHellblockData().isLocked() ? "DENY" : "ALLOW"));
			return CompletableFuture.completedFuture(true);
		});
	}

	@Override
	public CompletableFuture<Boolean> abandonIsland(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		if (getWorldGuardPlatform() == null) {
			return CompletableFuture
					.failedFuture(new IllegalStateException("abandonIsland: WorldGuard platform is not initialized."));
		}

		instance.debug("abandonIsland: Attempting to abandon WorldGuard region for UUID: " + ownerId);

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

			return instance.getScheduler().callSync(() -> {
				final ProtectedRegion region = getRegion(world, ownerId, data.getIslandId());
				if (region == null) {
					instance.getPluginLogger().warn(
							"abandonIsland: Could not get WorldGuard region when abandoning island for " + ownerId);
					return CompletableFuture.completedFuture(false);
				}

				// Clear owners and members
				region.getOwners().clear();
				region.getMembers().clear();

				// Apply WorldGuard flags
				region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.PVP), StateFlag.State.ALLOW);
				region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD), StateFlag.State.ALLOW);
				if (data.isLocked()) {
					region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY), StateFlag.State.ALLOW);
				}
				region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING), StateFlag.State.DENY);

				// Apply internal protection data
				data.setProtectionValue(new HellblockFlag(HellblockFlag.FlagType.PVP, HellblockFlag.AccessType.ALLOW));
				data.setProtectionValue(
						new HellblockFlag(HellblockFlag.FlagType.BUILD, HellblockFlag.AccessType.ALLOW));
				if (data.isLocked()) {
					data.setProtectionValue(
							new HellblockFlag(HellblockFlag.FlagType.ENTRY, HellblockFlag.AccessType.ALLOW));
				}
				data.setProtectionValue(
						new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING, HellblockFlag.AccessType.DENY));

				instance.debug("abandonIsland: Marked WorldGuard region as abandoned for " + ownerId);

				try {
					final RegionManager rm = getWorldGuardPlatform().getRegionContainer()
							.get(BukkitAdapter.adapt(world.bukkitWorld()));
					if (rm != null) {
						rm.save();
						instance.debug(
								"abandonIsland: Saved WorldGuard RegionManager after abandonment for " + ownerId);
					}
				} catch (Exception e) {
					instance.getPluginLogger()
							.severe("abandonIsland: Failed to save WorldGuard RegionManager for " + ownerId, e);
				}
				// Always save user data, even if region manager failed
				return instance.getStorageManager().saveUserData(ownerData, true).thenApply(v -> true);
			});
		}).handle((result, ex) -> {
			if (ex != null) {
				instance.getPluginLogger().warn("abandonIsland: Failed to abandon WorldGuard region for UUID " + ownerId
						+ ": " + ex.getMessage());
			}
			return instance.getStorageManager().unlockUserData(ownerId).thenApply(x -> true);
		}).thenCompose(Function.identity());
	}

	@Override
	public CompletableFuture<Boolean> restoreFlags(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		if (getWorldGuardPlatform() == null) {
			return CompletableFuture
					.failedFuture(new IllegalStateException("restoreFlags: WorldGuard platform is not initialized."));
		}

		instance.debug("restoreFlags: Restoring WorldGuard protection flags for UUID: " + ownerId);

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

			return instance.getScheduler().callSync(() -> {
				final ProtectedRegion region = getRegion(world, ownerId, data.getIslandId());
				if (region == null) {
					instance.getPluginLogger()
							.warn("restoreFlags: Could not get WorldGuard region when restoring flags for " + ownerId);
					return CompletableFuture.completedFuture(false);
				}

				// Apply owners and members
				region.getOwners().addPlayer(ownerId);
				data.getIslandMembers().forEach(region.getMembers()::addPlayer);

				// Apply WorldGuard flags
				region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.PVP), StateFlag.State.DENY);
				region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD), StateFlag.State.DENY);
				if (data.isLocked()) {
					region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY), StateFlag.State.DENY);
				}
				region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING), StateFlag.State.ALLOW);

				// Apply internal protection data
				data.setProtectionValue(new HellblockFlag(HellblockFlag.FlagType.PVP, HellblockFlag.AccessType.DENY));
				data.setProtectionValue(new HellblockFlag(HellblockFlag.FlagType.BUILD, HellblockFlag.AccessType.DENY));
				if (data.isLocked()) {
					data.setProtectionValue(
							new HellblockFlag(HellblockFlag.FlagType.ENTRY, HellblockFlag.AccessType.DENY));
				}
				data.setProtectionValue(
						new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING, HellblockFlag.AccessType.ALLOW));

				instance.debug("restoreFlags: Restored flags and members for WorldGuard region of " + ownerId);

				// Try saving the region — log issues, but don't block user data save
				try {
					final RegionManager rm = getWorldGuardPlatform().getRegionContainer()
							.get(BukkitAdapter.adapt(world.bukkitWorld()));
					if (rm != null) {
						rm.save();
						instance.debug("restoreFlags: Saved WorldGuard RegionManager after restore for " + ownerId);
					}
				} catch (Exception e) {
					instance.getPluginLogger()
							.severe("restoreFlags: Failed to save WorldGuard RegionManager for " + ownerId, e);
				}

				// Always save user data, even if region manager failed
				return instance.getStorageManager().saveUserData(ownerData, true).thenApply(v -> true);
			});
		}).handle((result, ex) -> {
			if (ex != null) {
				instance.getPluginLogger().warn("restoreFlags: Failed to restore WorldGuard flags for UUID " + ownerId
						+ ": " + ex.getMessage());
			}
			return instance.getStorageManager().unlockUserData(ownerId).thenApply(x -> true);
		}).thenCompose(Function.identity());
	}

	@Override
	public CompletableFuture<Boolean> changeHellblockFlag(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull HellblockFlag flag) {
		return instance.getScheduler().callSync(() -> {
			final ProtectedRegion region = getRegion(world, ownerData.getUUID(),
					ownerData.getHellblockData().getIslandId());
			if (region == null) {
				instance.getPluginLogger().warn(
						"WorldGuard region not found when attempting to change flag for owner " + ownerData.getUUID());
				return CompletableFuture.completedFuture(false);
			}

			final StateFlag wgFlag = convertToWorldGuardFlag(flag.getFlag());
			if (wgFlag == null) {
				instance.getPluginLogger()
						.warn("No matching WorldGuard flag for Hellblock flag type: " + flag.getFlag());
				return CompletableFuture.completedFuture(false);
			}

			StateFlag.State value = (flag.getStatus() == HellblockFlag.AccessType.ALLOW ? null : StateFlag.State.DENY);
			region.setFlag(wgFlag, value);
			instance.debug("Changed flag " + wgFlag.getName() + " to " + (value == null ? "ALLOW" : "DENY") + " for "
					+ ownerData.getUUID());
			return CompletableFuture.completedFuture(true);
		});
	}

	@Override
	public CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull HellblockWorld<?> world,
			@NotNull UUID ownerId) {
		if (getWorldGuardPlatform() == null) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("getMembersOfHellblockBounds: WorldGuard platform is not initialized."));
		}

		instance.debug("getMembersOfHellblockBounds: Fetching Hellblock members for island owner: " + ownerId);

		CompletableFuture<Set<UUID>> result = instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false)
				.thenCompose(optData -> {
					if (optData.isEmpty()) {
						instance.debug("getMembersOfHellblockBounds: No UserData found for UUID: " + ownerId);
						return CompletableFuture.completedFuture(Collections.emptySet());
					}

					final UserData ownerData = optData.get();

					return instance.getScheduler().callSyncImmediate(() -> {
						final ProtectedRegion region = getRegion(world, ownerId,
								ownerData.getHellblockData().getIslandId());
						if (region == null) {
							instance.getPluginLogger().warn(
									"getMembersOfHellblockBounds: Could not get WorldGuard region when retrieving members from owner "
											+ ownerId);
							throw new IllegalStateException("WorldGuard region not found for island owner: " + ownerId);
						}
						final Set<UUID> members = new HashSet<>(region.getMembers().getUniqueIds());
						instance.debug("Found " + members.size() + " member" + (members.size() == 1 ? "" : "s")
								+ " in WorldGuard region for " + ownerId);
						return members;
					});
				});

		return result.exceptionally(ex -> {
			instance.getPluginLogger().severe("Failed to fetch user data for getMembersOfHellblockBounds: " + ownerId,
					ex);
			return Collections.emptySet();
		});
	}

	@Override
	public CompletableFuture<Boolean> addMemberToHellblockBounds(@NotNull HellblockWorld<?> world,
			@NotNull UUID ownerId, @NotNull UUID memberId) {
		if (getWorldGuardPlatform() == null) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("addMemberToHellblockBounds: WorldGuard platform is not initialized."));
		}

		instance.debug("addMemberToHellblockBounds: Attempting to add member " + memberId + " to island " + ownerId);

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("addMemberToHellblockBounds: User data not found for island owner: " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();

			return instance.getScheduler().callSyncImmediate(() -> {
				final ProtectedRegion region = getRegion(world, ownerId, ownerData.getHellblockData().getIslandId());
				if (region == null) {
					instance.getPluginLogger()
							.warn("addMemberToHellblockBounds: Could not get WorldGuard region when adding member "
									+ memberId + " to owner " + ownerId);
					return false;
				}

				if (!region.getMembers().getUniqueIds().add(memberId)) {
					instance.debug("addMemberToHellblockBounds: Member " + memberId + " already present in region for "
							+ ownerId);
					return false;
				}

				instance.debug(
						"addMemberToHellblockBounds: Added member " + memberId + " to WorldGuard region of " + ownerId);

				try {
					final RegionManager rm = getWorldGuardPlatform().getRegionContainer()
							.get(BukkitAdapter.adapt(world.bukkitWorld()));
					if (rm != null) {
						rm.save();
						instance.debug(
								"addMemberToHellblockBounds: WorldGuard region saved after adding member " + memberId);
						return true;
					}
				} catch (Exception e) {
					instance.getPluginLogger()
							.severe("addMemberToHellblockBounds: Failed to save WorldGuard region after adding member "
									+ memberId, e);
					return false;
				}
				// Fallback: saving failed or RegionManager was null
				return false;
			});
		}).exceptionally(ex -> {
			instance.getPluginLogger().severe("Failed to fetch user data for addMemberToHellblockBounds: " + ownerId,
					ex);
			return false;
		});
	}

	@Override
	public CompletableFuture<Boolean> removeMemberFromHellblockBounds(@NotNull HellblockWorld<?> world,
			@NotNull UUID ownerId, @NotNull UUID memberId) {
		if (getWorldGuardPlatform() == null) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					"removeMemberFromHellblockBounds: WorldGuard platform is not initialized."));
		}

		instance.debug(
				"removeMemberFromHellblockBounds: Attempting to remove member " + memberId + " from island " + ownerId);

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenCompose(optData -> {
			if (optData.isEmpty()) {
				instance.debug("removeMemberFromHellblockBounds: User data not found for island owner: " + ownerId);
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();

			return instance.getScheduler().callSyncImmediate(() -> {
				final ProtectedRegion region = getRegion(world, ownerId, ownerData.getHellblockData().getIslandId());
				if (region == null) {
					instance.getPluginLogger().warn(
							"removeMemberFromHellblockBounds: Could not get WorldGuard region when removing member "
									+ memberId + " from owner " + ownerId);
					return false;
				}

				if (!region.getMembers().getUniqueIds().remove(memberId)) {
					instance.debug("removeMemberFromHellblockBounds: Member " + memberId
							+ " was not part of WorldGuard region for " + ownerId);
					return false;
				}

				instance.debug("removeMemberFromHellblockBounds: Removed member " + memberId
						+ " from WorldGuard region of " + ownerId);

				try {
					final RegionManager rm = getWorldGuardPlatform().getRegionContainer()
							.get(BukkitAdapter.adapt(world.bukkitWorld()));
					if (rm != null) {
						rm.save();
						instance.debug("removeMemberFromHellblockBounds: WorldGuard region saved after removing member "
								+ memberId);
						return true;
					}
				} catch (Exception e) {
					instance.getPluginLogger().severe(
							"removeMemberFromHellblockBounds: Failed to save WorldGuard region after removing member "
									+ memberId,
							e);
					return false;
				}
				// Fallback: saving failed or RegionManager was null
				return false;
			});
		}).exceptionally(ex -> {
			instance.getPluginLogger()
					.severe("Failed to fetch user data for removeMemberFromHellblockBounds: " + ownerId, ex);
			return false;
		});
	}

	@Override
	@NotNull
	public BlockVector3 getProtectionVectorUpperCorner(@NotNull UserData ownerData) {
		final BoundingBox bounds = ownerData.getHellblockData().getBoundingBox();

		if (bounds == null) {
			throw new IllegalStateException("Bounding box not set. Did you call expandBoundingBox()?");
		}

		instance.debug("Retrieved upper protection vector for " + ownerData.getUUID());

		return BlockVector3.at(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
	}

	@Override
	@NotNull
	public BlockVector3 getProtectionVectorLowerCorner(@NotNull UserData ownerData) {
		final BoundingBox bounds = ownerData.getHellblockData().getBoundingBox();

		if (bounds == null) {
			throw new IllegalStateException("Bounding box not set. Did you call expandBoundingBox()?");
		}

		instance.debug("Retrieved lower protection vector for " + ownerData.getUUID());

		return BlockVector3.at(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
	}

	/**
	 * Retrieves the WorldGuard protected region for a specific island.
	 *
	 * @param world    the world where the region is located
	 * @param ownerId  the island owner's UUID
	 * @param islandId the island ID (used in the region name)
	 * @return the {@link ProtectedRegion} if found, or {@code null} otherwise
	 * @throws IllegalStateException if WorldGuard platform or region manager is
	 *                               unavailable
	 */
	@Nullable
	private ProtectedRegion getRegion(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId, int islandId) {
		if (getWorldGuardPlatform() == null) {
			throw new IllegalStateException("WorldGuard platform is not initialized.");
		}

		final RegionManager regionManager = getWorldGuardPlatform().getRegionContainer()
				.get(BukkitAdapter.adapt(world.bukkitWorld()));
		if (regionManager == null) {
			throw new IllegalStateException("WorldGuard RegionManager not found for world: " + world.worldName());
		}

		final String regionId = "%s_%s".formatted(ownerId.toString(), islandId);
		instance.debug("Resolved WorldGuard region ID: " + regionId);
		return regionManager.getRegion(regionId);
	}

	/**
	 * Converts a {@link HellblockFlag.FlagType} to its corresponding WorldGuard
	 * {@link StateFlag}. Only applies to non-string flag types.
	 *
	 * @param flag the Hellblock flag type to convert
	 * @return the corresponding WorldGuard flag, or {@code null} if not applicable
	 * @throws IllegalArgumentException if the flag type has no WorldGuard
	 *                                  equivalent
	 */
	@Nullable
	private StateFlag convertToWorldGuardFlag(@NotNull FlagType flag) {
		return switch (flag) {
		case BLOCK_BREAK -> Flags.BLOCK_BREAK;
		case BLOCK_PLACE -> Flags.BLOCK_PLACE;
		case BUILD -> Flags.BUILD;
		case CHEST_ACCESS -> Flags.CHEST_ACCESS;
		case CHORUS_TELEPORT -> Flags.CHORUS_TELEPORT;
		case DAMAGE_ANIMALS -> Flags.DAMAGE_ANIMALS;
		case DESTROY_VEHICLE -> Flags.DESTROY_VEHICLE;
		case ENDERPEARL -> Flags.ENDERPEARL;
		case ENDER_BUILD -> Flags.ENDER_BUILD;
		case ENTRY -> Flags.ENTRY;
		case FALL_DAMAGE -> Flags.FALL_DAMAGE;
		case FIREWORK_DAMAGE -> Flags.FIREWORK_DAMAGE;
		case GHAST_FIREBALL -> Flags.GHAST_FIREBALL;
		case HEALTH_REGEN -> Flags.HEALTH_REGEN;
		case HUNGER_DRAIN -> Flags.HUNGER_DRAIN;
		case INTERACT -> Flags.INTERACT;
		case INVINCIBILITY -> Flags.INVINCIBILITY;
		case ITEM_FRAME_ROTATE -> Flags.ITEM_FRAME_ROTATE;
		case LIGHTER -> Flags.LIGHTER;
		case MOB_DAMAGE -> Flags.MOB_DAMAGE;
		case MOB_SPAWNING -> Flags.MOB_SPAWNING;
		case PLACE_VEHICLE -> Flags.PLACE_VEHICLE;
		case POTION_SPLASH -> Flags.POTION_SPLASH;
		case PVP -> Flags.PVP;
		case RESPAWN_ANCHORS -> Flags.RESPAWN_ANCHORS;
		case RIDE -> Flags.RIDE;
		case SLEEP -> Flags.SLEEP;
		case SNOWMAN_TRAILS -> Flags.SNOWMAN_TRAILS;
		case TNT -> Flags.TNT;
		case TRAMPLE_BLOCKS -> Flags.TRAMPLE_BLOCKS;
		case USE -> Flags.USE;
		case USE_ANVIL -> Flags.USE_ANVIL;
		case USE_DRIPLEAF -> Flags.USE_DRIPLEAF;
		case WIND_CHARGE_BURST -> Flags.WIND_CHARGE_BURST;
		case CREEPER_EXPLOSION -> Flags.CREEPER_EXPLOSION;
		case ENTITY_PAINTING_DESTROY -> Flags.ENTITY_PAINTING_DESTROY;
		case ENTITY_ITEM_FRAME_DESTROY -> Flags.ENTITY_ITEM_FRAME_DESTROY;
		case ITEM_PICKUP -> Flags.ITEM_PICKUP;
		case ITEM_DROP -> Flags.ITEM_DROP;
		case BREEZE_WIND_CHARGE -> Flags.BREEZE_WIND_CHARGE;
		case EXP_DROPS -> Flags.EXP_DROPS;
		case RAVAGER_RAVAGE -> Flags.RAVAGER_RAVAGE;
		default ->
			throw new IllegalArgumentException("The flag you defined can't be converted into a WorldGuard flag.");
		};
	}

	/**
	 * Converts a {@link HellblockFlag.FlagType} to a WorldGuard {@link StringFlag}.
	 * Only applies to flags such as greeting and farewell messages.
	 *
	 * @param flag the Hellblock string flag type to convert
	 * @return the corresponding WorldGuard {@link StringFlag}
	 * @throws IllegalArgumentException if the flag is not a convertible string flag
	 * @throws IllegalStateException    if the resolved flag is not an instance of
	 *                                  {@link StringFlag}
	 */
	@Nullable
	private StringFlag convertToWorldGuardStringFlag(@NotNull FlagType flag) {
		Flag<?> retrievedFlag = switch (flag) {
		case GREET_MESSAGE -> getFlagByName("greet-message");
		case FAREWELL_MESSAGE -> getFlagByName("farewell-message");
		default -> throw new IllegalArgumentException(
				"The string flag you defined can't be converted into a WorldGuard flag.");
		};

		if (retrievedFlag instanceof StringFlag stringFlag) {
			return stringFlag;
		}

		throw new IllegalStateException("Retrieved flag is not a StringFlag.");
	}

	/**
	 * Looks up a WorldGuard {@link Flag} by name from the global registry.
	 *
	 * @param name the name of the flag to retrieve
	 * @return the flag instance, or {@code null} if not found
	 */
	@Nullable
	private Flag<?> getFlagByName(@NotNull String name) {
		return WorldGuard.getInstance().getFlagRegistry().get(name);
	}
}