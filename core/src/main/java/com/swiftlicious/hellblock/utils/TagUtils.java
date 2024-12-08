package com.swiftlicious.hellblock.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.TagType;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.swiftlicious.hellblock.creation.item.tag.TagValueType;
import com.swiftlicious.hellblock.utils.extras.Pair;

import org.jetbrains.annotations.ApiStatus;

/**
 * Utility class for handling tag values.
 */
@ApiStatus.Internal
public class TagUtils {

	/**
	 * Parses a string into a pair containing a {@link TagValueType} and its
	 * associated data. The input string should be in the format "&lt;type&gt;
	 * data".
	 *
	 * @param str the string to be parsed
	 * @return a {@link Pair} containing the {@link TagValueType} and its associated
	 *         data
	 * @throws IllegalArgumentException if the input string is in an invalid format
	 */
	public static Pair<TagValueType, String> toTypeAndData(String str) {
		String[] parts = str.split(" ", 2);
		if (parts.length == 1) {
			return Pair.of(TagValueType.STRING, str);
		}
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid value format: " + str);
		}
		if (parts[0].startsWith("(") && parts[0].endsWith(")")) {
			TagValueType type = TagValueType
					.valueOf(parts[0].substring(1, parts[0].length() - 1).toUpperCase(Locale.ENGLISH));
			String data = parts[1];
			return Pair.of(type, data);
		} else {
			return Pair.of(TagValueType.STRING, str);
		}
	}

	public static Tag<?> fromBytes(byte[] bytes) {
		try {
			try (NBTInputStream nbtInputStream = new NBTInputStream(new ByteArrayInputStream(bytes),
					NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN)) {
				return nbtInputStream.readTag();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] toBytes(Tag<?> tag) {
		try {
			ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
			try (NBTOutputStream outStream = new NBTOutputStream(outByteStream, NBTInputStream.NO_COMPRESSION,
					ByteOrder.BIG_ENDIAN)) {
				outStream.writeTag(tag);
				return outByteStream.toByteArray();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// There is a problem with this method, which comes from flownbt itself
	// This method cannot perform a deep copy of a compound nested in a list.
	public static CompoundMap deepClone(CompoundMap initial) {
		CompoundMap clone = new CompoundMap();
		for (Tag<?> tag : initial) {
			if (tag.getType() == TagType.TAG_COMPOUND) {
				clone.put(deepClone((CompoundTag) tag));
			} else {
				clone.put(tag.clone());
			}
		}
		return clone;
	}

	public static CompoundTag deepClone(CompoundTag initial) {
		return new CompoundTag(initial.getName(), deepClone(initial.getValue()));
	}
}