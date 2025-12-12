package com.swiftlicious.hellblock.upgrades.costs;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.creation.addons.PlayerPointsHook;
import com.swiftlicious.hellblock.upgrades.CostHandler;
import com.swiftlicious.hellblock.upgrades.UpgradeCost;

/**
 * A {@link CostHandler} implementation for points-based upgrade costs.
 * <p>
 * This handler uses PlayerPoints to check and deduct the player's points.
 */
public class PointsCostHandler implements CostHandler {

	/**
	 * Checks if the player has enough points to cover the cost. Returns
	 * {@code false} if PlayerPoints is not hooked.
	 *
	 * @param player the player to check
	 * @param cost   the monetary upgrade cost
	 * @return {@code true} if the player has enough points
	 */
	@Override
	public boolean canAfford(Player player, UpgradeCost cost) {
		if (!PlayerPointsHook.isHooked()) {
			return false;
		}
		return PlayerPointsHook.hasPoints(player, (int) cost.getAmount());
	}

	/**
	 * Deducts the required amount of points from the player's points. Does nothing
	 * if PlayerPoints is not hooked.
	 *
	 * @param player the player to deduct from
	 * @param cost   the monetary cost
	 */
	@Override
	public void deduct(Player player, UpgradeCost cost) {
		if (PlayerPointsHook.isHooked()) {
			PlayerPointsHook.takePoints(player, (int) cost.getAmount());
		}
	}

	/**
	 * Returns a human-readable description of the points cost.
	 *
	 * @param cost the points cost to describe
	 * @return formatted string, e.g., "100 ✦ Points"
	 */
	@Override
	public String describe(UpgradeCost cost) {
		return String.format("%.0f ✦ Points", (int) cost.getAmount());
	}
}