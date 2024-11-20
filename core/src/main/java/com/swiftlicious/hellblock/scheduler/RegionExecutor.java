package com.swiftlicious.hellblock.scheduler;

public interface RegionExecutor<T> {

	void run(Runnable r, T l);

	SchedulerTask runLater(Runnable r, long delayTicks, T l);

	SchedulerTask runRepeating(Runnable r, long delayTicks, long period, T l);
}
