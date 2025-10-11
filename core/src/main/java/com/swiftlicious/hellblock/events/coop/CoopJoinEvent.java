package com.swiftlicious.hellblock.events.coop;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class CoopJoinEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UUID ownerId;
	private final UserData newMember;

	public CoopJoinEvent(UUID ownerId, UserData newMember) {
		this.ownerId = ownerId;
		this.newMember = newMember;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public UserData getNewMember() {
		return newMember;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}