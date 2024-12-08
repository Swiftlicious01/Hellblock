package com.swiftlicious.hellblock.scheduler;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class FoliaExecutor implements RegionExecutor<Location, World> {

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
	public void run(Runnable r, World world, int x, int z) {
		Bukkit.getRegionScheduler().execute(plugin, world, x, z, r);
	}

	@Override
	public SchedulerTask runLater(Runnable r, long delayTicks, Location l) {
		if (l == null) {
			if (delayTicks == 0) {
				return new FoliaCancellable(
						Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> r.run(), delayTicks));
			} else {
				return new FoliaCancellable(Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> r.run()));
			}
		} else {
			if (delayTicks == 0) {
				return new FoliaCancellable(Bukkit.getRegionScheduler().run(plugin, l, scheduledTask -> r.run()));
			} else {
				return new FoliaCancellable(
						Bukkit.getRegionScheduler().runDelayed(plugin, l, scheduledTask -> r.run(), delayTicks));
			}
		}
	}

	@Override
	public SchedulerTask runRepeating(Runnable r, long delayTicks, long period, Location l) {
		if (l == null) {
			return new FoliaCancellable(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
					scheduledTask -> r.run(), delayTicks, period));
		} else {
			return new FoliaCancellable(Bukkit.getRegionScheduler().runAtFixedRate(plugin, l, scheduledTask -> r.run(),
					delayTicks, period));
		}
	}

	public static class FoliaCancellable implements SchedulerTask {

		private final ScheduledTask task;

		public FoliaCancellable(ScheduledTask task) {
			this.task = task;
		}

		@Override
		public void cancel() {
			this.task.cancel();
		}

		@Override
		public boolean isCancelled() {
			return this.task.isCancelled();
		}
	}
}