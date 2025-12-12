package com.swiftlicious.hellblock.events.invasion;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a retreat portal is spawned and the invasion is retreating.
 */
public class InvasionRetreatEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final UUID ownerId;
	private final Location portalLocation;

	public InvasionRetreatEvent(UUID ownerId, Location portalLocation) {
		this.ownerId = ownerId;
		this.portalLocation = portalLocation;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public Location getPortalLocation() {
		return portalLocation;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}