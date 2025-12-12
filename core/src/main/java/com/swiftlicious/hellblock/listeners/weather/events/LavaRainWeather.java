package com.swiftlicious.hellblock.listeners.weather.events;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.listeners.weather.AbstractNetherWeatherTask;
import com.swiftlicious.hellblock.listeners.weather.NetherWeatherManager;
import com.swiftlicious.hellblock.listeners.weather.NetherWeatherManager.NetherWeatherRegion;
import com.swiftlicious.hellblock.listeners.weather.WeatherType;
import com.swiftlicious.hellblock.utils.EnumUtils;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

/**
 * Handles the Lava Rain Nether weather event for a specific island/world.
 */
public final class LavaRainWeather extends AbstractNetherWeatherTask {

	private final Context<Integer> islandContext;
	private static final Set<EntityType> IMMUNE_MOBS = buildImmuneMobs();
	private boolean hasLavaRain;

	@NotNull
	private static Set<EntityType> buildImmuneMobs() {
		Set<EntityType> set = EnumSet.noneOf(EntityType.class);
		List.of("PLAYER", "STRIDER", "BLAZE", "WITHER_SKELETON", "ZOMBIFIED_PIGLIN", "ZOGLIN", "VEX", "WITHER",
				"WARDEN", "PIGLIN", "PIGLIN_BRUTE", "HOGLIN", "MAGMA_CUBE", "GHAST").stream()
				.map(name -> EnumUtils.getEnum(EntityType.class, name)).filter(Objects::nonNull).forEach(set::add);
		return Set.copyOf(set);
	}

