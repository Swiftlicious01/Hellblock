package com.swiftlicious.hellblock.listeners.rain;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.listeners.rain.RainHandler.LavaRainLocation;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.sound.Sound;

/**
 * A task responsible for animating lava rain on a completely random timer
 * sequence.
 */
public final class LavaRainTask implements Runnable {

	private final HellblockPlugin instance;
	private final HellblockWorld<?> world;
	private final SchedulerTask cancellableTask;

	private boolean isRaining;
	private long howLongRainLasts;
	private boolean hasRainedRecently;

	private boolean waitCache;

	// according to me all nether mobs would be immune to the effects of lava +
	// warden obviously and vex is like a ghost so it makes sense?
	private static final Set<EntityType> IMMUNE_MOBS = Set.of(EntityType.PLAYER, EntityType.STRIDER, EntityType.BLAZE,
			EntityType.WITHER_SKELETON, EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOGLIN, EntityType.VEX,
			EntityType.WITHER, EntityType.WARDEN, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN,
			EntityType.MAGMA_CUBE, EntityType.GHAST);

	/**
	 * Constructs a new LavaRainTask.
	 *
	 * @param plugin            The Plugin instance.
	 * @param world             The world it is currently raining in.
	 * @param isRaining         Whether it is currently raining.
	 * @param hasRainedRecently Whether it has rained recently.
	 * @param howLongItRained   How long it will rain for.
	 */
	public LavaRainTask(HellblockPlugin plugin, HellblockWorld<?> world, boolean isRaining, boolean hasRainedRecently,
			long howLongRainLasts) {
		this.instance = plugin;
		this.world = world;
		this.isRaining = isRaining;
		this.hasRainedRecently = hasRainedRecently;
		this.howLongRainLasts = howLongRainLasts;
		this.cancellableTask = plugin.getScheduler().sync().runRepeating(this::run, 0,
				instance.getConfigManager().delay(), LocationUtils.getAnyLocationInstance());

		// Bootstrap the cycle if not starting with rain
		if (!isRaining) {
			// This schedules cooldown + warning + first rain
			handleRainEnd();
		}
	}

