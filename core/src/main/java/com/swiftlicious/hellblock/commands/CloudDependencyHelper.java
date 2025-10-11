package com.swiftlicious.hellblock.commands;

import java.util.EnumSet;
import java.util.Set;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;

public final class CloudDependencyHelper {

	private static final Set<Dependency> CLOUD_DEPENDENCIES = EnumSet.of(Dependency.CLOUD_CORE,
			Dependency.CLOUD_BRIGADIER, Dependency.CLOUD_PAPER, Dependency.CLOUD_BUKKIT,
			Dependency.CLOUD_MINECRAFT_EXTRAS, Dependency.CLOUD_SERVICES);

	private ClassLoader loader;

	public CloudDependencyHelper(HellblockPlugin plugin) {
		// Load all Cloud dependencies as one group
		plugin.getDependencyManager().loadDependencies(CLOUD_DEPENDENCIES);
		ClassLoader cloudLoader = plugin.getDependencyManager().obtainClassLoaderWith(CLOUD_DEPENDENCIES);

		// Store for later use
		setClassLoader(cloudLoader);
	}

	public void setClassLoader(ClassLoader cl) {
		loader = cl;
	}

	public ClassLoader getClassLoader() {
		return loader;
	}
}
