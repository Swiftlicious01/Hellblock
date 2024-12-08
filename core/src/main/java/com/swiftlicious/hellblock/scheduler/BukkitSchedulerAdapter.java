package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Location;
import org.bukkit.World;

import com.swiftlicious.hellblock.HellblockPlugin;

public class BukkitSchedulerAdapter extends AbstractJavaScheduler<Location, World> {
	protected RegionExecutor<Location, World> sync;

	public BukkitSchedulerAdapter(HellblockPlugin plugin) {
		super(plugin);
		if (plugin.getVersionManager().isFolia()) {
			this.sync = new FoliaExecutor(plugin);
		} else {
			this.sync = new BukkitExecutor(plugin);
		}
	}

	@Override
	public RegionExecutor<Location, World> sync() {
		return this.sync;
	}
}