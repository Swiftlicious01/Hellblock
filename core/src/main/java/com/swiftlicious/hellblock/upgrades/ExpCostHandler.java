package com.swiftlicious.hellblock.upgrades;

import org.bukkit.entity.Player;

public class ExpCostHandler implements CostHandler {

	@Override
	public boolean canAfford(Player player, UpgradeCost cost) {
		return player.getTotalExperience() >= cost.getAmount();
	}

	@Override
	public void deduct(Player player, UpgradeCost cost) {
		player.giveExp(-((int) cost.getAmount()));
	}

	@Override
	public String describe(UpgradeCost cost) {
		return String.format("%.0f ✦ XP", cost.getAmount());
	}
}
