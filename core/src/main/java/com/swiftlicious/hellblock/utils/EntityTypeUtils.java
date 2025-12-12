package com.swiftlicious.hellblock.utils;

import java.util.Locale;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class EntityTypeUtils {

	private EntityTypeUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Attempts to retrieve a compatible {@link EntityType} by trying multiple
	 * possible enum names.
	 * <p>
	 * This method is useful for supporting multiple Minecraft/Spigot versions where
	 * entity type names may have changed over time (e.g., "MOOSHROOM" vs
	 * "MUSHROOM_COW"). It iterates through the provided names in order and returns
	 * the first one that is valid for the current server version.
	 *
	 * @param possibleNames An ordered list of potential entity type names to try.
	 * @return The first matching {@link EntityType}, or throw
	 *         IllegalArgumentException if none are valid.
	 *
	 * @see org.bukkit.entity.EntityType
	 */
	@NotNull
	public static EntityType getCompatibleEntityType(@NotNull String... possibleNames) {
		for (String raw : possibleNames) {
			String name = raw.trim().toUpperCase(Locale.ROOT);

			// --- Try standard EntityType lookup (case-insensitive) ---
			try {
				return EntityType.valueOf(name);
			} catch (IllegalArgumentException ignored) {
			}

			// --- Try namespaced fallback ---
			if (name.startsWith("MINECRAFT:"))
				name = name.substring("MINECRAFT:".length());

			try {
				return EntityType.valueOf(name.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
			}

			// --- Try a few known aliases for version differences ---
			if (name.equalsIgnoreCase("PIGZOMBIE"))
				return EntityType.ZOMBIFIED_PIGLIN;
			if (name.equalsIgnoreCase("MAGMA_CUBE"))
				return EntityType.MAGMA_CUBE;
		}

		throw new IllegalArgumentException("Unknown entity type: " + String.join(", ", possibleNames));
	}
}