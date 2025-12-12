package com.swiftlicious.hellblock.logging;

/**
 * Represents the logger instance being used by Hellblock on the platform.
 *
 * <p>
 * Messages sent using the logger are sent prefixed with the Hellblock tag,
 * and on some implementations will be colored depending on the message type.
 * </p>
 */
public interface PluginLogger {

	void info(String s);

	void warn(String s);

	void warn(String s, Throwable t);

	void severe(String s);

	void severe(String s, Throwable t);
}