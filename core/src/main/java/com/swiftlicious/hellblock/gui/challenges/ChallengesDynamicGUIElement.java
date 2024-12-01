package com.swiftlicious.hellblock.gui.challenges;

import org.bukkit.inventory.ItemStack;

public class ChallengesDynamicGUIElement extends ChallengesGUIElement {

	public ChallengesDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}
}