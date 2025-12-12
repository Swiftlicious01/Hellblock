package com.swiftlicious.hellblock.gui.party;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;

public abstract class MemberGUI {

	protected final Player player;
	protected final String[] layout;
	protected final String title;

	protected final Map<Character, MemberGUIElement> symbolMap = new HashMap<>();
	protected final Map<Integer, MemberGUIElement> slotMap = new HashMap<>();

	protected final Inventory inventory;

	public MemberGUI(Player player, String title, String[] layout) {
		this.player = player;
		this.layout = layout;
		this.title = title;

		int size = layout.length * 9;
		MemberHolder holder = new MemberHolder();
		this.inventory = Bukkit.createInventory(holder, size);
		holder.setInventory(this.inventory);
	}

	/**
	 * Hook to build the GUI before showing.
	 */
	public abstract void build();

	/**
	 * Adds an element to a symbol.
	 */
	public void addElement(char symbol, MemberGUIElement element) {
		symbolMap.put(symbol, element);
	}

	/**
	 * Maps the symbol layout to actual inventory slots.
	 */
	public MemberGUI buildLayout() {
		int row = 0;
		for (String line : layout) {
			for (int col = 0; col < 9; col++) {
				char symbol = col < line.length() ? line.charAt(col) : ' ';
				MemberGUIElement element = symbolMap.get(symbol);
				if (element != null) {
					int slot = row * 9 + col;
					inventory.setItem(slot, element.getItemStack());
					slotMap.put(slot, element);
				}
			}
			row++;
		}
		return this;
	}

	/**
	 * Opens the GUI and sets the formatted title.
	 */
	public void show() {
		player.openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(player,
				AdventureHelper.componentToJson(AdventureHelper.parseCenteredTitleMultiline(title)));
	}

	/**
	 * Refreshes the display of all registered elements.
	 */
	public void refresh() {
		slotMap.forEach((slot, element) -> inventory.setItem(slot, element.getItemStack()));
	}

	/**
	 * Clears the GUI for rebuilding.
	 */
	public void clear() {
		symbolMap.clear();
		slotMap.clear();
		inventory.clear();
	}

	/**
	 * Gets the GUI element in a given slot.
	 */
	public MemberGUIElement getElement(int slot) {
		return slotMap.get(slot);
	}

	/**
	 * Custom holder (optional for type safety).
	 */
	public static class MemberHolder implements InventoryHolder {
		private Inventory inventory;

		public void setInventory(Inventory inventory) {
			this.inventory = inventory;
		}

		@Override
		public Inventory getInventory() {
			return inventory;
		}
	}
}