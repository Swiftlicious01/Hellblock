package com.swiftlicious.hellblock.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class StringUtils {

	private StringUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	public static boolean isCapitalLetter(String item) {
		final char[] chars = item.toCharArray();
		for (char character : chars) {
			if ((character < 65 || character > 90) && character != 95) {
				return false;
			}
		}
		return true;
	}

	public static String toLowerCase(String input) {
		final char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];
			if (c >= 'A' && c <= 'Z') {
				chars[i] = (char) (c + 32);
			}
		}
		return new String(chars);
	}

	public static String toCamelCase(String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}

		// Split on both underscore and dash
		final String[] parts = s.split("[-_]+");
		final StringBuilder camelCaseString = new StringBuilder();

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (part.isEmpty())
				continue;

			if (i > 0) {
				camelCaseString.append(' '); // insert space between words
			}
			camelCaseString.append(toProperCase(part));
		}

		return camelCaseString.toString();
	}

	/**
	 * Converts a name like IRON_INGOT into Iron Ingot to improve readability
	 *
	 * @param ugly The string such as IRON_INGOT
	 * @return A nicer version, such as Iron Ingot
	 *
	 *         Credits to mikenon on GitHub!
	 */
	public static String prettifyText(ItemStack ugly) {
		final StringBuilder fin = new StringBuilder();
		final String pretty = ugly.getType().toString().toLowerCase(java.util.Locale.ROOT);
		if (pretty.contains("_")) {
			final String[] splt = pretty.split("_");
			int i = 0;
			for (String s : splt) {
				i += 1;
				fin.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
				if (i < splt.length) {
					fin.append(" ");
				}
			}
		} else {
			fin.append(Character.toUpperCase(pretty.charAt(0))).append(pretty.substring(1));
		}
		return fin.toString();
	}

	public static String toProperCase(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	public static boolean isNotInteger(String s) {
		return !isInteger(s, 10);
	}

	public static boolean isInteger(String s, int radix) {
		if (s.isEmpty()) {
			return false;
		}
		for (int i = 0; i < s.length(); i++) {
			if (i == 0 && s.charAt(i) == '-') {
				if (s.length() == 1) {
					return false;
				} else {
					continue;
				}
			}
			if (Character.digit(s.charAt(i), radix) < 0) {
				return false;
			}
		}
		return true;
	}

	public static String serializeLoc(@NotNull Location loc) {
		if (loc.getWorld() == null) {
			return null;
		}
		return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
	}

	public static Location deserializeLoc(String serializedLoc) {
		if (serializedLoc == null || "".equals(serializedLoc.trim())) {
			return null;
		}
		final String[] locParts = serializedLoc.split(":");
		if (locParts.length != 4) {
			return null;
		}
		final World world = Bukkit.getWorld(locParts[0]);
		final double x = Double.parseDouble(locParts[1]);
		final double y = Double.parseDouble(locParts[2]);
		final double z = Double.parseDouble(locParts[3]);
		return new Location(world, x, y, z);
	}
}