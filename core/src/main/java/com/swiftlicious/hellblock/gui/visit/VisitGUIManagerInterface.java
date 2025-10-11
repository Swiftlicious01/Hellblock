package com.swiftlicious.hellblock.gui.visit;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager.VisitSorter;

/**
 * Interface for managing the visit menu
 */
public interface VisitGUIManagerInterface extends Reloadable {

	/**
	 * Opens the visit GUI for the specified player.
	 *
	 * @param player       the {@link Player} for whom the visit GUI will be opened
	 * @param visitSorter  the {@link VisitSorter} to open the menu on.
	 * @param showBackIcon show back menu icon
	 * @return true if the visit GUI was successfully opened, false otherwise
	 */
	boolean openVisitGUI(Player player, VisitSorter visitSorter, boolean showBackIcon);
}