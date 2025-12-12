package com.swiftlicious.hellblock.events.invasion;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.listeners.invasion.InvasionFormation.FormationType;

/**
 * Fired when the mob formation is changed during an active invasion.
 */
public class InvasionChangeFormationEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final UUID ownerId;
	private final FormationType formation;

	public InvasionChangeFormationEvent(UUID ownerId, FormationType formation) {
		this.ownerId = ownerId;
		this.formation = formation;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public FormationType getFormation() {
		return formation;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
