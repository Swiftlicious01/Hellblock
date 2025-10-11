package com.swiftlicious.hellblock.world;

/**
 * Represents the configuration settings for a Hellblock world, including
 * various parameters for ticking behavior and random events.
 */
public class WorldSetting implements Cloneable {

	private final boolean enableScheduler;
	private final int minTickUnit;
	private final boolean offlineTick;
	private final int maxOfflineTime;
	private final int maxLoadingTime;

	/**
	 * Private constructor to initialize a WorldSetting instance with the provided
	 * parameters.
	 *
	 * @param enableScheduler Whether the scheduler is enabled.
	 * @param minTickUnit     The minimum unit of tick.
	 * @param offlineTick     Whether offline ticking is enabled.
	 * @param maxOfflineTime  The maximum offline time allowed.
	 * @param maxLoadingTime  The maximum time allowed to load.
	 */
	private WorldSetting(boolean enableScheduler, int minTickUnit, boolean offlineTick, int maxOfflineTime,
			int maxLoadingTime) {
		this.enableScheduler = enableScheduler;
		this.minTickUnit = minTickUnit;
		this.offlineTick = offlineTick;
		this.maxOfflineTime = maxOfflineTime;
		this.maxLoadingTime = maxLoadingTime;
	}

	/**
	 * Factory method to create a new instance of WorldSetting.
	 *
	 * @param enableScheduler Whether the scheduler is enabled.
	 * @param minTickUnit     The minimum unit of tick.
	 * @param offlineTick     Whether offline ticking is enabled.
	 * @param maxOfflineTime  The maximum offline time allowed.
	 * @return A new WorldSetting instance.
	 */
	public static WorldSetting of(boolean enableScheduler, int minTickUnit, boolean offlineTick, int maxOfflineTime,
			int maxLoadingTime) {
		return new WorldSetting(enableScheduler, minTickUnit, offlineTick, maxOfflineTime, maxLoadingTime);
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