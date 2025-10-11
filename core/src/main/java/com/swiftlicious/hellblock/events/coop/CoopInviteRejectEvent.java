package com.swiftlicious.hellblock.events.coop;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class CoopInviteRejectEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UUID ownerId;
	private final UserData rejectingPlayer;

	public CoopInviteRejectEvent(UUID ownerId, UserData rejectingPlayer) {
		this.ownerId = ownerId;
		this.rejectingPlayer = rejectingPlayer;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public UserData getRejectingPlayer() {
		return rejectingPlayer;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}