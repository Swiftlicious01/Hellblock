package com.swiftlicious.hellblock.upgrades;

import java.util.List;
import java.util.Locale;

import org.bukkit.entity.Player;

public interface CostHandler {

	boolean canAfford(Player player, UpgradeCost cost);

	void deduct(Player player, UpgradeCost cost);

	default String describe(UpgradeCost cost) {
		String type = cost.getType().toUpperCase(Locale.ROOT);

		if ("ITEM".equals(type)) {
			List<String> items = CostFormatter.parseItems(cost.getItem());
			String itemDisplay = CostFormatter.formatItemList(items, cost.getAmount());
			return String.format("%.0f × %s", cost.getAmount(), itemDisplay);
		}
		if ("MONEY".equals(type)) {
			return CostFormatter.MONEY_FORMAT.format(cost.getAmount());
		}
		if ("EXP".equals(type) || "EXPERIENCE".equals(type) || "XP".equals(type)) {
			return String.format("%.0f ✦ XP", cost.getAmount());
		}
		return String.format("%.0f %s", cost.getAmount(), type);
	}
}