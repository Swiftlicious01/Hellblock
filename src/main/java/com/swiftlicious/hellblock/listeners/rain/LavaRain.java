package com.swiftlicious.hellblock.listeners.rain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.RandomUtils;

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
				() -> this.lavaRainTask = new LavaRainTask(instance, true, false,
						RandomUtils.generateRandomInt(150, 250)),
				RandomUtils.generateRandomInt(10, 15), TimeUnit.MINUTES);
	}

	public void stopLavaRainProcess() {
		if (this.lavaRainTask != null) {
			this.lavaRainTask.cancelAnimation();
			lavaRainTask = null;
		}
	}

	public @Nullable Block getHighestBlock(@Nullable Location location) {
		if (location == null || location.getWorld() == null)
			return null;

		Block highestBlock = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(),
				location.getBlockZ());
		highestBlock = highestBlock.getType().isAir() ? highestBlock
				: location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
		for (int y = 0; y < 9; y++) {
			// +2 for equal to eye level
			Block block = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() + 2 + y,
					location.getBlockZ());
			if (Tag.AIR.isTagged(block.getType()) || Tag.ALL_SIGNS.isTagged(block.getType())
					|| Tag.BANNERS.isTagged(block.getType()) || Tag.FENCES.isTagged(block.getType())
					|| Tag.FENCE_GATES.isTagged(block.getType()) || Tag.DOORS.isTagged(block.getType())
					|| Tag.BUTTONS.isTagged(block.getType()) || Tag.PRESSURE_PLATES.isTagged(block.getType())
					|| Tag.FIRE.isTagged(block.getType()) || block.getType() == Material.LAVA
					|| block.getType() == Material.WATER || block.getType() == Material.COBWEB
					|| block.getType() == Material.STRING || block.getType() == Material.FLOWER_POT
					|| Tag.ITEMS_BOATS.isTagged(block.getType()) || Tag.ITEMS_CHEST_BOATS.isTagged(block.getType())
					|| block.getType() == Material.MINECART || block.getType() == Material.CHEST_MINECART
					|| block.getType() == Material.BAMBOO || block.getType() == Material.BAMBOO_RAFT
					|| block.getType() == Material.FURNACE_MINECART || block.getType() == Material.HOPPER_MINECART
					|| block.getType() == Material.BAMBOO_CHEST_RAFT
					|| block.getType() == Material.COMMAND_BLOCK_MINECART || block.getType() == Material.NETHER_PORTAL
					|| block.getType() == Material.END_PORTAL || block.getType() == Material.END_GATEWAY
					|| block.getType() == Material.LADDER || block.getType() == Material.CHAIN
					|| block.getType() == Material.CANDLE || block.getType() == Material.SEA_PICKLE
					|| block.getType() == Material.VINE || block.getType() == Material.TWISTING_VINES
					|| block.getType() == Material.WEEPING_VINES || block.getType() == Material.END_ROD
					|| block.getType() == Material.LIGHTNING_ROD || block.getType() == Material.LEVER
					|| block.getType() == Material.SWEET_BERRY_BUSH || block.getType() == Material.SCAFFOLDING
					|| block.getType() == Material.LANTERN || block.getType() == Material.SOUL_LANTERN
					|| block.getType() == Material.TURTLE_EGG || block.getType() == Material.SMALL_DRIPLEAF
					|| block.getType() == Material.IRON_BARS || block.getType() == Material.POWDER_SNOW
					|| block.getType() == Material.TRIPWIRE || block.getType() == Material.TNT
					|| block.getType() == Material.TNT_MINECART)
				continue;
			// get first non-empty block above player
			highestBlock = block;
			break;
		}

		return highestBlock;
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
