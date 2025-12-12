package com.swiftlicious.hellblock.upgrades;

import java.util.List;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a handler capable of checking and applying specific upgrade cost
 * types.
 * <p>
 * Each cost type (e.g., MONEY, EXP, ITEM) should have a corresponding handler
 * implementation. This interface is used by {@link UpgradeCostProcessor} to
 * delegate cost operations.
 */
public interface CostHandler {

	/**
	 * Checks if the given player can afford the specified cost.
	 *
	 * @param player the player to check
	 * @param cost   the cost requirement to verify
	 * @return {@code true} if the player can afford it; {@code false} otherwise
	 */
	boolean canAfford(@NotNull Player player, @NotNull UpgradeCost cost);

	/**
	 * Deducts the specified cost from the player.
	 *
	 * @param player the player from whom to deduct the cost
	 * @param cost   the cost to deduct
	 */
	void deduct(@NotNull Player player, @NotNull UpgradeCost cost);

	/**
	 * Returns a human-readable description of the cost, used for UI or error
	 * messages.
	 *
	 * @param cost the upgrade cost to describe
	 * @return a formatted string representation of the cost
	 */
	@NotNull
	default String describe(@NotNull UpgradeCost cost) {
		UpgradeCostType type = cost.getType();

		if (type == UpgradeCostType.ITEM) {
			List<String> items = CostFormatter.parseItems(cost.getItem());
			String itemDisplay = CostFormatter.formatItemList(items, cost.getAmount());
			return String.format("%.0f × %s", cost.getAmount(), itemDisplay);
		}
		if (type == UpgradeCostType.MONEY) {
			return CostFormatter.MONEY_FORMAT.format(cost.getAmount());
		}
		if (type == UpgradeCostType.POINTS) {
			return String.format("%.0f Points", (int) cost.getAmount());
		}
		if (UpgradeCostType.isExpUpgradeCostType(type)) {
			return String.format("%.0f ✦ XP", cost.getAmount());
		}
		return String.format("%.0f %s", cost.getAmount(), type);
	}
}