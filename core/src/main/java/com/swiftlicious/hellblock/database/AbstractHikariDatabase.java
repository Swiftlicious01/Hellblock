package com.swiftlicious.hellblock.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;
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
	private String sqlBrand;

	protected final StorageType storageType;

	protected AbstractHikariDatabase(HellblockPlugin plugin, StorageType type) {
		super(plugin);
		this.storageType = type;

		this.driverClass = switch (type) {
		case MariaDB -> "org.mariadb.jdbc.Driver";
		case PostgreSQL -> "org.postgresql.Driver";
		default -> "com.mysql.cj.jdbc.Driver";
		};

		this.sqlBrand = switch (type) {
		case MariaDB -> "MariaDB";
		case PostgreSQL -> "PostgreSQL";
		default -> "MySQL";
		};

		try {
			Class.forName(driverClass);
		} catch (ClassNotFoundException e) {
			plugin.getPluginLogger().warn("No JDBC driver found for " + sqlBrand + ".");
		}
	}

	@Override
	public final StorageType getStorageType() {
		return storageType;
	}

	/**
	 * Initialize the database connection pool and create tables if they don't
	 * exist.
	 */
	@Override
	public void initialize(YamlDocument config) {
		final Section section = config.getSection(sqlBrand);
		if (section == null) {
			plugin.getPluginLogger().warn(
					"Failed to load database config. It seems that your config is broken. Please regenerate a new one.");
			return;
		}

		// Which dependencies your database setup requires
		Set<Dependency> sqlDeps = EnumSet.of(Dependency.HIKARI_CP, switch (sqlBrand.toUpperCase(Locale.ENGLISH)) {
		case "MYSQL" -> Dependency.MYSQL_DRIVER;
		case "MARIADB" -> Dependency.MARIADB_DRIVER;
		case "POSTGRESQL" -> Dependency.POSTGRESQL_DRIVER;
		default -> throw new IllegalArgumentException("Unknown database type: " + sqlBrand);
		});

		plugin.getDependencyManager().runWithLoader(sqlDeps, () -> {
			// Now we can safely use Hikari classes
			final HikariConfig hikariConfig = new HikariConfig();

			super.tablePrefix = section.getString("table-prefix", "hellblock");

			hikariConfig.setUsername(section.getString("user", "root"));
			hikariConfig.setPassword(section.getString("password", "pa55w0rd"));
			hikariConfig.setJdbcUrl("jdbc:%s://%s:%s/%s%s".formatted(sqlBrand.toLowerCase(Locale.ENGLISH),
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

			// Now that the classloader context is correct, this will work fine
			dataSource = new HikariDataSource(hikariConfig);

			super.createTableIfNotExist();
			return null;
		});
	}

	/**
	 * Disable the database by closing the connection pool.
	 */
	@Override
	public void disable() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
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

	@SuppressWarnings("unused")
	private static void handleClassloadingError(Throwable throwable, HellblockPlugin plugin) {
		final List<String> noteworthyClasses = ImmutableList.of("org.slf4j.LoggerFactory", "org.slf4j.ILoggerFactory",
				"org.apache.logging.slf4j.Log4jLoggerFactory", "org.apache.logging.log4j.spi.LoggerContext",
				"org.apache.logging.log4j.spi.AbstractLoggerAdapter", "org.slf4j.impl.StaticLoggerBinder",
				"org.slf4j.helpers.MessageFormatter");

		plugin.getPluginLogger().warn("A " + throwable.getClass().getSimpleName()
				+ " has occurred whilst initialising Hikari. This is likely due to classloading conflicts between other plugins.");
		plugin.getPluginLogger().warn(
				"Please check for other plugins below (and try loading Hellblock without them installed) before reporting the issue.");

		for (String className : noteworthyClasses) {
			final Class<?> clazz;
			try {
				clazz = Class.forName(className);
			} catch (Exception e) {
				continue;
			}

			final ClassLoader loader = clazz.getClassLoader();
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