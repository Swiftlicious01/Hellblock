package com.swiftlicious.hellblock.upgrades;

import java.util.HashMap;
import java.util.Map;

public class UpgradeTier {
	
	private final int tier;
	private final Map<IslandUpgradeType, UpgradeData> upgrades = new HashMap<>();

	public UpgradeTier(int tier) {
		this.tier = tier;
	}

	public int getTier() {
		return tier;
	}

	public Map<IslandUpgradeType, UpgradeData> getUpgrades() {
		return upgrades;
	}

	public void addUpgrade(IslandUpgradeType type, UpgradeData data) {
		upgrades.put(type, data);
	}

	public UpgradeData getUpgrade(IslandUpgradeType type) {
		return upgrades.get(type);
	}

	@Override
	public String toString() {
		return "UpgradeTier{" + "tier=" + tier + ", upgrades=" + upgrades + '}';
	}
}