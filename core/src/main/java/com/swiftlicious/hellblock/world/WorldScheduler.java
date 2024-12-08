package com.swiftlicious.hellblock.world;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

public class WorldScheduler {
	private static final int PARALLELISM = 1;

	protected final HellblockPlugin instance;

	private final ScheduledThreadPoolExecutor scheduler;
	private final ForkJoinPool worker;

	public WorldScheduler(HellblockPlugin plugin) {
		this.instance = plugin;

		this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("hellblock-world-scheduler");
			return thread;
		});
		this.scheduler.setMaximumPoolSize(1);
		this.scheduler.setRemoveOnCancelPolicy(true);
		this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		this.worker = new ForkJoinPool(PARALLELISM, new WorkerThreadFactory(), new ExceptionHandler(), false);
	}

	public Executor async() {
		return this.worker;
	}

	public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
		ScheduledFuture<?> future = this.scheduler.schedule(() -> this.worker.execute(task), delay, unit);
		return new JavaCancellable(future);
	}

	public SchedulerTask asyncRepeating(Runnable task, long delay, long interval, TimeUnit unit) {
		ScheduledFuture<?> future = this.scheduler.scheduleAtFixedRate(() -> this.worker.execute(task), delay, interval,
				unit);
		return new JavaCancellable(future);
	}

	public void shutdownScheduler() {
		this.scheduler.shutdownNow();
	}

	public void shutdownExecutor() {
		this.worker.shutdownNow();
	}

	private static final class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
		private static final AtomicInteger COUNT = new AtomicInteger(0);

		@Override
		public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
			ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
			thread.setDaemon(true);
			thread.setName("hellblock-world-worker-" + COUNT.getAndIncrement());
			return thread;
		}
	}

	private final class ExceptionHandler implements UncaughtExceptionHandler {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			WorldScheduler.this.instance.getPluginLogger()
					.warn("Thread " + t.getName() + " threw an uncaught exception", e);
		}
	}

	public static class JavaCancellable implements SchedulerTask {

		private final ScheduledFuture<?> future;

		public JavaCancellable(ScheduledFuture<?> future) {
			this.future = future;
		}

		@Override
		public void cancel() {
			this.future.cancel(false);
		}

		@Override
		public boolean isCancelled() {
			return future.isCancelled();
		}
	}
}