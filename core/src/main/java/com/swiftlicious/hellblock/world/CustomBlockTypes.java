package com.swiftlicious.hellblock.world;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Material;

import com.swiftlicious.hellblock.world.block.crop.CustomBeetrootBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomCarrotBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomCocoaBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomFarmlandBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomMelonAttachedStemBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomMelonStemBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomMushroomBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomNetherWartBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomPotatoBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomPumpkinAttachedStemBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomPumpkinStemBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomSaplingBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomSweetBerryBushBlock;
import com.swiftlicious.hellblock.world.block.crop.CustomWheatBlock;

import net.kyori.adventure.key.Key;

public final class CustomBlockTypes {

	// Internal registry
	private static final Map<Key, CustomBlock> REGISTRY = new HashMap<>();
	private static final Map<Material, CustomBlock> VANILLA_BLOCK_CACHE = new EnumMap<>(Material.class);

	public static Map<Key, CustomBlock> registry() {
		return Collections.unmodifiableMap(REGISTRY);
	}

	private static CustomBlock register(CustomBlock block) {
		Objects.requireNonNull(block, "Block cannot be null");
		Key key = block.type().key();
		if (REGISTRY.containsKey(key)) {
			throw new IllegalStateException("Duplicate custom block ID: " + key);
		}
		REGISTRY.put(key, block);
		return block;
	}

	// Air
	public static final CustomBlock AIR = register(new CustomBlock(Key.key("hellblock:air")));

	// Farmland
	public static final CustomBlock FARMLAND = register(new CustomFarmlandBlock(Key.key("hellblock:farmland")));

	// Mushrooms
	public static final CustomBlock RED_MUSHROOM = register(new CustomMushroomBlock(Key.key("hellblock:red_mushroom")));
	public static final CustomBlock BROWN_MUSHROOM = register(
			new CustomMushroomBlock(Key.key("hellblock:brown_mushroom")));

	// Crops
	public static final CustomBlock WHEAT = register(new CustomWheatBlock(Key.key("hellblock:wheat")));
	public static final CustomBlock CARROTS = register(new CustomCarrotBlock(Key.key("hellblock:carrots")));
	public static final CustomBlock POTATOES = register(new CustomPotatoBlock(Key.key("hellblock:potatoes")));
	public static final CustomBlock BEETROOTS = register(new CustomBeetrootBlock(Key.key("hellblock:beetroots")));
	public static final CustomBlock NETHER_WART = register(new CustomNetherWartBlock(Key.key("hellblock:nether_wart")));

	// Melon & Pumpkin Stems
	public static final CustomBlock MELON_STEM = register(new CustomMelonStemBlock(Key.key("hellblock:melon_stem")));
	public static final CustomBlock PUMPKIN_STEM = register(
			new CustomPumpkinStemBlock(Key.key("hellblock:pumpkin_stem")));
	public static final CustomBlock MELON_ATTACHED_STEM = register(
			new CustomMelonAttachedStemBlock(Key.key("hellblock:attached_melon_stem")));
	public static final CustomBlock PUMPKIN_ATTACHED_STEM = register(
			new CustomPumpkinAttachedStemBlock(Key.key("hellblock:attached_pumpkin_stem")));

	public static final CustomBlock MELON = register(new CustomBlock(Key.key("hellblock:melon")));
	public static final CustomBlock PUMPKIN = register(new CustomBlock(Key.key("hellblock:pumpkin")));

	// Other crops
	public static final CustomBlock COCOA = register(new CustomCocoaBlock(Key.key("hellblock:cocoa")));
	public static final CustomBlock SWEET_BERRY_BUSH = register(
			new CustomSweetBerryBushBlock(Key.key("hellblock:sweet_berry_bush")));

	// Saplings
	public static final CustomBlock GLOW_SAPLING = register(new CustomSaplingBlock(Key.key("hellblock:glow_sapling")));

	// Vertical crops
	public static final CustomBlock SUGAR_CANE = register(new CustomBlock(Key.key("hellblock:sugar_cane")));
	public static final CustomBlock BAMBOO = register(new CustomBlock(Key.key("hellblock:bamboo")));
	public static final CustomBlock CACTUS = register(new CustomBlock(Key.key("hellblock:cactus")));

	public static CustomBlock fromMaterial(Material material) {
		if (material == null || material == Material.AIR) {
			return AIR;
		}

		// Fast path cache
		if (VANILLA_BLOCK_CACHE.containsKey(material)) {
			return VANILLA_BLOCK_CACHE.get(material);
		}

		// Try hellblock:<id> first
		String lowerName = material.name().toLowerCase(Locale.ROOT);
		Key hellblockKey = Key.key("hellblock:" + lowerName);
		CustomBlock block = REGISTRY.get(hellblockKey);

		// If not found, fall back to minecraft:<id>
		if (block == null) {
			Key minecraftKey = Key.key("minecraft:" + lowerName);
			block = new CustomBlock(minecraftKey);
		}

		VANILLA_BLOCK_CACHE.put(material, block);
		return block;
	}

	private CustomBlockTypes() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}
}