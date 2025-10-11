package com.swiftlicious.hellblock.gui.upgrade;

import org.bukkit.inventory.ItemStack;

public class UpgradeDynamicGUIElement extends UpgradeGUIElement {

	public UpgradeDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}