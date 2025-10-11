package com.swiftlicious.hellblock.events.hellblock;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class HellblockRollbackEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UUID ownerId;
	private final long timestamp;

	public HellblockRollbackEvent(UUID ownerId, long timestamp) {
		this.ownerId = ownerId;
		this.timestamp = timestamp;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}