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
import org.bukkit.World;
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
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.rain.LavaRainTask;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.RandomUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;

public class FarmingHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final Cache<UUID, Set<Block>> farmCache = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES)
			.build();

	private final Map<Location, Integer> blockCache, moistureCache, revertCache;

	private final static BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	private final static Set<Material> sugarCaneGrowBlocks = Set.of(Material.GRASS_BLOCK, Material.DIRT,
			Material.COARSE_DIRT, Material.MYCELIUM, Material.SAND, Material.RED_SAND, Material.SUSPICIOUS_SAND,
			Material.MUD, Material.MOSS_BLOCK, Material.PODZOL, Material.ROOTED_DIRT);

	public FarmingHandler(HellblockPlugin plugin) {
		instance = plugin;
		blockCache = new LinkedHashMap<>();
		moistureCache = new LinkedHashMap<>();
		revertCache = new LinkedHashMap<>();
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		blockCache.clear();
		moistureCache.clear();
		revertCache.clear();
	}

	public void updateCrops(@NotNull World world, @NotNull Player player) {
		Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
				.filter(task -> world.getName().equalsIgnoreCase(task.getWorld().worldName())).findAny();
		getFarmlandOnHellblock(world, Context.player(player)).thenAccept((farmBlocks) -> {
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
					Location cache = updatingBlock.getLocation();
					if (checkForLavaAroundFarm(cache.getBlock())
							|| (lavaRain.isPresent() && lavaRain.get().isLavaRaining()
									&& updatingBlock.getWorld().getHighestBlockAt(cache).isEmpty())) {
						instance.getScheduler().sync().runLater(() -> {
							if (!this.blockCache.containsKey(cache)) {
								this.blockCache.put(cache, RandomUtils.generateRandomInt(15, 25));
							}
							do {
								if (this.blockCache.containsKey(cache) && this.blockCache.get(cache) != null
										&& this.blockCache.get(cache).intValue() == RandomUtils.generateRandomInt(15,
												25)) {
									if (world.getBlockAt(cache.getBlockX(), cache.getBlockY(), cache.getBlockZ())
											.getType() == Material.FARMLAND
											&& ((Farmland) world
													.getBlockAt(cache.getBlockX(), cache.getBlockY(), cache.getBlockZ())
													.getBlockData())
													.getMoisture() != ((Farmland) world.getBlockAt(cache.getBlockX(),
															cache.getBlockY(), cache.getBlockZ()).getBlockData())
															.getMaximumMoisture()
											&& !this.revertCache.containsKey(cache)
											&& !this.moistureCache.containsKey(cache)) {
										farm.setMoisture(farm.getMaximumMoisture());
										updatingBlock.setBlockData(farm);
										this.moistureCache.put(cache, farm.getMoisture());
										Optional<UserData> optionalData = instance.getStorageManager()
												.getOnlineUser(player.getUniqueId());
										if (optionalData.isEmpty())
											return;
										UserData user = optionalData.get();
										if (user.getPlayer() != null && user.isOnline()
												&& user.getHellblockData().hasHellblock()) {
											if (!user.getChallengeData().isChallengeActive(
													instance.getChallengeManager().getByActionType(ActionType.FARM))
													&& !user.getChallengeData().isChallengeCompleted(instance
															.getChallengeManager().getByActionType(ActionType.FARM))) {
												user.getChallengeData().beginChallengeProgression(user.getPlayer(),
														instance.getChallengeManager()
																.getByActionType(ActionType.FARM));
											} else {
												user.getChallengeData().updateChallengeProgression(user.getPlayer(),
														instance.getChallengeManager().getByActionType(ActionType.FARM),
														1);
												if (user.getChallengeData().isChallengeCompleted(instance
														.getChallengeManager().getByActionType(ActionType.FARM))) {
													user.getChallengeData().completeChallenge(user.getPlayer(), instance
															.getChallengeManager().getByActionType(ActionType.FARM));
												}
											}
										}
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
										if (world.getBlockAt(cache.getBlockX(), cache.getBlockY(), cache.getBlockZ())
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

	public int getMaxGrowthStage(@Nullable Block block) {
		if (block == null || block.isEmpty())
			return 0;
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return 0;

		if (Tag.CROPS.isTagged(block.getType())) {
			if (block.getBlockData() instanceof Ageable cropData) {
				return cropData.getMaximumAge();
			}
		}
		return 0;
	}

	public int getCurrentGrowthStage(@Nullable Block block) {
		if (block == null || block.isEmpty())
			return 0;
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return 0;

		if (Tag.CROPS.isTagged(block.getType())) {
			if (block.getBlockData() instanceof Ageable cropData) {
				return cropData.getAge();
			}
		}
		return 0;
	}

	public void updateGrowthStage(@Nullable Block block) {
		if (block == null || block.isEmpty())
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;
		if (Tag.CROPS.isTagged(block.getType())) {
			if (block.getBlockData() instanceof Ageable cropData) {
				int currentStage = getCurrentGrowthStage(block);
				int maxStage = getMaxGrowthStage(block);
				cropData.setAge(currentStage <= maxStage ? currentStage + 1 : maxStage);
				block.setBlockData(cropData);
			}
		}
	}

	private boolean hasUngrownCrops(@Nullable Set<Block> blocks) {
		if (blocks == null || blocks.isEmpty())
			return false;

		boolean ungrownStage = false;
		for (Block farm : blocks) {
			if (farm == null || farm.getType() != Material.FARMLAND)
				continue;
			if (!instance.getHellblockHandler().isInCorrectWorld(farm.getWorld()))
				continue;

			if (Tag.CROPS.isTagged(farm.getRelative(BlockFace.UP).getType())) {
				Block crop = farm.getRelative(BlockFace.UP);
				if (crop.getBlockData() instanceof Ageable cropData) {
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
			if (!instance.getHellblockHandler().isInCorrectWorld(farm.getWorld()))
				continue;

			if (farm.getBlockData() instanceof Farmland farmland) {
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
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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

	private CompletableFuture<Set<Block>> getFarmlandOnHellblock(@NotNull World world,
			@NotNull Context<Player> context) {
		CompletableFuture<Set<Block>> farm = new CompletableFuture<>();
		if (this.farmCache.getIfPresent(context.holder().getUniqueId()) != null)
			farm.complete(this.farmCache.getIfPresent(context.holder().getUniqueId()));
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(context.holder()).thenAccept(ownerUUID -> {
			if (ownerUUID == null)
				return;
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
						if (bounds == null)
							return;

						Set<Block> farmBlocks = new HashSet<>();
						int minX = (int) Math.min(bounds.getMinX(), bounds.getMaxX());
						int minY = (int) Math.min(bounds.getMinY(), bounds.getMaxY());
						int minZ = (int) Math.min(bounds.getMinZ(), bounds.getMaxZ());
						int maxX = (int) Math.max(bounds.getMinX(), bounds.getMaxX());
						int maxY = (int) Math.max(bounds.getMinY(), bounds.getMaxY());
						int maxZ = (int) Math.max(bounds.getMinZ(), bounds.getMaxZ());
						for (int x = minX; x <= maxX; x++) {
							for (int y = minY; y <= maxY; y++) {
								for (int z = minZ; z <= maxZ; z++) {
									Block farmBlock = world.getBlockAt(x, y, z);
									if (!(farmBlock.getBlockData() instanceof Farmland))
										continue;
									farmBlocks.add(farmBlock);
								}
							}
						}

						this.farmCache.put(context.holder().getUniqueId(), farmBlocks);
						farm.complete(farmBlocks);
					});
		});
		return farm;
	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			if (Tag.CROPS.isTagged(block.getRelative(BlockFace.UP).getType())
					|| block.getRelative(BlockFace.UP).isEmpty()) {
				Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
						.filter(task -> block.getWorld().getName().equalsIgnoreCase(task.getWorld().worldName()))
						.findAny();
				if (checkForLavaAroundFarm(block) || (lavaRain.isPresent() && lavaRain.get().isLavaRaining()
						&& block.getWorld().getHighestBlockAt(block.getLocation()).isEmpty())) {
					event.setCancelled(true);
					Collection<Entity> playersNearby = block.getWorld()
							.getNearbyEntities(block.getLocation(), 25, 25, 25).stream()
							.filter(e -> e.getType() == EntityType.PLAYER).toList();
					Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
							playersNearby);
					if (player != null) {
						updateCrops(block.getWorld(), player);
					}
				} else {
					if (this.blockCache.containsKey(block.getLocation())) {
						this.blockCache.remove(block.getLocation());
					}
					if (this.revertCache.containsKey(block.getLocation())) {
						this.revertCache.remove(block.getLocation());
					}
					if (this.moistureCache.containsKey(block.getLocation())) {
						this.moistureCache.remove(block.getLocation());
					}
				}
			}
		}
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		final List<Block> blocks = event.blockList();
		for (Block block : blocks) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
				continue;

			if (block.getBlockData() instanceof Farmland) {
				Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
						.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
				Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
						playersNearby);
				if (player != null) {
					updateCrops(block.getWorld(), player);
				}
				if (this.blockCache.containsKey(block.getLocation())) {
					this.blockCache.remove(block.getLocation());
				}
				if (this.revertCache.containsKey(block.getLocation())) {
					this.revertCache.remove(block.getLocation());
				}
				if (this.moistureCache.containsKey(block.getLocation())) {
					this.moistureCache.remove(block.getLocation());
				}
			}
		}
	}

	@EventHandler
	public void onLavaPlace(PlayerBucketEmptyEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		Block block = event.getBlock();
		if (event.getBucket() == Material.LAVA_BUCKET) {
			updateCrops(block.getWorld(), player);
		}
	}

	@EventHandler
	public void onLavaPickup(PlayerBucketFillEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		Block block = event.getBlock();
		if (event.getItemStack() != null && event.getItemStack().getType() == Material.LAVA_BUCKET) {
			updateCrops(block.getWorld(), player);
		}
	}

	@EventHandler
	public void onPickUpLavaWithDispense(BlockDispenseEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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
							updateCrops(block.getWorld(), player);
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onPistonExtendIntoLava(BlockPistonExtendEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25).stream()
				.filter(e -> e.getType() == EntityType.PLAYER).toList();
		Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(), playersNearby);
		if (player != null) {
			for (Block lava : event.getBlocks()) {
				if (lava.getType() == Material.LAVA) {
					updateCrops(block.getWorld(), player);
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			final Player player = event.getPlayer();
			updateCrops(block.getWorld(), player);
			if (block.getBlockData() instanceof Farmland) {
				if (this.blockCache.containsKey(block.getLocation())) {
					this.blockCache.remove(block.getLocation());
				}
				if (this.revertCache.containsKey(block.getLocation())) {
					this.revertCache.remove(block.getLocation());
				}
				if (this.moistureCache.containsKey(block.getLocation())) {
					this.moistureCache.remove(block.getLocation());
				}
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		final Player player = event.getPlayer();
		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			updateCrops(block.getWorld(), player);
		}

		if (event.getBlockReplacedState().getType() == Material.LAVA) {
			updateCrops(block.getWorld(), player);
		}
	}

	@EventHandler
	public void onMoistureChange(MoistureChangeEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
					.filter(task -> block.getWorld().getName().equalsIgnoreCase(task.getWorld().worldName())).findAny();
			if (checkForLavaAroundFarm(block) || (lavaRain.isPresent() && lavaRain.get().isLavaRaining()
					&& block.getWorld().getHighestBlockAt(block.getLocation()).isEmpty())) {
				event.setCancelled(true);
				Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
						.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
				Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
						playersNearby);
				if (player != null) {
					updateCrops(block.getWorld(), player);
				}
			} else {
				if (this.moistureCache.containsKey(block.getLocation())) {
					this.moistureCache.remove(block.getLocation());
				}
			}
		}
	}

	@EventHandler
	public void onBoneMeal(BlockFertilizeEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		final List<BlockState> blocks = event.getBlocks();
		for (BlockState block : blocks) {
			if (block.getBlockData() instanceof Ageable) {
				updateCrops(block.getWorld(), player);
			}
		}
	}

	@EventHandler
	public void onHarvest(PlayerHarvestBlockEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		final Block block = event.getHarvestedBlock();
		if (block.getBlockData() instanceof Ageable) {
			updateCrops(block.getWorld(), player);
		}
	}

	@EventHandler
	public void onGrow(BlockGrowEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		if (block.getBlockData() instanceof Ageable) {
			Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
					.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
			Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					playersNearby);
			if (player != null) {
				updateCrops(block.getWorld(), player);
			}
		}
	}

	@EventHandler
	public void onFarmland(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		if (!(event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
				|| event.getAction() == org.bukkit.event.block.Action.PHYSICAL)) {
			return;
		}

		if (event.getItem() != null && (!(Tag.ITEMS_HOES.isTagged(event.getItem().getType())
				|| Tag.CROPS.isTagged(event.getItem().getType())))) {
			return;
		}

		final Block block = event.getClickedBlock();
		if (block != null) {
			if (Tag.DIRT.isTagged(block.getType()) || block.getBlockData() instanceof Farmland) {
				updateCrops(block.getWorld(), player);
				if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL
						&& (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable)) {
					if (this.blockCache.containsKey(block.getLocation())) {
						this.blockCache.remove(block.getLocation());
					}
					if (this.revertCache.containsKey(block.getLocation())) {
						this.revertCache.remove(block.getLocation());
					}
					if (this.moistureCache.containsKey(block.getLocation())) {
						this.moistureCache.remove(block.getLocation());
					}
				}
			}
		}
	}

	@EventHandler
	public void onFarmlandEntity(EntityInteractEvent event) {
		final Entity entity = event.getEntity();
		if (!instance.getHellblockHandler().isInCorrectWorld(entity.getWorld()))
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
					updateCrops(block.getWorld(), player);
				}
				if (block.getBlockData() instanceof Farmland) {
					if (this.blockCache.containsKey(block.getLocation())) {
						this.blockCache.remove(block.getLocation());
					}
					if (this.revertCache.containsKey(block.getLocation())) {
						this.revertCache.remove(block.getLocation());
					}
					if (this.moistureCache.containsKey(block.getLocation())) {
						this.moistureCache.remove(block.getLocation());
					}
				}
			}
		}
	}

	private boolean checkForLavaAroundSugarCane(@Nullable Block block) {
		if (block == null || block.isEmpty() || !(sugarCaneGrowBlocks.contains(block.getType())))
			return false;
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return false;

		boolean lavaFound = false;
		for (BlockFace face : FACES) {
			if (block.getRelative(face).getType() == Material.AIR)
				continue;
			if (block.getRelative(face).getType() == Material.LAVA) {
				lavaFound = true;
				break;
			}
		}
		return lavaFound;
	}

	@EventHandler
	public void onForceSugarCanePlacement(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
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
							VersionHelper.getNMSManager().swingHand(player,
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
							VersionHelper.getNMSManager().swingHand(player,
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
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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
	public void onExplodeSugarCane(BlockExplodeEvent event) {
		final List<Block> blocks = event.blockList();

		for (Block block : blocks) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
				continue;
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
								block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation().add(0.5D, 0,
										0.5D),
								block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getBlockData());
						falling.setHurtEntities(true);
						falling.setDropItem(true);
						block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
					}
					if (Tag.WOOL_CARPETS
							.isTagged(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType())) {
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
				} else {
					event.setCancelled(true);
					if (block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType().hasGravity()) {
						FallingBlock falling = block.getWorld().spawnFallingBlock(
								block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation().add(0.5D, 0,
										0.5D),
								block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getBlockData());
						falling.setHurtEntities(true);
						falling.setDropItem(true);
						block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
					}
					if (Tag.WOOL_CARPETS
							.isTagged(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType())) {
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

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlaceSugarCane(BlockPlaceEvent event) {
		final Block block = event.getBlockPlaced();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
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
	public void onForceCocoaBeanPlacement(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
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
					VersionHelper.getNMSManager().swingHand(player,
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

	public class ConcreteConverter {

		@EventHandler
		public void onFlowConcretePowder(BlockFromToEvent event) {
			final Block block = event.getBlock();
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
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
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
				return;

			if (Tag.CONCRETE_POWDER.isTagged(block.getType())) {
				for (BlockFace face : FACES) {
					if (block.getRelative(face).getType() == Material.LAVA) {
						block.setType(convertToConcrete(block.getType()));
						for (Entity entity : event.getPlayer().getWorld()
								.getNearbyEntities(event.getPlayer().getLocation(),
										instance.getConfigManager().searchRadius(),
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
		public void onFallConcretePowder(EntityDropItemEvent event) {
			if (event.getEntity() instanceof FallingBlock fallingBlock) {
				if (!instance.getHellblockHandler().isInCorrectWorld(fallingBlock.getWorld()))
					return;
				if (Tag.CONCRETE_POWDER.isTagged(fallingBlock.getBlockData().getMaterial())) {
					Material powder = fallingBlock.getBlockData().getMaterial();
					if (fallingBlock.getLocation().getBlock().getRelative(BlockFace.UP).getType() == Material.LAVA) {
						event.setCancelled(true);
						instance.getScheduler().sync().runLater(() -> fallingBlock.getLocation().getBlock()
								.getRelative(BlockFace.UP).setType(convertToConcrete(powder)), 5,
								fallingBlock.getLocation());
						for (Entity entity : fallingBlock.getWorld().getNearbyEntities(fallingBlock.getLocation(),
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
						fallingBlock.setDropItem(false);
					} else {
						for (BlockFace face : FACES) {
							if (fallingBlock.getLocation().getBlock().getRelative(face).getType() == Material.LAVA) {
								event.setCancelled(true);
								instance.getScheduler().sync().runLater(
										() -> fallingBlock.getLocation().getBlock().setType(convertToConcrete(powder)),
										5, fallingBlock.getLocation());
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
	}
}