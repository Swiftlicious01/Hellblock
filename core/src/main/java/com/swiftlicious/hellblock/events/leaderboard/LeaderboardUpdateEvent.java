package com.swiftlicious.hellblock.events.leaderboard;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

public class LeaderboardUpdateEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();

	private final Map<Integer, Float> topIslands;

	public LeaderboardUpdateEvent(Map<Integer, Float> topIslands) {
		this.topIslands = topIslands;
	}

	public Map<Integer, Float> getTopIslands() {
		return topIslands;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}