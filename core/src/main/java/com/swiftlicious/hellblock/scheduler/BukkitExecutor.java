package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class BukkitExecutor implements RegionExecutor<Location> {

	private final Plugin plugin;

	public BukkitExecutor(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run(Runnable r, Location l) {
		if (Bukkit.isPrimaryThread()) {
			r.run();
			return;
		}
		Bukkit.getScheduler().runTask(plugin, r);
	}

	@Override
	public SchedulerTask runLater(Runnable r, long delayTicks, Location l) {
		if (delayTicks == 0) {
			if (Bukkit.isPrimaryThread()) {
				r.run();
				return () -> {
				};
			} else {
				return Bukkit.getScheduler().runTask(plugin, r)::cancel;
			}
		}
		return Bukkit.getScheduler().runTaskLater(plugin, r, delayTicks)::cancel;
	}

	@Override
	public SchedulerTask runRepeating(Runnable r, long delayTicks, long period, Location l) {
		return Bukkit.getScheduler().runTaskTimer(plugin, r, delayTicks, period)::cancel;
	}
}