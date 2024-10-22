package com.swiftlicious.hellblock.listeners.rain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;

import lombok.Getter;

@Getter
public class LavaRain {

	private int radius, fireChance, taskDelay;

	private LavaRainTask lavaRainTask;

	private final HellblockPlugin instance;

	public LavaRain(HellblockPlugin plugin) {
		instance = plugin;
		this.radius = Math.abs(instance.getConfig("config.yml").getInt("lava-rain-options.radius", 16));
		this.fireChance = Math.abs(instance.getConfig("config.yml").getInt("lava-rain-options.fireChance", 1));
		this.taskDelay = Math.abs(instance.getConfig("config.yml").getInt("lava-rain-options.taskDelay", 3));
	}

	public void startLavaRainProcess() {
		instance.getScheduler().runTaskAsyncLater(
				() -> this.lavaRainTask = new LavaRainTask(instance, true, false, RandomUtils.nextInt(150, 250)),
				RandomUtils.nextInt(10, 15), TimeUnit.MINUTES);
	}

	public void stopLavaRainProcess() {
		if (this.lavaRainTask != null) {
			this.lavaRainTask.cancelAnimation();
			lavaRainTask = null;
		}
	}

	public @Nullable Block getHighestBlock(@Nullable Location var1) {
		if (var1 == null || var1.getWorld() == null)
			return null;

		Block var2 = var1.getWorld().getBlockAt(var1.getBlockX(), var1.getBlockY(), var1.getBlockZ());
		var2 = var2.getType() == Material.AIR ? var2 : var1.getWorld().getBlockAt(var1.getBlockX(), var1.getBlockY() + 1, var1.getBlockZ());
		for (int y = 0; y < 9; y++) {
			// +2 for equal to eye level
			Block var3 = var1.getWorld().getBlockAt(var1.getBlockX(), var1.getBlockY() + 2 + y, var1.getBlockZ());
			if (var3.getType() == Material.AIR)
				continue;
			// get first non-empty block above player
			var2 = var3;
			break;
		}

		return var2;
	}

	public class LavaRainLocation {

		private final int x1;
		private final int x2;
		private final int y1;
		private final int y2;
		private final int z1;
		private final int z2;
		private final World world;

		public LavaRainLocation(Location var1, Location var2) {
			this.x1 = Math.min(var1.getBlockX(), var2.getBlockX());
			this.x2 = Math.max(var1.getBlockX(), var2.getBlockX());
			this.y1 = Math.min(var1.getBlockY(), var2.getBlockY());
			this.y2 = Math.max(var1.getBlockY(), var2.getBlockY());
			this.z1 = Math.min(var1.getBlockZ(), var2.getBlockZ());
			this.z2 = Math.max(var1.getBlockZ(), var2.getBlockZ());
			this.world = var1.getWorld();
		}

		public Iterator<Block> getBlocks() {
			List<Block> var1 = new ArrayList<Block>((int) this.getDistanceSquared());

			for (int var2 = this.x1; var2 <= this.x2; ++var2) {
				for (int var3 = this.y1; var3 <= this.y2; ++var3) {
					for (int var4 = this.z1; var4 <= this.z2; ++var4) {
						Block var5 = this.world.getBlockAt(var2, var3, var4);
						var1.add(var5);
					}
				}
			}

			return var1.iterator();
		}

		public Location getMainLocation() {
			return new Location(this.world, (double) ((this.x2 - this.x1) / 2 + this.x1),
					(double) ((this.y2 - this.y1) / 2 + this.y1), (double) ((this.z2 - this.z1) / 2 + this.z1));
		}

		public double getDistance() {
			return this.getMinLocation().distance(this.getMaxLocation());
		}

		public double getDistanceSquared() {
			return this.getMinLocation().distanceSquared(this.getMaxLocation());
		}

		public int getY() {
			return this.y2 - this.y1 + 1;
		}

		public Location getMinLocation() {
			return new Location(this.world, (double) this.x1, (double) this.y1, (double) this.z1);
		}

		public Location getMaxLocation() {
			return new Location(this.world, (double) this.x2, (double) this.y2, (double) this.z2);
		}

		public Location getRandomLocation() {
			Random var1 = new Random();
			int var2 = var1.nextInt(Math.abs(this.x2 - this.x1) + 1) + this.x1;
			int var3 = var1.nextInt(Math.abs(this.y2 - this.y1) + 1) + this.y1;
			int var4 = var1.nextInt(Math.abs(this.z2 - this.z1) + 1) + this.z1;
			return new Location(this.world, (double) var2, (double) var3, (double) var4);
		}

		public int getAllCoordinates() {
			return this.getY() * this.getX() * this.getZ();
		}

		public int getX() {
			return this.x2 - this.x1 + 1;
		}

		public int getZ() {
			return this.z2 - this.z1 + 1;
		}

		public boolean checkLocation(Location var1) {
			return var1.getWorld() == this.world && var1.getBlockX() >= this.x1 && var1.getBlockX() <= this.x2
					&& var1.getBlockY() >= this.y1 && var1.getBlockY() <= this.y2 && var1.getBlockZ() >= this.z1
					&& var1.getBlockZ() <= this.z2;
		}

		public boolean matches(Player var1) {
			return this.checkLocation(var1.getLocation());
		}
	}
}
