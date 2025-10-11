package com.swiftlicious.hellblock.events.hellblock;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class HellblockResetEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UUID ownerId;
	private final UserData userData;
	private final boolean forced;

	public HellblockResetEvent(UUID ownerId, UserData userData, boolean forced) {
		this.ownerId = ownerId;
		this.userData = userData;
		this.forced = forced;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public UserData getUserData() {
		return userData;
	}

	public boolean isForced() {
		return forced;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}