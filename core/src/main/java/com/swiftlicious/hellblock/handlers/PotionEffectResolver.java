package com.swiftlicious.hellblock.handlers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("rawtypes")
public class PotionEffectResolver {

	private static Registry potionEffectRegistry;

	private static final Method POTION_EFFECT_GET_KEY;

	static {
		Method getKeyMethod = null;
		try {
			getKeyMethod = PotionEffectType.class.getMethod("getKey");
		} catch (NoSuchMethodException ignored) {
			// Method doesn't exist in 1.17.1
		}
		POTION_EFFECT_GET_KEY = getKeyMethod;
		try {
			// Try new Spigot field
			Field field = Registry.class.getField("EFFECT");
			potionEffectRegistry = (Registry) field.get(null);
		} catch (NoSuchFieldException e) {
			try {
				// Fallback to older field
				Field field = Registry.class.getField("POTION_EFFECT_TYPE");
				potionEffectRegistry = (Registry) field.get(null);
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * Resolves a {@link PotionEffectType} from a string key, supporting all Bukkit
	 * versions.
	 * <p>
	 * Uses {@link org.bukkit.Registry#EFFECT} for 1.20.2+,
	 * {@link org.bukkit.Registry#POTION_EFFECT_TYPE} for 1.18–1.20.1, and falls
	 * back to {@code PotionEffectType.getByName()} for older versions.
	 * 
	 * Supports both namespaced keys like "minecraft:speed" and simple keys like
	 * "speed".
	 *
	 * @param input The effect key to resolve.
	 * @return A matching {@link PotionEffectType}, or {@code null} if not found or
	 *         unsupported.
	 */
	@SuppressWarnings("deprecation")
	@Nullable
	public static PotionEffectType resolve(@Nullable String input) {
		if (input == null || potionEffectRegistry == null)
			return null;

		NamespacedKey key = parseKey(input);
		if (key == null) {
			return null;
		}
		PotionEffectType type = (PotionEffectType) potionEffectRegistry.get(key);

		// Fallback for legacy support
		if (type == null) {
			type = PotionEffectType.getByName(input.toUpperCase());
		}

		return type;
	}

	@SuppressWarnings("deprecation")
	@Nullable
	public static NamespacedKey getPotionEffectKey(PotionEffectType effect) {
		if (POTION_EFFECT_GET_KEY != null) {
			try {
				return (NamespacedKey) POTION_EFFECT_GET_KEY.invoke(effect);
			} catch (Throwable ignored) {
			}
		}
		// fallback: emulate a key using name (no namespace info)
		try {
			return NamespacedKey.minecraft(effect.getName().toLowerCase(Locale.ROOT));
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static NamespacedKey parseKey(String input) {
		if (!input.contains(":")) {
			return NamespacedKey.minecraft(input.toLowerCase());
		}
		return NamespacedKey.fromString(input.toLowerCase());
	}

	private PotionEffectResolver() {
		// Static utility class; prevent instantiation
	}
}