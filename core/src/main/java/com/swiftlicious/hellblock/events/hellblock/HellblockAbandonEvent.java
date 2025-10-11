package com.swiftlicious.hellblock.events.hellblock;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.HellblockData;

public class HellblockAbandonEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UUID ownerId;
	private final HellblockData hellblockData;

	public HellblockAbandonEvent(UUID ownerId, HellblockData hellblockData) {
		this.ownerId = ownerId;
		this.hellblockData = hellblockData;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public HellblockData getHellblockData() {
		return hellblockData;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}