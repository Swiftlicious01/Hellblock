package com.swiftlicious.hellblock.events.fishing;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

public class LavaFishingReloadEvent extends Event {

	private static final HandlerList handlerList = new HandlerList();
	private final HellblockPlugin instance;

	public LavaFishingReloadEvent(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return getHandlerList();
	}

	public HellblockPlugin getPluginInstance() {
		return instance;
	}
}