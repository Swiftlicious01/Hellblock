package com.swiftlicious.hellblock.protection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.World;
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
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;

import net.kyori.adventure.text.Component;

public class WorldGuardHook implements IslandProtection {

	private static WorldGuardPlatform worldGuardPlatform = null;

	protected final HellblockPlugin instance;

	public WorldGuardHook(HellblockPlugin plugin) {
		instance = plugin;
	}

	public static boolean isWorking() {
		try {
			worldGuardPlatform = WorldGuard.getInstance().getPlatform();
			return worldGuardPlatform != null;
		} catch (Throwable t) {
			HellblockPlugin.getInstance().getPluginLogger().severe(
					"WorldGuard threw an error during initializing, make sure it's updated and API compatible(Must be 7.0 or higher)",
					t);
			return false;
		}
	}

	private void runSync(@NotNull Runnable task) {
		if (Bukkit.isPrimaryThread()) {
			task.run();
		} else {
			instance.getScheduler().executeSync(task);
		}
	}

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
	public CompletableFuture<Void> protectHellblock(@NotNull World world, @NotNull UserData owner) {
		if (worldGuardPlatform == null) {
			instance.getPluginLogger().severe("WorldGuard platform is unavailable!");
			return CompletableFuture.failedFuture(new IllegalStateException("WorldGuard not available"));
		}

		try {
			final RegionContainer regionContainer = worldGuardPlatform.getRegionContainer();
			final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));

			if (regionManager == null) {
				return CompletableFuture
						.failedFuture(new IllegalStateException("No region manager for world: " + world.getName()));
			}

			// Build new region
			final String regionId = "%s_%s".formatted(owner.getUUID(), owner.getHellblockData().getID());
			final ProtectedRegion region = new ProtectedCuboidRegion(regionId, getProtectionVectorLeft(owner),
					getProtectionVectorRight(owner));

			// Assign global parent & owner
			region.setParent(regionManager.getRegion(ProtectedRegion.GLOBAL_REGION));
			final DefaultDomain owners = new DefaultDomain();
			owners.addPlayer(owner.getUUID());
			region.setOwners(owners);
			region.setPriority(100);

			// Store bounding box in user data
			owner.getHellblockData()
					.setBoundingBox(new BoundingBox((double) region.getMinimumPoint().x(),
							(double) region.getMinimumPoint().y(), (double) region.getMinimumPoint().z(),
							(double) region.getMaximumPoint().x(), (double) region.getMaximumPoint().y(),
							(double) region.getMaximumPoint().z()));

