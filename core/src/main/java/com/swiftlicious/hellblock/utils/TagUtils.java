package com.swiftlicious.hellblock.utils;

import java.util.Locale;

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
}