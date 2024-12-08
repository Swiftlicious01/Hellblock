package com.swiftlicious.hellblock.world.wrapper;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.world.HellblockWorld;

public class WrappedBreakEvent {

	private final Entity entity;
	private final Block block;
	private final Location location;
	private final String brokenID;
	private final ItemStack itemInHand;
	private final String itemID;
	private final Cancellable event;
	private final HellblockWorld<?> world;
	private final BreakReason reason;
	private final EquipmentSlot hand;

	public WrappedBreakEvent(@Nullable Entity entityBreaker, @Nullable Block blockBreaker, @Nullable EquipmentSlot hand,
			Location location, String brokenID, ItemStack itemInHand, String itemID, BreakReason reason,
			HellblockWorld<?> world, Cancellable event) {
		this.entity = entityBreaker;
		this.block = blockBreaker;
		this.location = location;
		this.brokenID = brokenID;
		this.hand = hand;
		this.itemInHand = itemInHand;
		this.itemID = itemID;
		this.event = event;
		this.world = world;
		this.reason = reason;
	}

	@NotNull
	public BreakReason reason() {
		return reason;
	}

	@NotNull
	public HellblockWorld<?> world() {
		return world;
	}

	@NotNull
	public Location location() {
		return location;
	}

	@NotNull
	public String brokenID() {
		return brokenID;
	}

	@Nullable
	public ItemStack itemInHand() {
		return itemInHand;
	}

	@Nullable
	public String itemID() {
		return itemID;
	}

	@Nullable
	public EquipmentSlot hand() {
		return hand;
	}

	public boolean isCancelled() {
		return event.isCancelled();
	}

	public void setCancelled(boolean cancel) {
		event.setCancelled(cancel);
	}

	@Nullable
	public Player playerBreaker() {
		if (entity instanceof Player player) {
			return player;
		}
		return null;
	}

	@Nullable
	public Entity entityBreaker() {
		return entity;
	}

	@Nullable
	public Block blockBreaker() {
		return block;
	}
}