	public LavaRainWeather(@NotNull HellblockPlugin plugin, int islandId, @NotNull HellblockWorld<?> world) {
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
		this.hasLavaRain = true;
		this.islandContext.arg(ContextKeys.LAVA_RAIN, true);

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
			handleRainEnd();
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
		if (!hasLavaRain)
			return;

		handlePlayerEffects(player);
		handleMobEffects(player);
		spawnParticlesAndFire(player);

		if (instance.getNetherWeatherManager().willTNTExplode()) {
			triggerTNTEvents(player.getLocation());
		}
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
						.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LAVARAIN_WARNING.build())));
	}

	private void handlePlayerEffects(@NotNull Player player) {
		Block highest = instance.getNetherWeatherManager().getHighestBlock(player.getLocation());
		if (highest == null)
			return;

		boolean hasHelmet = hasValidNetherHelmet(player.getInventory().getHelmet());
		if (!hasHelmet && !player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && player.getFireTicks() <= 10) {
			player.setFireTicks(120);
		}
	}

	private boolean hasValidNetherHelmet(@Nullable ItemStack helmet) {
		return helmet != null && helmet.getType() != Material.AIR
				&& instance.getNetherArmorHandler().isNetherArmorEnabled(helmet)
				&& instance.getNetherArmorHandler().checkArmorData(helmet)
				&& instance.getNetherArmorHandler().getArmorData(helmet);
	}

	private void handleMobEffects(@NotNull Player player) {
		if (!instance.getNetherWeatherManager().canHurtLivingCreatures())
			return;

		World w = world.bukkitWorld();
		Collection<Entity> entities = w.getNearbyEntities(player.getLocation(), 20, 20, 20,
				e -> e instanceof LivingEntity && !IMMUNE_MOBS.contains(e.getType()));

		entities.stream().map(e -> (LivingEntity) e).filter(living -> {
			Block above = instance.getNetherWeatherManager().getHighestBlock(living.getLocation());
			return above != null && (above.isPassable() || above.isEmpty() || above.isLiquid())
					&& !living.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && living.getFireTicks() <= 10;
		}).forEach(living -> living.setFireTicks(120));
	}

	private void spawnParticlesAndFire(@NotNull Player player) {
		getWeatherRegion().thenAccept(region -> {
			if (region == null)
				return;
			World w = world.bukkitWorld();

			region.getBlocks().forEachRemaining(block -> {
				if (RandomUtils.generateRandomInt(8) != 1)
					return;

				Block particleBlock = findAirBelow(block);
				if (particleBlock != null) {
					Block highest = instance.getNetherWeatherManager().getHighestBlock(particleBlock.getLocation());
					if (highest != null && highest.getY() > particleBlock.getY())
						return;

					w.spawnParticle(ParticleUtils.getParticle("DRIP_LAVA"), particleBlock.getLocation(), 1);
					AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
							Sound.sound(Key.key("minecraft:block.pointed_dripstone.drip_lava"), Source.WEATHER, 1F, 1F),
							player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
				}

				if (Math.random() < instance.getConfigManager().fireChance() / 1000.0D) {
					tryPlaceFire(region);
				}
			});

			fillNearbyCauldronsWithLava(region);
		});
	}

	private void handleRainEnd() {
		if (!isWeatherAllowed(getType()))
			return;

		hasLavaRain = false;
		recentlyOccurred = true;
		islandContext.arg(ContextKeys.LAVA_RAIN, false);

		// Stop the repeating scheduler and mark inactive
		stop();

		// Reset flag eventually
		instance.getScheduler().asyncLater(() -> recentlyOccurred = false, 5, TimeUnit.MINUTES);

		// Delegate next-weather scheduling and warning
		instance.getNetherWeatherManager().onWeatherEnd(islandId, getType());
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

	@Nullable
	private Block findAirBelow(@NotNull Block start) {
		BlockIterator it = new BlockIterator(world.bukkitWorld(), start.getLocation().toVector(), new Vector(0, -1, 0),
				0.0D, 30);
		while (it.hasNext()) {
			Block b = it.next();
			if (b.isEmpty())
				return b;
		}
		return null;
	}

	private void tryPlaceFire(@NotNull NetherWeatherRegion region) {
		region.getBlocks().forEachRemaining(block -> {
			if (!block.isEmpty())
				return;

			Block highest = instance.getNetherWeatherManager().getHighestBlock(block.getLocation());
			if (highest != null && highest.getY() > block.getY())
				return;

			Block below = block.getRelative(BlockFace.DOWN);
			Material fireType = (below.getType() == Material.SOUL_SAND || below.getType() == Material.SOUL_SOIL)
					? Material.SOUL_FIRE
					: Material.FIRE;

			block.setType(fireType);
			block.getState().update(true, false);
		});
	}

	private void fillNearbyCauldronsWithLava(@NotNull NetherWeatherRegion region) {
		region.getBlocks().forEachRemaining(block -> {
			if (block.getType() == Material.CAULDRON || block.getType() == Material.LAVA_CAULDRON) {
				Block above = block.getRelative(BlockFace.UP);
				if (!above.isPassable())
					return;

				if (Math.random() < 0.005) {
					block.setType(Material.LAVA_CAULDRON);
					block.getState().update(true, true);
				}
			}
		});
	}

	private void triggerTNTEvents(@NotNull Location location) {
		getWeatherRegion().thenAccept(region -> {
			if (region == null)
				return;
			World w = world.bukkitWorld();

			region.getBlocks().forEachRemaining(block -> {
				Block top = w.getHighestBlockAt(block.getLocation());
				if (top.getType() == Material.TNT) {
					top.setType(Material.AIR);
					w.spawn(top.getLocation(), TNTPrimed.class,
							primed -> primed.setFuseTicks(RandomUtils.generateRandomInt(3, 5) * 20));
				}
			});
		});
	}

	@Override
	public void stop() {
		// Cancel the repeating scheduler
		cancel();

		// Mark both weather and task as inactive
		this.hasLavaRain = false;
		this.waitingForNextCycle = false;

		// Update context
		islandContext.arg(ContextKeys.LAVA_RAIN, false);
	}

	@Override
	public boolean canRun(@NotNull HellblockWorld<?> world) {
		return world.bukkitWorld().getEnvironment() == World.Environment.NETHER;
	}

	@Override
	public @NotNull WeatherType getType() {
		return WeatherType.LAVA_RAIN;
	}
}