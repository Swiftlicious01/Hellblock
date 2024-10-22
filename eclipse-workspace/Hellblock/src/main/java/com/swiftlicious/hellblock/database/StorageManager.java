package com.swiftlicious.hellblock.database;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.playerdata.OfflineUser;
import com.swiftlicious.hellblock.playerdata.OnlineUser;
import com.swiftlicious.hellblock.playerdata.PlayerData;
import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.LogUtils;

/**
 * This class implements the StorageManager interface and is responsible for
 * managing player data storage. It includes methods to handle player data
 * retrieval, storage, and serialization.
 */
public class StorageManager implements StorageManagerInterface, Listener {

	private final HellblockPlugin instance;
	private DataStorageInterface dataSource;
	private StorageType previousType;
	private final ConcurrentHashMap<UUID, OnlineUser> onlineUserMap;
	private final HashSet<UUID> locked;
	private boolean hasRedis;
	private RedisManager redisManager;
	private String uniqueID;
	private CancellableTask timerSaveTask;
	private final Gson gson;

	public StorageManager(HellblockPlugin plugin) {
		instance = plugin;
		this.locked = new HashSet<>();
		this.onlineUserMap = new ConcurrentHashMap<>();
		this.gson = new GsonBuilder().create();
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Reloads the storage manager configuration.
	 */
	public void reload() {
		YamlConfiguration config = instance.getConfig("database.yml");
		this.uniqueID = config.getString("unique-server-id", "default");

		// Check if storage type has changed and reinitialize if necessary
		StorageType storageType = StorageType.valueOf(config.getString("data-storage-method", "H2"));
		if (storageType != previousType) {
			if (this.dataSource != null)
				this.dataSource.disable();
			this.previousType = storageType;
			switch (storageType) {
			case H2 -> this.dataSource = new H2Handler(instance);
			case JSON -> this.dataSource = new JsonHandler(instance);
			case YAML -> this.dataSource = new YamlHandler(instance);
			case SQLite -> this.dataSource = new SQLiteHandler(instance);
			case MySQL -> this.dataSource = new MySQLHandler(instance);
			case MariaDB -> this.dataSource = new MariaDBHandler(instance);
			case MongoDB -> this.dataSource = new MongoDBHandler(instance);
			default -> {
				this.dataSource = new H2Handler(instance);
				throw new IllegalArgumentException("Unexpected value: " + storageType);
			}
			}
			if (this.dataSource != null)
				this.dataSource.initialize();
			else
				LogUtils.severe("No storage type is set.");

		}

		// Handle Redis configuration
		if (!this.hasRedis && config.getBoolean("Redis.enable", false)) {
			this.hasRedis = true;
			this.redisManager = new RedisManager(HellblockPlugin.getInstance());
			this.redisManager.initialize();
		}

		// Disable Redis if it was enabled but is now disabled
		if (this.hasRedis && !config.getBoolean("Redis.enable", false) && this.redisManager != null) {
			this.redisManager.disable();
			this.redisManager = null;
		}

		// Cancel any existing timerSaveTask
		if (this.timerSaveTask != null && !this.timerSaveTask.isCancelled()) {
			this.timerSaveTask.cancel();
		}

		// Schedule periodic data saving if dataSaveInterval is configured
		if (HBConfig.dataSaveInterval != -1 && HBConfig.dataSaveInterval != 0) {
			this.timerSaveTask = instance.getScheduler().runTaskAsyncTimer(() -> {
				long time1 = System.currentTimeMillis();
				this.dataSource.updateManyPlayersData(this.onlineUserMap.values(), !HBConfig.lockData);
				if (HBConfig.logDataSaving)
					LogUtils.info(
							"Data Saved for online players. Took " + (System.currentTimeMillis() - time1) + "ms.");
			}, HBConfig.dataSaveInterval, HBConfig.dataSaveInterval, TimeUnit.SECONDS);
		}
	}

	/**
	 * Disables the storage manager and cleans up resources.
	 */
	public void disable() {
		HandlerList.unregisterAll(this);
		if (this.dataSource != null)
			this.dataSource.updateManyPlayersData(onlineUserMap.values(), true);
		this.onlineUserMap.clear();
		if (this.dataSource != null)
			this.dataSource.disable();
		if (this.redisManager != null)
			this.redisManager.disable();
	}

	/**
	 * Gets the unique server identifier.
	 *
	 * @return The unique server identifier.
	 */
	@NotNull
	@Override
	public String getUniqueID() {
		return uniqueID;
	}

	/**
	 * Gets an OnlineUser instance for the specified UUID.
	 *
	 * @param uuid The UUID of the player.
	 * @return An OnlineUser instance if the player is online, or null if not.
	 */
	@Override
	public OnlineUser getOnlineUser(UUID uuid) {
		return onlineUserMap.get(uuid);
	}

	@Override
	public Collection<OnlineUser> getOnlineUsers() {
		return onlineUserMap.values();
	}

	/**
	 * Asynchronously retrieves an OfflineUser instance for the specified UUID.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock Whether to lock the data during retrieval.
	 * @return A CompletableFuture that resolves to an Optional containing the
	 *         OfflineUser instance if found, or empty if not found or locked.
	 */
	@Override
	public CompletableFuture<Optional<OfflineUser>> getOfflineUser(UUID uuid, boolean lock) {
		var optionalDataFuture = dataSource.getPlayerData(uuid, lock);
		return optionalDataFuture.thenCompose(optionalUser -> {
			if (optionalUser.isEmpty()) {
				// locked
				return CompletableFuture.completedFuture(Optional.empty());
			}
			PlayerData data = optionalUser.get();
			if (data.isLocked()) {
				return CompletableFuture.completedFuture(Optional.of(OfflineUser.LOCKED_USER));
			} else {
				OfflineUser offlineUser = new OfflineUser(uuid, data.getName(), data);
				return CompletableFuture.completedFuture(Optional.of(offlineUser));
			}
		});
	}

	@Override
	public boolean isLockedData(OfflineUser offlineUser) {
		return OfflineUser.LOCKED_USER == offlineUser;
	}

	/**
	 * Asynchronously saves user data for an OfflineUser.
	 *
	 * @param offlineUser The OfflineUser whose data needs to be saved.
	 * @param unlock      Whether to unlock the data after saving.
	 * @return A CompletableFuture that resolves to a boolean indicating the success
	 *         of the data saving operation.
	 */
	@Override
	public CompletableFuture<Boolean> saveUserData(OfflineUser offlineUser, boolean unlock) {
		return dataSource.updatePlayerData(offlineUser.getUUID(), offlineUser.getPlayerData(), unlock);
	}

	/**
	 * Gets the data source used for data storage.
	 *
	 * @return The data source.
	 */
	@Override
	public DataStorageInterface getDataSource() {
		return dataSource;
	}

	/**
	 * Event handler for when a player joins the server. Locks the player's data and
	 * initiates data retrieval if Redis is not used, otherwise, it starts a Redis
	 * data retrieval task.
	 */
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		locked.add(uuid);
		if (!hasRedis) {
			waitForDataLockRelease(uuid, 1);
		} else {
			instance.getScheduler()
					.runTaskAsyncLater(() -> redisManager.getChangeServer(uuid).thenAccept(changeServer -> {
						if (!changeServer) {
							waitForDataLockRelease(uuid, 3);
						} else {
							new RedisGetDataTask(uuid);
						}
					}), 500, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Event handler for when a player quits the server. If the player is not
	 * locked, it removes their OnlineUser instance, updates the player's data in
	 * Redis and the data source.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if (locked.contains(uuid))
			return;

		OnlineUser onlineUser = onlineUserMap.remove(uuid);
		if (onlineUser == null)
			return;
		PlayerData data = onlineUser.getPlayerData();

		if (hasRedis) {
			redisManager.setChangeServer(uuid).thenRun(() -> redisManager.updatePlayerData(uuid, data, true)
					.thenRun(() -> dataSource.updatePlayerData(uuid, data, true).thenAccept(result -> {
						if (result)
							locked.remove(uuid);
					})));
		} else {
			dataSource.updatePlayerData(uuid, data, true).thenAccept(result -> {
				if (result)
					locked.remove(uuid);
			});
		}
	}

	/**
	 * Runnable task for asynchronously retrieving data from Redis. Retries up to 6
	 * times and cancels the task if the player is offline.
	 */
	public class RedisGetDataTask implements Runnable {

		private final UUID uuid;
		private int triedTimes;
		private final CancellableTask task;

		public RedisGetDataTask(UUID uuid) {
			this.uuid = uuid;
			this.task = instance.getScheduler().runTaskAsyncTimer(this, 0, 333, TimeUnit.MILLISECONDS);
		}

		@Override
		public void run() {
			triedTimes++;
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline()) {
				// offline
				task.cancel();
				return;
			}
			if (triedTimes >= 6) {
				waitForDataLockRelease(uuid, 3);
				return;
			}
			redisManager.getPlayerData(uuid, false).thenAccept(optionalData -> {
				if (optionalData.isPresent()) {
					putDataInCache(player, optionalData.get());
					task.cancel();
					dataSource.lockOrUnlockPlayerData(uuid, HBConfig.lockData);
				}
			});
		}
	}

	/**
	 * Waits for data lock release with a delay and a maximum of three retries.
	 *
	 * @param uuid  The UUID of the player.
	 * @param times The number of times this method has been retried.
	 */
	public void waitForDataLockRelease(UUID uuid, int times) {
		instance.getScheduler().runTaskAsyncLater(() -> {
			var player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			if (times > 3) {
				LogUtils.warn("Tried 3 times when getting data for " + uuid + ". Giving up.");
				return;
			}
			this.dataSource.getPlayerData(uuid, HBConfig.lockData).thenAccept(optionalData -> {
				// Data should not be empty
				if (optionalData.isEmpty()) {
					LogUtils.severe("Unexpected error: Data is null");
					return;
				}

				if (optionalData.get().isLocked()) {
					waitForDataLockRelease(uuid, times + 1);
				} else {
					try {
						putDataInCache(player, optionalData.get());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}, 1, TimeUnit.SECONDS);
	}

	/**
	 * Puts player data in cache and removes the player from the locked set.
	 *
	 * @param player     The player whose data is being cached.
	 * @param playerData The data to be cached.
	 */
	public void putDataInCache(Player player, PlayerData playerData) {
		locked.remove(player.getUniqueId());
		OnlineUser bukkitUser = new OnlineUser(player, playerData);
		onlineUserMap.put(player.getUniqueId(), bukkitUser);
	}

	/**
	 * Checks if Redis is enabled.
	 *
	 * @return True if Redis is enabled; otherwise, false.
	 */
	@Override
	public boolean isRedisEnabled() {
		return hasRedis;
	}

	/**
	 * Gets the RedisManager instance.
	 *
	 * @return The RedisManager instance.
	 */
	@Nullable
	public RedisManager getRedisManager() {
		return redisManager;
	}

	/**
	 * Converts PlayerData to bytes.
	 *
	 * @param data The PlayerData to be converted.
	 * @return The byte array representation of PlayerData.
	 */
	@NotNull
	@Override
	public byte[] toBytes(@NotNull PlayerData data) {
		return toJson(data).getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Converts PlayerData to JSON format.
	 *
	 * @param data The PlayerData to be converted.
	 * @return The JSON string representation of PlayerData.
	 */
	@Override
	@NotNull
	public String toJson(@NotNull PlayerData data) {
		return gson.toJson(data);
	}

	/**
	 * Converts JSON string to PlayerData.
	 *
	 * @param json The JSON string to be converted.
	 * @return The PlayerData object.
	 */
	@NotNull
	@Override
	public PlayerData fromJson(String json) {
		try {
			return gson.fromJson(json, PlayerData.class);
		} catch (JsonSyntaxException e) {
			LogUtils.severe("Failed to parse PlayerData from json");
			LogUtils.info("Json: " + json);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts bytes to PlayerData.
	 *
	 * @param data The byte array to be converted.
	 * @return The PlayerData object.
	 */
	@Override
	@NotNull
	public PlayerData fromBytes(byte[] data) {
		return fromJson(new String(data, StandardCharsets.UTF_8));
	}
}