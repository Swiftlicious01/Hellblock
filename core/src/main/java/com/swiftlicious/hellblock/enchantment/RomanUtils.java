package com.swiftlicious.hellblock.enchantment;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for converting integers to their Roman numeral representation.
 * 
 * <p>
 * This class provides a simple method to convert small integer levels
 * (typically 1–10) into corresponding Roman numerals. For values beyond the
 * supported range, it falls back to returning the integer as a string.
 * </p>
 *
 * <p>
 * Example:
 * 
 * <pre>
 * RomanUtils.toRoman(4); // returns "IV"
 * RomanUtils.toRoman(12); // returns "12"
 * </pre>
 * </p>
 */
public class RomanUtils {

	private RomanUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Predefined Roman numeral representations for levels 1 through 10. The index
	 * corresponds to (level - 1).
	 */
	private static final String[] ROMAN = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };

	/**
	 * Converts an integer level into a Roman numeral string.
	 *
	 * @param level The integer level to convert (e.g., enchantment level).
	 * @return The Roman numeral representation if level is between 1–10; an empty
	 *         string if level ≤ 0; otherwise, the numeric level as a string
	 *         (fallback for >10).
	 */
	@NotNull
	public static String toRoman(int level) {
		if (level <= 0)
			return ""; // Return empty for invalid/non-positive levels
		if (level <= ROMAN.length)
			return ROMAN[level - 1]; // Lookup pre-defined Roman numeral
		return String.valueOf(level); // Fallback for levels greater than 10
	}
}