package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.listeners.rain.LavaRainTask;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

public class FortressHandler implements Runnable {

	protected final HellblockPlugin instance;

	private final UUID playerUUID;
	private final SchedulerTask cancellableTask;

	private final Set<Location> spawnCache;

	private final static int MAX_FORTRESS_COUNT = 25;

	public FortressHandler(HellblockPlugin plugin, @NotNull UUID playerUUID) {
		instance = plugin;
		this.playerUUID = playerUUID;
		this.spawnCache = new HashSet<>();
		this.cancellableTask = instance.getScheduler().asyncRepeating(this, RandomUtils.generateRandomInt(1, 3),
				RandomUtils.generateRandomInt(1, 15), TimeUnit.MINUTES);
	}

	@Override
	public void run() {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null || !player.isOnline())
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		if (player.getLocation() == null)
			return;

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null)
				return;
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(instance
								.getWorldManager().getHellblockWorldFormat(offlineUser.getHellblockData().getID()));
						if (world.isEmpty() || world.get() == null)
							throw new NullPointerException(
									"World returned null, please try to regenerate the world before reporting this issue.");
						World bukkitWorld = world.get().bukkitWorld();
						Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
								.filter(task -> bukkitWorld.getName().equalsIgnoreCase(task.getWorld().worldName()))
								.findAny();
						if (lavaRain.isPresent() && lavaRain.get().isLavaRaining())
							return;
						if (offlineUser.getHellblockData().isAbandoned())
							return;
						if (offlineUser.getHellblockData()
								.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) != AccessType.ALLOW)
							return;
						if (offlineUser.getHellblockData().getBiome() != HellBiome.NETHER_FORTRESS)
							return;
						instance.getBiomeHandler()
								.getHellblockChunks(bukkitWorld, offlineUser.getHellblockData().getBoundingBox())
								.thenAccept((chunks) -> {
									for (Chunk chunk : chunks) {
										int fortressSize = (int) Arrays.asList(chunk.getEntities()).stream()
												.filter((e) -> e.getType() == EntityType.BLAZE
														|| e.getType() == EntityType.WITHER_SKELETON)
												.count();
										if (fortressSize > MAX_FORTRESS_COUNT)
											continue;

										Block block;
										do {
											block = chunk.getBlock(RandomUtils.generateRandomInt(16),
													RandomUtils.generateRandomInt(bukkitWorld.getMaxHeight() - 1),
													RandomUtils.generateRandomInt(16));
										} while (block.getType().isSolid()
												&& !spawnCache.contains(block.getLocation()));

										Location spawn = block.getLocation().add(0.5, 1, 0.5);
										if ((spawn.getBlock().isEmpty() || spawn.getBlock().isPassable())
												&& (spawn.getBlock().getRelative(BlockFace.UP).isEmpty()
														|| spawn.getBlock().getRelative(BlockFace.UP).isPassable())
												&& block.getLightLevel() > (byte) 7) {
											instance.getScheduler().executeSync(() -> bukkitWorld.spawnEntity(spawn,
													RandomUtils.spawnFortressMob(), true), spawn);
											spawnCache.add(block.getLocation());
										}
									}

									instance.getScheduler().asyncRepeating(() -> spawnCache.clear(), 15, 30,
											TimeUnit.MINUTES);
								});
					});
		});
	}

	public void stopFortressSpawning() {
		if (!this.cancellableTask.isCancelled())
			this.cancellableTask.cancel();
	}
}