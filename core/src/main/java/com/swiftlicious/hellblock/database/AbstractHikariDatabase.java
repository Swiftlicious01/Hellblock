package com.swiftlicious.hellblock.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * An abstract base class for SQL databases using the HikariCP connection pool,
 * which handles player data storage.
 */
public abstract class AbstractHikariDatabase extends AbstractSQLDatabase {

	private HikariDataSource dataSource;
	private String driverClass;
	private final String sqlBrand;

	public AbstractHikariDatabase(HellblockPlugin plugin) {
		super(plugin);
		this.driverClass = getStorageType() == StorageType.MariaDB ? "org.mariadb.jdbc.Driver"
				: getStorageType() == StorageType.PostgreSQL ? "org.postgresql.Driver" : "com.mysql.cj.jdbc.Driver";
		this.sqlBrand = getStorageType() == StorageType.MariaDB ? "MariaDB"
				: getStorageType() == StorageType.PostgreSQL ? "PostgreSQL" : "MySQL";
		try {
			Class.forName(this.driverClass);
		} catch (ClassNotFoundException e1) {
			if (getStorageType() == StorageType.MariaDB) {
				try {
					Class.forName("org.mariadb.jdbc.Driver");
				} catch (ClassNotFoundException e2) {
					plugin.getPluginLogger().warn("No MariaDB driver is found.");
				}
			} else if (getStorageType() == StorageType.PostgreSQL) {
				try {
					Class.forName("org.postgresql.Driver");
				} catch (ClassNotFoundException e3) {
					plugin.getPluginLogger().warn("No PostgreSQL driver is found.");
				}
			} else if (getStorageType() == StorageType.MySQL) {
				try {
					Class.forName("com.mysql.jdbc.Driver");
				} catch (ClassNotFoundException e4) {
					plugin.getPluginLogger().warn("No MySQL driver is found.");
				}
			}
		}
	}

	/**
	 * Initialize the database connection pool and create tables if they don't
	 * exist.
	 */
	@Override
	public void initialize(YamlDocument config) {
		Section section = config.getSection(sqlBrand);

		if (section == null) {
			plugin.getPluginLogger().warn(
					"Failed to load database config. It seems that your config is broken. Please regenerate a new one.");
			return;
		}

		super.tablePrefix = section.getString("table-prefix", "hellblock");
		HikariConfig hikariConfig;
		try {
			hikariConfig = new HikariConfig();
		} catch (LinkageError e) {
			handleClassloadingError(e, plugin);
			throw e;
		}
		hikariConfig.setUsername(section.getString("user", "root"));
		hikariConfig.setPassword(section.getString("password", "pa55w0rd"));
		hikariConfig.setJdbcUrl(String.format("jdbc:%s://%s:%s/%s%s", sqlBrand.toLowerCase(Locale.ENGLISH),
				section.getString("host", "localhost"), section.getString("port", "3306"),
				section.getString("database", "minecraft"), section.getString("connection-parameters")));
		hikariConfig.setDriverClassName(driverClass);
		hikariConfig.setMaximumPoolSize(section.getInt("Pool-Settings.max-pool-size", 10));
		hikariConfig.setMinimumIdle(section.getInt("Pool-Settings.min-idle", 10));
		hikariConfig.setMaxLifetime(section.getLong("Pool-Settings.max-lifetime", 180000L));
		hikariConfig.setConnectionTimeout(section.getLong("Pool-Settings.time-out", 20000L));
		hikariConfig.setPoolName("HellblockHikariPool");
		try {
			hikariConfig.setKeepaliveTime(section.getLong("Pool-Settings.keep-alive-time", 60000L));
		} catch (NoSuchMethodError ignored) {
		}

		// don't perform any initial connection validation - we subsequently call
		// #getConnection
		// to setup the schema anyways
		hikariConfig.setInitializationFailTimeout(-1);

		final Properties properties = new Properties();
		properties.putAll(Map.of("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)), "cachePrepStmts",
				"true", "prepStmtCacheSize", "250", "prepStmtCacheSqlLimit", "2048", "useServerPrepStmts", "true",
				"useLocalSessionState", "true", "useLocalTransactionState", "true"));
		properties.putAll(Map.of("rewriteBatchedStatements", "true", "cacheResultSetMetadata", "true",
				"cacheServerConfiguration", "true", "elideSetAutoCommits", "true", "maintainTimeStats", "false"));
		hikariConfig.setDataSourceProperties(properties);
		dataSource = new HikariDataSource(hikariConfig);
		super.createTableIfNotExist();
	}

	/**
	 * Disable the database by closing the connection pool.
	 */
	@Override
	public void disable() {
		if (dataSource != null && !dataSource.isClosed())
			dataSource.close();
	}

	/**
	 * Get a connection to the SQL database from the connection pool.
	 *
	 * @return A database connection.
	 * @throws SQLException If there is an error establishing a connection.
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	private static void handleClassloadingError(Throwable throwable, HellblockPlugin plugin) {
		List<String> noteworthyClasses = ImmutableList.of("org.slf4j.LoggerFactory", "org.slf4j.ILoggerFactory",
				"org.apache.logging.slf4j.Log4jLoggerFactory", "org.apache.logging.log4j.spi.LoggerContext",
				"org.apache.logging.log4j.spi.AbstractLoggerAdapter", "org.slf4j.impl.StaticLoggerBinder",
				"org.slf4j.helpers.MessageFormatter");

		plugin.getPluginLogger().warn("A " + throwable.getClass().getSimpleName()
				+ " has occurred whilst initialising Hikari. This is likely due to classloading conflicts between other plugins.");
		plugin.getPluginLogger().warn(
				"Please check for other plugins below (and try loading Hellblock without them installed) before reporting the issue.");

		for (String className : noteworthyClasses) {
			Class<?> clazz;
			try {
				clazz = Class.forName(className);
			} catch (Exception e) {
				continue;
			}

			ClassLoader loader = clazz.getClassLoader();
			String loaderName;
			try {
				loaderName = plugin.identifyClassLoader(loader) + " (" + loader.toString() + ")";
			} catch (Throwable e) {
				loaderName = loader.toString();
			}

			plugin.getPluginLogger().warn("Class " + className + " has been loaded by: " + loaderName);
		}
	}
}