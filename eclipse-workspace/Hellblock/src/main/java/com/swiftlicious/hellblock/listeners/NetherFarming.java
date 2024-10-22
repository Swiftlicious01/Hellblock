package com.swiftlicious.hellblock.listeners;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

import lombok.NonNull;

public class NetherFarming implements Listener {

	private final HellblockPlugin instance;

	private final Map<String, Integer> blockCache, revertCache, moistureCache;

	public NetherFarming(HellblockPlugin plugin) {
		instance = plugin;
		this.blockCache = new HashMap<>();
		this.revertCache = new HashMap<>();
		this.moistureCache = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	// TODO: FIX SYSTEM NOT TRACKING PROPERLY
	public void trackNetherFarms(@NonNull HellblockPlayer hbPlayer) {
		Player player = hbPlayer.getPlayer();
		if (player == null)
			return;
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Set<Block> farmBlocks = getFarmlandOnHellblock(hbPlayer);
		if (farmBlocks != null) {
			Iterator<Block> islandBlocks = farmBlocks.iterator();
			while (true) {
				Block islandBlock;
				do {
					if (!islandBlocks.hasNext()) {
						return;
					}

					islandBlock = (Block) islandBlocks.next();
					final Block updatingBlock = islandBlock;
					Farmland farm = (Farmland) updatingBlock.getBlockData();
					if (checkForLavaAroundFarm(farmBlocks) || (instance.getLavaRain().getLavaRainTask() != null
							&& instance.getLavaRain().getLavaRainTask().isLavaRaining())) {
						instance.getScheduler().runTaskSyncLater(() -> {
							do {
								if (this.blockCache.containsKey(serializeFarmLocation(updatingBlock.getLocation()))
										&& this.blockCache
												.get(serializeFarmLocation(updatingBlock.getLocation())) != null
										&& this.blockCache.get(serializeFarmLocation(updatingBlock.getLocation()))
												.intValue() == RandomUtils.generateRandomInt(15, 25)) {
									if (player.getWorld()
											.getBlockAt(updatingBlock.getX(), updatingBlock.getY(),
													updatingBlock.getZ())
											.getType() == Material.FARMLAND
											&& ((Farmland) player.getWorld()
													.getBlockAt(updatingBlock.getX(), updatingBlock.getY(),
															updatingBlock.getZ())
													.getBlockData())
													.getMoisture() != ((Farmland) player.getWorld()
															.getBlockAt(updatingBlock.getX(), updatingBlock.getY(),
																	updatingBlock.getZ())
															.getBlockData()).getMaximumMoisture()
											&& !this.revertCache
													.containsKey(serializeFarmLocation(updatingBlock.getLocation()))) {
										farm.setMoisture(farm.getMaximumMoisture());
										updatingBlock.setBlockData(farm);
										updatingBlock.getState().update();
										this.moistureCache.put(serializeFarmLocation(updatingBlock.getLocation()),
												farm.getMoisture());
									}
									if (this.moistureCache
											.containsKey(serializeFarmLocation(updatingBlock.getLocation()))
											&& this.moistureCache
													.get(serializeFarmLocation(updatingBlock.getLocation())) != null
											&& this.moistureCache
													.get(serializeFarmLocation(updatingBlock.getLocation())) == farm
															.getMaximumMoisture()) {
										this.blockCache.remove(serializeFarmLocation(updatingBlock.getLocation()));
										this.moistureCache.remove(serializeFarmLocation(updatingBlock.getLocation()));
									}
								}
							} while (this.blockCache.containsKey(serializeFarmLocation(updatingBlock.getLocation())));
						}, updatingBlock.getLocation(), RandomUtils.generateRandomInt(15, 30), TimeUnit.SECONDS);

					} else {
						instance.getScheduler().runTaskSyncLater(() -> {
							farm.setMoisture(farm.getMoisture() > 0 ? farm.getMoisture() - 1 : 0);
							if (farm.getMoisture() == 0) {
								do {
									if (this.revertCache.containsKey(serializeFarmLocation(updatingBlock.getLocation()))
											&& this.revertCache
													.get(serializeFarmLocation(updatingBlock.getLocation())) != null
											&& this.revertCache.get(serializeFarmLocation(updatingBlock.getLocation()))
													.intValue() == RandomUtils.generateRandomInt(15, 20)) {
										if (player.getWorld()
												.getBlockAt(updatingBlock.getX(), updatingBlock.getY(),
														updatingBlock.getZ())
												.getType() == Material.FARMLAND
												&& !this.blockCache.containsKey(
														serializeFarmLocation(updatingBlock.getLocation()))) {
											updatingBlock.setType(Material.DIRT);
											updatingBlock.getState().update();
											if (Tag.CROPS.isTagged(updatingBlock.getRelative(BlockFace.UP).getType())) {
												updatingBlock.getRelative(BlockFace.UP).breakNaturally(true);
												updatingBlock.getRelative(BlockFace.UP).getState().update();
											}
										}
										this.revertCache.remove(serializeFarmLocation(updatingBlock.getLocation()));
									}
								} while (this.revertCache
										.containsKey(serializeFarmLocation(updatingBlock.getLocation())));
							}
						}, updatingBlock.getLocation(), RandomUtils.generateRandomInt(10, 15), TimeUnit.SECONDS);
					}
				} while (hasUngrownCrops(farmBlocks));
			}
		}
	}

	public String serializeFarmLocation(Location loc) {
		org.bukkit.World world = loc.getWorld();
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		return world.getName() + "," + x + "," + y + "," + z;
	}

	public Location deserializeFarmLocation(String loc) {
		String[] split = loc.split(",");
		org.bukkit.World world = Bukkit.getWorld(split[0]);
		int x = Integer.parseInt(split[1]);
		int y = Integer.parseInt(split[2]);
		int z = Integer.parseInt(split[3]);
		return new Location(world, x, y, z);
	}

	public boolean hasUngrownCrops(@Nullable Set<Block> blocks) {
		if (blocks == null || blocks.isEmpty())
			return false;

		boolean ungrownStage = false;
		for (Block farm : blocks) {
			if (!farm.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				continue;

			Farmland farmland = (Farmland) farm.getBlockData();
			if (Tag.CROPS.isTagged(farm.getRelative(BlockFace.UP).getType())) {
				Block crop = farm.getRelative(BlockFace.UP);
				if (crop.getBlockData() instanceof Ageable) {
					Ageable cropData = (Ageable) crop.getBlockData();
					if (cropData.getAge() < cropData.getMaximumAge()) {
						ungrownStage = true;
						break;
					}
				}
			}
			return ungrownStage || farmland.getMoisture() < farmland.getMaximumMoisture();
		}
		return false;
	}

	public boolean checkForLavaAroundFarm(@Nullable Set<Block> blocks) {
		if (blocks == null || blocks.isEmpty())
			return false;

		for (Block block : blocks) {
			if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				continue;

			int centerX = block.getLocation().getBlockX();
			int centerY = block.getLocation().getBlockY();
			int centerZ = block.getLocation().getBlockZ();
			for (int x = centerX - 4; x <= centerX + 4; x++) {
				for (int y = centerY - 1; y <= centerY + 1; y++) {
					for (int z = centerZ - 4; z <= centerZ + 4; z++) {
						Block b = block.getWorld().getBlockAt(x, y, z);
						if (b.getType() == Material.LAVA) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public @Nullable Set<Block> getFarmlandOnHellblock(@NonNull HellblockPlayer hbPlayer) {
		Player player = hbPlayer.getPlayer();
		if (player == null)
			return null;
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return null;
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			Set<Block> regionBlocks = new HashSet<>();
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe("Could not load WorldGuard regions for hellblock world: " + world.getName());
				return null;
			}
			BlockVector3 vector = BlockVector3.at(player.getX(), player.getY(), player.getZ());
			ApplicableRegionSet wgRegion = regions.getApplicableRegions(vector);
			if (wgRegion == null) {
				return null;
			}
			for (ProtectedRegion region : wgRegion) {
				if (region == null) {
					return null;
				}
				// weRegion is null if wgRegion is a global region
				Region weRegion = WorldEditRegionConverter.convertToRegion(region);
				if (weRegion == null) {
					return null;
				}
				Iterator<BlockVector3> iterator = weRegion.iterator();
				while (iterator.hasNext()) {
					BlockVector3 block = iterator.next();
					Block regionBlock = instance.getHellblockHandler().getHellblockWorld().getBlockAt(block.x(),
							block.y(), block.z());
					if (!(regionBlock.getBlockData() instanceof Farmland))
						continue;
					if (regionBlock.getType() != Material.FARMLAND)
						continue;
					regionBlocks.add(regionBlock);
					if (!this.blockCache.containsKey(serializeFarmLocation(regionBlock.getLocation()))) {
						this.blockCache.put(serializeFarmLocation(regionBlock.getLocation()),
								RandomUtils.generateRandomInt(15, 25));
					}
					if (!this.revertCache.containsKey(serializeFarmLocation(regionBlock.getLocation()))) {
						this.revertCache.put(serializeFarmLocation(regionBlock.getLocation()),
								RandomUtils.generateRandomInt(15, 20));
					}
				}
			}
			return regionBlocks;
		} else {
			// TODO: plugin protection
		}
		return null;
	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			if (checkForLavaAroundFarm(Set.of(block)) || (instance.getLavaRain().getLavaRainTask() != null
					&& instance.getLavaRain().getLavaRainTask().isLavaRaining())) {
				event.setCancelled(true);
				Collection<Entity> entitiesNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25);
				Player player = instance.getNetherrackGenerator().getClosestPlayer(block.getLocation(), entitiesNearby);
				if (player != null)
					trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
			}
		}
		if (this.blockCache.containsKey(serializeFarmLocation(block.getLocation()))) {
			this.blockCache.remove(serializeFarmLocation(block.getLocation()));
		}
		if (this.revertCache.containsKey(serializeFarmLocation(block.getLocation()))) {
			this.revertCache.remove(serializeFarmLocation(block.getLocation()));
		}
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (event.getExplosionResult() != ExplosionResult.DESTROY)
			return;

		if (block.getBlockData() instanceof Farmland) {
			Collection<Entity> entitiesNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25);
			Player player = instance.getNetherrackGenerator().getClosestPlayer(block.getLocation(), entitiesNearby);
			if (player != null) {
				trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
			}
		}
		if (this.blockCache.containsKey(serializeFarmLocation(block.getLocation()))) {
			this.blockCache.remove(serializeFarmLocation(block.getLocation()));
		}
		if (this.revertCache.containsKey(serializeFarmLocation(block.getLocation()))) {
			this.revertCache.remove(serializeFarmLocation(block.getLocation()));
		}
	}

	@EventHandler
	public void onLavaPlace(PlayerBucketEmptyEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getBucket() == Material.LAVA_BUCKET) {
			trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
		}
	}

	@EventHandler
	public void onLavaPickup(PlayerBucketFillEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getItemStack() != null && event.getItemStack().getType() == Material.LAVA_BUCKET) {
			trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			Player player = event.getPlayer();
			trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
		}
		if (this.blockCache.containsKey(serializeFarmLocation(block.getLocation()))) {
			this.blockCache.remove(serializeFarmLocation(block.getLocation()));
		}
		if (this.revertCache.containsKey(serializeFarmLocation(block.getLocation()))) {
			this.revertCache.remove(serializeFarmLocation(block.getLocation()));
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			Player player = event.getPlayer();
			trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
		}
	}

