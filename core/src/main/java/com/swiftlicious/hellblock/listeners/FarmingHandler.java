package com.swiftlicious.hellblock.listeners;

import static java.util.Map.entry;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.Acrobot.ChestShop.Libs.Kyori.adventure.text.format.NamedTextColor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.challenges.requirement.FarmRequirement;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.GlowTreeHandler.CustomTreeGrowContext;
import com.swiftlicious.hellblock.listeners.weather.WeatherType;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeData;
import com.swiftlicious.hellblock.upgrades.UpgradeTier;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockRenderer;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomBlockTypes;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;
import com.swiftlicious.hellblock.world.block.Growable;
import com.swiftlicious.hellblock.world.block.MoistureHolder;
import com.swiftlicious.hellblock.world.block.crop.CustomCocoaBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomMelonStemBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomMushroomBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomPumpkinStemBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomSaplingBlock;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Handles all crop, farmland, and farming-related mechanics for Hellblock
 * islands.
 *
 * <p>
 * This class manages:
 * </p>
 * <ul>
 * <li>Updating farmland moisture and crop states based on block metadata and
 * environment</li>
 * <li>Caching crop growth bonuses based on island upgrades</li>
 * <li>Handling player interactions, world events, and block lifecycle
 * changes</li>
 * <li>Resolving crop types and delegating to specific crop handlers (e.g.,
 * sugar cane, nether wart)</li>
 * </ul>
 *
 * <p>
 * The farming system is optimized using position-based caching (via
 * {@link Pos3}), asynchronous island resolution, and dynamic event responses
 * based on world state.
 * </p>
 *
 * <p>
 * This handler is central to maintaining accurate and performant farm behavior
 * across all active islands.
 * </p>
 */
