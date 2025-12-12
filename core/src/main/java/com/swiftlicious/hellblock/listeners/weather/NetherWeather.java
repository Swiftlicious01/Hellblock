package com.swiftlicious.hellblock.listeners.weather;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.world.HellblockWorld;

public interface NetherWeather {

	/**
	 * Called when this weather event begins.
	 */
	void start();

	/**
	 * Called each tick (or periodically) while active.
	 */
	void tick();

	/**
	 * Called when this weather event ends.
	 */
	void stop();

	/**
	 * @return the weather type identifier, e.g. "LAVA_RAIN" or "ASH_STORM"
	 */
	@NotNull
	WeatherType getType();

	/**
	 * @return true if this weather can run in the given island/world.
	 */
	boolean canRun(@NotNull HellblockWorld<?> world);
}