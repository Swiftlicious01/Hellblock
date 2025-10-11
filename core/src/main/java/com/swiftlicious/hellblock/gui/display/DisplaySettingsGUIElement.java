package com.swiftlicious.hellblock.gui.display;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public class DisplaySettingsGUIElement {

	private final char symbol;
	private final List<Integer> slots;
	protected ItemStack itemStack;

	public DisplaySettingsGUIElement(char symbol, ItemStack itemStack) {
		this.symbol = symbol;
		this.itemStack = itemStack;
		this.slots = new ArrayList<>();
	}

	// Method to add a slot to the list of slots for this element
	public void addSlot(int slot) {
		slots.add(slot);
	}

	// Getter method to retrieve the symbol associated with this element
	public char getSymbol() {
		return symbol;
	}

	// Getter method to retrieve the cloned ItemStack associated with this element
	public ItemStack getItemStack() {
		return itemStack.clone();
	}

	// Getter method to retrieve the list of slots where this element can appear
	public List<Integer> getSlots() {
		return slots;
	}
}
