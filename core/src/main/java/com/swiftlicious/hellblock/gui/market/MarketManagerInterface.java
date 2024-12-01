package com.swiftlicious.hellblock.gui.market;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.Context;

/**
 * Interface for managing the market
 */
public interface MarketManagerInterface extends Reloadable {

	/**
	 * Opens the market GUI for the specified player.
	 *
	 * @param player the {@link Player} for whom the market GUI will be opened
	 * @return true if the market GUI was successfully opened, false otherwise
	 */
	boolean openMarketGUI(Player player);

	/**
	 * Retrieves the price of the specified item within the given context.
	 *
	 * @param context   the {@link Context} in which the price is calculated
	 * @param itemStack the {@link ItemStack} representing the item
	 * @return the price of the item as a double
	 */
	double getItemPrice(Context<Player> context, ItemStack itemStack);

	/**
	 * Retrieves the formula used for calculating item prices.
	 *
	 * @return the pricing formula as a String
	 */
	String getFormula();

	/**
	 * Retrieves the earning limit within the given context.
	 *
	 * @param context the {@link Context} in which the earning limit is checked
	 * @return the earning limit as a double
	 */
	double earningLimit(Context<Player> context);

	/**
	 * Retrieves the earning multiplier within the given context.
	 *
	 * @param context the {@link Context} in which the earning multiplier is checked
	 * @return the earning multiplier as a double
	 */
	double earningsMultiplier(Context<Player> context);
}