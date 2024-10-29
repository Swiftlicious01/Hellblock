package com.swiftlicious.hellblock.listeners.rain;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.listeners.rain.LavaRain.LavaRainLocation;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.RandomUtils;

/**
 * A task responsible for animating lava rain on a completely random timer
 * sequence.
 */
public class LavaRainTask implements Runnable {

	private final CancellableTask cancellableTask;
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
		this.isRaining = isRaining;
		this.hasRainedRecently = hasRainedRecently;
		this.howLongRainLasts = howLongRainLasts;
		this.cancellableTask = plugin.getScheduler().runTaskSyncTimer(this,
				new Location(plugin.getHellblockHandler().getHellblockWorld(), 0,
						plugin.getHellblockHandler().getHeight(), 0),
				5, plugin.getLavaRain().getTaskDelay());
	}

	@Override
	public void run() {

		Iterator<HellblockPlayer> players = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers()
				.values().iterator();

		while (true) {

			if (getHowLongItWillLavaRainFor() <= 0) {
				if (!this.waitCache) {
					setLavaRainStatus(false);
					setHasLavaRainedRecently(true);
					HellblockPlugin.getInstance().getScheduler()
							.runTaskAsyncLater(() -> setHasLavaRainedRecently(false), 5, TimeUnit.MINUTES);
					HellblockPlugin.getInstance().getScheduler().runTaskAsyncLater(() -> {
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

					player = players.next().getPlayer();
				} while (player != null && !player.isOp());

				if (player != null) {
					Location location = player.getLocation();
					if (location == null)
						return;
					World world = player.getWorld();
					Location add = location.clone().add(
							(double) HellblockPlugin.getInstance().getLavaRain().getRadius(), 20.0D,
							(double) HellblockPlugin.getInstance().getLavaRain().getRadius());
					Location sub = location.clone()
							.subtract((double) HellblockPlugin.getInstance().getLavaRain().getRadius(), 0.0D,
									(double) HellblockPlugin.getInstance().getLavaRain().getRadius())
							.add(0.0D, 20.0D, 0.0D);
					LavaRainLocation lavaRainLocation = HellblockPlugin.getInstance()
							.getLavaRain().new LavaRainLocation(add, sub);
					Iterator<Block> blocks = lavaRainLocation.getBlocks();
					Block block;
					Block block2;
					if (world.getName()
							.equalsIgnoreCase(HellblockPlugin.getInstance().getHellblockHandler().getWorldName())
							&& !HellblockPlugin.getInstance().getHellblockHandler().checkIfInSpawn(location)) {
						while (true) {
							while (true) {
								do {
									do {
										if (!blocks.hasNext()) {
											block = HellblockPlugin.getInstance().getLavaRain()
													.getHighestBlock(location);
											if (block != null) {
												ItemStack[] armorSet = player.getInventory().getArmorContents();
												boolean checkArmor = false;
												if (armorSet != null) {
													for (ItemStack item : armorSet) {
														if (item == null || item.getType() == Material.AIR)
															continue;
														if (HellblockPlugin.getInstance().getNetherArmor()
																.isNetherArmorEnabled(item)) {
															if (HellblockPlugin.getInstance().getNetherArmor()
																	.checkArmorData(item)
																	&& HellblockPlugin.getInstance().getNetherArmor()
																			.getArmorData(item)) {
																checkArmor = true;
																break;
															}
														}
													}
												}
												if (!checkArmor) {
													player.setFireTicks(120);
												}
											}

											Collection<LivingEntity> entities = location.getWorld()
													.getNearbyLivingEntities(location, 35.0D);
											for (LivingEntity living : entities) {
												if (living instanceof Player)
													continue;
												Block above = HellblockPlugin.getInstance().getLavaRain()
														.getHighestBlock(living.getLocation());
												if (above != null) {
													living.setFireTicks(120);
												}
											}
											continue labelLavaRain;
										}

										block = blocks.next();
									} while (new Random().nextInt(8) != 1);

									BlockIterator iterator = new BlockIterator(world, block.getLocation().toVector(),
											new Vector(0, -1, 0), 0.0D, 30);
									block2 = null;

									while (iterator.hasNext()) {
										Block block3 = iterator.next();
										if (block3.isEmpty()) {
											block2 = block3;
											break;
										}
									}

									if (block2 != null) {
										world.spawnParticle(Particle.DRIPPING_LAVA, block2.getLocation(), 1, 0.0D, 0.0D,
												0.0D, 0.0D);
										HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
												net.kyori.adventure.sound.Sound.Source.PLAYER,
												net.kyori.adventure.key.Key
														.key("minecraft:block.pointed_dripstone.drip_lava"),
												1, 1);
									}
								} while (Math.random() >= (double) HellblockPlugin.getInstance().getLavaRain()
										.getFireChance() / 1000.0D);

								BlockIterator iterator2 = new BlockIterator(world,
										block.getLocation().subtract(0.0D, 50.0D, 0.0D).toVector(), new Vector(0, 1, 0),
										0.0D, 30);

								while (iterator2.hasNext()) {
									Block var13 = iterator2.next();
									if (var13.isEmpty()) {
										var13.setType(var13.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND
												? Material.SOUL_FIRE
												: Material.FIRE);
										break;
									}
								}

								BlockIterator iterator3 = new BlockIterator(world,
										block.getLocation().subtract(0.0D, 50.0D, 0.0D).toVector(), new Vector(0, 1, 0),
										0.0D, 30);

								while (iterator3.hasNext()) {
									Block var14 = iterator3.next();
									if (var14.getType() == Material.TNT) {
										TNTPrimed tnt = (TNTPrimed) var14.getWorld().spawnEntity(block.getLocation(),
												EntityType.TNT);
										tnt.setFuseTicks(3 * 20);
										var14.setType(Material.AIR);
										var14.getState().update();
										break;
									}
									if (var14.getType() == Material.TNT_MINECART) {
										ExplosiveMinecart tntMinecart = (ExplosiveMinecart) var14.getWorld()
												.spawnEntity(block.getLocation(), EntityType.TNT_MINECART);
										tntMinecart.setFuseTicks(3 * 20);
										var14.setType(Material.AIR);
										var14.getState().update();
										break;
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
		this.cancellableTask.cancel();
		this.isRaining = false;
		this.howLongRainLasts = 0L;
	}
}
