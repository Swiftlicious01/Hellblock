package com.swiftlicious.hellblock.events.coop;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.UserData;

public class CoopInviteSendEvent extends Event implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UserData owner;
	private final UserData invited;
	private boolean cancelled;

	public CoopInviteSendEvent(UserData owner, UserData invited) {
		this.owner = owner;
		this.invited = invited;
	}

	public UserData getOwner() {
		return owner;
	}

	public UserData getInvited() {
		return invited;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}