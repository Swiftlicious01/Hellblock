package com.swiftlicious.hellblock.gui.schematic;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the schematic menu
 */
public interface SchematicGUIManagerInterface extends Reloadable {

	/**
	 * Opens the schematic GUI for the specified player.
	 *
	 * @param player  the {@link Player} for whom the schematic GUI will be opened
	 * @param isReset whether the player opening the menu is resetting their island
	 *                or not.
	 * @return true if the schematic GUI was successfully opened, false otherwise
	 */
	boolean openSchematicGUI(Player player, boolean isReset);
}