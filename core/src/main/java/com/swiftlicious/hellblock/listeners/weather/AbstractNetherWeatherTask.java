package com.swiftlicious.hellblock.listeners.weather;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

public abstract class AbstractNetherWeatherTask implements NetherWeather {

	protected final HellblockPlugin instance;
	protected final int islandId;
	protected final HellblockWorld<?> world;
	protected SchedulerTask repeatingTask;
	protected boolean isActive;

	protected boolean waitingForNextCycle;
	protected long remainingTicks;
	protected boolean recentlyOccurred;

	protected AbstractNetherWeatherTask(@NotNull HellblockPlugin plugin, int islandId,
			@NotNull HellblockWorld<?> world) {
		this.instance = plugin;
		this.islandId = islandId;
		this.world = world;
	}

	/**
	 * Starts this weather repeating on the scheduler.
	 */
	public void schedule(long delayTicks, long periodTicks) {
		this.repeatingTask = instance.getScheduler().sync().runRepeating(this::runInternal, delayTicks, periodTicks,
				LocationUtils.getAnyLocationInstance());
		this.isActive = true;
	}

	/**
	 * Cancels this weather task and marks it as inactive.
	 */
	public void cancel() {
		if (this.repeatingTask != null && !this.repeatingTask.isCancelled()) {
			this.repeatingTask.cancel();
		}
		this.isActive = false;
	}
	
	public void stop() {
	    cancel(); // stops the scheduler and marks inactive
	    this.waitingForNextCycle = true;
	}

	public void resetCycle(long minTicks, long maxTicks) {
		this.remainingTicks = RandomUtils.generateRandomInt((int) minTicks, (int) maxTicks);
		this.waitingForNextCycle = false;
		this.recentlyOccurred = false;
	}

	public boolean tickDuration() {
		if (this.remainingTicks <= 0 && !this.waitingForNextCycle) {
			this.waitingForNextCycle = true;
			return false;
		}
		this.remainingTicks--;
		return true;
	}

	public boolean isActive() {
		return this.isActive;
	}

	public boolean isWeatherAllowed(@NotNull WeatherType weatherType) {
		return instance.getConfigManager().weatherEnabled()
				&& instance.getConfigManager().supportedWeatherTypes().contains(weatherType);
	}

	public boolean isInProtectedEvent() {
		return instance.getSkysiegeHandler().isSkysiegeRunning(this.islandId)
				|| instance.getInvasionHandler().isInvasionRunning(this.islandId)
				|| instance.getWitherHandler().getCustomWither().hasActiveWither(this.islandId);
	}

	public boolean hasPlayersOnIsland() {
		return !instance.getIslandManager().getPlayersOnIsland(this.islandId).isEmpty();
	}

	/**
	 * Runs each tick via the scheduler (do not override — use {@link #tick()}).
	 */
	protected final void runInternal() {
		tick();
	}

	@Override
	public abstract void tick();
}