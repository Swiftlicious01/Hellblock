package com.swiftlicious.hellblock.listeners.weather.events;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.listeners.weather.AbstractNetherWeatherTask;
import com.swiftlicious.hellblock.listeners.weather.WeatherType;
import com.swiftlicious.hellblock.world.HellblockWorld;

public final class MagmaWindWeather extends AbstractNetherWeatherTask {

	private final Context<Integer> islandContext;
	private boolean hasMagmaWind;

	public MagmaWindWeather(@NotNull HellblockPlugin plugin, int islandId, @NotNull HellblockWorld<?> world) {
		super(plugin, islandId, world);
		this.islandContext = Context.island(islandId);

		start();
	}

	@Override
	public void start() {
		if (instance.getNetherWeatherManager().isWeatherActive(islandId)) {
			instance.getPluginLogger()
					.warn(getType() + " attempted to start but another weather is active on island " + islandId);
			return;
		}

		resetCycle(instance.getConfigManager().minWeatherTime(), instance.getConfigManager().maxWeatherTime());
		this.hasMagmaWind = true;
		this.islandContext.arg(ContextKeys.MAGMA_WIND, true);

		schedule(0L, instance.getConfigManager().delay());
		broadcastWarning();
	}

	@Override
	public void tick() {
		// Global stop conditions
		if (!isWeatherAllowed(getType()) || isInProtectedEvent() || !hasPlayersOnIsland()) {
			return;
		}

		// Tick duration cycle from parent class
		if (!tickDuration()) {
			handleWindEnd();
			return;
		}

		// Main loop
		instance.getIslandManager().getPlayersOnIsland(islandId).stream().map(Bukkit::getPlayer)
				.filter(Objects::nonNull).filter(Player::isOnline)
				.filter(p -> p.getWorld().getName().equalsIgnoreCase(world.worldName()))
				.filter(p -> instance.getHellblockHandler().isInCorrectWorld(p))
				.filter(p -> world.bukkitWorld().getEnvironment() == World.Environment.NETHER)
				.forEach(this::processPlayer);
	}

	private void processPlayer(@NotNull Player player) {
		if (!hasMagmaWind)
			return;

	}

	private void broadcastWarning() {
		if (!instance.getNetherWeatherManager().willSendWarning())
			return;

		instance.getIslandManager().getPlayersOnIsland(islandId).stream().map(Bukkit::getPlayer)
				.filter(Objects::nonNull).filter(Player::isOnline)
				.filter(p -> p.getWorld().getName().equalsIgnoreCase(world.worldName()))
				.filter(p -> instance.getHellblockHandler().isInCorrectWorld(p))
				.filter(p -> world.bukkitWorld().getEnvironment() == World.Environment.NETHER)
				.map(instance.getSenderFactory()::wrap).forEach(sender -> sender.sendMessage(instance
						.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_MAGMAWIND_WARNING.build())));
	}

	private void handleWindEnd() {
		if (!isWeatherAllowed(getType()))
			return;

		hasMagmaWind = false;
		recentlyOccurred = true;
		islandContext.arg(ContextKeys.MAGMA_WIND, false);

		// Stop the repeating scheduler and mark inactive
		stop();

		// Reset flag eventually
		instance.getScheduler().asyncLater(() -> recentlyOccurred = false, 5, TimeUnit.MINUTES);

		// Delegate next-weather scheduling and warning
		instance.getNetherWeatherManager().onWeatherEnd(islandId, getType());
	}

	@Override
	public void stop() {
		// Cancel the repeating scheduler
		cancel();

		// Mark both weather and task as inactive
		this.hasMagmaWind = false;
		this.waitingForNextCycle = false;

		// Update context
		islandContext.arg(ContextKeys.MAGMA_WIND, false);
	}

	@Override
	public boolean canRun(@NotNull HellblockWorld<?> world) {
		return world.bukkitWorld().getEnvironment() == World.Environment.NETHER;
	}

	@Override
	public @NotNull WeatherType getType() {
		return WeatherType.MAGMA_WIND;
	}
}