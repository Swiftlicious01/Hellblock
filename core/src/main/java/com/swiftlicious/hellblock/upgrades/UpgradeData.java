package com.swiftlicious.hellblock.upgrades;

import java.util.List;

public class UpgradeData {

	private final Number value;
	private final List<UpgradeCost> costs;

	public UpgradeData(double value, List<UpgradeCost> costs) {
		this.value = value;
		this.costs = costs;
	}

	public Number getValue() {
		return value;
	}

	public List<UpgradeCost> getCosts() {
		return costs;
	}
}