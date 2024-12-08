package com.swiftlicious.hellblock.scheduler;

public interface RegionExecutor<T, W> {

	void run(Runnable r, T l);

	default void run(Runnable r) {
		run(r, null);
	}

	void run(Runnable r, W world, int x, int z);

	SchedulerTask runLater(Runnable r, long delayTicks, T l);

	SchedulerTask runRepeating(Runnable r, long delayTicks, long period, T l);
}
