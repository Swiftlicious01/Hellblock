package com.swiftlicious.hellblock.scheduler;

/**
 * Represents a scheduled task
 */
public interface SchedulerTask {

	/**
	 * Cancels the task.
	 */
	void cancel();

    boolean isCancelled();
}