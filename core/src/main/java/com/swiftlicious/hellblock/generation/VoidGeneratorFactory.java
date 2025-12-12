package com.swiftlicious.hellblock.generation;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;

/**
 * Factory class for constructing {@link VoidGenerator} instances.
 * <p>
 * This factory dynamically creates a ByteBuddy-based proxy generator for
 * environments that support {@code org.bukkit.generator.WorldInfo} (i.e.,
 * Minecraft 1.17.1+). On older versions (e.g., 1.17.0), it falls back to a
 * standard {@link VoidGenerator} with reduced capabilities.
 */
public final class VoidGeneratorFactory {

	public static final Set<Dependency> BYTEBUDDY_DEPENDENCIES = Set.of(Dependency.BYTE_BUDDY);

	/**
	 * Creates a {@link ChunkGenerator} compatible with the current server version.
	 * <p>
	 * If the {@code WorldInfo} class is available (1.17.1+), this method attempts
	 * to dynamically generate a proxy {@link ChunkGenerator} using ByteBuddy to
	 * bridge modern method signatures with the {@link VoidGenerator}
	 * implementation.
	 * <p>
	 * If the environment does not support {@code WorldInfo}, it returns the raw
	 * {@link VoidGenerator} instance directly.
	 *
	 * @return a working {@link ChunkGenerator} — either a ByteBuddy proxy or
	 *         fallback
	 */
	@NotNull
	public static ChunkGenerator create() {
		VoidGenerator delegate = new VoidGenerator();
		HellblockPlugin plugin = HellblockPlugin.getInstance();

		// Step 1: Check for WorldInfo availability (1.17.1+)
		Class<?> worldInfoClass;
		try {
			worldInfoClass = Class.forName("org.bukkit.generator.WorldInfo");
		} catch (ClassNotFoundException ignored) {
			plugin.debug("WorldInfo class not found — using legacy VoidGenerator.");
			return delegate;
		}

		// Step 2: Attempt to create ByteBuddy proxy if WorldInfo is present
		try {
			plugin.debug("Attempting to isolate ByteBuddy dependencies for VoidGenerator...");

			// Ensure ByteBuddy is loaded (dynamically or via dependency manager)
			plugin.getDependencyManager().loadDependencies(BYTEBUDDY_DEPENDENCIES);

			// Use isolated classloader to prevent classpath conflicts
			ClassLoader bbLoader = plugin.getDependencyManager().obtainClassLoaderWith(BYTEBUDDY_DEPENDENCIES);

			// Merge visibility: let it fall back to the plugin’s own classloader for Bukkit
			// API
			ClassLoader combinedLoader = new URLClassLoader(new URL[0], plugin.getClass().getClassLoader()) {
				@Override
				protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
					try {
						// First try Bukkit/plugin classes
						return getParent().loadClass(name);
					} catch (ClassNotFoundException ex) {
						// Fallback to ByteBuddy loader
						return bbLoader.loadClass(name);
					}
				}
			};

			// Load helper and invoke static factory method reflectively
			Class<?> helperClass = Class.forName("com.swiftlicious.hellblock.generation.ByteBuddyVoidGeneratorHelper",
					true, combinedLoader);
			Method createMethod = helperClass.getMethod("create", VoidGenerator.class, Class.class);

			// Create the proxy generator using ByteBuddy helper
			Object result = createMethod.invoke(null, delegate, worldInfoClass);

			plugin.debug("ByteBuddy proxy for VoidGenerator created successfully.");
			return (ChunkGenerator) result;

		} catch (Throwable t) {
			plugin.getPluginLogger()
					.warn("Failed to create ByteBuddy proxy for VoidGenerator (class visibility issue?)", t);
			return delegate;
		}
	}
}