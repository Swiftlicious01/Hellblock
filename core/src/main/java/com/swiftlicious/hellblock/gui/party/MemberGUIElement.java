package com.swiftlicious.hellblock.gui.party;

import org.bukkit.inventory.ItemStack;

public class MemberGUIElement {

	private final ItemStack itemStack;
	private final Runnable clickAction;

	public MemberGUIElement(ItemStack itemStack, Runnable clickAction) {
		this.itemStack = itemStack;
		this.clickAction = clickAction;
	}

	public MemberGUIElement(ItemStack itemStack) {
		this(itemStack, null);
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public void onClick() {
		if (clickAction != null)
			clickAction.run();
	}
}