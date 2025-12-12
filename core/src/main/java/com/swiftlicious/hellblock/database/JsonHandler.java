package com.swiftlicious.hellblock.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.io.Files;
import com.google.gson.JsonSyntaxException;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.PlayerData;

/**
 * A data storage implementation that uses JSON files to store player data.
 */
public class JsonHandler extends AbstractStorage {

	private final File dataFolder;

	private final ConcurrentHashMap<UUID, CompletableFuture<Optional<PlayerData>>> loadingCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> updatingCache = new ConcurrentHashMap<>();

	private final Cache<Integer, UUID> islandIdToUUIDCache = Caffeine.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build();

	private final Cache<UUID, PlayerData> memoryCache = Caffeine.newBuilder().maximumSize(500).build();

	public JsonHandler(HellblockPlugin plugin) {
		super(plugin);
		this.dataFolder = new File(plugin.getDataFolder(), "data");
		if (!dataFolder.exists() && !dataFolder.mkdirs()) {
			plugin.getPluginLogger().warn("Failed to create data folder for JSON storage.");
		}
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.JSON;
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor) {
		final Executor finalExecutor = executor != null ? executor : plugin.getScheduler().async();

		return loadingCache.computeIfAbsent(uuid, id -> {
			plugin.debug("getPlayerData: starting new load for " + id);
			final CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();

			// Ensure the entry is only removed after the future completes (success or
			// failure)
			future.whenComplete((result, throwable) -> loadingCache.remove(uuid));

			finalExecutor.execute(() -> {
				try {
					// Check memory cache first
					PlayerData cached = memoryCache.getIfPresent(uuid);
					if (cached != null) {
						plugin.debug("JSON cache hit for " + uuid);
						future.complete(Optional.of(cached));
						return;
					}

					plugin.debug("JSON cache miss for " + uuid);

					File file = getPlayerDataFile(uuid);
					PlayerData data = null;

					if (file.exists()) {
						try (GZIPInputStream gzipIn = new GZIPInputStream(new FileInputStream(file));
								InputStreamReader reader = new InputStreamReader(gzipIn, StandardCharsets.UTF_8)) {
							data = plugin.getStorageManager().getGson().fromJson(reader, PlayerData.class);
						} catch (IOException | JsonSyntaxException ex) {
							plugin.getPluginLogger().warn("Failed to parse JSON data for " + uuid, ex);
						}
					} else if (Bukkit.getPlayer(uuid) != null) {
						data = PlayerData.empty();
					}

					if (data != null) {
						data.setUUID(uuid);
						memoryCache.put(uuid, data);

						// Index by island ID
						int islandId = data.getHellblockData().getIslandId();
						if (islandId > 0)
							islandIdToUUIDCache.put(islandId, uuid);
					}

					future.complete(Optional.ofNullable(data));
				} catch (Exception ex) {
					plugin.getPluginLogger().warn("Failed to load JSON for " + uuid, ex);
					future.completeExceptionally(ex);
				}
			});

			return future;
		});
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerDataByIslandId(int islandId, boolean lock,
			Executor executor) {
		UUID uuid = islandIdToUUIDCache.getIfPresent(islandId);

		if (uuid != null) {
			return getPlayerData(uuid, lock, executor);
		}

		// UUID not yet known: scan files and populate index
		return scanJsonFilesForIslandId(islandId, lock, executor);
	}

	private CompletableFuture<Optional<PlayerData>> scanJsonFilesForIslandId(int islandId, boolean lock,
			Executor executor) {
		final Executor finalExecutor = executor != null ? executor : plugin.getScheduler().async();
		final CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();

		finalExecutor.execute(() -> {
			try {
				File dataDir = getPlayerDataFolder();
				File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json.gz"));

				if (files == null) {
					future.complete(Optional.empty());
					return;
				}

				for (File file : files) {
					UUID fileUUID;
					try {
						fileUUID = UUID.fromString(file.getName().replace(".json.gz", ""));
					} catch (IllegalArgumentException e) {
						continue;
					}

					// Fast path: already in memory
					PlayerData cached = memoryCache.getIfPresent(fileUUID);
					if (cached != null && cached.getHellblockData().getIslandId() == islandId) {
						islandIdToUUIDCache.put(islandId, fileUUID);
						future.complete(Optional.of(cached));
						return;
					}

					// Load from file
					try (GZIPInputStream gzipIn = new GZIPInputStream(new FileInputStream(file));
							InputStreamReader reader = new InputStreamReader(gzipIn, StandardCharsets.UTF_8)) {

						PlayerData data = plugin.getStorageManager().getGson().fromJson(reader, PlayerData.class);

						if (data != null && data.getHellblockData().getIslandId() == islandId) {
							data.setUUID(fileUUID);
							memoryCache.put(fileUUID, data);
							islandIdToUUIDCache.put(islandId, fileUUID);
							future.complete(Optional.of(data));
							return;
						}
					} catch (Exception e) {
						plugin.getPluginLogger().warn("Failed to read JSON for file " + file.getName(), e);
					}
				}

				future.complete(Optional.empty());
			} catch (Exception ex) {
				plugin.getPluginLogger().warn("Failed to scan JSON files for islandId=" + islandId, ex);
				future.completeExceptionally(ex);
			}
		});

		return future;
	}

	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean ignore) {
		return updatingCache.computeIfAbsent(uuid, id -> {
			final CompletableFuture<Boolean> future = new CompletableFuture<>();
			final Executor executor = plugin.getScheduler().async();

			// Ensure cleanup happens only once after complete
			future.whenComplete((result, throwable) -> updatingCache.remove(uuid));

			executor.execute(() -> {
				try {
					File file = getPlayerDataFile(uuid);

					try (GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(file, false));
							OutputStreamWriter writer = new OutputStreamWriter(gzipOut, StandardCharsets.UTF_8)) {

						plugin.getStorageManager().getGson().toJson(playerData, writer);
						memoryCache.put(uuid, playerData);
						future.complete(true);

					} catch (IOException ex) {
						plugin.getPluginLogger().warn("Failed to save JSON data for " + uuid, ex);
						future.completeExceptionally(ex);
					}
				} catch (Exception outerEx) {
					plugin.getPluginLogger().warn("Unexpected error while saving JSON for " + uuid, outerEx);
					future.completeExceptionally(outerEx);
				}
			});

			return future;
		});
	}

	/**
	 * Get the file associated with a player's UUID for storing JSON data.
	 *
	 * @param uuid The UUID of the player.
	 * @return The file for the player's data.
	 */
	public File getPlayerDataFile(UUID uuid) {
		return new File(dataFolder, uuid + ".json.gz");
	}

	public File getPlayerDataFolder() {
		return this.dataFolder;
	}

	// Retrieve a set of unique user UUIDs based on JSON data files in the 'data'
	// folder.
	@Override
	public Set<UUID> getUniqueUsers() {
		final Set<UUID> uuids = new HashSet<>();
		if (dataFolder.exists()) {
			File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json.gz"));
			if (files != null) {
				for (File file : files) {
					try {
						String uuidStr = Files.getNameWithoutExtension(file.getName());
						uuids.add(UUID.fromString(uuidStr));
					} catch (IllegalArgumentException ex) {
						plugin.getPluginLogger().warn("Invalid UUID filename in JSON data: " + file.getName());
						file.delete(); // optional cleanup
					}
				}
			}
		}
		return uuids;
	}

	@Override
	public void invalidateCache(UUID uuid) {
		memoryCache.invalidate(uuid);
	}

	@Override
	public void clearCache() {
		memoryCache.invalidateAll();
	}

	@Override
	public void invalidateIslandCache(int islandId) {
		islandIdToUUIDCache.invalidate(islandId);
	}

	@Override
	public boolean isPendingInsert(UUID uuid) {
		return false;
	}

	@Override
	public boolean isInsertStillRecent(UUID uuid) {
		return false;
	}

	@Override
	public Long getInsertAge(UUID uuid) {
		return null;
	}

	@Override
	public CompletableFuture<Void> getInsertFuture(UUID uuid) {
		return CompletableFuture.completedFuture(null);
	}
}