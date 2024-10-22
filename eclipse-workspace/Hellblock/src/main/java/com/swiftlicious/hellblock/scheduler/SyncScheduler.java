package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Location;

public interface SyncScheduler {

	/**
	 * Runs a task synchronously on the main server thread or region thread.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 */
	void runSyncTask(Runnable runnable, Location location);

	/**
	 * Runs a task synchronously with a specified delay and period.
	 *
	 * @param runnable    The task to run.
	 * @param location    The location associated with the task.
	 * @param delayTicks  The delay in ticks before the first execution.
	 * @param periodTicks The period between subsequent executions in ticks.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	CancellableTask runTaskSyncTimer(Runnable runnable, Location location, long delayTicks, long periodTicks);

	/**
	 * Runs a task synchronously with a specified delay in ticks.
	 *
	 * @param runnable   The task to run.
	 * @param location   The location associated with the task.
	 * @param delayTicks The delay in ticks before the task execution.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	CancellableTask runTaskSyncLater(Runnable runnable, Location location, long delayTicks);
}