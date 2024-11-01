package com.swiftlicious.hellblock.listeners;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LocationCache;
import com.swiftlicious.hellblock.utils.RandomUtils;

import lombok.NonNull;

public class NetherFarming implements Listener {

	private final HellblockPlugin instance;

	private final Map<Location, Integer> blockCache, revertCache, moistureCache;
	private final Map<HellblockPlayer, Collection<Location>> regionCache;

	public NetherFarming(HellblockPlugin plugin) {
		instance = plugin;
		this.blockCache = new LinkedHashMap<>();
		this.revertCache = new LinkedHashMap<>();
		this.moistureCache = new LinkedHashMap<>();
		this.regionCache = new LinkedHashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public CompletableFuture<Void> trackNetherFarms(@NonNull HellblockPlayer hbPlayer) {
		return CompletableFuture.runAsync(() -> {
			Set<Block> farmBlocks = getFarmlandOnHellblock(hbPlayer);
			if (farmBlocks != null) {
				Iterator<Block> islandBlocks = farmBlocks.iterator();
				Block islandBlock;
				do {
					if (!islandBlocks.hasNext()) {
						return;
					}

					islandBlock = (Block) islandBlocks.next();
					final Block updatingBlock = islandBlock;
					if (!(updatingBlock.getBlockData() instanceof Farmland))
						continue;
					Farmland farm = (Farmland) updatingBlock.getBlockData();
					Location cache = LocationCache.getCachedLocation(updatingBlock.getLocation());
					if (checkForLavaAroundFarm(cache.getBlock())
							|| (instance.getLavaRainHandler().getLavaRainTask() != null
									&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining()
									&& instance.getLavaRainHandler().getHighestBlock(cache) != null
									&& instance.getLavaRainHandler().getHighestBlock(cache).isEmpty())) {
						instance.getScheduler().runTaskSyncLater(() -> {
							if (!this.blockCache.containsKey(cache)) {
								this.blockCache.put(cache, RandomUtils.generateRandomInt(15, 25));
							}
							do {
								if (this.blockCache.containsKey(cache) && this.blockCache.get(cache) != null
										&& this.blockCache.get(cache).intValue() == RandomUtils.generateRandomInt(15,
												25)) {
									if (instance.getHellblockHandler().getHellblockWorld()
											.getBlockAt(cache.getBlockX(), cache.getBlockY(), cache.getBlockZ())
											.getType() == Material.FARMLAND
											&& ((Farmland) instance.getHellblockHandler().getHellblockWorld()
													.getBlockAt(cache.getBlockX(), cache.getBlockY(), cache.getBlockZ())
													.getBlockData())
													.getMoisture() != ((Farmland) instance.getHellblockHandler()
															.getHellblockWorld()
															.getBlockAt(cache.getBlockX(), cache.getBlockY(),
																	cache.getBlockZ())
															.getBlockData()).getMaximumMoisture()
											&& !this.revertCache.containsKey(cache)
											&& !this.moistureCache.containsKey(cache)) {
										farm.setMoisture(farm.getMaximumMoisture());
										updatingBlock.setBlockData(farm);
										updatingBlock.getState().update();
										if (!hbPlayer.isChallengeActive(ChallengeType.NETHER_FARM_CHALLENGE)
												&& !hbPlayer
														.isChallengeCompleted(ChallengeType.NETHER_FARM_CHALLENGE)) {
											hbPlayer.beginChallengeProgression(ChallengeType.NETHER_FARM_CHALLENGE);
										} else {
											hbPlayer.updateChallengeProgression(ChallengeType.NETHER_FARM_CHALLENGE, 1);
											if (hbPlayer.isChallengeCompleted(ChallengeType.NETHER_FARM_CHALLENGE)) {
												hbPlayer.completeChallenge(ChallengeType.NETHER_FARM_CHALLENGE);
											}
										}
										this.moistureCache.put(cache, farm.getMoisture());
									} else {
										break;
									}
									if (this.moistureCache.containsKey(cache) && this.moistureCache.get(cache) != null
											&& this.moistureCache.get(cache) == farm.getMaximumMoisture()) {
										this.blockCache.remove(cache);
										this.moistureCache.remove(cache);
									}
								}
							} while (this.blockCache.containsKey(cache) && this.blockCache.get(cache) != null);
						}, cache, RandomUtils.generateRandomInt(15, 30), TimeUnit.SECONDS);
					} else {
						instance.getScheduler().runTaskSyncLater(() -> {
							farm.setMoisture(farm.getMoisture() > 0 ? farm.getMoisture() - 1 : 0);
							if (farm.getMoisture() == 0) {
								if (!this.revertCache.containsKey(cache)) {
									this.revertCache.put(cache, RandomUtils.generateRandomInt(15, 20));
								}
								do {
									if (this.revertCache.containsKey(cache) && this.revertCache.get(cache) != null
											&& this.revertCache.get(cache).intValue() == RandomUtils
													.generateRandomInt(15, 20)) {
										if (instance.getHellblockHandler().getHellblockWorld()
												.getBlockAt(cache.getBlockX(), cache.getBlockY(), cache.getBlockZ())
												.getType() == Material.FARMLAND && !this.blockCache.containsKey(cache)
												&& this.moistureCache.containsKey(cache)) {
											if (Tag.CROPS.isTagged(updatingBlock.getRelative(BlockFace.UP).getType())) {
												updatingBlock.getRelative(BlockFace.UP).breakNaturally(true);
												updatingBlock.getRelative(BlockFace.UP).getState().update();
											}
											updatingBlock.setType(Material.DIRT);
										} else {
											break;
										}
										this.revertCache.remove(cache);
									}
								} while (this.revertCache.containsKey(cache) && this.revertCache.get(cache) != null);
							}
						}, cache, RandomUtils.generateRandomInt(10, 15), TimeUnit.SECONDS);
					}
				} while (hasDehydratedFarmland(farmBlocks) || hasUngrownCrops(farmBlocks));
			}
		});
	}

	private boolean hasUngrownCrops(@Nullable Set<Block> blocks) {
		if (blocks == null || blocks.isEmpty())
			return false;

		boolean ungrownStage = false;
		for (Block farm : blocks) {
			if (farm == null || farm.getType() != Material.FARMLAND)
				continue;
			if (!farm.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				continue;

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
		}
		return ungrownStage;
	}

	private boolean hasDehydratedFarmland(@Nullable Set<Block> blocks) {
		if (blocks == null || blocks.isEmpty())
			return false;

		boolean dehydrated = false;
		for (Block farm : blocks) {
			if (farm == null || farm.getType() != Material.FARMLAND)
				continue;
			if (!farm.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				continue;

			if (farm.getBlockData() instanceof Farmland) {
				Farmland farmland = (Farmland) farm.getBlockData();
				if (farmland.getMoisture() < farmland.getMaximumMoisture()) {
					dehydrated = true;
					break;
				}
			}
		}
		return dehydrated;
	}

	private boolean checkForLavaAroundFarm(@Nullable Block block) {
		if (block == null || block.isEmpty() || block.getType() != Material.FARMLAND)
			return false;
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return false;

		boolean lavaFound = false;
		int centerX = block.getLocation().getBlockX();
		int centerY = block.getLocation().getBlockY();
		int centerZ = block.getLocation().getBlockZ();
		for (int x = centerX - 4; x <= centerX + 4; x++) {
			for (int y = centerY - 1; y <= centerY; y++) {
				for (int z = centerZ - 4; z <= centerZ + 4; z++) {
					Block b = block.getWorld().getBlockAt(x, y, z);
					if (b.getType() == Material.AIR)
						continue;
					if (b.getType() == Material.LAVA) {
						lavaFound = true;
						break;
					}
				}
			}
		}
		return lavaFound;
	}

	private @Nullable Set<Block> getFarmlandOnHellblock(@NonNull HellblockPlayer hbPlayer) {
		Player player = hbPlayer.getPlayer();
		if (player == null || !player.isOnline())
			return null;
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return null;
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			Set<Block> regionBlocks = new HashSet<>();
			// lessen the load on the server
			if (this.regionCache.containsKey(hbPlayer) && this.regionCache.get(hbPlayer) != null) {
				Collection<Location> cached = this.regionCache.get(hbPlayer);
				for (Location cache : cached) {
					Block cachedBlock = cache.getBlock();
					regionBlocks.add(cachedBlock);
				}
				instance.getScheduler().runTaskAsyncLater(() -> resetRegionCache(hbPlayer), 3, TimeUnit.MINUTES);
				return regionBlocks;
			}
			Set<ProtectedRegion> wgRegion = instance.getWorldGuardHandler().getRegions(player.getUniqueId());
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
				}
			}

			// before we finish all of the original code we must cache it all
			// cache regions to keep it not thread heavy
			if (!this.regionCache.containsKey(hbPlayer)) {
				Collection<Location> localCache = new HashSet<>();
				regionBlocks.forEach(
						cachedBlock -> localCache.add(LocationCache.getCachedLocation(cachedBlock.getLocation())));
				this.regionCache.put(hbPlayer, localCache);
			}

			return regionBlocks;
		} else {
			// TODO: plugin protection
		}
		return null;
	}

