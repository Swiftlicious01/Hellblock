package com.swiftlicious.hellblock.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public abstract class BaseGUIElement {

	protected final char symbol;
	protected final List<Integer> slots;
	protected ItemStack itemStack;

	protected BaseGUIElement(char symbol, ItemStack itemStack) {
		this.symbol = symbol;
		this.itemStack = itemStack;
		this.slots = new ArrayList<>();
	}

	/** Adds a slot this element occupies */
	public void addSlot(int slot) {
		slots.add(slot);
	}

	/** The identifying symbol (like 'B', 'C', etc.) */
	public char getSymbol() {
		return symbol;
	}

	/** Clone the element's item for safe inventory usage */
	public ItemStack getItemStack() {
		return itemStack.clone();
	}

	/** List of inventory slot indices */
	public List<Integer> getSlots() {
		return slots;
	}

	/** Replace the underlying item stack */
	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
	}
}