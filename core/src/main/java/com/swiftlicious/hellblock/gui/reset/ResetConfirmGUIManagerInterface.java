package com.swiftlicious.hellblock.gui.reset;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing the reset confirm menu
 */
public interface ResetConfirmGUIManagerInterface extends Reloadable {

	/**
	 * Opens the reset confirm GUI for the specified player.
	 *
	 * @param player the {@link Player} for whom the reset confirm GUI will be
	 *               opened
	 * @return true if the reset confirm GUI was successfully opened, false
	 *         otherwise
	 */
	boolean openResetConfirmGUI(Player player);
}