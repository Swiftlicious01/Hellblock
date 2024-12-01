package com.swiftlicious.hellblock.gui.invite;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class InviteDynamicGUIElement extends InviteGUIElement {

	public InviteDynamicGUIElement(char symbol, ItemStack itemStack, UUID uuid) {
		super(symbol, itemStack, uuid);
	}

	public InviteDynamicGUIElement(char symbol, ItemStack itemStack) {
		super(symbol, itemStack);
	}

	public void setItemStack(ItemStack itemStack) {
		super.itemStack = itemStack;
	}

	public void setUUID(UUID uuid) {
		super.uuid = uuid;
	}
}