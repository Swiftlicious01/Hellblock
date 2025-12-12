package com.swiftlicious.hellblock.nms.util;

import java.lang.reflect.Method;
import java.util.Objects;

import org.bukkit.Bukkit;

public final class BukkitReflectionUtils {

	private static final String PREFIX_MC = "net.minecraft.";
	private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit";
	private static final String CRAFT_SERVER = "CraftServer";

	private static final String CB_PKG_VERSION;
	public static final int MAJOR_REVISION;
	public static final boolean IS_PAPER;

	private BukkitReflectionUtils() {
	}

	static {
		final Class<?> serverClass;

		if (Bukkit.getServer() == null) {
			// Fallback for Paper Bootstrapper (headless environment)
			serverClass = Objects.requireNonNull(getClassSafely("org.bukkit.craftbukkit.CraftServer"));
			IS_PAPER = true;
		} else {
			serverClass = Bukkit.getServer().getClass();
			IS_PAPER = isPaperServer();
		}

		// Extract the package version, e.g., "v1_20_R3"
		final String pkg = serverClass.getPackage().getName();
		final String nmsVersion = pkg.substring(pkg.lastIndexOf('.') + 1);

		if (!nmsVersion.contains("_")) {
			// No version string, likely newer version (1.17+), use reflection to extract
			// version
			MAJOR_REVISION = extractMajorVersionReflectively(serverClass);
		} else {
			// Legacy NMS versioning (pre-1.17)
			MAJOR_REVISION = Integer.parseInt(nmsVersion.split("_")[1]);
		}

		// Determine CraftBukkit package version (used in reflection)
		String name = serverClass.getName();
		name = name.substring(PREFIX_CRAFTBUKKIT.length());
		name = name.substring(0, name.length() - CRAFT_SERVER.length());
		CB_PKG_VERSION = name;
	}

	/**
	 * Builds a full class name for a CraftBukkit class.
	 * 
	 * @param className The simple class name (e.g., "entity.CraftPlayer")
	 * @return The fully qualified class name.
	 */
	public static String assembleCBClass(String className) {
		return PREFIX_CRAFTBUKKIT + CB_PKG_VERSION + className;
	}

	/**
	 * Builds a full class name for a Minecraft (NMS) class.
	 * 
	 * @param className The simple class name (e.g., "server.level.EntityPlayer")
	 * @return The fully qualified class name.
	 */
	public static String assembleMCClass(String className) {
		return PREFIX_MC + className;
	}

	/**
	 * Checks if the server is running on Paper.
	 */
	private static boolean isPaperServer() {
		try {
			Class.forName("com.destroystokyo.paper.PaperConfig");
			return true;
		} catch (ClassNotFoundException ignored) {
			try {
				Class.forName("io.papermc.paper.configuration.Configuration");
				return true;
			} catch (ClassNotFoundException ignored2) {
				return false;
			}
		}
	}

	/**
	 * Extracts the major Minecraft version (e.g., 20 for 1.20.x) via reflection.
	 * 
	 * @param serverClass The CraftServer class.
	 * @return The major Minecraft version.
	 */
	private static int extractMajorVersionReflectively(Class<?> serverClass) {
		// Try Paper first
		try {
			Class<?> sharedConstants = getClassSafely("net.minecraft.SharedConstants");
			if (sharedConstants != null) {
				Method getCurrentVersion = sharedConstants.getDeclaredMethod("getCurrentVersion");
				Object version = getCurrentVersion.invoke(null);
				Method getName = version.getClass().getDeclaredMethod("getName");
				String versionName = (String) getName.invoke(version);
				return Integer.parseInt(versionName.split("\\.")[1]); // e.g., "1.20.2" → 20
			}
		} catch (Exception ignored) {
		}

		// Fallback: use CraftServer#getMinecraftVersion()
		if (Bukkit.getServer() != null) {
			try {
				Method getMinecraftVersion = serverClass.getDeclaredMethod("getMinecraftVersion");
				String version = getMinecraftVersion.invoke(Bukkit.getServer()).toString();
				return Integer.parseInt(version.split("\\.")[1]);
			} catch (Exception ignored) {
			}
		}

		return -1; // Unknown version
	}

	/**
	 * Gets a class without throwing a checked exception.
	 */
	private static Class<?> getClassSafely(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}