package com.swiftlicious.hellblock.commands;

import java.util.EnumSet;
import java.util.Set;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;

public final class CloudDependencyHelper {

	public static final Set<Dependency> CLOUD_DEPENDENCIES = EnumSet.of(Dependency.CLOUD_CORE,
			Dependency.CLOUD_BRIGADIER, Dependency.CLOUD_PAPER, Dependency.CLOUD_BUKKIT,
			Dependency.CLOUD_MINECRAFT_EXTRAS, Dependency.CLOUD_SERVICES);

	private final HellblockPlugin plugin;
	private ClassLoader loader;
	private boolean loaded;

	public CloudDependencyHelper(HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	public void load() {
		try {
			plugin.getDependencyManager().loadDependencies(CLOUD_DEPENDENCIES);
			loader = plugin.getDependencyManager().obtainClassLoaderWith(CLOUD_DEPENDENCIES);
			loaded = true;
			plugin.getPluginLogger().info("Cloud command framework libraries loaded successfully.");
		} catch (Exception ex) {
			loaded = false;
			plugin.getPluginLogger().warn("Failed to load Cloud command framework dependencies.", ex);
		}
	}

	public boolean isLoaded() {
		return loaded;
	}

	public ClassLoader getClassLoader() {
		return loader;
	}
}