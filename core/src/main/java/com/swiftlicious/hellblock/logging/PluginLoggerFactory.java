package com.swiftlicious.hellblock.logging;

import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;

public final class PluginLoggerFactory {

	private PluginLoggerFactory() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Creates a PluginLogger for the given name, choosing the best available
	 * logging backend.
	 */
	public static PluginLogger create(String name) {
		// Prefer SLF4J if present
		try {
			Class.forName("org.slf4j.LoggerFactory");
			org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(name);
			return new Slf4jPluginLogger(slf4jLogger);
		} catch (ClassNotFoundException ignored) {
		}

		// Try Log4j next
		try {
			Class.forName("org.apache.logging.log4j.LogManager");
			org.apache.logging.log4j.Logger log4jLogger = LogManager.getLogger(name);
			return new Log4jPluginLogger(log4jLogger);
		} catch (ClassNotFoundException ignored) {
		}

		//️⃣ Fallback: use Java Util Logging
		Logger julLogger = Logger.getLogger(name);
		return new JavaPluginLogger(julLogger);
	}

	/**
	 * Overload that uses an existing java.util.logging.Logger (for
	 * Bukkit/Spigot/etc.)
	 */
	public static PluginLogger fromJavaLogger(Logger javaLogger) {
		return new JavaPluginLogger(javaLogger);
	}

	/**
	 * Overload that wraps an existing SLF4J Logger directly.
	 */
	public static PluginLogger fromSlf4j(org.slf4j.Logger logger) {
		return new Slf4jPluginLogger(logger);
	}

	/**
	 * Overload that wraps an existing Log4j Logger directly.
	 */
	public static PluginLogger fromLog4j(org.apache.logging.log4j.Logger logger) {
		return new Log4jPluginLogger(logger);
	}
}