public class FarmingHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	protected CropTypeResolver cropResolver;
	protected ConcreteConverter concreteConverter;

	private Set<CustomBlock> farmBlocks = ConcurrentHashMap.newKeySet();
	private Set<CustomBlock> verticalFarmBlocks = ConcurrentHashMap.newKeySet();
	private Set<CustomBlock> farmSaplings = ConcurrentHashMap.newKeySet();

	public Map<Material, CustomBlock> placedCropMapping = new ConcurrentHashMap<>();
	public Map<Material, CustomBlock> placedBlockCropMapping = new ConcurrentHashMap<>();
	public Map<Material, CustomBlock> placedSaplingMapping = new ConcurrentHashMap<>();

	private Map<Material, CustomBlock> buildPlacedSaplingMapping() {
		Map<Material, CustomBlock> map = new EnumMap<>(Material.class);

		try {
			map.put(Material.OAK_SAPLING, CustomBlockTypes.GLOW_SAPLING);
			map.put(Material.BIRCH_SAPLING, CustomBlockTypes.GLOW_SAPLING);
			map.put(Material.SPRUCE_SAPLING, CustomBlockTypes.GLOW_SAPLING);
			map.put(Material.JUNGLE_SAPLING, CustomBlockTypes.GLOW_SAPLING);
			map.put(Material.ACACIA_SAPLING, CustomBlockTypes.GLOW_SAPLING);
			map.put(Material.DARK_OAK_SAPLING, CustomBlockTypes.GLOW_SAPLING);
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			// Should never happen with these
		}

		// Handle newer saplings conditionally
		try {
			map.put(Material.matchMaterial("CHERRY_SAPLING"), CustomBlockTypes.GLOW_SAPLING);
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}

		try {
			map.put(Material.matchMaterial("MANGROVE_PROPAGULE"), CustomBlockTypes.GLOW_SAPLING);
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}

		try {
			map.put(Material.matchMaterial("PALE_OAK_SAPLING"), CustomBlockTypes.GLOW_SAPLING);
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}

		return Collections.unmodifiableMap(map);
	}

	public static final Set<String> SUGAR_CANE_KEYS = Set.of("minecraft:sugar_cane", "hellblock:sugar_cane");
	public static final Set<String> CACTUS_KEYS = Set.of("minecraft:cactus", "hellblock:cactus");
	public static final Set<String> BAMBOO_KEYS = Set.of("minecraft:bamboo", "hellblock:bamboo");

	private final Cache<Integer, Set<PositionedBlock>> farmCache = Caffeine.newBuilder()
			.expireAfterWrite(3, TimeUnit.MINUTES).build();

	private final Map<Pos3, Integer> blockCache = new LinkedHashMap<>();
	private final Map<Pos3, Integer> moistureCache = new LinkedHashMap<>();
	private final Map<Pos3, Integer> revertCache = new LinkedHashMap<>();

	private static final Map<String, Integer> BASE_CHANCE_NO_LAVA = Map.ofEntries(
			// Vanilla & Hellblock crop types — normal base growth
			entry("minecraft:wheat", 20), entry("hellblock:wheat", 20), entry("minecraft:carrots", 20),
			entry("hellblock:carrots", 20), entry("minecraft:potatoes", 20), entry("hellblock:potatoes", 20),
			entry("minecraft:beetroots", 10), entry("hellblock:beetroots", 10), entry("minecraft:melon_stem", 10),
			entry("hellblock:melon_stem", 10), entry("minecraft:pumpkin_stem", 10), entry("hellblock:pumpkin_stem", 10),
			entry("minecraft:attached_melon_stem", 10), entry("hellblock:attached_melon_stem", 10),
			entry("minecraft:attached_pumpkin_stem", 10), entry("hellblock:attached_pumpkin_stem", 10),
			entry("minecraft:sugar_cane", 0), // Does not grow without lava near
			// base
			entry("hellblock:sugar_cane", 0), entry("minecraft:nether_wart", 15), entry("hellblock:nether_wart", 15),
			entry("minecraft:red_mushroom", 10), entry("minecraft:brown_mushroom", 10),
			entry("hellblock:red_mushroom", 10), entry("hellblock:brown_mushroom", 10), entry("minecraft:cocoa", 10),
			entry("hellblock:cocoa", 10), entry("minecraft:sweet_berry_bush", 10),
			entry("hellblock:sweet_berry_bush", 10), entry("minecraft:cactus", 10), entry("hellblock:cactus", 10),
			entry("minecraft:bamboo", 10), entry("hellblock:bamboo", 10), entry("hellblock:glow_sapling", 10));

	private static final Map<String, Integer> BASE_CHANCE_LAVA = Map.ofEntries(
			// Lava rain or nearby lava (enhanced growth)
			entry("minecraft:wheat", 35), entry("hellblock:wheat", 35), entry("minecraft:carrots", 35),
			entry("hellblock:carrots", 35), entry("minecraft:potatoes", 35), entry("hellblock:potatoes", 35),
			entry("minecraft:beetroots", 25), entry("hellblock:beetroots", 25), entry("minecraft:melon_stem", 25),
			entry("hellblock:melon_stem", 25), entry("minecraft:pumpkin_stem", 25), entry("hellblock:pumpkin_stem", 25),
			entry("minecraft:attached_melon_stem", 25), entry("hellblock:attached_melon_stem", 25),
			entry("minecraft:attached_pumpkin_stem", 25), entry("hellblock:attached_pumpkin_stem", 25),
			entry("minecraft:sugar_cane", 35), // Still requires lava nearby to
			// function
			entry("hellblock:sugar_cane", 35), entry("minecraft:nether_wart", 30), entry("hellblock:nether_wart", 30),
			entry("minecraft:red_mushroom", 25), entry("minecraft:brown_mushroom", 25),
			// cocoa only influenced by lava rain
			entry("hellblock:red_mushroom", 25), entry("hellblock:brown_mushroom", 25), entry("minecraft:cocoa", 35),
			entry("hellblock:cocoa", 35), entry("minecraft:sweet_berry_bush", 25), // Only lava rain
			entry("hellblock:sweet_berry_bush", 25), entry("minecraft:cactus", 30), entry("hellblock:cactus", 30),
			entry("minecraft:bamboo", 30), entry("hellblock:bamboo", 30), entry("hellblock:glow_sapling", 25));

	public static final BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	private static final Set<String> VALID_SUGAR_CANE_SOIL_KEYS = Set.of("minecraft:grass_block", "minecraft:dirt",
			"minecraft:coarse_dirt", "minecraft:mycelium", "minecraft:sand", "minecraft:red_sand",
			"minecraft:suspicious_sand", "minecraft:mud", "minecraft:moss_block", "minecraft:podzol",
			"minecraft:rooted_dirt");

	private static final Set<String> VALID_MUSHROOM_SOIL_KEYS = Set.of("minecraft:dirt", "minecraft:grass_block",
			"minecraft:podzol", "minecraft:mycelium", "minecraft:crimson_nylium", "minecraft:warped_nylium");

	private static final Set<String> VALID_FRUIT_SOIL_KEYS = Set.of("minecraft:farmland", "minecraft:dirt",
			"minecraft:coarse_dirt", "minecraft:rooted_dirt", "minecraft:podzol", "minecraft:grass_block",
			"minecraft:moss_block");

	private static final Set<String> VALID_SWEET_BERRY_SOIL_KEYS = Set.of("minecraft:grass_block", "minecraft:dirt",
			"minecraft:coarse_dirt", "minecraft:podzol", "minecraft:farmland", "minecraft:moss_block",
			"minecraft:rooted_dirt", "minecraft:snow" // single snow layer is allowed
	);

	private static final Set<String> VALID_DIRT_BLOCK_KEYS = Set.of("minecraft:dirt", "minecraft:grass_block",
			"hellblock:dirt");

	private static final Set<String> COCOA_STEM_KEYS = Set.of("minecraft:crimson_stem", "minecraft:warped_stem");
	private static final Set<String> SOUL_SAND_KEY = Set.of("minecraft:soul_sand");
	private static final Set<String> LAVA_KEY = Set.of("minecraft:lava");

	/** Maximum radius around farmland to search for nearby lava blocks **/
	private static final int FARM_BLOCK_CHECK_RADIUS = 4;

	private final Map<Integer, Double> cropGrowthBonusCache = new ConcurrentHashMap<>();

	/** Tracks crops placed by players that have not yet grown naturally **/
	private final Set<Pos3> playerPlacedCrops = ConcurrentHashMap.newKeySet();
	private final Set<Pos3> lavaGrownMushrooms = ConcurrentHashMap.newKeySet();

	public FarmingHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		this.farmBlocks = Set.of(CustomBlockTypes.FARMLAND, CustomBlockTypes.NETHER_WART, CustomBlockTypes.RED_MUSHROOM,
				CustomBlockTypes.BROWN_MUSHROOM, CustomBlockTypes.SWEET_BERRY_BUSH, CustomBlockTypes.COCOA,
				CustomBlockTypes.WHEAT, CustomBlockTypes.CARROTS, CustomBlockTypes.POTATOES, CustomBlockTypes.BEETROOTS,
				CustomBlockTypes.MELON_STEM, CustomBlockTypes.PUMPKIN_STEM, CustomBlockTypes.MELON_ATTACHED_STEM,
				CustomBlockTypes.PUMPKIN_ATTACHED_STEM);
		this.verticalFarmBlocks = Set.of(CustomBlockTypes.SUGAR_CANE, CustomBlockTypes.CACTUS, CustomBlockTypes.BAMBOO);
		this.farmSaplings = Set.of(CustomBlockTypes.GLOW_SAPLING);
		this.placedCropMapping = Map.ofEntries(entry(Material.WHEAT_SEEDS, CustomBlockTypes.WHEAT),
				entry(Material.CARROTS, CustomBlockTypes.CARROTS), entry(Material.POTATOES, CustomBlockTypes.POTATOES),
				entry(Material.BEETROOT_SEEDS, CustomBlockTypes.BEETROOTS),
				entry(Material.MELON_SEEDS, CustomBlockTypes.MELON_STEM),
				entry(Material.PUMPKIN_SEEDS, CustomBlockTypes.PUMPKIN_STEM));
		this.placedBlockCropMapping = Map.ofEntries(entry(Material.RED_MUSHROOM, CustomBlockTypes.RED_MUSHROOM),
				entry(Material.BROWN_MUSHROOM, CustomBlockTypes.BROWN_MUSHROOM),
				entry(Material.CACTUS, CustomBlockTypes.CACTUS), entry(Material.BAMBOO, CustomBlockTypes.BAMBOO));
		this.placedSaplingMapping = buildPlacedSaplingMapping();
		this.cropResolver = new CropTypeResolver(this);
		this.concreteConverter = new ConcreteConverter();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.farmBlocks = null;
		this.verticalFarmBlocks = null;
		this.farmSaplings = null;
		this.placedCropMapping = null;
		this.placedBlockCropMapping = null;
		this.placedSaplingMapping = null;
		this.cropResolver = null;
		this.concreteConverter = null;
		this.farmCache.cleanUp();
		this.blockCache.clear();
		this.moistureCache.clear();
		this.revertCache.clear();
		this.cropGrowthBonusCache.clear();
	}

	@NotNull
	public Map<Material, CustomBlock> getPlacedCropMapping() {
		return this.placedCropMapping;
	}

	@NotNull
	public Map<Material, CustomBlock> getPlacedBlockCropMapping() {
		return this.placedBlockCropMapping;
	}

	@NotNull
	public Map<Material, CustomBlock> getPlacedSaplingMapping() {
		return this.placedSaplingMapping;
	}

	@NotNull
	public ConcreteConverter getConcreteConverter() {
		return this.concreteConverter;
	}

	/**
	 * Represents a block with both its position and associated custom block state.
	 *
	 * <p>
	 * This record is used throughout the farming system to associate a {@link Pos3}
	 * coordinate with a {@link CustomBlockState} in order to track and manipulate
	 * block data in the Hellblock world context.
	 * </p>
	 *
	 * @param pos   the 3D position of the block
	 * @param state the custom block state associated with that position
	 */
	public record PositionedBlock(@NotNull Pos3 pos, @NotNull CustomBlockState state) {
	}

	/**
	 * Clears all cached farming-related data for a specific island.
	 *
	 * <p>
	 * This includes:
	 * </p>
	 * <ul>
	 * <li>Invalidating the island’s farm crop cache</li>
	 * <li>Removing tracked player-placed crops located on the island</li>
	 * <li>Removing lava-grown mushrooms associated with the island</li>
	 * </ul>
	 *
	 * @param world    the Hellblock world instance associated with the island
	 * @param islandId the ID of the island whose farm cache should be cleared
	 */
	public void clearIslandFarmCache(@NotNull HellblockWorld<?> world, int islandId) {
		// Remove farm crop data cache
		farmCache.invalidate(islandId);

		// Remove any tracking that this farm is loaded (stored as Pos3)
		List<Pos3> toRemovePlacedCrops = new ArrayList<>();

		List<CompletableFuture<Void>> placedCropFutures = playerPlacedCrops.stream().map(pos -> {
			Location location = pos.toLocation(world.bukkitWorld());
			return instance.getIslandManager().resolveIslandId(location).thenAccept(optId -> {
				if (optId.isPresent() && optId.get() == islandId) {
					synchronized (toRemovePlacedCrops) {
						toRemovePlacedCrops.add(pos);
					}
				}
			});
		}).toList();

		CompletableFuture.allOf(placedCropFutures.toArray(CompletableFuture[]::new))
				.thenRun(() -> playerPlacedCrops.removeAll(toRemovePlacedCrops));

		// Remove lava-grown mushrooms (stored as Pos3)
		List<Pos3> toRemoveMushrooms = new ArrayList<>();

		List<CompletableFuture<Void>> mushroomFutures = lavaGrownMushrooms.stream().map(pos -> {
			Location loc = pos.toLocation(world.bukkitWorld());
			return instance.getIslandManager().resolveIslandId(loc).thenAccept(optId -> {
				if (optId.isPresent() && optId.get() == islandId) {
					synchronized (toRemoveMushrooms) {
						toRemoveMushrooms.add(pos);
					}
				}
			});
		}).toList();

		CompletableFuture.allOf(mushroomFutures.toArray(CompletableFuture[]::new))
				.thenRun(() -> lavaGrownMushrooms.removeAll(toRemoveMushrooms));

		instance.debug("Cleared farming cache for island ID " + islandId);
	}

	/**
	 * Triggers crop updates for the specified island.
	 *
	 * <p>
	 * This method processes all tracked farm blocks on the island. For each block:
	 * </p>
	 * <ul>
	 * <li>If it's farmland with moisture data, it is updated via
	 * {@code handleFarmland}</li>
	 * <li>If it's a known crop type, it is passed to the
	 * {@link CropTypeResolver}</li>
	 * </ul>
	 *
	 * <p>
	 * Also handles any glowstone tree growth mechanics associated with the island.
	 * </p>
	 *
	 * @param world    the Hellblock world instance the island resides in
	 * @param islandId the ID of the island to update
	 */
	public void updateCrops(@NotNull HellblockWorld<?> world, int islandId) {
		getFarmBlocksByIslandId(world, islandId).thenAccept(farmBlocks -> {
			if (farmBlocks == null || farmBlocks.isEmpty())
				return;

			for (PositionedBlock block : farmBlocks) {
				CustomBlockState state = block.state();
				if (state == null)
					continue;

				String key = state.type().type().key().asString().toLowerCase();
				if (Set.of("minecraft:farmland", "hellblock:farmland").contains(key)
						&& state.type() instanceof MoistureHolder) {
					handleFarmland(block, world);
					continue;
				}

				this.cropResolver.handleIfKnown(block, world);
			}
		});

		handleGlowstoneTreeGrowth(world, islandId);
	}

	/**
	 * Retrieves the cached crop growth bonus for the given player's island data.
	 *
	 * <p>
	 * If the bonus is not yet cached, it will be calculated using the player's
	 * current crop growth upgrade level and stored for future use.
	 * </p>
	 *
	 * @param data the Hellblock data containing island ownership and upgrades
	 * @return the total crop growth bonus as a decimal multiplier (e.g., 0.15 for
	 *         +15%)
	 */
	public double getCachedCropGrowthBonus(@NotNull HellblockData data) {
		int islandId = data.getIslandId();
		return cropGrowthBonusCache.computeIfAbsent(islandId, id -> calculateCropGrowthBonus(data));
	}

	/**
	 * Updates (or adds) the crop growth bonus in the cache for the given island
	 * owner.
	 *
	 * <p>
	 * This recalculates the total crop growth bonus based on the island's current
	 * upgrade level and stores the result in the internal cache.
	 * </p>
	 *
	 * @param data the Hellblock data containing the island owner's upgrade
	 *             information
	 */
	public void updateCropGrowthBonusCache(@NotNull HellblockData data) {
		cropGrowthBonusCache.put(data.getIslandId(), calculateCropGrowthBonus(data));
	}

	/**
	 * Removes the cached crop growth bonus entry for the specified island owner.
	 *
	 * <p>
	 * This forces a recalculation the next time the bonus is requested.
	 * </p>
	 *
	 * @param islandId the id of the island that should be cleared
	 */
	public void invalidateCropGrowthBonusCache(int islandId) {
		cropGrowthBonusCache.remove(islandId);
	}

	/**
	 * Calculates the total crop growth bonus based on the player's island upgrade
	 * level.
	 *
	 * <p>
	 * The method iterates through all tiers up to the player's current
	 * {@code CROP_GROWTH} upgrade level and accumulates any defined bonus values.
	 * </p>
	 *
	 * @param data the Hellblock data containing island upgrade levels
	 * @return the total crop growth bonus as a decimal multiplier
	 */
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

	/**
	 * Applies the crop growth bonus for a block if it is valid and within the
	 * island bounds.
	 *
	 * <p>
	 * This method resolves the block's owner, verifies the block is within the
	 * island's bounding box, retrieves the cached bonus, and passes it to the
	 * provided consumer.
	 * </p>
	 *
	 * @param block         the positioned block to evaluate
	 * @param world         the Hellblock world context
	 * @param bonusConsumer a consumer that accepts the valid crop growth bonus
	 */
	private void withCropGrowthBonusIfValid(@NotNull PositionedBlock block, @NotNull HellblockWorld<?> world,
			@NotNull Consumer<Double> bonusConsumer) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		Pos3 pos = block.pos();

		instance.getCoopManager().getHellblockOwnerOfBlock(pos, world).thenAcceptAsync(ownerUUID -> {
			if (ownerUUID == null)
				return;

			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty())
							return;

						HellblockData data = userDataOpt.get().getHellblockData();
						BoundingBox box = data.getBoundingBox();

						if (box == null || !box.contains(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5))
							return;

						double bonus = getCachedCropGrowthBonus(data);
						bonusConsumer.accept(bonus);
					});
		});
	}

	/**
	 * Processes a farmland block to determine whether it should be hydrated or
	 * dehydrated based on lava proximity (not rain).
	 *
	 * <p>
	 * Farmland requires nearby lava to stay hydrated. Lava rain alone does not
	 * hydrate farmland. This method checks for nearby lava and schedules hydration
	 * or dehydration accordingly. Dehydrated farmland may lead to crop failure.
	 * </p>
	 *
	 * @param block the farmland block to handle
	 * @param world the Hellblock world the block exists in
	 */
	private void handleFarmland(@NotNull PositionedBlock block, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		if (!(block.state().type() instanceof MoistureHolder farm))
			return;

		Pos3 pos = block.pos();

		checkForLavaAroundFarm(block, world).thenAccept(lavaNearby -> {
			if (lavaNearby) {
				scheduleFarmlandHydration(pos, block, world, farm);
			} else {
				scheduleFarmlandDehydration(pos, block, world, farm);
			}
		});
	}

	/**
	 * Schedules asynchronous hydration of a farmland block over time.
	 *
	 * <p>
	 * If the moisture level is below the maximum, it is increased, and the updated
	 * block state is stored. Once hydrated, a crop growth bonus may be applied if
	 * lava rain is active at the location.
	 * </p>
	 *
	 * @param pos   the position of the farmland block
	 * @param block the positioned farmland block
	 * @param world the Hellblock world context
	 * @param farm  the moisture holder representing the farmland state
	 */
	private void scheduleFarmlandHydration(@NotNull Pos3 pos, @NotNull PositionedBlock block,
			@NotNull HellblockWorld<?> world, @NotNull MoistureHolder farm) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		final int randomValue = RandomUtils.generateRandomInt(15, 25);
		blockCache.putIfAbsent(pos, randomValue);

		world.scheduler().asyncLater(() -> {
			Integer stored = blockCache.get(pos);
			if (!Objects.equals(stored, randomValue))
				return;

			int currentMoisture = farm.getMoisture(block.state());
			if (currentMoisture != farm.getMaxMoisture(block.state()) && !revertCache.containsKey(pos)
					&& !moistureCache.containsKey(pos)) {

				farm.setMoisture(block.state(), farm.getMaxMoisture(block.state()));
				world.updateBlockState(pos, block.state());
				moistureCache.put(pos, farm.getMoisture(block.state()));
			}

			if (Objects.equals(moistureCache.get(pos), farm.getMaxMoisture(block.state()))) {
				blockCache.remove(pos);
				moistureCache.remove(pos);
			}

			applyBonusCropGrowthIfEligible(pos, world);

		}, RandomUtils.generateRandomInt(15, 30), TimeUnit.SECONDS);
	}

	/**
	 * Applies a bonus crop growth tick to the block above the given farmland if
	 * lava rain is active or lava is nearby (hydrated farmland).
	 *
	 * <p>
	 * If the block above is a {@link Growable} crop that has not reached max age, a
	 * growth roll is performed based on the configured base chance (adjusted for
	 * lava conditions) and any crop growth bonus from the island. This allows
	 * enhanced growth from either lava rain or lava proximity.
	 * </p>
	 *
	 * @param pos   the position of the farmland block (checks the block above)
	 * @param world the Hellblock world context
	 */
	private void applyBonusCropGrowthIfEligible(@NotNull Pos3 pos, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		instance.getIslandManager().resolveIslandId(world, pos).thenCompose(optIslandId -> {
			if (optIslandId.isEmpty())
				return CompletableFuture.completedFuture(null);

			int islandId = optIslandId.get();
			boolean lavaRain = isLavaRainingAt(islandId, pos.toLocation(world.bukkitWorld()));
			PositionedBlock baseBlock = new PositionedBlock(pos, null);

			return checkForLavaAroundFarm(baseBlock, world).thenCompose(lavaNearby -> {
				Pos3 abovePos = pos.up();
				return world.getBlockState(abovePos).thenApply(optAboveState -> {
					if (optAboveState.isEmpty())
						return null;

					CustomBlockState aboveState = optAboveState.get();
					if (!(aboveState.type() instanceof Growable growable))
						return null;

					int age = growable.getAge(aboveState);
					int max = growable.getMaxAge(aboveState);
					if (age >= max)
						return null;

					String key = aboveState.type().type().key().asString();
					int baseChance = getBaseChance(key, lavaRain, lavaNearby);

					withCropGrowthBonusIfValid(new PositionedBlock(abovePos, aboveState), world, bonus -> {
						int finalChance = (int) Math.min(100, baseChance + bonus);
						if (RandomUtils.roll(finalChance)) {
							growable.setAge(aboveState, age + 1);

							// Special case: stem -> fruit
							if (growable instanceof CustomMelonStemBlock
									|| growable instanceof CustomPumpkinStemBlock) {
								if (age + 1 >= max) {
									trySpawnFruit(pos, aboveState, world);
								}
							}

							world.updateBlockState(abovePos, aboveState);
						}
					});
					return null;
				});
			});
		});
	}

	/**
	 * Schedules asynchronous dehydration of a farmland block over time.
	 *
	 * <p>
	 * Farmland moisture gradually decreases when no lava is nearby. Once moisture
	 * reaches zero, the farmland reverts to dirt. Any crops or stems above are
	 * destroyed to simulate crop death, except for melon or pumpkin stems that
	 * remain attached to an existing fruit.
	 * </p>
	 *
	 * @param pos   the position of the farmland block
	 * @param block the positioned farmland block
	 * @param world the Hellblock world context
	 * @param farm  the moisture holder representing the farmland state
	 */
	private void scheduleFarmlandDehydration(@NotNull Pos3 pos, @NotNull PositionedBlock block,
			@NotNull HellblockWorld<?> world, @NotNull MoistureHolder farm) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		final int randomValue = RandomUtils.generateRandomInt(15, 20);

		world.scheduler().asyncLater(() -> {
			int moisture = farm.getMoisture(block.state());
			int newMoisture = Math.max(moisture - 1, 0);
			farm.setMoisture(block.state(), newMoisture);

			world.updateBlockState(pos, block.state()).thenRun(() -> {

				if (newMoisture > 0) {
					scheduleFarmlandDehydration(pos, block, world, farm);
					return;
				}

				if (farm.getMoisture(block.state()) != 0)
					return;

				revertCache.putIfAbsent(pos, randomValue);
				if (!Objects.equals(revertCache.get(pos), randomValue))
					return;

				Pos3 abovePos = pos.up();

				world.getBlockState(abovePos).thenAccept(optAbove -> {
					if (optAbove.isEmpty()) {
						world.updateBlockState(pos, CustomBlockTypes.fromMaterial(Material.DIRT).createBlockState())
								.thenRun(() -> revertCache.remove(pos));
						return;
					}

					CustomBlockState above = optAbove.get();
					CustomBlock cropKey = above.type();
					String key = cropKey.type().key().asString().toLowerCase();

					boolean isStem = key.contains("stem");
					boolean isCrop = key.contains("crop") || key.contains("wheat") || key.contains("carrot")
							|| key.contains("potato") || key.contains("beetroot");

					if (isStem) {
						Set<String> validFruitKeys = Set.of("minecraft:melon", "hellblock:melon", "minecraft:pumpkin",
								"hellblock:pumpkin");

						CompletableFuture<Boolean> anyFruitAttached = CompletableFuture
								.allOf(Arrays.stream(FACES).map(face -> world.getBlockState(abovePos.offset(face))
										.thenApply(neighbor -> neighbor.map(n -> {
											String neighborKey = n.type().type().key().asString().toLowerCase();
											return validFruitKeys.contains(neighborKey);
										}).orElse(false))).toArray(CompletableFuture[]::new))
								.thenApply(v -> Arrays.stream(FACES).map(face -> world
										.getBlockState(abovePos.offset(face)).thenApply(opt -> opt.map(n -> {
											String neighborKey = n.type().type().key().asString().toLowerCase();
											return validFruitKeys.contains(neighborKey);
										}).orElse(false))).map(CompletableFuture::join).anyMatch(b -> b));

						anyFruitAttached.thenAccept(attached -> {
							if (!attached && isCrop || isStem) {
								dropAndRemoveCrop(abovePos, cropKey, above, world);
							}

							world.updateBlockState(pos, CustomBlockTypes.fromMaterial(Material.DIRT).createBlockState())
									.thenRun(() -> revertCache.remove(pos));
						});
					} else if (isCrop) {
						dropAndRemoveCrop(abovePos, cropKey, above, world);
						world.updateBlockState(pos, CustomBlockTypes.fromMaterial(Material.DIRT).createBlockState())
								.thenRun(() -> revertCache.remove(pos));
					} else {
						world.updateBlockState(pos, CustomBlockTypes.fromMaterial(Material.DIRT).createBlockState())
								.thenRun(() -> revertCache.remove(pos));
					}
				});
			});
		}, RandomUtils.generateRandomInt(10, 15), TimeUnit.SECONDS);
	}

	/**
	 * Handles the visual and gameplay effects of a crop being destroyed.
	 *
	 * <p>
	 * This method plays the crop break sound and particles, drops any resulting
	 * items, and replaces the crop block with air. It is executed synchronously to
	 * ensure proper interaction with Bukkit's world state and particle systems.
	 * </p>
	 *
	 * @param abovePos the position of the crop block to be removed
	 * @param cropKey  the type of crop block
	 * @param above    the current block state of the crop
	 * @param world    the Hellblock world context
	 */
	private void dropAndRemoveCrop(@NotNull Pos3 abovePos, @NotNull CustomBlock cropKey,
			@NotNull CustomBlockState above, @NotNull HellblockWorld<?> world) {
		List<Item<ItemStack>> drops = inferDropFromKey(cropKey, above);

		instance.getScheduler().executeSync(() -> {
			Location dropLoc = abovePos.toLocation(world.bukkitWorld());

			AdventureHelper.playPositionalSound(world.bukkitWorld(), dropLoc,
					Sound.sound(Key.key("minecraft:block.crop.break"), Source.BLOCK, 1.0f, 1.0f));

			Material mat = CustomBlockRenderer.resolveBlockType(above);
			world.bukkitWorld().spawnParticle(ParticleUtils.getParticle("BLOCK_DUST"),
					dropLoc.clone().add(0.5, 0.5, 0.5), 15, 0.25, 0.25, 0.25, mat.createBlockData());

			if (drops != null) {
				drops.forEach(drop -> world.bukkitWorld().dropItemNaturally(dropLoc, drop.loadCopy()));
			}

			world.removeBlockState(abovePos);
		});
	}

	/**
	 * Attempts to spawn a melon or pumpkin adjacent to a fully grown stem.
	 *
	 * <p>
	 * This method randomly picks a cardinal direction around the stem and checks if
	 * a fruit can be placed there. It ensures the target block is air and the block
	 * below is solid (e.g. farmland, dirt, grass block)— just like in vanilla. If
	 * conditions are valid, it places the fruit block and replaces the stem with
	 * its attached variant facing the fruit.
	 * </p>
	 *
	 * @param stemPos   the position of the melon/pumpkin stem
	 * @param stemState the current block state of the stem (must be max age)
	 * @param world     the custom world where blocks are managed
	 */
	private void trySpawnFruit(@NotNull Pos3 stemPos, @NotNull CustomBlockState stemState,
			@NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		List<BlockFace> directions = new ArrayList<>(Arrays.asList(FACES));
		Collections.shuffle(directions); // Randomize direction order

		// Check all adjacent blocks to see if a fruit already exists
		List<CompletableFuture<Optional<CustomBlockState>>> checkFutures = Arrays.stream(FACES)
				.map(face -> world.getBlockState(stemPos.offset(face))).toList();

		CompletableFuture.allOf(checkFutures.toArray(CompletableFuture[]::new)).thenRun(() -> {
			for (int i = 0; i < FACES.length; i++) {
				Optional<CustomBlockState> neighbor = checkFutures.get(i).join();
				if (neighbor.isPresent()) {
					String key = neighbor.get().type().type().key().asString().toLowerCase();
					if (Set.of("minecraft:melon", "hellblock:melon", "minecraft:pumpkin", "hellblock:pumpkin")
							.contains(key)) {
						return; // Fruit already present — abort
					}
				}
			}

			// Try each direction one-by-one to place the fruit
			for (BlockFace face : directions) {
				Pos3 targetPos = stemPos.offset(face);

				world.getBlockState(targetPos).thenCompose(targetOpt -> {
					if (targetOpt.isPresent())
						return CompletableFuture.completedFuture(false); // Block not empty

					Pos3 soilPos = targetPos.down();
					return world.getBlockState(soilPos).thenApply(soilOpt -> {
						if (soilOpt.isEmpty())
							return false;

						String soilKey = soilOpt.get().type().type().key().asString().toLowerCase();
						if (!VALID_FRUIT_SOIL_KEYS.contains(soilKey))
							return false;

						// Choose fruit + attached stem block type
						CustomBlock fruit = (stemState.type() instanceof CustomMelonStemBlock) ? CustomBlockTypes.MELON
								: CustomBlockTypes.PUMPKIN;

						CustomBlock attached = (stemState.type() instanceof CustomMelonStemBlock)
								? CustomBlockTypes.MELON_ATTACHED_STEM
								: CustomBlockTypes.PUMPKIN_ATTACHED_STEM;

						CustomBlockState fruitState = fruit.createBlockState();
						CustomBlockState newStemState = attached.createBlockState();

						if (attached instanceof Growable growable)
							growable.setAge(newStemState, growable.getMaxAge(newStemState));

						if (attached instanceof com.swiftlicious.hellblock.world.block.Directional directional)
							directional.setFacing(newStemState, face);

						Map<Pos3, CustomBlockState> updates = new HashMap<>();
						updates.put(targetPos, fruitState);
						updates.put(stemPos, newStemState);

						world.updateBlockStates(updates);
						return true; // Success
					});
				}).thenAccept(success -> {
					if (success)
						return; // First valid direction succeeded — stop trying others
				});

				break; // Don't launch more attempts — wait for async result
			}
		});
	}

	/**
	 * Infers the appropriate crop drop for a farmland-grown block based on its
	 * custom block key and its current growth state (NBT data).
	 *
	 * @param key   the key of the CustomBlock (e.g., CustomBlockTypes.WHEAT)
	 * @param state the current CustomBlockState containing NBT data like "age"
	 * @return the wrapped item to drop, or null if the block type doesn't drop
	 *         anything
	 */
	@Nullable
	private List<Item<ItemStack>> inferDropFromKey(@NotNull CustomBlock key, @NotNull CustomBlockState state) {
		if (!(key instanceof Growable))
			return null;

		int age = 0;
		BinaryTag tag = state.get("age");
		if (tag instanceof IntBinaryTag ageTag) {
			age = ageTag.intValue();
		}

		List<Item<ItemStack>> drops = new ArrayList<>();
		String id = key.type().key().asString().toLowerCase();

		switch (id) {
		case "hellblock:wheat":
		case "minecraft:wheat": {
			if (age >= 7) {
				// Fully grown
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.WHEAT)));
				int seedAmount = RandomUtils.generateRandomInt(0, 3); // 0–3
				if (seedAmount > 0)
					drops.add(instance.getItemManager().wrap(new ItemStack(Material.WHEAT_SEEDS, seedAmount)));
			} else {
				// Not mature: always 1 seed
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.WHEAT_SEEDS)));
			}
			break;
		}

		case "hellblock:carrots":
		case "minecraft:carrots": {
			if (age >= 7) {
				int amount = 1 + RandomUtils.generateRandomInt(0, 3); // 1–4
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.CARROT, amount)));
			} else {
				// Not mature: 1 carrot
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.CARROT)));
			}
			break;
		}

		case "hellblock:potatoes":
		case "minecraft:potatoes": {
			if (age >= 7) {
				int amount = 1 + RandomUtils.generateRandomInt(0, 3); // 1–4
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.POTATO, amount)));
				// Optional: rare chance for a poisonous potato (≈2%)
				if (RandomUtils.roll(2))
					drops.add(instance.getItemManager().wrap(new ItemStack(Material.POISONOUS_POTATO)));
			} else {
				// Not mature: 1 potato
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.POTATO)));
			}
			break;
		}

		case "hellblock:beetroots":
		case "minecraft:beetroots": {
			if (age >= 3) {
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.BEETROOT)));
				int seedAmount = RandomUtils.generateRandomInt(0, 3); // 0–3
				if (seedAmount > 0)
					drops.add(instance.getItemManager().wrap(new ItemStack(Material.BEETROOT_SEEDS, seedAmount)));
			} else {
				// Not mature: 1 beetroot seed
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.BEETROOT_SEEDS)));
			}
			break;
		}

		case "hellblock:melon_stem":
		case "hellblock:attached_melon_stem":
		case "minecraft:melon_stem":
		case "minecraft:attached_melon_stem": {
			if (age >= 7 || RandomUtils.roll(50)) {
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.MELON_SEEDS)));
			}
			break;
		}

		case "hellblock:pumpkin_stem":
		case "hellblock:attached_pumpkin_stem":
		case "minecraft:pumpkin_stem":
		case "minecraft:attached_pumpkin_stem": {
			if (age >= 7 || RandomUtils.roll(50)) {
				drops.add(instance.getItemManager().wrap(new ItemStack(Material.PUMPKIN_SEEDS)));
			}
			break;
		}

		default:
			return null;
		}

		return drops.isEmpty() ? null : drops;
	}

	/**
	 * Attempts to grow glowstone trees during lava rain if the sapling is on soul
	 * sand and other conditions are met. Applies crop growth bonuses as well.
	 *
	 * <p>
	 * Glowstone saplings only grow during lava rain; nearby lava does not affect
	 * them. This method checks lava rain at each sapling's position individually.
	 * </p>
	 *
	 * @param world    the Hellblock world
	 * @param islandId the island ID owning the sapling
	 */
	public void handleGlowstoneTreeGrowth(@NotNull HellblockWorld<?> world, int islandId) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		getFarmBlocksByIslandId(world, islandId).thenAccept(farmBlocks -> {
			if (farmBlocks == null || farmBlocks.isEmpty())
				return;

			for (PositionedBlock block : farmBlocks) {
				CustomBlockState state = block.state();
				String key = state.type().type().key().asString().toLowerCase();

				if (!(state.type() instanceof CustomSaplingBlock))
					continue;

				Pos3 below = block.pos().down();

				world.getBlockState(below).thenCompose(belowOpt -> {
					if (belowOpt.isEmpty()
							|| !SOUL_SAND_KEY.contains(belowOpt.get().type().type().value().toLowerCase()))
						return CompletableFuture.completedFuture(false);

					if (!isLavaRainingAt(islandId, block.pos().toLocation(world.bukkitWorld())))
						return CompletableFuture.completedFuture(false);

					return CompletableFuture.completedFuture(true);
				}).thenAccept(valid -> {
					if (!valid)
						return;

					withCropGrowthBonusIfValid(block, world, bonus -> {
						int baseChance = getBaseChance(key, true, false);
						int finalChance = (int) Math.min(100, baseChance + bonus);

						if (RandomUtils.roll(finalChance)
								&& instance.getGlowstoneTreeHandler().canGrow(block.pos(), world)) {
							CustomTreeGrowContext context = new CustomTreeGrowContext(TreeType.TREE, block.pos(),
									world.worldName());

							instance.getIslandGenerator()
									.generateHellblockGlowstoneTree(world, block.pos().toLocation(world.bukkitWorld()),
											false)
									.thenCompose(v -> instance.getStorageManager().getOfflineUserDataByIslandId(
											islandId, instance.getConfigManager().lockData()))
									.thenAccept(userOpt -> userOpt.ifPresent(userData -> {
										UUID ownerUUID = userData.getHellblockData().getOwnerUUID();
										if (ownerUUID != null && instance.getCooldownManager()
												.shouldUpdateActivity(userData.getUUID(), 5000)) {
											userData.getHellblockData().updateLastIslandActivity();
										}
										instance.getChallengeManager().handleChallengeProgression(userData,
												ActionType.GROW, context);
									}));
						}
					});
				});
			}
		});
	}

	/**
	 * Handles the growth behavior of Nether Wart when exposed to lava or lava rain.
	 *
	 * <p>
	 * If lava is nearby or lava rain is active, applies an enhanced growth chance.
	 * Otherwise, grows normally at its vanilla speed. Nether wart does not require
	 * lava to grow but benefits from it.
	 * </p>
	 *
	 * @param block the positioned Nether Wart block
	 * @param world the Hellblock world context
	 */
	private void handleNetherWart(@NotNull PositionedBlock block, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		CustomBlockState state = block.state();
		if (!(state.type() instanceof Growable))
			return;

		instance.getIslandManager().resolveIslandId(world, block.pos()).thenAcceptAsync(optIslandId -> {
			if (optIslandId.isEmpty())
				return;

			int islandId = optIslandId.get();
			boolean lavaRain = isLavaRainingAt(islandId, block.pos().toLocation(world.bukkitWorld()));
			Pos3 soilPos = block.pos().down();

			// Compose: get soil state -> then check for lava
			world.getBlockState(soilPos).thenCompose(soilOpt -> {
				PositionedBlock soil = new PositionedBlock(soilPos, soilOpt.orElse(null));
				return checkForLavaAroundFarm(soil, world);
			}).thenAccept(lavaNearby -> {
				int delay = RandomUtils.generateRandomInt(15, 30);

				world.scheduler().asyncLater(() -> {
					world.getBlockState(block.pos()).thenAccept(currentOpt -> {
						if (currentOpt.isEmpty())
							return;

						CustomBlockState current = currentOpt.get();
						if (!(current.type() instanceof Growable wart))
							return;

						int age = wart.getAge(current);
						int max = wart.getMaxAge(current);
						if (age >= max)
							return;

						String key = current.type().type().key().asString();
						int baseChance = getBaseChance(key, lavaRain, lavaNearby);

						withCropGrowthBonusIfValid(block, world, bonus -> {
							int finalChance = (int) Math.min(100, baseChance + bonus);
							if (RandomUtils.roll(finalChance)) {
								wart.setAge(current, age + 1);
								world.updateBlockState(block.pos(), current);
							}
						});
					});
				}, delay, TimeUnit.SECONDS);
			});
		}, instance.getScheduler()::executeSync);
	}

	/**
	 * Handles cocoa bean growth when exposed to lava rain.
	 *
	 * <p>
	 * Cocoa pods must be attached to a valid stem block (e.g., warped or crimson).
	 * Growth occurs only when lava rain is active. Lava near the base does not
	 * influence cocoa growth. The pod will grow through its stages and eventually
	 * mature.
	 * </p>
	 *
	 * @param block the positioned cocoa bean block
	 * @param world the Hellblock world context
	 */
	private void handleCocoaBeans(@NotNull PositionedBlock block, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		CustomBlockState state = block.state();

		if (!(state.type() instanceof Growable growable)
				|| !(state.type() instanceof com.swiftlicious.hellblock.world.block.Directional directional)) {
			return;
		}

		BlockFace facing = directional.getFacing(state);
		Pos3 attachedPos = block.pos().offset(facing.getOppositeFace());

		world.getBlockState(attachedPos).thenCompose(attachedOpt -> {
			if (attachedOpt.isEmpty())
				return CompletableFuture.completedFuture(null);

			String attachedKey = attachedOpt.get().type().type().value().toLowerCase();
			if (!COCOA_STEM_KEYS.contains(attachedKey))
				return CompletableFuture.completedFuture(null);

			return instance.getIslandManager().resolveIslandId(world, block.pos());
		}).thenAcceptAsync(optIslandId -> {
			if (optIslandId == null || optIslandId.isEmpty())
				return;

			int islandId = optIslandId.get();
			boolean lavaRain = isLavaRainingAt(islandId, block.pos().toLocation(world.bukkitWorld()));
			int baseChance = getBaseChance(state.type().type().key().asString(), lavaRain, false);

			withCropGrowthBonusIfValid(block, world, bonus -> {
				int finalChance = (int) Math.min(100, baseChance + bonus);
				int age = growable.getAge(state);
				int maxAge = growable.getMaxAge(state);

				if (age < maxAge && RandomUtils.roll(finalChance)) {
					growable.setAge(state, age + 1);
					world.updateBlockState(block.pos(), state);
				}
			});
		}, instance.getScheduler()::executeSync);
	}

	/**
	 * Handles sugar cane growth with lava rain or nearby lava.
	 *
	 * <p>
	 * Sugar cane grows vertically up to three blocks high, but it requires lava to
	 * be present near the soil block below it in order to grow at all. If lava is
	 * nearby and/or lava rain is active, its growth rate increases. Lava is
	 * strictly required for any growth to occur.
	 * </p>
	 *
	 * @param block the base sugar cane block
	 * @param world the Hellblock world context
	 */
	private void handleSugarCane(@NotNull PositionedBlock block, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		getCropHeight(block.pos(), SUGAR_CANE_KEYS, world).thenCompose(height -> {
			if (height >= 3)
				return CompletableFuture.completedFuture(null);

			return instance.getIslandManager().resolveIslandId(world, block.pos()).thenComposeAsync(optIslandId -> {
				if (optIslandId.isEmpty())
					return CompletableFuture.completedFuture(null);

				int islandId = optIslandId.get();
				boolean lavaRain = isLavaRainingAt(islandId, block.pos().toLocation(world.bukkitWorld()));

				Pos3 belowPos = block.pos().down();
				return world.getBlockState(belowPos).thenCompose(belowOpt -> {
					if (belowOpt.isEmpty())
						return CompletableFuture.completedFuture(null);

					PositionedBlock below = new PositionedBlock(belowPos, belowOpt.get());
					return checkForLavaAroundFarm(below, world).thenAccept(lavaNearby -> {
						if (!lavaNearby)
							return;

						int delay = RandomUtils.generateRandomInt(20, 35);
						world.scheduler().asyncLater(() -> {
							getCropTop(block.pos(), SUGAR_CANE_KEYS, world).thenAccept(top -> {
								Pos3 growTarget = top.up();
								world.getBlockState(growTarget).thenAccept(targetStateOpt -> {
									if (targetStateOpt.isPresent() && !targetStateOpt.get().isAir())
										return;

									int baseChance = getBaseChance(block.state().type().type().key().asString(),
											lavaRain, lavaNearby);

									withCropGrowthBonusIfValid(block, world, bonus -> {
										int finalChance = (int) Math.min(100, baseChance + bonus);
										if (RandomUtils.roll(finalChance)) {
											world.updateBlockState(growTarget, block.state());
										}
									});
								});
							});
						}, delay, TimeUnit.SECONDS);
					});
				});
			}, instance.getScheduler()::executeSync);
		});
	}

	/**
	 * Checks whether the soil beneath a sugar cane plant is adjacent to lava.
	 *
	 * @param soil  the positioned block representing the sugar cane’s soil
	 * @param world the Hellblock world context
	 * @return true if lava is found adjacent to the soil block; false otherwise
	 */
	public CompletableFuture<Boolean> checkForLavaAroundSugarCane(@NotNull PositionedBlock soil,
			@NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return CompletableFuture.completedFuture(false);

		CustomBlockState state = soil.state();
		if (state == null)
			return CompletableFuture.completedFuture(false);

		String typeKey = state.type().type().key().asString().toLowerCase();
		if (!VALID_SUGAR_CANE_SOIL_KEYS.contains(typeKey)) {
			return CompletableFuture.completedFuture(false);
		}

		List<CompletableFuture<Boolean>> checks = new ArrayList<>();

		for (BlockFace face : FACES) {
			Pos3 neighborPos = soil.pos().offset(face);
			CompletableFuture<Boolean> check = world.getBlockState(neighborPos).thenApply(optState -> {
				if (optState.isEmpty())
					return false;

				String key = optState.get().type().type().key().asString().toLowerCase();
				return LAVA_KEY.contains(key);
			});
			checks.add(check);
		}

		// Combine all futures and return true if any result is true
		return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
				.thenApply(v -> checks.stream().anyMatch(future -> future.join()));
	}

	/**
	 * Handles mushroom growth logic under lava rain or lava proximity.
	 *
	 * <p>
	 * Supports both red and brown mushrooms (vanilla and hellblock variants).
	 * Mushrooms grow and spread at a vanilla rate by default, but if exposed to
	 * lava rain or nearby lava, they gain an increased chance to spread into nearby
	 * valid soil blocks. Lava is not required for growth.
	 * </p>
	 *
	 * @param block the positioned mushroom block
	 * @param world the Hellblock world context
	 */
	private void handleMushrooms(@NotNull PositionedBlock block, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		CustomBlockState state = block.state();
		if (state == null)
			return;

		String key = state.type().type().key().asString().toLowerCase();
		if (!Set.of("minecraft:red_mushroom", "hellblock:red_mushroom", "minecraft:brown_mushroom",
				"hellblock:brown_mushroom").contains(key)) {
			return;
		}

		instance.getIslandManager().resolveIslandId(world, block.pos()).thenComposeAsync(optIslandId -> {
			if (optIslandId.isEmpty())
				return CompletableFuture.completedFuture(null);

			int islandId = optIslandId.get();
			boolean lavaRain = isLavaRainingAt(islandId, block.pos().toLocation(world.bukkitWorld()));

			return checkForLavaAroundFarm(block, world).thenAccept(lavaNearby -> {
				int delay = RandomUtils.generateRandomInt(20, 40);
				int baseChance = getBaseChance(state.type().type().key().asString(), lavaRain, lavaNearby);

				withCropGrowthBonusIfValid(block, world, bonus -> {
					int finalChance = (int) Math.min(100, baseChance + bonus);
					world.scheduler().asyncLater(() -> {
						if (RandomUtils.roll(finalChance)) {
							trySpreadMushroom(block, world);
						}
					}, delay, TimeUnit.SECONDS);
				});
			});
		}, instance.getScheduler()::executeSync);
	}

	/**
	 * Attempts to spread a mushroom from a source block into a nearby valid block.
	 * Only spreads if a valid mushroom soil is found adjacent to the source.
	 *
	 * @param source the current mushroom block
	 * @param world  the world in which the mushroom exists
	 */
	private void trySpreadMushroom(@NotNull PositionedBlock source, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		List<BlockFace> faces = Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
				BlockFace.UP, BlockFace.DOWN);
		Collections.shuffle(faces);

		// Process faces asynchronously in sequence
		CompletableFuture.runAsync(() -> {
			for (BlockFace face : faces) {
				Pos3 targetPos = source.pos().offset(face);

				CustomBlockState sourceState = source.state();
				if (sourceState == null || sourceState.isAir())
					continue;

				CompletableFuture<Optional<CustomBlockState>> targetFuture = world.getBlockState(targetPos);
				CompletableFuture<Optional<CustomBlockState>> belowFuture = world.getBlockState(targetPos.down());

				CompletableFuture.allOf(targetFuture, belowFuture).thenAccept(v -> {
					Optional<CustomBlockState> targetState = targetFuture.join();
					Optional<CustomBlockState> belowState = belowFuture.join();

					if (targetState.isPresent() && !targetState.get().isAir())
						return;

					if (belowState.isEmpty())
						return;

					String soilKey = belowState.get().type().type().key().asString().toLowerCase();
					if (VALID_MUSHROOM_SOIL_KEYS.contains(soilKey)) {
						world.updateBlockState(targetPos, sourceState).thenRun(() -> lavaGrownMushrooms.add(targetPos));
					}
				}).join(); // Wait for both before trying next face
				break; // Stop after first valid spread
			}
		});
	}

	/**
	 * Checks whether the given mushroom block was generated or spread due to
	 * lava-related conditions.
	 *
	 * <p>
	 * This identifies mushrooms that have grown naturally as part of lava rain or
	 * lava proximity mechanics, as opposed to player-placed mushrooms.
	 * </p>
	 *
	 * @param block the positioned mushroom block to check
	 * @return true if the mushroom was grown through lava-based spread mechanics;
	 *         false otherwise
	 */
	public boolean isLavaGrownMushroom(@NotNull PositionedBlock block) {
		return lavaGrownMushrooms.contains(block.pos());
	}

	/**
	 * Handles cactus growth when exposed to lava rain or nearby lava.
	 *
	 * <p>
	 * Cactus grows vertically up to three blocks tall. It grows at a normal rate by
	 * default but receives a growth speed boost when exposed to lava rain or when
	 * lava is near its base. Lava is not required for growth but improves it.
	 * </p>
	 *
	 * @param base  the base cactus block
	 * @param world the Hellblock world context
	 */
	private void handleCactus(@NotNull PositionedBlock base, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		getCropHeight(base.pos(), CACTUS_KEYS, world).thenCompose(height -> {
			if (height >= 3) {
				return CompletableFuture.completedFuture(null);
			}

			return instance.getIslandManager().resolveIslandId(world, base.pos()).thenComposeAsync(optIslandId -> {
				if (optIslandId.isEmpty()) {
					return CompletableFuture.completedFuture(null);
				}

				int islandId = optIslandId.get();
				boolean lavaRain = isLavaRainingAt(islandId, base.pos().toLocation(world.bukkitWorld()));

				return checkForLavaAroundCactus(base, world).thenAccept(lavaNearby -> {
					int baseChance = getBaseChance(base.state().type().type().key().asString(), lavaRain, lavaNearby);

					withCropGrowthBonusIfValid(base, world, bonus -> {
						int finalChance = (int) Math.min(100, baseChance + bonus);
						if (!RandomUtils.roll(finalChance))
							return;

						getCropTop(base.pos(), CACTUS_KEYS, world).thenAccept(top -> {
							Pos3 growTarget = top.up();
							world.getBlockState(growTarget).thenAccept(stateOpt -> {
								if (stateOpt.isEmpty() || stateOpt.get().isAir()) {
									world.updateBlockState(growTarget, base.state());
								}
							});
						});
					});
				});
			}, instance.getScheduler()::executeSync);
		});
	}

	/**
	 * Checks for the presence of lava adjacent to or beneath a cactus block.
	 *
	 * @param base  the base cactus block
	 * @param world the Hellblock world context
	 * @return true if lava is detected near the cactus base; false otherwise
	 */
	public CompletableFuture<Boolean> checkForLavaAroundCactus(@NotNull PositionedBlock base,
			@NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return CompletableFuture.completedFuture(false);

		Pos3 below = base.pos().down();
		List<CompletableFuture<Optional<CustomBlockState>>> neighborFutures = new ArrayList<>();

		for (BlockFace face : FACES) {
			Pos3 adjacent = below.offset(face);
			neighborFutures.add(world.getBlockState(adjacent));
		}

		return CompletableFuture.allOf(neighborFutures.toArray(CompletableFuture[]::new)).thenApply(v -> {
			for (CompletableFuture<Optional<CustomBlockState>> future : neighborFutures) {
				Optional<CustomBlockState> stateOpt = future.join(); // safe after allOf
				if (stateOpt.isPresent()) {
					String key = stateOpt.get().type().type().value().toLowerCase();
					if (LAVA_KEY.contains(key)) {
						return true;
					}
				}
			}
			return false;
		});
	}

	/**
	 * Handles sweet berry bush growth when exposed to lava rain.
	 *
	 * <p>
	 * Sweet berry bushes grow only at their vanilla rate unless lava rain is
	 * active, which increases their growth chance. Lava near the base does not
	 * influence their growth, and lava rain is the only environmental factor that
	 * provides a growth bonus.
	 * </p>
	 *
	 * @param bush  the positioned berry bush block
	 * @param world the Hellblock world context
	 */
	private void handleSweetBerryBush(@NotNull PositionedBlock bush, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		instance.getIslandManager().resolveIslandId(world, bush.pos()).thenComposeAsync(optIslandId -> {
			if (optIslandId.isEmpty())
				return CompletableFuture.completedFuture(null);

			int islandId = optIslandId.get();
			String key = bush.state().type().type().key().asString();
			boolean lavaRain = isLavaRainingAt(islandId, bush.pos().toLocation(world.bukkitWorld()));
			int baseChance = getBaseChance(key, lavaRain, false); // lavaNearby always false for sweet berry

			withCropGrowthBonusIfValid(bush, world, bonus -> {
				int finalChance = (int) Math.min(100, baseChance + bonus);

				CustomBlock type = bush.state().type();
				if (!(type instanceof Growable growable))
					return;

				Pos3 belowPos = bush.pos().down();
				world.getBlockState(belowPos).thenAccept(belowStateOpt -> {
					if (belowStateOpt.isEmpty())
						return;

					String soilKey = belowStateOpt.get().type().type().key().asString().toLowerCase();
					if (!VALID_SWEET_BERRY_SOIL_KEYS.contains(soilKey))
						return;

					int age = growable.getAge(bush.state());
					int maxAge = growable.getMaxAge(bush.state());

					if (age < maxAge && RandomUtils.roll(finalChance)) {
						growable.setAge(bush.state(), age + 1);
						world.updateBlockState(bush.pos(), bush.state());
					}
				});
			});

			return CompletableFuture.completedFuture(null);
		}, instance.getScheduler()::executeSync);
	}

	/**
	 * Checks whether the block beneath a sweet berry bush is near lava.
	 *
	 * <p>
	 * Reuses the general
	 * {@link #checkForLavaAroundFarm(PositionedBlock, HellblockWorld)} method to
	 * determine proximity to lava sources.
	 * </p>
	 *
	 * @param bush  the positioned berry bush block
	 * @param world the Hellblock world context
	 * @return true if lava is nearby; false otherwise
	 */
	public CompletableFuture<Boolean> checkForLavaAroundBerryBush(@NotNull PositionedBlock bush,
			@NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return CompletableFuture.completedFuture(false);

		Pos3 below = bush.pos().down();

		return world.getBlockState(below).thenCompose(belowState -> {
			if (belowState.isEmpty())
				return CompletableFuture.completedFuture(false);

			PositionedBlock belowBlock = new PositionedBlock(below, belowState.get());
			return checkForLavaAroundFarm(belowBlock, world); // reused async version
		});
	}

	/**
	 * Handles bamboo growth under lava rain or nearby lava exposure.
	 *
	 * <p>
	 * Bamboo grows vertically up to three blocks high. It grows at vanilla speed by
	 * default, and gains an increased growth chance if lava rain is active or lava
	 * is detected near its base. Lava is not required for growth.
	 * </p>
	 *
	 * @param base  the base bamboo block
	 * @param world the Hellblock world context
	 */
	private void handleBamboo(@NotNull PositionedBlock base, @NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return;

		getCropHeight(base.pos(), BAMBOO_KEYS, world).thenCompose(height -> {
			if (height >= 3)
				return CompletableFuture.completedFuture(null);

			return instance.getIslandManager().resolveIslandId(world, base.pos()).thenComposeAsync(optIslandId -> {
				if (optIslandId.isEmpty())
					return CompletableFuture.completedFuture(null);

				int islandId = optIslandId.get();
				boolean lavaRain = isLavaRainingAt(islandId, base.pos().toLocation(world.bukkitWorld()));

				return checkForLavaAroundBamboo(base, world).thenAccept(lavaNearby -> {
					int baseChance = getBaseChance(base.state().type().type().key().asString(), lavaRain, lavaNearby);

					withCropGrowthBonusIfValid(base, world, bonus -> {
						int finalChance = (int) Math.min(100, baseChance + bonus);
						if (!RandomUtils.roll(finalChance))
							return;

						getCropTop(base.pos(), BAMBOO_KEYS, world).thenAccept(top -> {
							Pos3 growTarget = top.up();
							world.getBlockState(growTarget).thenAccept(growTargetState -> {
								if (growTargetState.isEmpty() || growTargetState.get().isAir()) {
									world.updateBlockState(growTarget, base.state());
								}
							});
						});
					});
				});
			}, instance.getScheduler()::executeSync);
		});
	}

	/**
	 * Checks whether the block beneath a bamboo plant is adjacent to lava.
	 *
	 * @param base  the base bamboo block
	 * @param world the Hellblock world context
	 * @return true if lava is found nearby; false otherwise
	 */
	public CompletableFuture<Boolean> checkForLavaAroundBamboo(@NotNull PositionedBlock base,
			@NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return CompletableFuture.completedFuture(false);

		Pos3 below = base.pos().down();
		List<CompletableFuture<Optional<CustomBlockState>>> futures = new ArrayList<>();

		for (BlockFace face : FACES) {
			Pos3 adjacent = below.offset(face);
			futures.add(world.getBlockState(adjacent));
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
				.thenApply(v -> futures.stream().map(CompletableFuture::join).filter(Optional::isPresent)
						.map(Optional::get).map(state -> state.type().type().value().toLowerCase())
						.anyMatch(LAVA_KEY::contains));
	}

	/**
	 * Finds the bottommost block of a vertically stacked crop (e.g., sugar cane or
	 * bamboo).
	 *
	 * @param start    the starting position of the crop
	 * @param cropKeys a set of valid crop block keys
	 * @param world    the Hellblock world context
	 * @return the position of the crop’s base block
	 */
	@Nullable
	public CompletableFuture<Pos3> getCropBase(@NotNull Pos3 start, @NotNull Set<String> cropKeys,
			@NotNull HellblockWorld<?> world) {
		CompletableFuture<Pos3> future = CompletableFuture.completedFuture(start);

		return future.thenCompose(current -> {
			CompletableFuture<Pos3> loop = new CompletableFuture<>();

			BiFunction<Pos3, CompletableFuture<Pos3>, Void> recurse = new BiFunction<>() {
				@Override
				public Void apply(Pos3 pos, CompletableFuture<Pos3> result) {
					Pos3 below = pos.down();
					world.getBlockState(below).thenAccept(opt -> {
						if (opt.isEmpty()) {
							result.complete(pos);
						} else {
							String key = opt.get().type().type().value().toLowerCase();
							if (!cropKeys.contains(key)) {
								result.complete(pos);
							} else {
								apply(below, result);
							}
						}
					});
					return null;
				}
			};

			recurse.apply(start, loop);
			return loop;
		});
	}

	/**
	 * Finds the topmost block of a vertically stacked crop (e.g., sugar cane,
	 * cactus, bamboo).
	 *
	 * @param base     the base position of the crop
	 * @param cropKeys a set of valid crop block keys
	 * @param world    the Hellblock world context
	 * @return the position of the top crop block
	 */
	@Nullable
	public CompletableFuture<Pos3> getCropTop(@NotNull Pos3 base, @NotNull Set<String> cropKeys,
			@NotNull HellblockWorld<?> world) {
		CompletableFuture<Pos3> future = CompletableFuture.completedFuture(base);

		return future.thenCompose(current -> {
			CompletableFuture<Pos3> loop = new CompletableFuture<>();

			BiFunction<Pos3, CompletableFuture<Pos3>, Void> recurse = new BiFunction<>() {
				@Override
				public Void apply(Pos3 pos, CompletableFuture<Pos3> result) {
					Pos3 above = pos.up();
					world.getBlockState(above).thenAccept(opt -> {
						if (opt.isEmpty()) {
							result.complete(pos);
						} else {
							String key = opt.get().type().type().value().toLowerCase();
							if (!cropKeys.contains(key)) {
								result.complete(pos);
							} else {
								apply(above, result);
							}
						}
					});
					return null;
				}
			};

			recurse.apply(base, loop);
			return loop;
		});
	}

	/**
	 * Calculates the total height of a vertically growing crop.
	 *
	 * <p>
	 * Counts consecutive matching crop blocks above the base until an invalid block
	 * is found.
	 * </p>
	 *
	 * @param base     the base position of the crop
	 * @param cropKeys a set of valid crop block keys
	 * @param world    the Hellblock world context
	 * @return the number of crop blocks stacked vertically
	 */
	@NotNull
	public CompletableFuture<Integer> getCropHeight(@NotNull Pos3 base, @NotNull Set<String> cropKeys,
			@NotNull HellblockWorld<?> world) {
		AtomicInteger height = new AtomicInteger(1);
		CompletableFuture<Integer> result = new CompletableFuture<>();

		class CropHeightCounter {
			void count(Pos3 current) {
				Pos3 above = current.up();
				world.getBlockState(above).thenAccept(opt -> {
					if (opt.isEmpty()) {
						result.complete(height.get());
					} else {
						String key = opt.get().type().type().key().asString().toLowerCase();
						if (!cropKeys.contains(key)) {
							result.complete(height.get());
						} else {
							height.incrementAndGet();
							count(above);
						}
					}
				});
			}
		}

		new CropHeightCounter().count(base);
		return result;
	}

	/**
	 * Returns the maximum possible moisture value for a farmland block state.
	 *
	 * @param state the block state to evaluate
	 * @return the maximum moisture level, or 0 if the block is not farmland
	 */
	public int getMaxMoisture(@Nullable CustomBlockState state) {
		if (state == null || state.isAir())
			return 0;

		CustomBlock type = state.type();
		if (type instanceof MoistureHolder moisture) {
			return moisture.getMaxMoisture(state);
		}

		return 0;
	}

	/**
	 * Returns the current moisture value of a farmland block.
	 *
	 * @param state the block state to evaluate
	 * @return the current moisture level, or 0 if the block is not farmland
	 */
	public int getCurrentMoisture(@Nullable CustomBlockState state) {
		if (state == null || state.isAir())
			return 0;

		CustomBlock type = state.type();
		if (type instanceof MoistureHolder moisture) {
			return moisture.getMoisture(state);
		}

		return 0;
	}

	/**
	 * Returns the maximum growth stage value for a crop block.
	 *
	 * @param state the block state to evaluate
	 * @return the crop’s maximum growth age, or 0 if not applicable
	 */
	public int getMaxGrowthStage(@Nullable CustomBlockState state) {
		if (state == null || state.isAir())
			return 0;

		CustomBlock type = state.type();
		if (type instanceof Growable growable) {
			return growable.getMaxAge(state);
		}

		return 0;
	}

	/**
	 * Returns the current growth stage of a crop block.
	 *
	 * @param state the block state to evaluate
	 * @return the crop’s current growth age, or 0 if not applicable
	 */
	public int getCurrentGrowthStage(@Nullable CustomBlockState state) {
		if (state == null || state.isAir())
			return 0;

		CustomBlock type = state.type();
		if (type instanceof Growable growable) {
			return growable.getAge(state);
		}

		return 0;
	}

	/**
	 * Increments the growth stage of a crop block by one, up to its maximum age.
	 *
	 * @param state the block state to update
	 */
	public void updateGrowthStage(@Nullable CustomBlockState state) {
		if (state == null || state.isAir()) {
			return;
		}

		CustomBlock type = state.type();
		if (type instanceof Growable growable) {
			int current = growable.getAge(state);
			int max = growable.getMaxAge(state);
			growable.setAge(state, Math.min(current + 1, max));
		}
	}

	/**
	 * Checks for the presence of lava blocks within a configurable radius around
	 * farmland.
	 *
	 * <p>
	 * Used to determine whether farmland should remain hydrated or crops should
	 * benefit from lava-based growth bonuses.
	 * </p>
	 *
	 * @param block the farmland block to check
	 * @param world the Hellblock world context
	 * @return true if any lava is detected nearby; false otherwise
	 */
	public CompletableFuture<Boolean> checkForLavaAroundFarm(@NotNull PositionedBlock block,
			@NotNull HellblockWorld<?> world) {
		if (!isInCorrectWorld(world.bukkitWorld()))
			return CompletableFuture.completedFuture(false);

		if (block.state() == null || block.state().isAir())
			return CompletableFuture.completedFuture(false);

		if (!(block.state().type() instanceof MoistureHolder))
			return CompletableFuture.completedFuture(false);

		Pos3 origin = block.pos();
		final int centerX = origin.x();
		final int centerY = origin.y();
		final int centerZ = origin.z();

		List<CompletableFuture<Optional<CustomBlockState>>> futures = new ArrayList<>();

		for (int x = centerX - FARM_BLOCK_CHECK_RADIUS; x <= centerX + FARM_BLOCK_CHECK_RADIUS; x++) {
			for (int y = centerY - 1; y <= centerY; y++) {
				for (int z = centerZ - FARM_BLOCK_CHECK_RADIUS; z <= centerZ + FARM_BLOCK_CHECK_RADIUS; z++) {
					Pos3 nearby = new Pos3(x, y, z);
					futures.add(world.getBlockState(nearby));
				}
			}
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
				.thenApply(v -> futures.stream().map(CompletableFuture::join).filter(Optional::isPresent)
						.map(Optional::get).map(state -> state.type().type().value().toLowerCase())
						.anyMatch(LAVA_KEY::contains));
	}

	/**
	 * Asynchronously retrieves all farm-related blocks belonging to a specific
	 * island.
	 *
	 * <p>
	 * Uses the island’s bounding box to search for farmland, crops, saplings, and
	 * vertical crops, returning a cached result if available.
	 * </p>
	 *
	 * @param world    the Hellblock world instance
	 * @param islandId the ID of the island
	 * @return a CompletableFuture containing the set of farm blocks
	 */
	@NotNull
	private CompletableFuture<Set<PositionedBlock>> getFarmBlocksByIslandId(@NotNull HellblockWorld<?> world,
			int islandId) {
		return instance.getStorageManager()
				.getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData()).thenCompose(optUser -> {
					if (optUser.isEmpty())
						return CompletableFuture.completedFuture(Collections.emptySet());

					HellblockData data = optUser.get().getHellblockData();
					BoundingBox bounds = data.getBoundingBox();
					if (bounds == null)
						return CompletableFuture.completedFuture(Collections.emptySet());

					Set<PositionedBlock> cached = farmCache.getIfPresent(islandId);
					if (cached != null && !cached.isEmpty())
						return CompletableFuture.completedFuture(cached);

					// Fully async collection now
					return collectFarmBlocks(world, bounds).thenApply(farmBlocks -> {
						farmCache.put(islandId, farmBlocks);
						return farmBlocks;
					});
				});
	}

	/**
	 * Scans a world region within an island’s bounding box to identify all
	 * farm-related blocks.
	 *
	 * <p>
	 * Includes farmland, crops, saplings, and vertical crops such as sugar cane or
	 * cactus.
	 * </p>
	 *
	 * @param world  the Hellblock world context
	 * @param bounds the bounding box of the island
	 * @return a set of all detected farm blocks within the island bounds
	 */
	@NotNull
	public CompletableFuture<Set<PositionedBlock>> collectFarmBlocks(@NotNull HellblockWorld<?> world,
			@NotNull BoundingBox bounds) {
		Set<PositionedBlock> farmBlocks = ConcurrentHashMap.newKeySet();
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		int minX = (int) Math.floor(bounds.getMinX());
		int minY = (int) Math.floor(bounds.getMinY());
		int minZ = (int) Math.floor(bounds.getMinZ());
		int maxX = (int) Math.ceil(bounds.getMaxX());
		int maxY = (int) Math.ceil(bounds.getMaxY());
		int maxZ = (int) Math.ceil(bounds.getMaxZ());

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Pos3 pos = new Pos3(x, y, z);
					CompletableFuture<Void> future = world.getBlockState(pos).thenAccept(stateOpt -> {
						if (stateOpt.isEmpty())
							return;

						CustomBlockState state = stateOpt.get();
						CustomBlock blockType = state.type();

						if (isFarmBlock(blockType) || isFarmSapling(blockType)) {
							farmBlocks.add(new PositionedBlock(pos, state));
							return;
						}

						if (isVerticalCrop(blockType)) {
							world.getBlockState(pos.down()).thenAccept(belowOpt -> {
								if (belowOpt.isEmpty() || !belowOpt.get().type().equals(blockType)) {
									farmBlocks.add(new PositionedBlock(pos, state));
								}
							});
						}
					});

					futures.add(future);
				}
			}
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> farmBlocks);
	}

	/**
	 * Returns the base growth chance for a crop based on its key and environmental
	 * conditions.
	 *
	 * <p>
	 * - If the crop requires lava (e.g., sugar cane, hydrated crops) and no lava is
	 * nearby, returns 0. - If the crop is only influenced by lava rain, only
	 * returns bonus if rain is active. - If lava (rain or nearby) is present,
	 * returns the enhanced growth chance. - Otherwise, returns the normal vanilla
	 * chance from the no-lava map.
	 * </p>
	 *
	 * @param key      the namespaced key of the crop (e.g., "minecraft:wheat")
	 * @param lavaRain whether lava rain is active at the crop's location
	 * @param lavaNear whether lava is near the base of the crop (hydration source
	 *                 or proximity)
	 * @return base chance for the crop to grow under current conditions
	 */
	public int getBaseChance(@NotNull String key, boolean lavaRain, boolean lavaNear) {
		String lowerKey = key.toLowerCase();

		// If the crop requires lava to grow (like sugar cane or unhydrated farmland)
		// and it's not nearby
		if (requiresLavaForGrowth(lowerKey) && !lavaNear) {
			return 0;
		}

		// If only lava rain affects it, and lava rain is active
		if (onlyLavaRainInfluences(lowerKey)) {
			return lavaRain ? BASE_CHANCE_LAVA.getOrDefault(lowerKey, 0)
					: BASE_CHANCE_NO_LAVA.getOrDefault(lowerKey, 0);
		}

		// If either lava rain or nearby lava is active, return lava-enhanced value
		if (lavaRain || lavaNear) {
			return BASE_CHANCE_LAVA.getOrDefault(lowerKey, BASE_CHANCE_NO_LAVA.getOrDefault(lowerKey, 0));
		}

		// Otherwise, return vanilla growth chance
		return BASE_CHANCE_NO_LAVA.getOrDefault(lowerKey, 0);
	}

	/**
	 * Determines whether a given block type represents a farm-related block.
	 *
	 * <p>
	 * Includes farmland, crops, stems, and related plant blocks.
	 * </p>
	 *
	 * @param block the custom block to check
	 * @return true if the block is a farm block; false otherwise
	 */
	private boolean isFarmBlock(@NotNull CustomBlock block) {
		if (farmBlocks.contains(block))
			return true;

		String key = block.type().key().asString();

		return switch (key) {
		case "minecraft:farmland", "minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots",
				"minecraft:nether_wart", "minecraft:red_mushroom", "minecraft:brown_mushroom",
				"minecraft:sweet_berry_bush", "minecraft:cocoa", "minecraft:melon_stem",
				"minecraft:attached_melon_stem", "minecraft:pumpkin_stem", "minecraft:attached_pumpkin_stem",
				"hellblock:farmland", "hellblock:wheat", "hellblock:carrots", "hellblock:potatoes",
				"hellblock:beetroots", "hellblock:nether_wart", "hellblock:red_mushroom", "hellblock:brown_mushroom",
				"hellblock:sweet_berry_bush", "hellblock:cocoa", "hellblock:melon_stem",
				"hellblock:attached_melon_stem", "hellblock:pumpkin_stem", "hellblock:attached_pumpkin_stem" ->
			true;
		default -> false;
		};
	}

	/**
	 * Determines whether a block is a sapling that contributes to farming
	 * progression.
	 *
	 * @param block the custom block to check
	 * @return true if the block is a farm sapling; false otherwise
	 */
	public boolean isFarmSapling(@NotNull CustomBlock block) {
		if (farmSaplings.contains(block))
			return true;

		String key = block.type().key().asString();
		return switch (key) {
		case "minecraft:oak_sapling", "minecraft:spruce_sapling", "minecraft:birch_sapling", "minecraft:jungle_sapling",
				"minecraft:acacia_sapling", "minecraft:dark_oak_sapling", "minecraft:mangrove_sapling",
				"minecraft:cherry_sapling", "minecraft:bamboo_sapling", "hellblock:glow_sapling" ->
			true;
		default -> false;
		};
	}

	/**
	 * Determines whether a block type is a vertical crop (e.g., sugar cane, bamboo,
	 * cactus).
	 *
	 * @param block the custom block to check
	 * @return true if the block is vertically growing; false otherwise
	 */
	private boolean isVerticalCrop(@NotNull CustomBlock block) {
		if (verticalFarmBlocks.contains(block))
			return true;

		String key = block.type().key().asString();

		return switch (key) {
		case "minecraft:sugar_cane", "minecraft:bamboo", "minecraft:cactus", "hellblock:sugar_cane", "hellblock:bamboo",
				"hellblock:cactus" ->
			true;
		default -> false;
		};
	}

	/**
	 * Determines whether a crop type requires lava near its base to grow.
	 *
	 * <p>
	 * This includes crops that depend on hydrated farmland (like wheat or carrots)
	 * or blocks like sugar cane that will not grow at all unless lava is nearby.
	 * </p>
	 *
	 * @param key the full namespaced key of the crop (e.g., "minecraft:wheat")
	 * @return true if lava is required for growth; false otherwise
	 */
	public boolean requiresLavaForGrowth(@NotNull String key) {
		String lowerKey = key.toLowerCase();

		return Set.of("minecraft:wheat", "hellblock:wheat", "minecraft:carrots", "hellblock:carrots",
				"minecraft:potatoes", "hellblock:potatoes", "minecraft:beetroots", "hellblock:beetroots",
				"minecraft:melon_stem", "hellblock:melon_stem", "minecraft:pumpkin_stem", "hellblock:pumpkin_stem",
				"minecraft:attached_melon_stem", "hellblock:attached_melon_stem", "minecraft:attached_pumpkin_stem",
				"hellblock:attached_pumpkin_stem", "minecraft:sugar_cane", "hellblock:sugar_cane").contains(lowerKey);
	}

	/**
	 * Determines whether a crop is influenced solely by lava rain and not by lava
	 * near the block.
	 *
	 * <p>
	 * These crops do not grow faster from lava nearby — only when lava rain is
	 * occurring.
	 * </p>
	 *
	 * @param key the full namespaced key of the crop (e.g., "minecraft:cocoa")
	 * @return true if only lava rain affects growth; false otherwise
	 */
	public boolean onlyLavaRainInfluences(@NotNull String key) {
		String lowerKey = key.toLowerCase();

		return Set.of("minecraft:cocoa", "hellblock:cocoa", "minecraft:sweet_berry_bush", "hellblock:sweet_berry_bush",
				"hellblock:glow_sapling").contains(lowerKey);
	}

	/**
	 * Checks whether a crop requires hydrated farmland to grow.
	 *
	 * <p>
	 * These crops are planted on farmland and will not grow unless the farmland is
	 * kept hydrated, which requires lava nearby. Lava rain does not hydrate
	 * farmland.
	 * </p>
	 *
	 * @param key the full namespaced key of the crop (e.g., "minecraft:beetroots")
	 * @return true if hydrated farmland is required for growth; false otherwise
	 */
	public boolean requiresHydratedFarmland(@NotNull String key) {
		String lowerKey = key.toLowerCase();

		return Set.of("minecraft:wheat", "hellblock:wheat", "minecraft:carrots", "hellblock:carrots",
				"minecraft:potatoes", "hellblock:potatoes", "minecraft:beetroots", "hellblock:beetroots",
				"minecraft:melon_stem", "hellblock:melon_stem", "minecraft:pumpkin_stem", "hellblock:pumpkin_stem",
				"minecraft:attached_melon_stem", "hellblock:attached_melon_stem", "minecraft:attached_pumpkin_stem",
				"hellblock:attached_pumpkin_stem").contains(lowerKey);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		final Block block = event.getBlock();
		final World world = block.getWorld();

		if (!isInCorrectWorld(world))
			return;

		final Pos3 pos = Pos3.from(block.getLocation());

		// FARM tracking
		if (block.getType() == Material.MELON || block.getType() == Material.PUMPKIN) {
			playerPlacedCrops.add(pos);
		}

		CustomBlock custom = getPlacedCropMapping().get(block.getType());
		if (custom != null) {
			instance.getWorldManager().getWorld(world).ifPresent(hellblockWorld -> {
				CustomBlockState state = custom.createBlockState();
				hellblockWorld.updateBlockState(pos, state).thenRun(() -> {
					playerPlacedCrops.add(pos);
					instance.debug("Farm crop block placed at " + pos + " in world '" + world.getName() + "' using "
							+ block.getType() + " on " + event.getBlockAgainst().getType() + " → "
							+ state.type().type().key().asString());
				});
			});
		}

		// Trigger crop update for farmland or crops
		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			instance.getIslandManager().resolveIslandId(block.getLocation())
					.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager().getWorld(world)
							.ifPresent(hellblockWorld -> updateCrops(hellblockWorld, id))));
		}

		// Replaced a lava block (e.g. with concrete powder)
		if (event.getBlockReplacedState().getType() == Material.LAVA) {
			instance.getIslandManager().resolveIslandId(block.getLocation())
					.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager().getWorld(world)
							.ifPresent(hellblockWorld -> updateCrops(hellblockWorld, id))));
		}

		// Concrete powder → lava interaction
		if (concreteConverter.getConcretePowderBlocks().contains(block.getType())) {
			for (BlockFace face : FACES) {
				if (block.getRelative(face).getType() == Material.LAVA) {
					block.setType(this.concreteConverter.convertToConcrete(block.getType()));
					playExtinguishSoundNear(block);
					break;
				}
			}
		}
	}

	@EventHandler
	public void onVillagerFarm(EntityChangeBlockEvent event) {
		final Entity entity = event.getEntity();
		final Block block = event.getBlock();
		final World world = block.getWorld();

		if (!isInCorrectWorld(world))
			return;

		// --- Case A: Enderman placing a block into lava (replacing lava) →
		// trigger crop updates for affected island (mirrors onBlockPlace lava
		// replacement)
		if (entity instanceof Enderman) {
			// Block is the original block before the change; if it's lava, an Enderman
			// placed something into lava (or removed lava and placed a block), so update
			// crops.
			if (block.getType() == Material.LAVA) {
				instance.getIslandManager().resolveIslandId(block.getLocation())
						.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
								.getWorld(world).ifPresent(hellblockWorld -> updateCrops(hellblockWorld, id))));
			}
			return;
		}

		// --- Case B: Villager placing/transforming blocks (e.g. planting crops /
		// creating farmland)
		if (!(entity instanceof Villager))
			return;

		final Material newType = event.getTo();
		final Pos3 pos = Pos3.from(block.getLocation());

		// If a villager placed a supported crop (same set you check on player
		// breaks/places),
		// trigger crop updates for the island.
		if (FarmRequirement.SUPPORTED_CROPS.contains(newType)) {
			instance.getIslandManager().resolveIslandId(block.getLocation())
					.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager().getWorld(world)
							.ifPresent(hellblockWorld -> updateCrops(hellblockWorld, id))));
			return;
		}

		// If villager created farmland (tilling) or otherwise created/changed a block
		// that
		// should trigger crop updates (e.g. farmland conversion), trigger update +
		// clear caches if needed.
		if (newType == Material.FARMLAND) {
			instance.getIslandManager().resolveIslandId(block.getLocation())
					.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager().getWorld(world)
							.ifPresent(hellblockWorld -> updateCrops(hellblockWorld, id))));
			// Clear caches at the position since farmland state changed (mirrors
			// onBlockBreak/onBlockExplode)
			clearCachesAt(pos);
			return;
		}

		// Fallback: if the resulting block is an Ageable (crops that have growth
		// stages),
		// treat it like a crop change and trigger updates (defensive: in case API
		// provides the data)
		try {
			// The event does not expose BlockData directly for the 'to' material, so
			// we inspect the current block state after the change if possible.
			// This mirrors patterns in other event handlers: resolve island and update
			// crops.
			instance.getIslandManager().resolveIslandId(block.getLocation()).thenAccept(optIslandId -> optIslandId
					.ifPresent(id -> instance.getWorldManager().getWorld(world).ifPresent(hellblockWorld -> {
						// attempt to trigger update if the changed block is a crop or farmland
						hellblockWorld.getBlockState(pos).thenAccept(afterStateOpt -> {
							if (afterStateOpt.isEmpty())
								return;

							CustomBlockState afterState = afterStateOpt.get();
							if (afterState != null) {
								// If it's a growable/farmland-like custom block, update the crops.
								if (afterState.type() instanceof Growable
										|| afterState.type().type().key().asString().contains("farmland")) {
									updateCrops(hellblockWorld, id);
								}
							}
						});
					})));
		} catch (Throwable ignored) {
			// safe fallback: do nothing
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		final World world = block.getWorld();

		if (!isInCorrectWorld(world))
			return;

		final Player player = event.getPlayer();
		final Pos3 pos = Pos3.from(block.getLocation());

		// --- Case A: Sugar Cane (custom break logic)
		if (block.getType() == Material.SUGAR_CANE) {
			HellblockWorld<?> hellblockWorld = instance.getWorldManager().getWorld(world).orElse(null);
			if (hellblockWorld != null) {
				event.setCancelled(true);
				hellblockWorld.getBlockState(pos).thenAccept(optState -> {
					optState.ifPresent(
							state -> tryBreakCaneIfNoLava(new PositionedBlock(pos, state), player, hellblockWorld));
				});
			}
			return;
		}

		// --- Case B: Tracked Farm crops
		if (FarmRequirement.SUPPORTED_CROPS.contains(block.getType())) {
			if (playerPlacedCrops.contains(pos)) {
				playerPlacedCrops.remove(pos); // immature crop → no credit
			} else {
				instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
					if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 5000)) {
						userData.getHellblockData().updateLastIslandActivity();
					}
					instance.getChallengeManager().handleChallengeProgression(userData, ActionType.FARM, block);
				});
			}
			return;
		}

		// --- Case C: Crop or Farmland change → update crops
		if (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable) {
			instance.getIslandManager().resolveIslandId(block.getLocation())
					.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager().getWorld(world)
							.ifPresent(hellblockWorld -> updateCrops(hellblockWorld, id))));

			// --- Clear cache if it's farmland
			if (block.getBlockData() instanceof Farmland) {
				clearCachesAt(pos);
			}
		}
	}

	@EventHandler
	public void onBlockGrow(BlockGrowEvent event) {
		final Block block = event.getBlock();
		final World world = block.getWorld();

		if (!isInCorrectWorld(world)) {
			return;
		}

		// --- Case A: Sugar Cane (cancel and handle manually)
		if (block.getType() == Material.SUGAR_CANE
				|| block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {

			HellblockWorld<?> hellblockWorld = instance.getWorldManager().getWorld(world).orElse(null);
			if (hellblockWorld != null) {
				Block base = block.getType() == Material.SUGAR_CANE ? block : block.getRelative(BlockFace.DOWN);

				Pos3 pos = Pos3.from(base.getLocation());

				hellblockWorld.getBlockState(pos).thenAccept(optState -> {
					optState.ifPresent(state -> {
						event.setCancelled(true); // Prevent natural growth
						tryBreakCaneIfNoLava(new PositionedBlock(pos, state), null, hellblockWorld);
					});
				});
			}
			return;
		}

		// --- Case B: Grown crop placed by player (remove from tracking)
		final Pos3 pos = Pos3.from(block.getLocation());
		if (playerPlacedCrops.contains(pos)) {
			playerPlacedCrops.remove(pos);
		}

		// --- Case C: Trigger crop updates if supported
		instance.getIslandManager().resolveIslandId(block.getLocation())
				.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager().getWorld(world)
						.ifPresent(hellblockWorld -> updateCrops(hellblockWorld, id))));
	}

	@EventHandler
	public void onBonemealUse(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (item == null || item.getType() != Material.BONE_MEAL)
			return;

		Block clicked = event.getClickedBlock();
		if (clicked == null)
			return;

		Location loc = clicked.getLocation();
		World bukkitWorld = loc.getWorld();
		if (bukkitWorld == null)
			return;

		Pos3 pos = Pos3.from(loc);
		Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(bukkitWorld.getName());
		if (worldOpt.isEmpty())
			return;

		HellblockWorld<?> world = worldOpt.get();

		world.getBlockState(pos).thenAccept(optState -> {
			if (optState.isEmpty())
				return;

			CustomBlockState state = optState.get();
			if (!(state.type() instanceof Growable growable))
				return;

			// Skip bonemeal for specific crops
			if (state.type().equals(CustomBlockTypes.NETHER_WART))
				return;

			int age = growable.getAge(state);
			int maxAge = growable.getMaxAge(state);
			if (age >= maxAge)
				return;

			// Grow 1–3 random stages
			int stages = RandomUtils.generateRandomInt(1, 3);
			growable.setAge(state, Math.min(age + stages, maxAge));
			world.updateBlockState(pos, state).thenRun(() -> {

				instance.getIslandManager().resolveIslandId(loc).thenAccept(
						id -> id.filter(Objects::nonNull).ifPresent(islandId -> updateCrops(world, islandId)));

				// Visual feedback
				playBoneMealUseEffect(bukkitWorld, loc);

				// Consume item if not creative
				if (player.getGameMode() != GameMode.CREATIVE) {
					item.setAmount(item.getAmount() - 1);
					player.updateInventory();
				}
			});
		});

		event.setCancelled(true); // Prevent vanilla bonemeal behavior
	}

	@EventHandler
	public void onDispenserBonemeal(BlockDispenseEvent event) {
		ItemStack item = event.getItem();
		if (item.getType() != Material.BONE_MEAL)
			return;

		Block dispenser = event.getBlock();
		if (!(dispenser.getBlockData() instanceof org.bukkit.block.data.Directional directional))
			return;

		BlockFace facing = directional.getFacing();
		Block target = dispenser.getRelative(facing);

		World bukkitWorld = dispenser.getWorld();
		if (bukkitWorld == null)
			return;

		Pos3 pos = Pos3.from(target.getLocation());
		Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(bukkitWorld.getName());
		if (worldOpt.isEmpty())
			return;

		HellblockWorld<?> world = worldOpt.get();

		world.getBlockState(pos).thenAccept(optState -> {
			if (optState.isEmpty())
				return;

			CustomBlockState state = optState.get();
			if (!(state.type() instanceof Growable growable))
				return;

			// Skip Nether Wart
			if (state.type().equals(CustomBlockTypes.NETHER_WART))
				return;

			int age = growable.getAge(state);
			int maxAge = growable.getMaxAge(state);
			if (age >= maxAge)
				return;

			// Cancel vanilla dispenser behavior
			event.setCancelled(true);

			// Grow 1–3 random stages
			int stages = RandomUtils.generateRandomInt(1, 3);
			growable.setAge(state, Math.min(age + stages, maxAge));
			world.updateBlockState(pos, state).thenRun(() -> {

				instance.getIslandManager().resolveIslandId(target.getLocation()).thenAccept(
						id -> id.filter(Objects::nonNull).ifPresent(islandId -> updateCrops(world, islandId)));

				// Visual feedback
				playBoneMealUseEffect(bukkitWorld, target.getLocation());

				// Consume exactly one bonemeal
				if (dispenser.getState() instanceof Dispenser disp) {
					Inventory inv = disp.getInventory();
					for (ItemStack slot : inv.getContents()) {
						if (slot != null && slot.getType() == Material.BONE_MEAL) {
							slot.setAmount(slot.getAmount() - 1);
							break;
						}
					}
				}
			});
		});
	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld()))
			return;

		if (block.getBlockData() instanceof Farmland && (Tag.CROPS.isTagged(block.getRelative(BlockFace.UP).getType())
				|| block.getRelative(BlockFace.UP).isEmpty())) {

			final Location location = block.getLocation();

			instance.getIslandManager().resolveIslandId(location).thenAccept(optIslandId -> {
				if (optIslandId.isEmpty())
					return;

				final int islandId = optIslandId.get();
				if (!isLavaRainingAt(islandId, location)) {
					event.setCancelled(true);
					instance.getWorldManager().getWorld(block.getWorld())
							.ifPresent(world -> updateCrops(world, islandId));
				} else {
					clearCachesAt(Pos3.from(location));
				}
			});
		}
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		for (Block block : event.blockList()) {
			if (!isInCorrectWorld(block.getWorld()))
				continue;

			// Farmland: trigger crop updates + clear caches
			if (block.getBlockData() instanceof Farmland) {
				instance.getIslandManager().resolveIslandId(block.getLocation())
						.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
								.getWorld(block.getWorld()).ifPresent(world -> updateCrops(world, id))));
				clearCachesAt(Pos3.from(block.getLocation()));
			}

			// Sugar cane: prevent explosion + break naturally if needed
			if (block.getType() == Material.SUGAR_CANE) {
				instance.getWorldManager().getWorld(block.getWorld()).ifPresent(world -> {
					event.setCancelled(true); // prevent sugar cane from dropping unnaturally
					Pos3 pos = Pos3.from(block.getLocation());

					world.getBlockState(pos).thenAccept(optState -> {
						optState.ifPresent(state -> tryBreakCaneIfNoLava(new PositionedBlock(pos, state), null, world));
					});
				});
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
			instance.getIslandManager().resolveIslandId(block.getLocation())
					.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
							.getWorld(block.getWorld()).ifPresent(world -> updateCrops(world, id))));
		}
	}

	@EventHandler
	public void onLavaPickup(PlayerBucketFillEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player))
			return;

		final Block block = event.getBlock();
		final ItemStack item = event.getItemStack();

		// Only respond if lava bucket is result
		if (item == null || item.getType() != Material.LAVA_BUCKET)
			return;

		// Trigger crop updates for affected island
		instance.getIslandManager().resolveIslandId(block.getLocation())
				.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
						.getWorld(block.getWorld()).ifPresent(world -> updateCrops(world, id))));

		// Sugar cane break check (lava removed → break unsupported cane)
		instance.getWorldManager().getWorld(player.getWorld()).ifPresent(world -> {
			for (BlockFace face : FACES) {
				Block neighbor = block.getRelative(face).getRelative(BlockFace.UP);
				if (neighbor.getType() == Material.SUGAR_CANE) {
					Pos3 pos = Pos3.from(neighbor.getLocation());
					world.getBlockState(pos).thenAccept(optState -> {
						optState.ifPresent(
								state -> tryBreakCaneIfNoLava(new PositionedBlock(pos, state), player, world));
					});
				}
			}
		});
	}

	@EventHandler
	public void onLavaPickupByDispenser(BlockDispenseEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld()))
			return;

		if (!(block.getState() instanceof Dispenser dispenser))
			return;
		if (!(block.getBlockData() instanceof Directional directional))
			return;

		// Must be attempting to pick up lava
		if (!dispenser.getInventory().contains(Material.BUCKET)
				|| dispenser.getInventory().contains(Material.LAVA_BUCKET))
			return;

		Block facing = block.getRelative(directional.getFacing());
		if (facing.getType() != Material.LAVA)
			return;

		// Trigger crop updates for the island
		instance.getIslandManager().resolveIslandId(block.getLocation())
				.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
						.getWorld(block.getWorld()).ifPresent(world -> updateCrops(world, id))));

		// Break sugar cane above if needed
		instance.getWorldManager().getWorld(block.getWorld()).ifPresent(world -> {
			for (BlockFace face : FACES) {
				Block neighbor = facing.getRelative(face).getRelative(BlockFace.UP);
				if (neighbor.getType() == Material.SUGAR_CANE) {
					Pos3 pos = Pos3.from(neighbor.getLocation());
					world.getBlockState(pos).thenAccept(optState -> {
						optState.ifPresent(state -> tryBreakCaneIfNoLava(new PositionedBlock(pos, state), null, world));
					});
				}
			}
		});
	}

	@EventHandler
	public void onPistonExtend(BlockPistonExtendEvent event) {
		final Block piston = event.getBlock();
		final World world = piston.getWorld();

		if (!isInCorrectWorld(world))
			return;

		instance.getWorldManager().getWorld(world).ifPresent(hellblockWorld -> {
			event.getBlocks().forEach(movedBlock -> {
				// Lava pushed by piston → update crops on the island
				if (movedBlock.getType() == Material.LAVA) {
					instance.getIslandManager().resolveIslandId(movedBlock.getLocation())
							.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
									.getWorld(world).ifPresent(hbWorld -> updateCrops(hbWorld, id))));
				}

				// Sugar cane affected by piston → possibly break if no lava support
				if (movedBlock.getType() == Material.SUGAR_CANE) {
					Pos3 pos = Pos3.from(movedBlock.getLocation());
					hellblockWorld.getBlockState(pos).thenAccept(optState -> {
						optState.ifPresent(
								state -> tryBreakCaneIfNoLava(new PositionedBlock(pos, state), null, hellblockWorld));
					});
				}
			});
		});
	}

	@EventHandler
	public void onMoistureChange(MoistureChangeEvent event) {
		final Block block = event.getBlock();
		if (!isInCorrectWorld(block.getWorld()))
			return;

		if (block.getBlockData() instanceof Farmland) {
			final Location location = block.getLocation();

			instance.getIslandManager().resolveIslandId(location).thenAccept(optIslandId -> {
				if (optIslandId.isEmpty())
					return;

				final int islandId = optIslandId.get();
				if (!isLavaRainingAt(islandId, location)) {
					event.setCancelled(true);
					instance.getWorldManager().getWorld(block.getWorld())
							.ifPresent(world -> updateCrops(world, islandId));
				} else {
					moistureCache.remove(Pos3.from(location));
				}
			});
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
				.forEach(block -> instance.getIslandManager().resolveIslandId(block.getLocation())
						.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
								.getWorld(block.getWorld()).ifPresent(world -> updateCrops(world, id)))));
	}

	@EventHandler
	public void onHarvest(PlayerHarvestBlockEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player)) {
			return;
		}

		final Block block = event.getHarvestedBlock();
		if (block.getBlockData() instanceof Ageable) {
			instance.getIslandManager().resolveIslandId(block.getLocation())
					.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
							.getWorld(block.getWorld()).ifPresent(world -> updateCrops(world, id))));
		}
	}

	@EventHandler
	public void onFarmland(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		final Block block = event.getClickedBlock();
		final ItemStack item = event.getItem();
		final Action action = event.getAction();

		if (!isInCorrectWorld(player) || block == null)
			return;

		final World world = block.getWorld();
		final Pos3 pos = Pos3.from(block.getLocation());

		instance.getWorldManager().getWorld(world).ifPresent(hellblockWorld -> {
			// --- Case 1: Hoe used on dirt → farmland
			if (action == Action.RIGHT_CLICK_BLOCK && item != null && getHoeItems().contains(item.getType())) {
				hellblockWorld.getBlockState(pos).thenAccept(optState -> {
					if (optState.map(state -> state.type().type().key().asString()).map(VALID_DIRT_BLOCK_KEYS::contains)
							.orElse(false)) {
						instance.getIslandManager().resolveIslandId(block.getLocation())
								.thenAccept(optIslandId -> optIslandId.ifPresent(id -> {
									CustomBlockState newState = CustomBlockTypes.FARMLAND.createBlockState();
									hellblockWorld.updateBlockState(pos, newState).thenRun(() -> {
										instance.debug("FARMLAND tilled at " + pos + " in world '" + world.getName()
												+ "' using " + item.getType() + " on " + block.getType() + " → "
												+ newState.type().type().key().asString());
										updateCrops(hellblockWorld, id);
									});
								}));
					}
				});
				return;
			}

			// --- Case 2: Planting crop on farmland
			if (action == Action.RIGHT_CLICK_BLOCK && item != null && Tag.CROPS.isTagged(item.getType())) {
				final Pos3 abovePos = pos.up();
				hellblockWorld.getBlockState(pos).thenAccept(optState -> {
					if (optState.map(state -> state.type().type().key().asString())
							.map(Set.of("minecraft:farmland", "hellblock:farmland")::contains).orElse(false)) {
						CustomBlock custom = getPlacedCropMapping().get(item.getType());
						if (custom != null) {
							CustomBlockState cropState = custom.createBlockState();
							hellblockWorld.updateBlockState(abovePos, cropState).thenRun(() -> {
								playerPlacedCrops.add(pos);
								instance.debug("Crop placed at " + abovePos + " using " + item.getType());
							});
						}
					}
				});
				return;
			}

			// --- Case 3: Planting nether wart on soul sand
			if (action == Action.RIGHT_CLICK_BLOCK && item != null && item.getType() == Material.NETHER_WART) {
				final Pos3 abovePos = pos.up();
				hellblockWorld.getBlockState(pos).thenAccept(optState -> {
					if (optState.map(state -> state.type().type().key().asString()).map(SOUL_SAND_KEY::contains)
							.orElse(false)) {
						CustomBlockState nwState = CustomBlockTypes.NETHER_WART.createBlockState();
						hellblockWorld.updateBlockState(abovePos, nwState).thenRun(() -> {
							playerPlacedCrops.add(pos);
							instance.debug("Nether wart planted at " + abovePos + " using " + item.getType());
						});
					}
				});
				return;
			}

			// --- Case 4: Sweet berries
			if (action == Action.RIGHT_CLICK_BLOCK && item != null && item.getType() == Material.SWEET_BERRIES) {
				final Pos3 abovePos = pos.up();
				hellblockWorld.getBlockState(pos).thenAccept(optState -> {
					if (optState.map(state -> state.type().type().key().asString())
							.map(VALID_SWEET_BERRY_SOIL_KEYS::contains).orElse(false)) {
						CustomBlockState berryState = CustomBlockTypes.SWEET_BERRY_BUSH.createBlockState();
						hellblockWorld.updateBlockState(abovePos, berryState).thenRun(() -> {
							playerPlacedCrops.add(pos);
							instance.debug("Sweet berries planted at " + abovePos);
						});
					}
				});
				return;
			}

			// --- Case 5: Physical trample
			if (action == Action.PHYSICAL
					&& (block.getBlockData() instanceof Farmland || block.getBlockData() instanceof Ageable)) {
				clearCachesAt(pos);
			}
		});
	}

	@EventHandler
	public void onCropRightClick(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		final Player player = event.getPlayer();
		final Block clicked = event.getClickedBlock();

		if (clicked == null || !isInCorrectWorld(player))
			return;

		final Location location = clicked.getLocation();
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(location.getWorld());
		if (worldOpt.isEmpty())
			return;

		final HellblockWorld<?> world = worldOpt.get();
		final Pos3 pos = Pos3.from(location);

		world.getBlockState(pos).thenAccept(optState -> {
			if (optState.isEmpty())
				return;

			CustomBlockState state = optState.get();
			CustomBlock block = state.type();

			if (block instanceof CustomMushroomBlock || block instanceof CustomSaplingBlock
					|| block instanceof CustomCocoaBlock)
				return;
			if (!(block instanceof Growable growable))
				return;

			int age = growable.getAge(state);
			int maxAge = growable.getMaxAge(state);
			if (age >= maxAge)
				return;

			// Check below for dry farmland
			Pos3 belowPos = pos.down();
			world.getBlockState(belowPos).thenAccept(optBelow -> {
				if (optBelow.isPresent()) {
					CustomBlockState belowState = optBelow.get();
					if (belowState.type() instanceof MoistureHolder moistureHolder) {
						if (moistureHolder.getMoisture(belowState) == 0) {
							// Dry farmland → crop will break → skip hologram
							return;
						}
					}
				}

				int growthRemaining = maxAge - age;
				int baseSeconds = RandomUtils.generateRandomInt(20, 40) * growthRemaining;

				withCropGrowthBonusIfValid(new PositionedBlock(pos, state), world, bonus -> {
					int finalEstimate = (int) (baseSeconds * (1.0 - bonus)); // faster with bonus

					Component tooltip = generateGrowthBarWithTime(age, maxAge, finalEstimate);
					String message = AdventureHelper.componentToJson(tooltip);

					Location hologramLoc = pos.up().toLocation(location.getWorld()).clone().add(0.5, 0.5, 0.5);
					instance.getHologramManager().showHologram(player, hologramLoc, message, 5000); // 5s
				});
			});
		});
	}

	/**
	 * Creates a visual growth progress bar using colored blocks and appends a time
	 * estimate indicating when the crop will be fully grown.
	 * <p>
	 * The progress bar uses a dynamic color gradient based on the growth stage, and
	 * omits rendering if the crop is dehydrated (for farmland-based crops).
	 * <p>
	 * Example output: {@code [■■■□□] ~4.2s}
	 *
	 * @param progress    the growth progress from 0.0 (not grown) to 1.0 (fully
	 *                    grown)
	 * @param secondsLeft the estimated seconds remaining until full growth
	 * @param hydrated    whether the crop is hydrated; if {@code false}, the method
	 *                    may opt to suppress the display
	 * @return a formatted {@link Component} representing the growth bar, or
	 *         {@code null} if no hologram should be shown
	 */
	@NotNull
	private Component generateGrowthBarWithTime(int currentAge, int maxAge, double secondsLeft) {
		int segments = 5;
		double progress = (double) currentAge / maxAge;
		int filled = (int) Math.round(progress * segments);

		Component bar = Component.text("[");
		for (int i = 0; i < segments; i++) {
			String block = i < filled ? "■" : "□";

			TextColor color;
			if (i < filled) {
				float ratio = (float) i / (segments - 1); // 0.0 → 1.0
				int red = (int) ((1.0 - ratio) * 255);
				int green = (int) (ratio * 255);
				color = TextColor.color(red, green, 0);
			} else {
				color = TextColor.fromHexString(NamedTextColor.DARK_GRAY.asHexString());
			}

			bar = bar.append(Component.text(block, color));
		}
		bar = bar.append(Component.text("] "));

		// Append time remaining
		String timeStr = "~" + String.format(Locale.US, "%.1f", secondsLeft) + "s";
		bar = bar.append(Component.text(timeStr, TextColor.fromHexString(NamedTextColor.GRAY.asHexString())));

		return bar;
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
		instance.getIslandManager().resolveIslandId(block.getLocation())
				.thenAccept(optIslandId -> optIslandId.ifPresent(id -> instance.getWorldManager()
						.getWorld(block.getWorld()).ifPresent(world -> updateCrops(world, id))));
		if (block.getBlockData() instanceof Farmland) {
			clearCachesAt(Pos3.from(block.getLocation()));
		}
	}

	@EventHandler
	public void onForceSugarCanePlacement(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!isInCorrectWorld(player) || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final Block clickedBlock = event.getClickedBlock();
		final Material item = event.getMaterial();
		final BlockFace face = event.getBlockFace();

		if (clickedBlock == null || face != BlockFace.UP || item != Material.SUGAR_CANE) {
			return;
		}

		final World bukkitWorld = clickedBlock.getWorld();
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(bukkitWorld);
		if (worldOpt.isEmpty())
			return;

		final HellblockWorld<?> world = worldOpt.get();
		final Pos3 clickedPos = Pos3.from(clickedBlock.getLocation());

		CompletableFuture<Optional<CustomBlockState>> clickedStateFuture = world.getBlockState(clickedPos);
		CompletableFuture<Optional<CustomBlockState>> aboveStateFuture = world.getBlockState(clickedPos.up());

		clickedStateFuture.thenCombineAsync(aboveStateFuture, (clickedStateOpt, aboveStateOpt) -> {
			if (clickedStateOpt.isEmpty())
				return null;
			if (aboveStateOpt.isPresent() && !aboveStateOpt.get().isAir())
				return null;
			return new PositionedBlock(clickedPos, clickedStateOpt.get());
		}).thenCompose(positionedSoil -> {
			if (positionedSoil == null)
				return CompletableFuture.completedFuture(false);
			return checkForLavaAroundSugarCane(positionedSoil, world);
		}).thenAccept(hasLavaNearby -> {
			if (hasLavaNearby) {
				placeSugarCane(player, event, clickedBlock, face);
			} else {
				// Case 2: placing on sugar cane with lava at base
				clickedStateFuture.thenAccept(clickedStateOpt -> {
					if (clickedStateOpt.isEmpty())
						return;

					String typeKey = clickedStateOpt.get().type().type().key().asString().toLowerCase();
					if (!SUGAR_CANE_KEYS.contains(typeKey))
						return;

					Pos3 belowPos = clickedPos.down();
					world.getBlockState(belowPos).thenCompose(belowStateOpt -> {
						if (belowStateOpt.isEmpty())
							return CompletableFuture.completedFuture(false);
						PositionedBlock below = new PositionedBlock(belowPos, belowStateOpt.get());
						return checkForLavaAroundSugarCane(below, world);
					}).thenAccept(baseHasLava -> {
						if (baseHasLava) {
							placeSugarCane(player, event, clickedBlock, face);
						}
					});
				});
			}
		});
	}

	/**
	 * Handles manual sugar cane placement by a player.
	 * 
	 * <p>
	 * Consumes one item from the player's hand (if not in Creative mode), sets the
	 * target block to sugar cane, plays placement sound, and updates the inventory.
	 * </p>
	 *
	 * @param player the player placing the sugar cane
	 * @param event  the interaction event triggering the placement
	 * @param base   the block adjacent to where sugar cane should be placed
	 * @param face   the direction the player is facing when placing
	 */
	private void placeSugarCane(@NotNull Player player, @NotNull PlayerInteractEvent event, @NotNull Block base,
			@NotNull BlockFace face) {
		event.setUseItemInHand(Result.ALLOW);

		if (player.getGameMode() != GameMode.CREATIVE && event.getItem() != null) {
			event.getItem().setAmount(Math.max(0, event.getItem().getAmount() - 1));
		}

		if (event.getHand() != null) {
			VersionHelper.getNMSManager().swingHand(player,
					event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
		}

		final World bukkitWorld = base.getWorld();
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(bukkitWorld);
		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null)
			return;

		final HellblockWorld<?> world = worldOpt.get();
		final Pos3 targetPos = Pos3.from(base.getRelative(face).getLocation());

		CustomBlock sugarCaneBlock = CustomBlockTypes.SUGAR_CANE;
		CustomBlockState sugarCaneState = sugarCaneBlock.createBlockState();

		world.updateBlockState(targetPos, sugarCaneState).thenRun(() -> {
			playerPlacedCrops.add(targetPos);
			instance.debug("Sugar cane placed at " + targetPos + " in world '" + world.worldName() + "' using "
					+ (event.getItem() != null ? event.getItem().getType() : "null") + " on " + base.getType() + " → "
					+ sugarCaneState.type().type().key().asString());
			Location placedLoc = targetPos.toLocation(bukkitWorld);
			AdventureHelper.playPositionalSound(bukkitWorld, placedLoc,
					Sound.sound(Key.key("minecraft:block.grass.place"), Source.BLOCK, 1.0f, 1.0f));

			player.updateInventory();
		});
	}

	@EventHandler
	public void onSugarCaneUpdate(BlockPhysicsEvent event) {
		Block block = event.getSourceBlock();
		if (!isInCorrectWorld(block.getWorld()))
			return;

		HellblockWorld<?> world = instance.getWorldManager().getWorld(block.getWorld()).orElse(null);
		if (world == null)
			return;

		Pos3 targetPos = Pos3.from(block.getLocation());

		world.getBlockState(targetPos).thenAcceptAsync(optState -> {
			if (optState.isPresent() && isSugarCane(new PositionedBlock(targetPos, optState.get()))) {
				event.setCancelled(true);
				tryBreakCaneIfNoLava(new PositionedBlock(targetPos, optState.get()), null, world);
				return;
			}

			// Check neighbors
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			for (BlockFace face : FACES) {
				Block neighbor = block.getRelative(face);
				if (neighbor.getType() == Material.SUGAR_CANE) {
					Pos3 neighborPos = Pos3.from(neighbor.getLocation());
					futures.add(world.getBlockState(neighborPos).thenAcceptAsync(neighborOpt -> {
						if (neighborOpt.isPresent()
								&& isSugarCane(new PositionedBlock(neighborPos, neighborOpt.get()))) {
							event.setCancelled(true);
							tryBreakCaneIfNoLava(new PositionedBlock(neighborPos, neighborOpt.get()), null, world);
						}
					}));
				}
			}
		});
	}

	/**
	 * Breaks sugar cane blocks if no nearby lava or lava rain is detected near its
	 * base. Drops items and triggers visuals/sounds accordingly.
	 *
	 * @param block  the top sugar cane block
	 * @param player the player (can be null) who triggered the event
	 * @param world  the Hellblock world
	 */
	private void tryBreakCaneIfNoLava(@NotNull PositionedBlock block, @Nullable Player player,
			@NotNull HellblockWorld<?> world) {
		if (!isSugarCane(block))
			return;

		// Prepare the two positions we need to check for lava
		Pos3 belowPos = block.pos().offset(BlockFace.DOWN);
		Pos3 below2Pos = belowPos.offset(BlockFace.DOWN);

		CompletableFuture<Optional<CustomBlockState>> belowStateFuture = world.getBlockState(belowPos);
		CompletableFuture<Optional<CustomBlockState>> below2StateFuture = world.getBlockState(below2Pos);

		// Once both states are available, check for lava asynchronously
		belowStateFuture.thenCombineAsync(below2StateFuture, (belowOpt, below2Opt) -> {
			PositionedBlock below = new PositionedBlock(belowPos, belowOpt.orElse(null));
			PositionedBlock below2 = new PositionedBlock(below2Pos, below2Opt.orElse(null));
			return new AbstractMap.SimpleEntry<>(below, below2);
		}).thenCompose(pair -> {
			// Run lava checks on both blocks
			CompletableFuture<Boolean> lava1 = checkForLavaAroundSugarCane(pair.getKey(), world);
			CompletableFuture<Boolean> lava2 = checkForLavaAroundSugarCane(pair.getValue(), world);
			return lava1.thenCombine(lava2, (hasLava1, hasLava2) -> hasLava1 || hasLava2);
		}).thenAccept(hasLavaNearby -> {
			if (!hasLavaNearby) {
				handleBlocksAboveCane(block, world);
				breakSugarCaneChain(block, true, player, world);
			}
		});
	}

	/**
	 * Breaks a vertical chain of sugar cane blocks starting from the given
	 * position.
	 * 
	 * <p>
	 * This method optionally includes the base block and recursively removes all
	 * sugar cane blocks above it. It also handles item drops, particles, and sound
	 * effects based on player interaction or ambient triggers.
	 * </p>
	 *
	 * @param start       the bottom or starting sugar cane block
	 * @param includeBase whether to include the base block in the break chain
	 * @param player      the player responsible for breaking (may be null)
	 * @param world       the Hellblock world context
	 */
	private void breakSugarCaneChain(@NotNull PositionedBlock start, boolean includeBase, @Nullable Player player,
			@NotNull HellblockWorld<?> world) {
		Pos3 currentPos = includeBase ? start.pos() : start.pos().offset(BlockFace.UP);

		// Recursive async loop using a chain of CompletableFutures
		CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

		while (true) {
			Pos3 finalPos = currentPos;

			chain = chain.thenCompose(ignored -> world.getBlockState(finalPos).thenCompose(optState -> {
				if (optState.isEmpty())
					return CompletableFuture.completedFuture(null);

				CustomBlockState state = optState.get();
				PositionedBlock block = new PositionedBlock(finalPos, state);
				if (!isSugarCane(block))
					return CompletableFuture.completedFuture(null);

				Location loc = finalPos.toLocation(world.bukkitWorld());

				// Drop + update async-safe
				instance.getScheduler().executeSync(() -> world.removeBlockState(finalPos));

				world.bukkitWorld().dropItemNaturally(loc, new ItemStack(Material.SUGAR_CANE));
				world.bukkitWorld().spawnParticle(ParticleUtils.getParticle("BLOCK_DUST"), loc, 5,
						Material.SUGAR_CANE.createBlockData());

				if (player != null) {
					AdventureHelper.playSound(instance.getSenderFactory().getAudience(player), Sound.sound(
							net.kyori.adventure.key.Key.key("minecraft:block.grass.break"), Sound.Source.PLAYER, 1, 1));
				} else {
					AdventureHelper.playPositionalSound(world.bukkitWorld(), loc,
							Sound.sound(Key.key("minecraft:block.grass.break"), Source.BLOCK, 1.0f, 1.0f));
				}

				return CompletableFuture.completedFuture(null);
			}));

			// Prepare for next block up
			currentPos = currentPos.offset(BlockFace.UP);

			// We don't prefetch state, so we stop when the current chain is broken
			// The `thenCompose()` will short-circuit if the block is not sugar cane.
			if (chain.isDone())
				break;
		}
	}

	/**
	 * Handles the behavior of blocks placed above sugar cane when it is broken.
	 * 
	 * <p>
	 * If gravity-affected blocks (e.g., sand, gravel) are detected above, they are
	 * turned into falling entities. Wool carpets are dropped as items. All other
	 * blocks are simply removed from the custom world state.
	 * </p>
	 *
	 * @param caneBase the base sugar cane block that was broken
	 * @param world    the Hellblock world where the event occurred
	 */
	@SuppressWarnings("deprecation")
	private void handleBlocksAboveCane(@NotNull PositionedBlock caneBase, @NotNull HellblockWorld<?> world) {
		Pos3 upPos = caneBase.pos().offset(BlockFace.UP);
		Pos3 up2Pos = upPos.offset(BlockFace.UP);

		CompletableFuture<Optional<CustomBlockState>> upStateFuture = world.getBlockState(upPos);
		CompletableFuture<Optional<CustomBlockState>> up2StateFuture = world.getBlockState(up2Pos);

		upStateFuture.thenCombineAsync(up2StateFuture, (upStateOpt, up2StateOpt) -> {
			List<PositionedBlock> aboveBlocks = List.of(new PositionedBlock(upPos, upStateOpt.orElse(null)),
					new PositionedBlock(up2Pos, up2StateOpt.orElse(null)));

			return aboveBlocks;
		}).thenAccept(aboveBlocks -> {
			for (PositionedBlock block : aboveBlocks) {
				CustomBlockState state = block.state();
				if (state == null || state.isAir())
					continue;

				String key = state.type().type().key().asString().toUpperCase(Locale.ROOT);
				Material mat = Material.matchMaterial(key);

				if (mat != null) {
					if (mat.hasGravity()) {
						FallingBlock falling = world.bukkitWorld().spawnFallingBlock(
								block.pos().toLocation(world.bukkitWorld()).clone().add(0.5, 0, 0.5),
								mat.createBlockData());
						falling.setDropItem(false);
						falling.setHurtEntities(false);
					} else if (isWoolCarpet(mat)) {
						world.bukkitWorld().dropItemNaturally(block.pos().toLocation(world.bukkitWorld()),
								new ItemStack(mat));
					}
				}

				instance.getScheduler().executeSync(() -> world.removeBlockState(block.pos()));
			}
		});
	}

	/**
	 * Checks if the given block is sugar cane based on its custom block type key.
	 *
	 * @param block the positioned block to check
	 * @return true if the block is sugar cane; false otherwise
	 */
	private boolean isSugarCane(@NotNull PositionedBlock block) {
		if (block.state() == null)
			return false;
		String key = block.state().type().type().key().asString();
		return SUGAR_CANE_KEYS.contains(key);
	}

	@EventHandler
	public void onForceCocoaBeanPlacement(PlayerInteractEvent event) {
		final Player player = event.getPlayer();

		if (!isInCorrectWorld(player) || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final Block clicked = event.getClickedBlock();
		final Material item = event.getMaterial();
		final BlockFace face = event.getBlockFace();

		// Must click a valid horizontal face and be holding cocoa beans
		if (clicked == null || !Arrays.asList(FACES).contains(face) || item != Material.COCOA_BEANS) {
			return;
		}

		// Face must be air
		if (!clicked.getRelative(face).isEmpty()) {
			return;
		}

		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(clicked.getWorld());
		if (worldOpt.isEmpty()) {
			return;
		}

		final HellblockWorld<?> world = worldOpt.get();
		final Pos3 clickedPos = Pos3.from(clicked.getLocation());

		world.getBlockState(clickedPos).thenAcceptAsync(stateOpt -> {
			if (stateOpt.isEmpty())
				return;

			CustomBlockState state = stateOpt.get();
			String keyStr = state.type().type().asString().toLowerCase();

			if (!COCOA_STEM_KEYS.contains(keyStr))
				return;

			// Place cocoa if all conditions pass
			placeCocoaBean(player, event, clicked, face);
		});
	}

	/**
	 * Handles manual cocoa bean placement on a valid stem block by a player.
	 * 
	 * <p>
	 * Consumes one cocoa bean from the player's hand (unless in Creative mode),
	 * sets the adjacent block to COCOA, updates its facing direction, and plays a
	 * placement sound. Inventory is updated accordingly.
	 * </p>
	 *
	 * @param player the player placing the cocoa bean
	 * @param event  the interaction event triggering the placement
	 * @param stem   the log block being used as a placement surface
	 * @param face   the face of the stem where the cocoa bean should be attached
	 */
	private void placeCocoaBean(@NotNull Player player, @NotNull PlayerInteractEvent event, @NotNull Block stem,
			@NotNull BlockFace face) {
		event.setUseItemInHand(Result.ALLOW);

		if (player.getGameMode() != GameMode.CREATIVE && event.getItem() != null) {
			event.getItem().setAmount(Math.max(0, event.getItem().getAmount() - 1));
		}

		if (event.getHand() != null) {
			VersionHelper.getNMSManager().swingHand(player,
					event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
		}

		AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
				Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.wood.place"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));

		player.updateInventory();

		final World bukkitWorld = stem.getWorld();
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(bukkitWorld);
		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null)
			return;

		final HellblockWorld<?> world = worldOpt.get();
		final Pos3 targetPos = Pos3.from(stem.getRelative(face).getLocation());

		// Create a new CustomBlockState for cocoa
		CustomBlock cocoaBlock = CustomBlockTypes.COCOA;
		CustomBlockState cocoaState = cocoaBlock.createBlockState();

		// Apply facing using Directional interface
		if (cocoaBlock instanceof com.swiftlicious.hellblock.world.block.Directional directional) {
			directional.setFacing(cocoaState, face.getOppositeFace());
		}

		world.updateBlockState(targetPos, cocoaState).thenRun(() -> {
			playerPlacedCrops.add(targetPos);
			instance.debug("Cocoa beans placed at " + targetPos + " in world '" + world.worldName() + "' using "
					+ (event.getItem() != null ? event.getItem().getType() : "null") + " on " + stem.getType() + " → "
					+ cocoaState.type().type().key().asString());
		});
	}

	/**
	 * Handles the conversion of concrete powder to solid concrete when exposed to
	 * lava, mimicking a lava-water interaction mechanic unique to the Hellblock
	 * environment.
	 *
	 * <p>
	 * This class listens to block and entity events where concrete powder may come
	 * into contact with lava, and performs the appropriate conversion along with
	 * playing visual/auditory feedback.
	 * </p>
	 *
	 * <p>
	 * Supported interactions:
	 * <ul>
	 * <li>{@link org.bukkit.event.block.BlockFromToEvent} – when lava flows into
	 * adjacent concrete powder</li>
	 * <li>{@link org.bukkit.event.entity.EntityDropItemEvent} – when falling
	 * concrete powder lands near or on lava</li>
	 * </ul>
	 *
	 * <p>
	 * Conversion is triggered when concrete powder:
	 * <ul>
	 * <li>Is adjacent to lava during lava flow</li>
	 * <li>Falls next to or onto lava as a falling block entity</li>
	 * </ul>
	 *
	 * <p>
	 * Upon conversion, a lava extinguish sound is played for nearby players.
	 * </p>
	 */
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
					if (getConcretePowderBlocks().contains(event.getToBlock().getRelative(face).getType())) {
						event.getToBlock().getRelative(face)
								.setType(convertToConcrete(event.getToBlock().getRelative(face).getType()));
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
			if (!getConcretePowderBlocks().contains(fallingBlock.getBlockData().getMaterial())) {
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

		/**
		 * Retrieves the concrete powder block tag using a dynamic namespaced key
		 * lookup.
		 * <p>
		 * This avoids direct access to {@code Tag.CONCRETE_POWDER}, which may not be
		 * available in Bukkit 1.20.x.
		 *
		 * @return Set of concrete powder {@link Material}, or empty if tag is not
		 *         found.
		 */
		@NotNull
		public Set<Material> getConcretePowderBlocks() {
			NamespacedKey key = NamespacedKey.minecraft("concrete_powder");
			Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
			return tag != null ? tag.getValues() : Set.of();
		}

		/**
		 * Converts a concrete powder material into its corresponding solid concrete
		 * material.
		 * <p>
		 * If the given material is not a type of concrete powder, the original material
		 * is returned unchanged. This method supports all 16 standard Minecraft
		 * concrete colors.
		 *
		 * @param powderType the concrete powder material to convert
		 * @return the corresponding solid concrete material, or the original material
		 *         if no match is found
		 */
		@NotNull
		private Material convertToConcrete(@NotNull Material powderType) {
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

	/**
	 * Checks whether the given world is a valid Hellblock world.
	 *
	 * @param world the world to check, can be null
	 * @return true if the world is part of the Hellblock environment; false
	 *         otherwise
	 */
	private boolean isInCorrectWorld(@Nullable World world) {
		return world != null ? instance.getHellblockHandler().isInCorrectWorld(world) : false;
	}

	/**
	 * Checks whether the given player is currently in a Hellblock world.
	 *
	 * @param player the player to check, can be null
	 * @return true if the player's world is a Hellblock world; false otherwise
	 */
	private boolean isInCorrectWorld(@Nullable Player player) {
		return player != null ? instance.getHellblockHandler().isInCorrectWorld(player) : false;
	}

	/**
	 * Clears all internal moisture, revert, and block state caches for the given
	 * position.
	 * <p>
	 * This is typically called when farmland is broken or becomes invalid due to
	 * external events.
	 *
	 * @param pos the position of the block to remove from all related caches
	 */
	private void clearCachesAt(@NotNull Pos3 pos) {
		blockCache.remove(pos);
		revertCache.remove(pos);
		moistureCache.remove(pos);
	}

	/**
	 * Determines whether lava rain is actively occurring above the specified
	 * location.
	 * 
	 * This checks if the island is part of a lava rain event, and whether the block
	 * above the given location is exposed to air/passable material — allowing lava
	 * rain to reach it.
	 *
	 * @param islandId the island ID to check in
	 * @param location the location to check for lava rain exposure
	 * @return true if lava rain is occurring and the block is exposed; false
	 *         otherwise
	 */
	public boolean isLavaRainingAt(int islandId, @NotNull Location location) {
		if (!instance.getNetherWeatherManager().isWeatherActive(islandId, WeatherType.LAVA_RAIN))
			return false;

		Block highest = instance.getNetherWeatherManager().getHighestBlock(location);
		if (highest == null)
			return false;

		boolean passable = highest.isPassable() || highest.isEmpty() || highest.isLiquid();
		return passable && highest.getY() > location.getBlockY();
	}

	/**
	 * Plays a lava extinguishing sound at the given block's location. Typically
	 * used when concrete powder touches lava or during lava reactions.
	 *
	 * @param block the block where the extinguishing occurs
	 */
	private void playExtinguishSoundNear(@NotNull Block block) {
		AdventureHelper.playPositionalSound(block.getWorld(), block.getLocation(),
				Sound.sound(Key.key("minecraft:block.lava.extinguish"), Source.BLOCK, 1.0f, 1.0f));
	}

	/**
	 * Plays a visual and sound effect similar to using bone meal on a plant, in a
	 * version-safe way.
	 *
	 * <p>
	 * In Minecraft 1.17.1 and later, this uses {@code Effect.BONE_MEAL_USE} to
	 * trigger the native client-side effect.
	 * </p>
	 *
	 * <p>
	 * In Minecraft 1.17.0 and earlier, where the effect constant is not available,
	 * it falls back to manually spawning {@code VILLAGER_HAPPY} particles and
	 * playing the bone meal use sound to approximate the same feedback.
	 * </p>
	 *
	 * <p>
	 * This method ensures compatibility across versions without compile-time or
	 * runtime errors by dynamically resolving the effect constant if present.
	 * </p>
	 *
	 * @param world    the world in which to play the effect
	 * @param location the target location of the effect (typically the block being
	 *                 fertilized)
	 */
	public void playBoneMealUseEffect(@NotNull World world, @NotNull Location location) {
		Effect boneMealEffect = null;

		try {
			boneMealEffect = Effect.valueOf("BONE_MEAL_USE");
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			// Effect not available in this version
		}

		if (boneMealEffect != null) {
			try {
				world.playEffect(location, boneMealEffect, RandomUtils.generateRandomInt(2, 5));
				return;
			} catch (Throwable ignored) {
				// Fallback in case the effect is broken or unavailable
			}
		}

		// Manual fallback: green particle and bone meal use sound
		Location particleLoc = location.clone().add(0.5, 1.0, 0.5);
		world.spawnParticle(ParticleUtils.getParticle("VILLAGER_HAPPY"), particleLoc, 10, 0.4, 0.4, 0.4, 0.05);
		AdventureHelper.playPositionalSound(world, location,
				Sound.sound(Key.key("minecraft:item.bone_meal.use"), Source.BLOCK, 1.0f, 1.0f));
	}

	/**
	 * Retrieves the hoe items tag using a dynamic namespaced key lookup.
	 * <p>
	 * This avoids direct access to {@code Tag.ITEM_HOES}, which may not be
	 * available in Bukkit versions earlier than 1.19.4.
	 *
	 * @return Set of hoe {@link Material} items, or empty if tag is not found.
	 */
	@NotNull
	public Set<Material> getHoeItems() {
		NamespacedKey key = NamespacedKey.minecraft("hoes"); // Tag name for hoes
		Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
		return tag != null ? tag.getValues() : Set.of();
	}

	/**
	 * Checks if the given material is a wool carpet using version-safe reflection.
	 * <p>
	 * In Minecraft 1.19+, carpets are grouped under the {@code Tag.WOOL_CARPETS}
	 * tag, but in 1.18 and earlier, this tag was called {@code Tag.CARPETS}.
	 * <p>
	 * This method uses reflection to safely access the appropriate tag at runtime,
	 * avoiding compile-time errors when compiling against older versions of the
	 * API.
	 *
	 * @param material the material to check
	 * @return true if the material is a wool carpet; false otherwise
	 */
	@SuppressWarnings("unchecked")
	public boolean isWoolCarpet(@NotNull Material material) {
		try {
			// Try to get the newer WOOL_CARPETS tag first
			Field woolCarpetsField = Tag.class.getDeclaredField("WOOL_CARPETS");
			Tag<Material> woolCarpets = (Tag<Material>) woolCarpetsField.get(null);
			return woolCarpets.isTagged(material);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			// Fallback to CARPETS if WOOL_CARPETS doesn't exist
			try {
				Field carpetsField = Tag.class.getDeclaredField("CARPETS");
				Tag<Material> carpets = (Tag<Material>) carpetsField.get(null);
				return carpets.isTagged(material);
			} catch (NoSuchFieldException | IllegalAccessException ignored) {
			}
		}
		return false;
	}

	/**
	 * Resolves crop types by their unique string keys and delegates handling to
	 * appropriate logic.
	 *
	 * <p>
	 * This class allows dynamic registration and resolution of crop handlers based
	 * on string identifiers (e.g. "minecraft:sugar_cane"). It supports both vanilla
	 * and custom crop types (e.g. "hellblock:cactus").
	 * </p>
	 *
	 * <p>
	 * Usage:
	 * <ul>
	 * <li>Register crop keys and their handler functions via
	 * {@link #register(Set, BiConsumer)}</li>
	 * <li>Call {@link #handleIfKnown(PositionedBlock, HellblockWorld)} to process a
	 * block if it's a known crop</li>
	 * </ul>
	 * </p>
	 */
	public final class CropTypeResolver {

		private final Map<String, BiConsumer<PositionedBlock, HellblockWorld<?>>> handlers = new HashMap<>();

		/**
		 * Initializes the resolver and registers known crop handlers.
		 *
		 * @param farmingHandler the farming handler used to supply crop-specific
		 *                       methods
		 */
		public CropTypeResolver(@NotNull FarmingHandler farmingHandler) {
			register(Set.of("minecraft:nether_wart", "hellblock:nether_wart"), farmingHandler::handleNetherWart);
			register(Set.of("minecraft:sugar_cane", "hellblock:sugar_cane"), farmingHandler::handleSugarCane);
			register(Set.of("minecraft:cactus", "hellblock:cactus"), farmingHandler::handleCactus);
			register(Set.of("minecraft:red_mushroom", "hellblock:red_mushroom", "minecraft:brown_mushroom",
					"hellblock:brown_mushroom"), farmingHandler::handleMushrooms);
			register(Set.of("minecraft:bamboo", "hellblock:bamboo"), farmingHandler::handleBamboo);
			register(Set.of("minecraft:sweet_berry_bush", "hellblock:sweet_berry_bush"),
					farmingHandler::handleSweetBerryBush);
			register(Set.of("minecraft:cocoa", "hellblock:cocoa"), farmingHandler::handleCocoaBeans);
		}

		/**
		 * Registers one or more crop keys to a given crop handler.
		 *
		 * @param keys    a set of crop keys (e.g., "minecraft:sugar_cane",
		 *                "hellblock:bamboo")
		 * @param handler the crop handler logic to invoke for matching blocks
		 */
		private void register(@NotNull Set<String> keys,
				@NotNull BiConsumer<PositionedBlock, @NotNull HellblockWorld<?>> handler) {
			keys.forEach(key -> handlers.put(key.toLowerCase(), handler));
		}

		/**
		 * Executes the registered handler for a given block if its key is recognized.
		 *
		 * @param block the positioned block to evaluate
		 * @param world the Hellblock world context
		 */
		public void handleIfKnown(@NotNull PositionedBlock block, @NotNull HellblockWorld<?> world) {
			String key = block.state().type().type().key().asString().toLowerCase();
			BiConsumer<PositionedBlock, HellblockWorld<?>> handler = handlers.get(key);
			if (handler != null) {
				handler.accept(block, world);
			}
		}
	}
}