package com.swiftlicious.hellblock.protection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
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

	/**
	 * Executes a task synchronously on the main server thread.
	 * <p>
	 * If already on the primary thread, runs the task immediately. Otherwise,
	 * schedules it to be executed synchronously via the plugin's scheduler.
	 *
	 * @param task the task to execute
	 */
	private void runSync(@NotNull Runnable task) {
		if (Bukkit.isPrimaryThread()) {
			task.run();
		} else {
			instance.getScheduler().executeSync(task);
		}
	}

	/**
	 * Executes a {@link Callable} task synchronously and returns a
	 * {@link CompletableFuture} that completes with the task's result or exception.
	 * <p>
	 * If already on the primary thread, the task runs immediately. Otherwise, it is
	 * scheduled to run synchronously via the plugin's scheduler.
	 *
	 * @param task the task to execute
	 * @param <T>  the type of result returned by the task
	 * @return a {@link CompletableFuture} representing the result or failure of the
	 *         task
	 */
	private <T> CompletableFuture<T> runSyncFuture(@NotNull Callable<T> task) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		if (Bukkit.isPrimaryThread()) {
			try {
				future.complete(task.call());
			} catch (Exception ex) {
				future.completeExceptionally(ex);
			}
		} else {
			instance.getScheduler().executeSync(() -> {
				try {
					future.complete(task.call());
				} catch (Exception ex) {
					future.completeExceptionally(ex);
				}
			});
		}
		return future;
	}

	@Override
	public CompletableFuture<Void> protectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData) {
		if (getWorldGuardPlatform() == null) {
			instance.debug("WorldGuard platform is not initialized.");
			return CompletableFuture.failedFuture(new IllegalStateException("WorldGuard not available"));
		}

		try {
			instance.debug("Starting WorldGuard protection for island ID " + ownerData.getHellblockData().getIslandId()
					+ " (owner: " + ownerData.getUUID() + ") in world " + world.worldName());

			final RegionContainer regionContainer = getWorldGuardPlatform().getRegionContainer();
			final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world.bukkitWorld()));

			if (regionManager == null) {
				return CompletableFuture
						.failedFuture(new IllegalStateException("No RegionManager for world: " + world.worldName()));
			}

			final HellblockData hellblockData = ownerData.getHellblockData();
			final int islandId = hellblockData.getIslandId();
			if (islandId <= 0) {
				throw new IllegalStateException("Invalid island ID for player: " + ownerData.getName());
			}

			// --- Determine Bounding Box ---
			BoundingBox boundingBox;

			if (instance.getConfigManager().perPlayerWorlds()) {
				instance.debug("WorldGuard: Using per-player world bounding box.");

				double radius = instance.getUpgradeManager().getDefaultValue(IslandUpgradeType.PROTECTION_RANGE)
						.intValue();

				double minY = world.bukkitWorld().getMinHeight();
				double maxY = world.bukkitWorld().getMaxHeight();

				boundingBox = new BoundingBox(-radius, minY, -radius, radius, maxY, radius);

			} else if (hellblockData.getPreservedBoundingBox() != null) {
				boundingBox = hellblockData.getPreservedBoundingBox();
				instance.debug("WorldGuard: Using preserved bounding box from previous reset.");

			} else {
				boundingBox = instance.getPlacementDetector().computeSpiralBoundingBoxForIsland(islandId, world);
				instance.debug("WorldGuard: Computed bounding box from spiral logic.");
			}

			// Apply bounding box to player data
			hellblockData.setBoundingBox(boundingBox);
			instance.getPlacementDetector().cacheIslandBoundingBox(islandId, boundingBox);

			// --- Create WorldGuard Region from Bounding Box ---
			BlockVector3 min = BlockVector3.at(Math.floor(boundingBox.getMinX()), Math.floor(boundingBox.getMinY()),
					Math.floor(boundingBox.getMinZ()));

			BlockVector3 max = BlockVector3.at(Math.ceil(boundingBox.getMaxX()), Math.ceil(boundingBox.getMaxY()),
					Math.ceil(boundingBox.getMaxZ()));

			String regionId = "%s_%s".formatted(ownerData.getUUID(), islandId);
			ProtectedRegion region = new ProtectedCuboidRegion(regionId, min, max);

			region.setParent(regionManager.getRegion(ProtectedRegion.GLOBAL_REGION));
			DefaultDomain owners = new DefaultDomain();
			owners.addPlayer(ownerData.getUUID());
			region.setOwners(owners);
			region.setPriority(100);

			// --- Remove any overlapping user-defined regions ---
			ApplicableRegionSet existingRegions = regionManager.getApplicableRegions(min);
			for (ProtectedRegion regionCheck : existingRegions) {
				if (!regionCheck.getId().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION)) {
					instance.debug("Removing overlapping region: " + regionCheck.getId());
					regionManager.removeRegion(regionCheck.getId());
				}
			}

			// --- Apply region ---
			regionManager.addRegion(region);
			regionManager.save();

			instance.debug("WorldGuard region '" + regionId + "' saved for island ID: " + islandId);

			updateHellblockMessages(world, ownerData.getUUID());
			// Wait for lock status to complete before returning
			return instance.getProtectionManager().changeLockStatus(world, ownerData.getUUID())
					.thenRun(() -> instance.debug("WorldGuard: Lock status updated for " + ownerData.getName()));
		} catch (Exception ex) {
			instance.getPluginLogger().severe("WorldGuard protection failed for " + ownerData.getName(), ex);
			return CompletableFuture.failedFuture(ex);
		}
	}

	@Override
	public CompletableFuture<Void> unprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		if (getWorldGuardPlatform() == null) {
			instance.debug("WorldGuard platform is not initialized.");
			return CompletableFuture.failedFuture(new IllegalStateException("WorldGuard not available"));
		}

		final RegionContainer regionContainer = getWorldGuardPlatform().getRegionContainer();
		final RegionManager maybeRm = regionContainer.get(BukkitAdapter.adapt(world.bukkitWorld()));
		if (maybeRm == null) {
			instance.getPluginLogger().severe("No region manager for world: " + world.worldName());
			return CompletableFuture
					.failedFuture(new IllegalStateException("No region manager for world: " + world.worldName()));
		}
		final RegionManager regionManager = maybeRm;

		final CompletableFuture<Void> unprotection = new CompletableFuture<>();

		instance.debug("Starting unprotection for island owner: " + ownerId + " in world " + world.worldName());

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("No user data found for " + ownerId + ". Skipping unprotection.");
						unprotection.complete(null); // nothing to do
						return;
					}
					final UserData offlineUser = result.get();

					runSync(() -> {
						try {
							instance.getProtectionManager().clearHellblockEntities(world.bukkitWorld(),
									offlineUser.getHellblockData().getBoundingBox());

							final String regionName = "%s_%s".formatted(ownerId.toString(),
									offlineUser.getHellblockData().getIslandId());
							final ProtectedRegion region = regionManager.getRegion(regionName);

							if (region != null) {
								regionManager.removeRegion(regionName, RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
								regionManager.save();
								instance.debug("Region " + regionName + " successfully removed.");
							} else {
								instance.debug("Region " + regionName + " not found during unprotection.");
							}
							unprotection.complete(null);
						} catch (Exception ex) {
							instance.getPluginLogger().severe("Unable to unprotect " + ownerId + "'s hellblock!", ex);
							unprotection.completeExceptionally(ex);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to fetch offline user for unprotect: " + ownerId, ex);
					unprotection.completeExceptionally(ex);
					return null;
				});

		return unprotection;
	}

	@Override
	public CompletableFuture<Void> reprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull UserData transfereeData) {
		if (getWorldGuardPlatform() == null) {
			instance.debug("WorldGuard platform is not initialized.");
			return CompletableFuture.failedFuture(new IllegalStateException("WorldGuard not available"));
		}

		final RegionContainer regionContainer = getWorldGuardPlatform().getRegionContainer();
		final RegionManager maybeRm = regionContainer.get(BukkitAdapter.adapt(world.bukkitWorld()));
		if (maybeRm == null) {
			instance.getPluginLogger().severe("No region manager for world: " + world.worldName());
			return CompletableFuture
					.failedFuture(new IllegalStateException("No region manager for world: " + world.worldName()));
		}
		final RegionManager regionManager = maybeRm;

		instance.debug("Starting reprotection: Transferring island from " + ownerData.getUUID() + " to "
				+ transfereeData.getUUID());

		return runSyncFuture(() -> {
			try {
				final ProtectedRegion region = getRegion(world, ownerData.getUUID(),
						ownerData.getHellblockData().getIslandId());
				if (region == null) {
					throw new IllegalStateException(
							"Could not get the WorldGuard region for owner: " + ownerData.getUUID());
				}

				instance.debug("Found existing region: " + region.getId());

				// Transfer ownership and membership
				final DefaultDomain owners = region.getOwners();
				final DefaultDomain members = region.getMembers();

				if (!owners.getUniqueIds().contains(transfereeData.getUUID())) {
					owners.getUniqueIds().add(transfereeData.getUUID());
					instance.debug("Added transferee " + transfereeData.getUUID() + " to region owners.");
				}
				owners.getUniqueIds().remove(ownerData.getUUID());
				instance.debug("Removed old owner " + ownerData.getUUID() + " from region owners.");

				if (!members.getUniqueIds().contains(ownerData.getUUID())) {
					members.getUniqueIds().add(ownerData.getUUID());
					instance.debug("Added previous owner " + ownerData.getUUID() + " as a region member.");
				}
				members.getUniqueIds().remove(transfereeData.getUUID());

				// Create new region with transferee ID
				final String newRegionId = "%s_%s".formatted(transfereeData.getUUID(),
						transfereeData.getHellblockData().getIslandId());
				final ProtectedRegion renamedRegion = new ProtectedCuboidRegion(newRegionId,
						getProtectionVectorUpperCorner(ownerData), getProtectionVectorLowerCorner(ownerData));

				instance.debug("Created new region with ID: " + newRegionId);

				// Copy attributes
				renamedRegion.setOwners(owners);
				renamedRegion.setMembers(members);
				renamedRegion.setFlags(region.getFlags());
				renamedRegion.setPriority(region.getPriority());
				renamedRegion.setParent(region.getParent());

				regionManager.addRegion(renamedRegion);
				regionManager.removeRegion(region.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
				instance.debug("Replaced old region: " + region.getId() + " with new region: " + newRegionId);

				regionManager.save();
				instance.debug("Region manager saved after reprotection.");

				// Update bounding box
				transfereeData.getHellblockData().setBoundingBox(new BoundingBox(
						(double) renamedRegion.getMinimumPoint().x(), (double) renamedRegion.getMinimumPoint().y(),
						(double) renamedRegion.getMinimumPoint().z(), (double) renamedRegion.getMaximumPoint().x(),
						(double) renamedRegion.getMaximumPoint().y(), (double) renamedRegion.getMaximumPoint().z()));
				instance.getPlacementDetector().cacheIslandBoundingBox(transfereeData.getHellblockData().getIslandId(),
						transfereeData.getHellblockData().getBoundingBox());
				instance.debug("Updated bounding box for transferee: " + transfereeData.getUUID());

				updateHellblockMessages(world, transfereeData.getUUID());
				instance.getProtectionManager().changeLockStatus(world, transfereeData.getUUID());

				return null;
			} catch (Exception ex) {
				instance.getPluginLogger()
						.severe("Unable to reprotect %s's hellblock!".formatted(transfereeData.getName()), ex);
				throw ex;
			}
		});
	}

	@Override
	public void updateHellblockMessages(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		// First try online
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(ownerId);
		if (onlineUser.isPresent()) {
			final UserData user = onlineUser.get();
			final ProtectedRegion region = getRegion(world, ownerId, user.getHellblockData().getIslandId());
			if (region == null) {
				instance.getPluginLogger().severe("No WorldGuard region found for online user " + ownerId);
				return;
			}
			instance.debug("Applying message flags for online user: " + ownerId);
			applyEntryAndFarewellFlags(user, region);
			return;
		}

		// Fallback: offline lookup
		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("No cached user found for updateHellblockMessages: " + ownerId);
						return;
					}
					final UserData user = result.get();
					final ProtectedRegion region = getRegion(world, ownerId, user.getHellblockData().getIslandId());
					if (region == null) {
						instance.getPluginLogger().severe("No WorldGuard region found for offline user " + ownerId);
						return;
					}
					instance.debug("Applying message flags for offline user: " + ownerId);
					applyEntryAndFarewellFlags(user, region);
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to update hellblock messages for " + ownerId, ex);
					return null;
				});
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
		final StringFlag greetFlag = convertToWorldGuardStringFlag(HellblockFlag.FlagType.GREET_MESSAGE);
		final StringFlag farewellFlag = convertToWorldGuardStringFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE);

		// Entry message
		if (instance.getConfigManager().entryMessageEnabled()) {
			String entryMessage = abandoned
					? instance.getTranslationManager()
							.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
									MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key()))
					: instance.getTranslationManager().miniMessageTranslation(
							AdventureHelper.legacyToMiniMessage(MessageConstants.HELLBLOCK_ENTRY_MESSAGE
									.arguments(AdventureHelper.miniMessageToComponent(name)).build().key()));
			entryMessage = entryMessage.replace("<arg:0>", name);
			region.setFlag(greetFlag, entryMessage);
			instance.debug("Set greet message flag for " + ownerData.getUUID() + ": " + entryMessage);
		} else {
			region.setFlag(greetFlag, null);
			instance.debug("Greet message disabled. Cleared greet flag for " + ownerData.getUUID());
		}

		// Farewell message
		if (instance.getConfigManager().farewellMessageEnabled()) {
			String farewellMessage = abandoned
					? instance.getTranslationManager()
							.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
									MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key()))
					: instance.getTranslationManager().miniMessageTranslation(
							AdventureHelper.legacyToMiniMessage(MessageConstants.HELLBLOCK_FAREWELL_MESSAGE
									.arguments(AdventureHelper.miniMessageToComponent(name)).build().key()));
			farewellMessage = farewellMessage.replace("<arg:0>", name);
			region.setFlag(farewellFlag, farewellMessage);
			instance.debug("Set farewell message flag for " + ownerData.getUUID() + ": " + farewellMessage);
		} else {
			region.setFlag(farewellFlag, null);
			instance.debug("Farewell message disabled. Cleared farewell flag for " + ownerData.getUUID());
		}
	}

	@Override
	public void lockHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData) {
		runSync(() -> {
			final ProtectedRegion region = getRegion(world, ownerData.getUUID(),
					ownerData.getHellblockData().getIslandId());
			if (region == null) {
				instance.getPluginLogger()
						.warn("Region not found when attempting to lock for owner " + ownerData.getUUID());
				return;
			}
			final StateFlag entryFlag = convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY);
			region.setFlag(entryFlag, (!ownerData.getHellblockData().isLocked() ? null : StateFlag.State.DENY));

			instance.debug("Set ENTRY flag for " + ownerData.getUUID() + " to "
					+ (ownerData.getHellblockData().isLocked() ? "DENY" : "ALLOW"));
		});
	}

	@Override
	public void abandonIsland(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		if (getWorldGuardPlatform() == null) {
			throw new IllegalStateException("WorldGuard platform is not initialized.");
		}

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("No user data found for abandonIsland: " + ownerId);
						return;
					}

					final UserData offlineUser = result.get();
					if (!offlineUser.getHellblockData().isAbandoned()) {
						instance.debug("User " + ownerId + " is not marked as abandoned. Skipping abandonIsland.");
						return;
					}

					runSync(() -> {
						final ProtectedRegion region = getRegion(world, ownerId,
								offlineUser.getHellblockData().getIslandId());
						if (region == null) {
							instance.getPluginLogger()
									.warn("Could not get WorldGuard region when abandoning for " + ownerId);
							return;
						}

						region.getOwners().clear();
						region.getMembers().clear();

						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.PVP), StateFlag.State.ALLOW);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD), StateFlag.State.ALLOW);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY), StateFlag.State.ALLOW);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING),
								StateFlag.State.DENY);

						instance.debug("Marked region abandoned for " + ownerId);

						try {
							final RegionManager rm = getWorldGuardPlatform().getRegionContainer()
									.get(BukkitAdapter.adapt(world.bukkitWorld()));
							if (rm != null) {
								rm.save();
								instance.debug("Saved region manager after abandon for " + ownerId);
							}
						} catch (Exception e) {
							instance.getPluginLogger()
									.severe("Failed to save region manager after abandoning island for " + ownerId, e);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to fetch offline user for abandonIsland: " + ownerId, ex);
					return null;
				});
	}

	@Override
	public void restoreFlags(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		if (getWorldGuardPlatform() == null) {
			throw new IllegalStateException("WorldGuard platform is not initialized.");
		}

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("No user data found for restoreFlags: " + ownerId);
						return;
					}

					final UserData offlineUser = result.get();
					if (offlineUser.getHellblockData().isAbandoned()) {
						instance.debug("User " + ownerId + " is still abandoned. Skipping restoreFlags.");
						return;
					}

					runSync(() -> {
						final ProtectedRegion region = getRegion(world, ownerId,
								offlineUser.getHellblockData().getIslandId());
						if (region == null) {
							instance.getPluginLogger()
									.warn("Could not get WorldGuard region when restoring for " + ownerId);
							return;
						}

						region.getOwners().addPlayer(ownerId);
						offlineUser.getHellblockData().getIslandMembers().forEach(region.getMembers()::addPlayer);

						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.PVP), StateFlag.State.DENY);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD), StateFlag.State.DENY);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY), StateFlag.State.ALLOW);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING),
								StateFlag.State.ALLOW);

						instance.debug("Restored flags and members for region of " + ownerId);

						try {
							final RegionManager rm = getWorldGuardPlatform().getRegionContainer()
									.get(BukkitAdapter.adapt(world.bukkitWorld()));
							if (rm != null) {
								rm.save();
								instance.debug("Saved region manager after restore for " + ownerId);
							}
						} catch (Exception e) {
							instance.getPluginLogger()
									.severe("Failed to save region manager after restoring island for " + ownerId, e);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to fetch offline user for restoreIsland: " + ownerId, ex);
					return null;
				});
	}

	@Override
	public void changeHellblockFlag(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull HellblockFlag flag) {
		runSync(() -> {
			final ProtectedRegion region = getRegion(world, ownerData.getUUID(),
					ownerData.getHellblockData().getIslandId());
			if (region == null) {
				instance.getPluginLogger()
						.warn("Region not found when attempting to change flag for owner " + ownerData.getUUID());
				return;
			}

			final StateFlag wgFlag = convertToWorldGuardFlag(flag.getFlag());
			if (wgFlag != null) {
				StateFlag.State value = (flag.getStatus() == HellblockFlag.AccessType.ALLOW ? null
						: StateFlag.State.DENY);
				region.setFlag(wgFlag, value);
				instance.debug("Changed flag " + wgFlag.getName() + " to " + (value == null ? "ALLOW" : "DENY")
						+ " for " + ownerData.getUUID());
			} else {
				instance.debug("No matching WorldGuard flag for Hellblock flag type: " + flag.getFlag());
			}
		});
	}

	@Override
	public CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull HellblockWorld<?> world,
			@NotNull UUID ownerId) {
		if (getWorldGuardPlatform() == null) {
			instance.debug("WorldGuard platform is not initialized.");
			return CompletableFuture.failedFuture(new IllegalStateException("WorldGuard not available"));
		}

		final CompletableFuture<Set<UUID>> future = new CompletableFuture<>();

		instance.debug("Fetching Hellblock members for island owner: " + ownerId);
		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("No user data found for owner: " + ownerId);
						future.complete(Collections.emptySet());
						return;
					}
					final UserData offlineUser = result.get();
					runSync(() -> {
						try {
							final ProtectedRegion region = getRegion(world, ownerId,
									offlineUser.getHellblockData().getIslandId());
							if (region == null) {
								instance.debug("No WorldGuard region found for owner: " + ownerId);
								future.completeExceptionally(
										new IllegalStateException("Region not found for owner " + ownerId));
								return;
							}
							final Set<UUID> members = new HashSet<>(region.getMembers().getUniqueIds());
							instance.debug("Found " + members.size() + " members in region for " + ownerId);
							future.complete(members);
						} catch (Exception ex) {
							instance.getPluginLogger().severe("Error retrieving members of region for " + ownerId, ex);
							future.completeExceptionally(ex);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to get cached user data for " + ownerId, ex);
					future.completeExceptionally(ex);
					return null;
				});
		return future;
	}

	@Override
	public void addMemberToHellblockBounds(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId,
			@NotNull UUID memberId) {
		if (getWorldGuardPlatform() == null) {
			throw new IllegalStateException("WorldGuard platform is not initialized.");
		}

		instance.debug("Attempting to add member " + memberId + " to island " + ownerId);
		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("User data not found for island owner: " + ownerId);
						return;
					}
					final UserData offlineUser = result.get();
					runSync(() -> {
						final ProtectedRegion region = getRegion(world, ownerId,
								offlineUser.getHellblockData().getIslandId());
						if (region == null) {
							instance.getPluginLogger().warn(
									"Could not get region when adding member " + memberId + " to owner " + ownerId);
							return;
						}
						if (region.getMembers().getUniqueIds().add(memberId)) {
							instance.debug("Added member " + memberId + " to region of " + ownerId);
							try {
								final RegionManager rm = getWorldGuardPlatform().getRegionContainer()
										.get(BukkitAdapter.adapt(world.bukkitWorld()));
								if (rm != null) {
									rm.save();
									instance.debug("Region saved after adding member " + memberId);
								}
							} catch (Exception e) {
								instance.getPluginLogger()
										.severe("Failed to save region after adding member " + memberId, e);
							}
						} else {
							instance.debug("Member " + memberId + " already present in region for " + ownerId);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger()
							.severe("Failed to fetch user data for addMemberToHellblockBounds: " + ownerId, ex);
					return null;
				});
	}

	@Override
	public void removeMemberFromHellblockBounds(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId,
			@NotNull UUID memberId) {
		if (getWorldGuardPlatform() == null) {
			throw new IllegalStateException("WorldGuard platform is not initialized.");
		}

		instance.debug("Attempting to remove member " + memberId + " from island " + ownerId);
		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("User data not found for island owner: " + ownerId);
						return;
					}
					final UserData offlineUser = result.get();
					runSync(() -> {
						final ProtectedRegion region = getRegion(world, ownerId,
								offlineUser.getHellblockData().getIslandId());
						if (region == null) {
							instance.getPluginLogger().warn(
									"Could not get region when removing member " + memberId + " from owner " + ownerId);
							return;
						}
						if (region.getMembers().getUniqueIds().remove(memberId)) {
							instance.debug("Removed member " + memberId + " from region of " + ownerId);
							try {
								final RegionManager rm = getWorldGuardPlatform().getRegionContainer()
										.get(BukkitAdapter.adapt(world.bukkitWorld()));
								if (rm != null) {
									rm.save();
									instance.debug("Region saved after removing member " + memberId);
								}
							} catch (Exception e) {
								instance.getPluginLogger()
										.severe("Failed to save region after removing member " + memberId, e);
							}
						} else {
							instance.debug("Member " + memberId + " was not part of region for " + ownerId);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger()
							.severe("Failed to fetch user data for removeMemberFromHellblockBounds: " + ownerId, ex);
					return null;
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
			throw new IllegalStateException("RegionManager not found for world: " + world.worldName());
		}

		final String regionId = "%s_%s".formatted(ownerId.toString(), islandId);
		instance.debug("Resolved region ID: " + regionId);
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