package com.swiftlicious.hellblock.scheduler;

public class DummyTask implements SchedulerTask {

	@Override
	public void cancel() {
	}

	@Override
	public boolean isCancelled() {
		return true;
	}
}