	private void resetRegionCache(@NonNull HellblockPlayer hbPlayer) {
		if (this.regionCache.containsKey(hbPlayer)) {
			this.regionCache.remove(hbPlayer);
		}
	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			if (Tag.CROPS.isTagged(block.getRelative(BlockFace.UP).getType())
					|| block.getRelative(BlockFace.UP).isEmpty()) {
				if (checkForLavaAroundFarm(block) || (instance.getLavaRainHandler().getLavaRainTask() != null
						&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining()
						&& instance.getLavaRainHandler().getHighestBlock(block.getLocation()) != null
						&& instance.getLavaRainHandler().getHighestBlock(block.getLocation()).isEmpty())) {
					event.setCancelled(true);
					Collection<LivingEntity> entitiesNearby = block.getWorld()
							.getNearbyLivingEntities(block.getLocation(), 25, 25, 25);
					Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
							entitiesNearby);
					if (player != null)
						trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
				} else {
					if (this.blockCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.blockCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
					if (this.revertCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.revertCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
					if (this.moistureCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.moistureCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
				}
			}
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
			Collection<LivingEntity> entitiesNearby = block.getWorld().getNearbyLivingEntities(block.getLocation(), 25,
					25, 25);
			Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					entitiesNearby);
			if (player != null) {
				trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
			}
			if (this.blockCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
				this.blockCache.remove(LocationCache.getCachedLocation(block.getLocation()));
			}
			if (this.revertCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
				this.revertCache.remove(LocationCache.getCachedLocation(block.getLocation()));
			}
			if (this.moistureCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
				this.moistureCache.remove(LocationCache.getCachedLocation(block.getLocation()));
			}
		}
	}

