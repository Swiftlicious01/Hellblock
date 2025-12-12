package com.swiftlicious.hellblock.world;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Farmland;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public final class CustomBlockRenderer {

	private static final Map<Key, Material> MATERIAL_CACHE = new HashMap<>();

	private CustomBlockRenderer() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	public static void render(@Nullable CustomBlockState state, @NotNull Pos3 location, @NotNull World world) {
		Material material = resolveBlockType(state);
		Block bukkitBlock = location.toLocation(world).getBlock();
		if (state == null) {
			bukkitBlock.setType(Material.AIR, false);
			return;
		}
		if (bukkitBlock.getType() != material) {
			bukkitBlock.setType(material, false); // no physics
		}

		BlockData data = bukkitBlock.getBlockData();

		// Set block data values based on NBT
		if (data instanceof Ageable ageable) {
			BinaryTag tag = state.get("age");
			if (tag instanceof IntBinaryTag ageTag) {
				ageable.setAge(Math.min(ageTag.value(), ageable.getMaximumAge()));
				bukkitBlock.setBlockData(ageable, false);
			}
		} else if (data instanceof Farmland farmland) {
			BinaryTag tag = state.get("moisture");
			if (tag instanceof IntBinaryTag moistureTag) {
				farmland.setMoisture(Math.min(moistureTag.value(), farmland.getMaximumMoisture()));
				bukkitBlock.setBlockData(farmland, false);
			}
		} else if (data instanceof Directional directional) {
			BinaryTag tag = state.get("facing");
			if (tag instanceof StringBinaryTag facingTag) {
				BlockFace face = BlockFace.valueOf(facingTag.value().toUpperCase(Locale.ROOT));
				if (directional.getFaces().contains(face)) {
					directional.setFacing(face);
					bukkitBlock.setBlockData(directional, false);
				}
			}
		}
	}

	public static Material resolveBlockType(@Nullable CustomBlockState state) {
		if (state == null || state.type().type() == null) {
			return Material.AIR;
		}

		Key typeKey = state.type().type();
		if (MATERIAL_CACHE.containsKey(typeKey)) {
			return MATERIAL_CACHE.get(typeKey);
		}

		String key = typeKey.asString().toLowerCase(Locale.ROOT);
		Material result;

		// Handle known hellblock types
		switch (key) {
		case "hellblock:air" -> result = Material.AIR;
		case "hellblock:farmland" -> result = Material.FARMLAND;
		case "hellblock:red_mushroom" -> result = Material.RED_MUSHROOM;
		case "hellblock:brown_mushroom" -> result = Material.BROWN_MUSHROOM;
		case "hellblock:wheat" -> result = Material.WHEAT;
		case "hellblock:carrots" -> result = Material.CARROTS;
		case "hellblock:potatoes" -> result = Material.POTATOES;
		case "hellblock:beetroots" -> result = Material.BEETROOTS;
		case "hellblock:nether_wart" -> result = Material.NETHER_WART;
		case "hellblock:melon_stem" -> result = Material.MELON_STEM;
		case "hellblock:pumpkin_stem" -> result = Material.PUMPKIN_STEM;
		case "hellblock:attached_melon_stem" -> result = Material.ATTACHED_MELON_STEM;
		case "hellblock:attached_pumpkin_stem" -> result = Material.ATTACHED_PUMPKIN_STEM;
		case "hellblock:melon" -> result = Material.MELON;
		case "hellblock:pumpkin" -> result = Material.PUMPKIN;
		case "hellblock:cocoa" -> result = Material.COCOA;
		case "hellblock:sweet_berry_bush" -> result = Material.SWEET_BERRY_BUSH;
		case "hellblock:glow_sapling" -> result = RandomUtils.pickRandomSapling();
		case "hellblock:sugar_cane" -> result = Material.SUGAR_CANE;
		case "hellblock:bamboo" -> result = Material.BAMBOO;
		case "hellblock:cactus" -> result = Material.CACTUS;
		default -> {
			// Handle minecraft: key types
			if (key.startsWith("minecraft:")) {
				try {
					String name = key.substring("minecraft:".length()).toUpperCase(Locale.ROOT);
					result = Material.valueOf(name);
				} catch (IllegalArgumentException ex) {
					result = Material.AIR;
				}
			} else {
				result = Material.AIR;
			}
		}
		}

		MATERIAL_CACHE.put(typeKey, result);
		return result;
	}
}