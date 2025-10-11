package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

			if (!(rawTag instanceof CompoundBinaryTag schematicTag)) {
				throw new IOException("Invalid schematic format — expected CompoundBinaryTag");
			}

			final short width = getShort(schematicTag, "Width");
			final short length = getShort(schematicTag, "Length");
			final short height = getShort(schematicTag, "Height");
			final int version = getInt(schematicTag, "Version");
			final byte[] blockdata = getByteArray(schematicTag, "BlockData");

			final CompoundBinaryTag palette = getCompound(schematicTag, "Palette");

			List<CompoundBinaryTag> entities = List.of();
			final BinaryTag entitiesTag = schematicTag.get("Entities");

			if (entitiesTag instanceof ListBinaryTag listTag && listTag.elementType() == BinaryTagTypes.COMPOUND) {
				entities = getCompoundList(schematicTag, "Entities");
			}

			switch (version) {
			case 1 -> {
				final List<CompoundBinaryTag> tileEntities = getCompoundList(schematicTag, "TileEntities");
				return new SchematicData(width, length, height, tileEntities, entities, blockdata, palette, version);
			}
			case 2 -> {
				final List<CompoundBinaryTag> blockEntities = getCompoundList(schematicTag, "BlockEntities");
				return new SchematicData(width, length, height, blockEntities, entities, blockdata, palette, version);
			}
			default -> {
				return new SchematicData(width, length, height, Collections.emptyList(), blockdata, palette, version);
			}
			}
		}
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
}