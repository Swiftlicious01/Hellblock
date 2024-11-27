package com.swiftlicious.hellblock.gui.reset;

import org.bukkit.inventory.ItemStack;

public class ResetConfirmDynamicGUIElement extends ResetConfirmGUIElement {

	public ResetConfirmDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}