package com.swiftlicious.hellblock.world.wrapper;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.world.HellblockWorld;

public class WrappedInteractAirEvent {

	private final HellblockWorld<?> world;
	private final ItemStack itemInHand;
	private final String itemID;
	private final EquipmentSlot hand;
	private final Player player;

	public WrappedInteractAirEvent(HellblockWorld<?> world, Player player, EquipmentSlot hand, ItemStack itemInHand,
			String itemID) {
		this.world = world;
		this.itemInHand = itemInHand;
		this.itemID = itemID;
		this.hand = hand;
		this.player = player;
	}

	public HellblockWorld<?> world() {
		return world;
	}

	public ItemStack itemInHand() {
		return itemInHand;
	}

	public String itemID() {
		return itemID;
	}

	public EquipmentSlot hand() {
		return hand;
	}

	public Player player() {
		return player;
	}
}