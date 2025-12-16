package com.swiftlicious.hellblock.listeners.weather.events;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.listeners.weather.AbstractNetherWeatherTask;
import com.swiftlicious.hellblock.listeners.weather.NetherWeatherManager;
import com.swiftlicious.hellblock.listeners.weather.NetherWeatherManager.NetherWeatherRegion;
import com.swiftlicious.hellblock.listeners.weather.WeatherType;
import com.swiftlicious.hellblock.utils.PotionUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

public final class EmberFogWeather extends AbstractNetherWeatherTask {

	private final Context<Integer> islandContext;
	private boolean hasEmberFog;

	public EmberFogWeather(@NotNull HellblockPlugin plugin, int islandId, @NotNull HellblockWorld<?> world) {
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
		this.hasEmberFog = true;
		this.islandContext.arg(ContextKeys.EMBER_FOG, true);

		schedule(0L, instance.getConfigManager().delay());
		broadcastWarning();
	}

	@Override
	public void tick() {
		if (!isWeatherAllowed(getType()) || isInProtectedEvent() || !hasPlayersOnIsland()) {
			return;
		}

		if (!tickDuration()) {
			handleFogEnd();
			return;
		}

		instance.getIslandManager().getPlayersOnIsland(islandId).stream().map(Bukkit::getPlayer)
				.filter(Objects::nonNull).filter(Player::isOnline)
				.filter(p -> p.getWorld().getName().equalsIgnoreCase(world.worldName()))
				.filter(p -> instance.getHellblockHandler().isInCorrectWorld(p))
				.filter(p -> world.bukkitWorld().getEnvironment() == World.Environment.NETHER)
				.forEach(this::processPlayer);
	}

	private void processPlayer(@NotNull Player player) {
		if (!hasEmberFog)
			return;

		spawnEmberParticles(player);

		if (Math.random() < 0.02) {
			player.addPotionEffect(new PotionEffect(PotionUtils.getCompatiblePotionEffectType("CONFUSION", "NAUSEA"),
					80, 0, false, false));
		}
	}

	private void spawnEmberParticles(@NotNull Player player) {
		getWeatherRegion().thenAccept(region -> {
			if (region == null)
				return;
			World w = world.bukkitWorld();

			region.getBlocks().forEachRemaining(block -> {
				if (RandomUtils.generateRandomInt(6) != 1)
					return;
				w.spawnParticle(Particle.LAVA, block.getLocation(), 1, 0.3, 0.3, 0.3, 0.02);
				w.spawnParticle(Particle.FLAME, block.getLocation(), 1, 0.3, 0.3, 0.3, 0.01);
			});
		});
	}

	public CompletableFuture<NetherWeatherRegion> getWeatherRegion() {
		NetherWeatherManager weatherManager = instance.getNetherWeatherManager();
		return instance.getIslandManager().getIslandCenterLocation(islandId).thenApply(center -> {
			if (center == null || center.getWorld() == null
					|| !center.getWorld().getName().equalsIgnoreCase(world.worldName()))
				return null;

			int radius = instance.getConfigManager().radius();
			Location min = center.clone().subtract(radius, 0, radius).add(0, 20, 0);
			Location max = center.clone().add(radius, 20, radius);
			return weatherManager.new NetherWeatherRegion(min, max);
		});
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
						.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_EMBERFOG_WARNING.build())));
	}

	private void handleFogEnd() {
		if (!isWeatherAllowed(getType()))
			return;

		hasEmberFog = false;
		recentlyOccurred = true;
		islandContext.arg(ContextKeys.EMBER_FOG, false);
		stop();
		instance.getScheduler().asyncLater(() -> recentlyOccurred = false, 5, TimeUnit.MINUTES);
		instance.getNetherWeatherManager().onWeatherEnd(islandId, getType());
	}

	@Override
	public void stop() {
		cancel();
		this.hasEmberFog = false;
		this.waitingForNextCycle = false;
		islandContext.arg(ContextKeys.EMBER_FOG, false);
	}

	@Override
	public boolean canRun(@NotNull HellblockWorld<?> world) {
		return world.bukkitWorld().getEnvironment() == World.Environment.NETHER;
	}

	@Override
	public @NotNull WeatherType getType() {
		return WeatherType.EMBER_FOG;
	}
}