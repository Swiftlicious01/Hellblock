package com.swiftlicious.hellblock.events.hellblock;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockPostCreationEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final UserData userData;
	private final HellblockData hellblockData;

	public HellblockPostCreationEvent(Player player, UserData userData, HellblockData hellblockData) {
		this.player = player;
		this.userData = userData;
		this.hellblockData = hellblockData;
	}

	public Player getPlayer() {
		return player;
	}

	public UserData getUserData() {
		return userData;
	}

	public HellblockData getHellblockData() {
		return hellblockData;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}