package com.swiftlicious.hellblock.events.coop;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class CoopTrustRemoveEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final UserData owner;
	private final UUID untrusted;

	public CoopTrustRemoveEvent(UserData owner, UUID untrusted) {
		this.owner = owner;
		this.untrusted = untrusted;
	}

	public UserData getOwner() {
		return owner;
	}

	public UUID getUntrusted() {
		return untrusted;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}