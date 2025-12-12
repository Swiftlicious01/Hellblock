package com.swiftlicious.hellblock.handlers;

import java.util.EnumSet;
import java.util.Set;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;

public final class AdventureDependencyHelper {

	public static final Set<Dependency> ADVENTURE_DEPENDENCIES = EnumSet.of(Dependency.ADVENTURE_API,
			Dependency.ADVENTURE_EXAMINATION_API, Dependency.ADVENTURE_EXAMINATION_STRING, Dependency.ADVENTURE_OPTION,
			Dependency.ADVENTURE_MINIMESSAGE, Dependency.ADVENTURE_TEXT_JSON_LEGACY, Dependency.ADVENTURE_TEXT_LEGACY,
			Dependency.ADVENTURE_TEXT_GSON, Dependency.ADVENTURE_TEXT_JSON, Dependency.ADVENTURE_TEXT_PLAIN,
			Dependency.ADVENTURE_PLATFORM_BUKKIT, Dependency.ADVENTURE_PLATFORM_API,
			Dependency.ADVENTURE_PLATFORM_FACET, Dependency.ADVENTURE_NBT, Dependency.ADVENTURE_KEY);

	private final HellblockPlugin plugin;
	private ClassLoader loader;
	private boolean loaded;
	private net.kyori.adventure.platform.bukkit.BukkitAudiences bukkitAudiences; // cache of relocated BukkitAudiences

	public AdventureDependencyHelper(HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	public void load() {
		try {
			plugin.getDependencyManager().loadDependencies(ADVENTURE_DEPENDENCIES);
			loader = plugin.getDependencyManager().obtainClassLoaderWith(ADVENTURE_DEPENDENCIES);
			loaded = true;
			plugin.getPluginLogger().info("Adventure API libraries loaded successfully.");
		} catch (Exception ex) {
			loaded = false;
			plugin.getPluginLogger().warn("Failed to load Adventure API dependencies.", ex);
		}
	}

	public boolean isLoaded() {
		return loaded;
	}

	public ClassLoader getClassLoader() {
		return loader;
	}

	public net.kyori.adventure.platform.bukkit.BukkitAudiences createBukkitAudiences() {
		if (bukkitAudiences != null)
			return bukkitAudiences;

		if (!loaded || loader == null) {
			plugin.getPluginLogger().warn("Adventure not yet loaded, auto-loading dependencies.");
			load();
		}

		try {
			bukkitAudiences = plugin.getDependencyManager().runWithLoader(ADVENTURE_DEPENDENCIES, () -> {
				net.kyori.adventure.platform.bukkit.BukkitAudiences audiences = net.kyori.adventure.platform.bukkit.BukkitAudiences
						.create(plugin);
				plugin.getPluginLogger().info("Successfully initialized relocated Adventure API BukkitAudiences.");
				return audiences;
			});
			return bukkitAudiences;
		} catch (Throwable t) {
			plugin.getPluginLogger().warn("Failed to create relocated Adventure API BukkitAudiences instance.", t);
			return null;
		}
	}

	public net.kyori.adventure.platform.bukkit.BukkitAudiences getBukkitAudiences() {
		return bukkitAudiences;
	}

	public void closeBukkitAudiences() {
		if (bukkitAudiences == null)
			return;

		plugin.getDependencyManager().runWithLoader(ADVENTURE_DEPENDENCIES, () -> {
			bukkitAudiences.close();
			return null;
		});
		bukkitAudiences = null;
	}
}