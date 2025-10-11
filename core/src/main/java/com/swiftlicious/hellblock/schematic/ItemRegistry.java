package com.swiftlicious.hellblock.schematic;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import org.bukkit.Art;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for looking up various Minecraft item types by their string
 * IDs, using Paper's Registry API when available, with Spigot fallbacks.
 * <p>
 * Requires Paper 1.14-R0.1+ for full functionality. On Spigot, some lookups may
 * be limited or unavailable.
 * </p>
 * 
 * @see https://hub.spigotmc.org/javadocs/paper/1.14/io/papermc/paper/registry/RegistryAccess.html
 * @see https://hub.spigotmc.org/javadocs/paper/1.14/io/papermc/paper/registry/RegistryKey.html
 */
@SuppressWarnings("removal")
public final class ItemRegistry {

	private static final boolean PAPER_AVAILABLE;

	static {
		boolean paper;
		try {
			// Check for Paper's Registry API
			Class.forName("io.papermc.paper.registry.RegistryKey");
			paper = true;
		} catch (ClassNotFoundException e) {
			// Not Paper
			paper = false;
		}
		PAPER_AVAILABLE = paper;
	}

	/**
	 * Check if the server is running Paper (or a Paper fork like Purpur, etc)
	 * 
	 * @return true if Paper's Registry API is available, false otherwise
	 */
	public static boolean isPaper() {
		return PAPER_AVAILABLE;
	}

	/**
	 * Get a banner pattern by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The banner pattern ID (e.g. "creeper", "skull", etc)
	 * @return The PatternType enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/block/banner/PatternType.html
	 * @see https://minecraft.fandom.com/wiki/Banner#Patterns
	 * @see https://minecraft.fandom.com/wiki/Model#Block_models
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#Banner_pattern
	 */
	@Nullable
	public static PatternType getBannerPattern(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("BANNER_PATTERN", PatternType.class, id);
		}

		// Spigot fallback (legacy support)
		return PatternType.getByIdentifier(id.toLowerCase());
	}

	/**
	 * Get a villager profession by its ID. Added in Minecraft 1.14 / Paper
	 * 1.14-R0.1+
	 * 
	 * @param id The villager profession ID (e.g. "farmer", "librarian", etc)
	 * @return The Villager.Profession enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/entity/Villager.Profession.html
	 * @see https://minecraft.fandom.com/wiki/Villager#Professions
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#
	 *      Villager_profession
	 */
	@Nullable
	public static Villager.Profession getVillagerProfession(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("VILLAGER_PROFESSION", Villager.Profession.class, id);
		}

		// Fallback: try match by name
		for (Villager.Profession prof : Villager.Profession.values()) {
			if (prof.name().equalsIgnoreCase(id)) {
				return prof;
			}
		}
		return null;
	}

	/**
	 * Get a villager type by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The villager type ID (e.g. "plains", "desert", etc)
	 * @return The Villager.Type enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/entity/Villager.Type.html
	 * @see https://minecraft.fandom.com/wiki/Villager#Types
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#
	 *      Villager_biome_type
	 */
	@Nullable
	public static Villager.Type getVillagerType(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("VILLAGER_TYPE", Villager.Type.class, id);
		}

		// Fallback: try match by name
		for (Villager.Type type : Villager.Type.values()) {
			if (type.name().equalsIgnoreCase(id)) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Get a wolf variant by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The wolf variant ID (e.g. "NORMAL", "TUXEDO", etc)
	 * @return The Wolf.Variant enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/entity/Wolf.Variant.html
	 * @see https://minecraft.fandom.com/wiki/Wolf#Variants
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#Wolf_variantF
	 */
	@Nullable
	public static Wolf.Variant getWolfVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("WOLF_VARIANT", Wolf.Variant.class, id);
		}

		// Spigot fallback: reflect public static fields
		try {
			for (var field : Wolf.Variant.class.getFields()) {
				if (Modifier.isStatic(field.getModifiers()) && field.getType() == Wolf.Variant.class) {
					if (field.getName().equalsIgnoreCase(id)) {
						return (Wolf.Variant) field.get(null);
					}
				}
			}
		} catch (Exception ignored) {
			// Ignore reflection errors
		}
		return null;
	}

	@Nullable
	public static Wolf.SoundVariant getWolfSoundVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("WOLF_SOUND_VARIANT", Wolf.SoundVariant.class, id);
		}

		// Spigot fallback: reflect public static fields (atm no spigot fallback
		// available)
