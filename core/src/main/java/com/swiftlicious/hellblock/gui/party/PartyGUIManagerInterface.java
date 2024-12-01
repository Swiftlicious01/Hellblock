package com.swiftlicious.hellblock.gui.party;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the party menu
 */
public interface PartyGUIManagerInterface extends Reloadable {

	/**
	 * Opens the party GUI for the specified player.
	 *
	 * @param player  the {@link Player} for whom the party GUI will be opened
	 * @param isOwner whether the player opening the menu is the owner or not.
	 * @return true if the party GUI was successfully opened, false otherwise
	 */
	boolean openPartyGUI(Player player, boolean isOwner);
}