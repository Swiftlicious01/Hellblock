package com.swiftlicious.hellblock.database;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.PlayerData;
import com.swiftlicious.hellblock.utils.LogUtils;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;

/**
 * A RedisManager class responsible for managing interactions with a Redis
 * server for data storage.
 */
public class RedisManager extends AbstractStorage {

	private static RedisManager instance;
	private final static String STREAM = "hellblock";
	private JedisPool jedisPool;
	private String password;
	private int port;
	private String host;
	private boolean useSSL;
	private BlockingThreadTask threadTask;
	private boolean isNewerThan5;

	public RedisManager(HellblockPlugin plugin) {
		super(plugin);
		instance = this;
	}

	/**
	 * Get the singleton instance of the RedisManager.
	 *
	 * @return The RedisManager instance.
	 */
	public static RedisManager getInstance() {
		return instance;
	}

	/**
	 * Get a Jedis resource for interacting with the Redis server.
	 *
	 * @return A Jedis resource.
	 */
	public Jedis getJedis() {
		return jedisPool.getResource();
	}

	/**
	 * Initialize the Redis connection and configuration based on the plugin's YAML
	 * configuration.
	 */
	@Override
	public void initialize() {
		YamlConfiguration config = HellblockPlugin.getInstance().getConfig("database.yml");
		ConfigurationSection section = config.getConfigurationSection("Redis");
		if (section == null) {
			LogUtils.warn(
					"Failed to load database config. It seems that your config is broken. Please regenerate a new one.");
			return;
		}

		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setTestWhileIdle(true);
		jedisPoolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(30000));
		jedisPoolConfig.setNumTestsPerEvictionRun(-1);
		jedisPoolConfig
				.setMinEvictableIdleDuration(Duration.ofMillis(section.getInt("MinEvictableIdleTimeMillis", 1800000)));
		jedisPoolConfig.setMaxTotal(section.getInt("MaxTotal", 8));
		jedisPoolConfig.setMaxIdle(section.getInt("MaxIdle", 8));
		jedisPoolConfig.setMinIdle(section.getInt("MinIdle", 1));
		jedisPoolConfig.setMaxWait(Duration.ofMillis(section.getInt("MaxWaitMillis")));

		password = section.getString("password", "");
		port = section.getInt("port", 6379);
		host = section.getString("host", "localhost");
		useSSL = section.getBoolean("use-ssl", false);

		if (password.isBlank()) {
			jedisPool = new JedisPool(jedisPoolConfig, host, port, 0, useSSL);
		} else {
			jedisPool = new JedisPool(jedisPoolConfig, host, port, 0, password, useSSL);
		}
		String info;
		try (Jedis jedis = jedisPool.getResource()) {
			info = jedis.info();
			LogUtils.info("Redis server connected.");
		} catch (JedisException e) {
			LogUtils.warn("Failed to connect redis.", e);
			return;
		}

