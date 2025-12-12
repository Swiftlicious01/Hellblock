package com.swiftlicious.hellblock.gui.event;

import org.bukkit.inventory.ItemStack;

public class EventDynamicGUIElement extends EventGUIElement {

	public EventDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}
