package com.swiftlicious.hellblock.gui.leaderboard;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the leaderboard menu
 */
public interface LeaderboardGUIManagerInterface extends Reloadable {

	/**
	 * Opens the leaderboard GUI for the specified player.
	 *
	 * @param player       the {@link Player} for whom the leaderboard GUI will be
	 *                     opened.
	 * @param islandId     the islandId of the island owner.
	 * @param isOwner      whether the player opening the menu is the owner or not.
	 * @param showBackIcon show back menu icon.
	 * @return true if the leaderboard GUI was successfully opened, false otherwise
	 */
	boolean openLeaderboardGUI(Player player, int islandId, boolean isOwner, boolean showBackIcon);
}