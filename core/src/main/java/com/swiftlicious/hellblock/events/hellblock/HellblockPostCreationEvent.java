package com.swiftlicious.hellblock.events.hellblock;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class HellblockPostCreationEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UserData userData;

	public HellblockPostCreationEvent(UserData userData) {
		this.userData = userData;
	}

	public UserData getUserData() {
		return userData;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}