package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.BlockPos;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomChunk;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

public final class AnimalHandler implements Runnable {

	private final int islandId;
	private SchedulerTask spawnTask;

	private final HellblockPlugin plugin;

	private final Set<Pos3> spawnCache = ConcurrentHashMap.newKeySet();

	private static final NamespacedKey OWNER_KEY = new NamespacedKey(HellblockPlugin.getInstance(), "hellblock_owner");

	private static final int MIN_LIGHT_LEVEL = 9;

	public AnimalHandler(@NotNull HellblockPlugin plugin, int islandId) {
		this.plugin = plugin;
		this.islandId = islandId;
		this.spawnTask = plugin.getScheduler().asyncRepeating(this::run, RandomUtils.generateRandomInt(1, 3),
				RandomUtils.generateRandomInt(1, 15), TimeUnit.MINUTES);

		// schedule cache clear once
		plugin.getScheduler().asyncRepeating(spawnCache::clear, 15, 30, TimeUnit.MINUTES);
	}

	@Override
	public void run() {
		Set<UUID> players = plugin.getIslandManager().getPlayersOnIsland(islandId);
		if (players.isEmpty()) {
			return;
		}

		plugin.getStorageManager().getOfflineUserDataByIslandId(islandId, plugin.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}

					final UserData ownerData = result.get();
					final HellblockData hellblockData = ownerData.getHellblockData();

					if (hellblockData.isAbandoned()) {
						return;
					}
					if (hellblockData.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) != AccessType.ALLOW) {
						return;
					}

					final Optional<HellblockWorld<?>> worldOpt = plugin.getWorldManager()
							.getWorld(plugin.getWorldManager().getHellblockWorldFormat(islandId));

