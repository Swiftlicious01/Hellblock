package com.swiftlicious.hellblock.gui.event;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the event menu
 */
public interface EventGUIManagerInterface extends Reloadable {

	/**
	 * Opens the event GUI for the specified player.
	 *
	 * @param player   the {@link Player} for whom the event GUI will be opened.
	 * @param islandId the islandId of the island owner.
	 * @param isOwner  whether the player who opened the menu is the owner of the
	 *                 hellblock or not.
	 * @return true if the event GUI was successfully opened, false otherwise
	 */
	boolean openEventGUI(Player player, int islandId, boolean isOwner);
}