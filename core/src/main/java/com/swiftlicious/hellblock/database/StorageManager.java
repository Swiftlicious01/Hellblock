package com.swiftlicious.hellblock.database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;
import com.swiftlicious.hellblock.challenges.requirement.LevelUpRequirement;
import com.swiftlicious.hellblock.commands.CommandConfig;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.handlers.VisitManager.VisitRecord;
import com.swiftlicious.hellblock.player.CoopChatSetting;
import com.swiftlicious.hellblock.player.DisplaySettings;
import com.swiftlicious.hellblock.player.DisplaySettings.DisplayChoice;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.UserDataInterface;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.utils.adapters.BooleanAdapter;
import com.swiftlicious.hellblock.utils.adapters.DoubleAdapter;
import com.swiftlicious.hellblock.utils.adapters.EnumMapSerializer;
import com.swiftlicious.hellblock.utils.adapters.EnumOmitDefaultAdapter;
import com.swiftlicious.hellblock.utils.adapters.FloatAdapter;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory;
import com.swiftlicious.hellblock.utils.adapters.IntegerAdapter;
import com.swiftlicious.hellblock.utils.adapters.ListSerializer;
import com.swiftlicious.hellblock.utils.adapters.LongAdapter;
import com.swiftlicious.hellblock.utils.adapters.MapSerializer;
import com.swiftlicious.hellblock.utils.adapters.NestedMapSerializer;
import com.swiftlicious.hellblock.utils.adapters.SetSerializer;
import com.swiftlicious.hellblock.utils.adapters.StringAdapter;
import com.swiftlicious.hellblock.world.HellblockWorld;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * This class implements the StorageManager interface and is responsible for
 * managing player data storage. It includes methods to handle player data
 * retrieval, storage, and serialization.
 */
public class StorageManager implements StorageManagerInterface, Listener {

	protected final HellblockPlugin instance;
	private DataStorageProvider dataSource;
	private StorageType previousType;
	private final ConcurrentMap<UUID, UserData> onlineUserMap = new ConcurrentHashMap<>();
	private final Cache<UUID, UserData> offlineUserCache = Caffeine.newBuilder().maximumSize(10_000)
			.expireAfterAccess(Duration.ofMinutes(15)).build();
	// Keeps track of each user's last saved snapshot
	private final Map<UUID, PlayerData> lastSavedSnapshots = new ConcurrentHashMap<>();
	private final Set<UUID> locked = new HashSet<>();
	private boolean hasRedis;
	private RedisManager redisManager;
	private String serverID;
	private SchedulerTask timerSaveTask;
	private final Gson gson;

	private final Set<UUID> forceUnlockedPlayers = ConcurrentHashMap.newKeySet();
	private final Map<UUID, Object> cacheLocks = new ConcurrentHashMap<>();
	private final Set<UUID> cachingInProgress = ConcurrentHashMap.newKeySet();
	// Class-level set to deduplicate callbacks
	private final ConcurrentMap<UUID, Runnable> pendingCacheCallbacks = new ConcurrentHashMap<>();
	private final Set<UUID> confirmedInsertFutureCallbacks = ConcurrentHashMap.newKeySet();
	private final Set<UUID> callbackDeferralGuard = ConcurrentHashMap.newKeySet();
	private RetryManager retryManager;

	private final ExecutorService preloadExecutor = Executors.newFixedThreadPool(10); // adjust threads

	public StorageManager(HellblockPlugin plugin) {
		instance = plugin;
		// Build the gson
		// excludeFieldsWithoutExposeAnnotation - this means that every field to be
		// stored should use @Expose
		// enableComplexMapKeySerialization - forces GSON to use TypeAdapters even for
		// Map keys
		final GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.enableComplexMapKeySerialization().setPrettyPrinting();

		// --- Collections ---
		builder.registerTypeAdapter(new TypeToken<Map<ChallengeType, ChallengeResult>>() {
		}.getType(), new MapSerializer<>(ChallengeType.class, ChallengeResult.class));
		builder.registerTypeAdapter(new TypeToken<Map<UUID, Long>>() {
		}.getType(), new MapSerializer<>(UUID.class, Long.class));
		builder.registerTypeAdapter(new TypeToken<Map<UUID, Long>>() {
		}.getType(), new MapSerializer<>(String.class, Integer.class));
		builder.registerTypeAdapter(new TypeToken<Map<UUID, Long>>() {
		}.getType(), new MapSerializer<>(String.class, Float.class));
		builder.registerTypeAdapter(new TypeToken<Map<Integer, List<String>>>() {
		}.getType(), new MapSerializer<>(Integer.class, List.class));
		builder.registerTypeAdapter(new TypeToken<EnumMap<FlagType, HellblockFlag>>() {
		}.getType(), new EnumMapSerializer<>(FlagType.class, HellblockFlag.class));
		Type mapType = new TypeToken<Map<String, Map<String, Integer>>>() {
		}.getType();
		builder.registerTypeAdapter(mapType, new NestedMapSerializer<>(String.class, Integer.class));
		builder.registerTypeAdapter(new TypeToken<EnumMap<IslandUpgradeType, Integer>>() {
		}.getType(), new EnumMapSerializer<>(IslandUpgradeType.class, Integer.class));
		builder.registerTypeAdapter(new TypeToken<Set<UUID>>() {
		}.getType(), new SetSerializer<>(UUID.class));
		builder.registerTypeAdapter(new TypeToken<List<String>>() {
		}.getType(), new ListSerializer<>(String.class));
		builder.registerTypeAdapter(new TypeToken<List<MailboxEntry>>() {
		}.getType(), new ListSerializer<>(MailboxEntry.class));
		builder.registerTypeAdapter(new TypeToken<List<VisitRecord>>() {
		}.getType(), new ListSerializer<>(VisitRecord.class));

		// --- Plugin-specific Enums: use null for default (omit during serialization)
		builder.registerTypeAdapter(CompletionStatus.class,
				new EnumOmitDefaultAdapter<>(CompletionStatus.class, CompletionStatus.NOT_STARTED));
		builder.registerTypeAdapter(AccessType.class, new EnumOmitDefaultAdapter<>(AccessType.class, AccessType.ALLOW));
		builder.registerTypeAdapter(HellBiome.class,
				new EnumOmitDefaultAdapter<>(HellBiome.class, HellBiome.NETHER_WASTES));
		builder.registerTypeAdapter(CoopChatSetting.class,
				new EnumOmitDefaultAdapter<>(CoopChatSetting.class, CoopChatSetting.GLOBAL));
		builder.registerTypeAdapter(DisplayChoice.class,
				new EnumOmitDefaultAdapter<>(DisplayChoice.class, DisplayChoice.CHAT));

		// --- Primitives / Boxed primitives ---
		builder.registerTypeAdapter(Integer.class, new IntegerAdapter());
		builder.registerTypeAdapter(Long.class, new LongAdapter());
		builder.registerTypeAdapter(Float.class, new FloatAdapter());
		builder.registerTypeAdapter(Double.class, new DoubleAdapter());
		builder.registerTypeAdapter(Boolean.class, new BooleanAdapter());
		builder.registerTypeAdapter(String.class, new StringAdapter());

		// --- Core types ---
		builder.registerTypeAdapterFactory(new HellblockTypeAdapterFactory());

		// --- Cleanup output
		builder.disableHtmlEscaping();

		gson = builder.create();
	}

