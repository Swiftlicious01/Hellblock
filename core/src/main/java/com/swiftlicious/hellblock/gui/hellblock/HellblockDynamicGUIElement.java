package com.swiftlicious.hellblock.gui.hellblock;

import org.bukkit.inventory.ItemStack;

public class HellblockDynamicGUIElement extends HellblockGUIElement {

	public HellblockDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}