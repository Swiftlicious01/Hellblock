package com.swiftlicious.hellblock.database;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * An implementation of AbstractSQLDatabase that uses the SQLite database for
 * player data storage.
 */
public class SQLiteHandler extends AbstractSQLDatabase {

	private Connection connection;
	private File databaseFile;
	private Constructor<?> connectionConstructor;
	private ScheduledExecutorService executorService;

	private final Set<UUID> forceUnlocking = ConcurrentHashMap.newKeySet();
	private final ConcurrentHashMap<UUID, CompletableFuture<Optional<PlayerData>>> loadingCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> updatingCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> upsertCache = new ConcurrentHashMap<>();

	private final ConcurrentMap<UUID, Long> justInsertedTimestamps = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, CompletableFuture<Void>> pendingInserts = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, Object> lockGuards = new ConcurrentHashMap<>();

	private final Map<UUID, Long> lastAttemptTime = new ConcurrentHashMap<>();
	private static final long MIN_DELAY_BETWEEN_ATTEMPTS = 5000L; // 5 sec

	private final Cache<Integer, UUID> islandIdToUUIDCache = Caffeine.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build();

	public SQLiteHandler(HellblockPlugin plugin) {
		super(plugin);
	}

	/**
	 * Initialize the SQLite database and connection based on the configuration.
	 */
	@Override
	public void initialize(@NotNull YamlDocument config) {
		final ClassLoader classLoader = plugin.getDependencyManager().obtainClassLoaderWith(
				EnumSet.of(Dependency.SQLITE_DRIVER, Dependency.SLF4J_SIMPLE, Dependency.SLF4J_API));
		try {
			final Class<?> connectionClass = classLoader.loadClass("org.sqlite.jdbc4.JDBC4Connection");
			connectionConstructor = connectionClass.getConstructor(String.class, String.class, Properties.class);
		} catch (ReflectiveOperationException ex) {
			plugin.getPluginLogger().severe("Failed to initialize SQLite connection class via reflection.", ex);
			throw new RuntimeException("Failed to load SQLite JDBC connection class.", ex);
		}

		if (executorService == null || executorService.isShutdown()) {
			this.executorService = Executors
					.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("hb-sqlite-%d").build());
		}

		this.databaseFile = new File(plugin.getDataFolder(), config.getString("SQLite.file", "data") + ".db");
		super.tablePrefix = config.getString("SQLite.table-prefix", "hellblock");

