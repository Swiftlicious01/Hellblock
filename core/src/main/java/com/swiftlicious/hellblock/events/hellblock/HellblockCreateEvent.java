package com.swiftlicious.hellblock.events.hellblock;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockCreateEvent extends Event implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final UserData userData;
	private final IslandOptions islandChoice;
	private final int hellblockId;
	private boolean cancelled;

	public HellblockCreateEvent(Player player, UserData userData, IslandOptions islandChoice, int hellblockId) {
		this.player = player;
		this.userData = userData;
		this.islandChoice = islandChoice;
		this.hellblockId = hellblockId;
	}

	public Player getPlayer() {
		return player;
	}

	public UserData getUserData() {
		return userData;
	}

	public IslandOptions getIslandChoice() {
		return islandChoice;
	}

	public int getHellblockId() {
		return hellblockId;
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