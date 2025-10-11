package com.swiftlicious.hellblock.gui.flags;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the flags menu
 */
public interface FlagsGUIManagerInterface extends Reloadable {

	/**
	 * Opens the flags GUI for the specified player.
	 *
	 * @param player  the {@link Player} for whom the flags GUI will be opened
	 * @param isOwner whether or not they're the owner of the hellblock
	 * @return true if the flags GUI was successfully opened, false otherwise
	 */
	boolean openFlagsGUI(Player player, boolean isOwner);
}