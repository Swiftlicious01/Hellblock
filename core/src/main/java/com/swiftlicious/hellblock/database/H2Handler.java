package com.swiftlicious.hellblock.database;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * An implementation of AbstractSQLDatabase that uses the H2 embedded database
 * for player data storage.
 */
public class H2Handler extends AbstractSQLDatabase {

	private Object connectionPool;
	private Method disposeMethod;
	private Method getConnectionMethod;
	private boolean disposed = false;

	public H2Handler(HellblockPlugin plugin) {
		super(plugin);
	}

	/**
	 * Initialize the H2 database and connection pool based on the configuration.
	 */
	@Override
	public void initialize(@NotNull YamlDocument config) {
		final File databaseFile = new File(plugin.getDataFolder(), config.getString("H2.file", "data.db"));
		super.tablePrefix = config.getString("H2.table-prefix", "hellblock");
		this.disposed = false;

		final String url = "jdbc:h2:%s;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1".formatted(databaseFile.getAbsolutePath());
		final ClassLoader classLoader = plugin.getDependencyManager()
				.obtainClassLoaderWith(EnumSet.of(Dependency.H2_DRIVER));

		try {
			final Class<?> connectionClass = classLoader.loadClass("org.h2.jdbcx.JdbcConnectionPool");
			final Method createPoolMethod = connectionClass.getMethod("create", String.class, String.class,
					String.class);
			this.connectionPool = createPoolMethod.invoke(null, url, "sa", "");
			this.disposeMethod = connectionClass.getMethod("dispose");
			this.getConnectionMethod = connectionClass.getMethod("getConnection");
		} catch (ReflectiveOperationException ex) {
			plugin.getPluginLogger().severe("Failed to initialize H2 connection pool.", ex);
			throw new RuntimeException("Could not initialize H2 JDBC connection pool.", ex);
		}

		try {
			super.createTableIfNotExist();
			plugin.getPluginLogger().info("Initialized H2 storage at " + databaseFile.getAbsolutePath());
		} catch (Exception ex) {
			plugin.getPluginLogger().severe("Failed to initialize H2 schema.", ex);
			throw new RuntimeException("Could not create H2 schema.", ex);
		}
	}

	/**
	 * Disable the H2 database by disposing of the connection pool.
	 */
	@Override
	public void disable() {
		if (connectionPool != null && !disposed) {
			try {
				disposeMethod.invoke(connectionPool);
			} catch (ReflectiveOperationException ex) {
				plugin.getPluginLogger().warn("Failed to dispose H2 connection pool.", ex);
			}
		}
		disposed = true; // Mark as disposed, no matter what
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.H2;
	}

	@NotNull
	@Override
	public Connection getConnection() {
		if (connectionPool == null || getConnectionMethod == null) {
			throw new IllegalStateException("H2 connection pool not initialized.");
		}
		if (disposed) {
			throw new IllegalStateException("H2 connection pool has already been disposed.");
		}
		try {
			return (Connection) getConnectionMethod.invoke(connectionPool);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to establish H2 connection.", ex);
		}
	}
}