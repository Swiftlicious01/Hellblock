package com.swiftlicious.hellblock.gui.notification;

import org.bukkit.inventory.ItemStack;

public class NotificationDynamicGUIElement extends NotificationGUIElement {

	public NotificationDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}