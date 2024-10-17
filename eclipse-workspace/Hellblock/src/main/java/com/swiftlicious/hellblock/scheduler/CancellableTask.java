package com.swiftlicious.hellblock.scheduler;

public interface CancellableTask {

	/**
	 * Cancel the task
	 */
	void cancel();

	/**
	 * Get if the task is cancelled or not
	 *
	 * @return cancelled or not
	 */
	boolean isCancelled();
}