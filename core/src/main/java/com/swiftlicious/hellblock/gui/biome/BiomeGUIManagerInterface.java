package com.swiftlicious.hellblock.gui.biome;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the biome menu
 */
public interface BiomeGUIManagerInterface extends Reloadable {

	/**
	 * Opens the biome GUI for the specified player.
	 *
	 * @param player   the {@link Player} for whom the biome GUI will be opened.
	 * @param islandId the islandId of the island owner.
	 * @param isOwner  whether the player opening the menu is the owner or not.
	 * @return true if the biome GUI was successfully opened, false otherwise
	 */
	boolean openBiomeGUI(Player player, int islandId, boolean isOwner);
}