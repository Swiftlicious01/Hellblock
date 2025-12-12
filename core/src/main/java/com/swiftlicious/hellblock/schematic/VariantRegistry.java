package com.swiftlicious.hellblock.schematic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import org.bukkit.Art;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.world.registry.ItemRegistry;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.VersionHelper;

import net.kyori.adventure.nbt.CompoundBinaryTag;

/**
 * Utility class for looking up various Minecraft variant types by their string
 * IDs, using Paper's Registry API when available, with Spigot fallbacks.
 * <p>
 * Requires Paper 1.19-R0.1+ for full functionality. On Spigot, some lookups may
 * be limited or unavailable.
 * </p>
 * 
 * @see https://hub.spigotmc.org/javadocs/paper/1.14/io/papermc/paper/registry/RegistryAccess.html
 * @see https://hub.spigotmc.org/javadocs/paper/1.14/io/papermc/paper/registry/RegistryKey.html
 */
@SuppressWarnings("removal")
public final class VariantRegistry {

	private VariantRegistry() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

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
	 * Checks if the current server is running Paper or a Paper fork (e.g., Purpur).
	 * <p>
	 * This is determined by checking for the presence of the Paper registry API via
	 * {@code io.papermc.paper.registry.RegistryKey}.
	 *
	 * @return {@code true} if the Paper Registry API is available, {@code false}
	 *         otherwise.
	 */
	private static boolean isPaper() {
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
	 * Resolves a wolf variant from its string ID.
	 * <p>
	 * Uses Paper's registry system ({@code WOLF_VARIANT}) when available, allowing
	 * for future variant expansion in newer versions. On Spigot or legacy servers
	 * where {@code Wolf.Variant} does not exist, this method falls back to
	 * reflective static field lookup to maintain compatibility.
	 * <p>
	 * Example variant IDs include {@code "snowy"}, {@code "ash"} or
	 * {@code "rusty"}.
	 *
	 * @param id The variant name or registry ID.
	 * @return The resolved wolf variant object, or {@code null} if not found or
	 *         unsupported.
	 */
	@Nullable
	public static Object getWolfVariant(@NotNull String id) {
		return resolveVariant("WOLF_VARIANT", "org.bukkit.entity.Wolf$Variant", id);
	}

	/**
	 * Resolves a cat variant from its string ID.
	 * <p>
	 * Uses Paper's registry system ({@code CAT_VARIANT}) when available. On Spigot
	 * or legacy servers, falls back to matching {@link Cat.Type} constants by name.
	 * <p>
	 * Example variant IDs include {@code "tabby"}, {@code "tuxedo"},
	 * {@code "siamese"}, or {@code "black"}.
	 *
	 * @param id The cat variant ID.
	 * @return The resolved {@link Cat.Type} variant, or {@code null} if not found
	 *         or unsupported.
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
	 * Resolves a frog variant from its string ID.
	 * <p>
	 * Uses Paper's registry system ({@code FROG_VARIANT}) when available, which was
	 * introduced in Minecraft 1.19. On older versions where {@code Frog.Variant}
	 * does not exist, the method safely returns {@code null}.
	 * <p>
	 * Example variant IDs include {@code "temperate"}, {@code "cold"}, and
	 * {@code "warm"}.
	 *
	 * @param id The frog variant ID.
	 * @return The resolved frog variant object, or {@code null} if not found or
	 *         unsupported.
	 */
	@Nullable
	public static Object getFrogVariant(@NotNull String id) {
		return resolveVariant("FROG_VARIANT", "org.bukkit.entity.Frog$Variant", id);
	}

	/**
	 * Resolves a cow variant from its string ID.
	 * <p>
	 * Supports both modern Paper servers (via registry lookup) and legacy
	 * Spigot-based servers (via reflection-based fallback). Avoids direct
	 * dependency on {@code Cow.Variant}, which is not available in older versions.
	 *
	 * @param id The variant name (e.g., "brown", "default").
	 * @return The resolved variant object, or {@code null} if not found or
	 *         unsupported.
	 */
	@Nullable
	public static Object getCowVariant(@NotNull String id) {
		return resolveVariant("COW_VARIANT", "org.bukkit.entity.Cow$Variant", id);
	}

	/**
	 * Resolves a chicken variant from its string ID.
	 * <p>
	 * Uses Paper registry lookup when available, with fallback to static field
	 * lookup via reflection on Spigot or older versions.
	 *
	 * @param id The variant name (e.g., "default", "wild").
	 * @return The resolved variant object, or {@code null} if not found or
	 *         unsupported.
	 */
	@Nullable
	public static Object getChickenVariant(@NotNull String id) {
		return resolveVariant("CHICKEN_VARIANT", "org.bukkit.entity.Chicken$Variant", id);
	}

	/**
	 * Resolves a pig variant from its string ID.
	 * <p>
	 * Supports modern Paper servers using the registry system and falls back to
	 * reflective lookup of static fields on legacy versions.
	 *
	 * @param id The pig variant ID (e.g., "default", "spotted").
	 * @return The resolved variant object, or {@code null} if not found or
	 *         unsupported.
	 */
	@Nullable
	public static Object getPigVariant(@NotNull String id) {
		return resolveVariant("PIG_VARIANT", "org.bukkit.entity.Pig$Variant", id);
	}

	/**
	 * Resolves a salmon variant from its string ID.
	 * <p>
	 * Compatible with both modern Paper servers and legacy Spigot versions via
	 * reflection. Uses the "SALMON_VARIANT" registry when available.
	 *
	 * @param id The salmon variant ID (e.g., "large", "default").
	 * @return The resolved variant object, or {@code null} if not found or
	 *         unsupported.
	 */
	@Nullable
	public static Object getSalmonVariant(@NotNull String id) {
		return resolveVariant("SALMON_VARIANT", "org.bukkit.entity.Salmon$Variant", id);
	}

	/**
	 * Resolves a painting variant from its string ID.
	 * <p>
	 * Uses Paper's registry system ({@code PAINTING_VARIANT}) when available. On
	 * Spigot or older versions, this method falls back to matching enum constants
	 * from {@link Art#values()} by name.
	 * <p>
	 * Example variant IDs include {@code "kebab"}, {@code "wasteland"}, or
	 * {@code "donkey_kong"}.
	 *
	 * @param id The painting variant ID.
	 * @return The resolved {@link Art} variant, or {@code null} if not found or
	 *         unsupported.
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
	 * Resolves and writes the variant of an entity into the given NBT data builder,
	 * using version-safe logic that works across Spigot and Paper versions.
	 * <p>
	 * This method uses reflection to extract the variant ID from the entity via
	 * {@code getVariant()}, and delegates to {@link VariantRegistry} to resolve the
	 * correct variant object. If found, it attempts to extract a
	 * {@link NamespacedKey} via {@code getKey()}, or falls back to
	 * {@code toString()}.
	 *
	 * @param entity      The entity whose variant should be serialized.
	 * @param dataBuilder The NBT builder to populate with variant data.
	 */
	public static void writeVariantIfPresent(@NotNull Entity entity, @NotNull CompoundBinaryTag.Builder dataBuilder) {
		String id = extractVariantId(entity);
		if (id == null) {
			return;
		}

		Object resolved = switch (entity.getType()) {
		case WOLF -> getWolfVariant(id);
		case PIG -> getPigVariant(id);
		case COW -> getCowVariant(id);
		case CHICKEN -> getChickenVariant(id);
		case SALMON -> getSalmonVariant(id);
		default -> {
			if (isFrog(entity))
				yield getFrogVariant(id);
			else
				yield null;
		}
		};

		if (resolved != null) {
			try {
				Method getKey = resolved.getClass().getMethod("getKey");
				NamespacedKey key = (NamespacedKey) getKey.invoke(resolved);
				dataBuilder.putString("variant", key.toString());
			} catch (Exception e) {
				dataBuilder.putString("variant", resolved.toString());
			}
		}
	}

	private static boolean isFrog(@NotNull Entity entity) {
		try {
			return EntityType.valueOf("FROG") == entity.getType();
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			return false;
		}
	}

	/**
	 * Attempts to set the variant on an entity using reflection, for versions where
	 * {@code setVariant(...)} exists.
	 * <p>
	 * It safely skips execution if the method is not available (e.g., in older
	 * Spigot versions), and logs a warning if any error occurs during reflection.
	 *
	 * @param entity  The entity to modify (e.g., Wolf, Frog, Pig).
	 * @param variant The variant object resolved via {@link VariantRegistry}, or
	 *                null to skip.
	 */
	public static void setVariantIfPresent(@NotNull Entity entity, @Nullable Object variant) {
		if (variant == null)
			return;

		try {
			Method setVariant = entity.getClass().getMethod("setVariant", variant.getClass());
			setVariant.invoke(entity, variant);
		} catch (NoSuchMethodException ignored) {
			// Method doesn't exist on this server version — ignore safely
		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Failed to set variant for entity: " + entity.getType(), ex);
		}
	}

	/**
	 * Attempts to extract the string identifier of an entity's variant using
	 * reflection.
	 * <p>
	 * This method is designed to support multiple Minecraft/Spigot/Paper versions
	 * by safely calling {@code getVariant()} only if it exists at runtime. It also
	 * tries to extract the {@link NamespacedKey} if available (i.e., via
	 * {@code getKey()}), and returns the key's path.
	 * <p>
	 * If the variant or associated methods do not exist (e.g., on older versions),
	 * this method safely returns {@code null}.
	 *
	 * @param entity The entity whose variant should be extracted.
	 * @return The string ID of the variant (e.g., "temperate", "spotted", "snowy"),
	 *         or {@code null} if not available.
	 */
	@Nullable
	public static String extractVariantId(@NotNull Entity entity) {
		try {
			Method getVariant = entity.getClass().getMethod("getVariant");
			Object variant = getVariant.invoke(entity);
			if (variant == null)
				return null;

			// Try variant.getKey().getKey()
			try {
				Method getKey = variant.getClass().getMethod("getKey");
				Object keyObj = getKey.invoke(variant);
				if (keyObj instanceof NamespacedKey key) {
					return key.getKey(); // only "temperate", "snowy", etc.
				}
			} catch (NoSuchMethodException ignored) {
				// getKey() may not exist, fallback
			}

			return variant.toString(); // fallback (usually matches enum name)
		} catch (NoSuchMethodException ignored) {
			return null; // No getVariant() method — older version
		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger().warn("Failed to extract variant for " + entity.getType(),
					ex);
			return null;
		}
	}

	/**
	 * Resolves a variant object from a registry key and class name using Paper's
	 * registry system, with full compatibility fallback for Spigot and older
	 * versions.
	 * <p>
	 * This method dynamically loads the variant class using reflection, avoiding
	 * any compile-time dependency on classes that may not exist in older server
	 * versions (e.g., {@code Wolf.Variant}).
	 * <p>
	 * If the server is running a Paper fork, it attempts to resolve the variant
	 * using {@code ItemRegistry.getRegistryValue(...)}, which is available in
	 * modern Paper builds. If the registry lookup fails or returns {@code null}, it
	 * falls back to looking up a matching public static field by name.
	 * <p>
	 * On non-Paper (e.g., Spigot), the method directly falls back to static field
	 * reflection.
	 *
	 * @param registryKey The name of the registry (e.g., {@code "WOLF_VARIANT"}).
	 * @param className   The fully qualified class name of the variant (e.g.,
	 *                    {@code "org.bukkit.entity.Wolf$Variant"}).
	 * @param id          The name of the variant to resolve (e.g.,
	 *                    {@code "snowy"}).
	 * @return The resolved variant object, or {@code null} if not found or
	 *         unsupported in this server version.
	 */
	@Nullable
	private static Object resolveVariant(@NotNull String registryKey, @NotNull String className, @NotNull String id) {
		if (!VersionHelper.isPaperFork()) {
			return reflectVariantFromClass(className, id);
		}

		try {
			Class<?> variantClass = Class.forName(className);
			Method getRegistryValue = ItemRegistry.class.getDeclaredMethod("getRegistryValue", String.class,
					Class.class, String.class);
			Object result = getRegistryValue.invoke(null, registryKey, variantClass, id);

			// If registry lookup fails, fall back to static field reflection
			if (result == null) {
				return reflectVariantFromClass(className, id);
			}

			return result;
		} catch (ClassNotFoundException e) {
			return reflectVariantFromClass(className, id);
		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger().warn("Failed to resolve variant for " + registryKey, ex);
			return null;
		}
	}

	/**
	 * Reflectively resolves a variant by searching for a public static field with a
	 * matching name in the specified variant class.
	 * <p>
	 * This method is used as a compatibility fallback for Spigot and older Paper
	 * versions where the registry-based lookup via
	 * {@code ItemRegistry.getRegistryValue(...)} is not available. It dynamically
	 * loads the class using {@code Class.forName(...)} to avoid direct dependencies
	 * on version-specific classes such as {@code Wolf.Variant}.
	 * <p>
	 * If the class cannot be found or the field does not exist, {@code null} is
	 * returned.
	 *
	 * @param className The fully qualified name of the variant class.
	 * @param id        The name of the variant to resolve (case-insensitive).
	 * @return The resolved variant object, or {@code null} if not found or not
	 *         supported.
	 */
	@Nullable
	private static Object reflectVariantFromClass(@NotNull String className, @NotNull String id) {
		try {
			Class<?> variantClass = Class.forName(className);
			for (Field field : variantClass.getFields()) {
				if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(variantClass)) {
					if (field.getName().equalsIgnoreCase(id)) {
						return field.get(null);
					}
				}
			}
		} catch (ClassNotFoundException ignored) {
			// Class doesn't exist at all in old Spigot
		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger().warn("Failed to reflect variant for class: " + className,
					ex);
		}
		return null;
	}

	/**
	 * Generic lookup method for retrieving values from Paper's registry system
	 * using reflection.
	 * <p>
	 * This method avoids compile-time dependency on the Paper API by accessing
	 * {@code RegistryKey}, {@code RegistryAccess}, and registry instances
	 * reflectively.
	 * <p>
	 * If the registry lookup fails for any reason (missing class, invalid key,
	 * etc.), this method returns {@code null}.
	 *
	 * @param <T>             The expected type of the registry value.
	 * @param registryKeyName The name of the static field in {@code RegistryKey}
	 *                        (e.g., {@code "ENCHANTMENT"}, {@code "FROG_VARIANT"}).
	 * @param type            The class type of the value being looked up.
	 * @param id              The ID of the value (e.g., "snowy", "protection"). If
	 *                        not namespaced, "minecraft" is assumed.
	 * @return The resolved value from the registry, or {@code null} if not found or
	 *         on error.
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