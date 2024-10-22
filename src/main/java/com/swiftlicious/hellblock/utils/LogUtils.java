package com.swiftlicious.hellblock.utils;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

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
		Bukkit.getServer().getLogger().info("[Hellblock] " + message);
	}

	/**
	 * Log a warning message.
	 *
	 * @param message The message to log.
	 */
	public static void warn(@NotNull String message) {
		Bukkit.getServer().getLogger().warning("[Hellblock] " + message);
	}

	/**
	 * Log a severe error message.
	 *
	 * @param message The message to log.
	 */
	public static void severe(@NotNull String message) {
		Bukkit.getServer().getLogger().severe("[Hellblock] " + message);
	}

	/**
	 * Log a warning message with a throwable exception.
	 *
	 * @param message   The message to log.
	 * @param throwable The throwable exception to log.
	 */
	public static void warn(@NotNull String message, Throwable throwable) {
		Bukkit.getServer().getLogger().log(Level.WARNING, "[Hellblock] " + message, throwable);
	}

	/**
	 * Log a severe error message with a throwable exception.
	 *
	 * @param message   The message to log.
	 * @param throwable The throwable exception to log.
	 */
	public static void severe(@NotNull String message, Throwable throwable) {
		Bukkit.getServer().getLogger().log(Level.SEVERE, "[Hellblock] " + message, throwable);
	}

	private LogUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}
}