package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationCache;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;

public class NetherFarming implements Listener {

	protected final HellblockPlugin instance;

	private final Map<Location, Integer> blockCache, revertCache, moistureCache;
	private final Map<UserData, Collection<Location>> regionFarmCache;

	public NetherFarming(HellblockPlugin plugin) {
		instance = plugin;
		this.blockCache = new LinkedHashMap<>();
		this.revertCache = new LinkedHashMap<>();
		this.moistureCache = new LinkedHashMap<>();
		this.regionFarmCache = new LinkedHashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public CompletableFuture<Void> trackNetherFarms(@NotNull UserData user) {
		return CompletableFuture.runAsync(() -> {
			Set<Block> farmBlocks = getFarmlandOnHellblock(user);
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
									&& updatingBlock.getWorld().getHighestBlockAt(cache).isEmpty())) {
						instance.getScheduler().sync().runLater(() -> {
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
										if (user.getPlayer() != null) {
											if (!user.getChallengeData()
													.isChallengeActive(ChallengeType.NETHER_FARM_CHALLENGE)
													&& !user.getChallengeData().isChallengeCompleted(
															ChallengeType.NETHER_FARM_CHALLENGE)) {
												user.getChallengeData().beginChallengeProgression(user.getPlayer(),
														ChallengeType.NETHER_FARM_CHALLENGE);
											} else {
												user.getChallengeData().updateChallengeProgression(user.getPlayer(),
														ChallengeType.NETHER_FARM_CHALLENGE, 1);
												if (user.getChallengeData()
														.isChallengeCompleted(ChallengeType.NETHER_FARM_CHALLENGE)) {
													user.getChallengeData().completeChallenge(user.getPlayer(),
															ChallengeType.NETHER_FARM_CHALLENGE);
												}
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
						}, RandomUtils.generateRandomInt(15, 30) * 20L, cache);
					} else {
						instance.getScheduler().sync().runLater(() -> {
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
												updatingBlock.getRelative(BlockFace.UP).breakNaturally();
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
						}, RandomUtils.generateRandomInt(10, 15) * 20L, cache);
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
			if (!farm.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
			if (!farm.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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

	private @Nullable Set<Block> getFarmlandOnHellblock(@NotNull UserData user) {
		Player player = user.getPlayer();
		if (!user.isOnline())
			return null;
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return null;
		if (instance.getConfigManager().worldguardProtect()) {
			Set<Block> regionBlocks = new HashSet<>();
			// lessen the load on the server
			if (this.regionFarmCache.containsKey(user) && this.regionFarmCache.get(user) != null) {
				Collection<Location> cached = this.regionFarmCache.get(user);
				for (Location cache : cached) {
					Block cachedBlock = cache.getBlock();
					regionBlocks.add(cachedBlock);
				}
				instance.getScheduler().asyncLater(() -> resetRegionFarmCache(user), 3, TimeUnit.MINUTES);
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
			if (!this.regionFarmCache.containsKey(user)) {
				Collection<Location> localCache = new HashSet<>();
				regionBlocks.forEach(
						cachedBlock -> localCache.add(LocationCache.getCachedLocation(cachedBlock.getLocation())));
				this.regionFarmCache.put(user, localCache);
			}

			return regionBlocks;
		} else {
			// TODO: plugin protection
		}
		return null;
	}

	private void resetRegionFarmCache(@NotNull UserData user) {
		if (this.regionFarmCache.containsKey(user)) {
			this.regionFarmCache.remove(user);
		}
	}

	private final static BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	private final static Set<Material> sugarCaneGrowBlocks = Set.of(Material.GRASS_BLOCK, Material.DIRT,
			Material.COARSE_DIRT, Material.MYCELIUM, Material.SAND, Material.RED_SAND, Material.SUSPICIOUS_SAND,
			Material.MUD, Material.MOSS_BLOCK, Material.PODZOL, Material.ROOTED_DIRT);

	private boolean checkForLavaAroundSugarCane(@Nullable Block block) {
		if (block == null || block.isEmpty() || !(sugarCaneGrowBlocks.contains(block.getType())))
			return false;
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return false;

		boolean lavaFound = false;
		for (BlockFace face : FACES) {
			if (block.getRelative(face).getType() == Material.LAVA) {
				lavaFound = true;
				break;
			}
		}

		return lavaFound;
	}

	@EventHandler
	public void onFlowConcretePowder(BlockFromToEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (!Arrays.asList(FACES).contains(event.getFace())) {
			return;
		}

		if (block.getType() == Material.LAVA) {
			for (BlockFace face : FACES) {
				if (Tag.CONCRETE_POWDER.isTagged(event.getToBlock().getRelative(face).getType())) {
					event.getToBlock().getRelative(face)
							.setType(convertToConcrete(event.getToBlock().getRelative(face).getType()));
					for (Entity entity : block.getWorld()
							.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
									instance.getConfigManager().searchRadius(),
									instance.getConfigManager().searchRadius())
							.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
						if (entity instanceof Player player) {
							Audience audience = instance.getSenderFactory().getAudience(player);
							audience.playSound(
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.lava.extinguish"),
											net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
						}
					}
					break;
				}
			}
		}
	}

	@EventHandler
	public void onPlaceConcretePowder(BlockPlaceEvent event) {
		final Block block = event.getBlockPlaced();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (Tag.CONCRETE_POWDER.isTagged(block.getType())) {
			for (BlockFace face : FACES) {
				if (block.getRelative(face).getType() == Material.LAVA) {
					block.setType(convertToConcrete(block.getType()));
					for (Entity entity : event.getPlayer().getWorld().getNearbyEntities(event.getPlayer().getLocation(),
							instance.getConfigManager().searchRadius(), instance.getConfigManager().searchRadius(),
							instance.getConfigManager().searchRadius()).stream()
							.filter(e -> e.getType() == EntityType.PLAYER).toList()) {
						if (entity instanceof Player player) {
							Audience audience = instance.getSenderFactory().getAudience(player);
							audience.playSound(
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.lava.extinguish"),
											net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
						}
					}
					break;
				}
			}
		}
	}

	@EventHandler
	public void onFallConcretePowder(EntityDropItemEvent event) {
		if (event.getEntity() instanceof FallingBlock fallingBlock) {
			if (!fallingBlock.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
				return;
			if (Tag.CONCRETE_POWDER.isTagged(fallingBlock.getBlockData().getMaterial())) {
				Material powder = fallingBlock.getBlockData().getMaterial();
				if (fallingBlock.getLocation().getBlock().getRelative(BlockFace.UP).getType() == Material.LAVA) {
					event.setCancelled(true);
					instance.getScheduler().sync().runLater(() -> fallingBlock.getLocation().getBlock()
							.getRelative(BlockFace.UP).setType(convertToConcrete(powder)), 5,
							fallingBlock.getLocation());
					for (Entity entity : fallingBlock.getWorld()
							.getNearbyEntities(fallingBlock.getLocation(), instance.getConfigManager().searchRadius(),
									instance.getConfigManager().searchRadius(),
									instance.getConfigManager().searchRadius())
							.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
						if (entity instanceof Player player) {
							Audience audience = instance.getSenderFactory().getAudience(player);
							audience.playSound(
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.lava.extinguish"),
											net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
						}
					}
					fallingBlock.setDropItem(false);
				} else {
					for (BlockFace face : FACES) {
						if (fallingBlock.getLocation().getBlock().getRelative(face).getType() == Material.LAVA) {
							event.setCancelled(true);
							instance.getScheduler().sync().runLater(
									() -> fallingBlock.getLocation().getBlock().setType(convertToConcrete(powder)), 5,
									fallingBlock.getLocation());
							for (Entity entity : fallingBlock.getWorld()
									.getNearbyEntities(fallingBlock.getLocation(),
											instance.getConfigManager().searchRadius(),
											instance.getConfigManager().searchRadius(),
											instance.getConfigManager().searchRadius())
									.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
								if (entity instanceof Player player) {
									Audience audience = instance.getSenderFactory().getAudience(player);
									audience.playSound(Sound.sound(
											net.kyori.adventure.key.Key.key("minecraft:block.lava.extinguish"),
											net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
								}
							}
							fallingBlock.setDropItem(false);
							break;
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onForceCocoaBeanPlacement(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		final Block block = event.getClickedBlock();
		final Material cocoaBean = event.getMaterial();
		final BlockFace face = event.getBlockFace();

		if (!Arrays.asList(FACES).contains(face)) {
			return;
		}

		if (block != null) {
			if (cocoaBean == Material.COCOA_BEANS) {
				if ((block.getType() == Material.CRIMSON_STEM || block.getType() == Material.WARPED_STEM)
						&& block.getRelative(face).isEmpty()) {
					event.setUseItemInHand(Result.ALLOW);
					if (player.getGameMode() != GameMode.CREATIVE)
						event.getItem()
								.setAmount(event.getItem().getAmount() > 0 ? event.getItem().getAmount() - 1 : 0);
					instance.getVersionManager().getNMSManager().swingHand(player,
							event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
					instance.getSenderFactory().getAudience(player)
							.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.wood.place"),
									net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					player.updateInventory();
					block.getRelative(face).setType(Material.COCOA);
					if (block.getRelative(face).getBlockData() instanceof Cocoa data) {
						data.setFacing(face.getOppositeFace());
						block.getRelative(face).setBlockData(data);
					}
				}
			}
		}
	}

	@EventHandler
	public void onForceSugarCanePlacement(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		final Block block = event.getClickedBlock();
		final Material sugarCane = event.getMaterial();
		final BlockFace face = event.getBlockFace();
		if (block != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (face == BlockFace.UP) {
				if (sugarCane == Material.SUGAR_CANE) {
					if (sugarCaneGrowBlocks.contains(block.getType()) && block.getRelative(face).isEmpty()) {
						if (checkForLavaAroundSugarCane(block)) {
							event.setUseItemInHand(Result.ALLOW);
							if (player.getGameMode() != GameMode.CREATIVE)
								event.getItem().setAmount(
										event.getItem().getAmount() > 0 ? event.getItem().getAmount() - 1 : 0);
							instance.getVersionManager().getNMSManager().swingHand(player,
									event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
							audience.playSound(
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.place"),
											net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
							player.updateInventory();
							block.getRelative(face).setType(Material.SUGAR_CANE);
						}
					}
					if (block.getType() == Material.SUGAR_CANE) {
						if (checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN))) {
							event.setUseItemInHand(Result.ALLOW);
							if (player.getGameMode() != GameMode.CREATIVE)
								event.getItem().setAmount(
										event.getItem().getAmount() > 0 ? event.getItem().getAmount() - 1 : 0);
							instance.getVersionManager().getNMSManager().swingHand(player,
									event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
							audience.playSound(
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.place"),
											net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
							player.updateInventory();
							block.getRelative(face).setType(Material.SUGAR_CANE);
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onGrowSugarCane(BlockGrowEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.isEmpty() && block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE
				&& block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
			if (checkForLavaAroundSugarCane(
					block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN))) {
				event.setCancelled(true);
				return;
			} else {
				event.setCancelled(true);
				block.setType(Material.AIR);
				block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.SUGAR_CANE));
				block.getRelative(BlockFace.DOWN).setType(Material.AIR);
				block.getWorld().spawnParticle(Particle.BLOCK, block.getRelative(BlockFace.DOWN).getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(block.getRelative(BlockFace.DOWN).getLocation(),
						new ItemStack(Material.SUGAR_CANE));
				block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).setType(Material.AIR);
				block.getWorld().spawnParticle(Particle.BLOCK,
						block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(
						block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getLocation(),
						new ItemStack(Material.SUGAR_CANE));
				for (Entity entity : block.getWorld()
						.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
								instance.getConfigManager().searchRadius(), instance.getConfigManager().searchRadius())
						.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
					if (entity instanceof Player player) {
						Audience audience = instance.getSenderFactory().getAudience(player);
						audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
								net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
					}
				}
				return;
			}
		}

		if (block.getType() == Material.SUGAR_CANE
				&& block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
			if (checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN))) {
				event.setCancelled(true);
				return;
			} else {
				event.setCancelled(true);
				block.setType(Material.AIR);
				block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.SUGAR_CANE));
				block.getRelative(BlockFace.DOWN).setType(Material.AIR);
				block.getWorld().spawnParticle(Particle.BLOCK, block.getRelative(BlockFace.DOWN).getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(block.getRelative(BlockFace.DOWN).getLocation(),
						new ItemStack(Material.SUGAR_CANE));
				for (Entity entity : block.getWorld()
						.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
								instance.getConfigManager().searchRadius(), instance.getConfigManager().searchRadius())
						.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
					if (entity instanceof Player player) {
						Audience audience = instance.getSenderFactory().getAudience(player);
						audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
								net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
					}
				}
				return;
			}
		}

		if (block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE && block.isEmpty()) {
			if (checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN))) {
				event.setCancelled(true);
				block.setType(Material.SUGAR_CANE);
				for (Entity entity : block.getWorld()
						.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
								instance.getConfigManager().searchRadius(), instance.getConfigManager().searchRadius())
						.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
					if (entity instanceof Player player) {
						Audience audience = instance.getSenderFactory().getAudience(player);
						audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.place"),
								net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
					}
				}
				return;
			} else {
				event.setCancelled(true);
				instance.getScheduler().sync().runLater(() -> {
					block.getRelative(BlockFace.DOWN).setType(Material.AIR);
					block.getWorld().spawnParticle(Particle.BLOCK, block.getRelative(BlockFace.DOWN).getLocation(), 5,
							Material.SUGAR_CANE.createBlockData());
					block.getWorld().dropItemNaturally(block.getRelative(BlockFace.DOWN).getLocation(),
							new ItemStack(Material.SUGAR_CANE));
					for (Entity entity : block.getWorld()
							.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
									instance.getConfigManager().searchRadius(),
									instance.getConfigManager().searchRadius())
							.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
						if (entity instanceof Player player) {
							Audience audience = instance.getSenderFactory().getAudience(player);
							audience.playSound(
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
											net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
						}
					}
				}, 5, block.getRelative(BlockFace.DOWN).getLocation());
				return;
			}
		}
	}

	@EventHandler
	public void onSugarCaneUpdate(BlockPhysicsEvent event) {
		final Block block = event.getSourceBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.getType() == Material.SUGAR_CANE) {
			if (checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN))) {
				event.setCancelled(true);
			}
		}
		if (block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
			if (checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN))) {
				event.setCancelled(true);
			}
		}
		for (BlockFace face : FACES) {
			if (block.getRelative(face).getType() == Material.SUGAR_CANE) {
				if (checkForLavaAroundSugarCane(block.getRelative(face).getRelative(BlockFace.DOWN))) {
					event.setCancelled(true);
				}
			}
			if (block.getRelative(face).getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
				if (checkForLavaAroundSugarCane(
						block.getRelative(face).getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN))) {
					event.setCancelled(true);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPistonExtendSugarCane(BlockPistonExtendEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		for (Block lava : event.getBlocks()) {
			if (lava.getType() == Material.LAVA) {
				for (BlockFace face : FACES) {
					if (block.getRelative(face).getType() == Material.LAVA)
						break;
					if (sugarCaneGrowBlocks.contains(lava.getRelative(face).getType())) {
						if (lava.getRelative(face).getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
							if (lava.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
									.getRelative(BlockFace.UP).getType().hasGravity()) {
								FallingBlock falling = block.getWorld().spawnFallingBlock(
										lava.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
												.getRelative(BlockFace.UP).getLocation().add(0.5D, 0, 0.5D),
										lava.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
												.getRelative(BlockFace.UP).getBlockData());
								falling.setHurtEntities(true);
								falling.setDropItem(true);
								lava.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
										.getRelative(BlockFace.UP).setType(Material.AIR);
							}
							if (Tag.WOOL_CARPETS.isTagged(lava.getRelative(face).getRelative(BlockFace.UP)
									.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType())) {
								block.getWorld().dropItemNaturally(
										lava.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
												.getRelative(BlockFace.UP).getLocation(),
										new ItemStack(lava.getRelative(face).getRelative(BlockFace.UP)
												.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType()));
								lava.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
										.getRelative(BlockFace.UP).setType(Material.AIR);
							}
							if (lava.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
									.getType() == Material.SUGAR_CANE) {
								block.getWorld().dropItemNaturally(lava.getRelative(face).getRelative(BlockFace.UP)
										.getRelative(BlockFace.UP).getLocation(), new ItemStack(Material.SUGAR_CANE));
								block.getWorld().spawnParticle(
										Particle.BLOCK, lava.getRelative(face).getRelative(BlockFace.UP)
												.getRelative(BlockFace.UP).getLocation(),
										5, Material.SUGAR_CANE.createBlockData());
								lava.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
										.setType(Material.AIR);
							}
							block.getWorld().dropItemNaturally(
									lava.getRelative(face).getRelative(BlockFace.UP).getLocation(),
									new ItemStack(Material.SUGAR_CANE));
							block.getWorld().spawnParticle(Particle.BLOCK,
									lava.getRelative(face).getRelative(BlockFace.UP).getLocation(), 5,
									Material.SUGAR_CANE.createBlockData());
							lava.getRelative(face).getRelative(BlockFace.UP).setType(Material.AIR);
							for (Entity entity : block.getWorld()
									.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
											instance.getConfigManager().searchRadius(),
											instance.getConfigManager().searchRadius())
									.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
								if (entity instanceof Player player) {
									Audience audience = instance.getSenderFactory().getAudience(player);
									audience.playSound(
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
													net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
								}
							}
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onBreakSugarCane(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		final Player player = event.getPlayer();
		if (block.getType() == Material.SUGAR_CANE) {
			if (checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN))
					|| checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN))) {
				event.setCancelled(true);
				if (block.getRelative(BlockFace.UP).getType().hasGravity()) {
					FallingBlock falling = block.getWorld().spawnFallingBlock(
							block.getRelative(BlockFace.UP).getLocation().add(0.5D, 0, 0.5D),
							block.getRelative(BlockFace.UP).getBlockData());
					falling.setHurtEntities(true);
					falling.setDropItem(true);
					block.getRelative(BlockFace.UP).setType(Material.AIR);
				}
				if (Tag.WOOL_CARPETS.isTagged(block.getRelative(BlockFace.UP).getType())) {
					block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation(),
							new ItemStack(block.getRelative(BlockFace.UP).getType()));
					block.getRelative(BlockFace.UP).setType(Material.AIR);
				}
				if (block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType().hasGravity()) {
					FallingBlock falling = block.getWorld().spawnFallingBlock(
							block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation().add(0.5D, 0, 0.5D),
							block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getBlockData());
					falling.setHurtEntities(true);
					falling.setDropItem(true);
					block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
				}
				if (Tag.WOOL_CARPETS.isTagged(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType())) {
					block.getWorld().dropItemNaturally(
							block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation(),
							new ItemStack(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType()));
					block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
				}
				if (block.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
					block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation(),
							new ItemStack(Material.SUGAR_CANE));
					block.getWorld().spawnParticle(Particle.BLOCK, block.getRelative(BlockFace.UP).getLocation(), 5,
							Material.SUGAR_CANE.createBlockData());
					block.getRelative(BlockFace.UP).setType(Material.AIR, false);
				}
				block.setType(Material.AIR, false);
				block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.SUGAR_CANE));
				instance.getSenderFactory().getAudience(player)
						.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
			} else {
				event.setCancelled(true);
				if (block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType().hasGravity()) {
					FallingBlock falling = block.getWorld().spawnFallingBlock(
							block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation().add(0.5D, 0, 0.5D),
							block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getBlockData());
					falling.setHurtEntities(true);
					falling.setDropItem(true);
					block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
				}
				if (Tag.WOOL_CARPETS.isTagged(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType())) {
					block.getWorld().dropItemNaturally(
							block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation(),
							new ItemStack(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType()));
					block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
				}
				if (block.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
					block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation(),
							new ItemStack(Material.SUGAR_CANE));
					block.getWorld().spawnParticle(Particle.BLOCK, block.getRelative(BlockFace.UP).getLocation(), 5,
							Material.SUGAR_CANE.createBlockData());
					block.getRelative(BlockFace.UP).setType(Material.AIR);
				}
				block.setType(Material.AIR);
				block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.SUGAR_CANE));
				instance.getSenderFactory().getAudience(player)
						.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlaceSugarCane(BlockPlaceEvent event) {
		final Block block = event.getBlockPlaced();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		final Player player = event.getPlayer();
		if (block.getType() == Material.SUGAR_CANE && block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE
				&& block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
			event.setCancelled(true);
			block.setType(Material.AIR);
			block.getRelative(BlockFace.DOWN).setType(Material.AIR);
			block.getWorld().spawnParticle(Particle.BLOCK, block.getRelative(BlockFace.DOWN).getLocation(), 5,
					Material.SUGAR_CANE.createBlockData());
			block.getWorld().dropItemNaturally(block.getRelative(BlockFace.DOWN).getLocation(),
					new ItemStack(Material.SUGAR_CANE));
			block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).setType(Material.AIR);
			block.getWorld().spawnParticle(Particle.BLOCK,
					block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getLocation(), 5,
					Material.SUGAR_CANE.createBlockData());
			block.getWorld().dropItemNaturally(
					block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getLocation(),
					new ItemStack(Material.SUGAR_CANE));
			instance.getSenderFactory().getAudience(player)
					.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
			return;
		}
		if (block.getType() == Material.SUGAR_CANE
				&& block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
			if (checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN))) {
				event.setCancelled(true);
				if (player.getGameMode() != GameMode.CREATIVE)
					event.getItemInHand().setAmount(
							event.getItemInHand().getAmount() > 0 ? event.getItemInHand().getAmount() - 1 : 0);
				block.setType(Material.SUGAR_CANE);
				instance.getSenderFactory().getAudience(player)
						.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.place"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
			} else {
				event.setCancelled(true);
				if (block.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
					block.getWorld().dropItemNaturally(block.getRelative(BlockFace.UP).getLocation(),
							new ItemStack(Material.SUGAR_CANE));
					block.getRelative(BlockFace.UP).setType(Material.AIR);
				}
				block.setType(Material.AIR);
				block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.SUGAR_CANE));
				block.getRelative(BlockFace.DOWN).setType(Material.AIR);
				block.getWorld().spawnParticle(Particle.BLOCK, block.getRelative(BlockFace.DOWN).getLocation(), 5,
						Material.SUGAR_CANE.createBlockData());
				block.getWorld().dropItemNaturally(block.getRelative(BlockFace.DOWN).getLocation(),
						new ItemStack(Material.SUGAR_CANE));
				instance.getSenderFactory().getAudience(player)
						.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
			}
		}

		if (event.getBlockReplacedState().getType() == Material.LAVA) {
			for (BlockFace face : FACES) {
				if (sugarCaneGrowBlocks
						.contains(event.getBlockReplacedState().getBlock().getRelative(face).getType())) {
					if (event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
							.getType() == Material.SUGAR_CANE) {
						if (event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
								.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType().hasGravity()) {
							FallingBlock falling = block.getWorld().spawnFallingBlock(
									event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation()
											.add(0.5D, 0, 0.5D),
									event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getBlockData());
							falling.setHurtEntities(true);
							falling.setDropItem(true);
							event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
									.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
						}
						if (Tag.WOOL_CARPETS.isTagged(
								event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
										.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType())) {
							block.getWorld().dropItemNaturally(
									event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation(),
									new ItemStack(event.getBlockReplacedState().getBlock().getRelative(face)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getType()));
							event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
									.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
						}
						if (event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
								.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
							block.getWorld().dropItemNaturally(
									event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getLocation(),
									new ItemStack(Material.SUGAR_CANE));
							block.getWorld().spawnParticle(Particle.BLOCK,
									event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getLocation(),
									5, Material.SUGAR_CANE.createBlockData());
							event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
									.getRelative(BlockFace.UP).setType(Material.AIR);
						}
						block.getWorld().dropItemNaturally(event.getBlockReplacedState().getBlock().getRelative(face)
								.getRelative(BlockFace.UP).getLocation(), new ItemStack(Material.SUGAR_CANE));
						block.getWorld().spawnParticle(
								Particle.BLOCK, event.getBlockReplacedState().getBlock().getRelative(face)
										.getRelative(BlockFace.UP).getLocation(),
								5, Material.SUGAR_CANE.createBlockData());
						event.getBlockReplacedState().getBlock().getRelative(face).getRelative(BlockFace.UP)
								.setType(Material.AIR);
						instance.getSenderFactory().getAudience(player)
								.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
										net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPickUpLavaWithDispenseSugarCane(BlockDispenseEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.getState() instanceof Dispenser dispenser) {
			if (dispenser.getInventory().contains(Material.BUCKET)
					&& !dispenser.getInventory().contains(Material.LAVA_BUCKET)) {
				if (dispenser.getBlockData() instanceof Directional direction) {
					if (block.getRelative(direction.getFacing()).getType() == Material.LAVA) {
						for (BlockFace face : FACES) {
							if (block.getRelative(direction.getFacing()).getRelative(face).getType() == Material.LAVA)
								break;
							if (sugarCaneGrowBlocks
									.contains(block.getRelative(direction.getFacing()).getRelative(face).getType())) {
								if (block.getRelative(direction.getFacing()).getRelative(face).getRelative(BlockFace.UP)
										.getType() == Material.SUGAR_CANE) {
									if (block.getRelative(direction.getFacing()).getRelative(face)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getType().hasGravity()) {
										FallingBlock falling = block.getWorld().spawnFallingBlock(
												block.getRelative(direction.getFacing()).getRelative(face)
														.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
														.getRelative(BlockFace.UP).getLocation().add(0.5D, 0, 0.5D),
												block.getRelative(direction.getFacing()).getRelative(face)
														.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
														.getRelative(BlockFace.UP).getBlockData());
										falling.setHurtEntities(true);
										falling.setDropItem(true);
										block.getRelative(direction.getFacing()).getRelative(face)
												.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
												.getRelative(BlockFace.UP).setType(Material.AIR);
									}
									if (Tag.WOOL_CARPETS.isTagged(block.getRelative(direction.getFacing())
											.getRelative(face).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getType())) {
										block.getWorld().dropItemNaturally(
												block.getRelative(direction.getFacing()).getRelative(face)
														.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
														.getRelative(BlockFace.UP).getLocation(),
												new ItemStack(block.getRelative(direction.getFacing()).getRelative(face)
														.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
														.getRelative(BlockFace.UP).getType()));
										block.getRelative(direction.getFacing()).getRelative(face)
												.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
												.getRelative(BlockFace.UP).setType(Material.AIR);
									}
									if (block.getRelative(direction.getFacing()).getRelative(face)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
											.getType() == Material.SUGAR_CANE) {
										block.getWorld()
												.dropItemNaturally(block.getRelative(direction.getFacing())
														.getRelative(face).getRelative(BlockFace.UP)
														.getRelative(BlockFace.UP).getLocation(),
														new ItemStack(Material.SUGAR_CANE));
										block.getWorld().spawnParticle(Particle.BLOCK,
												block.getRelative(direction.getFacing()).getRelative(face)
														.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
														.getLocation(),
												5, Material.SUGAR_CANE.createBlockData());
										block.getRelative(direction.getFacing()).getRelative(face)
												.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
												.setType(Material.AIR);
									}
									block.getWorld().dropItemNaturally(
											block.getRelative(direction.getFacing()).getRelative(face)
													.getRelative(BlockFace.UP).getLocation(),
											new ItemStack(Material.SUGAR_CANE));
									block.getWorld().spawnParticle(Particle.BLOCK,
											block.getRelative(direction.getFacing()).getRelative(face)
													.getRelative(BlockFace.UP).getLocation(),
											5, Material.SUGAR_CANE.createBlockData());
									block.getRelative(direction.getFacing()).getRelative(face).getRelative(BlockFace.UP)
											.setType(Material.AIR);
									for (Entity entity : block.getWorld()
											.getNearbyEntities(block.getLocation(),
													instance.getConfigManager().searchRadius(),
													instance.getConfigManager().searchRadius(),
													instance.getConfigManager().searchRadius())
											.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
										if (entity instanceof Player player) {
											Audience audience = instance.getSenderFactory().getAudience(player);
											audience.playSound(Sound.sound(
													net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
													net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPickUpLavaNextToSugarCane(PlayerBucketFillEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (event.getItemStack() != null && event.getItemStack().getType() == Material.LAVA_BUCKET) {
			for (BlockFace face : FACES) {
				if (event.getBlockClicked().getRelative(face).getType() == Material.LAVA)
					break;
				if (sugarCaneGrowBlocks.contains(event.getBlockClicked().getRelative(face).getType())) {
					if (event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
							.getType() == Material.SUGAR_CANE) {
						if (event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
								.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType().hasGravity()) {
							FallingBlock falling = event.getBlockClicked().getWorld().spawnFallingBlock(
									event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation()
											.add(0.5D, 0, 0.5D),
									event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getBlockData());
							falling.setHurtEntities(true);
							falling.setDropItem(true);
							event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
									.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
						}
						if (Tag.WOOL_CARPETS
								.isTagged(event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
										.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType())) {
							event.getBlockClicked().getWorld().dropItemNaturally(
									event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation(),
									new ItemStack(event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType()));
							event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
									.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
						}
						if (event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
								.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
							event.getBlockClicked().getWorld().dropItemNaturally(
									event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getLocation(),
									new ItemStack(Material.SUGAR_CANE));
							event.getBlockClicked().getWorld().spawnParticle(Particle.BLOCK,
									event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
											.getRelative(BlockFace.UP).getLocation(),
									5, Material.SUGAR_CANE.createBlockData());
							event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP)
									.getRelative(BlockFace.UP).setType(Material.AIR);
						}
						event.getBlockClicked().getWorld().dropItemNaturally(
								event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP).getLocation(),
								new ItemStack(Material.SUGAR_CANE));
						event.getBlockClicked().getWorld().spawnParticle(Particle.BLOCK,
								event.getBlockClicked().getLocation(), 5, Material.SUGAR_CANE.createBlockData());
						event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP).setType(Material.AIR);
						instance.getSenderFactory().getAudience(player)
								.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
										net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					}
				}
			}
		}

	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			if (Tag.CROPS.isTagged(block.getRelative(BlockFace.UP).getType())
					|| block.getRelative(BlockFace.UP).isEmpty()) {
				if (checkForLavaAroundFarm(block) || (instance.getLavaRainHandler().getLavaRainTask() != null
						&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining()
						&& block.getWorld().getHighestBlockAt(block.getLocation()).isEmpty())) {
					event.setCancelled(true);
					Collection<Entity> playersNearby = block.getWorld()
							.getNearbyEntities(block.getLocation(), 25, 25, 25).stream()
							.filter(e -> e.getType() == EntityType.PLAYER).toList();
					Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
							playersNearby);
					if (player != null) {
						Optional<UserData> onlineUser = instance.getStorageManager()
								.getOnlineUser(player.getUniqueId());
						if (!onlineUser.isEmpty())
							trackNetherFarms(onlineUser.get());
					}
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
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
					.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
			Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					playersNearby);
			if (player != null) {
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (!onlineUser.isEmpty())
					trackNetherFarms(onlineUser.get());
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
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (event.getBucket() == Material.LAVA_BUCKET) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (!onlineUser.isEmpty())
				trackNetherFarms(onlineUser.get());
		}
	}

	@EventHandler
	public void onLavaPickup(PlayerBucketFillEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (event.getItemStack() != null && event.getItemStack().getType() == Material.LAVA_BUCKET) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (!onlineUser.isEmpty()) {
				resetRegionFarmCache(onlineUser.get());
				trackNetherFarms(onlineUser.get());
			}
		}
	}

	@EventHandler
	public void onPickUpLavaWithDispense(BlockDispenseEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.getState() instanceof Dispenser dispenser) {
			if (dispenser.getInventory().contains(Material.BUCKET)
					&& !dispenser.getInventory().contains(Material.LAVA_BUCKET)) {
				if (dispenser.getBlockData() instanceof Directional direction) {
					if (block.getRelative(direction.getFacing()).getType() == Material.LAVA) {
						Collection<Entity> playersNearby = block.getWorld()
								.getNearbyEntities(block.getLocation(), 25, 25, 25).stream()
								.filter(e -> e.getType() == EntityType.PLAYER).toList();
						Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
								playersNearby);
						if (player != null) {
							Optional<UserData> onlineUser = instance.getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (!onlineUser.isEmpty()) {
								resetRegionFarmCache(onlineUser.get());
								trackNetherFarms(onlineUser.get());
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onPistonExtendIntoLava(BlockPistonExtendEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25).stream()
				.filter(e -> e.getType() == EntityType.PLAYER).toList();
		Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(), playersNearby);
		if (player != null) {
			for (Block lava : event.getBlocks()) {
				if (lava.getType() == Material.LAVA) {
					Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
					if (!onlineUser.isEmpty()) {
						resetRegionFarmCache(onlineUser.get());
						trackNetherFarms(onlineUser.get());
					}
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			final Player player = event.getPlayer();
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (!onlineUser.isEmpty())
				trackNetherFarms(onlineUser.get());
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
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		final Player player = event.getPlayer();
		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (!onlineUser.isEmpty())
				trackNetherFarms(onlineUser.get());
		}

		if (event.getBlockReplacedState().getType() == Material.LAVA) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (!onlineUser.isEmpty()) {
				resetRegionFarmCache(onlineUser.get());
				trackNetherFarms(onlineUser.get());
			}
		}
	}

	@EventHandler
	public void onMoistureChange(MoistureChangeEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			if (checkForLavaAroundFarm(block) || (instance.getLavaRainHandler().getLavaRainTask() != null
					&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining()
					&& block.getWorld().getHighestBlockAt(block.getLocation()).isEmpty())) {
				event.setCancelled(true);
				Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
						.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
				Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
						playersNearby);
				if (player != null) {
					Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
					if (!onlineUser.isEmpty())
						trackNetherFarms(onlineUser.get());
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
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		final List<BlockState> blocks = event.getBlocks();
		for (BlockState block : blocks) {
			if (block.getBlockData() instanceof Ageable) {
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (!onlineUser.isEmpty())
					trackNetherFarms(onlineUser.get());
			}
		}
	}

	@EventHandler
	public void onHarvest(PlayerHarvestBlockEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		final Block block = event.getHarvestedBlock();
		if (block.getBlockData() instanceof Ageable) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (!onlineUser.isEmpty())
				trackNetherFarms(onlineUser.get());
		}
	}

	@EventHandler
	public void onGrow(BlockGrowEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (block.getBlockData() instanceof Ageable) {
			Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
					.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
			Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					playersNearby);
			if (player != null) {
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (!onlineUser.isEmpty())
					trackNetherFarms(onlineUser.get());
			}
		}
	}

	@EventHandler
	public void onFarmland(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL)) {
			return;
		}

		if (event.getItem() != null && (!(Tag.ITEMS_HOES.isTagged(event.getItem().getType())
				|| Tag.CROPS.isTagged(event.getItem().getType())))) {
			return;
		}

		final Block block = event.getClickedBlock();
		if (block != null) {
			if (Tag.DIRT.isTagged(block.getType()) || block.getBlockData() instanceof Farmland) {
				new FarmUpdater(player.getUniqueId());
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
		final Entity entity = event.getEntity();
		if (!entity.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		if (entity instanceof Player)
			return;

		if (entity instanceof LivingEntity) {
			final Block block = event.getBlock();
			if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
				Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
						.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
				Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
						playersNearby);
				if (player != null) {
					Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
					if (!onlineUser.isEmpty())
						trackNetherFarms(onlineUser.get());
				}
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

	private @NotNull Material convertToConcrete(Material powderType) {
		Material concrete;
		switch (powderType) {
		case WHITE_CONCRETE_POWDER:
			concrete = Material.WHITE_CONCRETE;
			break;
		case BLACK_CONCRETE_POWDER:
			concrete = Material.BLACK_CONCRETE;
			break;
		case BROWN_CONCRETE_POWDER:
			concrete = Material.BROWN_CONCRETE;
			break;
		case GRAY_CONCRETE_POWDER:
			concrete = Material.GRAY_CONCRETE;
			break;
		case LIGHT_GRAY_CONCRETE_POWDER:
			concrete = Material.LIGHT_GRAY_CONCRETE;
			break;
		case BLUE_CONCRETE_POWDER:
			concrete = Material.BLUE_CONCRETE;
			break;
		case LIGHT_BLUE_CONCRETE_POWDER:
			concrete = Material.LIGHT_BLUE_CONCRETE;
			break;
		case CYAN_CONCRETE_POWDER:
			concrete = Material.CYAN_CONCRETE;
			break;
		case GREEN_CONCRETE_POWDER:
			concrete = Material.GREEN_CONCRETE;
			break;
		case LIME_CONCRETE_POWDER:
			concrete = Material.LIME_CONCRETE;
			break;
		case RED_CONCRETE_POWDER:
			concrete = Material.RED_CONCRETE;
			break;
		case YELLOW_CONCRETE_POWDER:
			concrete = Material.YELLOW_CONCRETE;
			break;
		case ORANGE_CONCRETE_POWDER:
			concrete = Material.ORANGE_CONCRETE;
			break;
		case PINK_CONCRETE_POWDER:
			concrete = Material.PINK_CONCRETE;
			break;
		case PURPLE_CONCRETE_POWDER:
			concrete = Material.PURPLE_CONCRETE;
			break;
		case MAGENTA_CONCRETE_POWDER:
			concrete = Material.MAGENTA_CONCRETE;
			break;
		default:
			concrete = powderType;
			break;
		}

		return concrete;
	}

	protected class FarmUpdater implements Runnable {

		private final UUID playerUUID;
		private final SchedulerTask cancellableTask;

		public FarmUpdater(@NotNull UUID playerUUID) {
			this.playerUUID = playerUUID;
			this.cancellableTask = instance.getScheduler().asyncRepeating(this, 0, RandomUtils.generateRandomInt(3, 5),
					TimeUnit.MINUTES);
		}

		@Override
		public void run() {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.isEmpty() || !onlineUser.get().isOnline()) {
				cancelTask();
				return;
			}

			trackNetherFarms(onlineUser.get());
		}

		public void cancelTask() {
			if (this.cancellableTask != null)
				this.cancellableTask.cancel();
		}
	}
}
