package com.swiftlicious.hellblock.scheduler;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class FoliaExecutor implements RegionExecutor<Location> {

	private final Plugin plugin;

	public FoliaExecutor(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run(Runnable r, Location l) {
		Optional.ofNullable(l).ifPresentOrElse(loc -> Bukkit.getRegionScheduler().execute(plugin, loc, r),
				() -> Bukkit.getGlobalRegionScheduler().execute(plugin, r));
	}

	@Override
	public SchedulerTask runLater(Runnable r, long delayTicks, Location l) {
		if (l == null) {
			if (delayTicks == 0) {
				return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> r.run(),
						delayTicks)::cancel;
			} else {
				return Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> r.run())::cancel;
			}
		} else {
			if (delayTicks == 0) {
				return Bukkit.getRegionScheduler().run(plugin, l, scheduledTask -> r.run())::cancel;
			} else {
				return Bukkit.getRegionScheduler().runDelayed(plugin, l, scheduledTask -> r.run(), delayTicks)::cancel;
			}
		}
	}

	@Override
	public SchedulerTask runRepeating(Runnable r, long delayTicks, long period, Location l) {
		if (l == null) {
			return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> r.run(), delayTicks,
					period)::cancel;
		} else {
			return Bukkit.getRegionScheduler().runAtFixedRate(plugin, l, scheduledTask -> r.run(), delayTicks,
					period)::cancel;
		}
	}
}