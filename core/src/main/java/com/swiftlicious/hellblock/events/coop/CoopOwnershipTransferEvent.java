package com.swiftlicious.hellblock.events.coop;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class CoopOwnershipTransferEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UserData oldOwner;
	private final UserData newOwner;
	private final boolean forcedByAdmin;

	public CoopOwnershipTransferEvent(UserData oldOwner, UserData newOwner, boolean forcedByAdmin) {
		this.oldOwner = oldOwner;
		this.newOwner = newOwner;
		this.forcedByAdmin = forcedByAdmin;
	}

	public UserData getOldOwner() {
		return oldOwner;
	}

	public UserData getNewOwner() {
		return newOwner;
	}

	public boolean isForcedByAdmin() {
		return forcedByAdmin;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}