//		try {
//			for (var field : Wolf.SoundVariant.class.getFields()) {
//				if (Modifier.isStatic(field.getModifiers()) && field.getType() == Wolf.SoundVariant.class) {
//					if (field.getName().equalsIgnoreCase(id)) {
//						return (Wolf.SoundVariant) field.get(null);
//					}
//				}
//			}
//		} catch (Exception ignored) {
//			// Ignore reflection errors
//		}
		return null;
	}

	/**
	 * Get a cat variant by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The cat variant ID (e.g. "TABBY", "TUXEDO", "SIAMESE", etc)
	 * @return The Cat.Type enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/entity/Cat.Type.html
	 * @see https://minecraft.fandom.com/wiki/Cat#Variants
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#Cat_variant
	 */
	@Nullable
	public static Cat.Type getCatVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("CAT_VARIANT", Cat.Type.class, id);
		}

		// Fallback: try match by name
		for (Cat.Type type : Cat.Type.values()) {
			if (type.name().equalsIgnoreCase(id)) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Get a frog variant by its ID. Added in Minecraft 1.19.3 / Paper 1.19.3-R0.1+
	 * 
	 * @param id The frog variant ID (e.g. "TEMPERATE", "COLD", "WARM")
	 * @return The Frog.Variant enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      19.3/org/bukkit/entity/Frog.Variant.html
	 * @see https://minecraft.fandom.com/wiki/Frog#Variants
	 */
	@Nullable
	public static Frog.Variant getFrogVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("FROG_VARIANT", Frog.Variant.class, id);
		}

		// Fallback: try match by name
		for (Frog.Variant variant : Frog.Variant.values()) {
			if (variant.name().equalsIgnoreCase(id)) {
				return variant;
			}
		}
		return null;
	}

	/**
	 * Get a cow variant by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The cow variant ID (e.g. "NORMAL", "SHEARED", "MUSHROOM", etc)
	 * @return The Cow.Variant enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/entity/Cow.Variant.html
	 * @see https://minecraft.fandom.com/wiki/Cow#Variants
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#Cow_variant
	 */
	@Nullable
	public static Cow.Variant getCowVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("COW_VARIANT", Cow.Variant.class, id);
		}

		// Spigot fallback: reflect public static fields
		try {
			for (var field : Cow.Variant.class.getFields()) {
				if (Modifier.isStatic(field.getModifiers()) && field.getType() == Cow.Variant.class) {
					if (field.getName().equalsIgnoreCase(id)) {
						return (Cow.Variant) field.get(null);
					}
				}
			}
		} catch (Exception ignored) {
			// Ignore reflection errors
		}
		return null;
	}

	/**
	 * Get a chicken variant by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The chicken variant ID (e.g. "NORMAL", "BLACK", "WHITE", etc)
	 * @return The Chicken.Variant enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/entity/Chicken.Variant.html
	 * @see https://minecraft.fandom.com/wiki/Chicken#Variants
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#Chicken_variant
	 */
	@Nullable
	public static Chicken.Variant getChickenVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("CHICKEN_VARIANT", Chicken.Variant.class, id);
		}

		// Spigot fallback: reflect public static fields
		try {
			for (var field : Chicken.Variant.class.getFields()) {
				if (Modifier.isStatic(field.getModifiers()) && field.getType() == Chicken.Variant.class) {
					if (field.getName().equalsIgnoreCase(id)) {
						return (Chicken.Variant) field.get(null);
					}
				}
			}
		} catch (Exception ignored) {
			// Ignore reflection errors
		}
		return null;
	}

	/**
	 * Get a pig variant by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The pig variant ID (e.g. "NORMAL", "SADDLED", etc)
	 * @return The Pig.Variant enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/entity/Pig.Variant.html
	 * @see https://minecraft.fandom.com/wiki/Pig#Variants
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#Pig_variant
	 */
	@Nullable
	public static Pig.Variant getPigVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("PIG_VARIANT", Pig.Variant.class, id);
		}

		// Spigot fallback: reflect public static fields
		try {
			for (var field : Pig.Variant.class.getFields()) {
				if (Modifier.isStatic(field.getModifiers()) && field.getType() == Pig.Variant.class) {
					if (field.getName().equalsIgnoreCase(id)) {
						return (Pig.Variant) field.get(null);
					}
				}
			}
		} catch (Exception ignored) {
			// Ignore reflection errors
		}
		return null;
	}

	/**
	 * Get a salmon variant by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The salmon variant ID (e.g. "SALMON", "SMALL", "ATLANTIC", etc)
	 * @return The Salmon.Variant enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.
	 *      14.1/org/bukkit/entity/Salmon.Variant.html
	 * @see https://minecraft.fandom.com/wiki/Salmon#Variants
	 * @see https://minecraft.fandom.com/wiki/Java_Edition_data_values#Salmon_variant
	 */
	@Nullable
	public static Salmon.Variant getSalmonVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("SALMON_VARIANT", Salmon.Variant.class, id);
		}

		// Fallback: try match by name
		for (Salmon.Variant variant : Salmon.Variant.values()) {
			if (variant.name().equalsIgnoreCase(id)) {
				return variant;
			}
		}
		return null;
	}

	/**
	 * Get an enchantment by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The enchantment ID (e.g. "sharpness", "mending", etc)
	 * @return The Enchantment, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.14/org/bukkit/enchantments/Enchantment.html
	 * @see https://minecraft.fandom.com/wiki/Enchantment#List_of_enchantments
	 */
	@Nullable
	@SuppressWarnings("deprecation")
	public static Enchantment getEnchantment(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("ENCHANTMENT", Enchantment.class, id);
		}

		// Spigot fallback (legacy support)
		NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
		if (key == null) {
			return null;
		}

		return Enchantment.getByKey(key);
	}

	/**
	 * Get a painting variant by its ID. Added in Minecraft 1.14 / Paper 1.14-R0.1+
	 * 
	 * @param id The painting variant ID (e.g. "Kebab", "Aztec", etc)
	 * @return The Art enum value, or null if not found
	 * @see https://hub.spigotmc.org/javadocs/paper/1.14/org/bukkit/Art.html
	 * @see https://hub.spigotmc.org/javadocs/paper/1.14/org/bukkit/block/Painting.html
	 * @see https://minecraft.fandom.com/wiki/Painting#List_of_paintings
	 */
	@Nullable
	public static Art getPaintingVariant(@NotNull String id) {
		if (isPaper()) {
			return getRegistryValue("PAINTING_VARIANT", Art.class, id);
		}

		// Fallback: try match by name
		for (Art art : Art.values()) {
			if (art.name().equalsIgnoreCase(id)) {
				return art;
			}
		}
		return null;
	}

	/**
	 * Generic registry lookup using Paper's Registry API. Returns null on any
	 * failure.
	 * 
	 * @param <T>             The type of the registry value
	 * @param registryKeyName The static field name in {@code RegistryKey} ( e.g.
	 *                        "ENCHANTMENT", "PAINTING_VARIANT", etc)
	 * @param type            The class of the registry value (e.g.
	 *                        Enchantment.class)
	 * @param id              The namespaced ID (without namespace, assumed to be
	 *                        "minecraft")
	 * @return The registry value, or null if not found or on error
	 * @throws ClassNotFoundException if Paper's Registry API is not available
	 */
	@Nullable
	private static <T> T getRegistryValue(@NotNull String registryKeyName, @NotNull Class<T> type, @NotNull String id) {
		try {
			// Reflection to avoid compile-time dependency on Paper
			final Class<?> registryKeyClass = Class.forName("io.papermc.paper.registry.RegistryKey");
			final Object registryKey = registryKeyClass.getField(registryKeyName).get(null);

			// Get the RegistryAccess singleton
			final Class<?> registryAccessClass = Class.forName("io.papermc.paper.registry.RegistryAccess");
			final Method registryAccessMethod = registryAccessClass.getMethod("registryAccess");
			final Object registryAccess = registryAccessMethod.invoke(null);

			// Get the specific registry
			final Method getRegistryMethod = registryAccess.getClass().getMethod("getRegistry", registryKeyClass);
			final Object registry = getRegistryMethod.invoke(registryAccess, registryKey);

			// Properly parse the key (handles both "protection" and "minecraft:protection")
			NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
			if (key == null) {
				return null;
			}

			// Get the value by NamespacedKey
			final Method getMethod = registry.getClass().getMethod("get", NamespacedKey.class);
			return type.cast(getMethod.invoke(registry, key));
		} catch (Exception e) {
			return null;
		}
	}
}