		String version = parseRedisVersion(info);
		if (isRedisNewerThan5(version)) {
			// For Redis 5.0+
			this.threadTask = new BlockingThreadTask();
			this.isNewerThan5 = true;
		} else {
			// For Redis 2.0+
			this.subscribe();
			this.isNewerThan5 = false;
		}
	}

	/**
	 * Disable the Redis connection by closing the JedisPool.
	 */
	@Override
	public void disable() {
		if (threadTask != null)
			threadTask.stop();
		if (jedisPool != null && !jedisPool.isClosed())
			jedisPool.close();
	}

	/**
	 * Send a message to Redis on a specified channel.
	 *
	 * @param server  The Redis channel to send the message to.
	 * @param message The message to send.
	 */
	public void publishRedisMessage(@NotNull String server, @NotNull String message) {
		message = server + ";" + message;
		HellblockPlugin.getInstance().debug(String.format("Sent Redis message: %s", message));
		if (isNewerThan5) {
			try (Jedis jedis = jedisPool.getResource()) {
				HashMap<String, String> messages = new HashMap<>();
				messages.put("value", message);
				jedis.xadd(getStream(), StreamEntryID.NEW_ENTRY, messages);
			}
		} else {
			try (Jedis jedis = jedisPool.getResource()) {
				jedis.publish(getStream(), message);
			}
		}
	}

	/**
	 * Subscribe to Redis messages on a separate thread and handle received
	 * messages.
	 */
	private void subscribe() {
		Thread thread = new Thread(() -> {
			try (final Jedis jedis = password.isBlank() ? new Jedis(host, port, 0, useSSL)
					: new Jedis(host, port, DefaultJedisClientConfig.builder().password(password).timeoutMillis(0)
							.ssl(useSSL).build())) {
				jedis.connect();
				jedis.subscribe(new JedisPubSub() {
					@Override
					public void onMessage(String channel, String message) {
						if (!channel.equals(getStream())) {
							return;
						}
						handleMessage(message);
					}
				}, getStream());
			}
		});
		thread.start();
	}

	private void handleMessage(String message) {
		LogUtils.info(String.format("Received Redis message: %s", message));
		String[] split = message.split(";");
		String server = split[0];
		if (!"default".contains(server)) {
			return;
		}
		String action = split[1];
		HellblockPlugin.getInstance().getScheduler().runTaskSync(() -> {
			switch (action) {
			case "start" -> {
				//
			}
			case "end" -> {
				//
			}
			case "stop" -> {
				//
			}
			}
		}, new Location(HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld(), 0, 0, 0));
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.Redis;
	}

	/**
	 * Set a "change server" flag for a specified player UUID in Redis.
	 *
	 * @param uuid The UUID of the player.
	 * @return A CompletableFuture indicating the operation's completion.
	 */
	public CompletableFuture<Void> setChangeServer(UUID uuid) {
		var future = new CompletableFuture<Void>();
		HellblockPlugin.getInstance().getScheduler().runTaskAsync(() -> {
			try (Jedis jedis = jedisPool.getResource()) {
				jedis.setex(getRedisKey("hb_server", uuid), 10, new byte[0]);
			}
			future.complete(null);
			LogUtils.info(String.format("Server data set for %s", uuid));
		});
		return future;
	}

	/**
	 * Get the "change server" flag for a specified player UUID from Redis and
	 * remove it.
	 *
	 * @param uuid The UUID of the player.
	 * @return A CompletableFuture with a Boolean indicating whether the flag was
	 *         set.
	 */
	public CompletableFuture<Boolean> getChangeServer(UUID uuid) {
		var future = new CompletableFuture<Boolean>();
		HellblockPlugin.getInstance().getScheduler().runTaskAsync(() -> {
			try (Jedis jedis = jedisPool.getResource()) {
				byte[] key = getRedisKey("hb_server", uuid);
				if (jedis.get(key) != null) {
					jedis.del(key);
					future.complete(true);
					LogUtils.info(String.format("Server data retrieved for %s; value: true", uuid));
				} else {
					future.complete(false);
					LogUtils.info(String.format("Server data retrieved for %s; value: false", uuid));
				}
			}
		});
		return future;
	}

	/**
	 * Asynchronously retrieve player data from Redis.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock Flag indicating whether to lock the data.
	 * @return A CompletableFuture with an optional PlayerData.
	 */
	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock) {
		var future = new CompletableFuture<Optional<PlayerData>>();
		HellblockPlugin.getInstance().getScheduler().runTaskAsync(() -> {
			try (Jedis jedis = jedisPool.getResource()) {
				byte[] key = getRedisKey("hb_data", uuid);
				byte[] data = jedis.get(key);
				jedis.del(key);
				if (data != null) {
					future.complete(Optional.of(HellblockPlugin.getInstance().getStorageManager().fromBytes(data)));
					HellblockPlugin.getInstance()
							.debug(String.format("Redis data retrieved for %s; normal data", uuid));
				} else {
					future.complete(Optional.empty());
					HellblockPlugin.getInstance().debug(String.format("Redis data retrieved for %s; empty data", uuid));
				}
			} catch (Exception e) {
				future.complete(Optional.empty());
				LogUtils.warn(String.format("Failed to get redis data for %s", uuid), e);
			}
		});
		return future;
	}

	/**
	 * Asynchronously update player data in Redis.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player's data to update.
	 * @param ignore     Flag indicating whether to ignore the update (not used).
	 * @return A CompletableFuture indicating the update result.
	 */
	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean ignore) {
		var future = new CompletableFuture<Boolean>();
		HellblockPlugin.getInstance().getScheduler().runTaskAsync(() -> {
			try (Jedis jedis = jedisPool.getResource()) {
				jedis.setex(getRedisKey("hb_data", uuid), 10,
						HellblockPlugin.getInstance().getStorageManager().toBytes(playerData));
				future.complete(true);
				LogUtils.info(String.format("Redis data set for %s", uuid));
			} catch (Exception e) {
				future.complete(false);
				LogUtils.warn(String.format("Failed to set redis data for player %s", uuid), e);
			}
		});
		return future;
	}

	/**
	 * Get a set of unique player UUIDs from Redis (Returns an empty set). This
	 * method is designed for importing and exporting so it would not actually be
	 * called.
	 *
	 * @param legacy Flag indicating whether to retrieve legacy data (not used).
	 * @return An empty set of UUIDs.
	 */
	@Override
	public Set<UUID> getUniqueUsers(boolean legacy) {
		return new HashSet<>();
	}

	/**
	 * Generate a Redis key for a specified key and UUID.
	 *
	 * @param key  The key identifier.
	 * @param uuid The UUID to include in the key.
	 * @return A byte array representing the Redis key.
	 */
	private byte[] getRedisKey(String key, @NotNull UUID uuid) {
		return (key + ":" + uuid).getBytes(StandardCharsets.UTF_8);
	}

	public String getStream() {
		return STREAM;
	}

	private boolean isRedisNewerThan5(String version) {
		String[] split = version.split("\\.");
		int major = Integer.parseInt(split[0]);
		if (major < 7) {
			LogUtils.warn(String.format("Detected that you are running an outdated Redis server. v%s.", version));
			LogUtils.warn("It's recommended to update to avoid security vulnerabilities!");//
		}
		return major >= 5;
	}

	private String parseRedisVersion(String info) {
		for (String line : info.split("\n")) {
			if (line.startsWith("redis_version:")) {
				return line.split(":")[1];
			}
		}
		return "Unknown";
	}

	public class BlockingThreadTask {

		private boolean stopped;

		public void stop() {
			stopped = true;
		}

		public BlockingThreadTask() {
			Thread thread = new Thread(() -> {
				var map = new HashMap<String, StreamEntryID>();
				map.put(getStream(), StreamEntryID.XGROUP_LAST_ENTRY);
				while (!this.stopped) {
					try {
						var connection = getJedis();
						if (connection != null) {
							var messages = connection.xread(XReadParams.xReadParams().count(1).block(2000), map);
							connection.close();
							if (messages != null && messages.size() != 0) {
								for (Map.Entry<String, List<StreamEntry>> message : messages) {
									if (message.getKey().equals(getStream())) {
										var value = message.getValue().get(0).getFields().get("value");
										handleMessage(value);
									}
								}
							}
						} else {
							Thread.sleep(2000);
						}
					} catch (Exception e) {
						LogUtils.warn("Failed to connect redis. Try reconnecting 10s later.", e);
						try {
							Thread.sleep(10000);
						} catch (InterruptedException ex) {
							this.stopped = true;
						}
					}
				}
			});
			thread.start();
		}
	}
}
