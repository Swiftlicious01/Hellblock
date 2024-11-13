package com.swiftlicious.hellblock.scheduler;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.LogUtils;

/**
 * Abstract implementation of {@link SchedulerAdapter} using a
 * {@link ScheduledExecutorService}.
 */
public abstract class AbstractJavaScheduler<T> implements SchedulerAdapter<T> {
	private static final int PARALLELISM = 16;

	private final ScheduledThreadPoolExecutor scheduler;
	private final ForkJoinPool worker;

	public AbstractJavaScheduler(HellblockPlugin plugin) {

		this.scheduler = new ScheduledThreadPoolExecutor(4, r -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("hellblock-scheduler");
			return thread;
		});
		this.scheduler.setRemoveOnCancelPolicy(true);
		this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		this.worker = new ForkJoinPool(PARALLELISM, new WorkerThreadFactory(), new ExceptionHandler(), false);
	}

	@Override
	public Executor async() {
		return this.worker;
	}

	@Override
	public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
		ScheduledFuture<?> future = this.scheduler.schedule(() -> this.worker.execute(task), delay, unit);
		return () -> future.cancel(false);
	}

	@Override
	public SchedulerTask asyncRepeating(Runnable task, long delay, long interval, TimeUnit unit) {
		ScheduledFuture<?> future = this.scheduler.scheduleAtFixedRate(() -> this.worker.execute(task), delay, interval,
				unit);
		return () -> future.cancel(false);
	}

	@Override
	public void shutdownScheduler() {
		this.scheduler.shutdown();
		try {
			if (!this.scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
				LogUtils.severe("Timed out waiting for the Hellblock scheduler to terminate");
				reportRunningTasks(thread -> thread.getName().equals("hellblock-scheduler"));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void shutdownExecutor() {
		this.worker.shutdown();
		try {
			if (!this.worker.awaitTermination(1, TimeUnit.MINUTES)) {
				LogUtils.severe("Timed out waiting for the Hellblock worker thread pool to terminate");
				reportRunningTasks(thread -> thread.getName().startsWith("hellblock-worker-"));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void reportRunningTasks(Predicate<Thread> predicate) {
		Thread.getAllStackTraces().forEach((thread, stack) -> {
			if (predicate.test(thread)) {
				LogUtils.warn(
						"Thread " + thread.getName() + " is blocked, and may be the reason for the slow shutdown!\n"
								+ Arrays.stream(stack).map(el -> "  " + el).collect(Collectors.joining("\n")));
			}
		});
	}

	private static final class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
		private static final AtomicInteger COUNT = new AtomicInteger(0);

		@Override
		public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
			ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
			thread.setDaemon(true);
			thread.setName("hellblock-worker-" + COUNT.getAndIncrement());
			return thread;
		}
	}

	private final class ExceptionHandler implements UncaughtExceptionHandler {
		@Override
		public void uncaughtException(Thread t, Throwable ex) {
			LogUtils.warn("Thread " + t.getName() + " threw an uncaught exception", ex);
		}
	}
}