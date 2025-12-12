package com.swiftlicious.hellblock.gui.notification;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the notification menu
 */
public interface NotificationGUIManagerInterface extends Reloadable {

	/**
	 * Opens the notification GUI for the specified player.
	 *
	 * @param player   the {@link Player} for whom the notification GUI will be
	 *                 opened.
	 * @param islandId the islandId of the island owner.
	 * @param isOwner  whether the player opening the menu is the owner or not.
	 * @return true if the notification GUI was successfully opened, false
	 *         otherwise.
	 */
	boolean openNotificationGUI(Player player, int islandId, boolean isOwner);
}