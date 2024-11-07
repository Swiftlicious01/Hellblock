package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.RandomUtils;

import lombok.NonNull;

public class NetherAnimalSpawningTask implements Runnable {

	private final HellblockPlugin instance;

	private final UUID playerUUID;
	private final CancellableTask cancellableTask;

	private final Set<Location> spawnCache;

	private final static int MAX_ANIMAL_COUNT = 15;

	public NetherAnimalSpawningTask(HellblockPlugin plugin, @NonNull UUID playerUUID) {
		instance = plugin;
		this.playerUUID = playerUUID;
		this.spawnCache = new HashSet<>();
		this.cancellableTask = instance.getScheduler().runTaskSyncTimer(this, null, RandomUtils.generateRandomInt(1, 3),
				RandomUtils.generateRandomInt(1, 15) * 60 * 20L);
	}

	@Override
	public void run() {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null || !player.isOnline() || player.getLocation() == null)
			return;
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (instance.getHellblockHandler().isWorldguardProtected()) {
			if (instance.getHellblockHandler().checkIfInSpawn(player.getLocation()))
				return;
			if (instance.getLavaRainHandler().getLavaRainTask() != null
					&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining())
				return;
			UUID ownerUUID = instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player);
			if (ownerUUID == null)
				return;
			HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(ownerUUID);
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(ownerUUID, ti.getID());
			if (region == null)
				return;
			if (ti.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) != AccessType.ALLOW)
				return;
			World world = instance.getHellblockHandler().getHellblockWorld();
			instance.getBiomeHandler().getHellblockChunks(region).thenAccept((chunks) -> {
				for (Chunk chunk : chunks) {
					if (!chunk.getChunkSnapshot().contains(Bukkit.createBlockData(Material.GRASS_BLOCK)))
						continue;
					int animalSize = (int) Arrays.asList(chunk.getEntities()).stream()
							.filter((e) -> e instanceof Animals).count();
					if (animalSize > MAX_ANIMAL_COUNT)
						continue;

					Block block;
					do {
						block = chunk.getBlock(RandomUtils.generateRandomInt(16),
								RandomUtils.generateRandomInt(world.getMaxHeight() - 1),
								RandomUtils.generateRandomInt(16));
					} while (block.getType() != Material.GRASS_BLOCK && !spawnCache.contains(block.getLocation()));

					Location spawn = block.getLocation().add(0.5, 1, 0.5);
					if ((spawn.getBlock().isEmpty() || spawn.getBlock().isPassable())
							&& (spawn.getBlock().getRelative(BlockFace.UP).isEmpty()
									|| spawn.getBlock().getRelative(BlockFace.UP).isPassable())
							&& block.getLightLevel() > (byte) 9) {
						instance.getScheduler().runTaskSync(
								() -> world.spawnEntity(spawn, RandomUtils.spawnRandomAnimal(), true), spawn);
						spawnCache.add(block.getLocation());
					}
				}

				if (spawnCache.size() > RandomUtils.generateRandomInt(2, 5))
					spawnCache.clear();
			});
		} else {
			// TODO: using plugin protection
		}
	}

	public void stopAnimalSpawning() {
		if (!this.cancellableTask.isCancelled())
			this.cancellableTask.cancel();
	}
}
