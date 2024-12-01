package com.swiftlicious.hellblock.gui.party;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class PartyDynamicGUIElement extends PartyGUIElement {

	public PartyDynamicGUIElement(char symbol, ItemStack itemStack, UUID uuid) {
		super(symbol, itemStack, uuid);
	}

	public PartyDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}

	public void setUUID(UUID uuid) {
		super.uuid = uuid;
	}
}