		try {
			super.createTableIfNotExist();
			plugin.getPluginLogger().info("Initialized SQLite storage at " + databaseFile.getAbsolutePath());
		} catch (Exception ex) {
			plugin.getPluginLogger().severe("Failed to initialize SQLite schema.", ex);
			throw new RuntimeException("Could not create SQLite schema.", ex);
		}
	}

	/**
	 * Disable the SQLite database by closing the connection.
	 */
	@Override
	public void disable() {
		if (executorService != null) {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt(); // restore interrupt flag
			}
		}

		if (connection != null) {
			try {
				if (!connection.isClosed()) {
					connection.close();
				}
			} catch (SQLException e) {
				// Prefer logging over throwing during shutdown
				plugin.getPluginLogger().severe("Failed to close database connection during shutdown.", e);
			}
		}
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.SQLite;
	}

	/**
	 * Get a connection to the SQLite database.
	 *
	 * @return A database connection.
	 * @throws SQLException If there is an error establishing a connection.
	 */
	@NotNull
	@Override
	public Connection getConnection() throws SQLException {
		if (connection != null && !connection.isClosed()) {
			return connection;
		}
		try {
			final var properties = new Properties();
			properties.setProperty("foreign_keys", Boolean.toString(true));
			properties.setProperty("encoding", "'UTF-8'");
			properties.setProperty("synchronous", "FULL");
			connection = (Connection) this.connectionConstructor.newInstance("jdbc:sqlite:" + databaseFile.toString(),
					databaseFile.toString(), properties);
			// Recommended PRAGMA settings for concurrency safety
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("PRAGMA busy_timeout = 3000");
				stmt.execute("PRAGMA journal_mode = WAL"); // Optional: improves concurrency in modern SQLite
			}
			return connection;
		} catch (ReflectiveOperationException ex) {
			if (ex.getCause() instanceof SQLException) {
				throw (SQLException) ex.getCause();
			}
			throw new RuntimeException("Failed to establish SQLite connection.", ex);
		}
	}

	/**
	 * Asynchronously retrieve player data from the SQLite database.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock Flag indicating whether to lock the data.
	 * @return A CompletableFuture with an optional PlayerData.
	 */
	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor) {
		final Executor finalExecutor = executor != null ? executor : this.executorService;

		CompletableFuture<Void> insertFuture = pendingInserts.get(uuid);
		if (insertFuture != null && !insertFuture.isDone()) {
			plugin.debug("Insert in progress for " + uuid + ", waiting for completion...");

			CompletableFuture<Optional<PlayerData>> existing = loadingCache.get(uuid);
			if (existing != null) {
				return existing;
			}

			CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();

			insertFuture.whenComplete((ignored, error) -> {
				if (error != null) {
					future.completeExceptionally(error);
					return;
				}

				plugin.getScheduler().async().execute(() -> {
					CompletableFuture<Optional<PlayerData>> restarted = getPlayerData(uuid, lock, executor);
					restarted.whenComplete((result, ex) -> {
						if (ex != null)
							future.completeExceptionally(ex);
						else
							future.complete(result);
					});
				});
			});

			return future;
		}

		return loadingCache.computeIfAbsent(uuid, id -> {
			plugin.debug("getPlayerData: starting new load for " + id);
			final CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();
			final int maxRetries = 5;
			final long retryDelayMs = 250;
			AtomicInteger attempt = new AtomicInteger(0);

			plugin.getScheduler().async().execute(() -> {
				long now = System.currentTimeMillis();
				long last = lastAttemptTime.getOrDefault(uuid, 0L);
				if (now - last < MIN_DELAY_BETWEEN_ATTEMPTS) {
					long delayRemaining = MIN_DELAY_BETWEEN_ATTEMPTS - (now - last);
					plugin.debug("Too soon to retry loading " + uuid + ", skipping for " + delayRemaining + " ms");
					future.complete(Optional.empty());
					return;
				}
				lastAttemptTime.put(uuid, now);
			});

			future.whenComplete((res, ex) -> plugin.getScheduler().asyncLater(() -> loadingCache.remove(uuid), 5,
					TimeUnit.SECONDS));

			Runnable loadTask = new Runnable() {
				@Override
				public void run() {
					finalExecutor.execute(() -> {
						try {
							// Step 1: Wait if insert is still ongoing
							CompletableFuture<Void> insertWait = pendingInserts.get(uuid);
							if (insertWait != null) {
								plugin.debug("Waiting for insert to complete for " + uuid);
								insertWait.join(); // safe here
							}

							// Step 2: Check if a recent insert *just finished*
							Long insertedAt = justInsertedTimestamps.get(uuid);
							boolean stillInserting = pendingInserts.containsKey(uuid);
							long now = System.currentTimeMillis();

							try (Connection connection = getConnection();
									PreparedStatement statement = connection.prepareStatement(
											SqlConstants.SQL_SELECT_BY_UUID.formatted(getTableName("data")))) {

								statement.setString(1, uuid.toString());
								ResultSet rs = statement.executeQuery();

								if (rs.next()) {
									int lockValue = rs.getInt("lock");

									if (lock) {
										Object guard = lockGuards.computeIfAbsent(uuid, __ -> new Object());
										synchronized (guard) {
											if (lockValue != 0 && getCurrentSeconds() - 30 <= lockValue) {
												int currentAttempt = attempt.incrementAndGet();

												if (currentAttempt >= maxRetries) {
													plugin.getPluginLogger()
															.warn("Max retries reached for %s. Forcing unlock."
																	.formatted(uuid));
													try {
														if (forceUnlocking.add(uuid)) {
															try {
																lockOrUnlockPlayerData(uuid, false);
																plugin.getPluginLogger().warn(
																		"Forced unlock of %s's data.".formatted(uuid));
															} finally {
																forceUnlocking.remove(uuid);
															}
														}
													} catch (Exception ex) {
														plugin.getPluginLogger().warn(
																"Failed to force unlock %s's data.".formatted(uuid),
																ex);
													}

													executorService
															.schedule(() -> getPlayerData(uuid, lock, finalExecutor)
																	.whenComplete((result, throwable) -> {
																		if (throwable != null) {
																			future.completeExceptionally(throwable);
																		} else {
																			future.complete(result);
																		}
																	}), retryDelayMs * 2, TimeUnit.MILLISECONDS);
													return;
												}

												plugin.getPluginLogger()
														.warn("Player %s's data is locked. Retrying (%d/%d)..."
																.formatted(uuid, currentAttempt, maxRetries));
												executorService.schedule(this, retryDelayMs, TimeUnit.MILLISECONDS);
												return;
											}

											// Combined logic: skip locking if we're still inserting OR it's a recent
											// insert
											if (!stillInserting && (insertedAt == null || now - insertedAt > 5000)) {
												plugin.debug("Locking player data for " + uuid);
												lockOrUnlockPlayerData(uuid, true);
											} else {
												plugin.debug("Skipping lock — insert still recent or ongoing for "
														+ uuid + " (age: "
														+ (now - (insertedAt != null ? insertedAt : now)) + " ms)");
												future.complete(Optional.empty());
												return;
											}

											lockGuards.remove(uuid);
										}
									}

									byte[] dataBytes = rs.getBytes("data");
									final PlayerData data = plugin.getStorageManager().fromBytes(dataBytes);
									data.setUUID(uuid);

									int islandId = data.getHellblockData().getIslandId();
									if (islandId > 0) {
										islandIdToUUIDCache.put(islandId, uuid);
									}

									future.complete(Optional.of(data));
									return;
								}

								if (Bukkit.getPlayer(uuid) != null) {
									PlayerData data = PlayerData.empty();
									data.setUUID(uuid);

									int islandId = data.getHellblockData().getIslandId();
									if (islandId > 0) {
										islandIdToUUIDCache.put(islandId, uuid);
									}

									// Async insert and wait before completing
									insertPlayerData(uuid, data, lock, connection, true).thenRun(() -> {
										plugin.debug("Insert complete for " + uuid + ", finishing getPlayerData");
										future.complete(Optional.of(data));
									}).exceptionally(ex -> {
										plugin.getPluginLogger().warn("Insert failed during getPlayerData for " + uuid,
												ex);
										future.completeExceptionally(ex);
										return null;
									});
									return;
								} else {
									future.complete(Optional.empty());
								}

							}
						} catch (SQLException ex) {
							plugin.getPluginLogger().warn("Failed to get %s's data.".formatted(uuid), ex);
							future.completeExceptionally(ex);
						}
					});
				}
			};

			loadTask.run();
			return future;
		});
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerDataByIslandId(int islandId, boolean lock,
			Executor executor) {
		final Executor finalExecutor = executor != null ? executor : this.executorService;
		final CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();

		UUID cachedUUID = islandIdToUUIDCache.getIfPresent(islandId);
		if (cachedUUID != null) {
			return getPlayerData(cachedUUID, lock, finalExecutor);
		}

		Runnable scanTask = () -> finalExecutor.execute(() -> {
			try (Connection connection = getConnection();
					PreparedStatement statement = connection
							.prepareStatement(SqlConstants.SQL_SELECT_ALL_UUID.formatted(getTableName("data")));
					ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					String uuidStr = rs.getString("uuid");
					UUID uuid;
					try {
						uuid = UUID.fromString(uuidStr);
					} catch (IllegalArgumentException e) {
						continue;
					}
					byte[] dataBytes = rs.getBytes("data");
					PlayerData parsed;
					try {
						parsed = plugin.getStorageManager().fromBytes(dataBytes);
					} catch (Exception e) {
						plugin.getPluginLogger().warn("Failed to parse SQL blob for UUID " + uuid, e);
						continue;
					}
					if (parsed != null && parsed.getHellblockData() != null
							&& parsed.getHellblockData().getIslandId() == islandId) {
						// Cache for future lookup
						islandIdToUUIDCache.put(islandId, uuid);
						// Delegate to UUID-based load (this handles locking/retry)
						getPlayerData(uuid, lock, finalExecutor).whenComplete((result, throwable) -> {
							if (throwable != null) {
								future.completeExceptionally(throwable);
							} else {
								future.complete(result);
							}
						});
						return;
					}
				}
				// Not found
				future.complete(Optional.empty());
			} catch (SQLException ex) {
				plugin.getPluginLogger().warn("Failed to scan SQL blobs for islandId=" + islandId, ex);
				future.completeExceptionally(ex);
			}
		});

		scanTask.run();
		return future;
	}

	/**
	 * Asynchronously update or insert player data in the SQLite database.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player's data to update.
	 * @param unlock     Flag indicating whether to unlock the data.
	 * @return A CompletableFuture indicating the update/insert result.
	 */
	@Override
	public CompletableFuture<Boolean> updateOrInsertPlayerData(UUID uuid, PlayerData playerData, boolean unlock) {
		return upsertCache.computeIfAbsent(uuid, id -> {
			final CompletableFuture<Boolean> future = new CompletableFuture<>();
			final int maxRetries = 3;
			final long retryDelayMs = 500;

			// Remove from cache only after future completes
			future.whenComplete((result, throwable) -> plugin.getScheduler().asyncLater(() -> upsertCache.remove(uuid),
					5, TimeUnit.SECONDS));

			class UpsertRunnable implements Runnable {
				int attempt = 1;

				@Override
				public void run() {
					executorService.execute(() -> {
						try (Connection connection = getConnection()) {
							boolean exists;
							try (PreparedStatement check = connection.prepareStatement(
									SqlConstants.SQL_SELECT_BY_UUID.formatted(getTableName("data")))) {
								check.setString(1, uuid.toString());
								exists = check.executeQuery().next();
							}

							if (exists) {
								try (PreparedStatement update = connection.prepareStatement(
										SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data")))) {
									update.setInt(1, unlock ? 0 : getCurrentSeconds());
									update.setBytes(2, plugin.getStorageManager().toBytes(playerData));
									update.setString(3, uuid.toString());
									update.executeUpdate();
								}
							} else {
								plugin.debug("Performing insert fallback via upsert for " + uuid);
								insertPlayerData(uuid, playerData, !unlock, connection, false);
							}

							future.complete(true);
						} catch (SQLException ex) {
							plugin.getPluginLogger().warn(
									"Upsert failed for %s (attempt %d/%d)".formatted(uuid, attempt, maxRetries), ex);
							if (attempt++ < maxRetries) {
								executorService.schedule(this, retryDelayMs, TimeUnit.MILLISECONDS);
							} else {
								future.completeExceptionally(ex);
							}
						}
					});
				}
			}

			new UpsertRunnable().run();
			return future;
		});
	}

	/**
	 * Asynchronously update player data in the SQLite database.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player's data to update.
	 * @param unlock     Flag indicating whether to unlock the data.
	 * @return A CompletableFuture indicating the update result.
	 */
	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean unlock) {
		return updatingCache.computeIfAbsent(uuid, id -> {
			final CompletableFuture<Boolean> future = new CompletableFuture<>();
			final int maxRetries = 3;
			final long retryDelayMs = 500;

			// Ensure cleanup happens only once after complete
			future.whenComplete((result, throwable) -> plugin.getScheduler()
					.asyncLater(() -> updatingCache.remove(uuid), 5, TimeUnit.SECONDS));

			class UpdateRunnable implements Runnable {
				int attempt = 1;

				@Override
				public void run() {
					executorService.execute(() -> {
						try (Connection connection = getConnection();
								PreparedStatement statement = connection.prepareStatement(
										SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data")))) {

							statement.setInt(1, unlock ? 0 : getCurrentSeconds());
							statement.setBytes(2, playerData.toBytes());
							statement.setString(3, uuid.toString());

							int affected = statement.executeUpdate();
							if (affected == 0) {
								plugin.debug("No rows updated for %s; attempting insert.".formatted(uuid));
								insertPlayerData(uuid, playerData, !unlock, connection, false);
							}

							future.complete(true);
						} catch (SQLException ex) {
							plugin.getPluginLogger().warn(
									"Failed to update %s's data (attempt %d/%d)".formatted(uuid, attempt, maxRetries),
									ex);
							if (attempt++ < maxRetries) {
								executorService.schedule(this, retryDelayMs, TimeUnit.MILLISECONDS);
							} else {
								future.completeExceptionally(ex);
							}
						}
					});
				}
			}

			new UpdateRunnable().run();
			return future;
		});
	}

	/**
	 * Asynchronously update data for multiple players in the SQLite database.
	 *
	 * @param users  A collection of User instances to update.
	 * @param unlock Flag indicating whether to unlock the data.
	 */
	@Override
	public CompletableFuture<Boolean> updateManyPlayersData(Collection<? extends UserData> users, boolean unlock) {
		final CompletableFuture<Boolean> future = new CompletableFuture<>();

		if (users == null || users.isEmpty()) {
			future.complete(false);
			return future;
		}

		final List<UserData> userList = new ArrayList<>(users);
		final int maxRetries = 3;
		final long retryDelayMs = 500;
		final AtomicInteger attempt = new AtomicInteger(1);

		Runnable task = new Runnable() {
			@Override
			public void run() {
				executorService.execute(() -> {
					try (Connection connection = getConnection()) {
						connection.setAutoCommit(false);
						final String sql = SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data"));

						try (PreparedStatement statement = connection.prepareStatement(sql)) {
							for (UserData user : userList) {
								statement.setInt(1, unlock ? 0 : getCurrentSeconds());
								statement.setBytes(2, plugin.getStorageManager().toBytes(user.toPlayerData()));
								statement.setString(3, user.getUUID().toString());
								statement.addBatch();
							}

							statement.executeBatch();
							connection.commit();
							future.complete(true);

						} catch (SQLException ex) {
							connection.rollback();
							plugin.getPluginLogger().warn("Batch update failed (attempt %d/%d). Retrying..."
									.formatted(attempt.get(), maxRetries), ex);

							if (attempt.incrementAndGet() <= maxRetries) {
								executorService.schedule(this, retryDelayMs, TimeUnit.MILLISECONDS);
							} else {
								plugin.getPluginLogger().severe("Batch update failed after max retries.");
								future.completeExceptionally(ex);
							}
						}
					} catch (SQLException ex) {
						plugin.getPluginLogger().warn(
								"Batch update connection failure (attempt %d/%d).".formatted(attempt.get(), maxRetries),
								ex);

						if (attempt.incrementAndGet() <= maxRetries) {
							executorService.schedule(this, retryDelayMs, TimeUnit.MILLISECONDS);
						} else {
							plugin.getPluginLogger().severe("Batch update connection failed after max retries.");
							future.completeExceptionally(ex);
						}
					}
				});
			}
		};

		task.run();
		return future;
	}

	protected CompletableFuture<Void> insertPlayerData(UUID uuid, PlayerData playerData, boolean lock,
			@Nullable Connection previous) {
		return insertPlayerData(uuid, playerData, lock, previous, false);
	}

	/**
	 * Insert player data into the SQLite database.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player's data to insert.
	 * @param lock       Flag indicating whether to lock the data.
	 */
	@Override
	protected CompletableFuture<Void> insertPlayerData(UUID uuid, PlayerData playerData, boolean lock,
			@Nullable Connection previous, boolean trackInsert) {
		CompletableFuture<Void> insertFuture = null;

		// Only track insert if caller explicitly enables it (e.g. from getPlayerData)
		if (trackInsert) {
			insertFuture = new CompletableFuture<>();
			pendingInserts.put(uuid, insertFuture);
			justInsertedTimestamps.putIfAbsent(uuid, System.currentTimeMillis());
		}

		try (Connection connection = previous == null ? getConnection() : previous;
				PreparedStatement statement = connection
						.prepareStatement(SqlConstants.SQL_INSERT_DATA_BY_UUID.formatted(getTableName("data")))) {

			statement.setString(1, uuid.toString());
			statement.setInt(2, lock ? getCurrentSeconds() : 0);
			statement.setBytes(3, plugin.getStorageManager().toBytes(playerData));
			statement.execute();

			plugin.debug("Inserted data for player %s (lock=%s, trackInsert=%s)".formatted(uuid, lock, trackInsert));

			if (trackInsert && insertFuture != null) {
				insertFuture.complete(null);
			}

		} catch (SQLException ex) {
			if (trackInsert && insertFuture != null) {
				insertFuture.completeExceptionally(ex);
			}
			plugin.getPluginLogger().warn("Failed to insert data for %s".formatted(uuid), ex);
			throw new RuntimeException("Insert failed for " + uuid, ex);
		} finally {
			if (trackInsert) {
				plugin.getScheduler().asyncLater(() -> {
					pendingInserts.remove(uuid);
					justInsertedTimestamps.remove(uuid);
				}, 5, TimeUnit.SECONDS);
			}
		}
		return insertFuture;
	}

	@Override
	public void invalidateIslandCache(int islandId) {
		islandIdToUUIDCache.invalidate(islandId);
	}

	@Override
	public boolean isPendingInsert(UUID uuid) {
		return pendingInserts.containsKey(uuid);
	}

	@Override
	public Long getInsertAge(UUID uuid) {
		Long inserted = justInsertedTimestamps.get(uuid);
		return inserted != null ? (System.currentTimeMillis() - inserted) : -1;
	}

	@Override
	public boolean isInsertStillRecent(UUID uuid) {
		Long insertedAt = justInsertedTimestamps.get(uuid);
		return insertedAt != null && (System.currentTimeMillis() - insertedAt) < 5000;
	}

	@Override
	public CompletableFuture<Void> getInsertFuture(UUID uuid) {
		return pendingInserts.getOrDefault(uuid, CompletableFuture.completedFuture(null));
	}
}