	@EventHandler
	public void onMoistureChange(MoistureChangeEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			Collection<Entity> entitiesNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25);
			Player player = instance.getNetherrackGenerator().getClosestPlayer(block.getLocation(), entitiesNearby);
			if (player != null)
				trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
		}
		if (this.blockCache.containsKey(serializeFarmLocation(block.getLocation()))) {
			this.blockCache.remove(serializeFarmLocation(block.getLocation()));
		}
		if (this.revertCache.containsKey(serializeFarmLocation(block.getLocation()))) {
			this.revertCache.remove(serializeFarmLocation(block.getLocation()));
		}
	}

	@EventHandler
	public void onBoneMeal(BlockFertilizeEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		List<BlockState> blocks = event.getBlocks();
		for (BlockState block : blocks) {
			if (block.getBlockData() instanceof Ageable) {
				trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
			}
		}
	}

	@EventHandler
	public void onHarvest(PlayerHarvestBlockEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Block block = event.getHarvestedBlock();
		if (block.getBlockData() instanceof Ageable) {
			trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
		}
	}

	@EventHandler
	public void onGrow(BlockGrowEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Ageable) {
			Collection<Entity> entitiesNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25);
			Player player = instance.getNetherrackGenerator().getClosestPlayer(block.getLocation(), entitiesNearby);
			if (player != null) {
				trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
			}
		}
	}

	@EventHandler
	public void onFarmland(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getAction() != Action.PHYSICAL) {
			return;
		}

		if (event.getItem() != null && (!(Tag.ITEMS_HOES.isTagged(event.getItem().getType())
				|| Tag.CROPS.isTagged(event.getItem().getType())))) {
			return;
		}

		Block block = event.getClickedBlock();
		if (block != null) {
			if (Tag.DIRT.isTagged(block.getType()) || block.getBlockData() instanceof Farmland) {
				trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
			}
			if (this.blockCache.containsKey(serializeFarmLocation(block.getLocation()))) {
				this.blockCache.remove(serializeFarmLocation(block.getLocation()));
			}
			if (this.revertCache.containsKey(serializeFarmLocation(block.getLocation()))) {
				this.revertCache.remove(serializeFarmLocation(block.getLocation()));
			}
		}
	}

	@EventHandler
	public void onFarmlandEntity(EntityInteractEvent event) {
		Entity entity = event.getEntity();
		if (!entity.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getEntity() instanceof Player)
			return;

		if (entity instanceof LivingEntity) {
			Block block = event.getBlock();
			if (block.getBlockData() instanceof Farmland) {
				Collection<Entity> entitiesNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25);
				Player player = instance.getNetherrackGenerator().getClosestPlayer(block.getLocation(), entitiesNearby);
				if (player != null)
					trackNetherFarms(new HellblockPlayer(player.getUniqueId()));
			}
			if (this.blockCache.containsKey(serializeFarmLocation(block.getLocation()))) {
				this.blockCache.remove(serializeFarmLocation(block.getLocation()));
			}
			if (this.revertCache.containsKey(serializeFarmLocation(block.getLocation()))) {
				this.revertCache.remove(serializeFarmLocation(block.getLocation()));
			}
		}
	}
}
