package com.swiftlicious.hellblock.gui.challenges;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the island choice menu
 */
public interface ChallengesGUIManagerInterface extends Reloadable {

	/**
	 * Opens the challenges GUI for the specified player.
	 *
	 * @param player       the {@link Player} for whom the challenges GUI will be
	 *                     opened.
	 * @param islandId     the islandId of the island owner.
	 * @param isOwner      whether the player opening the menu is the owner or not.
	 * @param showBackIcon show the back icon in the menu.
	 * @return true if the challenges GUI was successfully opened, false otherwise.
	 */
	boolean openChallengesGUI(Player player, int islandId, boolean isOwner, boolean showBackIcon);
}