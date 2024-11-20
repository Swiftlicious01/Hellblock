package com.swiftlicious.hellblock.events.fishing;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * This class represents an event that is triggered when the Hellblock plugin is
 * reloaded.
 */
public class LavaFishingReloadEvent extends Event {

	private static final HandlerList handlerList = new HandlerList();

	private final HellblockPlugin plugin;

	/**
	 * Constructs a new LavaFishingReloadEvent.
	 *
	 * @param plugin The instance of the Hellblock plugin that is being reloaded
	 */
	public LavaFishingReloadEvent(HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Gets the instance of the {@link HellblockPlugin} that is being reloaded.
	 *
	 * @return The instance of the Hellblock plugin
	 */
	public HellblockPlugin getPluginInstance() {
		return plugin;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return getHandlerList();
	}
}