					if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
						throw new NullPointerException(
								"World returned null, please try to regenerate the world before reporting this issue.");
					}

					if (plugin.getNetherWeatherManager().isWeatherActive(islandId)) {
						return;
					}

					final HellblockWorld<?> hellWorld = worldOpt.get();
					plugin.getProtectionManager().getHellblockChunks(hellWorld, islandId)
							.thenAccept(chunkPositions -> plugin.getScheduler().executeSync(() -> {
								final int maxAnimalCount = getMaxAnimalCount(hellblockData);
								handleAnimals(hellWorld, chunkPositions, hellblockData, maxAnimalCount);
							}));

				}).exceptionally(ex -> {
					plugin.getPluginLogger().severe("Error fetching offline user data from islandId=" + islandId, ex);
					return null;
				});
	}

	private CompletableFuture<Void> handleAnimals(@NotNull HellblockWorld<?> world,
			@NotNull Set<ChunkPos> chunkPositions, @NotNull HellblockData hellblockData, int maxAnimalCount) {

		double bonus = plugin.getMobSpawnHandler().getCachedMobSpawnBonus(hellblockData);
		int islandId = hellblockData.getIslandId();

		long animalCount = world.bukkitWorld().getEntities().stream().filter(e -> e instanceof Animals).filter(e -> {
			PersistentDataContainer container = e.getPersistentDataContainer();
			Integer storedId = container.get(OWNER_KEY, PersistentDataType.INTEGER);
			return storedId != null && storedId.equals(islandId);
		}).count();

		if (animalCount >= maxAnimalCount) {
			return CompletableFuture.completedFuture(null);
		}

		List<CompletableFuture<Void>> tasks = new ArrayList<>();

		for (ChunkPos chunkPos : chunkPositions) {
			Optional<CustomChunk> optionalChunk = world.getLoadedChunk(chunkPos);
			if (optionalChunk.isEmpty())
				continue;

			CustomChunk chunk = optionalChunk.get();
			int attempts = Math.min(1 + (int) Math.floor(bonus), 5);

			for (int i = 0; i < attempts; i++) {
				CompletableFuture<Void> task = findAnimalSpawnPos(chunk, world).thenCompose(pos -> {
					if (pos == null || spawnCache.contains(pos)) {
						return CompletableFuture.completedFuture(null);
					}

					return isValidSpawnLocation(world, pos).thenCompose(valid -> {
						if (!valid)
							return CompletableFuture.completedFuture(null);

						if (!rollSpawnChance((int) hellblockData.getIslandLevel(), bonus)) {
							return CompletableFuture.completedFuture(null);
						}

						return RandomUtils.spawnRandomAnimal(world, chunkPos, bonus, spawnCache).thenAccept(type -> {
							if (type != null) {
								Location spawn = pos.toLocation(world.bukkitWorld()).clone().add(0.5, 1, 0.5);
								Entity entity = world.bukkitWorld().spawnEntity(spawn, type);
								entity.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.INTEGER,
										islandId);
								spawnCache.add(pos);
							}
						});
					});
				});

				tasks.add(task);
			}
		}

		return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
	}

	@Nullable
	private CompletableFuture<Pos3> findAnimalSpawnPos(@NotNull CustomChunk chunk, @NotNull HellblockWorld<?> world) {
		Set<String> validKeys = Set.of("minecraft:grass_block", "minecraft:moss_block", "minecraft:mycelium",
				"minecraft:podzol", "minecraft:warped_nylium", "minecraft:crimson_nylium", "minecraft:soul_sand",
				"minecraft:soul_soil");

		List<CompletableFuture<Pos3>> attempts = new ArrayList<>();

		for (int i = 0; i < 50; i++) {
			int x = RandomUtils.generateRandomInt(16);
			int y = RandomUtils.generateRandomInt(world.bukkitWorld().getMaxHeight());
			int z = RandomUtils.generateRandomInt(16);

			BlockPos local = BlockPos.fromSection(chunk.chunkPos(), x, y, z);
			Pos3 pos = local.toPos3(chunk.chunkPos());

			attempts.add(world.getBlockState(pos).thenApply(stateOpt -> {
				if (stateOpt.isEmpty())
					return null;
				String key = stateOpt.get().type().type().value().toLowerCase();
				return validKeys.contains(key) && !spawnCache.contains(pos) ? pos : null;
			}));
		}

		return CompletableFuture.allOf(attempts.toArray(CompletableFuture[]::new)).thenApply(
				v -> attempts.stream().map(CompletableFuture::join).filter(Objects::nonNull).findFirst().orElse(null));
	}

	@NotNull
	private CompletableFuture<Boolean> isValidSpawnLocation(@NotNull HellblockWorld<?> world, @NotNull Pos3 pos) {
		Pos3 above = pos.up();
		Pos3 twoAbove = above.up();

		CompletableFuture<Boolean> baseSolidFuture = world.getBlockState(pos)
				.thenApply(state -> state.map(s -> !s.isAir()).orElse(false));

		CompletableFuture<Boolean> aboveClearFuture = world.getBlockState(above)
				.thenApply(state -> state.map(CustomBlockState::isAir).orElse(true));

		CompletableFuture<Boolean> headClearFuture = world.getBlockState(twoAbove)
				.thenApply(state -> state.map(CustomBlockState::isAir).orElse(true));

		return CompletableFuture.allOf(baseSolidFuture, aboveClearFuture, headClearFuture).thenApply(v -> {
			boolean baseSolid = baseSolidFuture.join();
			boolean aboveClear = aboveClearFuture.join();
			boolean headClear = headClearFuture.join();

			int lightLevel = world.bukkitWorld().getBlockAt(pos.x(), pos.y(), pos.z()).getLightLevel();
			return baseSolid && aboveClear && headClear && lightLevel > MIN_LIGHT_LEVEL;
		});
	}

	public void stopAnimalSpawning() {
		if (this.spawnTask != null && !this.spawnTask.isCancelled()) {
			this.spawnTask.cancel();
			this.spawnTask = null;
		}
	}

	private int getMaxAnimalCount(@NotNull HellblockData hellblockData) {
		final int base = 25;
		// Apply upgrade boost
		double bonus = plugin.getMobSpawnHandler().getCachedMobSpawnBonus(hellblockData);
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