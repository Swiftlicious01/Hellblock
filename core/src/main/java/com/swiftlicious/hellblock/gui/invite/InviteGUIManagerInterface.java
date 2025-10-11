package com.swiftlicious.hellblock.gui.invite;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the invite menu
 */
public interface InviteGUIManagerInterface extends Reloadable {

	/**
	 * Opens the invite GUI for the specified player.
	 *
	 * @param player the {@link Player} for whom the invite GUI will be opened
	 * @param isOwner whether the player opening the menu is the owner or not.
	 * @return true if the invite GUI was successfully opened, false otherwise
	 */
	boolean openInvitationGUI(Player player, boolean isOwner);
}