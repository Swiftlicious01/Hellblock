package com.swiftlicious.hellblock.database;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

/**
 * An abstract base class for SQL database implementations that handle player
 * data storage.
 */
public abstract class AbstractSQLDatabase extends AbstractStorage {

	protected String tablePrefix;

	public AbstractSQLDatabase(HellblockPlugin plugin) {
		super(plugin);
	}

	/**
	 * Get a connection to the SQL database.
	 *
	 * @return A database connection.
	 * @throws SQLException If there is an error establishing a connection.
	 */
	public abstract Connection getConnection() throws SQLException;

	/**
	 * Create tables for storing data if they don't exist in the database.
	 */
	public void createTableIfNotExist() {
		try (Connection connection = getConnection()) {
			final String[] databaseSchema = getSchema(getStorageType().name().toLowerCase(Locale.ENGLISH));
			try (Statement statement = connection.createStatement()) {
				for (String tableCreationStatement : databaseSchema) {
					statement.execute(tableCreationStatement);
				}
			} catch (SQLException ex) {
				plugin.getPluginLogger().warn("Failed to create tables.", ex);
			}
		} catch (SQLException ex) {
			plugin.getPluginLogger().warn("Failed to get sql connection.", ex);
		} catch (IOException ex) {
			plugin.getPluginLogger().warn("Failed to get schema resource.", ex);
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
		return replaceSchemaPlaceholder(
				new String(Objects.requireNonNull(plugin.getResource("schema/" + fileName + ".sql")).readAllBytes(),
						StandardCharsets.UTF_8))
				.split(";");
	}

	/**
	 * Replace placeholder values in SQL schema with the table prefix.
	 *
	 * @param sql The SQL schema string.
	 * @return The SQL schema string with placeholders replaced.
	 */
	private String replaceSchemaPlaceholder(@NotNull String sql) {
		return sql.replace("{prefix}", tablePrefix);
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
		final var future = new CompletableFuture<Optional<PlayerData>>();
		if (executor == null) {
			executor = plugin.getScheduler().async();
		}
		executor.execute(() -> {
			try (Connection connection = getConnection();
					PreparedStatement statement = connection
							.prepareStatement(SqlConstants.SQL_SELECT_BY_UUID.formatted(getTableName("data")))) {
				statement.setString(1, uuid.toString());
				final ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					final Blob blob = rs.getBlob("data");
					final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
					blob.free();
					final PlayerData data = plugin.getStorageManager().fromBytes(dataByteArray);
					data.setUUID(uuid);
					if (lock) {
						final int lockValue = rs.getInt(2);
						if (lockValue != 0 && getCurrentSeconds() - 30 <= lockValue) {
							data.setLocked(true);
							future.complete(Optional.of(data));
							plugin.getPluginLogger().warn("Player %s's data is locked. Retrying...".formatted(uuid));
							return;
						}
					}
					if (lock) {
						lockOrUnlockPlayerData(uuid, true);
					}
					future.complete(Optional.of(data));
				} else if (Bukkit.getPlayer(uuid) != null) {
					// the player is online
					final var data = PlayerData.empty();
					data.setUUID(uuid);
					insertPlayerData(uuid, data, lock, connection);
					future.complete(Optional.of(data));
				} else {
					future.complete(Optional.empty());
				}
			} catch (SQLException ex) {
				plugin.getPluginLogger().warn("Failed to get %s's data.".formatted(uuid), ex);
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
		final var future = new CompletableFuture<Boolean>();
		plugin.getScheduler().async().execute(() -> {
			try (Connection connection = getConnection();
					PreparedStatement statement = connection
							.prepareStatement(SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data")))) {
				statement.setInt(1, unlock ? 0 : getCurrentSeconds());
				statement.setBlob(2, new ByteArrayInputStream(playerData.toBytes()));
				statement.setString(3, uuid.toString());
				statement.executeUpdate();
				future.complete(true);
				plugin.debug("SQL data saved for %s; unlock: %s".formatted(uuid, unlock));
			} catch (SQLException ex) {
				plugin.getPluginLogger().warn("Failed to update %s's data.".formatted(uuid), ex);
				future.completeExceptionally(ex);
			}
		});
		return future;
	}

	/**
	 * Update data for multiple players in the SQL database.
	 *
	 * @param users  A collection of OfflineUser objects representing players.
	 * @param unlock Whether to unlock the player data after updating.
	 */
	@Override
	public void updateManyPlayersData(Collection<? extends UserData> users, boolean unlock) {
		final String sql = SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data"));
		try (Connection connection = getConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				for (UserData user : users) {
					statement.setInt(1, unlock ? 0 : getCurrentSeconds());
					statement.setBlob(2,
							new ByteArrayInputStream(plugin.getStorageManager().toBytes(user.toPlayerData())));
					statement.setString(3, user.getUUID().toString());
					statement.addBatch();
				}
				statement.executeBatch();
				connection.commit();
			} catch (SQLException ex) {
				connection.rollback();
				plugin.getPluginLogger().warn("Failed to update data for online players.", ex);
			}
		} catch (SQLException ex) {
			plugin.getPluginLogger().warn("Failed to get connection when saving online players' data.", ex);
		}
	}

	/**
	 * Insert a new player's data into the SQL database.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player data to insert.
	 * @param lock       Whether to lock the player data upon insertion.
	 */
	protected void insertPlayerData(UUID uuid, PlayerData playerData, boolean lock, @Nullable Connection previous) {
		try (Connection connection = previous == null ? getConnection() : previous;
				PreparedStatement statement = connection
						.prepareStatement(SqlConstants.SQL_INSERT_DATA_BY_UUID.formatted(getTableName("data")))) {
			statement.setString(1, uuid.toString());
			statement.setInt(2, lock ? getCurrentSeconds() : 0);
			statement.setBlob(3, new ByteArrayInputStream(plugin.getStorageManager().toBytes(playerData)));
			statement.execute();
		} catch (SQLException ex) {
			plugin.getPluginLogger().warn("Failed to insert %s's data.".formatted(uuid), ex);
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
		try (Connection connection = getConnection();
				PreparedStatement statement = connection
						.prepareStatement(SqlConstants.SQL_LOCK_BY_UUID.formatted(getTableName("data")))) {
			statement.setInt(1, lock ? getCurrentSeconds() : 0);
			statement.setString(2, uuid.toString());
			statement.execute();
		} catch (SQLException ex) {
			plugin.getPluginLogger().warn("Failed to lock %s's data.".formatted(uuid), ex);
		}
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
		final var future = new CompletableFuture<Boolean>();
		plugin.getScheduler().async().execute(() -> {
			try (Connection connection = getConnection();
					PreparedStatement statement = connection
							.prepareStatement(SqlConstants.SQL_SELECT_BY_UUID.formatted(getTableName("data")))) {
				statement.setString(1, uuid.toString());
				final ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					try (PreparedStatement statement2 = connection
							.prepareStatement(SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data")))) {
						statement2.setInt(1, unlock ? 0 : getCurrentSeconds());
						statement2.setBlob(2, new ByteArrayInputStream(plugin.getStorageManager().toBytes(playerData)));
						statement2.setString(3, uuid.toString());
						statement2.executeUpdate();
					} catch (SQLException ex) {
						plugin.getPluginLogger().warn("Failed to update %s's data.".formatted(uuid), ex);
					}
					future.complete(true);
				} else {
					insertPlayerData(uuid, playerData, !unlock, connection);
					future.complete(true);
				}
			} catch (SQLException ex) {
				plugin.getPluginLogger().warn("Failed to get %s's data.".formatted(uuid), ex);
			}
		});
		return future;
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
			plugin.getPluginLogger().warn("Failed to get unique data.", ex);
		}
		return uuids;
	}

	/**
	 * Constants defining SQL statements used for database operations.
	 */
	public static class SqlConstants {
		public static final String SQL_SELECT_BY_UUID = "SELECT * FROM `%s` WHERE `uuid` = ?";
		public static final String SQL_SELECT_ALL_UUID = "SELECT uuid FROM `%s`";
		public static final String SQL_UPDATE_BY_UUID = "UPDATE `%s` SET `lock` = ?, `data` = ? WHERE `uuid` = ?";
		public static final String SQL_LOCK_BY_UUID = "UPDATE `%s` SET `lock` = ? WHERE `uuid` = ?";
		public static final String SQL_INSERT_DATA_BY_UUID = "INSERT INTO `%s`(`uuid`, `lock`, `data`) VALUES(?, ?, ?)";
	}
}