package com.swiftlicious.hellblock.gui.hellblock;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the hellblock menu
 */
public interface HellblockGUIManagerInterface extends Reloadable {

	/**
	 * Opens the hellblock GUI for the specified player.
	 *
	 * @param player   the {@link Player} for whom the hellblock GUI will be opened.
	 * @param islandId the islandId of the island owner.
	 * @param isOwner  whether the player who opened the menu is the owner of the
	 *                 hellblock or not.
	 * @return true if the hellblock GUI was successfully opened, false otherwise
	 */
	boolean openHellblockGUI(Player player, int islandId, boolean isOwner);
}