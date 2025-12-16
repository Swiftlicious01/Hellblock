package com.swiftlicious.hellblock.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A scheduler for running tasks using the systems provided by the platform
 */
public interface SchedulerAdapter<T, W> {

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
	RegionExecutor<T, W> sync();

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

	/**
	 * Executes a task sync
	 *
	 * @param task the task
	 */
	default void executeSync(Runnable task) {
		sync().run(task, null);
	}

	/**
	 * Executes a supplier synchronously on the main thread, returning a
	 * CompletableFuture that completes with the supplier’s result once the task is
	 * run.
	 *
	 * @param supplier The supplier to run synchronously
	 * @param <R>      The result type
	 * @return A CompletableFuture that completes with the supplier’s result
	 */
	default <R> CompletableFuture<R> supplySync(Supplier<R> supplier) {
		CompletableFuture<R> future = new CompletableFuture<>();
		executeSync(() -> {
			try {
				R result = supplier.get();
				future.complete(result);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		return future;
	}

	/**
	 * Executes a supplier synchronously at a specific location, returning a future
	 * for the result.
	 *
	 * @param supplier The supplier to run synchronously
	 * @param location The location or region key
	 * @param <R>      The result type
	 * @return A CompletableFuture that completes with the supplier’s result
	 */
	default <R> CompletableFuture<R> supplySync(Supplier<R> supplier, T location) {
		CompletableFuture<R> future = new CompletableFuture<>();
		executeSync(() -> {
			try {
				R result = supplier.get();
				future.complete(result);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		}, location);
		return future;
	}

	/**
	 * Executes a {@link Supplier} that returns a {@link CompletableFuture}
	 * synchronously on the main thread (or platform’s primary execution context).
	 * <p>
	 * This method is useful when you need to start an asynchronous task (such as
	 * one that depends on main-thread operations) and wait for its result to
	 * complete. The supplier itself is invoked synchronously, but the returned
	 * future is transparently chained so the result of the inner future becomes the
	 * result of the returned {@code CompletableFuture}.
	 * </p>
	 *
	 * <pre>
	 * // Example: run recalc synchronously, but wait for async result
	 * scheduler.callSync(() -> islandLevelManager.recalculateIslandLevel(islandId))
	 * 		.thenAccept(level -> log.info("Recalculated level: " + level));
	 * </pre>
	 *
	 * @param supplier a supplier that produces a CompletableFuture task to run
	 *                 synchronously
	 * @param <R>      the result type of the inner and returned futures
	 * @return a future that completes with the result of the inner
	 *         CompletableFuture
	 */
	default <R> CompletableFuture<R> callSync(Supplier<CompletableFuture<R>> supplier) {
		CompletableFuture<R> outer = new CompletableFuture<>();
		executeSync(() -> {
			try {
				supplier.get().whenComplete((result, ex) -> {
					if (ex != null)
						outer.completeExceptionally(ex);
					else
						outer.complete(result);
				});
			} catch (Throwable t) {
				outer.completeExceptionally(t);
			}
		});
		return outer;
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