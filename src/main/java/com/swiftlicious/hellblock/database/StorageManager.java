package com.swiftlicious.hellblock.database;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.player.OfflineUser;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory;
import com.swiftlicious.hellblock.utils.adapters.ListSerializer;
import com.swiftlicious.hellblock.utils.adapters.MapSerializer;
import com.swiftlicious.hellblock.utils.adapters.SetSerializer;

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
		// Build the gson
		// excludeFieldsWithoutExposeAnnotation - this means that every field to be
		// stored should use @Expose
		// enableComplexMapKeySerialization - forces GSON to use TypeAdapters even for
		// Map keys
		GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.enableComplexMapKeySerialization().setPrettyPrinting();
		// Register map serializers
		builder.registerTypeAdapter((new TypeToken<Map<ChallengeType, ChallengeResult>>() {
		}).getType(), new MapSerializer<>(ChallengeType.class, ChallengeResult.class));
		builder.registerTypeAdapter((new TypeToken<Map<UUID, Long>>() {
		}).getType(), new MapSerializer<>(UUID.class, Long.class));
		builder.registerTypeAdapter((new TypeToken<Map<FlagType, AccessType>>() {
		}).getType(), new MapSerializer<>(FlagType.class, AccessType.class));
		builder.registerTypeAdapter((new TypeToken<Set<UUID>>() {
		}).getType(), new SetSerializer<>(UUID.class));
		builder.registerTypeAdapter((new TypeToken<List<String>>() {
		}).getType(), new ListSerializer<>(String.class));
		// Register adapter factory
		builder.registerTypeAdapterFactory(new HellblockTypeAdapterFactory());
		// Allow characters like < or > without escaping them
		builder.disableHtmlEscaping();
		gson = builder.create();
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
				if (this.onlineUserMap.values().isEmpty())
					return;
				long finalTime = System.currentTimeMillis();
				this.dataSource.updateManyPlayersData(this.onlineUserMap.values(), !HBConfig.lockData);
				if (HBConfig.logDataSaving)
					LogUtils.info(String.format("Data Saved for online players. Took %sms.",
							(System.currentTimeMillis() - finalTime)));
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

	public Gson getGson() {
		return gson;
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
		if (player.hasPermission("hellblock.updates") && instance.isUpdateAvailable()) {
			instance.getAdventureManager().sendMessageWithPrefix(player,
					"<red>There is a new update available!: <dark_red><u>https://github.com/Swiftlicious01/Hellblock<!u>");
		}
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

		instance.getIslandLevelManager().saveCache(uuid);
		instance.getNetherrackGeneratorHandler().savePistons(uuid);

		OnlineUser onlineUser = onlineUserMap.remove(uuid);
		if (onlineUser == null)
			return;
		PlayerData data = onlineUser.getPlayerData();

		onlineUser.hideBorder();
		onlineUser.stopSpawningAnimals();

		if (onlineUser.hasGlowstoneToolEffect() || onlineUser.hasGlowstoneArmorEffect()) {
			if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				onlineUser.isHoldingGlowstoneTool(false);
				onlineUser.isWearingGlowstoneArmor(false);
			}
		}
		if (instance.getPlayerListener().getCancellablePortal().containsKey(uuid)
				&& instance.getPlayerListener().getCancellablePortal().get(uuid) != null) {
			if (!instance.getPlayerListener().getCancellablePortal().get(uuid).isCancelled())
				instance.getPlayerListener().getCancellablePortal().get(uuid).cancel();
			instance.getPlayerListener().getCancellablePortal().remove(uuid);
		}
		if (instance.getPlayerListener().getLinkPortalCatcher().contains(uuid))
			instance.getPlayerListener().getLinkPortalCatcher().remove(uuid);
		// Cleanup
		instance.getNetherrackGeneratorHandler().getGenManager().cleanupExpiredPistons(uuid);
		instance.getNetherrackGeneratorHandler().getGenManager().cleanupExpiredLocations();

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
				LogUtils.warn(String.format("Tried 3 times when getting data for %s. Giving up.", uuid));
				return;
			}
			this.dataSource.getPlayerData(uuid, HBConfig.lockData).thenAccept(optionalData -> {
				// Data should not be empty
				if (optionalData.isEmpty()) {
					LogUtils.severe("Unexpected error: Data is null.");
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
		if (bukkitUser.getHellblockData().isAbandoned()) {
			instance.getAdventureManager().sendMessageWithPrefix(player,
					String.format("<red>Your hellblock was deemed abandoned for not logging in for the past %s days!",
							instance.getConfig("config.yml").getInt("hellblock.abandon-after-days")));
			instance.getAdventureManager().sendMessageWithPrefix(player,
					"<red>You've lost access to your island, if you wish to recover it speak to an administrator.");
		}

		instance.getIslandLevelManager().loadCache(player.getUniqueId());
		instance.getNetherrackGeneratorHandler().loadPistons(player.getUniqueId());

		bukkitUser.showBorder();
		bukkitUser.startSpawningAnimals();
		instance.getNetherFarmingHandler().trackNetherFarms(bukkitUser);

		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (player.getLocation() != null) {
			LocationUtils.isSafeLocationAsync(player.getLocation()).thenAccept((playerResult) -> {
				if (!playerResult.booleanValue()) {
					if (bukkitUser.getHellblockData().hasHellblock()) {
						if (bukkitUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						instance.getStorageManager().getOfflineUser(bukkitUser.getHellblockData().getOwnerUUID(), false)
								.thenAccept((owner) -> {
									OfflineUser ownerUser = owner.get();
									if (ownerUser.getHellblockData().getHomeLocation() != null) {
										instance.getCoopManager().makeHomeLocationSafe(ownerUser, bukkitUser);
									} else {
										instance.getHellblockHandler().teleportToSpawn(player, false);
									}
								});
					} else {
						instance.getHellblockHandler().teleportToSpawn(player, false);
					}
				}
			});
		}

		if (instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player) != null) {
			instance.getCoopManager()
					.kickVisitorsIfLocked(instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player));

			if (instance.getCoopManager().trackBannedPlayer(
					instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player), player.getUniqueId())) {
				if (bukkitUser.getHellblockData().hasHellblock()) {
					if (bukkitUser.getHellblockData().getOwnerUUID() == null) {
						throw new NullPointerException(
								"Owner reference returned null, please report this to the developer.");
					}
					instance.getStorageManager().getOfflineUser(bukkitUser.getHellblockData().getOwnerUUID(), false)
							.thenAccept((owner) -> {
								OfflineUser ownerUser = owner.get();
								instance.getCoopManager().makeHomeLocationSafe(ownerUser, bukkitUser);
							});
				} else {
					instance.getHellblockHandler().teleportToSpawn(player, true);
				}
			}
		}

		if (bukkitUser.inUnsafeLocation()) {
			instance.getHellblockHandler().teleportToSpawn(player, true);
			bukkitUser.setInUnsafeLocation(false);
			instance.getAdventureManager().sendMessageWithPrefix(player,
					"<red>You logged out in an unsafe hellblock environment because it was reset or deleted.");
		}

		if (instance.getNetherArmorHandler().gsNightVisionArmor && instance.getNetherArmorHandler().gsArmor) {
			ItemStack[] armorSet = player.getInventory().getArmorContents();
			boolean checkArmor = false;
			if (armorSet != null) {
				for (ItemStack item : armorSet) {
					if (item == null || item.getType() == Material.AIR)
						continue;
					if (instance.getNetherArmorHandler().checkNightVisionArmorStatus(item)
							&& instance.getNetherArmorHandler().getNightVisionArmorStatus(item)) {
						checkArmor = true;
						break;
					}
				}

				if (checkArmor) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
					bukkitUser.isWearingGlowstoneArmor(true);
				}
			}
		}

		if (instance.getNetherToolsHandler().gsNightVisionTool && instance.getNetherToolsHandler().gsTools) {
			ItemStack tool = player.getInventory().getItemInMainHand();
			if (tool.getType() == Material.AIR) {
				tool = player.getInventory().getItemInOffHand();
				if (tool.getType() == Material.AIR) {
					return;
				}
			}

			if (instance.getNetherToolsHandler().checkNightVisionToolStatus(tool)
					&& instance.getNetherToolsHandler().getNightVisionToolStatus(tool)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				bukkitUser.isHoldingGlowstoneTool(true);
			}
		}
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
			LogUtils.severe("Failed to parse PlayerData from json.");
			LogUtils.info(String.format("Json: %s", json));
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