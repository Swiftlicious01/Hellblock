package com.swiftlicious.hellblock.events.fishing;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class FishingLootSpawnEvent extends PlayerEvent implements Cancellable {

	private static final HandlerList handlerList = new HandlerList();
	private final Location location;
	private final Item item;
	private boolean isCancelled;

	public FishingLootSpawnEvent(@NotNull Player who, Location location, Item item) {
		super(who);
		this.item = item;
		this.location = location;
		this.isCancelled = false;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		isCancelled = cancel;
	}

	public Location getLocation() {
		return location;
	}

	public Item getItem() {
		return item;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlerList;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}
}
