package com.swiftlicious.hellblock.events.hellblock;

import org.bukkit.block.Biome;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockBiomeChangedEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UserData user;
	private final HellblockData hellblockData;
	private final Biome oldBiome;
	private final HellBiome newBiome;

	public HellblockBiomeChangedEvent(UserData user, HellblockData hellblockData, Biome oldBiome, HellBiome newBiome) {
		this.user = user;
		this.hellblockData = hellblockData;
		this.oldBiome = oldBiome;
		this.newBiome = newBiome;
	}

	public UserData getUser() {
		return user;
	}

	public HellblockData getHellblockData() {
		return hellblockData;
	}

	public Biome getOldBiome() {
		return oldBiome;
	}

	public HellBiome getNewBiome() {
		return newBiome;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
