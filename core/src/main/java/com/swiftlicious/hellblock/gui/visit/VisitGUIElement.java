package com.swiftlicious.hellblock.gui.visit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class VisitGUIElement {

	private final char symbol;
	private final List<Integer> slots;
	protected ItemStack itemStack;
	protected UUID uuid;

	public VisitGUIElement(char symbol, ItemStack itemStack, UUID uuid) {
		this.symbol = symbol;
		this.itemStack = itemStack;
		this.uuid = uuid;
		this.slots = new ArrayList<>();
	}

	public VisitGUIElement(char symbol, ItemStack itemStack) {
		this(symbol, itemStack, null);
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

	// Getter method to retrieve uuid from visiting owner associated with this
	// element
	public UUID getUUID() {
		return uuid;
	}

	// Getter method to retrieve the list of slots where this element can appear
	public List<Integer> getSlots() {
		return slots;
	}
}