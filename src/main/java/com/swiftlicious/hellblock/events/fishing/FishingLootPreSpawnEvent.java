package com.swiftlicious.hellblock.events.fishing;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FishingLootPreSpawnEvent extends PlayerEvent implements Cancellable {

	private static final HandlerList handlerList = new HandlerList();
	private final Location location;
	private final ItemStack itemStack;
	private boolean isCancelled;

	public FishingLootPreSpawnEvent(@NotNull Player who, Location location, ItemStack itemStack) {
		super(who);
		this.itemStack = itemStack;
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

	public ItemStack getItemStack() {
		return itemStack;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlerList;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}
}
