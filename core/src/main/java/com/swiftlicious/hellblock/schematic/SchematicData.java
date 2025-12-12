package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.swiftlicious.hellblock.HellblockPlugin;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;

public class SchematicData {
	public final short width;
	public final short length;
	public final short height;
	public List<CompoundBinaryTag> tileEntities;
	public List<CompoundBinaryTag> entities;
	public byte[] blockdata;
	public CompoundBinaryTag palette;
	public Integer version;

	public SchematicData(short width, short length, short height, List<CompoundBinaryTag> tileEntities,
			List<CompoundBinaryTag> entities, byte[] blockdata, CompoundBinaryTag palette, Integer version) {
		this.width = width;
		this.length = length;
		this.height = height;
		this.tileEntities = tileEntities;
		this.entities = entities != null ? entities : Collections.emptyList();
		this.blockdata = blockdata;
		this.palette = palette;
		this.version = version;
	}

	// Convenience constructor
	public SchematicData(short width, short length, short height, List<CompoundBinaryTag> tileEntities,
			byte[] blockdata, CompoundBinaryTag palette, Integer version) {
		this(width, length, height, tileEntities, null, blockdata, palette, version);
	}

	/**
	 * Load a schematic from a file.
	 *
	 * @param file The schematic file to load.
	 * @return The loaded SchematicData.
	 * @throws IOException If an I/O error occurs or the file format is invalid.
	 */
	public static SchematicData loadSchematic(File file) throws IOException {
		try (InputStream stream = new FileInputStream(file)) {
			final BinaryTag rawTag = BinaryTagIO.reader().read(stream);

			if (!(rawTag instanceof CompoundBinaryTag tag)) {
				throw new IOException("Invalid schematic format — expected CompoundBinaryTag");
			}

			// === Detect legacy `.schematic` format ===
			if (tag.contains("Blocks") && tag.contains("Data")) {
				return parseLegacySchematic(tag);
			}

			// === Modern Sponge format ===
			final short width = getShort(tag, "Width");
			final short length = getShort(tag, "Length");
			final short height = getShort(tag, "Height");
			final int version = getInt(tag, "Version");
			final byte[] blockdata = getByteArray(tag, "BlockData");
			final CompoundBinaryTag palette = getCompound(tag, "Palette");

			List<CompoundBinaryTag> entities = List.of();
			if (tag.get("Entities") instanceof ListBinaryTag list && list.elementType() == BinaryTagTypes.COMPOUND) {
				entities = getCompoundList(tag, "Entities");
			}

			switch (version) {
			case 1 -> {
				final List<CompoundBinaryTag> tileEntities = getCompoundList(tag, "TileEntities");
				return new SchematicData(width, length, height, tileEntities, entities, blockdata, palette, version);
			}
			case 2 -> {
				final List<CompoundBinaryTag> blockEntities = getCompoundList(tag, "BlockEntities");
				return new SchematicData(width, length, height, blockEntities, entities, blockdata, palette, version);
			}
			default -> {
				return new SchematicData(width, length, height, Collections.emptyList(), blockdata, palette, version);
			}
			}
		}
	}

	private static SchematicData parseLegacySchematic(CompoundBinaryTag tag) {
		final short width = getShort(tag, "Width");
		final short height = getShort(tag, "Height");
		final short length = getShort(tag, "Length");

		final byte[] blocks = getByteArray(tag, "Blocks");
		final byte[] data = getByteArray(tag, "Data");

		final List<CompoundBinaryTag> tileEntities = getCompoundList(tag, "TileEntities");

		// Create palette from block IDs (0–255) + data (0–15)
		Map<String, Integer> reversedPalette = new HashMap<>();
		byte[] blockdata = new byte[blocks.length];
		CompoundBinaryTag.Builder paletteBuilder = CompoundBinaryTag.builder();

		for (int i = 0; i < blocks.length; i++) {
			int id = blocks[i] & 0xFF;
			int meta = data[i] & 0x0F;
			String legacyName = LegacyBlockConverter.getBlockData(id, meta); // We'll define this next

			int paletteId = reversedPalette.computeIfAbsent(legacyName, key -> {
				int newId = reversedPalette.size();
				paletteBuilder.putInt(key, newId);
				return newId;
			});

			blockdata[i] = (byte) paletteId;
		}

		CompoundBinaryTag palette = paletteBuilder.build();

		// Use version 1 for compatibility
		return new SchematicData(width, length, height, tileEntities, blockdata, palette, 1);
	}

	private static short getShort(CompoundBinaryTag tag, String key) {
		final BinaryTag t = tag.get(key);
		if (!(t instanceof ShortBinaryTag sbt)) {
			throw new IllegalArgumentException("Tag \"" + key + "\" is not a Short");
		}
		return sbt.value();
	}

