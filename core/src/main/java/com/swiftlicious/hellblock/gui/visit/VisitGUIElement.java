package com.swiftlicious.hellblock.gui.visit;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.gui.BaseGUIElement;

public class VisitGUIElement extends BaseGUIElement {

	protected UUID uuid;

	public VisitGUIElement(char symbol, ItemStack itemStack, UUID uuid) {
		super(symbol, itemStack);
		this.uuid = uuid;
	}

	public VisitGUIElement(char symbol, ItemStack itemStack) {
		this(symbol, itemStack, null);
	}

	// Getter method to retrieve uuid from visiting owner associated with this
	// element
	public UUID getUUID() {
		return uuid;
	}
}