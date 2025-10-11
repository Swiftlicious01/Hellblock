package com.swiftlicious.hellblock.sender;

import java.util.UUID;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.extras.Tristate;

import net.kyori.adventure.text.Component;

/**
 * Wrapper interface to represent a CommandSender/CommandSource within the
 * common command implementations.
 */
public interface Sender {

	/** The uuid used by the console sender. */
	UUID CONSOLE_UUID = new UUID(0, 0); // 00000000-0000-0000-0000-000000000000
	/** The name used by the console sender. */
	String CONSOLE_NAME = "Console";

	/**
	 * Gets the plugin instance the sender is from.
	 *
	 * @return the plugin
	 */
	HellblockPlugin getPlugin();

	/**
	 * Gets the sender's username
	 *
	 * @return a friendly username for the sender
	 */
	String getName();

	/**
	 * Gets the sender's unique id.
	 *
	 * <p>
	 * See {@link #CONSOLE_UUID} for the console's UUID representation.
	 * </p>
	 *
	 * @return the sender's uuid
	 */
	UUID getUniqueId();

	/**
	 * Send a json message to the Sender.
	 *
	 * @param message the message to send.
	 */
	void sendMessage(Component message);

	/**
	 * Send a json message to the Sender.
	 *
	 * @param message     the message to send.
	 * @param ignoreEmpty whether to ignore empty component
	 */
	void sendMessage(Component message, boolean ignoreEmpty);

	/**
	 * Gets the tristate a permission is set to.
	 *
	 * @param permission the permission to check for
	 * @return a tristate
	 */
	Tristate getPermissionValue(String permission);

	/**
	 * Check if the Sender has a permission.
	 *
	 * @param permission the permission to check for
	 * @return true if the sender has the permission
	 */
	boolean hasPermission(String permission);

	/**
	 * Makes the sender perform a command.
	 *
	 * @param commandLine the command
	 */
	void performCommand(String commandLine);

	/**
	 * Gets whether this sender is the console
	 *
	 * @return if the sender is the console
	 */
	boolean isConsole();

	/**
	 * Gets whether this sender is still valid & receiving messages.
	 *
	 * @return if this sender is valid
	 */
	default boolean isValid() {
		return true;
	}
}