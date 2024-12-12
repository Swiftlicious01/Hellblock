package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Location;
import org.bukkit.World;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.VersionHelper;

public class BukkitSchedulerAdapter extends AbstractJavaScheduler<Location, World> {
	protected RegionExecutor<Location, World> sync;

	public BukkitSchedulerAdapter(HellblockPlugin plugin) {
		super(plugin);
		if (VersionHelper.isFolia()) {
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