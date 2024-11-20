package com.swiftlicious.hellblock.api.compatibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class WorldGuardHook {

	protected final HellblockPlugin instance;
	@Getter
	@Setter
	private WorldGuardPlatform worldGuardPlatform;

	public final static String SPAWN_REGION = "spawn";

	public WorldGuardHook(HellblockPlugin plugin) {
		instance = plugin;
	}

	public CompletableFuture<Void> protectHellblock(@NonNull UserData user) {
		return CompletableFuture.runAsync(() -> {
			try {
				if (this.worldGuardPlatform == null) {
					throw new NullPointerException("Could not retrieve WorldGuard platform.");
				}
				RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
				RegionManager regionManager = regionContainer
						.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
				if (regionManager == null) {
					instance.getPluginLogger().severe(String.format("Could not get the WorldGuard region manager for the world: %s",
							instance.getConfigManager().worldName()));
					return;
				}
				DefaultDomain owners = new DefaultDomain();
				ProtectedRegion region = new ProtectedCuboidRegion(
						String.format("%s_%s", user.getUUID().toString(), user.getHellblockData().getID()),
						getProtectionVectorLeft(user.getHellblockData().getHellblockLocation()),
						getProtectionVectorRight(user.getHellblockData().getHellblockLocation()));
				region.setParent(regionManager.getRegion(ProtectedRegion.GLOBAL_REGION));
				owners.addPlayer(user.getUUID());
				region.setOwners(owners);
				region.setPriority(100);
				updateHellblockMessages(user.getUUID(), region);
				user.getHellblockData()
						.setBoundingBox(new BoundingBox((double) region.getMinimumPoint().x(),
								(double) region.getMinimumPoint().y(), (double) region.getMinimumPoint().z(),
								(double) region.getMaximumPoint().x(), (double) region.getMaximumPoint().y(),
								(double) region.getMaximumPoint().z()));
				ApplicableRegionSet set = regionManager
						.getApplicableRegions(BlockVector3.at(user.getHellblockData().getHellblockLocation().getX(),
								user.getHellblockData().getHellblockLocation().getY(),
								user.getHellblockData().getHellblockLocation().getZ()));
				if (set.size() > 0) {
					Iterator<ProtectedRegion> regions = set.iterator();

					while (regions.hasNext()) {
						ProtectedRegion regionCheck = (ProtectedRegion) regions.next();
						if (!regionCheck.getId().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION)
								&& !regionCheck.getId().equalsIgnoreCase(SPAWN_REGION)) {
							regionManager.removeRegion(regionCheck.getId());
						}
					}
				}

				regionManager.addRegion(region);
				regionManager.save();
				instance.getCoopManager().changeLockStatus(user);
			} catch (Exception ex) {
				instance.getPluginLogger().severe(String.format("Unable to protect %s's hellblock!", user.getName()), ex);
				return;
			}
		});
	}

	public CompletableFuture<Void> unprotectHellblock(@NonNull UUID id, boolean force) {
		return CompletableFuture.runAsync(() -> {
			try {
				if (this.worldGuardPlatform == null) {
					throw new NullPointerException("Could not retrieve WorldGuard platform.");
				}
				RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
				RegionManager regionManager = regionContainer
						.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
				if (regionManager == null) {
					instance.getPluginLogger().severe(String.format("Could not get the WorldGuard region manager for the world: %s",
							instance.getConfigManager().worldName()));
					return;
				}
				instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData()).thenAccept((result) -> {
					UserData offlineUser = result.orElseThrow();
					String regionName = String.format("%s_%s", id.toString(), offlineUser.getHellblockData().getID());
					ProtectedRegion region = regionManager.getRegion(regionName);
					if (region != null) {
						instance.getScheduler().executeSync(() -> clearEntities(region),
								offlineUser.getHellblockData().getHellblockLocation());
						regionManager.removeRegion(regionName, RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
						try {
							regionManager.save();
						} catch (Exception ex) {
							instance.getPluginLogger().severe(String.format("Unable to unprotect %s's hellblock!",
									Bukkit.getPlayer(id).getName()), ex);
							return;
						}
					}
				});
			} catch (Exception ex) {
				instance.getPluginLogger().severe(String.format("Unable to unprotect %s's hellblock!", Bukkit.getPlayer(id).getName()),
						ex);
				return;
			}
		});
	}

	public void clearEntities(@NonNull ProtectedRegion region) {
		World world = instance.getHellblockHandler().getHellblockWorld();
		world.getEntities().stream()
				.filter(entity -> region.contains(BlockVector3.at(entity.getX(), entity.getY(), entity.getZ())))
				.filter(entity -> entity.getType() != EntityType.PLAYER).forEach(Entity::remove);
	}

	public void updateHellblockMessages(@NonNull UUID id, @NonNull ProtectedRegion region) {
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (!onlineUser.isEmpty()) {
			String name = onlineUser.get().getName();
			if (name == null) {
				instance.getPluginLogger().warn("Failed to retrieve player's username to update hellblock entry and farewell messages.");
				return;
			}
			StringFlag greetFlag = instance.getIslandProtectionManager()
					.convertToWorldGuardStringFlag(HellblockFlag.FlagType.GREET_MESSAGE);
			if (instance.getConfigManager().entryMessageEnabled()) {
				if (!onlineUser.get().getHellblockData().isAbandoned()) {
					region.setFlag(greetFlag, String.format("&cYou're entering &4%s&c's Hellblock!", name));
				} else {
					region.setFlag(greetFlag, "&4** &cYou're entering an abandoned Hellblock! &4**");
				}
			} else {
				region.setFlag(greetFlag, null);
			}
			StringFlag farewellFlag = instance.getIslandProtectionManager()
					.convertToWorldGuardStringFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE);
			if (instance.getConfigManager().farewellMessageEnabled()) {
				if (!onlineUser.get().getHellblockData().isAbandoned()) {
					region.setFlag(farewellFlag, String.format("&cYou're leaving &4%s&c's Hellblock!", name));
				} else {
					region.setFlag(farewellFlag, "&4** &cYou're leaving an abandoned Hellblock! &4**");
				}
			} else {
				region.setFlag(farewellFlag, null);
			}
		} else {
			instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData()).thenAccept((result) -> {
				UserData offlineUser = result.orElseThrow();
				String name = offlineUser.getName();
				if (name == null) {
					instance.getPluginLogger().warn(
							"Failed to retrieve player's username to update hellblock entry and farewell messages.");
					return;
				}
				StringFlag greetFlag = instance.getIslandProtectionManager()
						.convertToWorldGuardStringFlag(HellblockFlag.FlagType.GREET_MESSAGE);
				if (instance.getConfigManager().entryMessageEnabled()) {
					if (!offlineUser.getHellblockData().isAbandoned()) {
						region.setFlag(greetFlag, String.format("&cYou're entering &4%s&c's Hellblock!", name));
					} else {
						region.setFlag(greetFlag, "&4** &cYou're entering an abandoned Hellblock! &4**");
					}
				} else {
					region.setFlag(greetFlag, null);
				}
				StringFlag farewellFlag = instance.getIslandProtectionManager()
						.convertToWorldGuardStringFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE);
				if (instance.getConfigManager().farewellMessageEnabled()) {
					if (!offlineUser.getHellblockData().isAbandoned()) {
						region.setFlag(farewellFlag, String.format("&cYou're leaving &4%s&c's Hellblock!", name));
					} else {
						region.setFlag(farewellFlag, "&4** &cYou're leaving an abandoned Hellblock! &4**");
					}
				} else {
					region.setFlag(farewellFlag, null);
				}
			});
		}
	}

	public void abandonIsland(@NonNull UUID id, @NonNull ProtectedRegion region) {
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData()).thenAccept((result) -> {
			UserData offlineUser = result.orElseThrow();
			if (offlineUser.getHellblockData().isAbandoned()) {
				region.getOwners().clear();
				region.getMembers().clear();
				region.setFlag(
						instance.getIslandProtectionManager().convertToWorldGuardFlag(HellblockFlag.FlagType.PVP),
						StateFlag.State.DENY);
				region.setFlag(
						instance.getIslandProtectionManager().convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD),
						StateFlag.State.DENY);
				region.setFlag(
						instance.getIslandProtectionManager().convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY),
						StateFlag.State.DENY);
				region.setFlag(instance.getIslandProtectionManager()
						.convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING), StateFlag.State.DENY);
			}
		});
	}

	public @NonNull BlockVector3 getProtectionVectorLeft(@NonNull Location loc) {
		return BlockVector3.at(loc.getX() + (double) (instance.getConfigManager().protectionRange() / 2),
				instance.getHellblockHandler().getHellblockWorld().getMaxHeight(),
				loc.getZ() + (double) (instance.getConfigManager().protectionRange() / 2));
	}

	public @NonNull BlockVector3 getProtectionVectorRight(@NonNull Location loc) {
		return BlockVector3.at(loc.getX() - (double) (instance.getConfigManager().protectionRange() / 2),
				instance.getHellblockHandler().getHellblockWorld().getMinHeight(),
				loc.getZ() - (double) (instance.getConfigManager().protectionRange() / 2));
	}

	public @NonNull Vector getSpawnCenter() {
		World world = instance.getHellblockHandler().getHellblockWorld();
		// Get top location
		if (this.worldGuardPlatform == null) {
			throw new NullPointerException("Could not retrieve WorldGuard platform.");
		}
		RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
		RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
		if (regionManager == null) {
			instance.getPluginLogger().severe(
					String.format("Could not get the WorldGuard region manager for the world: %s", instance.getConfigManager().worldName()));
			return new Location(world, 0, instance.getConfigManager().height(), 0).toVector();
		}
		ProtectedRegion region = regionManager.getRegion(SPAWN_REGION);
		if (region == null) {
			instance.getPluginLogger().severe("Could not get the WorldGuard spawn region");
			return new Location(world, 0, instance.getConfigManager().height(), 0).toVector();
		}
		Location top = new Location(world, 0, instance.getConfigManager().height(), 0);
		top.setX(region.getMaximumPoint().x());
		top.setY(region.getMaximumPoint().y());
		top.setZ(region.getMaximumPoint().z());

		// Get bottom location
		Location bottom = new Location(world, 0, instance.getConfigManager().height(), 0);
		bottom.setX(region.getMinimumPoint().x());
		bottom.setY(region.getMinimumPoint().y());
		bottom.setZ(region.getMinimumPoint().z());

		// Setup center location
		return top.toVector().getMidpoint(bottom.toVector());
	}

	public @NonNull Vector getCenter(@NonNull ProtectedRegion region) {
		World world = instance.getHellblockHandler().getHellblockWorld();
		// Get top location
		Location top = new Location(world, 0, instance.getConfigManager().height(), 0);
		top.setX(region.getMaximumPoint().x());
		top.setY(region.getMaximumPoint().y());
		top.setZ(region.getMaximumPoint().z());

		// Get bottom location
		Location bottom = new Location(world, 0, instance.getConfigManager().height(), 0);
		bottom.setX(region.getMinimumPoint().x());
		bottom.setY(region.getMinimumPoint().y());
		bottom.setZ(region.getMinimumPoint().z());

		// Setup center location
		return top.toVector().getMidpoint(bottom.toVector());
	}

	public boolean isRegionProtected(@NonNull Location location) {
		com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
		if (this.worldGuardPlatform == null) {
			throw new NullPointerException("Could not retrieve WorldGuard platform.");
		}
		RegionContainer container = this.worldGuardPlatform.getRegionContainer();
		RegionQuery query = container.createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(loc);
		if (set.size() != 0)
			return true;
		return false;
	}

	public CompletableFuture<List<Block>> getRegionBlocks(@NonNull UUID id) {
		return CompletableFuture.supplyAsync((Supplier<List<Block>>) () -> {
			World world = instance.getHellblockHandler().getHellblockWorld();
			RegionBlockSupplier regionBlockSupplier = new RegionBlockSupplier();
			instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData()).thenAccept((result) -> {
				UserData offlineUser = result.orElseThrow();
				ProtectedRegion region = getRegion(id, offlineUser.getHellblockData().getID());
				if (region == null) {
					regionBlockSupplier.setRegionBlockSupplier(new ArrayList<>());
				}

				Location min = BukkitAdapter.adapt(world, region.getMinimumPoint());
				Location max = BukkitAdapter.adapt(world, region.getMaximumPoint());

				List<Block> regionBlocks = new ArrayList<>();
				for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
					for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
						for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
							Block block = world.getBlockAt(x, y, z);
							regionBlocks.add(block);
						}
					}
				}
				regionBlockSupplier.setRegionBlockSupplier(regionBlocks);
			});
			return regionBlockSupplier.getRegionBlockSupplier();
		});
	}

	public ProtectedRegion getRegion(@NonNull UUID playerUUID, int hellblockID) {
		if (this.worldGuardPlatform == null) {
			throw new NullPointerException("Could not retrieve WorldGuard platform.");
		}
		World world = instance.getHellblockHandler().getHellblockWorld();
		RegionManager regionManager = this.worldGuardPlatform.getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null) {
			instance.getPluginLogger().severe(
					String.format("Could not get the WorldGuard region manager for the world: %s", instance.getConfigManager().worldName()));
			return null;
		}
		ProtectedRegion region = regionManager.getRegion(String.format("%s_%s", playerUUID.toString(), hellblockID));
		if (region == null) {
			return null;
		}

		return region;
	}

	public ProtectedRegion getSpawnRegion() {
		if (this.worldGuardPlatform == null) {
			throw new NullPointerException("Could not retrieve WorldGuard platform.");
		}
		World world = instance.getHellblockHandler().getHellblockWorld();
		RegionManager regionManager = this.worldGuardPlatform.getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null) {
			instance.getPluginLogger().severe(
					String.format("Could not get the WorldGuard region manager for the world: %s", instance.getConfigManager().worldName()));
			return null;
		}
		ProtectedRegion region = regionManager.getRegion(SPAWN_REGION);
		if (region == null) {
			return null;
		}

		return region;
	}

	public CompletableFuture<Void> protectSpawn() {
		return CompletableFuture.runAsync(() -> {
			try {
				if (this.worldGuardPlatform == null) {
					throw new NullPointerException("Could not retrieve WorldGuard platform.");
				}
				BlockVector3 pos1 = BlockVector3.at((double) instance.getConfigManager().spawnSize(),
						instance.getHellblockHandler().getHellblockWorld().getMaxHeight(), (double) instance.getConfigManager().spawnSize());
				BlockVector3 pos2 = BlockVector3.at((double) (0 - instance.getConfigManager().spawnSize()),
						instance.getHellblockHandler().getHellblockWorld().getMinHeight(),
						(double) (0 - instance.getConfigManager().spawnSize()));
				ProtectedRegion region = new ProtectedCuboidRegion(SPAWN_REGION, pos1, pos2);
				RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
				RegionManager regionManager = regionContainer
						.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
				if (regionManager == null) {
					instance.getPluginLogger().severe(String.format("Could not get the WorldGuard region manager for the world: %s",
							instance.getConfigManager().worldName()));
					return;
				}
				region.setParent(regionManager.getRegion(ProtectedRegion.GLOBAL_REGION));
				region.setPriority(100);
				region.setFlag(instance.getIslandProtectionManager()
						.convertToWorldGuardFlag(HellblockFlag.FlagType.INVINCIBILITY), StateFlag.State.ALLOW);
				region.setFlag(
						instance.getIslandProtectionManager().convertToWorldGuardFlag(HellblockFlag.FlagType.PVP),
						StateFlag.State.DENY);
				region.setFlag(
						instance.getIslandProtectionManager().convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD),
						StateFlag.State.DENY);
				region.setFlag(instance.getIslandProtectionManager()
						.convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING), StateFlag.State.DENY);
				regionManager.addRegion(region);
				regionManager.save();
				return;
			} catch (Exception ex) {
				instance.getPluginLogger().severe("Unable to protect spawn area!", ex);
				return;
			}
		});
	}

	/**
	 * Gets the regions a player is currently in.
	 *
	 * @param playerUUID UUID of the player in question.
	 * @return Set of WorldGuard protected regions that the player is currently in.
	 */
	@NonNull
	public Set<ProtectedRegion> getRegions(@NonNull UUID playerUUID) {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null || !player.isOnline() || this.worldGuardPlatform == null)
			return Collections.emptySet();

		RegionQuery query = this.worldGuardPlatform.getRegionContainer().createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
		return set.getRegions();
	}

	/**
	 * Gets the regions names a player is currently in.
	 *
	 * @param playerUUID UUID of the player in question.
	 * @return Set of Strings with the names of the regions the player is currently
	 *         in.
	 */
	@NonNull
	public Set<String> getRegionsNames(@NonNull UUID playerUUID) {
		return getRegions(playerUUID).stream().map(ProtectedRegion::getId).collect(Collectors.toSet());
	}

	/**
	 * Checks whether a player is in one or several regions
	 *
	 * @param playerUUID  UUID of the player in question.
	 * @param regionNames Set of regions to check.
	 * @return True if the player is in (all) the named region(s).
	 */
	public boolean isPlayerInAllRegions(@NonNull UUID playerUUID, Set<String> regionNames) {
		Set<String> regions = getRegionsNames(playerUUID);
		if (regionNames.isEmpty())
			throw new IllegalArgumentException("You need to check for at least one region!");

		return regions.containsAll(regionNames.stream().map(String::toLowerCase).collect(Collectors.toSet()));
	}

	/**
	 * Checks whether a player is in one or several regions
	 *
	 * @param playerUUID  UUID of the player in question.
	 * @param regionNames Set of regions to check.
	 * @return True if the player is in (any of) the named region(s).
	 */
	public boolean isPlayerInAnyRegion(@NonNull UUID playerUUID, Set<String> regionNames) {
		Set<String> regions = getRegionsNames(playerUUID);
		if (regionNames.isEmpty())
			throw new IllegalArgumentException("You need to check for at least one region!");
		for (String region : regionNames) {
			if (regions.contains(region.toLowerCase()))
				return true;
		}
		return false;
	}

	/**
	 * Checks whether a player is in one or several regions
	 *
	 * @param playerUUID UUID of the player in question.
	 * @param regionName List of regions to check.
	 * @return True if the player is in (any of) the named region(s).
	 */
	public boolean isPlayerInAnyRegion(@NonNull UUID playerUUID, String... regionName) {
		return isPlayerInAnyRegion(playerUUID, new HashSet<>(Arrays.asList(regionName)));
	}

	/**
	 * Checks whether a player is in one or several regions
	 *
	 * @param playerUUID UUID of the player in question.
	 * @param regionName List of regions to check.
	 * @return True if the player is in (any of) the named region(s).
	 */
	public boolean isPlayerInAllRegions(@NonNull UUID playerUUID, String... regionName) {
		return isPlayerInAllRegions(playerUUID, new HashSet<>(Arrays.asList(regionName)));
	}

	public class RegionBlockSupplier {
		private List<Block> regionBlockSupplier;

		public @NonNull List<Block> getRegionBlockSupplier() {
			return this.regionBlockSupplier;
		}

		public void setRegionBlockSupplier(@NonNull List<Block> regionBlockSupplier) {
			this.regionBlockSupplier = regionBlockSupplier;
		}
	}
}
