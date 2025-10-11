package com.swiftlicious.hellblock.listeners.rain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
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
import com.swiftlicious.hellblock.world.HellblockWorld;

public class RainHandler implements Reloadable {

	private final List<LavaRainTask> lavaRainingWorlds = new ArrayList<>();

	protected final HellblockPlugin instance;

	public RainHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getWorlds()
				.forEach(world -> instance.getWorldManager().getWorld(world).ifPresent(this::startLavaRainProcess));
	}

	@Override
	public void unload() {
		stopAllLavaRain();
	}

	@Override
	public void disable() {
		unload();
	}

	public void startLavaRainProcess(HellblockWorld<?> world) {
		if (!instance.getConfigManager().lavaRainEnabled()) {
			return;
		}

		// Schedule the *first* LavaRainTask with a random cooldown (10–15 min)
		instance.getScheduler().asyncLater(
				() -> getLavaRainingWorlds()
						.add(new LavaRainTask(instance, world, true, false, RandomUtils.generateRandomInt(150, 250))),
				RandomUtils.generateRandomInt(10, 15), TimeUnit.MINUTES);
	}

	public void stopLavaRainProcess(String worldName) {
		getLavaRainingWorlds().stream().filter(task -> task.getWorld().worldName().equalsIgnoreCase(worldName))
				.findAny().ifPresent(task -> {
					task.cancelAnimation();
					getLavaRainingWorlds().remove(task);
				});
	}

	public void stopAllLavaRain() {
		getLavaRainingWorlds().forEach(LavaRainTask::cancelAnimation);
		getLavaRainingWorlds().clear();
	}

	public @NotNull List<LavaRainTask> getLavaRainingWorlds() {
		return this.lavaRainingWorlds;
	}

	public boolean canHurtLivingCreatures() {
		return instance.getConfigManager().canHurtCreatures();
	}

	public boolean willSendWarning() {
		return instance.getConfigManager().willWarnPlayers();
	}

	public boolean willTNTExplode() {
		return instance.getConfigManager().canExplodeTNT();
	}

	public @Nullable Block getHighestBlock(@Nullable Location location) {
		if (location == null || location.getWorld() == null) {
			return null;
		}

		Block highestBlock = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(),
				location.getBlockZ());
		for (int y = 0; y < 10; y++) {
			// +2 for eye level
			final Block block = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() + 2 + y,
					location.getBlockZ());
			if (isTransparent(block)) {
				continue;
			}
			highestBlock = block;
			break;
		}
		return highestBlock;
	}

	private boolean isTransparent(Block block) {
		final Material type = block.getType();
		return type == Material.AIR || Tag.ALL_SIGNS.isTagged(type) || Tag.BANNERS.isTagged(type)
				|| Tag.FENCES.isTagged(type) || Tag.FENCE_GATES.isTagged(type) || Tag.DOORS.isTagged(type)
				|| Tag.BUTTONS.isTagged(type) || Tag.PRESSURE_PLATES.isTagged(type) || Tag.FIRE.isTagged(type)
				|| type == Material.LAVA || type == Material.WATER || type == Material.COBWEB || type == Material.STRING
				|| type == Material.FLOWER_POT || type == Material.BAMBOO || type == Material.NETHER_PORTAL
				|| type == Material.END_PORTAL || type == Material.END_GATEWAY || type == Material.LADDER
				|| type == Material.CHAIN || type == Material.CANDLE || type == Material.SEA_PICKLE
				|| type == Material.VINE || type == Material.TWISTING_VINES || type == Material.WEEPING_VINES
				|| type == Material.END_ROD || type == Material.LIGHTNING_ROD || type == Material.LEVER
				|| type == Material.SWEET_BERRY_BUSH || type == Material.SCAFFOLDING || type == Material.LANTERN
				|| type == Material.SOUL_LANTERN || type == Material.TURTLE_EGG || type == Material.SMALL_DRIPLEAF
				|| type == Material.BIG_DRIPLEAF || type == Material.IRON_BARS || type == Material.POWDER_SNOW
				|| type == Material.TRIPWIRE || type == Material.TRIPWIRE_HOOK
				|| type.toString().contains("GLASS_PANE");
	}

	public class LavaRainLocation {
		private World world;
		private final int x1;
		private final int x2;
		private final int y1;
		private final int y2;
		private final int z1;
		private final int z2;

		public LavaRainLocation(@NotNull Location min, @NotNull Location max) {
			this.x1 = Math.min(min.getBlockX(), max.getBlockX());
			this.x2 = Math.max(min.getBlockX(), max.getBlockX());
			this.y1 = Math.min(min.getBlockY(), max.getBlockY());
			this.y2 = Math.max(min.getBlockY(), max.getBlockY());
			this.z1 = Math.min(min.getBlockZ(), max.getBlockZ());
			this.z2 = Math.max(min.getBlockZ(), max.getBlockZ());
			final boolean checkWorld = Objects.equals(Objects.requireNonNull(min.getWorld()),
					Objects.requireNonNull(max.getWorld()));
			if (checkWorld) {
				this.world = min.getWorld();
			}
		}

		public @NotNull Iterator<Block> getBlocks() {
			final List<Block> list = new ArrayList<>(this.getAllCoordinates());

			for (int x = this.x1; x <= this.x2; ++x) {
				for (int y = this.y1; y <= this.y2; ++y) {
					for (int z = this.z1; z <= this.z2; ++z) {
						final Block block = this.world.getBlockAt(x, y, z);
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
			final int rndX = RandomUtils.generateRandomInt(Math.abs(this.x2 - this.x1) + 1) + this.x1;
			final int rndY = RandomUtils.generateRandomInt(Math.abs(this.y2 - this.y1) + 1) + this.y1;
			final int rndZ = RandomUtils.generateRandomInt(Math.abs(this.z2 - this.z1) + 1) + this.z1;
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