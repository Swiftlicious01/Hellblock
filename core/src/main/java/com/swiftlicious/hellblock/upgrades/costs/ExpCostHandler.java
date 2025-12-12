package com.swiftlicious.hellblock.upgrades.costs;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.upgrades.CostHandler;
import com.swiftlicious.hellblock.upgrades.UpgradeCost;

/**
 * A {@link CostHandler} implementation for handling experience (XP) upgrade
 * costs.
 * <p>
 * This handler checks if a player has enough total experience and deducts the
 * required amount if they can afford it.
 */
public class ExpCostHandler implements CostHandler {

	/**
	 * Checks if the player has enough experience to cover the cost.
	 *
	 * @param player the player to check
	 * @param cost   the XP cost
	 * @return {@code true} if the player has at least the required XP
	 */
	@Override
	public boolean canAfford(Player player, UpgradeCost cost) {
		return player.getTotalExperience() >= cost.getAmount();
	}

	/**
	 * Deducts the required XP amount from the player. Uses negative XP gain to
	 * remove levels.
	 *
	 * @param player the player to deduct XP from
	 * @param cost   the XP cost
	 */
	@Override
	public void deduct(Player player, UpgradeCost cost) {
		player.giveExp(-((int) cost.getAmount()));
	}

	/**
	 * Returns a human-readable description of the XP cost.
	 *
	 * @param cost the XP cost to describe
	 * @return formatted string, e.g., "100 ✦ XP"
	 */
	@Override
	public String describe(UpgradeCost cost) {
		return String.format("%.0f ✦ XP", cost.getAmount());
	}
}