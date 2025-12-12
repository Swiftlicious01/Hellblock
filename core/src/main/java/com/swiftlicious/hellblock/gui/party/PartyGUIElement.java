package com.swiftlicious.hellblock.gui.party;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.gui.BaseGUIElement;

public class PartyGUIElement extends BaseGUIElement {

	protected UUID uuid;

	public PartyGUIElement(char symbol, ItemStack itemStack, UUID uuid) {
		super(symbol, itemStack);
		this.uuid = uuid;
	}

	public PartyGUIElement(char symbol, ItemStack itemStack) {
		this(symbol, itemStack, null);
	}

	// Getter method to retrieve uuid from party member associated with this element
	public UUID getUUID() {
		return uuid;
	}
}