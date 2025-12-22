package com.swiftlicious.hellblock.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bukkit.Bukkit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * An implementation of AbstractStorage that uses MongoDB for player data
 * storage.
 */
public class MongoDBHandler extends AbstractStorage {

	private MongoClient mongoClient;
	private MongoDatabase database;
	private String collectionPrefix;

	private final Set<UUID> forceUnlocking = ConcurrentHashMap.newKeySet();
	private final ConcurrentMap<UUID, CompletableFuture<Optional<PlayerData>>> loadingCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, CompletableFuture<Boolean>> updatingCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, CompletableFuture<Void>> lockCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Boolean> lockStates = new ConcurrentHashMap<>();

	private final ConcurrentMap<UUID, Long> justInsertedTimestamps = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, CompletableFuture<Void>> pendingInserts = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Object> lockGuards = new ConcurrentHashMap<>();

	private final Map<UUID, Long> lastAttemptTime = new ConcurrentHashMap<>();
	private static final long MIN_DELAY_BETWEEN_ATTEMPTS = 5000L; // 5 sec

	private final Cache<Integer, UUID> islandIdToUUIDCache = Caffeine.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build();

	private final Cache<UUID, PlayerData> memoryCache = Caffeine.newBuilder().maximumSize(10_000).build();

	public MongoDBHandler(HellblockPlugin plugin) {
		super(plugin);
	}

