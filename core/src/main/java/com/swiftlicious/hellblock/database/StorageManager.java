package com.swiftlicious.hellblock.database;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VisitManager.VisitRecord;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.UserDataInterface;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory;
import com.swiftlicious.hellblock.utils.adapters.ListSerializer;
import com.swiftlicious.hellblock.utils.adapters.MapSerializer;
import com.swiftlicious.hellblock.utils.adapters.SetSerializer;
import com.swiftlicious.hellblock.world.HellblockWorld;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;

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
	private final Set<UUID> locked = new HashSet<>();
	private boolean hasRedis;
	private RedisManager redisManager;
	private String serverID;
	private SchedulerTask timerSaveTask;
	private final Gson gson;

	public StorageManager(HellblockPlugin plugin) {
		instance = plugin;
		// Build the gson
		// excludeFieldsWithoutExposeAnnotation - this means that every field to be
		// stored should use @Expose
		// enableComplexMapKeySerialization - forces GSON to use TypeAdapters even for
		// Map keys
		final GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.enableComplexMapKeySerialization().setPrettyPrinting();
		// Register map, set & list serializers
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
		builder.registerTypeAdapter((new TypeToken<List<MailboxEntry>>() {
		}).getType(), new ListSerializer<>(MailboxEntry.class));
		builder.registerTypeAdapter((new TypeToken<List<VisitRecord>>() {
		}).getType(), new ListSerializer<>(VisitRecord.class));
		// Register adapter factory
		builder.registerTypeAdapterFactory(new HellblockTypeAdapterFactory());
		// Allow characters like < or > without escaping them
		builder.disableHtmlEscaping();
		gson = builder.create();
	}

	public void init() {
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
				if (this.onlineUserMap.values().isEmpty()) {
					return;
				}
				final long finalTime = System.currentTimeMillis();
				this.dataSource.updateManyPlayersData(this.onlineUserMap.values(),
						!instance.getConfigManager().lockData());
				if (instance.getConfigManager().logDataSaving()) {
					instance.getPluginLogger().info("Data Saved for online players. Took %sms."
							.formatted((System.currentTimeMillis() - finalTime)));
				}
			}, instance.getConfigManager().dataSaveInterval(), instance.getConfigManager().dataSaveInterval(),
					TimeUnit.SECONDS);
		}
	}

	/**
	 * Disables the storage manager and cleans up resources.
	 */
	@Override
	public void disable() {
		HandlerList.unregisterAll(this);
		if (this.dataSource != null && !onlineUserMap.isEmpty()) {
			this.dataSource.updateManyPlayersData(onlineUserMap.values(), true);
		}
		if (this.dataSource != null) {
			this.dataSource.disable();
		}
		if (this.redisManager != null) {
			this.redisManager.disable();
		}
		this.onlineUserMap.clear();
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
	public Optional<UserData> getOnlineUser(UUID uuid) {
		return Optional.ofNullable(onlineUserMap.get(uuid));
	}

	@NotNull
	@Override
	public Collection<UserData> getOnlineUsers() {
		return onlineUserMap.values();
	}

	@Override
	public CompletableFuture<Optional<UserData>> getOfflineUserData(UUID uuid, boolean lock) {
		final CompletableFuture<Optional<PlayerData>> optionalDataFuture = dataSource.getPlayerData(uuid, lock, null);
		return optionalDataFuture.thenCompose(optionalUser -> {
			if (optionalUser.isEmpty()) {
				return CompletableFuture.completedFuture(Optional.empty());
			}
			final PlayerData data = optionalUser.get();
			return CompletableFuture.completedFuture(Optional.of(UserDataInterface.builder().setData(data).build()));
		});
	}

	@Override
	public CompletableFuture<Boolean> saveUserData(UserData userData, boolean unlock) {
		return dataSource.updatePlayerData(userData.getUUID(), userData.toPlayerData(), unlock);
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
		if (player.hasPermission("hellblock.updates") && instance.isUpdateAvailable()) {
			instance.getSenderFactory().wrap(player).sendMessage(AdventureHelper.miniMessage(
					"<red>There is a new update available!: <dark_red><u>https://github.com/Swiftlicious01/Hellblock<!u>"));
		}
		if (!hasRedis) {
			waitForDataLockRelease(uuid, 1);
		} else {
			instance.getScheduler().asyncLater(() -> redisManager.getChangeServer(uuid).thenAccept(changeServer -> {
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
		final Player player = event.getPlayer();
		final UUID uuid = player.getUniqueId();
		if (locked.contains(uuid)) {
			return;
		}

		instance.getProtectionManager().cancelBlockScan(uuid);

		instance.getSchematicManager().schematicPaster.cancelPaste(uuid);

		instance.getIslandLevelManager().saveCache(uuid);
		instance.getNetherrackGeneratorHandler().savePistons(uuid);

		final UserData onlineUser = onlineUserMap.remove(uuid);
		final PlayerData data = onlineUser.toPlayerData();

		instance.getBorderHandler().stopBorderTask(uuid);
		onlineUser.stopSpawningAnimals();
		onlineUser.stopSpawningFortressMobs();

		// Check if this player is an island owner
		instance.getCoopManager().getIslandOwner(uuid).thenAccept(optionalOwner -> {
			if (optionalOwner.isPresent()) {
				final UUID ownerId = optionalOwner.get();

				// Only snapshot for the owner (even if a coop member logs out)
				// small delay to let logout finish
				instance.getScheduler().asyncLater(() -> instance.getIslandBackupManager().maybeSnapshot(ownerId), 2,
						TimeUnit.SECONDS);
			}
		});

		if ((onlineUser.hasGlowstoneToolEffect() || onlineUser.hasGlowstoneArmorEffect())
				&& player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			onlineUser.isHoldingGlowstoneTool(false);
			onlineUser.isWearingGlowstoneArmor(false);
		}
		if (instance.getPlayerListener().getCancellablePortalMap().containsKey(uuid)
				&& instance.getPlayerListener().getCancellablePortalMap().get(uuid) != null) {
			instance.getPlayerListener().getCancellablePortalMap().get(uuid).cancel();
			instance.getPlayerListener().getCancellablePortalMap().remove(uuid);
		}
		if (instance.getPlayerListener().getLinkPortalCatcherSet().contains(uuid)) {
			instance.getPlayerListener().getLinkPortalCatcherSet().remove(uuid);
		}
		// Cleanup
		instance.getNetherrackGeneratorHandler().getGeneratorManager().cleanupExpiredPistons(uuid);
		instance.getNetherrackGeneratorHandler().getGeneratorManager().cleanupExpiredLocations();

		if (hasRedis) {
			redisManager.setChangeServer(uuid).thenRun(() -> redisManager.updatePlayerData(uuid, data, true)
					.thenRun(() -> dataSource.updatePlayerData(uuid, data, true).thenAccept(result -> {
						if (result) {
							locked.remove(uuid);
						}
					})));
		} else {
			dataSource.updatePlayerData(uuid, data, true).thenAccept(result -> {
				if (result) {
					locked.remove(uuid);
				}
			});
		}
	}

	/**
	 * Runnable task for asynchronously retrieving data from Redis. Retries up to 6
	 * times and cancels the task if the player is offline.
	 */
	public final class RedisGetDataTask implements Runnable {

		private final UUID uuid;
		private int triedTimes;
		private final SchedulerTask task;

		public RedisGetDataTask(UUID uuid) {
			this.uuid = uuid;
			this.task = instance.getScheduler().asyncRepeating(this, 0, 333, TimeUnit.MILLISECONDS);
		}

		@Override
		public void run() {
			triedTimes++;
			final Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline()) {
				// offline
				task.cancel();
				return;
			}
			if (triedTimes >= 6) {
				waitForDataLockRelease(uuid, 3);
				task.cancel();
				return;
			}
			redisManager.getPlayerData(uuid, false, null).thenAccept(optionalData -> {
				if (optionalData.isPresent()) {
					putDataInCache(player, optionalData.get());
					task.cancel();
					dataSource.lockOrUnlockPlayerData(uuid, instance.getConfigManager().lockData());
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
		instance.getScheduler().asyncLater(() -> {
			final var player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline()) {
				return;
			}
			if (times > 3) {
				instance.getPluginLogger().warn("Tried 3 times when getting data for %s. Giving up.".formatted(uuid));
				return;
			}
			this.dataSource.getPlayerData(uuid, instance.getConfigManager().lockData(), null)
					.thenAccept(optionalData -> {
						// Data should not be empty
						if (optionalData.isEmpty()) {
							instance.getPluginLogger().severe("Unexpected error: Data is null.");
							return;
						}

						if (optionalData.get().isLocked()) {
							waitForDataLockRelease(uuid, times + 1);
						} else {
							try {
								putDataInCache(player, optionalData.get());
							} catch (Exception ex) {
								ex.printStackTrace();
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
		// updates the player's name if changed
		String storedName = UserDataInterface.builder().setData(playerData).build().getName();
		final var bukkitUser = UserDataInterface.builder().setData(playerData).setName(player.getName())
				.updateLastActivity().build();
		onlineUserMap.put(player.getUniqueId(), bukkitUser);
		final Sender audience = instance.getSenderFactory().wrap(player);
		if (bukkitUser.getHellblockData().isAbandoned()) {
			audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LOGIN_ABANDONED
					.arguments(
							AdventureHelper.miniMessage(String.valueOf(instance.getConfigManager().abandonAfterDays())))
					.build()));
		}

		// If the stored bio, name and entry messages references an old name
		// placeholder,
		// regenerate it
		if (bukkitUser.getHellblockData().getOwnerUUID() != null
				&& bukkitUser.getHellblockData().getOwnerUUID().equals(bukkitUser.getUUID())) {
			String bio = bukkitUser.getHellblockData().getDisplaySettings().getIslandBio();
			if (bukkitUser.getHellblockData().getDisplaySettings().isDefaultIslandBio()
					&& !bio.contains(player.getName())) {
				bukkitUser.getHellblockData().getDisplaySettings()
						.setIslandBio(bukkitUser.getHellblockData().getDefaultIslandBio());
				bukkitUser.getHellblockData().getDisplaySettings().setAsDefaultIslandBio();
				instance.debug("Updated island bio for " + player.getName() + " due to name change.");
			}
			String name = bukkitUser.getHellblockData().getDisplaySettings().getIslandName();
			if (bukkitUser.getHellblockData().getDisplaySettings().isDefaultIslandName()
					&& !name.contains(player.getName())) {
				bukkitUser.getHellblockData().getDisplaySettings()
						.setIslandName(bukkitUser.getHellblockData().getDefaultIslandName());
				bukkitUser.getHellblockData().getDisplaySettings().setAsDefaultIslandName();
				instance.debug("Updated island name for " + player.getName() + " due to name change.");
			}
			if (!bukkitUser.getHellblockData().isAbandoned()) {
				String currentName = player.getName();
				if (!currentName.equalsIgnoreCase(storedName)) {
					Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(
							instance.getWorldManager().getHellblockWorldFormat(bukkitUser.getHellblockData().getID()));
					world.ifPresent(value -> {
						instance.getProtectionManager().getIslandProtection()
								.updateHellblockMessages(value.bukkitWorld(), bukkitUser.getUUID());
						instance.debug(
								"Updated island entry messages for " + player.getName() + " due to name change.");
					});
				}
			}
		}

		if (bukkitUser.toPlayerData().hasHellblockInviteNotifications()
				&& !bukkitUser.getHellblockData().getInvitations().isEmpty()) {
			int invitations = bukkitUser.getHellblockData().getInvitations().size();
			audience.sendMessage(instance.getTranslationManager().render(
					MessageConstants.MSG_HELLBLOCK_INVITE_REMINDER.arguments(Component.text(invitations)).build()));
		}

		if (bukkitUser.toPlayerData().hasHellblockJoinNotifications() && bukkitUser.getHellblockData().hasHellblock()) {
			UUID ownerUUID = bukkitUser.getHellblockData().getOwnerUUID();
			if (ownerUUID != null) {
				getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
						.thenAccept(ownerOpt -> ownerOpt.ifPresent(ownerData -> {
							Set<UUID> partyPlusOwner = ownerData.getHellblockData().getPartyPlusOwner();
							partyPlusOwner.stream().filter(id -> !id.equals(player.getUniqueId()))
									.map(Bukkit::getPlayer).filter(member -> member != null && member.isOnline())
									.forEach(member -> {
										Sender sender = instance.getSenderFactory().wrap(member);
										sender.sendMessage(instance.getTranslationManager()
												.render(MessageConstants.MSG_HELLBLOCK_COOP_JOINED_SERVER
														.arguments(Component.text(bukkitUser.getName())).build()));
									});
						}));
			}
		}

		instance.getMailboxManager().handleLogin(player);

		if (instance.getHellblockHandler().isInCorrectWorld(player)) {
			instance.getBorderHandler().startBorderTask(player);
		}
		bukkitUser.startSpawningAnimals();
		bukkitUser.startSpawningFortressMobs();
		instance.getFarmingManager().updateCrops(player.getWorld(), player);
		instance.getIslandLevelManager().loadCache(player.getUniqueId());
		instance.getNetherrackGeneratorHandler().loadPistons(player.getUniqueId());

		instance.getHellblockHandler().ensureSafety(player, bukkitUser);

		instance.getHellblockHandler().handleVisitingIsland(player, bukkitUser);

		final ItemStack[] armorSet = player.getInventory().getArmorContents();
		boolean checkArmor = false;
		if (armorSet != null) {
			for (ItemStack item : armorSet) {
				if (item == null || item.getType() == Material.AIR) {
					continue;
				}
				if (!instance.getNetherArmorHandler().isNetherArmorEnabled(item)) {
					continue;
				}
				if (!instance.getNetherArmorHandler().isNetherArmorNightVisionAllowed(item)) {
					continue;
				}
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

		ItemStack tool = player.getInventory().getItemInMainHand();
		if (tool.getType() == Material.AIR) {
			tool = player.getInventory().getItemInOffHand();
			if (tool.getType() == Material.AIR) {
				return;
			}
		}

		if (instance.getNetherToolsHandler().isNetherToolEnabled(tool)
				&& instance.getNetherToolsHandler().isNetherToolNightVisionAllowed(tool)
				&& instance.getNetherToolsHandler().checkNightVisionToolStatus(tool)
				&& instance.getNetherToolsHandler().getNightVisionToolStatus(tool)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			bukkitUser.isHoldingGlowstoneTool(true);
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
}