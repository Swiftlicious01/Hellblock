package com.swiftlicious.hellblock.gui.schematic;

import org.bukkit.inventory.ItemStack;

public class SchematicDynamicGUIElement extends SchematicGUIElement {

	public SchematicDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}