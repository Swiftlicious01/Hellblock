package com.swiftlicious.hellblock.listeners.rain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.RandomUtils;

import lombok.Getter;
import lombok.NonNull;

public class LavaRain {

	@Getter
	private int radius, fireChance, taskDelay;
	@Getter
	private boolean enabled;
	private boolean canHurtLivingCreatures, willTNTExplode;
	@Getter
	private LavaRainTask lavaRainTask;

	private final HellblockPlugin instance;

	public LavaRain(HellblockPlugin plugin) {
		instance = plugin;
		this.radius = Math.abs(instance.getConfig("config.yml").getInt("lava-rain-options.radius", 16));
		this.fireChance = Math.abs(instance.getConfig("config.yml").getInt("lava-rain-options.fireChance", 1));
		this.taskDelay = Math.abs(instance.getConfig("config.yml").getInt("lava-rain-options.taskDelay", 3));
		this.enabled = instance.getConfig("config.yml").getBoolean("lava-rain-options.enabled", true);
		this.canHurtLivingCreatures = instance.getConfig("config.yml")
				.getBoolean("lava-rain-options.canHurtLivingCreatures", true);
		this.willTNTExplode = instance.getConfig("config.yml").getBoolean("lava-rain-options.willTNTExplode", true);
	}

	public void startLavaRainProcess() {
		if (!this.enabled)
			return;
		instance.getScheduler().runTaskAsyncLater(
				() -> this.lavaRainTask = new LavaRainTask(instance, true, false,
						RandomUtils.generateRandomInt(150, 250)),
				RandomUtils.generateRandomInt(10, 15), TimeUnit.MINUTES);
	}

	public void stopLavaRainProcess() {
		if (this.lavaRainTask != null) {
			this.lavaRainTask.cancelAnimation();
			this.lavaRainTask = null;
		}
	}

	public boolean canHurtLivingCreatures() {
		return this.canHurtLivingCreatures;
	}

	public boolean willTNTExplode() {
		return this.willTNTExplode;
	}

	public @Nullable Block getHighestBlock(@Nullable Location location) {
		if (location == null || location.getWorld() == null)
			return null;

		Block highestBlock = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(),
				location.getBlockZ());
		for (int y = 0; y < 10; y++) {
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
					|| block.getType() == Material.BIG_DRIPLEAF || block.getType() == Material.IRON_BARS
					|| block.getType() == Material.POWDER_SNOW || block.getType() == Material.TRIPWIRE
					|| block.getType() == Material.TRIPWIRE_HOOK || block.getType() == Material.TNT_MINECART
					|| block.getType().toString().contains("GLASS_PANE"))
				continue;
			// get first non-empty full block above player
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

		public LavaRainLocation(@NonNull Location min, @NonNull Location max) {
			this.x1 = Math.min(min.getBlockX(), max.getBlockX());
			this.x2 = Math.max(min.getBlockX(), max.getBlockX());
			this.y1 = Math.min(min.getBlockY(), max.getBlockY());
			this.y2 = Math.max(min.getBlockY(), max.getBlockY());
			this.z1 = Math.min(min.getBlockZ(), max.getBlockZ());
			this.z2 = Math.max(min.getBlockZ(), max.getBlockZ());
		}

		public @NonNull Iterator<Block> getBlocks() {
			List<Block> list = new ArrayList<>(this.getAllCoordinates());

			for (int x = this.x1; x <= this.x2; ++x) {
				for (int y = this.y1; y <= this.y2; ++y) {
					for (int z = this.z1; z <= this.z2; ++z) {
						Block block = instance.getHellblockHandler().getHellblockWorld().getBlockAt(x, y, z);
						list.add(block);
					}
				}
			}

			return list.iterator();
		}

		public @NonNull Location getMainLocation() {
			return new Location(instance.getHellblockHandler().getHellblockWorld(),
					(double) ((this.x2 - this.x1) / 2 + this.x1), (double) ((this.y2 - this.y1) / 2 + this.y1),
					(double) ((this.z2 - this.z1) / 2 + this.z1));
		}

		public double getDistance() {
			return this.getMinLocation().distance(this.getMaxLocation());
		}

		public double getDistanceSquared() {
			return this.getMinLocation().distanceSquared(this.getMaxLocation());
		}

		public @NonNull Location getMinLocation() {
			return new Location(instance.getHellblockHandler().getHellblockWorld(), (double) this.x1, (double) this.y1,
					(double) this.z1);
		}

		public @NonNull Location getMaxLocation() {
			return new Location(instance.getHellblockHandler().getHellblockWorld(), (double) this.x2, (double) this.y2,
					(double) this.z2);
		}

		public @NonNull Location getRandomLocation() {
			int rndX = RandomUtils.generateRandomInt(Math.abs(this.x2 - this.x1) + 1) + this.x1;
			int rndY = RandomUtils.generateRandomInt(Math.abs(this.y2 - this.y1) + 1) + this.y1;
			int rndZ = RandomUtils.generateRandomInt(Math.abs(this.z2 - this.z1) + 1) + this.z1;
			return new Location(instance.getHellblockHandler().getHellblockWorld(), (double) rndX, (double) rndY,
					(double) rndZ);
		}

		public int getAllCoordinates() {
			return this.getY() * this.getX() * this.getZ();
		}

		public int getX() {
			return this.x2 - this.x1 + 1;
		}

		public int getY() {
			return this.y2 - this.y1 + 1;
		}

		public int getZ() {
			return this.z2 - this.z1 + 1;
		}

		public boolean checkLocation(@NonNull Location location) {
			return location.getWorld() != null
					&& location.getWorld().getName()
							.equals(instance.getHellblockHandler().getHellblockWorld().getName())
					&& location.getBlockX() >= this.x1 && location.getBlockX() <= this.x2
					&& location.getBlockY() >= this.y1 && location.getBlockY() <= this.y2
					&& location.getBlockZ() >= this.z1 && location.getBlockZ() <= this.z2;
		}

		public boolean matches(@NonNull Player player) {
			return this.checkLocation(player.getLocation());
		}
	}
}
