package com.swiftlicious.hellblock.api;

public class TpsMonitor {
	private static final int SAMPLE_SIZE = 100;
	private final long[] tickTimes = new long[SAMPLE_SIZE];
	private int tickCount = 0;

	public void onTick() {
		tickTimes[tickCount++ % SAMPLE_SIZE] = System.nanoTime();
	}

	public double getRecentTps() {
		if (tickCount < SAMPLE_SIZE) {
			return 20.0;
		}

		final int index = tickCount % SAMPLE_SIZE;
		final long elapsed = System.nanoTime() - tickTimes[index];
		final double elapsedSeconds = elapsed / 1.0E9;
		return Math.min(20.0, SAMPLE_SIZE / elapsedSeconds);
	}
}
