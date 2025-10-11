package com.swiftlicious.hellblock.upgrades;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.creation.addons.VaultHook;

public class MoneyCostHandler implements CostHandler {

	@Override
	public boolean canAfford(Player player, UpgradeCost cost) {
		if (!VaultHook.isHooked()) {
			return false;
		}
		return VaultHook.hasBalance(player, cost.getAmount());
	}

	@Override
	public void deduct(Player player, UpgradeCost cost) {
		if (VaultHook.isHooked()) {
			VaultHook.withdraw(player, cost.getAmount());
		}
	}

	@Override
	public String describe(UpgradeCost cost) {
		return CostFormatter.MONEY_FORMAT.format(cost.getAmount());
	}
}
