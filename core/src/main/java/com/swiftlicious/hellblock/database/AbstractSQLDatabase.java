package com.swiftlicious.hellblock.database;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

/**
 * An abstract base class for SQL database implementations that handle player
 * data storage.
 */
public abstract class AbstractSQLDatabase extends AbstractStorage {

	protected String tablePrefix;

	private final Set<UUID> forceUnlocking = ConcurrentHashMap.newKeySet();
	private final ConcurrentMap<UUID, CompletableFuture<Optional<PlayerData>>> loadingCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, CompletableFuture<Boolean>> updatingCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, CompletableFuture<Boolean>> upsertCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, CompletableFuture<Void>> lockCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Boolean> lockStates = new ConcurrentHashMap<>();

	protected final ConcurrentMap<UUID, Long> justInsertedTimestamps = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, CompletableFuture<Void>> pendingInserts = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Object> lockGuards = new ConcurrentHashMap<>();

	private final Map<UUID, Long> lastAttemptTime = new ConcurrentHashMap<>();
	private static final long MIN_DELAY_BETWEEN_ATTEMPTS = 5000L; // 5 sec

	private final Cache<Integer, UUID> islandIdToUUIDCache = Caffeine.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build();

	public AbstractSQLDatabase(HellblockPlugin plugin) {
		super(plugin);
	}

	/**
	 * Get a connection to the SQL database.
	 *
	 * @return A database connection.
	 * @throws SQLException If there is an error establishing a connection.
	 */
	@NotNull
	public abstract Connection getConnection() throws SQLException;

	/**
	 * Create tables for storing data if they don't exist in the database.
	 */
	public void createTableIfNotExist() {
		try (Connection connection = getConnection()) {
			final String[] databaseSchema = getSchema(getStorageType().name().toLowerCase(Locale.ENGLISH));
			try (Statement statement = connection.createStatement()) {
				plugin.getPluginLogger().info("Ensuring %s schema exists... ".formatted(getStorageType().name()));

				for (String tableCreationStatement : databaseSchema) {
					String sql = tableCreationStatement.trim();
					if (sql.isEmpty() || sql.startsWith("--"))
						continue; // skip comments/empty lines
					try {
						statement.execute(sql);
					} catch (SQLException ex) {
						plugin.getPluginLogger().warn("Failed to execute SQL:\n" + sql, ex);
					}
				}

				plugin.getPluginLogger()
						.info("%s schema validation/creation completed.".formatted(getStorageType().name()));
			} catch (SQLException ex) {
				plugin.getPluginLogger().warn("Failed to create tables.", ex);
			}
		} catch (SQLException ex) {
			plugin.getPluginLogger().warn("Failed to get SQL connection.", ex);
		} catch (IOException ex) {
			plugin.getPluginLogger().warn("Failed to load schema resource.", ex);
		}
	}

	/**
	 * Get the SQL schema from a resource file.
	 *
	 * @param fileName The name of the schema file.
	 * @return An array of SQL statements to create tables.
	 * @throws IOException If there is an error reading the schema resource.
	 */
	private String[] getSchema(@NotNull String fileName) throws IOException {
		InputStream stream = plugin.getResource("schema/" + fileName + ".sql");
		if (stream == null) {
			throw new FileNotFoundException("Schema file not found: schema/" + fileName + ".sql");
		}
		String raw = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		return replaceSchemaPlaceholder(raw).split(";");
	}

	/**
	 * Replace placeholder values in SQL schema with the table prefix.
	 *
	 * @param sql The SQL schema string.
	 * @return The SQL schema string with placeholders replaced.
	 */
	private String replaceSchemaPlaceholder(@NotNull String sql) {
		return sql.replace("{prefix}", Objects.toString(tablePrefix, ""));
	}

	/**
	 * Get the name of a database table based on a sub-table name and the table
	 * prefix.
	 *
	 * @param sub The sub-table name.
	 * @return The full table name.
	 */
	public String getTableName(String sub) {
		return getTablePrefix() + "_" + sub;
	}

