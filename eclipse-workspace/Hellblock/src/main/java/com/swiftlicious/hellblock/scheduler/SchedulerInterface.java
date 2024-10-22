package com.swiftlicious.hellblock.scheduler;

import org.bukkit.Location;

import java.util.concurrent.TimeUnit;

public interface SchedulerInterface {

	/**
	 * Runs a task synchronously on the main server thread or region thread.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 */
	void runTaskSync(Runnable runnable, Location location);

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
	 * Runs a task asynchronously with a specified delay.
	 *
	 * @param runnable The task to run.
	 * @param delay    The delay before the task execution.
	 * @param timeUnit The time unit for the delay.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	CancellableTask runTaskAsyncLater(Runnable runnable, long delay, TimeUnit timeUnit);

	/**
	 * Runs a task asynchronously.
	 *
	 * @param runnable The task to run.
	 */
	void runTaskAsync(Runnable runnable);

	/**
	 * Runs a task synchronously with a specified delay.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 * @param delay    The delay before the task execution.
	 * @param timeUnit The time unit for the delay.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	CancellableTask runTaskSyncLater(Runnable runnable, Location location, long delay, TimeUnit timeUnit);

	/**
	 * Runs a task synchronously with a specified delay in ticks.
	 *
	 * @param runnable   The task to run.
	 * @param location   The location associated with the task.
	 * @param delayTicks The delay in ticks before the task execution.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	CancellableTask runTaskSyncLater(Runnable runnable, Location location, long delayTicks);

	/**
	 * Runs a task asynchronously with a specified delay and period.
	 *
	 * @param runnable The task to run.
	 * @param delay    The delay before the first execution.
	 * @param period   The period between subsequent executions.
	 * @param timeUnit The time unit for the delay and period.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	CancellableTask runTaskAsyncTimer(Runnable runnable, long delay, long period, TimeUnit timeUnit);
}