			// Remove old non-global regions at same location
			final ApplicableRegionSet existingRegions = regionManager
					.getApplicableRegions(BlockVector3.at(owner.getHellblockData().getHellblockLocation().getX(),
							owner.getHellblockData().getHellblockLocation().getY(),
							owner.getHellblockData().getHellblockLocation().getZ()));
			for (ProtectedRegion regionCheck : existingRegions) {
				if (!regionCheck.getId().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION)) {
					regionManager.removeRegion(regionCheck.getId());
				}
			}

			// Add and save
			regionManager.addRegion(region);
			regionManager.save();

			// Update region messages + lock
			updateHellblockMessages(world, owner.getUUID());
			instance.getProtectionManager().changeLockStatus(world, owner.getUUID());

			return CompletableFuture.completedFuture(null);
		} catch (Exception ex) {
			instance.getPluginLogger().severe("Unable to protect %s's hellblock!".formatted(owner.getName()), ex);
			return CompletableFuture.failedFuture(ex);
		}
	}

	@Override
	public CompletableFuture<Void> unprotectHellblock(@NotNull World world, @NotNull UUID id) {
		if (worldGuardPlatform == null) {
			instance.getPluginLogger().severe("WorldGuard platform is unavailable!");
			return CompletableFuture.failedFuture(new IllegalStateException("WorldGuard not available"));
		}

		final RegionContainer regionContainer = worldGuardPlatform.getRegionContainer();
		final RegionManager maybeRm = regionContainer.get(BukkitAdapter.adapt(world));
		if (maybeRm == null) {
			instance.getPluginLogger().severe("No region manager for world: " + world.getName());
			return CompletableFuture
					.failedFuture(new IllegalStateException("No region manager for world: " + world.getName()));
		}
		final RegionManager regionManager = maybeRm;

		final CompletableFuture<Void> unprotection = new CompletableFuture<>();

		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						unprotection.complete(null); // nothing to do
						return;
					}
					final UserData offlineUser = result.get();
					// schedule WG + Bukkit changes on main thread
					runSync(() -> {
						try {
							instance.getProtectionManager().clearHellblockEntities(world,
									offlineUser.getHellblockData().getBoundingBox());

							final String regionName = "%s_%s".formatted(id.toString(),
									offlineUser.getHellblockData().getID());
							final ProtectedRegion region = regionManager.getRegion(regionName);
							if (region != null) {
								regionManager.removeRegion(regionName, RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
								regionManager.save();
							}
							unprotection.complete(null);
						} catch (Exception ex) {
							instance.getPluginLogger().severe("Unable to unprotect " + id + "'s hellblock!", ex);
							unprotection.completeExceptionally(ex);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to fetch offline user for unprotect: " + id, ex);
					unprotection.completeExceptionally(ex);
					return null;
				});

		return unprotection;
	}

	@Override
	public CompletableFuture<Void> reprotectHellblock(@NotNull World world, @NotNull UserData owner,
			@NotNull UserData transferee) {
		if (worldGuardPlatform == null) {
			instance.getPluginLogger().severe("WorldGuard platform is unavailable!");
			return CompletableFuture.failedFuture(new IllegalStateException("WorldGuard not available"));
		}

		final RegionContainer regionContainer = worldGuardPlatform.getRegionContainer();
		final RegionManager maybeRm = regionContainer.get(BukkitAdapter.adapt(world));
		if (maybeRm == null) {
			instance.getPluginLogger().severe("No region manager for world: " + world.getName());
			return CompletableFuture
					.failedFuture(new IllegalStateException("No region manager for world: " + world.getName()));
		}
		final RegionManager regionManager = maybeRm;

		return runSyncFuture(() -> {
			try {
				final ProtectedRegion region = getRegion(world, owner.getUUID(), owner.getHellblockData().getID());
				if (region == null) {
					throw new IllegalStateException(
							"Could not get the WorldGuard region for owner: " + owner.getUUID());
				}

				// Transfer ownership/membership
				final DefaultDomain owners = region.getOwners();
				final DefaultDomain members = region.getMembers();

				if (!owners.getUniqueIds().contains(transferee.getUUID())) {
					owners.getUniqueIds().add(transferee.getUUID());
				}
				owners.getUniqueIds().remove(owner.getUUID());

				if (!members.getUniqueIds().contains(owner.getUUID())) {
					members.getUniqueIds().add(owner.getUUID());
				}
				members.getUniqueIds().remove(transferee.getUUID());

				// Create new region with transferee coords/ID and copy data
				final ProtectedRegion renamedRegion = new ProtectedCuboidRegion(
						"%s_%s".formatted(transferee.getUUID().toString(), transferee.getHellblockData().getID()),
						getProtectionVectorLeft(owner), getProtectionVectorRight(owner));

				renamedRegion.setOwners(owners);
				renamedRegion.setMembers(members);
				renamedRegion.setFlags(region.getFlags());
				renamedRegion.setPriority(region.getPriority());
				renamedRegion.setParent(region.getParent());

				regionManager.addRegion(renamedRegion);
				regionManager.removeRegion(region.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
				regionManager.save();

				// update bounding box stored for transferee
				transferee.getHellblockData().setBoundingBox(new BoundingBox(
						(double) renamedRegion.getMinimumPoint().x(), (double) renamedRegion.getMinimumPoint().y(),
						(double) renamedRegion.getMinimumPoint().z(), (double) renamedRegion.getMaximumPoint().x(),
						(double) renamedRegion.getMaximumPoint().y(), (double) renamedRegion.getMaximumPoint().z()));

				updateHellblockMessages(world, transferee.getUUID());
				instance.getProtectionManager().changeLockStatus(world, transferee.getUUID());

				return null;
			} catch (Exception ex) {
				instance.getPluginLogger()
						.severe("Unable to reprotect " + transferee.getPlayer().getName() + "'s hellblock!", ex);
				throw ex;
			}
		});
	}

	@Override
	public void updateHellblockMessages(@NotNull World world, @NotNull UUID id) {
		// First try online
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isPresent()) {
			final UserData user = onlineUser.get();
			final ProtectedRegion region = getRegion(world, id, user.getHellblockData().getID());
			if (region == null) {
				instance.getPluginLogger().severe("No WorldGuard region found for online user " + id);
				return;
			}
			applyEntryAndFarewellFlags(user, region);
			return;
		}

		// Fallback: offline lookup
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}
					final UserData user = result.get();
					final ProtectedRegion region = getRegion(world, id, user.getHellblockData().getID());
					if (region == null) {
						instance.getPluginLogger().severe("No WorldGuard region found for offline user " + id);
						return;
					}
					applyEntryAndFarewellFlags(user, region);
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to update hellblock messages for " + id, ex);
					return null;
				});
	}

	private void applyEntryAndFarewellFlags(@NotNull UserData user, @NotNull ProtectedRegion region) {
		final String name = user.getName();
		if (name == null) {
			instance.getPluginLogger().warn("Failed to retrieve username for region messages.");
			return;
		}

		final boolean abandoned = user.getHellblockData().isAbandoned();
		final StringFlag greetFlag = convertToWorldGuardStringFlag(HellblockFlag.FlagType.GREET_MESSAGE);
		final StringFlag farewellFlag = convertToWorldGuardStringFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE);

		// Entry
		if (instance.getConfigManager().entryMessageEnabled()) {
			region.setFlag(greetFlag, abandoned
					? instance.getTranslationManager()
							.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
									MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key()))
					: instance.getTranslationManager().miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
							MessageConstants.HELLBLOCK_ENTRY_MESSAGE.arguments(Component.text(name)).build().key())));
		} else {
			region.setFlag(greetFlag, null);
		}

		// Farewell
		if (instance.getConfigManager().farewellMessageEnabled()) {
			region.setFlag(farewellFlag,
					abandoned
							? instance.getTranslationManager()
									.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
											MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key()))
							: instance.getTranslationManager().miniMessageTranslation(
									AdventureHelper.legacyToMiniMessage(MessageConstants.HELLBLOCK_FAREWELL_MESSAGE
											.arguments(Component.text(name)).build().key())));
		} else {
			region.setFlag(farewellFlag, null);
		}
	}

	@Override
	public void lockHellblock(@NotNull World world, @NotNull UserData owner) {
		runSync(() -> {
			final ProtectedRegion region = getRegion(world, owner.getUUID(), owner.getHellblockData().getID());
			if (region == null) {
				instance.getPluginLogger()
						.warn("Region not found when attempting to lock for owner " + owner.getUUID());
				return;
			}
			final StateFlag entryFlag = convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY);
			region.setFlag(entryFlag, (!owner.getHellblockData().isLocked() ? null : StateFlag.State.DENY));
		});
	}

	@Override
	public void abandonIsland(@NotNull World world, @NotNull UUID id) {
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}
					final UserData offlineUser = result.get();
					if (!offlineUser.getHellblockData().isAbandoned()) {
						return;
					}

					runSync(() -> {
						final ProtectedRegion region = getRegion(world, id, offlineUser.getHellblockData().getID());
						if (region == null) {
							instance.getPluginLogger()
									.warn("Could not get WorldGuard region when abandoning for " + id);
							return;
						}
						region.getOwners().clear();
						region.getMembers().clear();

						// set a conservative set of deny flags for abandoned region
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.PVP), StateFlag.State.ALLOW);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD), StateFlag.State.ALLOW);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY), StateFlag.State.DENY);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING),
								StateFlag.State.DENY);

						// Save region manager
						try {
							final RegionManager rm = worldGuardPlatform.getRegionContainer()
									.get(BukkitAdapter.adapt(world));
							if (rm != null) {
								rm.save();
							}
						} catch (Exception e) {
							instance.getPluginLogger()
									.severe("Failed to save region manager after abandoning island for " + id, e);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to fetch offline user for abandonIsland: " + id, ex);
					return null;
				});
	}

	@Override
	public void restoreFlags(@NotNull World world, @NotNull UUID id) {
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}
					final UserData offlineUser = result.get();
					if (offlineUser.getHellblockData().isAbandoned()) {
						return;
					}

					runSync(() -> {
						final ProtectedRegion region = getRegion(world, id, offlineUser.getHellblockData().getID());
						if (region == null) {
							instance.getPluginLogger()
									.warn("Could not get WorldGuard region when abandoning for " + id);
							return;
						}
						region.getOwners().addPlayer(id);
						offlineUser.getHellblockData().getIslandMembers()
								.forEach(player -> region.getMembers().addPlayer(player));

						// revert abandoned flags
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.PVP), StateFlag.State.DENY);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD), StateFlag.State.DENY);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY), StateFlag.State.ALLOW);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING),
								StateFlag.State.ALLOW);

						// Save region manager
						try {
							final RegionManager rm = worldGuardPlatform.getRegionContainer()
									.get(BukkitAdapter.adapt(world));
							if (rm != null) {
								rm.save();
							}
						} catch (Exception e) {
							instance.getPluginLogger()
									.severe("Failed to save region manager after restoring island for " + id, e);
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to fetch offline user for restoreIsland: " + id, ex);
					return null;
				});
	}

	@Override
	public void changeHellblockFlag(@NotNull World world, @NotNull UserData owner, @NotNull HellblockFlag flag) {
		runSync(() -> {
			final ProtectedRegion region = getRegion(world, owner.getUUID(), owner.getHellblockData().getID());
			if (region == null) {
				instance.getPluginLogger()
						.warn("Region not found when attempting to change flag for owner " + owner.getUUID());
				return;
			}
			final StateFlag wgFlag = convertToWorldGuardFlag(flag.getFlag());
			if (wgFlag != null) {
				region.setFlag(wgFlag,
						(flag.getStatus() == HellblockFlag.AccessType.ALLOW ? null : StateFlag.State.DENY));
			}
		});
	}

	@Override
	public CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull World world, @NotNull UUID ownerID) {
		if (worldGuardPlatform == null) {
			return CompletableFuture.failedFuture(new IllegalStateException("WorldGuard not available"));
		}

		final CompletableFuture<Set<UUID>> future = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserData(ownerID, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						future.complete(Collections.emptySet());
						return;
					}
					final UserData offlineUser = result.get();
					runSync(() -> {
						try {
							final ProtectedRegion region = getRegion(world, ownerID,
									offlineUser.getHellblockData().getID());
							if (region == null) {
								future.completeExceptionally(
										new IllegalStateException("Region not found for owner " + ownerID));
								return;
							}
							future.complete(new HashSet<>(region.getMembers().getUniqueIds()));
						} catch (Exception ex) {
							future.completeExceptionally(ex);
						}
					});
				}).exceptionally(ex -> {
					future.completeExceptionally(ex);
					return null;
				});
		return future;
	}

	@Override
	public void addMemberToHellblockBounds(@NotNull World world, @NotNull UUID ownerID, @NotNull UUID id) {
		instance.getStorageManager().getOfflineUserData(ownerID, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}
					final UserData offlineUser = result.get();
					runSync(() -> {
						final ProtectedRegion region = getRegion(world, ownerID,
								offlineUser.getHellblockData().getID());
						if (region == null) {
							instance.getPluginLogger()
									.warn("Could not get region when adding member " + id + " to owner " + ownerID);
							return;
						}
						if (!region.getMembers().getUniqueIds().contains(id)) {
							region.getMembers().getUniqueIds().add(id);
							try {
								final RegionManager rm = worldGuardPlatform.getRegionContainer()
										.get(BukkitAdapter.adapt(world));
								if (rm != null) {
									rm.save();
								}
							} catch (Exception e) {
								instance.getPluginLogger().severe("Failed to save region manager after adding member",
										e);
							}
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger()
							.severe("Failed to fetch offline user for addMemberToHellblockBounds: " + ownerID, ex);
					return null;
				});
	}

	@Override
	public void removeMemberFromHellblockBounds(@NotNull World world, @NotNull UUID ownerID, @NotNull UUID id) {
		instance.getStorageManager().getOfflineUserData(ownerID, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}
					final UserData offlineUser = result.get();
					runSync(() -> {
						final ProtectedRegion region = getRegion(world, ownerID,
								offlineUser.getHellblockData().getID());
						if (region == null) {
							instance.getPluginLogger()
									.warn("Could not get region when removing member " + id + " from owner " + ownerID);
							return;
						}
						if (region.getMembers().getUniqueIds().contains(id)) {
							region.getMembers().getUniqueIds().remove(id);
							try {
								final RegionManager rm = worldGuardPlatform.getRegionContainer()
										.get(BukkitAdapter.adapt(world));
								if (rm != null) {
									rm.save();
								}
							} catch (Exception e) {
								instance.getPluginLogger().severe("Failed to save region manager after removing member",
										e);
							}
						}
					});
				}).exceptionally(ex -> {
					instance.getPluginLogger()
							.severe("Failed to fetch offline user for removeMemberFromHellblockBounds: " + ownerID, ex);
					return null;
				});
	}

	/**
	 * Returns the upper corner of the island protection box.
	 */
	public @NotNull BlockVector3 getProtectionVectorLeft(@NotNull UserData owner) {
		final BoundingBox bounds = owner.getHellblockData().getBoundingBox();
		if (bounds == null) {
			throw new IllegalStateException("Bounding box not set. Did you call expandBoundingBox()?");
		}
		return BlockVector3.at(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
	}

	/**
	 * Returns the lower corner of the island protection box.
	 */
	public @NotNull BlockVector3 getProtectionVectorRight(@NotNull UserData owner) {
		final BoundingBox bounds = owner.getHellblockData().getBoundingBox();
		if (bounds == null) {
			throw new IllegalStateException("Bounding box not set. Did you call expandBoundingBox()?");
		}
		return BlockVector3.at(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
	}

	private @Nullable ProtectedRegion getRegion(@NotNull World world, @NotNull UUID playerUUID, int hellblockID) {
		if (worldGuardPlatform == null) {
			throw new IllegalStateException("WorldGuard platform not available.");
		}
		final RegionManager regionManager = worldGuardPlatform.getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null) {
			throw new IllegalStateException("No region manager for world: " + world.getName());
		}
		return regionManager.getRegion("%s_%s".formatted(playerUUID.toString(), hellblockID));
	}

	/**
	 * Note: only for non-string flags
	 * 
	 * @param flag the HellblockFlag.FlagType
	 * @return the corresponding WorldGuard StateFlag, or null if not applicable
	 * @throws IllegalArgumentException if the flag type is not convertible
	 */
	private @Nullable StateFlag convertToWorldGuardFlag(@NotNull FlagType flag) {
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
		default ->
			throw new IllegalArgumentException("The flag you defined can't be converted into a WorldGuard flag.");
		};
	}

	/**
	 * Note: only for greet/farewell messages
	 * 
	 * @param flag the HellblockFlag.FlagType
	 * @return the corresponding WorldGuard StringFlag, or null if not applicable
	 * @throws IllegalArgumentException if the flag type is not convertible
	 */
	private @Nullable StringFlag convertToWorldGuardStringFlag(@NotNull FlagType flag) {
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

	private Flag<?> getFlagByName(String name) {
		return WorldGuard.getInstance().getFlagRegistry().get(name);
	}
}