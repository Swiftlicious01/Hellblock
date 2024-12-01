package com.swiftlicious.hellblock.gui.flags;

import org.bukkit.inventory.ItemStack;

public class FlagsDynamicGUIElement extends FlagsGUIElement {

	public FlagsDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}