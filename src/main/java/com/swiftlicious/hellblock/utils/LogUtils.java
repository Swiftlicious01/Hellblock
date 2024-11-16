package com.swiftlicious.hellblock.utils;

import java.util.logging.Level;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * Utility class for logging messages with various log levels.
 */
public final class LogUtils {

	/**
	 * Log an informational message.
	 *
	 * @param message The message to log.
	 */
	public static void info(@NotNull String message) {
		HellblockPlugin.getInstance().getLogger().info(message);
	}

	/**
	 * Log a warning message.
	 *
	 * @param message The message to log.
	 */
	public static void warn(@NotNull String message) {
		HellblockPlugin.getInstance().getLogger().warning(message);
	}

	/**
	 * Log a severe error message.
	 *
	 * @param message The message to log.
	 */
	public static void severe(@NotNull String message) {
		HellblockPlugin.getInstance().getLogger().severe(message);
	}

	/**
	 * Log a warning message with a throwable exception.
	 *
	 * @param message   The message to log.
	 * @param throwable The throwable exception to log.
	 */
	public static void warn(@NotNull String message, Throwable throwable) {
		HellblockPlugin.getInstance().getLogger().log(Level.WARNING, message, throwable);
	}

	/**
	 * Log a severe error message with a throwable exception.
	 *
	 * @param message   The message to log.
	 * @param throwable The throwable exception to log.
	 */
	public static void severe(@NotNull String message, Throwable throwable) {
		HellblockPlugin.getInstance().getLogger().log(Level.SEVERE, message, throwable);
	}

	private LogUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}
}