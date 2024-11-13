package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Location;

import com.swiftlicious.hellblock.HellblockPlugin;

public class BukkitSchedulerAdapter extends AbstractJavaScheduler<Location> {
	protected RegionExecutor<Location> sync;

	public BukkitSchedulerAdapter(HellblockPlugin plugin) {
		super(plugin);
		if (plugin.getVersionManager().isFolia()) {
			this.sync = new FoliaExecutor(plugin);
		} else {
			this.sync = new BukkitExecutor(plugin);
		}
	}

	@Override
	public RegionExecutor<Location> sync() {
		return this.sync;
	}
}