package com.swiftlicious.hellblock.gui.display;

import org.bukkit.inventory.ItemStack;

public class DisplaySettingsDynamicGUIElement extends DisplaySettingsGUIElement {

	public DisplaySettingsDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}