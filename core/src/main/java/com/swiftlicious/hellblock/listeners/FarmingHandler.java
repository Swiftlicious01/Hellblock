package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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
import com.swiftlicious.hellblock.challenges.requirement.FarmRequirement;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.NetherGeneratorHandler.LocationKey;
import com.swiftlicious.hellblock.listeners.rain.LavaRainTask;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeData;
import com.swiftlicious.hellblock.upgrades.UpgradeTier;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.sound.Sound;

public class FarmingHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final Cache<UUID, Set<Block>> farmCache = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES)
			.build();

	private final Map<Location, Integer> blockCache = new LinkedHashMap<>();
	private final Map<Location, Integer> moistureCache = new LinkedHashMap<>();
	private final Map<Location, Integer> revertCache = new LinkedHashMap<>();

	private static final BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	private static final Set<Material> GROWING_SUGAR_CANE_BLOCKS = EnumSet.of(Material.GRASS_BLOCK, Material.DIRT,
			Material.COARSE_DIRT, Material.MYCELIUM, Material.SAND, Material.RED_SAND, Material.SUSPICIOUS_SAND,
			Material.MUD, Material.MOSS_BLOCK, Material.PODZOL, Material.ROOTED_DIRT);

	private static final int PLAYER_SEARCH_RADIUS = 25;
	private static final int FARM_BLOCK_CHECK_RADIUS = 4; // used in lava detection

	private final Map<UUID, Double> cropGrowthBonusCache = new ConcurrentHashMap<>();

	public FarmingHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		blockCache.clear();
		moistureCache.clear();
		revertCache.clear();
		cropGrowthBonusCache.clear();
	}

	public void updateCrops(@NotNull World world, @NotNull Player player) {
		getFarmBlocksOnHellblock(world, Context.player(player)).thenAccept(farmBlocks -> {
			if (farmBlocks == null || farmBlocks.isEmpty()) {
				return;
			}

			for (Block block : farmBlocks) {
				final Location cache = block.getLocation();

				// --- Case 1: Farmland hydration/dehydration (also handles normal crop growth)
				// ---
				if (block.getBlockData() instanceof Farmland farm) {
					handleFarmland(block, farm, cache, world);
					continue;
				}

				// --- Case 2: Nether Wart growth acceleration ---
				if (block.getType() == Material.NETHER_WART) {
					handleNetherWart(block, cache, world);
					continue;
				}

				// --- Case 3: Sugar Cane growth ---
				if (block.getType() == Material.SUGAR_CANE) {
					handleSugarCane(block, cache, world);
				}

				// --- Case 4: Cocoa bean growth ---
				if (block.getType() == Material.COCOA) {
					handleCocoaBeans(world, player);
				}

				// --- Case 5: Mushroom spread growth ---
				if (block.getType() == Material.RED_MUSHROOM || block.getType() == Material.BROWN_MUSHROOM) {
					handleMushrooms(world, player);
				}

				// --- Case 6: Cactus growth ---
				if (block.getType() == Material.CACTUS) {
					handleCactus(block);
				}

				// --- Case 7: Sweet berry bush growth ---
				if (block.getType() == Material.SWEET_BERRY_BUSH) {
					handleSweetBerryBush(block);
				}

				// --- Case 8: Bamboo growth ---
				if (block.getType() == Material.BAMBOO) {
					handleBamboo(block);
				}

				// -- Case 9: Glowstone tree extra growth during lava rain ---
				instance.getGlowstoneTreeHandler().updateGlowstoneTrees(world, player);
			}
		});
	}

	public double getCachedCropGrowthBonus(@NotNull HellblockData data) {
		UUID ownerUUID = data.getOwnerUUID();
		return cropGrowthBonusCache.computeIfAbsent(ownerUUID, id -> calculateCropGrowthBonus(data));
	}

	public void updateCropGrowthBonusCache(@NotNull HellblockData data) {
		cropGrowthBonusCache.put(data.getOwnerUUID(), calculateCropGrowthBonus(data));
	}

	public void invalidateCropGrowthBonusCache(@NotNull UUID ownerUUID) {
		cropGrowthBonusCache.remove(ownerUUID);
	}

	private double calculateCropGrowthBonus(@NotNull HellblockData data) {
		int level = data.getUpgradeLevel(IslandUpgradeType.CROP_GROWTH);
		double total = 0.0;

		for (int i = 0; i <= level; i++) {
			UpgradeTier tier = instance.getUpgradeManager().getTier(i);
			if (tier == null)
				continue;

			UpgradeData upgrade = tier.getUpgrade(IslandUpgradeType.CROP_GROWTH);
			if (upgrade != null && upgrade.getValue() != null) {
				total += upgrade.getValue().doubleValue();
			}
		}
		return total;
	}

	public void withCropGrowthBonusIfValid(@NotNull Block block, @NotNull Consumer<Double> bonusConsumer) {
		instance.getCoopManager().getHellblockOwnerOfBlock(block.getLocation().getBlock())
				.thenAcceptAsync(ownerUUID -> {
					if (ownerUUID == null)
						return;

					instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
							.thenAccept(userDataOpt -> {
								if (userDataOpt.isEmpty())
									return;

								HellblockData data = userDataOpt.get().getHellblockData();
								BoundingBox box = data.getBoundingBox();
								if (box == null || !box.contains(block.getLocation().toVector()))
									return;

								double bonus = getCachedCropGrowthBonus(data);
								bonusConsumer.accept(bonus);
							});
				});
	}

	private void handleFarmland(Block block, Farmland farm, Location cache, World world) {
		// Case 1: Moisturize farmland if near lava or lava rain
		if (checkForLavaAroundFarm(block) || lavaRainCheck(block)) {
			final int randomValue = RandomUtils.generateRandomInt(15, 25);
			blockCache.putIfAbsent(cache, randomValue);

			instance.getScheduler().sync().runLater(() -> {
				final Integer stored = blockCache.get(cache);
				if (Objects.equals(stored, randomValue)) {
					final Block target = world.getBlockAt(cache);
					if (target.getType() == Material.FARMLAND) {
						final Farmland targetFarm = (Farmland) target.getBlockData();
						if (targetFarm.getMoisture() != targetFarm.getMaximumMoisture()
								&& !revertCache.containsKey(cache) && !moistureCache.containsKey(cache)) {

							farm.setMoisture(farm.getMaximumMoisture());
							block.setBlockData(farm);
							moistureCache.put(cache, farm.getMoisture());
						}
					}

					// clear caches once moisture maxed
					if (Objects.equals(moistureCache.get(cache), farm.getMaximumMoisture())) {
						blockCache.remove(cache);
						moistureCache.remove(cache);
					}
				}

				// Extra: crops above farmland grow faster during lava rain
				if (lavaRainCheck(block)) {
					Block above = block.getRelative(BlockFace.UP);
					if (above.getBlockData() instanceof Ageable crop) {
						if (Tag.CROPS.isTagged(above.getType()) || above.getType() == Material.MELON_STEM
								|| above.getType() == Material.ATTACHED_MELON_STEM
								|| above.getType() == Material.PUMPKIN_STEM
								|| above.getType() == Material.ATTACHED_PUMPKIN_STEM) {

							final int baseChance = 40;

							withCropGrowthBonusIfValid(above, bonus -> {
								int finalChance = (int) Math.min(100, baseChance + bonus);
								if (crop.getAge() < crop.getMaximumAge() && rollChance(finalChance)) {
									crop.setAge(crop.getAge() + 1);
									above.setBlockData(crop);
								}
							});
						}
					}
				}

			}, RandomUtils.generateRandomInt(15, 30) * 20L, cache);

		} else {
			// Case 2: Normal dehydration and possible farmland revert
			final int randomValue = RandomUtils.generateRandomInt(15, 20);

			instance.getScheduler().sync().runLater(() -> {
				farm.setMoisture(Math.max(farm.getMoisture() - 1, 0));
				block.setBlockData(farm);

				if (farm.getMoisture() == 0) {
					revertCache.putIfAbsent(cache, randomValue);
					if (Objects.equals(revertCache.get(cache), randomValue)) {
						final Block checkBlock = world.getBlockAt(cache);
						if (checkBlock.getType() == Material.FARMLAND && !blockCache.containsKey(cache)
								&& moistureCache.containsKey(cache)) {

							// break crops or stems above when farmland reverts
							Material aboveType = block.getRelative(BlockFace.UP).getType();
							if (Tag.CROPS.isTagged(aboveType) || aboveType == Material.MELON_STEM
									|| aboveType == Material.ATTACHED_MELON_STEM || aboveType == Material.PUMPKIN_STEM
									|| aboveType == Material.ATTACHED_PUMPKIN_STEM) {
								block.getRelative(BlockFace.UP).breakNaturally();
								block.getRelative(BlockFace.UP).getState().update();
							}
							block.setType(Material.DIRT);
						}
						revertCache.remove(cache);
					}
				}
			}, RandomUtils.generateRandomInt(10, 15) * 20L, cache);
		}
	}

	private void handleNetherWart(Block block, Location cache, World world) {
		if (!(block.getBlockData() instanceof Ageable)) {
			return;
		}

		if (checkForLavaAroundFarm(block.getRelative(BlockFace.DOWN)) || lavaRainCheck(block)) {
			final int randomDelay = RandomUtils.generateRandomInt(15, 30);

			instance.getScheduler().sync().runLater(() -> {
				final Block current = world.getBlockAt(cache);
				if (current.getType() != Material.NETHER_WART
						|| !(current.getBlockData() instanceof Ageable wartData)) {
					return;
				}

				final int currentAge = wartData.getAge();
				final int maxAge = wartData.getMaximumAge();
				if (currentAge >= maxAge)
					return;

				final int baseChance = lavaRainCheck(current) ? 50 : 25;

				// apply bonus if valid island crop
				withCropGrowthBonusIfValid(current, bonus -> {
					int finalChance = (int) Math.min(100, baseChance + bonus);
					if (rollChance(finalChance)) {
						wartData.setAge(currentAge + 1);
						current.setBlockData(wartData);
					}
				});
			}, randomDelay * 20L, cache);
		}
	}

	private void handleCocoaBeans(@NotNull World world, @NotNull Player player) {
		instance.getProtectionManager().getHellblockBlocks(world, player.getUniqueId()).thenAccept(blocks -> {
			if (blocks == null || blocks.isEmpty()) {
				return;
			}

			for (Block block : blocks) {
				if (block.getType() != Material.COCOA || !(block.getBlockData() instanceof Ageable ageable)) {
					continue;
				}

				Block attached = block.getRelative(((Directional) block.getBlockData()).getFacing().getOppositeFace());

				// Only allow growth if attached to valid stem and lava rain is active
				if ((attached.getType() == Material.CRIMSON_STEM || attached.getType() == Material.WARPED_STEM)
						&& lavaRainCheck(block)) {

					final int baseChance = 40;

					withCropGrowthBonusIfValid(block, bonus -> {
						int finalChance = (int) Math.min(100, baseChance + bonus);
						if (ageable.getAge() < ageable.getMaximumAge() && rollChance(finalChance)) {
							ageable.setAge(ageable.getAge() + 1);
							block.setBlockData(ageable);
						}
					});
				}
			}
		});
	}

	private void handleSugarCane(Block block, Location cache, World world) {
		// Only handle base cane blocks (already filtered in getFarmBlocksOnHellblock)
		Block above = block.getRelative(BlockFace.UP);
		Block above2 = above.getRelative(BlockFace.UP);

		if (above2.getType() == Material.SUGAR_CANE) {
			return; // Already at max height (3)
		}

		if (checkForLavaAroundFarm(block.getRelative(BlockFace.DOWN)) || lavaRainCheck(block)) {
			final int randomDelay = RandomUtils.generateRandomInt(20, 35);

			instance.getScheduler().sync().runLater(() -> {
				Block current = world.getBlockAt(cache);
				if (current.getType() != Material.SUGAR_CANE) {
					return;
				}

				Block top = getSugarCaneTop(current);
				if (getSugarCaneHeight(current) >= 3) {
					return;
				}

				final int baseChance = lavaRainCheck(current) ? 40 : 20;

				withCropGrowthBonusIfValid(current, bonus -> {
					int finalChance = (int) Math.min(100, baseChance + bonus);
					if (rollChance(finalChance)) {
						Block growTarget = top.getRelative(BlockFace.UP);
						if (growTarget.getType() == Material.AIR) {
							growTarget.setType(Material.SUGAR_CANE);
						}
					}
				});
			}, randomDelay * 20L, cache);
		}
	}

	private Block getSugarCaneTop(Block base) {
		Block current = base;
		while (current.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
			current = current.getRelative(BlockFace.UP);
		}
		return current;
	}

	private int getSugarCaneHeight(Block base) {
		int height = 1;
		Block current = base;
		while (current.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE) {
			current = current.getRelative(BlockFace.UP);
			height++;
		}
		return height;
	}

	private void handleMushrooms(@NotNull World world, @NotNull Player player) {
		instance.getProtectionManager().getHellblockBlocks(world, player.getUniqueId()).thenAccept(blocks -> {
			if (blocks == null || blocks.isEmpty()) {
				return;
			}

			for (Block block : blocks) {
				if (block.getType() != Material.RED_MUSHROOM && block.getType() != Material.BROWN_MUSHROOM) {
					continue;
				}

				boolean nearLava = checkForLavaAroundFarm(block) || lavaRainCheck(block);
				if (!nearLava) {
					continue;
				}

				final int baseChance = lavaRainCheck(block) ? 40 : 20;

				withCropGrowthBonusIfValid(block, bonus -> {
					int finalChance = (int) Math.min(100, baseChance + bonus);

					instance.getScheduler().sync().runLater(() -> {
						if (rollChance(finalChance)) {
							trySpreadMushroom(block);
						}
					}, RandomUtils.generateRandomInt(20, 40) * 20L, block.getLocation());
				});
			}
		});
	}

	private final Set<Location> lavaGrownMushrooms = ConcurrentHashMap.newKeySet();

	/**
	 * Tries to spread a mushroom into a nearby valid block.
	 */
	private void trySpreadMushroom(Block source) {
		List<BlockFace> faces = Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
				BlockFace.UP, BlockFace.DOWN);
		Collections.shuffle(faces);

		for (BlockFace face : faces) {
			Block target = source.getRelative(face);

			if (target.getType() == Material.AIR) {
				Block below = target.getRelative(BlockFace.DOWN);
				if (isValidMushroomSoil(below.getType())) {
					target.setType(source.getType());
					lavaGrownMushrooms.add(target.getLocation()); // mark as lava-grown
					break;
				}
			}
		}
	}

	public boolean isLavaGrownMushroom(Block block) {
		return lavaGrownMushrooms.contains(block.getLocation());
	}

	/**
	 * Returns whether a block is valid soil for mushrooms.
	 */
	private boolean isValidMushroomSoil(Material type) {
		return type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.PODZOL
				|| type == Material.MYCELIUM || type == Material.CRIMSON_NYLIUM || type == Material.WARPED_NYLIUM;
	}

	private void handleCactus(Block base) {
		if (getCactusHeight(base) >= 3) {
			return;
		}

		final int baseChance = lavaRainCheck(base) ? 50 : 15;

		withCropGrowthBonusIfValid(base, bonus -> {
			int finalChance = (int) Math.min(100, baseChance + bonus);
			if (rollChance(finalChance)) {
				Block top = base;
				while (top.getRelative(BlockFace.UP).getType() == Material.CACTUS) {
					top = top.getRelative(BlockFace.UP);
				}

				Block growTarget = top.getRelative(BlockFace.UP);
				if (growTarget.getType() == Material.AIR) {
					growTarget.setType(Material.CACTUS);
				}
			}
		});
	}

	private int getCactusHeight(Block base) {
		int height = 1;
		Block current = base;
		while (current.getRelative(BlockFace.UP).getType() == Material.CACTUS) {
			current = current.getRelative(BlockFace.UP);
			height++;
		}
		return height;
	}

	public boolean checkForLavaAroundCactus(Block base) {
		Block support = base.getRelative(BlockFace.DOWN);
		for (BlockFace bf : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST }) {
			if (support.getRelative(bf).getType() == Material.LAVA) {
				return true;
			}
		}
		return false;
	}

	public void handleSweetBerryBush(Block block) {
		if (!lavaRainCheck(block)) {
			return; // Lava rain required for growth
		}

		Block above = block.getRelative(BlockFace.UP);
		if (!(above.getBlockData() instanceof Ageable crop)) {
			return;
		}

		if (above.getType() != Material.SWEET_BERRY_BUSH) {
			return;
		}

		final int baseChance = 40;

		withCropGrowthBonusIfValid(above, bonus -> {
			int finalChance = (int) Math.min(100, baseChance + bonus);
			if (crop.getAge() < crop.getMaximumAge() && rollChance(finalChance)) {
				crop.setAge(crop.getAge() + 1);
				above.setBlockData(crop);
			}
		});
	}

	public boolean checkForLavaAroundBerryBush(Block bush) {
		Block below = bush.getRelative(BlockFace.DOWN);
		return checkForLavaAroundFarm(below); // reuse farmland-style check
	}

	private void handleBamboo(Block base) {
		if (getBambooHeight(base) >= 3) {
			return;
		}

		final int baseChance = lavaRainCheck(base) ? 50 : 15;

		withCropGrowthBonusIfValid(base, bonus -> {
			int finalChance = (int) Math.min(100, baseChance + bonus);
			if (rollChance(finalChance)) {
				Block top = base;
				while (top.getRelative(BlockFace.UP).getType() == Material.BAMBOO) {
					top = top.getRelative(BlockFace.UP);
				}

				Block growTarget = top.getRelative(BlockFace.UP);
				if (growTarget.getType() == Material.AIR) {
					growTarget.setType(Material.BAMBOO);
				}
			}
		});
	}

	private int getBambooHeight(Block base) {
		int height = 1;
		Block current = base;
		while (current.getRelative(BlockFace.UP).getType() == Material.BAMBOO) {
			current = current.getRelative(BlockFace.UP);
			height++;
		}
		return height;
	}

	public boolean checkForLavaAroundBamboo(Block base) {
		Block support = base.getRelative(BlockFace.DOWN);
		for (BlockFace bf : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST }) {
			if (support.getRelative(bf).getType() == Material.LAVA) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Simple percentage chance helper (0-100).
	 */
	public boolean rollChance(int percent) {
		return RandomUtils.generateRandomInt(1, 100) <= percent;
	}

	public int getMaxMoisture(@Nullable Block block) {
		if (block == null || block.isEmpty()) {
			return 0;
		}
		if (!isInCorrectWorld(block.getWorld())) {
			return 0;
		}

		if (Tag.CROPS.isTagged(block.getType())
				&& block.getRelative(BlockFace.DOWN).getBlockData() instanceof Farmland farmData) {
			return farmData.getMaximumMoisture();
		}
		return 0;
	}

	public int getCurrentMoisture(@Nullable Block block) {
		if (block == null || block.isEmpty()) {
			return 0;
		}
		if (!isInCorrectWorld(block.getWorld())) {
			return 0;
		}

		if (Tag.CROPS.isTagged(block.getType())
				&& block.getRelative(BlockFace.DOWN).getBlockData() instanceof Farmland farmData) {
			return farmData.getMoisture();
		}
		return 0;
	}

	public int getMaxGrowthStage(@Nullable Block block) {
		if (block == null || block.isEmpty()) {
			return 0;
		}
		if (!isInCorrectWorld(block.getWorld())) {
			return 0;
		}

		if (Tag.CROPS.isTagged(block.getType()) && block.getBlockData() instanceof Ageable cropData) {
			return cropData.getMaximumAge();
		}
		return 0;
	}

	public int getCurrentGrowthStage(@Nullable Block block) {
		if (block == null || block.isEmpty()) {
			return 0;
		}
		if (!isInCorrectWorld(block.getWorld())) {
			return 0;
		}

		if (Tag.CROPS.isTagged(block.getType()) && block.getBlockData() instanceof Ageable cropData) {
			return cropData.getAge();
		}
		return 0;
	}

	public void updateGrowthStage(@Nullable Block block) {
		if (block == null || block.isEmpty()) {
			return;
		}
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}
		if (!(Tag.CROPS.isTagged(block.getType()) && block.getBlockData() instanceof Ageable cropData)) {
			return;
		}
		final int currentStage = getCurrentGrowthStage(block);
		final int maxStage = getMaxGrowthStage(block);
		cropData.setAge(Math.min(currentStage + 1, maxStage));
		block.setBlockData(cropData);
	}

	public boolean checkForLavaAroundFarm(@Nullable Block block) {
		if (block == null || block.isEmpty() || block.getType() != Material.FARMLAND) {
			return false;
		}
		if (!isInCorrectWorld(block.getWorld())) {
			return false;
		}

		final int centerX = block.getLocation().getBlockX();
		final int centerY = block.getLocation().getBlockY();
		final int centerZ = block.getLocation().getBlockZ();
		for (int x = centerX - FARM_BLOCK_CHECK_RADIUS; x <= centerX + FARM_BLOCK_CHECK_RADIUS; x++) {
			for (int y = centerY - 1; y <= centerY; y++) {
				for (int z = centerZ - FARM_BLOCK_CHECK_RADIUS; z <= centerZ + FARM_BLOCK_CHECK_RADIUS; z++) {
					final Block b = block.getWorld().getBlockAt(x, y, z);
					if (b.getType() == Material.AIR) {
						continue;
					}
					if (b.getType() == Material.LAVA) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private CompletableFuture<Set<Block>> getFarmBlocksOnHellblock(@NotNull World world,
			@NotNull Context<Player> context) {
		final Set<Block> cached = farmCache.getIfPresent(context.holder().getUniqueId());
		if (cached != null) {
			return CompletableFuture.completedFuture(cached);
		}

		CompletableFuture<Set<Block>> farm = new CompletableFuture<>();

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(context.holder()).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				farm.complete(Collections.emptySet());
				return;
			}

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							farm.complete(Collections.emptySet());
							return;
						}

						final UserData offlineUser = result.get();
						final BoundingBox bounds = offlineUser.getHellblockData().getBoundingBox();
						if (bounds == null) {
							farm.complete(Collections.emptySet());
							return;
						}

						final Set<Block> farmBlocks = new HashSet<>();
						final int minX = (int) Math.min(bounds.getMinX(), bounds.getMaxX());
						final int minY = (int) Math.min(bounds.getMinY(), bounds.getMaxY());
						final int minZ = (int) Math.min(bounds.getMinZ(), bounds.getMaxZ());
						final int maxX = (int) Math.max(bounds.getMinX(), bounds.getMaxX());
						final int maxY = (int) Math.max(bounds.getMinY(), bounds.getMaxY());
						final int maxZ = (int) Math.max(bounds.getMinZ(), bounds.getMaxZ());

						for (int x = minX; x <= maxX; x++) {
							for (int y = minY; y <= maxY; y++) {
								for (int z = minZ; z <= maxZ; z++) {
									final Block block = world.getBlockAt(x, y, z);

									// Case 1: Farmland
									if (block.getBlockData() instanceof Farmland) {
										farmBlocks.add(block);
										continue;
									}

									// Case 2: Nether Wart
									if (block.getType() == Material.NETHER_WART) {
										farmBlocks.add(block);
										continue;
									}

									// Case 3: Sugar Cane (only add base block to avoid duplicates)
									if (block.getType() == Material.SUGAR_CANE
											&& block.getRelative(BlockFace.DOWN).getType() != Material.SUGAR_CANE) {
										farmBlocks.add(block);
									}
								}
							}
						}

						this.farmCache.put(context.holder().getUniqueId(), farmBlocks);
						farm.complete(farmBlocks);
					});
		});

		return farm;
	}

	// Tracks crops placed by players that have not yet grown naturally
	private final Set<LocationKey> playerPlacedCrops = ConcurrentHashMap.newKeySet();

	@EventHandler
	public void onFarmBlockPlace(BlockPlaceEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}

		final Material type = event.getBlock().getType();
		if (!FarmRequirement.SUPPORTED_CROPS.contains(type)) {
			return; // only track supported crops/cane
		}

		final NetherGeneratorHandler handler = instance.getNetherrackGeneratorHandler();
		final LocationKey locKey = handler.new LocationKey(event.getBlock().getLocation());
		playerPlacedCrops.add(locKey);
	}

	@EventHandler
	public void onCropGrow(BlockGrowEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}

		final Block block = event.getBlock();

		// Only process FARM action for supported crops
		if (FarmRequirement.SUPPORTED_CROPS.contains(block.getType())) {
			return;
		}

		final NetherGeneratorHandler handler = instance.getNetherrackGeneratorHandler();
		final LocationKey locKey = handler.new LocationKey(block.getLocation());

		// Once crop or cane grows naturally, it's no longer "player-placed"
		if (playerPlacedCrops.contains(locKey)) {
			playerPlacedCrops.remove(locKey);
		}
	}

	@EventHandler
	public void onFarmBlockBreak(BlockBreakEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}

		final Player player = event.getPlayer();
		final Block block = event.getBlock();

		// Only process FARM action for supported crops
		if (FarmRequirement.SUPPORTED_CROPS.contains(block.getType())) {
			return;
		}

		final NetherGeneratorHandler handler = instance.getNetherrackGeneratorHandler();
		final LocationKey locKey = handler.new LocationKey(block.getLocation());

		// Ignore if it hasn’t grown naturally yet
		if (playerPlacedCrops.contains(locKey)) {
			playerPlacedCrops.remove(locKey); // cleanup
			return;
		}

		// Handle FARM challenges
		// FarmRequirement checks full growth or cane height/lava
		instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(
				user -> instance.getChallengeManager().handleChallengeProgression(player, ActionType.FARM, block));
	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		if (block.getBlockData() instanceof Farmland && (Tag.CROPS.isTagged(block.getRelative(BlockFace.UP).getType())
				|| block.getRelative(BlockFace.UP).isEmpty())) {
			if (!lavaRainCheck(block)) {
				event.setCancelled(true);
				final Player player = closestPlayerFor(block);
				if (player != null) {
					updateCrops(block.getWorld(), player);
				}
			} else {
				clearCachesAt(block.getLocation());
			}
		}
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		final List<Block> blocks = event.blockList();
		for (Block block : blocks) {
			if (!isInCorrectWorld(block.getWorld())) {
				continue;
			}
			if (block.getBlockData() instanceof Farmland) {
				final Player player = closestPlayerFor(block);
				if (player != null) {
					updateCrops(block.getWorld(), player);
				}
				clearCachesAt(block.getLocation());
			}
		}
	}

	@EventHandler
	public void onLavaPlace(PlayerBucketEmptyEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}

		final Block block = event.getBlock();
		if (event.getBucket() == Material.LAVA_BUCKET) {
			updateCrops(block.getWorld(), player);
		}
	}

	@EventHandler
	public void onLavaPickup(PlayerBucketFillEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}

		final Block block = event.getBlock();
		if (event.getItemStack() != null && event.getItemStack().getType() == Material.LAVA_BUCKET) {
			updateCrops(block.getWorld(), player);
		}
	}

	@EventHandler
	public void onPickUpLavaWithDispense(BlockDispenseEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		if (block.getState() instanceof Dispenser dispenser && dispenser.getInventory().contains(Material.BUCKET)
				&& !dispenser.getInventory().contains(Material.LAVA_BUCKET)
				&& dispenser.getBlockData() instanceof Directional direction
				&& block.getRelative(direction.getFacing()).getType() == Material.LAVA) {
			final Player player = closestPlayerFor(block);
			if (player != null) {
				updateCrops(block.getWorld(), player);
			}
		}
	}

	@EventHandler
	public void onPistonExtendIntoLava(BlockPistonExtendEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		final Player player = closestPlayerFor(block);
		if (player != null) {
			event.getBlocks().stream().filter(lava -> lava.getType() == Material.LAVA)
					.forEach(lava -> updateCrops(block.getWorld(), player));
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		if (!(block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable)) {
			return;
		}
		final Player player = event.getPlayer();
		updateCrops(block.getWorld(), player);
		if (block.getBlockData() instanceof Farmland) {
			clearCachesAt(block.getLocation());
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

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
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		if (block.getBlockData() instanceof Farmland) {
			if (!lavaRainCheck(block)) {
				event.setCancelled(true);
				final Player player = closestPlayerFor(block);
				if (player != null) {
					updateCrops(block.getWorld(), player);
				}
			} else {
				moistureCache.remove(block.getLocation());
			}
		}
	}

	@EventHandler
	public void onBoneMeal(BlockFertilizeEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}

		final List<BlockState> blocks = event.getBlocks();
		blocks.stream().filter(block -> block.getBlockData() instanceof Ageable)
				.forEach(block -> updateCrops(block.getWorld(), player));
	}

	@EventHandler
	public void onHarvest(PlayerHarvestBlockEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}

		final Block block = event.getHarvestedBlock();
		if (block.getBlockData() instanceof Ageable) {
			updateCrops(block.getWorld(), player);
		}
	}

	@EventHandler
	public void onGrow(BlockGrowEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		final Player player = closestPlayerFor(block);
		if (player != null) {
			updateCrops(block.getWorld(), player);
		}
	}

	@EventHandler
	public void onFarmland(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}

		if (!(event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
				|| event.getAction() == org.bukkit.event.block.Action.PHYSICAL)) {
			return;
		}

		if (event.getItem() != null && (!(Tag.ITEMS_HOES.isTagged(event.getItem().getType())
				|| Tag.CROPS.isTagged(event.getItem().getType())))) {
			return;
		}

		final Block block = event.getClickedBlock();
		if (block != null && (Tag.DIRT.isTagged(block.getType()) || block.getBlockData() instanceof Farmland)) {
			updateCrops(block.getWorld(), player);
			if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL
					&& (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable)) {
				clearCachesAt(block.getLocation());
			}
		}
	}

	@EventHandler
	public void onFarmlandEntity(EntityInteractEvent event) {
		final Entity entity = event.getEntity();
		if (!isInCorrectWorld(entity.getWorld())) {
			return;
		}

		if (entity instanceof Player) {
			return;
		}

		if (!(entity instanceof LivingEntity)) {
			return;
		}
		final Block block = event.getBlock();
		if (!(block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable)) {
			return;
		}
		final Player player = closestPlayerFor(block);
		if (player != null) {
			updateCrops(block.getWorld(), player);
		}
		if (block.getBlockData() instanceof Farmland) {
			clearCachesAt(block.getLocation());
		}
	}

	public boolean checkForLavaAroundSugarCane(@Nullable Block block) {
		if (block == null || block.isEmpty() || !(GROWING_SUGAR_CANE_BLOCKS.contains(block.getType()))) {
			return false;
		}
		if (!isInCorrectWorld(block.getWorld())) {
			return false;
		}

		for (BlockFace face : FACES) {
			if (block.getRelative(face).getType() == Material.AIR) {
				continue;
			}
			if (block.getRelative(face).getType() == Material.LAVA) {
				return true;
			}
		}
		return false;
	}

	@EventHandler
	public void onForceSugarCanePlacement(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}
		if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final Block block = event.getClickedBlock();
		final Material item = event.getMaterial();
		final BlockFace face = event.getBlockFace();

		if (block == null || face != BlockFace.UP || item != Material.SUGAR_CANE) {
			return;
		}

		// Case 1: placing on growable block with lava nearby
		if (GROWING_SUGAR_CANE_BLOCKS.contains(block.getType()) && block.getRelative(face).isEmpty()
				&& checkForLavaAroundSugarCane(block)) {
			placeSugarCane(player, event, block, face);
			return;
		}

		// Case 2: placing on existing cane with lava around bottom
		if (block.getType() == Material.SUGAR_CANE && checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN))) {
			placeSugarCane(player, event, block, face);
		}
	}

	private void placeSugarCane(Player player, PlayerInteractEvent event, Block base, BlockFace face) {
		event.setUseItemInHand(Result.ALLOW);

		if (player.getGameMode() != GameMode.CREATIVE && event.getItem() != null) {
			event.getItem().setAmount(Math.max(0, event.getItem().getAmount() - 1));
		}

		VersionHelper.getNMSManager().swingHand(player,
				event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);

		final Block target = base.getRelative(face);
		target.setType(Material.SUGAR_CANE);

		playPlaceSoundAround(target);
		player.updateInventory();
	}

	@EventHandler
	public void onSugarCaneUpdate(BlockPhysicsEvent event) {
		final Block block = event.getSourceBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		if (block.getType() == Material.SUGAR_CANE) {
			event.setCancelled(true);
			tryBreakCaneIfNoLava(block, null);
			return;
		}

		final Block below = block.getRelative(BlockFace.DOWN);
		if (below.getType() == Material.SUGAR_CANE) {
			event.setCancelled(true);
			tryBreakCaneIfNoLava(below, null);
			return;
		}

		for (BlockFace face : FACES) {
			final Block neighbor = block.getRelative(face);

			if (neighbor.getType() == Material.SUGAR_CANE) {
				event.setCancelled(true);
				tryBreakCaneIfNoLava(neighbor, null);
				return;
			}

			final Block neighborBelow = neighbor.getRelative(BlockFace.DOWN);
			if (neighborBelow.getType() == Material.SUGAR_CANE) {
				event.setCancelled(true);
				tryBreakCaneIfNoLava(neighborBelow, null);
				return;
			}
		}
	}

	@EventHandler
	public void onPickUpLavaWithDispenseSugarCane(BlockDispenseEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		if (block.getState() instanceof Dispenser dispenser && dispenser.getInventory().contains(Material.BUCKET)
				&& !dispenser.getInventory().contains(Material.LAVA_BUCKET)
				&& dispenser.getBlockData() instanceof Directional direction
				&& block.getRelative(direction.getFacing()).getType() == Material.LAVA) {

			for (BlockFace face : FACES) {
				final Block neighbor = block.getRelative(direction.getFacing()).getRelative(face)
						.getRelative(BlockFace.UP);
				if (neighbor.getType() == Material.SUGAR_CANE) {
					tryBreakCaneIfNoLava(neighbor, null);
				}
			}
		}
	}

	private void tryBreakCaneIfNoLava(Block block, @Nullable Player player) {
		// safety: must be cane
		if (block.getType() != Material.SUGAR_CANE) {
			return;
		}

		// If lava nearby → just cancel
		if (checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN))
				|| checkForLavaAroundSugarCane(block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN))) {
			return;
		}

		// Otherwise, break cane + handle blocks above
		handleBlocksAboveCane(block);
		breakSugarCaneChain(block, true, player);
	}

	private void breakSugarCaneChain(@NotNull Block start, boolean includeBase, @Nullable Player player) {
		final World world = start.getWorld();
		Block current = includeBase ? start : start.getRelative(BlockFace.UP);

		while (current.getType() == Material.SUGAR_CANE) {
			final Location loc = current.getLocation();

			world.dropItemNaturally(loc, new ItemStack(Material.SUGAR_CANE));
			world.spawnParticle(Particle.BLOCK, loc, 5, Material.SUGAR_CANE.createBlockData());
			current.setType(Material.AIR, false);

			current = current.getRelative(BlockFace.UP);
		}

		// Sound either to player or area
		if (player != null) {
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
		} else {
			playBreakSoundAround(start);
		}
	}

	@SuppressWarnings("deprecation")
	private void handleBlocksAboveCane(@NotNull Block caneBase) {
		final World world = caneBase.getWorld();
		final Block above1 = caneBase.getRelative(BlockFace.UP);
		final Block above2 = above1.getRelative(BlockFace.UP);

		List.of(above1, above2).forEach(block -> {
			if (block.getType().hasGravity()) {
				FallingBlock falling = world.spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5),
						block.getBlockData());
				falling.setDropItem(false);
				falling.setHurtEntities(false);
				block.setType(Material.AIR);
			} else if (Tag.WOOL_CARPETS.isTagged(block.getType())) {
				world.dropItemNaturally(block.getLocation(), new ItemStack(block.getType()));
				block.setType(Material.AIR);
			}
		});
	}

	@EventHandler
	public void onBreakSugarCane(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		final Player player = event.getPlayer();
		if (block.getType() != Material.SUGAR_CANE) {
			return;
		}
		event.setCancelled(true);
		tryBreakCaneIfNoLava(block, player);
	}

	@EventHandler
	public void onExplodeSugarCane(BlockExplodeEvent event) {
		for (Block block : event.blockList()) {
			if (!isInCorrectWorld(block.getWorld())) {
				continue;
			}
			if (block.getType() == Material.SUGAR_CANE) {
				event.setCancelled(true);
				tryBreakCaneIfNoLava(block, null);
			}
		}
	}

	@EventHandler
	public void onGrowSugarCane(BlockGrowEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld())) {
			return;
		}

		if (!(block.getType() == Material.SUGAR_CANE
				|| block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE)) {
			return;
		}
		event.setCancelled(true);
		tryBreakCaneIfNoLava(block, null);
	}

	@EventHandler
	public void onPistonExtendSugarCane(BlockPistonExtendEvent event) {
		final Block piston = event.getBlock();
		if (!isInCorrectWorld(piston.getWorld())) {
			return;
		}

		event.getBlocks().stream().filter(moved -> moved.getType() == Material.SUGAR_CANE)
				.forEach(moved -> tryBreakCaneIfNoLava(moved, null));
	}

	@EventHandler
	public void onPickUpLavaNextToSugarCane(PlayerBucketFillEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}

		if (event.getItemStack() != null && event.getItemStack().getType() == Material.LAVA_BUCKET) {
			for (BlockFace face : FACES) {
				final Block neighbor = event.getBlockClicked().getRelative(face).getRelative(BlockFace.UP);
				if (neighbor.getType() == Material.SUGAR_CANE) {
					tryBreakCaneIfNoLava(neighbor, player);
				}
			}
		}
	}

	@EventHandler
	public void onForceCocoaBeanPlacement(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}
		if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final Block clicked = event.getClickedBlock();
		final Material item = event.getMaterial();
		final BlockFace face = event.getBlockFace();

		// Must click valid face
		if (clicked == null || !Arrays.asList(FACES).contains(face)) {
			return;
		}

		// Must be cocoa beans on a crimson/warped stem, with air on the target face
		if (item != Material.COCOA_BEANS
				|| !(clicked.getType() == Material.CRIMSON_STEM || clicked.getType() == Material.WARPED_STEM)
				|| !clicked.getRelative(face).isEmpty()) {
			return;
		}

		placeCocoaBean(player, event, clicked, face);
	}

	private void placeCocoaBean(Player player, PlayerInteractEvent event, Block stem, BlockFace face) {
		event.setUseItemInHand(Result.ALLOW);

		if (player.getGameMode() != GameMode.CREATIVE && event.getItem() != null) {
			event.getItem().setAmount(Math.max(0, event.getItem().getAmount() - 1));
		}

		VersionHelper.getNMSManager().swingHand(player,
				event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);

		AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
				Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.wood.place"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));

		player.updateInventory();

		final Block target = stem.getRelative(face);
		target.setType(Material.COCOA);

		if (!(target.getBlockData() instanceof Cocoa cocoa)) {
			return;
		}
		cocoa.setFacing(face.getOppositeFace());
		target.setBlockData(cocoa);
	}

	public class ConcreteConverter {

		@EventHandler
		public void onFlowConcretePowder(BlockFromToEvent event) {
			final Block block = event.getBlock();
			if (!isInCorrectWorld(block.getWorld())) {
				return;
			}
			if (!Arrays.asList(FACES).contains(event.getFace())) {
				return;
			}

			if (block.getType() == Material.LAVA) {
				for (BlockFace face : FACES) {
					if (Tag.CONCRETE_POWDER.isTagged(event.getToBlock().getRelative(face).getType())) {
						event.getToBlock().getRelative(face)
								.setType(convertToConcrete(event.getToBlock().getRelative(face).getType()));
						playExtinguishSoundNear(block);
						break;
					}
				}
			}
		}

		@EventHandler
		public void onPlaceConcretePowder(BlockPlaceEvent event) {
			final Block block = event.getBlockPlaced();
			if (!isInCorrectWorld(block.getWorld())) {
				return;
			}
			if (Tag.CONCRETE_POWDER.isTagged(block.getType())) {
				for (BlockFace face : FACES) {
					if (block.getRelative(face).getType() == Material.LAVA) {
						block.setType(convertToConcrete(block.getType()));
						playExtinguishSoundNear(block);
						break;
					}
				}
			}
		}

		@EventHandler
		public void onFallConcretePowder(EntityDropItemEvent event) {
			if (!(event.getEntity() instanceof FallingBlock fallingBlock)) {
				return;
			}
			if (!isInCorrectWorld(fallingBlock.getWorld())) {
				return;
			}
			if (!Tag.CONCRETE_POWDER.isTagged(fallingBlock.getBlockData().getMaterial())) {
				return;
			}
			final Material powder = fallingBlock.getBlockData().getMaterial();
			if (fallingBlock.getLocation().getBlock().getRelative(BlockFace.UP).getType() == Material.LAVA) {
				event.setCancelled(true);
				instance.getScheduler().sync().runLater(() -> fallingBlock.getLocation().getBlock()
						.getRelative(BlockFace.UP).setType(convertToConcrete(powder)), 5, fallingBlock.getLocation());
				playExtinguishSoundNear(fallingBlock.getLocation().getBlock());
				fallingBlock.setDropItem(false);
			} else {
				for (BlockFace face : FACES) {
					if (fallingBlock.getLocation().getBlock().getRelative(face).getType() == Material.LAVA) {
						event.setCancelled(true);
						instance.getScheduler().sync().runLater(
								() -> fallingBlock.getLocation().getBlock().setType(convertToConcrete(powder)), 5,
								fallingBlock.getLocation());
						playExtinguishSoundNear(fallingBlock.getLocation().getBlock());
						fallingBlock.setDropItem(false);
						break;
					}
				}
			}
		}

		private @NotNull Material convertToConcrete(Material powderType) {
			return switch (powderType) {
			case WHITE_CONCRETE_POWDER -> Material.WHITE_CONCRETE;
			case BLACK_CONCRETE_POWDER -> Material.BLACK_CONCRETE;
			case BROWN_CONCRETE_POWDER -> Material.BROWN_CONCRETE;
			case GRAY_CONCRETE_POWDER -> Material.GRAY_CONCRETE;
			case LIGHT_GRAY_CONCRETE_POWDER -> Material.LIGHT_GRAY_CONCRETE;
			case BLUE_CONCRETE_POWDER -> Material.BLUE_CONCRETE;
			case LIGHT_BLUE_CONCRETE_POWDER -> Material.LIGHT_BLUE_CONCRETE;
			case CYAN_CONCRETE_POWDER -> Material.CYAN_CONCRETE;
			case GREEN_CONCRETE_POWDER -> Material.GREEN_CONCRETE;
			case LIME_CONCRETE_POWDER -> Material.LIME_CONCRETE;
			case RED_CONCRETE_POWDER -> Material.RED_CONCRETE;
			case YELLOW_CONCRETE_POWDER -> Material.YELLOW_CONCRETE;
			case ORANGE_CONCRETE_POWDER -> Material.ORANGE_CONCRETE;
			case PINK_CONCRETE_POWDER -> Material.PINK_CONCRETE;
			case PURPLE_CONCRETE_POWDER -> Material.PURPLE_CONCRETE;
			case MAGENTA_CONCRETE_POWDER -> Material.MAGENTA_CONCRETE;
			default -> powderType;
			};
		}
	}

	private boolean isInCorrectWorld(@Nullable World world) {
		if (world == null) {
			return false;
		}
		return instance.getHellblockHandler().isInCorrectWorld(world);
	}

	private boolean isInCorrectWorld(@Nullable Player player) {
		if (player == null) {
			return false;
		}
		return instance.getHellblockHandler().isInCorrectWorld(player);
	}

	/**
	 * Clear the three caches at once (replaces repeated removal code).
	 */
	private void clearCachesAt(@NotNull Location loc) {
		blockCache.remove(loc);
		revertCache.remove(loc);
		moistureCache.remove(loc);
	}

	/**
	 * Reused lava / rain cancel check.
	 */
	public boolean lavaRainCheck(@NotNull Block block) {
		final Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
				.filter(task -> block.getWorld().getName().equalsIgnoreCase(task.getWorld().worldName())).findAny();

		Block highest = instance.getLavaRainHandler().getHighestBlock(block.getLocation());
		return lavaRain.isPresent() && lavaRain.get().isLavaRaining() && highest != null
				&& (highest.isPassable() || highest.isEmpty() || highest.isLiquid()) && highest.getY() > block.getY();
	}

	/**
	 * Helper: find closest player in 25 radius (original behavior).
	 */
	private Player closestPlayerFor(@NotNull Block block) {
		final Collection<Entity> playersNearby = block.getWorld()
				.getNearbyEntities(block.getLocation(), PLAYER_SEARCH_RADIUS, PLAYER_SEARCH_RADIUS,
						PLAYER_SEARCH_RADIUS)
				.stream().filter(e -> e.getType() == EntityType.PLAYER).collect(Collectors.toList());
		return instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(), playersNearby);
	}

	private void playBreakSoundAround(Block block) {
		block.getWorld()
				.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
						instance.getConfigManager().searchRadius(), instance.getConfigManager().searchRadius())
				.stream().filter(e -> e.getType() == EntityType.PLAYER).toList().stream()
				.filter(entity -> entity instanceof Player).map(entity -> (Player) entity)
				.forEach(player -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.break"),
								net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1)));
	}

	private void playPlaceSoundAround(Block block) {
		block.getWorld()
				.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
						instance.getConfigManager().searchRadius(), instance.getConfigManager().searchRadius())
				.stream().filter(e -> e.getType() == EntityType.PLAYER).toList().stream()
				.filter(entity -> entity instanceof Player).map(entity -> (Player) entity)
				.forEach(player -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.grass.place"),
								net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1)));
	}

	private void playExtinguishSoundNear(Block block) {
		block.getWorld()
				.getNearbyEntities(block.getLocation(), instance.getConfigManager().searchRadius(),
						instance.getConfigManager().searchRadius(), instance.getConfigManager().searchRadius())
				.stream().filter(e -> e.getType() == EntityType.PLAYER).toList().stream()
				.filter(entity -> entity instanceof Player).map(entity -> (Player) entity)
				.forEach(player -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.lava.extinguish"),
								net.kyori.adventure.sound.Sound.Source.AMBIENT, 1, 1)));
	}
}