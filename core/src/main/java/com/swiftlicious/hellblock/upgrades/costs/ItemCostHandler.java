package com.swiftlicious.hellblock.upgrades.costs;

import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.upgrades.CostFormatter;
import com.swiftlicious.hellblock.upgrades.CostHandler;
import com.swiftlicious.hellblock.upgrades.UpgradeCost;

/**
 * A {@link CostHandler} implementation for item-based upgrade costs.
 * <p>
 * This handler checks if a player has the required quantity of specific items
 * and deducts them from their inventory if they can afford the upgrade.
 */
public class ItemCostHandler implements CostHandler {

	/**
	 * Checks whether the player has at least the required amount of each item
	 * specified in the upgrade cost.
	 *
	 * @param player the player to check
	 * @param cost   the item cost to validate
	 * @return {@code true} if the player has enough of each required item
	 */
	@Override
	public boolean canAfford(Player player, UpgradeCost cost) {
		if (cost.getItem() == null) {
			return false;
		}
		List<String> items = CostFormatter.parseItems(cost.getItem());
		double amountEach = cost.getAmount();

		for (String itemName : items) {
			Material mat = Material.matchMaterial(itemName.toUpperCase(Locale.ROOT));
			if (mat == null) {
				HellblockPlugin.getInstance().getPluginLogger().warn("Unknown item material: " + itemName);
				return false;
			}
			if (!player.getInventory().containsAtLeast(new ItemStack(mat), (int) amountEach)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Deducts the required amount of each item in the cost from the player's
	 * inventory.
	 *
	 * @param player the player to deduct items from
	 * @param cost   the item cost
	 */
	@Override
	public void deduct(Player player, UpgradeCost cost) {
		if (cost.getItem() == null) {
			return;
		}
		List<String> items = CostFormatter.parseItems(cost.getItem());
		double amountEach = cost.getAmount();

		for (String itemName : items) {
			Material mat = Material.matchMaterial(itemName.toUpperCase(Locale.ROOT));
			if (mat == null) {
				HellblockPlugin.getInstance().getPluginLogger().warn("Unknown item material: " + itemName);
				continue;
			}
			player.getInventory().removeItem(new ItemStack(mat, (int) amountEach));
		}
	}

	/**
	 * Returns a human-readable description of the item cost.
	 * <p>
	 * Example: {@code "16 × Iron Ingots & Gold Ingots"}
	 *
	 * @param cost the item cost
	 * @return formatted item description
	 */
	@Override
	public String describe(UpgradeCost cost) {
		if (cost.getItem() == null) {
			return "";
		}
		List<String> items = CostFormatter.parseItems(cost.getItem());
		String itemDisplay = CostFormatter.formatItemList(items, cost.getAmount());
		return String.format("%.0f × %s", cost.getAmount(), itemDisplay);
	}
}