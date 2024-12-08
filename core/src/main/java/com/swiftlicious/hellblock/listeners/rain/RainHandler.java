package com.swiftlicious.hellblock.listeners.rain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class RainHandler implements Reloadable {

	private LavaRainTask lavaRainTask;

	protected final HellblockPlugin instance;

	public RainHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		startLavaRainProcess();
	}

	@Override
	public void unload() {
		stopLavaRainProcess();
	}

	@Override
	public void disable() {
		stopLavaRainProcess();
	}

	private void startLavaRainProcess() {
		if (!instance.getConfigManager().lavaRainEnabled())
			return;
		instance.getScheduler().asyncLater(
				() -> this.lavaRainTask = new LavaRainTask(instance, true, false,
						RandomUtils.generateRandomInt(150, 250)),
				RandomUtils.generateRandomInt(10, 15), TimeUnit.MINUTES);
	}

	private void stopLavaRainProcess() {
		if (this.lavaRainTask != null) {
			this.lavaRainTask.cancelAnimation();
			this.lavaRainTask = null;
		}
	}

	public @Nullable LavaRainTask getLavaRainTask() {
		return this.lavaRainTask;
	}

	public boolean canHurtLivingCreatures() {
		return instance.getConfigManager().canHurtCreatures();
	}

	public boolean willTNTExplode() {
		return instance.getConfigManager().canExplodeTNT();
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
			if (block.getType() == Material.AIR || Tag.ALL_SIGNS.isTagged(block.getType())
					|| Tag.BANNERS.isTagged(block.getType()) || Tag.FENCES.isTagged(block.getType())
					|| Tag.FENCE_GATES.isTagged(block.getType()) || Tag.DOORS.isTagged(block.getType())
					|| Tag.BUTTONS.isTagged(block.getType()) || Tag.PRESSURE_PLATES.isTagged(block.getType())
					|| Tag.FIRE.isTagged(block.getType()) || block.getType() == Material.LAVA
					|| block.getType() == Material.WATER || block.getType() == Material.COBWEB
					|| block.getType() == Material.STRING || block.getType() == Material.FLOWER_POT
					|| block.getType() == Material.BAMBOO || block.getType() == Material.NETHER_PORTAL
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
					|| block.getType() == Material.TRIPWIRE_HOOK || block.getType().toString().contains("GLASS_PANE"))
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
		private World world;

		public LavaRainLocation(@NotNull Location min, @NotNull Location max) {
			this.x1 = Math.min(min.getBlockX(), max.getBlockX());
			this.x2 = Math.max(min.getBlockX(), max.getBlockX());
			this.y1 = Math.min(min.getBlockY(), max.getBlockY());
			this.y2 = Math.max(min.getBlockY(), max.getBlockY());
			this.z1 = Math.min(min.getBlockZ(), max.getBlockZ());
			this.z2 = Math.max(min.getBlockZ(), max.getBlockZ());
			boolean checkWorld = Objects.equals(Objects.requireNonNull(min.getWorld()),
					Objects.requireNonNull(max.getWorld()));
			if (checkWorld) {
				this.world = min.getWorld();
			}
		}

		public @NotNull Iterator<Block> getBlocks() {
			List<Block> list = new ArrayList<>(this.getAllCoordinates());

			for (int x = this.x1; x <= this.x2; ++x) {
				for (int y = this.y1; y <= this.y2; ++y) {
					for (int z = this.z1; z <= this.z2; ++z) {
						Block block = this.world.getBlockAt(x, y, z);
						list.add(block);
					}
				}
			}

			return list.iterator();
		}

		public @NotNull Location getMainLocation() {
			return new Location(this.world, (double) ((this.x2 - this.x1) / 2 + this.x1),
					(double) ((this.y2 - this.y1) / 2 + this.y1), (double) ((this.z2 - this.z1) / 2 + this.z1));
		}

		public double getDistance() {
			return this.getMinLocation().distance(this.getMaxLocation());
		}

		public double getDistanceSquared() {
			return this.getMinLocation().distanceSquared(this.getMaxLocation());
		}

		public @NotNull Location getMinLocation() {
			return new Location(this.world, (double) this.x1, (double) this.y1, (double) this.z1);
		}

		public @NotNull Location getMaxLocation() {
			return new Location(this.world, (double) this.x2, (double) this.y2, (double) this.z2);
		}

		public @NotNull Location getRandomLocation() {
			int rndX = RandomUtils.generateRandomInt(Math.abs(this.x2 - this.x1) + 1) + this.x1;
			int rndY = RandomUtils.generateRandomInt(Math.abs(this.y2 - this.y1) + 1) + this.y1;
			int rndZ = RandomUtils.generateRandomInt(Math.abs(this.z2 - this.z1) + 1) + this.z1;
			return new Location(this.world, (double) rndX, (double) rndY, (double) rndZ);
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

		public boolean checkLocation(@NotNull Location location) {
			return location.getWorld() != null && location.getWorld().getName().equals(this.world.getName())
					&& location.getBlockX() >= this.x1 && location.getBlockX() <= this.x2
					&& location.getBlockY() >= this.y1 && location.getBlockY() <= this.y2
					&& location.getBlockZ() >= this.z1 && location.getBlockZ() <= this.z2;
		}

		public boolean matches(@NotNull Player player) {
			return this.checkLocation(player.getLocation());
		}
	}
}