	private static int getInt(CompoundBinaryTag tag, String key) {
		final BinaryTag t = tag.get(key);
		if (!(t instanceof IntBinaryTag ibt)) {
			throw new IllegalArgumentException("Tag \"" + key + "\" is not an Int");
		}
		return ibt.value();
	}

	private static byte[] getByteArray(CompoundBinaryTag tag, String key) {
		final BinaryTag t = tag.get(key);
		if (!(t instanceof ByteArrayBinaryTag bbt)) {
			throw new IllegalArgumentException("Tag \"" + key + "\" is not a ByteArray");
		}
		return bbt.value();
	}

	private static CompoundBinaryTag getCompound(CompoundBinaryTag tag, String key) {
		final BinaryTag t = tag.get(key);
		if (!(t instanceof CompoundBinaryTag cbt)) {
			throw new IllegalArgumentException("Tag \"" + key + "\" is not a CompoundBinaryTag");
		}
		return cbt;
	}

	private static List<CompoundBinaryTag> getCompoundList(CompoundBinaryTag tag, String key) {
		final BinaryTag t = tag.get(key);
		if (!(t instanceof ListBinaryTag list)) {
			throw new IllegalArgumentException("Tag \"" + key + "\" is not a List");
		}
		if (list.elementType() != BinaryTagTypes.COMPOUND) {
			throw new IllegalArgumentException("List \"" + key + "\" does not contain Compound tags");
		}
		final List<CompoundBinaryTag> result = new ArrayList<>();
		for (BinaryTag item : list) {
			result.add((CompoundBinaryTag) item);
		}
		return result;
	}

