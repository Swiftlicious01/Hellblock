package com.swiftlicious.hellblock.database;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;
import com.swiftlicious.hellblock.player.PlayerData;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.XReadParams;

/**
 * A RedisManager class responsible for managing interactions with a Redis
 * server for data storage.
 */
public class RedisManager extends AbstractStorage {

	private static RedisManager instance;
	private static final String STREAM = "hellblock";
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
	public void initialize(YamlDocument config) {
		final Section section = config.getSection("Redis");
		if (section == null) {
			plugin.getPluginLogger().warn(
					"Failed to load database config. It seems that your config is broken. Please regenerate a new one.");
			return;
		}

		// Identify required dependencies for Redis
		Set<Dependency> redisDeps = EnumSet.of(Dependency.JEDIS, Dependency.COMMONS_POOL_2);

		plugin.getDependencyManager().runWithLoader(redisDeps, () -> {
			final JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
			jedisPoolConfig.setTestWhileIdle(true);
			jedisPoolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(30000));
			jedisPoolConfig.setNumTestsPerEvictionRun(-1);
			jedisPoolConfig.setMinEvictableIdleDuration(
					Duration.ofMillis(section.getInt("MinEvictableIdleTimeMillis", 1800000)));
			jedisPoolConfig.setMaxTotal(section.getInt("MaxTotal", 8));
			jedisPoolConfig.setMaxIdle(section.getInt("MaxIdle", 8));
			jedisPoolConfig.setMinIdle(section.getInt("MinIdle", 1));
			jedisPoolConfig.setMaxWait(Duration.ofMillis(section.getInt("MaxWaitMillis")));

			password = section.getString("password", "");
			port = section.getInt("port", 6379);
			host = section.getString("host", "localhost");
			useSSL = section.getBoolean("use-ssl", false);

			jedisPool = password.isBlank() ? new JedisPool(jedisPoolConfig, host, port, 0, useSSL)
					: new JedisPool(jedisPoolConfig, host, port, 0, password, useSSL);

			try (Jedis jedis = jedisPool.getResource()) {
				final String info = jedis.info();
				plugin.getPluginLogger().info("Redis server connected.");

				final String version = parseRedisVersion(info);
				if (isRedisNewerThan5(version)) {
					this.threadTask = new BlockingThreadTask();
					this.isNewerThan5 = true;
				} else {
					this.subscribe();
					this.isNewerThan5 = false;
				}
			} catch (JedisException ex) {
				plugin.getPluginLogger().warn("Failed to connect to Redis.", ex);
			}
			return null;
		});
	}

	/**
	 * Disable the Redis connection by closing the JedisPool.
	 */
	@Override
	public void disable() {
		if (threadTask != null) {
			threadTask.stop();
		}
		if (jedisPool != null && !jedisPool.isClosed()) {
			jedisPool.close();
		}
	}

	/**
	 * Send a message to Redis on a specified channel.
	 *
	 * @param message The message to send.
	 */
	public void publishRedisMessage(@NotNull String message) {
		if (isNewerThan5) {
			try (Jedis jedis = jedisPool.getResource()) {
				final Map<String, String> messages = new HashMap<>();
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
		final Thread thread = new Thread(() -> {
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
						try {
							handleMessage(message);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}, getStream());
			}
		});
		thread.start();
	}

	private void handleMessage(String message) throws IOException {
		final DataInputStream input = new DataInputStream(
				new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
		final String server = input.readUTF();
		if (!plugin.getConfigManager().serverGroup().equals(server)) {
			return;
		}
		final String type = input.readUTF();
		switch (type) {
		case "online" -> {
			plugin.getPlayerListener().updatePlayerCount(UUID.fromString(input.readUTF()), input.readInt());
		}
		}
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
		final var future = new CompletableFuture<Void>();
		plugin.getScheduler().async().execute(() -> {
			try (Jedis jedis = jedisPool.getResource()) {
				jedis.setex(getRedisKey("hb_server", uuid), 10, new byte[0]);
			}
			future.complete(null);
			plugin.debug("Server data set for %s".formatted(uuid));
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
		final var future = new CompletableFuture<Boolean>();
		plugin.getScheduler().async().execute(() -> {
			try (Jedis jedis = jedisPool.getResource()) {
				final byte[] key = getRedisKey("hb_server", uuid);
				if (jedis.get(key) != null) {
					jedis.del(key);
					future.complete(true);
					plugin.debug("Server data retrieved for %s; value: true".formatted(uuid));
				} else {
					future.complete(false);
					plugin.debug("Server data retrieved for %s; value: false".formatted(uuid));
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
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor) {
		final var future = new CompletableFuture<Optional<PlayerData>>();
		if (executor == null) {
			executor = plugin.getScheduler().async();
		}
		executor.execute(() -> {
			try (Jedis jedis = jedisPool.getResource()) {
				final byte[] key = getRedisKey("hb_data", uuid);
				final byte[] data = jedis.get(key);
				jedis.del(key);
				if (data != null) {
					final PlayerData playerData = plugin.getStorageManager().fromBytes(data);
					playerData.setUUID(uuid);
					plugin.debug("Redis data retrieved for %s; normal data".formatted(uuid));
				} else {
					future.complete(Optional.empty());
					plugin.debug("Redis data retrieved for %s; empty data".formatted(uuid));
				}
			} catch (Exception ex) {
				future.complete(Optional.empty());
				plugin.getPluginLogger().warn("Failed to get redis data for %s".formatted(uuid), ex);
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
		final var future = new CompletableFuture<Boolean>();
		plugin.getScheduler().async().execute(() -> {
			try (Jedis jedis = jedisPool.getResource()) {
				jedis.setex(getRedisKey("hb_data", uuid), 10, playerData.toBytes());
				future.complete(true);
				plugin.debug("Redis data set for %s".formatted(uuid));
			} catch (Exception e) {
				future.complete(false);
				plugin.getPluginLogger().warn("Failed to set redis data for player %s".formatted(uuid), e);
			}
		});
		return future;
	}

	/**
	 * Get a set of unique player UUIDs from Redis (Returns an empty set). This
	 * method is designed for importing and exporting so it would not actually be
	 * called.
	 *
	 * @return An empty set of UUIDs.
	 */
	@Override
	public Set<UUID> getUniqueUsers() {
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
		final String[] split = version.split("\\.");
		final int major = Integer.parseInt(split[0]);
		if (major < 7) {
			plugin.getPluginLogger()
					.warn("Detected that you are running an outdated Redis server. v%s.".formatted(version));
			plugin.getPluginLogger().warn("It's recommended to update to avoid security vulnerabilities!");
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
			final Thread thread = new Thread(() -> {
				final var map = new HashMap<String, StreamEntryID>();
				map.put(getStream(), StreamEntryID.XREAD_NEW_ENTRY);
				while (!this.stopped) {
					try (final var connection = getJedis()) {
						if (connection != null) {
							final var messages = connection.xread(XReadParams.xReadParams().count(1).block(2000), map);
							if (messages != null && !messages.isEmpty()) {
								messages.stream().filter(message -> message.getKey().equals(getStream()))
										.map(message -> message.getValue().get(0).getFields().get("value"))
										.forEach(value -> {
											try {
												handleMessage(value);
											} catch (IOException ex) {
												ex.printStackTrace();
											}
										});
							}
						} else {
							Thread.sleep(2000);
						}
					} catch (Exception ex) {
						plugin.getPluginLogger().warn("Failed to connect redis. Try reconnecting 10s later", ex);
						try {
							Thread.sleep(10000);
						} catch (InterruptedException stop) {
							this.stopped = true;
						}
					}
				}
			});
			thread.start();
		}
	}
}