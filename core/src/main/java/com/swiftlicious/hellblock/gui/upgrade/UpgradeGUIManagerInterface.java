package com.swiftlicious.hellblock.gui.upgrade;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the upgrade menu
 */
public interface UpgradeGUIManagerInterface extends Reloadable {

	/**
	 * Opens the upgrade GUI for the specified player.
	 *
	 * @param player   the {@link Player} for whom the upgrade GUI will be opened.
	 * @param islandId the islandId of the island owner.
	 * @param isOwner  whether the player who opened the menu is the owner of the
	 *                 hellblock or not.
	 * @return true if the upgrade GUI was successfully opened, false otherwise
	 */
	boolean openUpgradeGUI(Player player, int islandId, boolean isOwner);
}