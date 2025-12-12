package com.swiftlicious.hellblock.upgrades.costs;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.creation.addons.VaultHook;
import com.swiftlicious.hellblock.upgrades.CostFormatter;
import com.swiftlicious.hellblock.upgrades.CostHandler;
import com.swiftlicious.hellblock.upgrades.UpgradeCost;

/**
 * A {@link CostHandler} implementation for money-based upgrade costs.
 * <p>
 * This handler uses Vault to check and deduct the player's balance.
 */
public class MoneyCostHandler implements CostHandler {

	/**
	 * Checks if the player has enough money to cover the cost. Returns
	 * {@code false} if Vault is not hooked.
	 *
	 * @param player the player to check
	 * @param cost   the monetary upgrade cost
	 * @return {@code true} if the player has enough balance
	 */
	@Override
	public boolean canAfford(Player player, UpgradeCost cost) {
		if (!VaultHook.isHooked()) {
			return false;
		}
		return VaultHook.hasBalance(player, cost.getAmount());
	}

	/**
	 * Deducts the required amount of money from the player's balance. Does nothing
	 * if Vault is not hooked.
	 *
	 * @param player the player to deduct from
	 * @param cost   the monetary cost
	 */
	@Override
	public void deduct(Player player, UpgradeCost cost) {
		if (VaultHook.isHooked()) {
			VaultHook.withdraw(player, cost.getAmount());
		}
	}

	/**
	 * Returns a formatted currency string representing the cost. Uses the
	 * locale-specific money format defined by the plugin.
	 *
	 * @param cost the monetary cost
	 * @return formatted currency string (e.g., "$1,000.00")
	 */
	@Override
	public String describe(UpgradeCost cost) {
		return CostFormatter.MONEY_FORMAT.format(cost.getAmount());
	}
}