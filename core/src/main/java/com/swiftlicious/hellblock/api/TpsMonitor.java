package com.swiftlicious.hellblock.api;

/**
 * TpsMonitor is a utility class for monitoring server ticks per second (TPS).
 * It maintains a sliding window of tick times and calculates the recent TPS
 * based on the time elapsed between ticks.
 * 
 * <p>
 * The maximum TPS value reported is capped at 20.0, which is the standard for
 * many game servers.
 */
public class TpsMonitor {

	private static final int SAMPLE_SIZE = 100;
	private final long[] tickTimes = new long[SAMPLE_SIZE];
	private int tickCount = 0;

	/**
	 * Records the current tick timestamp using {@code System.nanoTime()}. This
	 * method should be called once per server tick.
	 */
	public void onTick() {
		tickTimes[tickCount++ % SAMPLE_SIZE] = System.nanoTime();
	}

	/**
	 * Calculates and returns the recent ticks per second (TPS) based on the stored
	 * tick times.
	 * 
	 * @return The recent TPS, capped at 20.0. If the number of recorded ticks is
	 *         less than the sample size, it returns 20.0 as a default.
	 */
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