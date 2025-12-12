package com.swiftlicious.hellblock.utils;

import java.lang.reflect.Method;
import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.handlers.VersionHelper;

public class PotionUtils {

	private PotionUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Resolves a compatible {@link PotionEffectType} from a list of possible names.
	 * <p>
	 * Tries each name in order using {@code PotionEffectType.getByName()}, which
	 * handles both legacy (pre-1.13) and modern names depending on the server
	 * version.
	 *
	 * @param possibleNames Ordered list of effect names (e.g., "SLOWNESS", "SLOW").
	 * @return The first matching {@link PotionEffectType}, or throw
	 *         IllegalArgumentException if none found.
	 *
	 * @see PotionEffectType#getByName(String)
	 */
	@SuppressWarnings("deprecation")
	@NotNull
	public static PotionEffectType getCompatiblePotionEffectType(@NotNull String... possibleNames) {
		for (String raw : possibleNames) {
			String name = raw.trim().toUpperCase(Locale.ROOT);
			PotionEffectType type = null;

			// --- Modern lookup (only run on 1.18+ where method exists) ---
			if (VersionHelper.isVersionNewerThan1_18()) {
				try {
					// Use reflection to avoid compile-time linking on 1.17 jars
					Method getByKey = PotionEffectType.class.getMethod("getByKey", NamespacedKey.class);
					type = (PotionEffectType) getByKey.invoke(null,
							NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
				} catch (Throwable ignored) {
					// safely ignore if not found or fails
				}

				if (type != null)
					return type;
			}

			// --- Fallback for all older versions ---
			type = PotionEffectType.getByName(name);
			if (type != null)
				return type;
		}

		throw new IllegalArgumentException("Unknown potion effect type: " + String.join(", ", possibleNames));
	}
}