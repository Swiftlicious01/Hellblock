package com.swiftlicious.hellblock.events.coop;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class CoopLeaveEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UserData leavingPlayer;
	private final UUID ownerId;

	public CoopLeaveEvent(UserData leavingPlayer, UUID ownerId) {
		this.leavingPlayer = leavingPlayer;
		this.ownerId = ownerId;
	}

	public UserData getLeavingPlayer() {
		return leavingPlayer;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}