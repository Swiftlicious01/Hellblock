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
	 * @param player the {@link Player} for whom the challenges GUI will be opened
	 * @return true if the challenges GUI was successfully opened, false otherwise
	 */
	boolean openChallengesGUI(Player player);
}