	@Override
	public void run() {
		if (!instance.getConfigManager().lavaRainEnabled()) {
			return;
		}

		tickRainCycle();

		instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer).filter(this::isInTargetWorld)
				.forEach(this::processPlayer);
	}

	private void tickRainCycle() {
		if (howLongRainLasts <= 0) {
			if (!waitCache && isRaining) {
				handleRainEnd(); // handles cooldown, warning, and next rain scheduling
			}
		} else if (isRaining) {
			howLongRainLasts--;
		}
	}

	private void processPlayer(Player player) {
		if (!isRaining) {
			return;
		}

		handlePlayerEffects(player);
		handleMobEffects(player);
		spawnParticlesAndFire(player);
		if (instance.getLavaRainHandler().willTNTExplode()) {
			triggerTNTEvents(player.getWorld(), player.getLocation());
		}
	}

	private void broadcastWarning() {
		if (!instance.getLavaRainHandler().willSendWarning()) {
			return;
		}
		instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer).filter(this::isInTargetWorld)
				.forEach(player -> instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LAVARAIN_WARNING.build())));
	}

	private void handlePlayerEffects(Player player) {
		final Block highest = instance.getLavaRainHandler().getHighestBlock(player.getLocation());
		if (highest == null) {
			return;
		}

		final boolean hasHelmet = hasValidNetherHelmet(player.getInventory().getHelmet());
		if (!hasHelmet && !player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && player.getFireTicks() <= 10) {
			player.setFireTicks(120);
		}
	}

	private boolean hasValidNetherHelmet(ItemStack helmet) {
		return helmet != null && helmet.getType() != Material.AIR
				&& instance.getNetherArmorHandler().isNetherArmorEnabled(helmet)
				&& instance.getNetherArmorHandler().checkArmorData(helmet)
				&& instance.getNetherArmorHandler().getArmorData(helmet);
	}

	private void handleMobEffects(Player player) {
		if (!instance.getLavaRainHandler().canHurtLivingCreatures()) {
			return;
		}

		final World w = world.bukkitWorld();
		final Collection<Entity> entities = w.getNearbyEntities(player.getLocation(), 20.0D, 20.0D, 20.0D,
				e -> !IMMUNE_MOBS.contains(e.getType()) && e instanceof LivingEntity);

		entities.stream().map(e -> (LivingEntity) e).filter(living -> {
			final Block above = instance.getLavaRainHandler().getHighestBlock(living.getLocation());
			return above != null && (above.isPassable() || above.isEmpty() || above.isLiquid())
					&& !living.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && living.getFireTicks() <= 10;
		}).forEach(living -> living.setFireTicks(120));
	}

	private void spawnParticlesAndFire(Player player) {
		final World w = world.bukkitWorld();
		final LavaRainLocation rainArea = getRainArea(player.getLocation());

		rainArea.getBlocks().forEachRemaining(block -> {
			if (RandomUtils.generateRandomInt(8) != 1) {
				return;
			}

			final Block particleBlock = findAirBelow(block);
			if (particleBlock != null) {
				Block highest = instance.getLavaRainHandler().getHighestBlock(particleBlock.getLocation());
				if (highest != null && highest.getY() > particleBlock.getY())
					return;

				w.spawnParticle(Particle.DRIPPING_LAVA, particleBlock.getLocation(), 1);
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.pointed_dripstone.drip_lava"),
								Sound.Source.WEATHER, 1F, 1F),
						player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
			}

			if (Math.random() < instance.getConfigManager().fireChance() / 1000.0D) {
				tryPlaceFire(rainArea);
			}
		});

		// Fill cauldrons with lava during lava rain
		fillNearbyCauldronsWithLava(player.getLocation());
	}

	private void fillNearbyCauldronsWithLava(Location center) {
		final World world = center.getWorld();
		if (world == null)
			return;

		// Define search radius (similar to lava rain radius)
		final int radius = instance.getConfigManager().radius();

		for (int x = -radius; x <= radius; x++) {
			for (int y = -2; y <= 2; y++) { // Vertical search - limited
				for (int z = -radius; z <= radius; z++) {
					Block block = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y,
							center.getBlockZ() + z);

					if (block.getType() == Material.CAULDRON || block.getType() == Material.LAVA_CAULDRON) {
						// Optional: check if exposed to sky/lava rain (not covered)
						Block above = block.getRelative(BlockFace.UP);
						if (!above.isPassable())
							continue;

						// Small random chance (like water cauldrons in vanilla rain)
						if (Math.random() < 0.005) { // 0.5% chance per tick
							block.setType(Material.LAVA_CAULDRON);
							block.getState().update(true, true);
						}
					}
				}
			}
		}
	}

	private Block findAirBelow(Block start) {
		final BlockIterator it = new BlockIterator(world.bukkitWorld(), start.getLocation().toVector(),
				new Vector(0, -1, 0), 0.0D, 30);
		while (it.hasNext()) {
			final Block b = it.next();
			if (b.isEmpty()) {
				return b;
			}
		}
		return null;
	}

	private void tryPlaceFire(LavaRainLocation area) {
		area.getBlocks().forEachRemaining(block -> {
			if (!block.isEmpty())
				return;

			// Prevent placing fire under cover
			Block highest = instance.getLavaRainHandler().getHighestBlock(block.getLocation());
			if (highest != null && highest.getY() > block.getY())
				return;

			final Block below = block.getRelative(BlockFace.DOWN);
			final Material fireType = (below.getType() == Material.SOUL_SAND || below.getType() == Material.SOUL_SOIL)
					? Material.SOUL_FIRE
					: Material.FIRE;

			block.setType(fireType);
			block.getState().update(true, false);
		});
	}

	private void triggerTNTEvents(World w, Location location) {
		// TNT blocks
		getRainArea(location).getBlocks().forEachRemaining(block -> {
			final Block top = w.getHighestBlockAt(block.getLocation());
			if (top.getType() == Material.TNT) {
				top.setType(Material.AIR);
				w.spawn(top.getLocation(), TNTPrimed.class,
						primed -> primed.setFuseTicks(RandomUtils.generateRandomInt(3, 5) * 20));
			}
		});

		// TNT minecarts
		w.getNearbyEntities(location, 15, 15, 15, e -> e.getType() == EntityType.TNT_MINECART).forEach(entity -> {
			final Block top = instance.getLavaRainHandler().getHighestBlock(entity.getLocation());
			if (top != null && (top.isPassable() || top.isEmpty() || top.isLiquid())) {
				((ExplosiveMinecart) entity).ignite();
			}
		});
	}

	private void handleRainEnd() {
		setLavaRainStatus(false);
		setHasLavaRainedRecently(true);

		// reset "recently rained" after 5 minutes
		instance.getScheduler().asyncLater(() -> setHasLavaRainedRecently(false), 5, TimeUnit.MINUTES);

		// instead of scheduling next rain right away, just mark waiting
		this.waitCache = true;

		// schedule the start of next rain directly (cooldown happens before start)
		final long cooldownMinutes = RandomUtils.generateRandomInt(10, 25);
		final long cooldownTicks = TimeUnit.MINUTES.toSeconds(cooldownMinutes) * 20;

		// schedule the warning 5 seconds before rain
		final long warnDelayTicks = Math.max(cooldownTicks - 100, 0);
		instance.getScheduler().sync().runLater(this::broadcastWarning, warnDelayTicks,
				LocationUtils.getAnyLocationInstance());

		// schedule the actual rain start after cooldown
		instance.getScheduler().asyncLater(this::startNextRain, cooldownMinutes, TimeUnit.MINUTES);
	}

	private void startNextRain() {
		setHowLongItWillLavaRainFor(RandomUtils.generateRandomInt(150, 300));
		setLavaRainStatus(true);
		this.waitCache = false; // ready for next cycle
	}

	private LavaRainLocation getRainArea(Location center) {
		final Location add = center.clone().add(instance.getConfigManager().radius(), 20,
				instance.getConfigManager().radius());
		final Location sub = center.clone()
				.subtract(instance.getConfigManager().radius(), 0, instance.getConfigManager().radius()).add(0, 20, 0);
		return instance.getLavaRainHandler().new LavaRainLocation(add, sub);
	}

	private boolean isInTargetWorld(Player player) {
		return player != null && player.isOnline() && player.getWorld().getName().equalsIgnoreCase(world.worldName())
				&& instance.getHellblockHandler().isInCorrectWorld(player)
				&& world.bukkitWorld().getEnvironment() == Environment.NETHER;
	}

	public HellblockWorld<?> getWorld() {
		return this.world;
	}

	public boolean isLavaRaining() {
		return this.isRaining;
	}

	public long getHowLongItWillLavaRainFor() {
		return this.howLongRainLasts;
	}

	public boolean hasLavaRainedRecently() {
		return this.hasRainedRecently;
	}

	public void setLavaRainStatus(boolean isRaining) {
		this.isRaining = isRaining;
	}

	public void setHowLongItWillLavaRainFor(long howLongRainLasts) {
		this.howLongRainLasts = howLongRainLasts;
	}

	public void setHasLavaRainedRecently(boolean hasRainedRecently) {
		this.hasRainedRecently = hasRainedRecently;
	}

	/**
	 * Cancels the rain animation and cleans up resources.
	 */
	public void cancelAnimation() {
		if (!this.cancellableTask.isCancelled()) {
			this.cancellableTask.cancel();
		}
		this.isRaining = false;
		this.howLongRainLasts = 0L;
	}
}