	public void initialize() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	/**
	 * Reloads the storage manager configuration.
	 */
	@Override
	public void reload() {
		final YamlDocument config = instance.getConfigManager().loadConfig("database.yml");
		this.serverID = config.getString("unique-server-id", "default");
		try {
			config.save(new File(instance.getDataFolder(), "database.yml"));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		// Check if storage type has changed and reinitialize if necessary
		final StorageType storageType = StorageType
				.valueOf(config.getString("data-storage-method", "H2").toUpperCase(Locale.ENGLISH));
		if (storageType != previousType) {
			if (this.dataSource != null) {
				this.dataSource.disable();
			}
			this.previousType = storageType;
			switch (storageType) {
			case H2 -> this.dataSource = new H2Handler(instance);
			case JSON -> this.dataSource = new JsonHandler(instance);
			case YAML -> this.dataSource = new YamlHandler(instance);
			case SQLite -> this.dataSource = new SQLiteHandler(instance);
			case MySQL -> this.dataSource = new MySQLHandler(instance);
			case PostgreSQL -> this.dataSource = new PostgreSQLHandler(instance);
			case MariaDB -> this.dataSource = new MariaDBHandler(instance);
			case MongoDB -> this.dataSource = new MongoDBHandler(instance);
			default -> {
				this.dataSource = new H2Handler(instance);
				throw new IllegalArgumentException(
						"Defaulting to H2 because of unexpected value: " + config.getString("data-storage-method"));
			}
			}
			if (this.dataSource != null) {
				this.dataSource.initialize(config);
			} else {
				instance.getPluginLogger().severe("No storage type is set.");
			}
		}

		this.retryManager = new RetryManager(
				(task, delay) -> instance.getScheduler().asyncLater(task, delay, TimeUnit.SECONDS), 4, // max retries
				1 // initial delay in seconds
		);

		// Handle Redis configuration
		if (!this.hasRedis && config.getBoolean("Redis.enable", false)) {
			this.redisManager = new RedisManager(instance);
			this.redisManager.initialize(config);
			this.hasRedis = true;
		}

		// Disable Redis if it was enabled but is now disabled
		if (this.hasRedis && !config.getBoolean("Redis.enable", false) && this.redisManager != null) {
			this.hasRedis = false;
			this.redisManager.disable();
			this.redisManager = null;
		}

		// Cancel any existing timerSaveTask
		if (this.timerSaveTask != null && !this.timerSaveTask.isCancelled()) {
			this.timerSaveTask.cancel();
			this.timerSaveTask = null;
		}

		// Schedule periodic data saving if dataSaveInterval is configured
		if (instance.getConfigManager().dataSaveInterval() > 0) {
			this.timerSaveTask = instance.getScheduler().asyncRepeating(() -> {
				if (this.onlineUserMap.isEmpty() && this.offlineUserCache.asMap().isEmpty()) {
					return;
				}

				final long startTime = System.currentTimeMillis();

				// Determine which online users have changed
				List<UserData> dirtyOnlineUsers = this.onlineUserMap.values().stream().filter(user -> {
					PlayerData current = user.toPlayerData();
					PlayerData lastSaved = lastSavedSnapshots.get(user.getUUID());
					return lastSaved == null || !Arrays.equals(current.toBytes(), lastSaved.toBytes());
				}).toList();

				if (!dirtyOnlineUsers.isEmpty()) {
					this.dataSource.updateManyPlayersData(dirtyOnlineUsers, true).thenRun(() -> {
						// Update snapshots after successful save
						dirtyOnlineUsers.forEach(user -> lastSavedSnapshots.put(user.getUUID(), user.toPlayerData()));
						instance.debug("Online batch update successful for " + dirtyOnlineUsers.size() + " user"
								+ (dirtyOnlineUsers.size() == 1 ? "" : "s") + "!");
					}).exceptionally(ex -> {
						instance.getPluginLogger().severe("Online batch update failed", ex);
						return null;
					});
				}

				// Handle offline users, but skip ones already updated
				Set<UUID> updatedOnlineUUIDs = dirtyOnlineUsers.stream().map(UserData::getUUID)
						.collect(Collectors.toSet());

				Collection<UserData> filteredOfflineUsers = this.offlineUserCache.asMap().values().stream()
						.filter(user -> !updatedOnlineUUIDs.contains(user.getUUID())).filter(user -> {
							PlayerData current = user.toPlayerData();
							PlayerData lastSaved = lastSavedSnapshots.get(user.getUUID());
							return lastSaved == null || !Arrays.equals(current.toBytes(), lastSaved.toBytes());
						}).toList();

				if (!filteredOfflineUsers.isEmpty()) {
					this.dataSource.updateManyPlayersData(filteredOfflineUsers, true).thenRun(() -> {
						filteredOfflineUsers
								.forEach(user -> lastSavedSnapshots.put(user.getUUID(), user.toPlayerData()));
						instance.debug("Offline batch update successful for " + filteredOfflineUsers.size() + " user"
								+ (filteredOfflineUsers.size() == 1 ? "" : "s") + "!");
					}).exceptionally(ex -> {
						instance.getPluginLogger().severe("Offline batch update failed", ex);
						return null;
					});
				}

				// Logging
				if ((!dirtyOnlineUsers.isEmpty() || !filteredOfflineUsers.isEmpty())
						&& instance.getConfigManager().logDataSaving()) {
					instance.getPluginLogger()
							.info("Data saved for " + (dirtyOnlineUsers.size() + filteredOfflineUsers.size()) + " user"
									+ ((dirtyOnlineUsers.size() + filteredOfflineUsers.size()) == 1 ? "" : "s")
									+ " (online/offline). Took " + (System.currentTimeMillis() - startTime) + "ms.");
				}

			}, instance.getConfigManager().dataSaveInterval(), instance.getConfigManager().dataSaveInterval(),
					TimeUnit.SECONDS);
		}

		preloadCachedIslandOwners();
	}

	/**
	 * Disables the storage manager and cleans up resources.
	 */
	@Override
	public void disable() {
		HandlerList.unregisterAll(this);
		if (this.dataSource != null) {
			List<UserData> onlineUsers = new ArrayList<>(this.onlineUserMap.values());
			List<UserData> offlineUsers = new ArrayList<>(this.offlineUserCache.asMap().values());

			if (onlineUsers.isEmpty() && offlineUsers.isEmpty()) {
				instance.debug("No user data to save during shutdown.");
			} else {
				Set<UUID> onlineUUIDs = onlineUsers.stream().map(UserData::getUUID).collect(Collectors.toSet());

				// Save online users
				if (!onlineUsers.isEmpty()) {
					this.dataSource.updateManyPlayersData(onlineUsers, true)
							.thenRun(() -> instance.debug("Online batch update successful for " + onlineUsers.size()
									+ " user" + (onlineUsers.size() == 1 ? "" : "s") + "!"))
							.exceptionally(ex -> {
								instance.getPluginLogger().severe("Online batch update failed", ex);
								return null;
							});
				}

				// Save offline users excluding already updated online ones
				List<UserData> filteredOfflineUsers = offlineUsers.stream()
						.filter(user -> !onlineUUIDs.contains(user.getUUID())).toList();

				if (!filteredOfflineUsers.isEmpty()) {
					this.dataSource.updateManyPlayersData(filteredOfflineUsers, true).thenRun(
							() -> instance.debug("Offline batch update successful for " + filteredOfflineUsers.size()
									+ " user" + (filteredOfflineUsers.size() == 1 ? "" : "s") + "!"))
							.exceptionally(ex -> {
								instance.getPluginLogger().severe("Offline batch update failed", ex);
								return null;
							});
				}
			}

			this.dataSource.disable();
		}
		if (this.redisManager != null) {
			this.redisManager.disable();
		}
		if (this.retryManager != null) {
			this.retryManager.active.stream().forEach(id -> this.retryManager.cancel(id));
			this.retryManager.active.clear();
		}
		this.onlineUserMap.clear();
		this.offlineUserCache.cleanUp();
	}

	@Override
	public CompletableFuture<Void> preloadCachedIslandOwners() {
		return instance.getCoopManager().getCachedIslandOwners().thenCompose(owners -> {
			if (owners == null || owners.isEmpty()) {
				return CompletableFuture.completedFuture(null);
			}

			List<CompletableFuture<Void>> preloadTasks = owners.stream()
					.map(ownerId -> getCachedUserDataWithFallback(ownerId, false).thenAcceptAsync(optData -> {
						if (optData.isPresent()) {
							instance.debug("Preloaded user data for island ownerId: " + ownerId);
						}
					}, preloadExecutor)).toList();

			return CompletableFuture.allOf(preloadTasks.toArray(CompletableFuture[]::new))
					.thenRun(() -> instance.debug("Finished preloading island owner data for " + owners.size()
							+ " owner" + (owners.size() == 1 ? "" : "s") + "."));
		});
	}

	public void shutdownPreloadExecutor() {
		preloadExecutor.shutdownNow();
	}

	@NotNull
	@Override
	public String getServerID() {
		return serverID;
	}

	public Gson getGson() {
		return gson;
	}

	@NotNull
	@Override
	public Optional<@Nullable UserData> getOnlineUser(UUID uuid) {
		return Optional.ofNullable(onlineUserMap.get(uuid));
	}

	@NotNull
	@Override
	public Collection<UserData> getOnlineUsers() {
		return onlineUserMap.values();
	}

	@NotNull
	@Override
	public Optional<@Nullable UserData> getCachedUserData(@NotNull UUID uuid) {
		// Prefer online user
		Optional<UserData> online = getOnlineUser(uuid);
		if (online.isPresent())
			return online;

		// Check cached offline user
		return Optional.ofNullable(offlineUserCache.getIfPresent(uuid));
	}

	@NotNull
	@Override
	public CompletableFuture<Optional<UserData>> getCachedUserDataWithFallback(UUID uuid, boolean lock) {
		// First, try cached (online or offline)
		Optional<UserData> cached = getCachedUserData(uuid);
		if (cached.isPresent()) {
			return CompletableFuture.completedFuture(cached);
		}

		// Fallback to async offline data load
		return getOfflineUserData(uuid, lock);
	}

	@Override
	public void invalidateCachedUserData(@NotNull UUID uuid) {
		offlineUserCache.invalidate(uuid);
	}

	@Override
	public CompletableFuture<Optional<UserData>> getOfflineUserData(UUID uuid, boolean lock) {
		final CompletableFuture<Optional<PlayerData>> optionalDataFuture = dataSource.getPlayerData(uuid, lock, null);

		return optionalDataFuture.thenCompose(optData -> {
			if (optData.isEmpty()) {
				return CompletableFuture.completedFuture(Optional.empty());
			}

			final PlayerData data = optData.get();
			UserData userData = UserDataInterface.builder().setData(data).build();

			offlineUserCache.put(uuid, userData);

			return CompletableFuture.completedFuture(Optional.of(userData));
		});
	}

	@Override
	public CompletableFuture<Optional<UserData>> getOfflineUserDataByIslandId(int islandId, boolean lock) {
		CompletableFuture<Optional<PlayerData>> optionalDataFuture = dataSource.getPlayerDataByIslandId(islandId, lock,
				null);

		return optionalDataFuture.thenCompose(optData -> {
			if (optData.isEmpty()) {
				return CompletableFuture.completedFuture(Optional.empty());
			}

			PlayerData data = optData.get();
			UserData userData = UserDataInterface.builder().setData(data).build();

			UUID ownerUUID = userData.getHellblockData().getOwnerUUID();
			if (ownerUUID != null) {
				offlineUserCache.put(ownerUUID, userData);
			}

			return CompletableFuture.completedFuture(Optional.of(userData));
		});
	}

	@Override
	public CompletableFuture<Boolean> saveUserData(UserData userData, boolean unlock) {
		return dataSource.updatePlayerData(userData.getUUID(), userData.toPlayerData(), unlock);
	}

	@Override
	public CompletableFuture<Void> unlockUserData(UUID uuid) {
		return CompletableFuture.runAsync(() -> {
			dataSource.lockOrUnlockPlayerData(uuid, false);
		}, instance.getScheduler().async());
	}

	@NotNull
	@Override
	public DataStorageProvider getDataSource() {
		return dataSource;
	}

	/**
	 * Event handler for when a player joins the server. Locks the player's data and
	 * initiates data retrieval if Redis is not used, otherwise, it starts a Redis
	 * data retrieval task.
	 */
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final UUID uuid = player.getUniqueId();
		locked.add(uuid);

		// Send update notification if needed
		if (player.hasPermission("hellblock.updates") && instance.isUpdateAvailable()) {
			String currentVersion = VersionHelper.getCurrentVersion();
			String latestVersion = VersionHelper.getLatestVersion();

			String message = """
					<gradient:#ff5555:#aa0000><bold>[!]</bold></gradient> <bold><red>Hellblock Update Available!</red>
					<gray>You're on</gray> <yellow>v%s</yellow><gray>, latest is</gray> <green>v%s</green>
					<hover:show_text:'<green>Open GitHub page'><click:open_url:'https://github.com/Swiftlicious01/Hellblock'><underlined><aqua>Click here to update</aqua></underlined></click></hover>
					"""
					.formatted(currentVersion, latestVersion);

			instance.getSenderFactory().wrap(player).sendMessage(AdventureHelper.miniMessageToComponent(message));
		}

		if (!hasRedis) {
			// Local data load with retries
			scheduleDataUnlockRetry(uuid);
		} else {
			// fallback path
			// Redis path with retries
			// Redis-based retry, fallback to local if needed
			instance.getScheduler().asyncLater(() -> redisManager.getChangeServer(uuid).thenAccept(changeServer -> {
				if (!changeServer) {
					scheduleDataUnlockRetry(uuid);
				} else {
					scheduleRedisDataRetry(uuid);
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
		final Player player = event.getPlayer();
		final UUID uuid = player.getUniqueId();
		if (locked.contains(uuid)) {
			return;
		}

		instance.getProtectionManager().cancelBlockScan(uuid);

		instance.getSchematicManager().schematicPaster.cancelPaste(uuid);

		trackOfflineVisitor(player).whenComplete((success, ex) -> {
			if (ex != null) {
				instance.getPluginLogger().warn("Failed to track offline visitor state for " + player.getName(), ex);
			}

			instance.getCoopManager().invalidateVisitingIsland(uuid);

			final UserData userData = onlineUserMap.remove(uuid);
			if (userData == null) {
				instance.debug("No UserData found for " + player.getName() + ". Possibly already unloaded.");
				return;
			}

			if (userData.getHellblockData().hasHellblock())
				userData.getHellblockData().updateLastIslandActivity();

			instance.getBorderHandler().setBorderExpanding(uuid, false);
			instance.getBorderHandler().stopBorderTask(uuid);

			if (userData.getHellblockData().getOwnerUUID() != null
					&& userData.getHellblockData().getOwnerUUID().equals(uuid)) {
				instance.getIslandLevelManager().serializePlacedBlocks(userData.getHellblockData().getIslandId());
				String worldName = instance.getWorldManager()
						.getHellblockWorldFormat(userData.getHellblockData().getIslandId());
				Optional<HellblockWorld<?>> hellWorld = instance.getWorldManager().getWorld(worldName);
				hellWorld.ifPresent(world -> instance.getNetherrackGeneratorHandler()
						.savePistonsByIsland(userData.getHellblockData().getIslandId(), world));
			}

			instance.getIslandManager().resolveIslandId(player.getLocation()).thenAccept(optIslandId -> optIslandId
					.ifPresent(islandId -> instance.getIslandManager().handlePlayerLeaveIsland(player, islandId)));

			getDataSource().invalidateCache(uuid);

			if ((userData.hasGlowstoneToolEffect() || userData.hasGlowstoneArmorEffect())
					&& player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				userData.isHoldingGlowstoneTool(false);
				userData.isWearingGlowstoneArmor(false);
			}

			// Cleanup
			instance.getNetherrackGeneratorHandler().getGeneratorManager().cleanupExpiredPositions();

			// Check if this player is an island owner
			instance.getCoopManager().getIslandOwner(uuid).thenAccept(optData -> {
				if (optData.isPresent()) {
					final UUID ownerId = optData.get();

					// Only snapshot for the owner (even if a coop member logs out)
					// small delay to let logout finish
					instance.getScheduler().asyncLater(() -> instance.getIslandBackupManager().maybeSnapshot(ownerId),
							2, TimeUnit.SECONDS);
				}

				final PlayerData data = userData.toPlayerData();

				if (hasRedis) {
					redisManager.setChangeServer(uuid).thenCompose(v -> redisManager.updatePlayerData(uuid, data, true))
							.thenCompose(v -> dataSource.updatePlayerData(uuid, data, true)).thenAccept(result -> {
								if (result)
									locked.remove(uuid);
							});
				} else {
					dataSource.updatePlayerData(uuid, data, true).thenAccept(result -> {
						if (result) {
							locked.remove(uuid);
						}
					});
				}
			});
		});
	}

	private CompletableFuture<Boolean> trackOfflineVisitor(@NotNull Player player) {
		final UUID uuid = player.getUniqueId();

		return instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(false);
			}

			return getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return CompletableFuture.completedFuture(false);
				}

				final UserData ownerData = optData.get();
				final HellblockData hellblockData = ownerData.getHellblockData();
				final BoundingBox bounds = hellblockData.getBoundingBox();
				final Location hellblockLocation = hellblockData.getHellblockLocation();

				if (bounds == null || hellblockLocation == null || hellblockLocation.getWorld() == null) {
					return CompletableFuture.completedFuture(false);
				}

				if (hellblockData.getPartyPlusOwner().contains(uuid)) {
					return CompletableFuture.completedFuture(false);
				}

				if (player.getWorld().getUID().equals(hellblockLocation.getWorld().getUID())
						&& bounds.contains(player.getLocation().toVector())) {

					if (hellblockData.getOfflineVisitors().contains(uuid)) {
						return CompletableFuture.completedFuture(false);
					}

					hellblockData.addOfflineVisitor(uuid);
					return saveUserData(ownerData, true);
				}

				return CompletableFuture.completedFuture(false);
			}).handle((result, ex) -> {
				return unlockUserData(ownerUUID).thenApply(unused -> {
					if (ex != null) {
						instance.getPluginLogger().warn("Failed to add player " + player.getName()
								+ " to offline visitor list: " + ex.getMessage(), ex);
						return false;
					}
					return result != null && result;
				});
			}).thenCompose(Function.identity());
		});
	}

	/**
	 * Runnable task for asynchronously retrieving data from Redis. Retries up to 6
	 * times and cancels the task if the player is offline.
	 */
	public void scheduleRedisDataRetry(UUID uuid) {
		retryManager.retry(uuid, (id, attempt, retry, cancel) -> {
			final Player player = Bukkit.getPlayer(id);
			if (player == null || !player.isOnline()) {
				cancel.run(); // Player went offline, stop retrying
				return;
			}

			if (dataSource.isPendingInsert(uuid) || dataSource.isInsertStillRecent(uuid)) {
				instance.debug("Delaying Redis retry — insert still active or recent for " + id);
				instance.getScheduler().asyncLater(() -> scheduleRedisDataRetry(id), 250, TimeUnit.MILLISECONDS);
				return;
			}

			if (attempt >= 6) {
				instance.getPluginLogger()
						.warn("Redis data fetch failed after 6 attempts for " + id + ". Falling back.");
				cancel.run();
				scheduleDataUnlockRetry(id);
				return;
			}

			redisManager.getPlayerData(id, false, null).thenAccept(optionalData -> {
				if (optionalData.isPresent()) {
					try {
						putDataInCache(player, optionalData.get());
						dataSource.lockOrUnlockPlayerData(id, false);
					} catch (Exception e) {
						e.printStackTrace();
					}
					cancel.run(); // Done
				} else {
					retry.run(); // Try again later
				}
			});
		});
	}

	/**
	 * Waits for data lock release with a delay and a maxium of four retries.
	 *
	 * @param uuid The UUID of the player.
	 */
	public void scheduleDataUnlockRetry(UUID uuid) {
		retryManager.retry(uuid, (id, attempt, retry, cancel) -> {
			final Player player = Bukkit.getPlayer(id);
			if (player == null || !player.isOnline()) {
				cancel.run(); // stop retrying if player left
				return;
			}

			if (dataSource.isPendingInsert(uuid) || dataSource.isInsertStillRecent(uuid)) {
				Long startedAt = dataSource.getInsertAge(uuid);

				long elapsed = startedAt != null ? (System.currentTimeMillis() - startedAt) : -1;
				String elapsedStr = elapsed >= 0 ? " (insert age: " + elapsed + "ms)" : "";

				instance.debug("Delaying retry — insert still active or recent for " + id + elapsedStr);

				instance.getScheduler().asyncLater(() -> scheduleDataUnlockRetry(id), 250, TimeUnit.MILLISECONDS);
				return;
			}

			dataSource.getPlayerData(id, false, null).thenAccept(optionalData -> {
				if (optionalData.isEmpty()) {
					instance.getPluginLogger().severe("Unexpected error: Player data is null for " + id);
					cancel.run();
					return;
				}

				PlayerData playerData = optionalData.get();

				if (playerData.isLocked()) {
					if (attempt >= 4) {
						instance.getPluginLogger().severe("Player " + id + "'s data is still locked after " + attempt
								+ " attempts. Forcing unlock...");

						// Run unlock with timeout protection
						CompletableFuture.runAsync(() -> {
							dataSource.lockOrUnlockPlayerData(id, false);

							// Fallback: re-insert empty row if missing
							dataSource.getPlayerData(id, false, null).thenAccept(optional -> {
								if (optional.isPresent() && !optional.get().isLocked()) {
									instance.debug("Row unlocked successfully for retry: " + id);
								}
								if (optional.isEmpty()) {
									instance.getPluginLogger()
											.warn("Unlock fallback: player row missing — inserting empty record.");
									forceUnlockedPlayers.add(id);
									PlayerData data = PlayerData.empty();
									data.setUUID(id);
									dataSource.updateOrInsertPlayerData(id, data, false);
								} else {
									instance.debug("Unlock fallback successful — existing player record unlocked.");
								}
							});
						}).orTimeout(3, TimeUnit.SECONDS).whenComplete((unused, ex) -> {
							if (ex != null) {
								instance.getPluginLogger().warn("Timeout or error while force-unlocking data for " + id,
										ex);
							} else {
								instance.getPluginLogger().warn("Force-unlocked data for " + id);
							}

							// One final retry after unlock
							retry.run();
						});

					} else {
						instance.getPluginLogger()
								.warn("Player " + id + "'s data is locked. Retrying (" + attempt + "/4)...");
						retry.run();
					}
				} else {
					try {
						putDataInCache(player, playerData);
					} catch (Exception e) {
						e.printStackTrace();
					}
					cancel.run(); // success
				}
			});
		});
	}

	/**
	 * Puts player data in cache and removes the player from the locked set.
	 *
	 * @param player     The player whose data is being cached.
	 * @param playerData The data to be cached.
	 */
	private void putDataInCache(Player player, PlayerData playerData) {
		putDataInCache(player, playerData, false);
	}

	/**
	 * Puts player data in cache and removes the player from the locked set.
	 *
	 * @param player       The player whose data is being cached.
	 * @param playerData   The data to be cached.
	 * @param fromCallback whether it's a callback version
	 */
	private void putDataInCache(Player player, PlayerData playerData, boolean fromCallback) {
		UUID uuid = player.getUniqueId();

		// EARLY EXIT: Block duplicates unless it's the delayed retry
		if (!fromCallback && !cachingInProgress.add(uuid)) {
			instance.debug("Cache already in progress for %s, skipping.".formatted(player.getName()));
			return;
		}

		Object lock = cacheLocks.computeIfAbsent(uuid, __ -> new Object());

		synchronized (lock) {
			// Only check this if we're *not already* in the callback
			if (!fromCallback && dataSource.isPendingInsert(uuid)) {
				instance.debug("Insert still pending, will cache %s after it finishes.".formatted(player.getName()));

				Runnable previous = pendingCacheCallbacks.putIfAbsent(uuid,
						() -> instance.getScheduler().executeSync(() -> putDataInCache(player, playerData, true)));

				if (previous == null) {
					instance.debug("Insert still pending, callback registered for %s.".formatted(player.getName()));

					dataSource.getInsertFuture(uuid).thenRun(() -> {
						instance.debug("Insert completed, now caching %s.".formatted(player.getName()));
						Runnable callback = pendingCacheCallbacks.remove(uuid);
						if (callback != null)
							callback.run();
					});
				} else {
					instance.debug("Callback already queued for %s, skipping duplicate.".formatted(player.getName()));
				}

				cachingInProgress.remove(uuid);
				return;
			}

			// Delay caching if insert was just recently completed
			if (dataSource.isInsertStillRecent(uuid)) {
				// If already confirmed via future, proceed without deferring again
				if (confirmedInsertFutureCallbacks.contains(uuid)) {
					instance.debug("Insert recently completed, but future already confirmed for %s — proceeding."
							.formatted(player.getName()));
				} else if (fromCallback && !callbackDeferralGuard.add(uuid)) {
					instance.debug("Insert still recent for %s — skipping repeat deferral from callback."
							.formatted(player.getName()));
					return;
				} else {
					instance.debug("Insert recently completed, deferring cache for %s until insert future confirms."
							.formatted(player.getName()));

					Runnable previous = pendingCacheCallbacks.putIfAbsent(uuid,
							() -> instance.getScheduler().executeSync(() -> putDataInCache(player, playerData, true)));

					if (previous == null) {
						dataSource.getInsertFuture(uuid).thenRun(() -> {
							confirmedInsertFutureCallbacks.add(uuid);
							instance.debug(
									"Insert future confirmed complete, now caching %s.".formatted(player.getName()));
							Runnable callback = pendingCacheCallbacks.remove(uuid);
							if (callback != null) {
								callback.run();
							}
						});
					} else {
						instance.debug("Callback already queued for %s (insert recent), skipping."
								.formatted(player.getName()));
					}

					cachingInProgress.remove(uuid);
					return;
				}
			}

			// Safe to cache now — mark in-progress and clean up concurrency guards
			pendingCacheCallbacks.remove(uuid);
			cachingInProgress.remove(uuid);
			cacheLocks.remove(uuid);
			locked.remove(uuid);
			forceUnlockedPlayers.remove(uuid);
			callbackDeferralGuard.remove(uuid);
			confirmedInsertFutureCallbacks.remove(uuid);

			// Build user wrapper with updated name and activity
			String storedName = UserDataInterface.builder().setData(playerData).build().getName();
			UserData userData = UserDataInterface.builder().setData(playerData).setName(player.getName()).build();

			// Store user data in cache
			onlineUserMap.put(uuid, userData);

			clearOfflineVisitorOnJoin(player).thenCompose(success -> {
				// Run startWorldHandlers
				return startWorldHandlers(userData);
			}).thenCompose(v -> {
				// Collect post-login futures
				CompletableFuture<Boolean> abandonedFuture = sendAbandonedWarningIfNeeded(userData);
				CompletableFuture<Boolean> displayUpdateFuture = updateIslandDisplayInfoIfNameChanged(userData,
						storedName);
				CompletableFuture<Boolean> inviteFuture = notifyPendingInvitations(userData);
				CompletableFuture<Boolean> coopNotifyFuture = notifyCoopMembersOnJoin(userData);
				CompletableFuture<Boolean> levelUpFuture = updateLevelUpChallenge(userData);

				return CompletableFuture
						.allOf(abandonedFuture, displayUpdateFuture, inviteFuture, coopNotifyFuture, levelUpFuture)
						.thenApply(ignored -> true);
			}).handle((ignored, ex) -> {
				if (ex != null) {
					instance.getPluginLogger().warn("Login initialization failed for " + player.getName(), ex);
				}

				// Run login-related systems that are not async
				instance.getMailboxManager().handleLogin(userData);
				handleNightVisionFromGear(userData);

				instance.debug("Finished caching logic for " + player.getName());
				return null;
			});
		}
	}

	private CompletableFuture<Boolean> clearOfflineVisitorOnJoin(@NotNull Player player) {
		final UUID uuid = player.getUniqueId();

		return instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(false);
			}

			return getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return CompletableFuture.completedFuture(false);
				}

				final UserData ownerData = optData.get();
				final HellblockData hellblockData = ownerData.getHellblockData();

				if (hellblockData.getPartyPlusOwner().contains(uuid)) {
					return CompletableFuture.completedFuture(false);
				}

				// If visitor isn't in the list, skip save
				if (!hellblockData.getOfflineVisitors().contains(uuid)) {
					return CompletableFuture.completedFuture(false);
				}

				hellblockData.removeOfflineVisitor(uuid);
				return saveUserData(ownerData, true);
			}).handle((result, ex) -> {
				return unlockUserData(ownerUUID).thenApply(unused -> {
					if (ex != null) {
						instance.getPluginLogger().warn("Failed to remove player " + player.getName()
								+ " from offline visitor list: " + ex.getMessage(), ex);
						return false;
					}
					return result != null && result;
				});
			}).thenCompose(Function.identity());
		});
	}

	private CompletableFuture<Boolean> sendAbandonedWarningIfNeeded(@NotNull UserData userData) {
		if (!userData.getHellblockData().hasHellblock())
			return CompletableFuture.completedFuture(false);

		final HellblockData data = userData.getHellblockData();
		final UUID uuid = userData.getUUID();
		final UUID ownerUUID = data.getOwnerUUID();

		// Skip if island is not owned
		if (ownerUUID == null)
			return CompletableFuture.completedFuture(false);

		// Asynchronously get the owner's UserData
		return getCachedUserDataWithFallback(ownerUUID, false).thenCompose(optData -> {
			// If owner data is not present, do nothing
			if (optData.isEmpty())
				return CompletableFuture.completedFuture(false);

			UserData ownerData = optData.get();
			HellblockData hellblockData = ownerData.getHellblockData();

			// Get party (including owner)
			Set<UUID> party = hellblockData.getPartyPlusOwner();

			// If user is not in the party, skip
			if (!party.contains(uuid))
				return CompletableFuture.completedFuture(false);

			if (!hellblockData.isAbandoned())
				return CompletableFuture.completedFuture(false);

			// If the island is abandoned, send message
			return instance.getScheduler().callSync(() -> {
				final Sender audience = instance.getSenderFactory().wrap(userData.getPlayer());
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LOGIN_ABANDONED.arguments(AdventureHelper
								.miniMessageToComponent(String.valueOf(instance.getConfigManager().abandonAfterDays())))
								.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(userData.getPlayer()),
						Sound.sound(Key.key("minecraft:entity.villager.no"), Sound.Source.PLAYER, 1, 1));
				return CompletableFuture.completedFuture(true);
			});
		}).exceptionally(ex -> {
			instance.getPluginLogger().warn("Error sending abandoned warning to player " + userData.getName(), ex);
			return false;
		});
	}

	/**
	 * Updates the island display name, bio, and entry messages if the player's name
	 * has changed.
	 *
	 * @param userData   The user data associated with the island.
	 * @param storedName The stored name previously associated with the island.
	 */
	private CompletableFuture<Boolean> updateIslandDisplayInfoIfNameChanged(@NotNull UserData userData,
			@NotNull String storedName) {
		if (!userData.getHellblockData().hasHellblock())
			return CompletableFuture.completedFuture(false);
		HellblockData data = userData.getHellblockData();

		// Only update if user is the actual owner
		if (data.getOwnerUUID() == null || !Objects.equals(data.getOwnerUUID(), userData.getUUID())) {
			return CompletableFuture.completedFuture(false);
		}

		DisplaySettings display = data.getDisplaySettings();
		AtomicBoolean changed = new AtomicBoolean(false);

		// Update bio if using default and name has changed
		if (display.isDefaultIslandBio() && !display.getIslandBio().contains(userData.getName())) {
			display.setIslandBio(data.getDefaultIslandBio());
			display.setAsDefaultIslandBio();
			instance.debug("Updated island bio for " + userData.getName() + " due to name change.");
			changed.set(true);
		}

		// Update name if using default and name has changed
		if (display.isDefaultIslandName() && !display.getIslandName().contains(userData.getName())) {
			display.setIslandName(data.getDefaultIslandName());
			display.setAsDefaultIslandName();
			instance.debug("Updated island name for " + userData.getName() + " due to name change.");
			changed.set(true);
		}

		// Update entry/farewell messages if the island is not abandoned and name
		// changed
		if (!data.isAbandoned() && !storedName.equalsIgnoreCase(userData.getName())) {
			Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
					.getWorld(instance.getWorldManager().getHellblockWorldFormat(data.getIslandId()));

			if (worldOpt.isPresent()) {
				HellblockWorld<?> world = worldOpt.get();

				return instance.getProtectionManager().getIslandProtection()
						.updateHellblockMessages(world, userData.getUUID()).thenApply(v -> {
							instance.debug(
									"Updated island entry messages for " + userData.getName() + " due to name change.");
							return true;
						}).exceptionally(ex -> {
							instance.getPluginLogger().warn("Failed to update entry messages for " + userData.getName(),
									ex);
							return changed.get(); // fallback to whether name/bio changed
						});
			}
		}

		// If no async message update was needed, just return what we changed
		// synchronously
		return CompletableFuture.completedFuture(changed.get());
	}

	private CompletableFuture<Boolean> notifyPendingInvitations(@NotNull UserData userData) {
		if (!userData.getNotificationSettings().hasInviteNotifications())
			return CompletableFuture.completedFuture(false);
		if (userData.getHellblockData().getInvitations().isEmpty())
			return CompletableFuture.completedFuture(false);

		Player player = userData.getPlayer();
		if (player == null || !player.isOnline())
			return CompletableFuture.completedFuture(false);
		final Sender audience = instance.getSenderFactory().wrap(player);
		final int invitationCount = userData.getHellblockData().getInvitations().size();

		CommandConfig<?> config = instance.getCommandManager().getCommandConfig(
				instance.getConfigManager().loadConfig(HellblockCommandManager.commandsFile), "coop_invites");

		List<String> usages = config.getUsages();

		if (usages.isEmpty()) {
			return CompletableFuture.failedFuture(
					new IllegalStateException("No usages defined for 'coop_invites' command in commands.yml"));
		}

		String command = usages.get(0);

		// Build the [HERE] button
		Component button = MessageConstants.BTN_HELLBLOCK_INVITE_REMINDER_HERE
				.clickEvent(ClickEvent.runCommand(command))
				.hoverEvent(HoverEvent.showText(MessageConstants.BTN_HELLBLOCK_INVITE_REMINDER_HOVER.build())).build();

		// Build the full message with injected args: count, button
		Component reminder = MessageConstants.MSG_HELLBLOCK_INVITE_REMINDER
				.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(invitationCount)), button).build();

		return instance.getScheduler().callSync(() -> {
			// Send the message
			audience.sendMessage(instance.getTranslationManager().render(reminder));

			// Play notification sound
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					Sound.sound(Key.key("minecraft:block.note_block.pling"), Sound.Source.PLAYER, 1, 1.2f));
			return CompletableFuture.completedFuture(true);
		});
	}

	private CompletableFuture<Boolean> notifyCoopMembersOnJoin(@NotNull UserData userData) {
		if (!userData.getNotificationSettings().hasJoinNotifications())
			return CompletableFuture.completedFuture(false);
		if (!userData.getHellblockData().hasHellblock())
			return CompletableFuture.completedFuture(false);

		UUID ownerUUID = userData.getHellblockData().getOwnerUUID();
		if (ownerUUID == null)
			return CompletableFuture.completedFuture(false);

		return getCachedUserDataWithFallback(ownerUUID, false).thenCompose(optData -> {
			if (optData.isEmpty()) {
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();
			final Set<UUID> party = ownerData.getHellblockData().getPartyPlusOwner();

			return instance.getScheduler().callSync(() -> {
				boolean notified = false;

				for (UUID id : party) {
					if (id.equals(userData.getUUID()))
						continue;

					Player member = Bukkit.getPlayer(id);
					if (member == null || !member.isOnline())
						continue;

					Sender sender = instance.getSenderFactory().wrap(member);
					sender.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_JOINED_SERVER
									.arguments(AdventureHelper.miniMessageToComponent(userData.getName())).build()));
					AdventureHelper.playSound(instance.getSenderFactory().getAudience(member),
							Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1, 1));
					notified = true;
				}

				return CompletableFuture.completedFuture(notified);
			});
		}).exceptionally(ex -> {
			instance.getPluginLogger()
					.warn("Error sending coop join notification for player " + userData.getName() + "'s login", ex);
			return false;
		});
	}

	public CompletableFuture<Boolean> startWorldHandlers(@NotNull UserData userData) {
		Player player = userData.getPlayer();
		if (player == null || !player.isOnline()) {
			return CompletableFuture.completedFuture(false);
		}

		// Start border task immediately if in correct world
		if (instance.getHellblockHandler().isInCorrectWorld(player)) {
			instance.getBorderHandler().startBorderTask(userData.getUUID());
		}

		// Collect futures
		CompletableFuture<Void> resolveFuture = instance.getIslandManager().resolveIslandId(player.getLocation())
				.thenAccept(optIslandId -> optIslandId
						.ifPresent(islandId -> instance.getIslandManager().handlePlayerEnterIsland(player, islandId)));

		CompletableFuture<Boolean> safetyFuture = instance.getHellblockHandler().ensureSafety(userData);
		CompletableFuture<Boolean> visitFuture = instance.getHellblockHandler().handleVisitingIsland(player, userData);

		// Combine all futures and return a void future
		return CompletableFuture.allOf(resolveFuture, safetyFuture, visitFuture).thenApply(v -> true);
	}

	private boolean handleNightVisionFromGear(@NotNull UserData userData) {
		Player player = userData.getPlayer();
		if (player == null || !player.isOnline())
			return false;
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return false;

		boolean hasGlowstoneArmor = Arrays.stream(player.getInventory().getArmorContents()).filter(Objects::nonNull)
				.anyMatch(item -> item.getType() != Material.AIR
						&& instance.getNetherArmorHandler().isNetherArmorEnabled(item)
						&& instance.getNetherArmorHandler().isNetherArmorNightVisionAllowed(item)
						&& instance.getNetherArmorHandler().checkNightVisionArmorStatus(item)
						&& instance.getNetherArmorHandler().getNightVisionArmorStatus(item));

		if (hasGlowstoneArmor) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			userData.isWearingGlowstoneArmor(true);
		}

		ItemStack tool = player.getInventory().getItemInMainHand();
		if (tool.getType() == Material.AIR) {
			tool = player.getInventory().getItemInOffHand();
		}

		if (tool.getType() != Material.AIR && instance.getNetherToolsHandler().isNetherToolEnabled(tool)
				&& instance.getNetherToolsHandler().isNetherToolNightVisionAllowed(tool)
				&& instance.getNetherToolsHandler().checkNightVisionToolStatus(tool)
				&& instance.getNetherToolsHandler().getNightVisionToolStatus(tool)) {

			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			userData.isHoldingGlowstoneTool(true);
		}

		return userData.hasGlowstoneArmorEffect() || userData.hasGlowstoneToolEffect();
	}

	private CompletableFuture<Boolean> updateLevelUpChallenge(@NotNull UserData userData) {
		if (!userData.getHellblockData().hasHellblock())
			return CompletableFuture.completedFuture(false);

		UUID ownerUUID = userData.getHellblockData().getOwnerUUID();
		if (ownerUUID == null)
			return CompletableFuture.completedFuture(false);

		return getCachedUserDataWithFallback(ownerUUID, false).thenCompose(optData -> {
			if (optData.isEmpty()) {
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();
			float currentLevel = ownerData.getHellblockData().getIslandLevel();

			boolean progressed = false;

			for (ChallengeType challenge : instance.getChallengeManager().getByActionType(ActionType.LEVELUP)) {
				if (userData.getChallengeData().isChallengeActive(challenge)
						&& challenge.getRequiredData() instanceof LevelUpRequirement req && req.isRelative() && userData
								.getChallengeData().getChallengeMeta(challenge, "startLevel", Double.class).isEmpty()) {

					// Record baseline if missing
					userData.getChallengeData().setChallengeMeta(challenge, "startLevel", currentLevel);

					instance.getChallengeManager().handleChallengeProgression(userData, ActionType.LEVELUP,
							currentLevel);
					progressed = true;
				}
			}

			return CompletableFuture.completedFuture(progressed);
		}).exceptionally(ex -> {
			instance.getPluginLogger().warn("Error updating level up challenge for player " + userData.getName(), ex);
			return false;
		});
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
			PlayerData data = gson.fromJson(json, PlayerData.class);

			if (data.getVersion() < PlayerData.CURRENT_VERSION) {
				// migratePlayerData(data, data.getVersion());
				data.setVersion(PlayerData.CURRENT_VERSION);
			}
			return data;
		} catch (JsonSyntaxException ex) {
			instance.getPluginLogger().severe("Failed to parse PlayerData from json.");
			instance.getPluginLogger().info("Json: %s".formatted(json));
			throw new RuntimeException(ex);
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

	public class RetryManager {

		private final RetryScheduler scheduler;
		private final int maxAttempts;
		private final long initialDelaySeconds;
		private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();
		private final Set<UUID> active = ConcurrentHashMap.newKeySet();

		public interface RetryTask {
			void run(UUID uuid, int attempt, Runnable retryCallback, Runnable cancelCallback);
		}

		public interface RetryScheduler {
			void schedule(Runnable task, long delaySeconds);
		}

		public RetryManager(RetryScheduler scheduler, int maxAttempts, long initialDelaySeconds) {
			this.scheduler = scheduler;
			this.maxAttempts = maxAttempts;
			this.initialDelaySeconds = initialDelaySeconds;
		}

		public void retry(UUID uuid, RetryTask task) {
			if (!active.add(uuid))
				return; // already retrying this UUID
			runRetry(uuid, task);
		}

		private void runRetry(UUID uuid, RetryTask task) {
			int attempt = attempts.getOrDefault(uuid, 1);

			// Exponential backoff with configurable initial delay
			long delay = (attempt == 1) ? initialDelaySeconds : Math.min(8, (long) Math.pow(2, attempt - 1));

			// Max attempts reached
			// Explicit cancel
			scheduler.schedule(() -> task.run(uuid, attempt, () -> {
				int next = attempt + 1;
				if (next > maxAttempts) {
					cancel(uuid);
				} else {
					attempts.put(uuid, next);
					runRetry(uuid, task);
				}
			}, () -> cancel(uuid)), delay);
		}

		public void cancel(UUID uuid) {
			attempts.remove(uuid);
			active.remove(uuid);
		}
	}
}