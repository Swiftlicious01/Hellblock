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
	 * @param player the {@link Player} for whom the biome GUI will be opened
	 * @return true if the biome GUI was successfully opened, false otherwise
	 */
	boolean openBiomeGUI(Player player);
}