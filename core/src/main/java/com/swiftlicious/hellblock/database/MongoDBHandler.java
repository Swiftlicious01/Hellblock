package com.swiftlicious.hellblock.database;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bukkit.Bukkit;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
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

			if (!"".equals(section.getString("connection-uri", ""))) {
				settings.applyConnectionString(new ConnectionString(section.getString("connection-uri", "")));
				this.mongoClient = MongoClients.create(settings.build());
				this.database = mongoClient.getDatabase(section.getString("database", "minecraft"));
				return null;
			}

			if (section.contains("user") && !section.getString("user").isEmpty() && section.contains("password")
					&& !section.getString("password").isEmpty()) {
				final MongoCredential credential = MongoCredential.createCredential(section.getString("user", "root"),
						section.getString("database", "minecraft"),
						section.getString("password", "password").toCharArray());
				settings.credential(credential);
			}

			settings.applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(
					new ServerAddress(section.getString("host", "localhost"), section.getInt("port", 27017)))));

			this.mongoClient = MongoClients.create(settings.build());
			this.database = mongoClient.getDatabase(section.getString("database", "minecraft"));
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
	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor) {
		final var future = new CompletableFuture<Optional<PlayerData>>();
		if (executor == null) {
			executor = plugin.getScheduler().async();
		}
		executor.execute(() -> {
			final MongoCollection<Document> collection = database.getCollection(getCollectionName("data"));
			final Document doc = collection.find(Filters.eq("uuid", uuid)).first();
			if (doc == null) {
				if (Bukkit.getPlayer(uuid) != null) {
					if (lock) {
						lockOrUnlockPlayerData(uuid, true);
					}
					final var data = PlayerData.empty();
					data.setUUID(uuid);
					future.complete(Optional.of(data));
				} else {
					future.complete(Optional.empty());
				}
			} else {
				final Binary binary = (Binary) doc.get("data");
				final PlayerData data = plugin.getStorageManager().fromBytes(binary.getData());
				data.setUUID(uuid);
				if (doc.getInteger("lock") != 0 && getCurrentSeconds()
						- plugin.getConfigManager().dataSaveInterval() <= doc.getInteger("lock")) {
					data.setLocked(true);
					future.complete(Optional.of(data));
					return;
				}
				if (lock) {
					lockOrUnlockPlayerData(uuid, true);
				}
				future.complete(Optional.of(data));
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
		final var future = new CompletableFuture<Boolean>();
		plugin.getScheduler().async().execute(() -> {
			final MongoCollection<Document> collection = database.getCollection(getCollectionName("data"));
			try {
				final Document query = new Document("uuid", uuid);
				final Bson updates = Updates.combine(Updates.set("lock", unlock ? 0 : getCurrentSeconds()),
						Updates.set("data", new Binary(playerData.toBytes())));
				final UpdateOptions options = new UpdateOptions().upsert(true);
				final UpdateResult result = collection.updateOne(query, updates, options);
				future.complete(result.wasAcknowledged());

			} catch (MongoException ex) {
				future.completeExceptionally(ex);
			}
		});
		return future;
	}

	/**
	 * Asynchronously update data for multiple players in the MongoDB database.
	 *
	 * @param users  A collection of OfflineUser instances to update.
	 * @param unlock Flag indicating whether to unlock the data.
	 */
	@Override
	public void updateManyPlayersData(Collection<? extends UserData> users, boolean unlock) {
		final MongoCollection<Document> collection = database.getCollection(getCollectionName("data"));
		try {
			final int lock = unlock ? 0 : getCurrentSeconds();
			final var list = users.stream().map(it -> new UpdateOneModel<Document>(new Document("uuid", it.getUUID()),
					Updates.combine(Updates.set("lock", lock),
							Updates.set("data", new Binary(plugin.getStorageManager().toBytes(it.toPlayerData())))),
					new UpdateOptions().upsert(true))).toList();
			if (list.isEmpty()) {
				return;
			}
			collection.bulkWrite(list);
		} catch (MongoException ex) {
			plugin.getPluginLogger().warn("Failed to update data for online players.", ex);
		}
	}

	/**
	 * Lock or unlock player data in the MongoDB database.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock Flag indicating whether to lock or unlock the data.
	 */
	@Override
	public void lockOrUnlockPlayerData(UUID uuid, boolean lock) {
		final MongoCollection<Document> collection = database.getCollection(getCollectionName("data"));
		try {
			final Document query = new Document("uuid", uuid);
			final Bson updates = Updates.combine(Updates.set("lock", !lock ? 0 : getCurrentSeconds()));
			final UpdateOptions options = new UpdateOptions().upsert(true);
			collection.updateOne(query, updates, options);
		} catch (MongoException ex) {
			plugin.getPluginLogger().warn("Failed to lock data for " + uuid, ex);
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
			final Bson projectionFields = Projections.fields(Projections.include("uuid"));
			try (MongoCursor<Document> cursor = collection.find().projection(projectionFields).iterator()) {
				while (cursor.hasNext()) {
					uuids.add(cursor.next().get("uuid", UUID.class));
				}
			}
		} catch (MongoException ex) {
			plugin.getPluginLogger().warn("Failed to get unique data.", ex);
		}
		return uuids;
	}
}