	/**
	 * Initialize the MongoDB connection and configuration based on the plugin's
	 * YAML configuration.
	 */
	@Override
	public void initialize(YamlDocument config) {
		final Section section = config.getSection("MongoDB");
		if (section == null) {
			plugin.getPluginLogger().warn(
					"Failed to load database config. It seems that your config is broken. Please regenerate a new one.");
			return;
		}

		collectionPrefix = section.getString("collection-prefix", "hellblock");

		// Ensure we load MongoDB driver classes from the isolated loader
		Set<Dependency> mongoDeps = EnumSet.of(Dependency.MONGODB_DRIVER_CORE, Dependency.MONGODB_DRIVER_SYNC,
				Dependency.MONGODB_DRIVER_BSON);

		plugin.getDependencyManager().runWithLoader(mongoDeps, () -> {
			// Now all Mongo classes are visible and linked properly
			final var settings = MongoClientSettings.builder().uuidRepresentation(UuidRepresentation.STANDARD);

			if (!section.getString("connection-uri", "").isEmpty()) {
				settings.applyConnectionString(new ConnectionString(section.getString("connection-uri")));
			} else {
				if (section.contains("user") && !section.getString("user").isEmpty() && section.contains("password")
						&& !section.getString("password").isEmpty()) {
					MongoCredential credential = MongoCredential.createCredential(section.getString("user", "root"),
							section.getString("database", "minecraft"),
							section.getString("password", "password").toCharArray());
					settings.credential(credential);
				}
				settings.applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(
						new ServerAddress(section.getString("host", "localhost"), section.getInt("port", 27017)))));
			}

			this.mongoClient = MongoClients.create(settings.build());
			this.database = mongoClient.getDatabase(section.getString("database", "minecraft"));

			// Ensure 'uuid' index exists on player data collection
			MongoCollection<Document> collection = this.database.getCollection(getCollectionName("data"));
			collection.createIndex(Indexes.ascending("uuid"), new IndexOptions().unique(true));
			return null;
		});
	}

	/**
	 * Disable the MongoDB connection by closing the MongoClient.
	 */
	@Override
	public void disable() {
		if (this.mongoClient != null) {
			this.mongoClient.close();
		}
	}

	/**
	 * Get the collection name for a specific subcategory of data.
	 *
	 * @param value The subcategory identifier.
	 * @return The full collection name including the prefix.
	 */
	public String getCollectionName(String value) {
		return getCollectionPrefix() + "_" + value;
	}

	/**
	 * Get the collection prefix used for MongoDB collections.
	 *
	 * @return The collection prefix.
	 */
	public String getCollectionPrefix() {
		return collectionPrefix;
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.MongoDB;
	}

	/**
	 * Asynchronously retrieve player data from the MongoDB database.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock Flag indicating whether to lock the data.
	 * @return A CompletableFuture with an optional PlayerData.
	 */
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

							PlayerData cached = memoryCache.getIfPresent(uuid);
							if (cached != null) {
								plugin.debug("Mongo cache hit for " + uuid);
								future.complete(Optional.of(cached));
								return;
							}

							plugin.debug("Mongo cache miss for " + uuid);

							final MongoCollection<Document> collection = database
									.getCollection(getCollectionName("data"));
							final Document doc = collection.find(Filters.eq("uuid", uuid.toString())).first();

							if (doc == null) {
								if (Bukkit.getPlayer(uuid) != null) {
									PlayerData empty = PlayerData.empty();
									empty.setUUID(uuid);

									int islandId = empty.getHellblockData().getIslandId();
									if (islandId > 0) {
										islandIdToUUIDCache.put(islandId, uuid);
									}

									// Async insert and wait before completing
									insertPlayerData(uuid, empty, lock).thenRun(() -> {
										plugin.debug("Insert complete for " + uuid + ", finishing getPlayerData");
										memoryCache.put(uuid, empty);
										future.complete(Optional.of(empty));
									}).exceptionally(ex -> {
										plugin.getPluginLogger().warn("Insert failed during getPlayerData for " + uuid,
												ex);
										future.completeExceptionally(ex);
										return null;
									});
								} else {
									future.complete(Optional.empty());
								}
								return;
							}

							int lockValue = doc.getInteger("lock", 0);
							if (lock) {
								Object guard = lockGuards.computeIfAbsent(uuid, __ -> new Object());
								synchronized (guard) {
									if (lockValue != 0 && getCurrentSeconds() - 30 <= lockValue) {
										int currentAttempt = attempt.incrementAndGet();

										if (currentAttempt >= maxRetries) {
											plugin.getPluginLogger()
													.warn("Max retries reached for " + uuid + ", forcing unlock.");
											try {
												if (forceUnlocking.add(uuid)) {
													lockOrUnlockPlayerData(uuid, false);
												}
											} finally {
												forceUnlocking.remove(uuid);
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

										plugin.getPluginLogger().warn("Player " + uuid + " data locked. Retrying...");
										plugin.getScheduler().asyncLater(this, retryDelayMs, TimeUnit.MILLISECONDS);
										return;
									}

									// Combined logic: skip locking if we're still inserting OR it's a recent insert
									if (!stillInserting && (insertedAt == null || now - insertedAt > 5000)) {
										plugin.debug("Locking player data for " + uuid);
										lockOrUnlockPlayerData(uuid, true);
									} else {
										plugin.debug("Skipping lock — insert still recent or ongoing for " + uuid
												+ " (age: " + (now - (insertedAt != null ? insertedAt : now)) + " ms)");
										future.complete(Optional.empty());
										return;
									}

									lockGuards.remove(uuid);
								}
							}

							final Binary binary = doc.get("data", Binary.class);
							PlayerData data = plugin.getStorageManager().fromBytes(binary.getData());
							data.setUUID(uuid);

							int islandId = data.getHellblockData().getIslandId();
							if (islandId > 0) {
								islandIdToUUIDCache.put(islandId, uuid);
							}

							memoryCache.put(uuid, data);
							future.complete(Optional.of(data));

						} catch (Exception ex) {
							plugin.getPluginLogger().warn("Failed to load Mongo data for " + uuid, ex);
							future.completeExceptionally(ex);
						}
					});
				}
			};

			loadTask.run();
			return future;
		}).orTimeout(5, TimeUnit.SECONDS).exceptionally(ex -> {
			plugin.getPluginLogger().warn("Timeout retrieving Mongo data for " + uuid, ex);
			return Optional.empty();
		});
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerDataByIslandId(int islandId, boolean lock,
			Executor executor) {
		final Executor finalExecutor = executor != null ? executor : plugin.getScheduler().async();
		final CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();

		// Step 1: Fast path — check Caffeine cache
		UUID cachedUUID = islandIdToUUIDCache.getIfPresent(islandId);
		if (cachedUUID != null) {
			return getPlayerData(cachedUUID, lock, finalExecutor);
		}

		// Step 2: Async Mongo lookup
		finalExecutor.execute(() -> {
			try {
				final MongoCollection<Document> collection = database.getCollection(getCollectionName("data"));

				// Optional optimization: only fetch UUID + data fields
				final FindIterable<Document> docs = collection.find().projection(Projections.include("uuid", "data"));

				for (Document doc : docs) {
					String uuidStr = doc.getString("uuid");
					if (uuidStr == null)
						continue;

					UUID uuid;
					try {
						uuid = UUID.fromString(uuidStr);
					} catch (IllegalArgumentException e) {
						continue;
					}

					// Memory cache check before deserializing
					PlayerData cached = memoryCache.getIfPresent(uuid);
					if (cached != null && cached.getHellblockData().getIslandId() == islandId) {
						islandIdToUUIDCache.put(islandId, uuid);
						future.complete(Optional.of(cached));
						return;
					}

					// Deserialize blob from Mongo
					final Binary binary = doc.get("data", Binary.class);
					if (binary == null)
						continue;

					PlayerData parsed;
					try {
						parsed = plugin.getStorageManager().fromBytes(binary.getData());
					} catch (Exception e) {
						plugin.getPluginLogger().warn("Failed to parse Mongo data for " + uuid, e);
						continue;
					}

					if (parsed != null && parsed.getHellblockData() != null
							&& parsed.getHellblockData().getIslandId() == islandId) {

						// Cache for next time
						memoryCache.put(uuid, parsed);
						islandIdToUUIDCache.put(islandId, uuid);

						// Delegate to the UUID-based loader for lock consistency
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

				// No matching island found
				future.complete(Optional.empty());

			} catch (Exception ex) {
				plugin.getPluginLogger().warn("Failed to scan Mongo collection for islandId=" + islandId, ex);
				future.completeExceptionally(ex);
			}
		});

		return future;
	}

	/**
	 * Asynchronously update player data in the MongoDB database.
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

			class UpdateTask implements Runnable {
				int attempt = 1;

				@Override
				public void run() {
					plugin.getScheduler().async().execute(() -> {
						try {
							final MongoCollection<Document> collection = database
									.getCollection(getCollectionName("data"));
							final Document query = new Document("uuid", uuid);
							final Bson updates = Updates.combine(Updates.set("lock", unlock ? 0 : getCurrentSeconds()),
									Updates.set("data", new Binary(playerData.toBytes())));

							collection.updateOne(query, updates, new UpdateOptions().upsert(true));
							memoryCache.put(uuid, playerData);
							future.complete(true);

						} catch (MongoException ex) {
							plugin.getPluginLogger().warn(
									"Mongo update failed for " + uuid + " (attempt " + attempt + "/" + maxRetries + ")",
									ex);
							if (attempt++ < maxRetries) {
								plugin.getScheduler().asyncLater(this, retryDelayMs, TimeUnit.MILLISECONDS);
							} else {
								future.completeExceptionally(ex);
							}
						}
					});
				}
			}

			new UpdateTask().run();
			return future;
		}).orTimeout(5, TimeUnit.SECONDS).exceptionally(ex -> {
			plugin.getPluginLogger().warn("Timeout while updating Mongo data for " + uuid, ex);
			return false;
		});
	}

	/**
	 * Asynchronously update data for multiple players in the MongoDB database.
	 *
	 * @param users  A collection of OfflineUser instances to update.
	 * @param unlock Flag indicating whether to unlock the data.
	 */
	@Override
	public CompletableFuture<Boolean> updateManyPlayersData(Collection<? extends UserData> users, boolean unlock) {
		final CompletableFuture<Boolean> future = new CompletableFuture<>();

		if (users == null || users.isEmpty()) {
			future.complete(false);
			return future;
		}

		final MongoCollection<Document> collection = database.getCollection(getCollectionName("data"));
		final int lock = unlock ? 0 : getCurrentSeconds();

		final List<UserData> userList = new ArrayList<>(users);
		final int maxRetries = 3;
		final long retryDelayMs = 500;
		final AtomicInteger attempt = new AtomicInteger(1);

		Runnable task = new Runnable() {
			@Override
			public void run() {
				plugin.getScheduler().async().execute(() -> {
					try {
						// Prepare batch updates
						final List<UpdateOneModel<Document>> list = userList.stream().map(it -> {
							PlayerData pd = it.toPlayerData();
							memoryCache.put(it.getUUID(), pd);
							return new UpdateOneModel<Document>(new Document("uuid", it.getUUID()),
									Updates.combine(Updates.set("lock", lock),
											Updates.set("data", new Binary(plugin.getStorageManager().toBytes(pd)))),
									new UpdateOptions().upsert(true));
						}).toList();

						if (!list.isEmpty()) {
							collection.bulkWrite(list, new BulkWriteOptions().ordered(false));
							plugin.debug("Mongo bulk update completed for %d players (attempt %d/%d)"
									.formatted(list.size(), attempt.get(), maxRetries));
						}
						future.complete(true);

					} catch (MongoException ex) {
						plugin.getPluginLogger().warn("Mongo bulk update failed (attempt %d/%d). Retrying..."
								.formatted(attempt.get(), maxRetries), ex);

						if (attempt.incrementAndGet() <= maxRetries) {
							plugin.getScheduler().asyncLater(this, retryDelayMs, TimeUnit.MILLISECONDS);
						} else {
							plugin.getPluginLogger().severe("Mongo bulk update failed after max retries.");
							future.completeExceptionally(ex);
						}
					} catch (Exception ex) {
						plugin.getPluginLogger().warn("Unexpected error in Mongo bulk update.", ex);
						future.completeExceptionally(ex);
					}
				});
			}
		};

		task.run();
		return future;
	}

	protected CompletableFuture<Void> insertPlayerData(UUID uuid, PlayerData playerData, boolean lock) {
		CompletableFuture<Void> insertFuture = new CompletableFuture<>();
		pendingInserts.put(uuid, insertFuture);
		// Only set if not already inserted recently
		justInsertedTimestamps.putIfAbsent(uuid, System.currentTimeMillis());

		try {
			final MongoCollection<Document> collection = database.getCollection(getCollectionName("data"));
			Document doc = new Document("uuid", uuid).append("lock", lock ? getCurrentSeconds() : 0).append("data",
					new Binary(plugin.getStorageManager().toBytes(playerData)));
			collection.insertOne(doc);

			plugin.debug("Inserted Mongo data for player %s (lock=%s)".formatted(uuid, lock));

			insertFuture.complete(null); // signal insert done
		} catch (MongoException ex) {
			insertFuture.completeExceptionally(ex);
			plugin.getPluginLogger().warn("Failed to insert Mongo data for " + uuid, ex);
			throw new RuntimeException("Mongo Insert failed for " + uuid, ex);
		} finally {
			// Schedule cleanup of both caches
			plugin.getScheduler().asyncLater(() -> {
				pendingInserts.remove(uuid);
				justInsertedTimestamps.remove(uuid);
			}, 5, TimeUnit.SECONDS);
		}

		return insertFuture;
	}

	/**
	 * Lock or unlock player data in the MongoDB database.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock Flag indicating whether to lock or unlock the data.
	 */
	@Override
	public void lockOrUnlockPlayerData(UUID uuid, boolean lock) {
		Object guard = lockGuards.computeIfAbsent(uuid, __ -> new Object());

		synchronized (guard) {
			try {
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

					// Ensure cleanup only after future completes
					future.whenComplete((result, throwable) -> plugin.getScheduler()
							.asyncLater(() -> lockCache.remove(uuid), 5, TimeUnit.SECONDS));

					plugin.getScheduler().async().execute(() -> {
						try {
							final MongoCollection<Document> collection = database
									.getCollection(getCollectionName("data"));
							final Document query = new Document("uuid", uuid);
							final Bson update = Updates.set("lock", lock ? getCurrentSeconds() : 0);
							collection.updateOne(query, update, new UpdateOptions().upsert(true));
							future.complete(null);
						} catch (MongoException ex) {
							plugin.getPluginLogger()
									.warn("Failed to " + (lock ? "lock" : "unlock") + " Mongo data for " + uuid, ex);
							future.completeExceptionally(ex);
						}
					});
					return future;
				});
			} finally {
				// prevent memory leak
				plugin.getScheduler().asyncLater(() -> lockGuards.remove(uuid), 5, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * Get a set of unique player UUIDs from the MongoDB database.
	 *
	 * @return A set of unique player UUIDs.
	 */
	@Override
	public Set<UUID> getUniqueUsers() {
		final Set<UUID> uuids = new HashSet<>();
		final MongoCollection<Document> collection = database.getCollection(getCollectionName("data"));
		try {
			final Bson projection = Projections.include("uuid");
			try (MongoCursor<Document> cursor = collection.find().projection(projection).iterator()) {
				while (cursor.hasNext()) {
					UUID id = cursor.next().get("uuid", UUID.class);
					if (id != null)
						uuids.add(id);
				}
			}
		} catch (MongoException ex) {
			plugin.getPluginLogger().warn("Failed to get unique UUIDS using Mongo database.", ex);
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
}