	/**
	 * Retrieve a child tag from a CompoundBinaryTag with proper type checking.
	 *
	 * @param <T>      The expected tag type (e.g., IntBinaryTag.class).
	 * @param tag      The parent CompoundBinaryTag.
	 * @param key      The key to fetch.
	 * @param expected The expected class of the tag.
	 * @return The tag casted to the expected type.
	 * @throws IllegalArgumentException if the key is missing or the type
	 *                                  mismatches.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends BinaryTag> T getChildTag(CompoundBinaryTag tag, String key, Class<T> expected) {
		final BinaryTag child = tag.get(key);

		if (child == null) {
			throw new IllegalArgumentException("Missing tag: " + key);
		}

		if (!expected.isInstance(child)) {
			throw new IllegalArgumentException("Tag \"" + key + "\" is not of type " + expected.getSimpleName());
		}

		return (T) child;
	}

	/**
	 * Safely retrieve an optional child tag from a CompoundBinaryTag.
	 *
	 * @param <T>      The expected tag type (e.g., IntBinaryTag.class).
	 * @param tag      The CompoundBinaryTag containing child tags.
	 * @param key      The key of the child tag.
	 * @param expected The expected class of the tag.
	 * @return The tag casted to the expected type, or null if not present or
	 *         mismatched.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends BinaryTag> T getOptionalChildTag(CompoundBinaryTag tag, String key, Class<T> expected) {
		final BinaryTag child = tag.get(key);

		if (child == null || !expected.isInstance(child)) {
			return null;
		}

		return (T) child;
	}

	public class LegacyBlockConverter {

		private static final Map<Integer, Map<Integer, String>> blockIdMetaToName = new HashMap<>();

		private LegacyBlockConverter() {
			// Static utility class; prevent instantiation
		}

		static {
			// === Stone (ID 1) ===
			blockIdMetaToName.put(1,
					Map.of(0, "minecraft:stone", 1, "minecraft:granite", 2, "minecraft:polished_granite", 3,
							"minecraft:diorite", 4, "minecraft:polished_diorite", 5, "minecraft:andesite", 6,
							"minecraft:polished_andesite"));

			// === Grass (ID 2) ===
			blockIdMetaToName.put(2, Map.of(0, "minecraft:grass_block"));

			// === Dirt (ID 3) ===
			blockIdMetaToName.put(3, Map.of(0, "minecraft:dirt", 1, "minecraft:coarse_dirt", 2, "minecraft:podzol"));

			blockIdMetaToName.put(4, Map.of(0, "minecraft:cobblestone")); // Cobblestone
			blockIdMetaToName.put(6, Map.of(0, "minecraft:oak_sapling")); // Sapling (simplified)
			blockIdMetaToName.put(20, Map.of(0, "minecraft:glass")); // Glass

			// === Planks (ID 5) ===
			Map<Integer, String> planks = new HashMap<>();
			planks.put(0, "minecraft:oak_planks");
			planks.put(1, "minecraft:spruce_planks");
			planks.put(2, "minecraft:birch_planks");
			planks.put(3, "minecraft:jungle_planks");
			planks.put(4, "minecraft:acacia_planks");
			planks.put(5, "minecraft:dark_oak_planks");
			blockIdMetaToName.put(5, planks);

			// === Wool (ID 35) ===
			Map<Integer, String> wool = new HashMap<>();
			String[] woolColors = { "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
					"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black" };
			for (int i = 0; i < woolColors.length; i++) {
				wool.put(i, "minecraft:" + woolColors[i] + "_wool");
			}
			blockIdMetaToName.put(35, wool);

			// === Terracotta (ID 159) ===
			Map<Integer, String> terracotta = new HashMap<>();
			for (int i = 0; i < woolColors.length; i++) {
				terracotta.put(i, "minecraft:" + woolColors[i] + "_terracotta");
			}
			blockIdMetaToName.put(159, terracotta);

			// === Stained Glass (ID 95) ===
			Map<Integer, String> stainedGlass = new HashMap<>();
			for (int i = 0; i < woolColors.length; i++) {
				stainedGlass.put(i, "minecraft:" + woolColors[i] + "_stained_glass");
			}
			blockIdMetaToName.put(95, stainedGlass);

			// === Stone Bricks (ID 98) ===
			blockIdMetaToName.put(98, Map.of(0, "minecraft:stone_bricks", 1, "minecraft:mossy_stone_bricks", 2,
					"minecraft:cracked_stone_bricks", 3, "minecraft:chiseled_stone_bricks"));

			// === Quartz (ID 155) ===
			blockIdMetaToName.put(155, Map.of(0, "minecraft:quartz_block", 1, "minecraft:chiseled_quartz_block", 2,
					"minecraft:quartz_pillar"));

			// === Log (ID 17) — axis not stored in metadata in legacy ===
			Map<Integer, String> logs = new HashMap<>();
			logs.put(0, "minecraft:oak_log");
			logs.put(1, "minecraft:spruce_log");
			logs.put(2, "minecraft:birch_log");
			logs.put(3, "minecraft:jungle_log");
			blockIdMetaToName.put(17, logs);

			// === Leaves (ID 18) — simplified ===
			blockIdMetaToName.put(18, Map.of(0, "minecraft:oak_leaves", 1, "minecraft:spruce_leaves", 2,
					"minecraft:birch_leaves", 3, "minecraft:jungle_leaves"));

			// === Slabs (ID 44) — bottom only ===
			Map<Integer, String> slabs = new HashMap<>();
			slabs.put(0, "minecraft:stone_slab[type=bottom]");
			slabs.put(1, "minecraft:sandstone_slab[type=bottom]");
			slabs.put(2, "minecraft:oak_slab[type=bottom]");
			slabs.put(3, "minecraft:cobblestone_slab[type=bottom]");
			slabs.put(4, "minecraft:brick_slab[type=bottom]");
			slabs.put(5, "minecraft:stone_brick_slab[type=bottom]");
			slabs.put(6, "minecraft:nether_brick_slab[type=bottom]");
			slabs.put(7, "minecraft:quartz_slab[type=bottom]");
			blockIdMetaToName.put(44, slabs);

			// === Stairs (ID 53, etc.) — only direction/half supported ===
			blockIdMetaToName.put(53, Map.of( // Oak stairs
					0, "minecraft:oak_stairs[facing=east,half=bottom]", 1,
					"minecraft:oak_stairs[facing=west,half=bottom]", 2,
					"minecraft:oak_stairs[facing=south,half=bottom]", 3,
					"minecraft:oak_stairs[facing=north,half=bottom]", 4, "minecraft:oak_stairs[facing=east,half=top]",
					5, "minecraft:oak_stairs[facing=west,half=top]", 6, "minecraft:oak_stairs[facing=south,half=top]",
					7, "minecraft:oak_stairs[facing=north,half=top]"));

			// === Air (ID 0) ===
			blockIdMetaToName.put(0, Map.of(0, "minecraft:air"));
		}

		/**
		 * Converts legacy block ID + metadata to modern BlockData string.
		 *
		 * @param id   Legacy block ID
		 * @param meta Metadata (0–15)
		 * @return BlockData string like "minecraft:oak_stairs[facing=north,half=top]"
		 */
		public static String getBlockData(int id, int meta) {
			Map<Integer, String> metaMap = blockIdMetaToName.get(id);
			if (metaMap != null) {
				String data = metaMap.get(meta);
				if (data != null) {
					return data;
				}
				// Fallback to meta = 0
				String fallback = metaMap.get(0);
				if (fallback != null) {
					HellblockPlugin.getInstance().getPluginLogger()
							.warn("Missing variant for ID %d meta %d, using meta 0.".formatted(id, meta));
					return fallback;
				}
			}

			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Unknown legacy block: ID %d meta %d".formatted(id, meta));
			return "minecraft:air";
		}
	}
}