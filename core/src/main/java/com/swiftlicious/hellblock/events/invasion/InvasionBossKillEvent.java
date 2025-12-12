package com.swiftlicious.hellblock.events.invasion;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a boss mob is killed during an invasion.
 */
public class InvasionBossKillEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final UUID ownerId;
	private final LivingEntity bossMob;

	public InvasionBossKillEvent(UUID ownerId, LivingEntity bossMob) {
		this.ownerId = ownerId;
		this.bossMob = bossMob;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public LivingEntity getBossMob() {
		return bossMob;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}