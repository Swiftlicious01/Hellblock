package com.swiftlicious.hellblock.scheduler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.utils.LogUtils;

/**
 * A scheduler implementation responsible for scheduling and managing tasks in a
 * multi-threaded environment.
 */
public class Scheduler implements SchedulerInterface {

	private final SyncScheduler syncScheduler;
	private final ScheduledThreadPoolExecutor schedule;
	private final HellblockPlugin instance;

	public Scheduler(HellblockPlugin plugin) {
		this.instance = plugin;
        this.syncScheduler = plugin.getVersionManager().isFolia() ?
                new FoliaScheduler(instance) : new BukkitScheduler(instance);
		this.schedule = new ScheduledThreadPoolExecutor(1);
		this.schedule.setMaximumPoolSize(1);
		this.schedule.setKeepAliveTime(30, TimeUnit.SECONDS);
		this.schedule.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
	}

	/**
	 * Reloads the scheduler configuration based on CustomFishingPlugin settings.
	 */
	public void reload() {
		try {
			this.schedule.setMaximumPoolSize(HBConfig.maximumPoolSize);
			this.schedule.setCorePoolSize(HBConfig.corePoolSize);
			this.schedule.setKeepAliveTime(HBConfig.keepAliveTime, TimeUnit.SECONDS);
		} catch (IllegalArgumentException e) {
			LogUtils.warn("Failed to create thread pool. Please lower the corePoolSize in config.yml.", e);
		}
	}

	/**
	 * Shuts down the scheduler.
	 */
	public void shutdown() {
		if (this.schedule != null && !this.schedule.isShutdown())
			this.schedule.shutdown();
	}

	/**
	 * Runs a task synchronously on the main server thread or region thread.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 */
	@Override
	public void runTaskSync(Runnable runnable, Location location) {
		this.syncScheduler.runSyncTask(runnable, location);
	}

	/**
	 * Runs a task asynchronously.
	 *
	 * @param runnable The task to run.
	 */
	@Override
	public void runTaskAsync(Runnable runnable) {
		try {
			this.schedule.execute(runnable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Runs a task synchronously with a specified delay and period.
	 *
	 * @param runnable    The task to run.
	 * @param location    The location associated with the task.
	 * @param delayTicks  The delay in ticks before the first execution.
	 * @param periodTicks The period between subsequent executions in ticks.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskSyncTimer(Runnable runnable, Location location, long delayTicks, long periodTicks) {
		return this.syncScheduler.runTaskSyncTimer(runnable, location, delayTicks, periodTicks);
	}

	/**
	 * Runs a task asynchronously with a specified delay.
	 *
	 * @param runnable The task to run.
	 * @param delay    The delay before the task execution.
	 * @param timeUnit The time unit for the delay.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskAsyncLater(Runnable runnable, long delay, TimeUnit timeUnit) {
		return new ScheduledTask(schedule.schedule(() -> {
			try {
				runnable.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, delay, timeUnit));
	}

	/**
	 * Runs a task synchronously with a specified delay.
	 *
	 * @param runnable The task to run.
	 * @param location The location associated with the task.
	 * @param delay    The delay before the task execution.
	 * @param timeUnit The time unit for the delay.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskSyncLater(Runnable runnable, Location location, long delay, TimeUnit timeUnit) {
		return new ScheduledTask(schedule.schedule(() -> {
			runTaskSync(runnable, location);
		}, delay, timeUnit));
	}

	/**
	 * Runs a task synchronously with a specified delay in ticks.
	 *
	 * @param runnable   The task to run.
	 * @param location   The location associated with the task.
	 * @param delayTicks The delay in ticks before the task execution.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskSyncLater(Runnable runnable, Location location, long delayTicks) {
		return this.syncScheduler.runTaskSyncLater(runnable, location, delayTicks);
	}

	/**
	 * Runs a task asynchronously with a specified delay and period.
	 *
	 * @param runnable The task to run.
	 * @param delay    The delay before the first execution.
	 * @param period   The period between subsequent executions.
	 * @param timeUnit The time unit for the delay and period.
	 * @return A CancellableTask for managing the scheduled task.
	 */
	@Override
	public CancellableTask runTaskAsyncTimer(Runnable runnable, long delay, long period, TimeUnit timeUnit) {
		return new ScheduledTask(schedule.scheduleAtFixedRate(() -> {
			try {
				runnable.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, delay, period, timeUnit));
	}

	/**
	 * Represents a thread-pool task that can be cancelled.
	 */
	public static class ScheduledTask implements CancellableTask {

		private final ScheduledFuture<?> scheduledFuture;

		public ScheduledTask(ScheduledFuture<?> scheduledFuture) {
			this.scheduledFuture = scheduledFuture;
		}

		@Override
		public void cancel() {
			this.scheduledFuture.cancel(false);
		}

		@Override
		public boolean isCancelled() {
			return this.scheduledFuture.isCancelled();
		}
	}
}