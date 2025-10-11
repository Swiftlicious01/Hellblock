package com.swiftlicious.hellblock.listeners;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.listeners.rain.LavaRainTask;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

public final class AnimalHandler implements Runnable {

	private final UUID playerUUID;
	private final SchedulerTask cancellableTask;

	private final Set<Location> spawnCache = ConcurrentHashMap.newKeySet();

	private static final int MIN_LIGHT_LEVEL = 9;

	public AnimalHandler(@NotNull UUID playerUUID) {
		this.playerUUID = playerUUID;
		this.cancellableTask = HellblockPlugin.getInstance().getScheduler().asyncRepeating(this::run,
				RandomUtils.generateRandomInt(1, 3), RandomUtils.generateRandomInt(1, 15), TimeUnit.MINUTES);

		// schedule cache clear once
		HellblockPlugin.getInstance().getScheduler().asyncRepeating(spawnCache::clear, 15, 30, TimeUnit.MINUTES);
	}

	@Override
	public void run() {
		final Player player = Bukkit.getPlayer(playerUUID);
		if (player == null || !player.isOnline()) {
			return;
		}
		if (!HellblockPlugin.getInstance().getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (player.getLocation() == null) {
			return;
		}

		HellblockPlugin.getInstance().getCoopManager().getHellblockOwnerOfVisitingIsland(player)
				.thenAccept(ownerUUID -> {
					if (ownerUUID == null) {
						return;
					}
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(ownerUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept((result) -> {
								if (result.isEmpty()) {
									return;
								}
								final UserData offlineUser = result.get();
								final Optional<HellblockWorld<?>> world = HellblockPlugin.getInstance()
										.getWorldManager().getWorld(HellblockPlugin.getInstance().getWorldManager()
												.getHellblockWorldFormat(offlineUser.getHellblockData().getID()));
								if (world.isEmpty()) {
									throw new NullPointerException(
											"World returned null, please try to regenerate the world before reporting this issue.");
								}
								final World bukkitWorld = world.get().bukkitWorld();
								final Optional<LavaRainTask> lavaRain = HellblockPlugin.getInstance()
										.getLavaRainHandler().getLavaRainingWorlds().stream().filter(task -> bukkitWorld
												.getName().equalsIgnoreCase(task.getWorld().worldName()))
										.findAny();
								if (lavaRain.isPresent() && lavaRain.get().isLavaRaining()) {
									return;
								}
								if (offlineUser.getHellblockData().isAbandoned()) {
									return;
								}
								if (offlineUser.getHellblockData()
										.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) != AccessType.ALLOW) {
									return;
								}

								HellblockPlugin.getInstance().getBiomeHandler()
										.getHellblockChunks(bukkitWorld,
												offlineUser.getHellblockData().getBoundingBox())
										.thenAccept(chunks -> HellblockPlugin.getInstance().getScheduler()
												.executeSync(() -> {
													final int maxAnimalCount = getMaxAnimalCount(
															offlineUser.getHellblockData());
													handleAnimals(bukkitWorld, chunks, offlineUser.getHellblockData(),
															maxAnimalCount);
												}));

							}).exceptionally(ex -> {
								HellblockPlugin.getInstance().getPluginLogger()
										.severe("Error fetching offline user data", ex);
								return null;
							});
				}).exceptionally(ex -> {
					HellblockPlugin.getInstance().getPluginLogger().severe("Error resolving owner for animal spawn",
							ex);
					return null;
				});
	}

	private void handleAnimals(World world, List<Chunk> chunks, HellblockData hellblockData, int maxAnimalCount) {
		double bonus = HellblockPlugin.getInstance().getMobSpawnHandler().getCachedMobSpawnBonus(hellblockData);

		for (Chunk chunk : chunks) {
			final long currentCount = Stream.of(chunk.getEntities()).filter(e -> e instanceof Animals)
					.limit(maxAnimalCount + 1).count();

			if (currentCount > maxAnimalCount) {
				continue;
			}

			int attempts = Math.min(1 + (int) Math.floor(bonus), 5);
			for (int i = 0; i < attempts; i++) {
				final Block block = findAnimalSpawnBlock(chunk);
				if (block == null)
					continue;

				final Location spawn = block.getLocation().add(0.5, 1, 0.5);
				if (isValidSpawnLocation(spawn, block)) {
					final int level = (int) hellblockData.getLevel();

					if (rollSpawnChance(level, bonus)) {
						EntityType toSpawn = RandomUtils.spawnRandomAnimal(world, chunk, bonus, spawnCache);
						if (toSpawn != null) {
							world.spawnEntity(spawn, toSpawn, true);
							spawnCache.add(block.getLocation());
						}
					}
				}
			}
		}
	}

	private Block findAnimalSpawnBlock(Chunk chunk) {
		Set<Material> validSpawnBlocks = Set.of(Material.GRASS_BLOCK, Material.MOSS_BLOCK, Material.MYCELIUM,
				Material.PODZOL, Material.WARPED_NYLIUM, Material.CRIMSON_NYLIUM, Material.SOUL_SAND,
				Material.SOUL_SOIL);

		for (int attempts = 0; attempts < 50; attempts++) {
			final Block block = chunk.getBlock(RandomUtils.generateRandomInt(16),
					RandomUtils.generateRandomInt(chunk.getWorld().getMaxHeight() - 1),
					RandomUtils.generateRandomInt(16));

			if (validSpawnBlocks.contains(block.getType()) && !spawnCache.contains(block.getLocation())) {
				return block;
			}
		}

		return null;
	}

	private boolean isValidSpawnLocation(Location spawn, Block block) {
		return (spawn.getBlock().isEmpty() || spawn.getBlock().isPassable())
				&& (spawn.getBlock().getRelative(BlockFace.UP).isEmpty()
						|| spawn.getBlock().getRelative(BlockFace.UP).isPassable())
				&& block.getLightLevel() > MIN_LIGHT_LEVEL;
	}

	public void stopAnimalSpawning() {
		if (!this.cancellableTask.isCancelled()) {
			this.cancellableTask.cancel();
		}
	}

	private int getMaxAnimalCount(HellblockData hellblockData) {
		final int base = 25;
		// Apply upgrade boost
		double bonus = HellblockPlugin.getInstance().getMobSpawnHandler().getCachedMobSpawnBonus(hellblockData);
		// Treat bonus as a multiplier: base + percent bonus (e.g., +20% for 0.2)
		int boosted = (int) Math.floor(base + (base * bonus));
		return Math.min(boosted, 150); // Apply cap
	}

	/**
	 * Computes a spawn roll based on base spawn chance, island level, and upgrade
	 * bonus.
	 *
	 * @param level island level
	 * @param bonus upgrade bonus (from 0.0 to 1.0 or more)
	 * @return true if spawn should succeed
	 */
	public boolean rollSpawnChance(int level, double bonus) {
		final int baseChance = 10; // 10% base chance
		final double levelBonus = level * 0.05; // 0.05% per level (adjustable)

		double totalChance = baseChance + levelBonus + (bonus * 100.0); // convert bonus from 0.2 → 20%
		return RandomUtils.generateRandomInt(1, 100) <= totalChance;
	}
}