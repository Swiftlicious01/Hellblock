package com.swiftlicious.hellblock.gui.hellblock;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a holder for the Hellblock GUI inventory. This class is used to
 * associate the Hellblock GUI's inventory with an object.
 */
public class HellblockGUIHolder implements InventoryHolder {

	private Inventory inventory;

	/**
	 * Sets the inventory associated with this holder.
	 *
	 * @param inventory The inventory to associate with this holder.
	 */
	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	/**
	 * Retrieves the inventory associated with this holder.
	 *
	 * @return The associated inventory.
	 */
	@Override
	public @NotNull Inventory getInventory() {
		return inventory;
	}
}