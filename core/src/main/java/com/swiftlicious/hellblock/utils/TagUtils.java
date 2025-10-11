package com.swiftlicious.hellblock.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import com.swiftlicious.hellblock.creation.item.tag.TagValueType;
import com.swiftlicious.hellblock.utils.extras.Pair;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

/**
 * Utility class for handling tag values.
 */
public class TagUtils {

	/**
	 * Parses a string into a pair containing a {@link TagValueType} and its
	 * associated data. The input string should be in the format "(type) value" or
	 * just "value" for plain strings.
	 *
	 * Examples: - "(int) 42" -> Pair.of(TagValueType.INT, "42") - "hello" ->
	 * Pair.of(TagValueType.STRING, "hello")
	 *
	 * @param str the input string
	 * @return a pair of TagValueType and string value
	 * @throws IllegalArgumentException if the input format is invalid or the tag
	 *                                  type is unknown
	 */
	public static Pair<TagValueType, String> toTypeAndData(String str) {
		if (str == null || str.isEmpty()) {
			return Pair.of(TagValueType.STRING, "");
		}

		final String[] parts = str.split(" ", 2);

		// Case: no type prefix, just raw string
		if (parts.length == 1 || !parts[0].startsWith("(") || !parts[0].endsWith(")")) {
			return Pair.of(TagValueType.STRING, str);
		}

		try {
			final String typeName = parts[0].substring(1, parts[0].length() - 1).trim().toUpperCase(Locale.ENGLISH);
			final TagValueType valueType = TagValueType.valueOf(typeName);
			final String data = parts.length == 2 ? parts[1] : "";
			return Pair.of(valueType, data);
		} catch (IllegalArgumentException ex) {
			// fallback to STRING if unknown type
			return Pair.of(TagValueType.STRING, str);
		}
	}

	/**
	 * Deserialize a byte array into an Adventure CompoundBinaryTag.
	 *
	 * @param bytes The byte array to deserialize.
	 * @return The deserialized CompoundBinaryTag.
	 */
	public static CompoundBinaryTag fromBytes(byte[] bytes) {
		try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
			return BinaryTagIO.reader().read(in);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read BinaryTag from bytes", e);
		}
	}

	/**
	 * Serialize an Adventure CompoundBinaryTag into a byte array.
	 *
	 * @param tag The CompoundBinaryTag to serialize.
	 * @return A byte array representing the tag.
	 */
	public static byte[] toBytes(CompoundBinaryTag tag) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			BinaryTagIO.writer().write(tag, out); // Provide an empty root name
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Failed to write BinaryTag to bytes", e);
		}
	}

	/**
	 * Deep clone for a CompoundBinaryTag using Adventure NBT.
	 *
	 * @param original The original compound tag.
	 * @return A deep-cloned CompoundBinaryTag.
	 */
	public static CompoundBinaryTag deepClone(CompoundBinaryTag original) {
		final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
		original.keySet().forEach(key -> {
			final BinaryTag tag = original.get(key);
			if (tag != null) {
				builder.put(key, deepCloneTag(tag));
			}
		});
		return builder.build();
	}

	/**
	 * Deep clones a BinaryTag. Supports nested compounds and lists.
	 *
	 * @param tag The BinaryTag to clone.
	 * @return A cloned tag.
	 */
	private static BinaryTag deepCloneTag(BinaryTag tag) {
		if (tag instanceof CompoundBinaryTag compound) {
			return deepClone(compound); // Recursive
		} else if (tag instanceof ListBinaryTag list) {
			final BinaryTagType<?> elementType = list.elementType();

			// Create a builder for the specific type
			@SuppressWarnings("unchecked")
			final ListBinaryTag.Builder<BinaryTag> builder = (ListBinaryTag.Builder<BinaryTag>) ListBinaryTag
					.builder(elementType);

			for (BinaryTag element : list) {
				builder.add(deepCloneTag(element)); // Recursive clone
			}

			return builder.build();
		}

		// Primitive tags are immutable and can be reused directly
		return tag;
	}
}