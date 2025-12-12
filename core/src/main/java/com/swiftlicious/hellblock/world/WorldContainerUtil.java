package com.swiftlicious.hellblock.world;

import java.lang.reflect.Method;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * Utility class providing reflective access to {@link PersistentDataContainer}
 * for {@link org.bukkit.World} instances.
 *
 * <p>
 * This allows the plugin to safely read and write persistent metadata to
 * worlds, such as configuration data, format versioning, and plugin-specific
 * state, even on legacy Minecraft versions that do not directly support PDC on
 * worlds.
 * </p>
 */
public class WorldContainerUtil {

	private static Method getPDCMethod;

	static {
		try {
			getPDCMethod = World.class.getMethod("getPersistentDataContainer");
		} catch (NoSuchMethodException e) {
			getPDCMethod = null; // Not available in 1.18
		}
	}

	/**
	 * Key used to store the world format version in the PersistentDataContainer.
	 * This value is namespaced to avoid collisions with other plugins.
	 */
	private static final NamespacedKey VERSION_KEY = new NamespacedKey(HellblockPlugin.getInstance(), "version");

	private WorldContainerUtil() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Check whether PersistentDataContainer is supported by the World class.
	 *
	 * @return true if getPersistentDataContainer() is available
	 */
	public static boolean isPersistentDataContainerAvailable() {
		return getPDCMethod != null;
	}

	/**
	 * Attempt to get the PersistentDataContainer from the given World using
	 * reflection.
	 *
	 * @param world the World instance
	 * @return the container if available, or null
	 */
	@Nullable
	public static PersistentDataContainer getContainer(@NotNull World world) {
		if (getPDCMethod == null)
			return null;

		try {
			Object result = getPDCMethod.invoke(world);
			if (result instanceof PersistentDataContainer container) {
				return container;
			}
		} catch (ReflectiveOperationException e) {
			// Swallow or log if needed
		}
		return null;
	}

	/**
	 * Loads a string value from the world's PersistentDataContainer using the given
	 * key.
	 *
	 * @param world the World
	 * @param key   the NamespacedKey to retrieve
	 * @return the string value or null
	 */
	@Nullable
	public static String getString(@NotNull World world, @NotNull NamespacedKey key) {
		PersistentDataContainer container = getContainer(world);
		return container != null ? container.get(key, PersistentDataType.STRING) : null;
	}

	/**
	 * Stores a string value in the world's PersistentDataContainer using the given
	 * key.
	 *
	 * @param world the World
	 * @param key   the NamespacedKey to store
	 * @param value the string value to store
	 * @return true if successful, false otherwise
	 */
	public static boolean setString(@NotNull World world, @NotNull NamespacedKey key, @NotNull String value) {
		PersistentDataContainer container = getContainer(world);
		if (container != null) {
			container.set(key, PersistentDataType.STRING, value);
			return true;
		}
		return false;
	}

	/**
	 * Retrieves the stored Hellblock world format version from the given world's
	 * {@link PersistentDataContainer}.
	 *
	 * <p>
	 * This version represents the schema or data format used when the world was
	 * last saved by the plugin. It can be compared against the current plugin world
	 * version constant (e.g. {@code HellblockPlugin.CURRENT_WORLD_VERSION}) to
	 * determine if migration or data patching is required.
	 * </p>
	 *
	 * <p>
	 * If no version entry is present or the world does not support
	 * {@link PersistentDataContainer}, this method returns {@code null}.
	 * </p>
	 *
	 * @param world the Bukkit {@link World} whose stored version to retrieve
	 * @return the stored version number, or {@code null} if unavailable
	 */
	@Nullable
	public static Integer getVersion(@NotNull World world) {
		PersistentDataContainer container = getContainer(world);
		if (container == null)
			return null;
		return container.get(VERSION_KEY, PersistentDataType.INTEGER);
	}

	/**
	 * Writes the current Hellblock world format version into the given world's
	 * {@link PersistentDataContainer}.
	 *
	 * <p>
	 * This method is typically called immediately after a world is created or
	 * successfully saved, ensuring that the stored version matches the plugin’s
	 * current data format version.
	 * </p>
	 *
	 * <p>
	 * If the world or server version does not support
	 * {@link PersistentDataContainer}, this method safely does nothing and returns
	 * {@code false}.
	 * </p>
	 *
	 * @param world   the Bukkit {@link World} to mark with a format version
	 * @param version the integer version to store (e.g.
	 *                {@code CURRENT_WORLD_VERSION})
	 * @return {@code true} if the version was successfully written, {@code false}
	 *         otherwise
	 */
	public static boolean setVersion(@NotNull World world, int version) {
		PersistentDataContainer container = getContainer(world);
		if (container != null) {
			container.set(VERSION_KEY, PersistentDataType.INTEGER, version);
			return true;
		}
		return false;
	}
}