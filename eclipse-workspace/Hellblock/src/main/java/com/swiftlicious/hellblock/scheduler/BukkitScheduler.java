package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * A scheduler implementation for synchronous tasks using Bukkit's Scheduler.
 */
public class BukkitScheduler implements SyncScheduler {

	private final HellblockPlugin instance;

	public BukkitScheduler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	/**
	 * Runs a synchronous task on the main server thread using Bukkit's Scheduler.
	 * If already on the main thread, the task is executed immediately.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 */
	@Override
	public void runSyncTask(Runnable runnable, Location location) {
		if (Bukkit.isPrimaryThread())
			runnable.run();
		else
			Bukkit.getScheduler().runTask(instance, runnable);
	}

	/**
	 * Runs a synchronous task repeatedly with a specified delay and period using
	 * Bukkit's Scheduler.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 * @param delay    The delay in ticks before the first execution.
	 * @param period   The period between subsequent executions in ticks.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskSyncTimer(Runnable runnable, Location location, long delay, long period) {
		return new BukkitCancellableTask(Bukkit.getScheduler().runTaskTimer(instance, runnable, delay, period));
	}

	/**
	 * Runs a synchronous task with a specified delay using Bukkit's Scheduler.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 * @param delay    The delay in ticks before the task execution.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskSyncLater(Runnable runnable, Location location, long delay) {
		if (delay == 0) {
			if (Bukkit.isPrimaryThread())
				runnable.run();
			else
				Bukkit.getScheduler().runTask(instance, runnable);
			return new BukkitCancellableTask(null);
		}
		return new BukkitCancellableTask(Bukkit.getScheduler().runTaskLater(instance, runnable, delay));
	}

	/**
	 * Represents a scheduled task using Bukkit's Scheduler that can be cancelled.
	 */
	public static class BukkitCancellableTask implements CancellableTask {

		private final BukkitTask bukkitTask;

		public BukkitCancellableTask(BukkitTask bukkitTask) {
			this.bukkitTask = bukkitTask;
		}

		@Override
		public void cancel() {
			if (this.bukkitTask != null)
				this.bukkitTask.cancel();
		}

		@Override
		public boolean isCancelled() {
			if (this.bukkitTask == null)
				return true;
			return this.bukkitTask.isCancelled();
		}
	}
}