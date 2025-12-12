package com.swiftlicious.hellblock.utils;

import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnchantmentUtils {

	private EnchantmentUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Attempts to retrieve a compatible {@link Enchantment} by trying multiple
	 * possible names.
	 * <p>
	 * Supports both legacy (pre-1.13) names via {@code Enchantment.getByName()} and
	 * modern namespaced keys (1.13+) via {@code Enchantment.getByKey()}. The method
	 * tries each name as a namespaced key first, then falls back to legacy name
	 * lookup.
	 *
	 * @param possibleNames An ordered list of potential enchantment names (e.g.,
	 *                      "unbreaking", "durability").
	 * @return The first matching {@link Enchantment}, or throw
	 *         IllegalArgumentException if none are found.
	 */
	@SuppressWarnings("deprecation")
	@Nullable
	public static Enchantment getCompatibleEnchantment(@NotNull String... possibleNames) {
		for (String name : possibleNames) {
			if (name == null || name.isBlank())
				continue;

			name = name.toLowerCase(Locale.ROOT);
			Enchantment enchantment = null;

			try {
				NamespacedKey key;

				// Handle full namespace (e.g. "minecraft:unbreaking" or "custom:lifesteal")
				if (name.contains(":")) {
					key = NamespacedKey.fromString(name);
				} else {
					// Default to minecraft namespace
					key = NamespacedKey.minecraft(name);
				}

				enchantment = Enchantment.getByKey(key);
				if (enchantment != null) {
					return enchantment;
				}
			} catch (IllegalArgumentException ignored) {
				// Ignore invalid namespace syntax, fall back to legacy
			}

			// Try legacy pre-1.13 names (e.g. "UNBREAKING", "EFFICIENCY")
			enchantment = Enchantment.getByName(name.toUpperCase(Locale.ROOT));
			if (enchantment != null) {
				return enchantment;
			}
		}

		return null; // Nothing matched
	}
}