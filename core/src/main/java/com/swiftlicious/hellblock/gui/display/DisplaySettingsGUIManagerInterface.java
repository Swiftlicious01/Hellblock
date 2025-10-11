package com.swiftlicious.hellblock.gui.display;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the display settings menu
 */
public interface DisplaySettingsGUIManagerInterface extends Reloadable {

	/**
	 * Opens the display settings GUI for the specified player.
	 *
	 * @param player  the {@link Player} for whom the display settings GUI will be
	 *                opened
	 * @param isOwner whether or not they're the owner of the hellblock
	 * @return true if the display settings GUI was successfully opened, false
	 *         otherwise
	 */
	boolean openDisplaySettingsGUI(Player player, boolean isOwner);
}