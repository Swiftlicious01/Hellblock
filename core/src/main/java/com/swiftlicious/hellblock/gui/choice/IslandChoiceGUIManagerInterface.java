package com.swiftlicious.hellblock.gui.choice;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the island choice menu
 */
public interface IslandChoiceGUIManagerInterface extends Reloadable {

	/**
	 * Opens the island choice GUI for the specified player.
	 *
	 * @param player  the {@link Player} for whom the island choice GUI will be
	 *                opened
	 * @param isReset whether the player opening the menu is resetting their island
	 *                or not.
	 * @return true if the island choice GUI was successfully opened, false
	 *         otherwise
	 */
	boolean openIslandChoiceGUI(Player player, boolean isReset);
}