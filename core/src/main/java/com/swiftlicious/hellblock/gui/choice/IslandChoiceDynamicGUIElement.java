package com.swiftlicious.hellblock.gui.choice;

import org.bukkit.inventory.ItemStack;

public class IslandChoiceDynamicGUIElement extends IslandChoiceGUIElement {

	public IslandChoiceDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}