package com.swiftlicious.hellblock.database;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

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
	private ExecutorService executor;

	public SQLiteHandler(HellblockPlugin plugin) {
		super(plugin);
	}

	/**
	 * Initialize the SQLite database and connection based on the configuration.
	 */
	@Override
	public void initialize(YamlDocument config) {
		final ClassLoader classLoader = plugin.getDependencyManager().obtainClassLoaderWith(
				EnumSet.of(Dependency.SQLITE_DRIVER, Dependency.SLF4J_SIMPLE, Dependency.SLF4J_API));
		try {
			final Class<?> connectionClass = classLoader.loadClass("org.sqlite.jdbc4.JDBC4Connection");
			connectionConstructor = connectionClass.getConstructor(String.class, String.class, Properties.class);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}

		this.executor = Executors.newFixedThreadPool(1,
				new ThreadFactoryBuilder().setNameFormat("hb-sqlite-%d").build());

		this.databaseFile = new File(plugin.getDataFolder(), config.getString("SQLite.file", "data") + ".db");
		super.tablePrefix = config.getString("SQLite.table-prefix", "hellblock");
		super.createTableIfNotExist();
	}

	/**
	 * Disable the SQLite database by closing the connection.
	 */
	@Override
	public void disable() {
		if (executor != null) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
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
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Failed to close database connection during shutdown", e);
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
			return connection;
		} catch (ReflectiveOperationException ex) {
			if (ex.getCause() instanceof SQLException) {
				throw (SQLException) ex.getCause();
			}
			throw new RuntimeException(ex);
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
		final var future = new CompletableFuture<Optional<PlayerData>>();
		if (executor == null) {
			executor = this.executor;
		}
		executor.execute(() -> {
			try (Connection connection = getConnection();
					PreparedStatement statement = connection
							.prepareStatement(SqlConstants.SQL_SELECT_BY_UUID.formatted(getTableName("data")))) {
				statement.setString(1, uuid.toString());
				final ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					final byte[] dataByteArray = rs.getBytes("data");
					final PlayerData data = plugin.getStorageManager().fromBytes(dataByteArray);
					data.setUUID(uuid);
					final int lockValue = rs.getInt(2);
					if (lockValue != 0 && getCurrentSeconds() - 30 <= lockValue) {
						data.setLocked(true);
						future.complete(Optional.of(data));
						return;
					}
					if (lock) {
						lockOrUnlockPlayerData(uuid, true);
					}
					future.complete(Optional.of(data));
				} else if (Bukkit.getPlayer(uuid) != null) {
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

	@Override
	public CompletableFuture<Boolean> updateOrInsertPlayerData(UUID uuid, PlayerData playerData, boolean unlock) {
		final var future = new CompletableFuture<Boolean>();
		executor.execute(() -> {
			try (Connection connection = getConnection();
					PreparedStatement statement = connection
							.prepareStatement(SqlConstants.SQL_SELECT_BY_UUID.formatted(getTableName("data")))) {
				statement.setString(1, uuid.toString());
				final ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					try (PreparedStatement statement2 = connection
							.prepareStatement(SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data")))) {
						statement2.setInt(1, unlock ? 0 : getCurrentSeconds());
						statement2.setBytes(2, plugin.getStorageManager().toBytes(playerData));
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
				future.completeExceptionally(ex);
			}
		});
		return future;
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
		final var future = new CompletableFuture<Boolean>();
		executor.execute(() -> {
			try (Connection connection = getConnection();
					PreparedStatement statement = connection
							.prepareStatement(SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data")))) {
				statement.setInt(1, unlock ? 0 : getCurrentSeconds());
				statement.setBytes(2, playerData.toBytes());
				statement.setString(3, uuid.toString());
				statement.executeUpdate();
				future.complete(true);
			} catch (SQLException ex) {
				plugin.getPluginLogger().warn("Failed to update %s's data.".formatted(uuid), ex);
				future.completeExceptionally(ex);
			}
		});
		return future;
	}

	/**
	 * Asynchronously update data for multiple players in the SQLite database.
	 *
	 * @param users  A collection of User instances to update.
	 * @param unlock Flag indicating whether to unlock the data.
	 */
	@Override
	public void updateManyPlayersData(Collection<? extends UserData> users, boolean unlock) {
		final String sql = SqlConstants.SQL_UPDATE_BY_UUID.formatted(getTableName("data"));
		try (Connection connection = getConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				for (UserData user : users) {
					statement.setInt(1, unlock ? 0 : getCurrentSeconds());
					statement.setBytes(2, plugin.getStorageManager().toBytes(user.toPlayerData()));
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
	 * Insert player data into the SQLite database.
	 *
	 * @param uuid       The UUID of the player.
	 * @param playerData The player's data to insert.
	 * @param lock       Flag indicating whether to lock the data.
	 */
	@Override
	protected void insertPlayerData(UUID uuid, PlayerData playerData, boolean lock, @Nullable Connection previous) {
		try (Connection connection = previous == null ? getConnection() : previous;
				PreparedStatement statement = connection
						.prepareStatement(SqlConstants.SQL_INSERT_DATA_BY_UUID.formatted(getTableName("data")))) {
			statement.setString(1, uuid.toString());
			statement.setInt(2, lock ? getCurrentSeconds() : 0);
			statement.setBytes(3, plugin.getStorageManager().toBytes(playerData));
			statement.execute();
		} catch (SQLException ex) {
			plugin.getPluginLogger().warn("Failed to insert %s's data.".formatted(uuid), ex);
		}
	}
}