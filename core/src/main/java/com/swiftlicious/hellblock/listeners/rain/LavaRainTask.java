package com.swiftlicious.hellblock.listeners.rain;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
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
import com.swiftlicious.hellblock.listeners.rain.LavaRain.LavaRainLocation;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.RandomUtils;

/**
 * A task responsible for animating lava rain on a completely random timer
 * sequence.
 */
public class LavaRainTask implements Runnable {

	protected final HellblockPlugin instance;

	private final SchedulerTask cancellableTask;
	private boolean isRaining;
	private long howLongRainLasts;
	private boolean hasRainedRecently;
	private boolean waitCache;

	/**
	 * Constructs a new LavaRainTask.
	 *
	 * @param plugin            The Plugin instance.
	 * @param isRaining         Whether it is currently raining.
	 * @param hasRainedRecently Whether it has rained recently.
	 * @param howLongItRained   How long it will rain for.
	 */
	public LavaRainTask(HellblockPlugin plugin, boolean isRaining, boolean hasRainedRecently, long howLongRainLasts) {
		instance = plugin;
		this.isRaining = isRaining;
		this.hasRainedRecently = hasRainedRecently;
		this.howLongRainLasts = howLongRainLasts;
		this.cancellableTask = plugin.getScheduler().sync().runRepeating(this, 0, instance.getConfigManager().delay(),
				null);
	}

