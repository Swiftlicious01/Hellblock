package com.swiftlicious.hellblock.utils;

import java.util.List;

/**
 * Utility class for handling operations related to lists.
 */
public class ListUtils {

	private ListUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Converts an object to a list of strings. If the object is a string, it
	 * returns a list containing the string. If the object is a list, it casts and
	 * returns the list as a list of strings.
	 *
	 * @param obj the object to convert
	 * @return the resulting list of strings
	 * @throws IllegalArgumentException if the object cannot be converted to a list
	 *                                  of strings
	 */
	@SuppressWarnings("unchecked")
	public static List<String> toList(final Object obj) {
		if (obj instanceof String s) {
			return List.of(s);
		} else if (obj instanceof List<?> list) {
			return (List<String>) list;
		}
		throw new IllegalArgumentException(
				"Invalid value found. Cannot convert " + obj.getClass().getSimpleName() + " to a list");
	}
}