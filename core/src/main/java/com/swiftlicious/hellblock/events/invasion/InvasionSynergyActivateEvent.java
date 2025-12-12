package com.swiftlicious.hellblock.events.invasion;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.listeners.invasion.InvasionSynergy.SynergyPattern;

/**
 * Fired when a synergy pattern is successfully activated during an invasion.
 */
public class InvasionSynergyActivateEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final UUID leaderId;
	private final SynergyPattern pattern;

	public InvasionSynergyActivateEvent(UUID leaderId, SynergyPattern pattern) {
		this.leaderId = leaderId;
		this.pattern = pattern;
	}

	public UUID getLeaderId() {
		return leaderId;
	}

	public SynergyPattern getPattern() {
		return pattern;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}