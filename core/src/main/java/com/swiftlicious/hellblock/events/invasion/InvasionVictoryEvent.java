package com.swiftlicious.hellblock.events.invasion;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.listeners.invasion.InvasionProfile;

/**
 * Fired when a player successfully completes an invasion.
 */
public class InvasionVictoryEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final UUID ownerId;
	private final InvasionProfile profile;

	public InvasionVictoryEvent(UUID ownerId, InvasionProfile profile) {
		this.ownerId = ownerId;
		this.profile = profile;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public InvasionProfile getProfile() {
		return profile;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}