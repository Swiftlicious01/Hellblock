package com.swiftlicious.hellblock.scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A scheduler for running tasks using the systems provided by the platform
 */
public interface SchedulerAdapter<T> {

	/**
	 * Gets an async executor instance
	 *
	 * @return an async executor instance
	 */
	Executor async();

	/**
	 * Gets a sync executor instance
	 *
	 * @return a sync executor instance
	 */
	RegionExecutor<T> sync();

	/**
	 * Executes a task async
	 *
	 * @param task the task
	 */
	default void executeAsync(Runnable task) {
		async().execute(task);
	}

	/**
	 * Executes a task sync
	 *
	 * @param task the task
	 */
	default void executeSync(Runnable task, T location) {
		sync().run(task, location);
	}

	default void executeSync(Runnable task) {
		sync().run(task, null);
	}

	/**
	 * Executes the given task with a delay.
	 *
	 * @param task  the task
	 * @param delay the delay
	 * @param unit  the unit of delay
	 * @return the resultant task instance
	 */
	SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit);

	/**
	 * Executes the given task repeatedly at a given interval.
	 *
	 * @param task     the task
	 * @param interval the interval
	 * @param unit     the unit of interval
	 * @return the resultant task instance
	 */
	SchedulerTask asyncRepeating(Runnable task, long delay, long interval, TimeUnit unit);

	/**
	 * Shuts down the scheduler instance.
	 *
	 * <p>
	 * {@link #asyncLater(Runnable, long, TimeUnit)} and
	 * {@link #asyncRepeating(Runnable, long, long, TimeUnit)}.
	 * </p>
	 */
	void shutdownScheduler();

	/**
	 * Shuts down the executor instance.
	 *
	 * <p>
	 * {@link #async()} and {@link #executeAsync(Runnable)}.
	 * </p>
	 */
	void shutdownExecutor();

}