package com.swiftlicious.hellblock.utils;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/**
 * Utility class for handling events.
 */
public class EventUtils {

	/**
	 * Fires an event and does not wait for any result.
	 *
	 * @param event the {@link Event} to be fired
	 */
	public static void fireAndForget(Event event) {
		Bukkit.getPluginManager().callEvent(event);
	}

	/**
	 * Fires an event and checks if it is cancelled. This method only accepts events
	 * that implement {@link Cancellable}.
	 *
	 * @param event the {@link Event} to be fired
	 * @return true if the event is cancelled, false otherwise
	 * @throws IllegalArgumentException if the event is not cancellable
	 */
	public static boolean fireAndCheckCancel(Event event) {
		if (!(event instanceof Cancellable cancellable)) {
			throw new IllegalArgumentException("Only cancellable events are allowed here");
		}
		Bukkit.getPluginManager().callEvent(event);
		return cancellable.isCancelled();
	}
}