package com.swiftlicious.hellblock.events.coop;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class CoopTrustAddEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final UserData owner;
	private final UUID trusted;

	public CoopTrustAddEvent(UserData owner, UUID trusted) {
		this.owner = owner;
		this.trusted = trusted;
	}

	public UserData getOwner() {
		return owner;
	}

	public UUID getTrusted() {
		return trusted;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}