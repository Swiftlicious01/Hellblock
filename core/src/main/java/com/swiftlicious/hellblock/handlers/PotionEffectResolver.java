package com.swiftlicious.hellblock.handlers;

import java.lang.reflect.Field;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unchecked")
public class PotionEffectResolver {

	private static Registry<PotionEffectType> potionEffectRegistry;

	static {
		// Try Paper's name first
		try {
			Field field = Registry.class.getField("POTION_EFFECT_TYPE");
			potionEffectRegistry = (Registry<PotionEffectType>) field.get(null);
		} catch (NoSuchFieldException e) {
			// Try Spigot's name
			try {
				Field field = Registry.class.getField("EFFECT");
				potionEffectRegistry = (Registry<PotionEffectType>) field.get(null);
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	@SuppressWarnings("deprecation")
	public static PotionEffectType resolve(String input) {
		if (input == null || potionEffectRegistry == null)
			return null;

		NamespacedKey key = parseKey(input);
		if (key == null) {
			return null;
		}
		PotionEffectType type = potionEffectRegistry.get(key);

		// Fallback for legacy support
		if (type == null) {
			type = PotionEffectType.getByName(input.toUpperCase());
		}

		return type;
	}

	private static NamespacedKey parseKey(String input) {
		if (!input.contains(":")) {
			return NamespacedKey.minecraft(input.toLowerCase());
		}
		return NamespacedKey.fromString(input.toLowerCase());
	}
}
