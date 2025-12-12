package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class BukkitExecutor implements RegionExecutor<Location, World> {

	private final Plugin plugin;

	public BukkitExecutor(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run(Runnable r, Location l) {
		if (Bukkit.isPrimaryThread()) {
			r.run();
		} else {
			Bukkit.getScheduler().runTask(plugin, r);
		}
	}

	@Override
	public void run(Runnable r, World world, int x, int z) {
		run(r);
	}

	@Override
	public SchedulerTask runLater(Runnable r, long delayTicks, Location l) {
		if (delayTicks == 0) {
			if (Bukkit.isPrimaryThread()) {
				r.run();
				return new DummyTask();
			} else {
				return new BukkitCancellable(Bukkit.getScheduler().runTask(plugin, r));
			}
		}
		return new BukkitCancellable(Bukkit.getScheduler().runTaskLater(plugin, r, delayTicks));
	}

	@Override
	public SchedulerTask runRepeating(Runnable r, long delayTicks, long period, Location l) {
		return new BukkitCancellable(Bukkit.getScheduler().runTaskTimer(plugin, r, delayTicks, period));
	}

	public static class BukkitCancellable implements SchedulerTask {

		private final BukkitTask task;

		public BukkitCancellable(BukkitTask task) {
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