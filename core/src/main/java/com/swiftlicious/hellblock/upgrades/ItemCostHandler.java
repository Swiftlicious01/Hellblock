package com.swiftlicious.hellblock.upgrades;

import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;

public class ItemCostHandler implements CostHandler {

	@Override
	public boolean canAfford(Player player, UpgradeCost cost) {
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

	@Override
	public void deduct(Player player, UpgradeCost cost) {
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

	@Override
	public String describe(UpgradeCost cost) {
		List<String> items = CostFormatter.parseItems(cost.getItem());
		String itemDisplay = CostFormatter.formatItemList(items, cost.getAmount());
		return String.format("%.0f × %s", cost.getAmount(), itemDisplay);
	}
}