	/**
	 * Get the current table prefix.
	 *
	 * @return The table prefix.
	 */
	public String getTablePrefix() {
		return tablePrefix;
	}

	/**
	 * Retrieve a player's data from the SQL database.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock Whether to lock the player data during retrieval.
	 * @return A CompletableFuture containing the optional player data.
	 */
	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor) {
		final Executor finalExecutor = executor != null ? executor : plugin.getScheduler().async();

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

			future.whenComplete((res, ex) -> plugin.getScheduler().asyncLater(() -> {
				loadingCache.remove(uuid);
				lastAttemptTime.remove(uuid);
			}, 5, TimeUnit.SECONDS));

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
								final ResultSet rs = statement.executeQuery();

								if (rs.next()) {
									final int lockValue = rs.getInt("lock");

									// Guarded section for concurrent access control
									if (lock) {
										Object guard = lockGuards.computeIfAbsent(uuid, __ -> new Object());
										synchronized (guard) {
											if (lockValue != 0 && getCurrentSeconds() - 30 <= lockValue) {
												int currentAttempt = attempt.incrementAndGet();

												if (currentAttempt >= maxRetries) {
													plugin.getPluginLogger().warn(
															"Max retries reached for player %s (attempts=%d). Forcing unlock..."
																	.formatted(uuid, currentAttempt));
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
													} catch (Exception unlockEx) {
														plugin.getPluginLogger().warn(
																"Failed to force unlock %s's data.".formatted(uuid),
																unlockEx);
													}

													plugin.getScheduler()
															.asyncLater(() -> getPlayerData(uuid, lock, finalExecutor)
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
												plugin.getScheduler().asyncLater(this, retryDelayMs,
														TimeUnit.MILLISECONDS);
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

											plugin.getScheduler().asyncLater(() -> lockGuards.remove(uuid), 5,
													TimeUnit.SECONDS);
										}
									}

									// Deserialize outside synchronized block
									final Blob blob = rs.getBlob("data");
									final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
									blob.free();

									final PlayerData data = plugin.getStorageManager().fromBytes(dataByteArray);
									data.setUUID(uuid);

									int islandId = data.getHellblockData().getIslandId();
									if (islandId > 0) {
										islandIdToUUIDCache.put(islandId, uuid);
									}

									future.complete(Optional.of(data));
									return;
								}

								// Create new data if player online and no existing record
								if (Bukkit.getPlayer(uuid) != null) {
									final PlayerData data = PlayerData.empty();
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
								}

								future.complete(Optional.empty());

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
		final Executor finalExecutor = executor != null ? executor : plugin.getScheduler().async();
		final CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();

		// Step 1: Try Caffeine cache
		UUID cachedUUID = islandIdToUUIDCache.getIfPresent(islandId);
		if (cachedUUID != null) {
			getPlayerData(cachedUUID, lock, finalExecutor).whenComplete((result, throwable) -> {
				if (throwable != null) {
					future.completeExceptionally(throwable);
				} else {
					future.complete(result);
				}
			});
			return future;
		}

		// Step 2: Scan SQL to find matching islandId
		finalExecutor.execute(() -> {
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

					Blob blob = rs.getBlob("data");
					byte[] dataBytes = blob.getBytes(1, (int) blob.length());
					blob.free();

					PlayerData parsed;
					try {
						parsed = plugin.getStorageManager().fromBytes(dataBytes);
					} catch (Exception e) {
						plugin.getPluginLogger().warn("Failed to parse SQL blob for UUID " + uuid, e);
						continue;
					}

					if (parsed != null && parsed.getHellblockData() != null
							&& parsed.getHellblockData().getIslandId() == islandId) {

						// Cache for future
						islandIdToUUIDCache.put(islandId, uuid);

						// Delegate to existing UUID loader
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

			} catch (Exception ex) {
				plugin.getPluginLogger().warn("Failed to scan SQL blobs for islandId=" + islandId, ex);
				future.completeExceptionally(ex);
			}
		});

		return future;
	}

	/**
	 * Update a player's data in the SQL database.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player data to update.
	 * @param unlock     Whether to unlock the player data after updating.
	 * @return A CompletableFuture indicating the success of the update.
	 */
	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean unlock) {
		// Deduplicate concurrent updates per-UUID
		return updatingCache.computeIfAbsent(uuid, id -> {
			final CompletableFuture<Boolean> future = new CompletableFuture<>();
			final int maxRetries = 3;
			final long retryDelayMs = 500;

			// Ensure cleanup happens only once after complete
			future.whenComplete((result, throwable) -> plugin.getScheduler()
					.asyncLater(() -> updatingCache.remove(uuid), 5, TimeUnit.SECONDS));

			// Runnable to perform update attempts; uses an inner attempt counter
			final class UpdateRunnable implements Runnable {
				int attempt = 1;

				@Override
				public void run() {
					plugin.getScheduler().async().execute(() -> {
						try (Connection connection = getConnection()) {
							connection.setAutoCommit(false);
							try (PreparedStatement statement = connection.prepareStatement(
									SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data")))) {

								statement.setInt(1, unlock ? 0 : getCurrentSeconds());
								statement.setBlob(2, new ByteArrayInputStream(playerData.toBytes()));
								statement.setString(3, uuid.toString());

								int affected = statement.executeUpdate();
								if (affected == 0) {
									// fallback to upsert/insert
									plugin.debug("No rows updated for %s during update (attempt %d). Performing upsert."
											.formatted(uuid, attempt));
									insertPlayerData(uuid, playerData, !unlock, connection, false);
								}

								connection.commit();
								future.complete(true);
							} catch (SQLException ex) {
								try {
									connection.rollback();
								} catch (SQLException rollbackEx) {
									plugin.getPluginLogger().warn("Rollback failed", rollbackEx);
								}

								plugin.getPluginLogger().warn("Failed to update data for %s (attempt %d/%d)"
										.formatted(uuid, attempt, maxRetries), ex);

								if (attempt < maxRetries) {
									attempt++;
									// schedule another try
									plugin.getScheduler().asyncLater(this, retryDelayMs, TimeUnit.MILLISECONDS);
								} else {
									plugin.getPluginLogger()
											.severe("Exceeded max retries for updatePlayerData for " + uuid);
									future.completeExceptionally(ex);
								}
							}
						} catch (SQLException ex) {
							plugin.getPluginLogger().warn("Failed to get connection to update %s (attempt %d/%d)"
									.formatted(uuid, attempt, maxRetries), ex);

							if (attempt < maxRetries) {
								attempt++;
								plugin.getScheduler().asyncLater(this, retryDelayMs, TimeUnit.MILLISECONDS);
							} else {
								plugin.getPluginLogger().severe(
										"Exceeded max retries (connection failure) for updatePlayerData for " + uuid);
								future.completeExceptionally(ex);
							}
						}
					});
				}
			}

			// start first attempt
			new UpdateRunnable().run();
			return future;
		});
	}

	/**
	 * Update data for multiple players in the SQL database.
	 *
	 * @param users  A collection of OfflineUser objects representing players.
	 * @param unlock Whether to unlock the player data after updating.
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

		final Runnable task = new Runnable() {
			@Override
			public void run() {
				plugin.getScheduler().async().execute(() -> {
					try (Connection connection = getConnection()) {
						connection.setAutoCommit(false);
						final String sql = SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data"));

						try (PreparedStatement statement = connection.prepareStatement(sql)) {
							for (UserData user : userList) {
								statement.setInt(1, unlock ? 0 : getCurrentSeconds());
								statement.setBlob(2, new ByteArrayInputStream(
										plugin.getStorageManager().toBytes(user.toPlayerData())));
								statement.setString(3, user.getUUID().toString());
								statement.addBatch();
							}

							statement.executeBatch();
							connection.commit();
							future.complete(true); // success

						} catch (SQLException ex) {
							connection.rollback();
							plugin.getPluginLogger().warn("Batch update failed (attempt %d/%d). Retrying..."
									.formatted(attempt.get(), maxRetries), ex);

							if (attempt.incrementAndGet() <= maxRetries) {
								plugin.getScheduler().asyncLater(this, retryDelayMs, TimeUnit.MILLISECONDS);
							} else {
								plugin.getPluginLogger().severe("Batch update failed after max retries.");
								future.completeExceptionally(ex); // fail
							}
						}
					} catch (SQLException ex) {
						plugin.getPluginLogger().warn(
								"Batch update connection failure (attempt %d/%d).".formatted(attempt.get(), maxRetries),
								ex);

						if (attempt.incrementAndGet() <= maxRetries) {
							plugin.getScheduler().asyncLater(this, retryDelayMs, TimeUnit.MILLISECONDS);
						} else {
							plugin.getPluginLogger().severe("Batch update connection failed after max retries.");
							future.completeExceptionally(ex); // fail
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
	 * Insert a new player's data into the SQL database.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player data to insert.
	 * @param lock       Whether to lock the player data upon insertion.
	 */
	protected CompletableFuture<Void> insertPlayerData(UUID uuid, PlayerData playerData, boolean lock,
			@Nullable Connection previous, boolean trackInsert) {
		CompletableFuture<Void> insertFuture = null;
		if (trackInsert) {
			insertFuture = new CompletableFuture<>();
			pendingInserts.put(uuid, insertFuture);
			// Only set if not already inserted recently
			justInsertedTimestamps.putIfAbsent(uuid, System.currentTimeMillis());
		}

		try (Connection connection = previous != null ? previous : getConnection();
				PreparedStatement statement = connection
						.prepareStatement(SqlConstants.SQL_INSERT_DATA_BY_UUID.formatted(getTableName("data")))) {

			statement.setString(1, uuid.toString());
			statement.setInt(2, lock ? getCurrentSeconds() : 0);
			statement.setBlob(3, new ByteArrayInputStream(plugin.getStorageManager().toBytes(playerData)));
			statement.executeUpdate();

			plugin.debug("Inserted data for player %s (lock=%s, trackInsert=%s)".formatted(uuid, lock, trackInsert));

			if (trackInsert && insertFuture != null) {
				insertFuture.complete(null); // signal insert done
			}

		} catch (SQLException ex) {
			if (trackInsert && insertFuture != null) {
				insertFuture.completeExceptionally(ex);
			}
			plugin.getPluginLogger().warn("Failed to insert data for %s".formatted(uuid), ex);
			throw new RuntimeException("Insert failed for " + uuid, ex);
		} finally {
			// Only clean up if this was a tracked insert
			if (trackInsert) {
				plugin.getScheduler().asyncLater(() -> {
					pendingInserts.remove(uuid);
					justInsertedTimestamps.remove(uuid);
				}, 5, TimeUnit.SECONDS);
			}
		}
		return insertFuture;
	}

	// Optional Connection variant
	public void lockOrUnlockPlayerData(UUID uuid, boolean lock, @Nullable Connection externalConnection) {
		Object guard = lockGuards.computeIfAbsent(uuid, __ -> new Object());

		synchronized (guard) {
			try {
				if (externalConnection != null) {
					try (PreparedStatement statement = externalConnection
							.prepareStatement(SqlConstants.SQL_LOCK_BY_UUID.formatted(getTableName("data")))) {

						statement.setInt(1, lock ? getCurrentSeconds() : 0);
						statement.setString(2, uuid.toString());

						int affected = statement.executeUpdate();
						if (affected == 0) {
							plugin.getPluginLogger().warn("Tried to %s data for %s, but no record found."
									.formatted(lock ? "lock" : "unlock", uuid));
						}

					} catch (SQLException ex) {
						plugin.getPluginLogger()
								.warn("Failed to %s %s's data.".formatted(lock ? "lock" : "unlock", uuid), ex);
					}
				} else {
					// Async fallback
					lockCache.compute(uuid, (id, existingFuture) -> {
						Boolean currentState = lockStates.get(uuid); 
						if (currentState != null && currentState == lock) {
							// Already in desired state, skip
							return existingFuture;
						}

						// Store new state
						lockStates.put(uuid, lock);

						plugin.debug("Setting lock for %s to %s".formatted(uuid, lock));
						final CompletableFuture<Void> future = new CompletableFuture<>();

						future.whenComplete((result, throwable) -> plugin.getScheduler()
								.asyncLater(() -> lockCache.remove(uuid), 5, TimeUnit.SECONDS));

						plugin.getScheduler().async().execute(() -> {
							try (Connection connection = getConnection();
									PreparedStatement statement = connection.prepareStatement(
											SqlConstants.SQL_LOCK_BY_UUID.formatted(getTableName("data")))) {

								statement.setInt(1, lock ? getCurrentSeconds() : 0);
								statement.setString(2, uuid.toString());

								int affected = statement.executeUpdate();
								if (affected == 0) {
									plugin.getPluginLogger().warn("Tried to %s data for %s, but no record found."
											.formatted(lock ? "lock" : "unlock", uuid));
								}
								future.complete(null);

							} catch (SQLException ex) {
								plugin.getPluginLogger()
										.warn("Failed to %s %s's data.".formatted(lock ? "lock" : "unlock", uuid), ex);
								future.completeExceptionally(ex);
							}
						});

						return future;
					});
				}
			} finally {
				// Always schedule cleanup — safe and outside synchronized block
				plugin.getScheduler().asyncLater(() -> lockGuards.remove(uuid), 5, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * Lock or unlock a player's data in the SQL database.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock Whether to lock or unlock the player data.
	 */
	@Override
	public void lockOrUnlockPlayerData(UUID uuid, boolean lock) {
		lockOrUnlockPlayerData(uuid, lock, null);
	}

	/**
	 * Update or insert a player's data into the SQL database.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player data to update or insert.
	 * @param unlock     Whether to unlock the player data after updating or
	 *                   inserting.
	 * @return A CompletableFuture indicating the success of the operation.
	 */
	@Override
	public CompletableFuture<Boolean> updateOrInsertPlayerData(UUID uuid, PlayerData playerData, boolean unlock) {
		return upsertCache.computeIfAbsent(uuid, id -> {
			final CompletableFuture<Boolean> future = new CompletableFuture<>();
			final int maxRetries = 3;
			final long retryDelayMs = 500;

			// Remove from cache only after future completes
			future.whenComplete(
					(res, ex) -> plugin.getScheduler().asyncLater(() -> upsertCache.remove(uuid), 5, TimeUnit.SECONDS));

			class UpsertRunnable implements Runnable {
				int attempt = 1;

				@Override
				public void run() {
					plugin.getScheduler().async().execute(() -> {
						try (Connection connection = getConnection()) {
							connection.setAutoCommit(false);

							final boolean exists;
							try (PreparedStatement check = connection.prepareStatement(
									SqlConstants.SQL_SELECT_BY_UUID.formatted(getTableName("data")))) {
								check.setString(1, uuid.toString());
								try (ResultSet rs = check.executeQuery()) {
									exists = rs.next();
								}
							}

							if (exists) {
								try (PreparedStatement update = connection.prepareStatement(
										SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data")))) {
									update.setInt(1, unlock ? 0 : getCurrentSeconds());
									update.setBlob(2,
											new ByteArrayInputStream(plugin.getStorageManager().toBytes(playerData)));
									update.setString(3, uuid.toString());
									update.executeUpdate();
								}
							} else {
								plugin.debug("Performing insert fallback via upsert for " + uuid);
								insertPlayerData(uuid, playerData, !unlock, connection, false);
							}

							connection.commit();
							future.complete(true);

						} catch (SQLException ex) {
							plugin.getPluginLogger().warn(
									"Upsert attempt %d/%d failed for %s".formatted(attempt, maxRetries, uuid), ex);
							if (attempt++ < maxRetries) {
								plugin.getScheduler().asyncLater(this, retryDelayMs, TimeUnit.MILLISECONDS);
							} else {
								plugin.getPluginLogger()
										.severe("Upsert failed for %s after %d retries.".formatted(uuid, maxRetries));
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
	 * Get a set of unique user UUIDs from the SQL database.
	 *
	 * @return A set of unique user UUIDs.
	 */
	@Override
	public Set<UUID> getUniqueUsers() {
		final Set<UUID> uuids = new HashSet<>();
		try (Connection connection = getConnection();
				PreparedStatement statement = connection
						.prepareStatement(SqlConstants.SQL_SELECT_ALL_UUID.formatted(getTableName("data")))) {
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					final UUID uuid = UUID.fromString(rs.getString("uuid"));
					uuids.add(uuid);
				}
			}
		} catch (SQLException ex) {
			plugin.getPluginLogger().warn("Failed to get unique UUIDS using SQL database.", ex);
		}
		return uuids;
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
	public boolean isInsertStillRecent(UUID uuid) {
		Long insertedAt = justInsertedTimestamps.get(uuid);
		return insertedAt != null && (System.currentTimeMillis() - insertedAt) < 5000;
	}

	@Override
	public Long getInsertAge(UUID uuid) {
		Long inserted = justInsertedTimestamps.get(uuid);
		return inserted != null ? (System.currentTimeMillis() - inserted) : -1;
	}

	@Override
	public CompletableFuture<Void> getInsertFuture(UUID uuid) {
		return pendingInserts.getOrDefault(uuid, CompletableFuture.completedFuture(null));
	}

	/**
	 * Constants defining SQL statements used for database operations.
	 */
	public static class SqlConstants {
		public static final String SQL_SELECT_BY_UUID = "SELECT " + quote("uuid") + ", " + quote("lock") + ", "
				+ quote("data") + " FROM " + quote("%s") + " WHERE " + quote("uuid") + " = ?";
		public static final String SQL_SELECT_ALL_UUID = "SELECT " + quote("uuid") + ", " + quote("data") + " FROM "
				+ quote("%s");
		public static final String SQL_UPDATE_BY_UUID = "UPDATE " + quote("%s") + " SET " + quote("lock") + " = ?, "
				+ quote("data") + " = ? WHERE " + quote("uuid") + " = ?";
		public static final String SQL_LOCK_BY_UUID = "UPDATE " + quote("%s") + " SET " + quote("lock") + " = ? WHERE "
				+ quote("uuid") + " = ?";
		public static final String SQL_INSERT_DATA_BY_UUID = switch (HellblockPlugin.getInstance().getStorageManager()
				.getDataSource().getStorageType()) {
		case H2 -> "MERGE INTO " + quote("%s") + " KEY(" + quote("uuid") + ") VALUES(?, ?, ?)";
		case MySQL,
				MariaDB ->
			"INSERT INTO " + quote("%s") + "(" + quote("uuid") + ", " + quote("lock") + ", " + quote("data") + ") "
					+ "VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE " + quote("lock") + " = VALUES(" + quote("lock") + "), "
					+ quote("data") + " = VALUES(" + quote("data") + ")";
		case PostgreSQL ->
			"INSERT INTO %s(uuid, lock, data) VALUES(?, ?, ?) ON CONFLICT (uuid) DO UPDATE SET lock = EXCLUDED.lock, data = EXCLUDED.data";
		case SQLite -> "INSERT OR REPLACE INTO " + quote("%s") + "(" + quote("uuid") + ", " + quote("lock") + ", "
				+ quote("data") + ") VALUES(?, ?, ?)";
		default -> throw new UnsupportedOperationException("No upsert query defined for this database type.");
		};
	}

	private static String quote(String identifier) {
		return switch (HellblockPlugin.getInstance().getStorageManager().getDataSource().getStorageType()) {
		case PostgreSQL -> "\"" + identifier + "\"";
		default -> "`" + identifier + "`";
		};
	}
}