	@Override
	public void run() {
		if (!instance.getConfigManager().lavaRainEnabled())
			return;

		Iterator<Player> players = instance.getStorageManager().getOnlineUsers().stream()
				.filter(user -> user != null && user.isOnline()).map(UserData::getPlayer).iterator();

		while (true) {

			if (getHowLongItWillLavaRainFor() <= 0) {
				if (!this.waitCache) {
					setLavaRainStatus(false);
					setHasLavaRainedRecently(true);
					instance.getScheduler().asyncLater(() -> setHasLavaRainedRecently(false), 5, TimeUnit.MINUTES);
					instance.getScheduler().asyncLater(() -> {
						setHowLongItWillLavaRainFor(RandomUtils.generateRandomInt(150, 300));
						setLavaRainStatus(true);
						this.waitCache = false;
					}, RandomUtils.generateRandomInt(10, 25), TimeUnit.MINUTES);
					this.waitCache = true;
				}
				return;
			}

			labelLavaRain: while (true) {
				Player player;
				do {
					if (!players.hasNext()) {
						setHowLongItWillLavaRainFor(
								getHowLongItWillLavaRainFor() > 0 ? getHowLongItWillLavaRainFor() - 1 : 0);
						return;
					}

					player = players.next();
				} while (!player.isOp() && !player.hasPermission("hellblock.admin"));

				Location location = player.getLocation();
				if (location == null)
					return;
				World world = player.getWorld();
				Location add = location.clone().add((double) instance.getConfigManager().radius(), 20.0D,
						(double) instance.getConfigManager().radius());
				Location sub = location.clone().subtract((double) instance.getConfigManager().radius(), 0.0D,
						(double) instance.getConfigManager().radius()).add(0.0D, 20.0D, 0.0D);
				LavaRainLocation lavaRainLocation = instance.getLavaRainHandler().new LavaRainLocation(add, sub);
				Iterator<Block> blocks = lavaRainLocation.getBlocks();
				Block block;
				Block particleSpawn;
				if (world.getName().equalsIgnoreCase(instance.getConfigManager().worldName())
						&& !instance.getHellblockHandler().checkIfInSpawn(location)) {
					while (true) {
						while (true) {
							do {
								do {
									if (!blocks.hasNext()) {
										block = instance.getLavaRainHandler().getHighestBlock(location);
										if (block != null && (block.isPassable() || block.isEmpty() || block.isLiquid()
												|| !block.isSolid() || block.getType().isOccluding())) {
											ItemStack helmet = player.getInventory().getHelmet();
											boolean checkHelmet = false;
											if (helmet != null && helmet.getType() != Material.AIR) {
												if (instance.getNetherArmorHandler().isNetherArmorEnabled(helmet)) {
													if (instance.getNetherArmorHandler().checkArmorData(helmet)
															&& instance.getNetherArmorHandler().getArmorData(helmet)) {
														checkHelmet = true;
													}
												}
											}
											if (!checkHelmet
													&& !player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)
													&& player.getFireTicks() <= 10) {
												player.setFireTicks(120);
											}
										}
										if (instance.getLavaRainHandler().canHurtLivingCreatures()) {
											Collection<LivingEntity> entities = world.getNearbyLivingEntities(location,
													20.0D, (e) -> e.getType() != EntityType.PLAYER);
											for (LivingEntity living : entities) {
												Block above = instance.getLavaRainHandler()
														.getHighestBlock(living.getLocation());
												if (above != null
														&& (above.isPassable() || above.isEmpty() || above.isLiquid()
																|| !above.isSolid() || above.getType().isOccluding())) {
													if (!living.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)
															&& living.getFireTicks() <= 10)
														living.setFireTicks(120);
												}
											}
										}
										continue labelLavaRain;
									}

									block = blocks.next();
								} while (RandomUtils.generateRandomInt(8) != 1);

								BlockIterator iterator = new BlockIterator(world, block.getLocation().toVector(),
										new Vector(0, -1, 0), 0.0D, 30);
								particleSpawn = null;

								while (iterator.hasNext()) {
									Block air = iterator.next();
									if (air.isEmpty()) {
										particleSpawn = air;
										break;
									}
								}

								if (particleSpawn != null) {
									world.spawnParticle(Particle.DRIPPING_LAVA, particleSpawn.getLocation(), 1, 0.0D,
											0.0D, 0.0D, 0.0D);
									instance.getAdventureManager().playSound(player.getLocation(),
											net.kyori.adventure.sound.Sound.Source.WEATHER, net.kyori.adventure.key.Key
													.key("minecraft:block.pointed_dripstone.drip_lava"),
											1, 1);
								}
							} while (Math.random() >= (double) (instance.getConfigManager().fireChance() / 1000.0D));

							if (Math.random() < (double) (instance.getConfigManager().fireChance() / 1000.0D)) {
								for (Iterator<Block> fireIterator = lavaRainLocation.getBlocks(); fireIterator
										.hasNext();) {
									Block fire = fireIterator.next();
									if (fire.isEmpty()) {
										fire.setType(fire.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND
												|| fire.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL
														? Material.SOUL_FIRE
														: Material.FIRE);
										break;
									}
								}
							}

							if (instance.getLavaRainHandler().willTNTExplode()) {
								for (Iterator<Block> tntIterator = lavaRainLocation.getBlocks(); tntIterator
										.hasNext();) {
									Block tnt = tntIterator.next();
									Block aboveTnt = world.getHighestBlockAt(tnt.getLocation());
									if (aboveTnt.getType() == Material.TNT) {
										aboveTnt.setType(Material.AIR);
										world.spawn(aboveTnt.getLocation(), TNTPrimed.class, (primed) -> {
											primed.setFuseTicks(RandomUtils.generateRandomInt(3, 5) * 20);
										});
									}
								}
								Collection<Entity> entities = world.getNearbyEntities(location, 15.0D, 15.0D, 15.0D,
										(e) -> e.getType() == EntityType.TNT_MINECART);
								for (Entity entity : entities) {
									Block aboveMinecart = instance.getLavaRainHandler()
											.getHighestBlock(entity.getLocation());
									if (aboveMinecart != null && (aboveMinecart.isPassable() || aboveMinecart.isEmpty()
											|| aboveMinecart.isLiquid() || !aboveMinecart.isSolid()
											|| aboveMinecart.getType().isOccluding())) {
										ExplosiveMinecart tntMinecart = (ExplosiveMinecart) entity;
										if (!tntMinecart.isIgnited())
											tntMinecart.ignite();
									}
								}
							}

							setHowLongItWillLavaRainFor(
									getHowLongItWillLavaRainFor() > 0 ? getHowLongItWillLavaRainFor() - 1 : 0);
						}
					}
				}
			}
		}
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
		if (this.cancellableTask != null)
			this.cancellableTask.cancel();
		this.isRaining = false;
		this.howLongRainLasts = 0L;
	}
}
