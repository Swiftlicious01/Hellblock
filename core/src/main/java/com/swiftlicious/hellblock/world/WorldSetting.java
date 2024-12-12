package com.swiftlicious.hellblock.world;

/**
 * Represents the configuration settings for a Hellblock world, including
 * various parameters for ticking behavior and random events.
 */
public class WorldSetting implements Cloneable {

	private final boolean enableScheduler;
	private final int minTickUnit;
	private final int tickCropInterval;
	private final boolean offlineTick;
	private final int randomTickSpeed;
	private final int maxOfflineTime;
	private final int maxLoadingTime;
	private final int tickCropMode;
	private final int cropPerChunk;

	/**
	 * Private constructor to initialize a WorldSetting instance with the provided
	 * parameters.
	 *
	 * @param enableScheduler  Whether the scheduler is enabled.
	 * @param minTickUnit      The minimum unit of tick.
	 * @param tickCropMode     The tick mode of crop
	 * @param tickCropInterval The interval for ticking crops.
	 * @param cropPerChunk     The maximum number of crops per chunk.
	 * @param offlineTick      Whether offline ticking is enabled.
	 * @param maxOfflineTime   The maximum offline time allowed.
	 * @param maxLoadingTime   The maximum time allowed to load.
	 * @param randomTickSpeed  The random tick speed.
	 */
	private WorldSetting(boolean enableScheduler, int minTickUnit, int tickCropMode, int tickCropInterval,
			boolean offlineTick, int maxOfflineTime, int maxLoadingTime, int cropPerChunk, int randomTickSpeed) {
		this.enableScheduler = enableScheduler;
		this.minTickUnit = minTickUnit;
		this.tickCropInterval = tickCropInterval;
		this.offlineTick = offlineTick;
		this.maxOfflineTime = maxOfflineTime;
		this.maxLoadingTime = maxLoadingTime;
		this.randomTickSpeed = randomTickSpeed;
		this.cropPerChunk = cropPerChunk;
		this.tickCropMode = tickCropMode;
	}

	/**
	 * Factory method to create a new instance of WorldSetting.
	 *
	 * @param enableScheduler  Whether the scheduler is enabled.
	 * @param minTickUnit      The minimum unit of tick.
	 * @param tickCropMode     The tick mode of crop
	 * @param tickCropInterval The interval for ticking crops.
	 * @param cropPerChunk     The maximum number of crops per chunk.
	 * @param offlineTick      Whether offline ticking is enabled.
	 * @param maxOfflineTime   The maximum offline time allowed.
	 * @param randomTickSpeed  The random tick speed.
	 * @return A new WorldSetting instance.
	 */
	public static WorldSetting of(boolean enableScheduler, int minTickUnit, int tickCropMode, int tickCropInterval,
			boolean offlineTick, int maxOfflineTime, int maxLoadingTime, int cropPerChunk, int randomTickSpeed) {
		return new WorldSetting(enableScheduler, minTickUnit, tickCropMode, tickCropInterval, offlineTick,
				maxOfflineTime, maxLoadingTime, cropPerChunk, randomTickSpeed);
	}

	/**
	 * Checks if the scheduler is enabled.
	 *
	 * @return true if the scheduler is enabled, false otherwise.
	 */
	public boolean enableScheduler() {
		return enableScheduler;
	}

	/**
	 * Gets the minimum tick unit.
	 *
	 * @return The minimum tick unit.
	 */
	public int minTickUnit() {
		return minTickUnit;
	}

	/**
	 * Gets the interval for ticking crops.
	 *
	 * @return The tick interval for crops.
	 */
	public int tickCropInterval() {
		return tickCropInterval;
	}

	/**
	 * Checks if offline ticking is enabled.
	 *
	 * @return true if offline ticking is enabled, false otherwise.
	 */
	public boolean offlineTick() {
		return offlineTick;
	}

	/**
	 * Gets the max time allowed to load a chunk
	 *
	 * @return The max loading time
	 */
	public int maxLoadingTime() {
		return maxLoadingTime;
	}

	/**
	 * Gets the maximum number of crops per chunk.
	 *
	 * @return The maximum number of crops per chunk.
	 */
	public int cropPerChunk() {
		return cropPerChunk;
	}

	/**
	 * Gets the random tick speed.
	 *
	 * @return The random tick speed.
	 */
	public int randomTickSpeed() {
		return randomTickSpeed;
	}

	/**
	 * Gets the tick mode of crop
	 *
	 * @return the mode
	 */
	public int tickCropMode() {
		return tickCropMode;
	}

	/**
	 * Gets the maximum offline time allowed.
	 *
	 * @return The maximum offline time.
	 */
	public int maxOfflineTime() {
		return maxOfflineTime;
	}

	/**
	 * Creates a clone of this WorldSetting instance.
	 *
	 * @return A cloned instance of WorldSetting.
	 */
	@Override
	public WorldSetting clone() {
		try {
			return (WorldSetting) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}