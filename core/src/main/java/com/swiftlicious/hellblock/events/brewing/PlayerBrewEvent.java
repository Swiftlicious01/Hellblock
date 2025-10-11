package com.swiftlicious.hellblock.events.brewing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

public class PlayerBrewEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private final Block brewingStand;
	private final BrewerInventory inventory;
	private final List<ItemStack> resultPotions;
	private boolean cancelled;

	public PlayerBrewEvent(Player player, Block brewingStand, BrewerInventory inventory) {
		this.player = player;
		this.brewingStand = brewingStand;
		this.inventory = inventory;

		// Capture brewed potions at the time the event is created
		this.resultPotions = Arrays.asList(safeClone(inventory.getItem(0)), safeClone(inventory.getItem(1)),
				safeClone(inventory.getItem(2)));
	}

	public Player getPlayer() {
		return player;
	}

	public Block getBrewingStand() {
		return brewingStand;
	}

	public BrewerInventory getInventory() {
		return inventory;
	}

	/**
	 * Returns an immutable list of brewed potions (slots 0,1,2). Missing/empty
	 * slots will be null.
	 */
	public List<ItemStack> getResultPotions() {
		return Collections.unmodifiableList(resultPotions);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	// Helper: avoid direct references to inventory stacks
	private static ItemStack safeClone(ItemStack stack) {
		return stack == null ? null : stack.clone();
	}
}