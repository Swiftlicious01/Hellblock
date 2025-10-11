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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.listeners.rain.LavaRainTask;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

public final class FortressHandler implements Runnable {

	private final UUID playerUUID;
	private final SchedulerTask cancellableTask;
	private final Set<Location> spawnCache = ConcurrentHashMap.newKeySet();

	private static final int MIN_LIGHT_LEVEL = 7;

	public FortressHandler(@NotNull UUID playerUUID) {
		this.playerUUID = playerUUID;
		this.cancellableTask = HellblockPlugin.getInstance().getScheduler().asyncRepeating(this::run,
				RandomUtils.generateRandomInt(1, 3), RandomUtils.generateRandomInt(1, 15), TimeUnit.MINUTES);

		// schedule cache clear once
		HellblockPlugin.getInstance().getScheduler().asyncRepeating(spawnCache::clear, 15, 30, TimeUnit.MINUTES);
	}

	@Override
	public void run() {
		final Player player = Bukkit.getPlayer(playerUUID);
		if (player == null || !player.isOnline())
			return;
		if (!HellblockPlugin.getInstance().getHellblockHandler().isInCorrectWorld(player))
			return;

		HellblockPlugin.getInstance().getCoopManager().getHellblockOwnerOfVisitingIsland(player)
				.thenAccept(ownerUUID -> {
					if (ownerUUID == null)
						return;

					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(ownerUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty())
									return;

								final UserData offlineUser = result.get();
								final HellblockData hellblockData = offlineUser.getHellblockData();

								if (hellblockData.isAbandoned())
									return;
								if (hellblockData
										.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) != AccessType.ALLOW)
									return;
								if (hellblockData.getBiome() != HellBiome.NETHER_FORTRESS)
									return;

								final Optional<HellblockWorld<?>> worldOpt = HellblockPlugin.getInstance()
										.getWorldManager().getWorld(HellblockPlugin.getInstance().getWorldManager()
												.getHellblockWorldFormat(hellblockData.getID()));

								if (worldOpt.isEmpty())
									throw new NullPointerException("World is missing or corrupted.");

								final World world = worldOpt.get().bukkitWorld();

								// Skip spawning during lava rain
								Optional<LavaRainTask> lavaRain = HellblockPlugin.getInstance().getLavaRainHandler()
										.getLavaRainingWorlds().stream()
										.filter(task -> task.getWorld().worldName().equalsIgnoreCase(world.getName()))
										.findAny();

								if (lavaRain.isPresent() && lavaRain.get().isLavaRaining())
									return;

								HellblockPlugin.getInstance().getBiomeHandler()
										.getHellblockChunks(world, hellblockData.getBoundingBox())
										.thenAccept(chunks -> HellblockPlugin.getInstance().getScheduler()
												.executeSync(() -> {
													final int maxMobCount = getMaxFortressMobCount(hellblockData);
													handleFortressMobs(world, chunks, hellblockData, maxMobCount);
												}));

							}).exceptionally(ex -> {
								HellblockPlugin.getInstance().getPluginLogger()
										.severe("Error fetching offline user data", ex);
								return null;
							});
				}).exceptionally(ex -> {
					HellblockPlugin.getInstance().getPluginLogger().severe("Error resolving owner for mob spawn", ex);
					return null;
				});
	}

	private void handleFortressMobs(World world, List<Chunk> chunks, HellblockData data, int maxMobCount) {
		final double bonus = HellblockPlugin.getInstance().getMobSpawnHandler().getCachedMobSpawnBonus(data);
		final int level = (int) data.getLevel();

		for (Chunk chunk : chunks) {
			final long currentCount = Stream.of(chunk.getEntities())
					.filter(e -> e.getType() == EntityType.BLAZE || e.getType() == EntityType.WITHER_SKELETON)
					.limit(maxMobCount + 1).count();

			if (currentCount > maxMobCount)
				continue;

			int attempts = Math.min(1 + (int) Math.floor(bonus), 5); // cap to 5 attempts
			for (int i = 0; i < attempts; i++) {
				final Block block = findFortressBlock(chunk);
				if (block == null)
					continue;

				final Location spawn = block.getLocation().add(0.5, 1, 0.5);
				if (!isValidSpawnLocation(spawn, block))
					continue;

				if (rollSpawnChance(level, bonus)) {
					EntityType toSpawn = RandomUtils.spawnFortressMob(world, chunk, bonus, spawnCache);
					if (toSpawn != null) {
						world.spawnEntity(spawn, toSpawn, true);
						spawnCache.add(block.getLocation());
					}
				}
			}
		}
	}

	private Block findFortressBlock(Chunk chunk) {
		Set<Material> validSpawnBlocks = Set.of(Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS, Material.SOUL_SAND,
				Material.SOUL_SOIL, Material.BLACKSTONE, Material.POLISHED_BLACKSTONE,
				Material.CHISELED_POLISHED_BLACKSTONE, Material.BASALT, Material.POLISHED_BASALT);

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

	public void stopFortressSpawning() {
		if (!this.cancellableTask.isCancelled()) {
			this.cancellableTask.cancel();
		}
	}

	private int getMaxFortressMobCount(HellblockData data) {
		final int base = 25;
		final double bonus = HellblockPlugin.getInstance().getMobSpawnHandler().getCachedMobSpawnBonus(data);
		final int boosted = (int) Math.floor(base + (base * bonus));
		return Math.min(boosted, 150); // hard cap
	}

	/**
	 * Computes whether a mob should spawn, based on level and bonus.
	 */
	public boolean rollSpawnChance(int level, double bonus) {
		final int baseChance = 10; // 10%
		final double levelBonus = level * 0.05; // 0.05% per level
		final double totalChance = baseChance + levelBonus + (bonus * 100.0); // bonus: 0.25 → +25%
		return RandomUtils.generateRandomInt(1, 100) <= totalChance;
	}
}