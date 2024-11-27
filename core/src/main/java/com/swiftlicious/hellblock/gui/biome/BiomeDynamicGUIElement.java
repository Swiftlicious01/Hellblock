package com.swiftlicious.hellblock.gui.biome;

import org.bukkit.inventory.ItemStack;

public class BiomeDynamicGUIElement extends BiomeGUIElement {

	public BiomeDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}