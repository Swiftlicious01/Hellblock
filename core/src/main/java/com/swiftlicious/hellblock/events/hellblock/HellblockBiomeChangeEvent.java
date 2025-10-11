package com.swiftlicious.hellblock.events.hellblock;

import org.bukkit.block.Biome;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockBiomeChangeEvent extends Event implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();
	private final UserData user;
	private final HellblockData hellblockData;
	private final Biome oldBiome;
	private final HellBiome newBiome;
	private final boolean performedByGUI;
	private final boolean forceChange;
	private boolean cancelled;

	public HellblockBiomeChangeEvent(UserData user, HellblockData hellblockData, Biome oldBiome, HellBiome newBiome,
			boolean performedByGUI, boolean forceChange) {
		this.user = user;
		this.hellblockData = hellblockData;
		this.oldBiome = oldBiome;
		this.newBiome = newBiome;
		this.performedByGUI = performedByGUI;
		this.forceChange = forceChange;
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

	public boolean isPerformedByGUI() {
		return performedByGUI;
	}

	public boolean isForceChange() {
		return forceChange;
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