	@EventHandler
	public void onLavaPlace(PlayerBucketEmptyEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getBucket() == Material.LAVA_BUCKET) {
			trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
		}
	}

	@EventHandler
	public void onLavaPickup(PlayerBucketFillEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getItemStack() != null && event.getItemStack().getType() == Material.LAVA_BUCKET) {
			resetRegionCache(instance.getHellblockHandler().getActivePlayer(player));
			trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			Player player = event.getPlayer();
			trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
			if (block.getBlockData() instanceof Farmland) {
				if (this.blockCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
					this.blockCache.remove(LocationCache.getCachedLocation(block.getLocation()));
				}
				if (this.revertCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
					this.revertCache.remove(LocationCache.getCachedLocation(block.getLocation()));
				}
				if (this.moistureCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
					this.moistureCache.remove(LocationCache.getCachedLocation(block.getLocation()));
				}
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Player player = event.getPlayer();
		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
		}

		if (event.getBlockReplacedState().getType() == Material.LAVA) {
			resetRegionCache(instance.getHellblockHandler().getActivePlayer(player));
			trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
		}
	}

	@EventHandler
	public void onMoistureChange(MoistureChangeEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			if (checkForLavaAroundFarm(block) || (instance.getLavaRainHandler().getLavaRainTask() != null
					&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining()
					&& instance.getLavaRainHandler().getHighestBlock(block.getLocation()) != null
					&& instance.getLavaRainHandler().getHighestBlock(block.getLocation()).isEmpty())) {
				event.setCancelled(true);
				Collection<LivingEntity> entitiesNearby = block.getWorld().getNearbyLivingEntities(block.getLocation(),
						25, 25, 25);
				Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
						entitiesNearby);
				if (player != null) {
					trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
				}
			} else {
				if (this.moistureCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
					this.moistureCache.remove(LocationCache.getCachedLocation(block.getLocation()));
				}
			}
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
				trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
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
			trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
		}
	}

	@EventHandler
	public void onGrow(BlockGrowEvent event) {
		Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (block.getBlockData() instanceof Ageable) {
			Collection<LivingEntity> entitiesNearby = block.getWorld().getNearbyLivingEntities(block.getLocation(), 25,
					25, 25);
			Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					entitiesNearby);
			if (player != null) {
				trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
			}
		}
	}

	@EventHandler
	public void onFarmland(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL)) {
			return;
		}

		if (event.getItem() != null && (!(Tag.ITEMS_HOES.isTagged(event.getItem().getType())
				|| Tag.CROPS.isTagged(event.getItem().getType())))) {
			return;
		}

		Block block = event.getClickedBlock();
		if (block != null) {
			if (Tag.DIRT.isTagged(block.getType()) || block.getBlockData() instanceof Farmland) {
				trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
				if (event.getAction() == Action.PHYSICAL
						&& (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable)) {
					if (this.blockCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.blockCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
					if (this.revertCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.revertCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
					if (this.moistureCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.moistureCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
				}
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
			if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
				Collection<LivingEntity> entitiesNearby = block.getWorld().getNearbyLivingEntities(block.getLocation(),
						25, 25, 25);
				Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
						entitiesNearby);
				if (player != null)
					trackNetherFarms(instance.getHellblockHandler().getActivePlayer(player));
				if (block.getBlockData() instanceof Farmland) {
					if (this.blockCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.blockCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
					if (this.revertCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.revertCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
					if (this.moistureCache.containsKey(LocationCache.getCachedLocation(block.getLocation()))) {
						this.moistureCache.remove(LocationCache.getCachedLocation(block.getLocation()));
					}
				}
			}
		}
	}
}
