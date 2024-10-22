package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.swiftlicious.hellblock.HellblockPlugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * A scheduler implementation for "synchronous" tasks using Folia's
 * RegionScheduler.
 */
public class FoliaScheduler implements SyncScheduler {

	private final HellblockPlugin instance;

	public FoliaScheduler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	/**
	 * Runs a "synchronous" task on the region thread using Folia's RegionScheduler.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 */
	@Override
	public void runSyncTask(Runnable runnable, Location location) {
		if (location == null) {
			Bukkit.getGlobalRegionScheduler().execute(instance, runnable);
		} else {
			Bukkit.getRegionScheduler().execute(instance, location, runnable);
		}
	}

	/**
	 * Runs a "synchronous" task repeatedly with a specified delay and period using
	 * Folia's RegionScheduler.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 * @param delay    The delay in ticks before the first execution.
	 * @param period   The period between subsequent executions in ticks.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskSyncTimer(Runnable runnable, Location location, long delay, long period) {
		if (location == null) {
			return new FoliaCancellableTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(instance,
					(scheduledTask -> runnable.run()), delay, period));
		}
		return new FoliaCancellableTask(Bukkit.getRegionScheduler().runAtFixedRate(instance, location,
				(scheduledTask -> runnable.run()), delay, period));
	}

	/**
	 * Runs a "synchronous" task with a specified delay using Folia's
	 * RegionScheduler.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 * @param delay    The delay in ticks before the task execution.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskSyncLater(Runnable runnable, Location location, long delay) {
		if (delay == 0) {
			if (location == null) {
				return new FoliaCancellableTask(
						Bukkit.getGlobalRegionScheduler().run(instance, (scheduledTask -> runnable.run())));
			}
			return new FoliaCancellableTask(
					Bukkit.getRegionScheduler().run(instance, location, (scheduledTask -> runnable.run())));
		}
		if (location == null) {
			return new FoliaCancellableTask(
					Bukkit.getGlobalRegionScheduler().runDelayed(instance, (scheduledTask -> runnable.run()), delay));
		}
		return new FoliaCancellableTask(
				Bukkit.getRegionScheduler().runDelayed(instance, location, (scheduledTask -> runnable.run()), delay));
	}

	/**
	 * Represents a scheduled task using Folia's RegionScheduler that can be
	 * cancelled.
	 */
	public static class FoliaCancellableTask implements CancellableTask {

		private final ScheduledTask scheduledTask;

		public FoliaCancellableTask(ScheduledTask scheduledTask) {
			this.scheduledTask = scheduledTask;
		}

		@Override
		public void cancel() {
			this.scheduledTask.cancel();
		}

		@Override
		public boolean isCancelled() {
			return this.scheduledTask.isCancelled();
		}
	}
}