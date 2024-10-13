package com.swiftlicious.hellblock.listeners.rain;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.listeners.rain.LavaRain.LavaRainLocation;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.scheduler.CancellableTask;

/**
 * A task responsible for animating lava rain on a completely random timer
 * sequence.
 */
public class LavaRainTask implements Runnable {

	private final CancellableTask cancellableTask;
	private boolean isRaining;
	private long howLongRainLasts;
	private boolean hasRainedRecently;

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
		this.cancellableTask = plugin.getScheduler().runTaskSyncTimer(this, new Location(plugin.getHellblockHandler().getHellblockWorld(), 0, 0, 0), 5, plugin.getLavaRain().getTaskDelay());
	}

	@Override
	public void run() {

		Iterator<HellblockPlayer> players = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values().iterator();

		if (!players.hasNext()) {
			this.cancelAnimation();
		}

		if (this.howLongRainLasts <= 0) {
			this.cancelAnimation();
		}

		while (true) {
			labelRain: while (true) {
				Player player;
				do {
					if (!players.hasNext()) {
						return;
					}

					player = (Player) players.next().getPlayer();
				} while (!player.isOp());

				Location location = player.getLocation();
				World world = player.getWorld();
				Location add = location.clone().add((double) HellblockPlugin.getInstance().getLavaRain().getRadius(), 20.0D, (double) HellblockPlugin.getInstance().getLavaRain().getRadius());
				Location sub = location.clone().subtract((double) HellblockPlugin.getInstance().getLavaRain().getRadius(), 0.0D, (double) HellblockPlugin.getInstance().getLavaRain().getRadius()).add(0.0D, 20.0D, 0.0D);
				LavaRainLocation lavaRainLocation = HellblockPlugin.getInstance().getLavaRain().new LavaRainLocation(add, sub);
				Iterator<Block> blocks = lavaRainLocation.getBlocks();
				Block block;
				Block block2;
				if (world.getName().equalsIgnoreCase(HellblockPlugin.getInstance().getHellblockHandler().getWorldName())) {
					while (true) {
						while (true) {
							do {
								do {
									if (!blocks.hasNext()) {
										block = location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP);
										if (block.isEmpty() && block.getRelative(BlockFace.UP).isEmpty() && player.getFireTicks() <= 10) {
											player.setFireTicks(30);
										}
										continue labelRain;
									}

									block = (Block) blocks.next();
								} while (new Random().nextInt(8) != 1);

								BlockIterator iterator = new BlockIterator(world, block.getLocation().toVector(), new Vector(0, -1, 0), 0.0D, 30);
								block2 = null;

								while (iterator.hasNext()) {
									Block block3 = iterator.next();
									if (block3.isEmpty()) {
										block2 = block3;
										break;
									}
								}

								if (block2 != null) {
									world.spawnParticle(Particle.DRIPPING_LAVA, block2.getLocation(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
								}
							} while (Math.random() >= (double) HellblockPlugin.getInstance().getLavaRain().getFireChance() / 1000.0D);

							BlockIterator iterator2 = new BlockIterator(world, block.getLocation().subtract(0.0D, 50.0D, 0.0D).toVector(), new Vector(0, 1, 0), 0.0D, 30);

							while (iterator2.hasNext()) {
								Block var13 = iterator2.next();
								if (var13.isEmpty()) {
									var13.setType(Material.FIRE);
									break;
								}
							}
							setHowLongItWillLavaRainFor(getHowLongItWillLavaRainFor() > 0 ? getHowLongItWillLavaRainFor() - 1 : 0);
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
		HellblockPlugin.getInstance().getScheduler().runTaskSyncLater(() -> setHasLavaRainedRecently(false), null, 5, TimeUnit.MINUTES);
	}
}
