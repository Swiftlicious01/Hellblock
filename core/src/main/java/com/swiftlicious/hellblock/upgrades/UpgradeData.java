package com.swiftlicious.hellblock.upgrades;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the data for a specific upgrade at a given tier, including its
 * value and the list of costs required to unlock it.
 */
public class UpgradeData {

	private final Number value;
	private final List<UpgradeCost> costs;

	/**
	 * Constructs a new {@code UpgradeData} object.
	 *
	 * @param value the upgrade value (can be int or double depending on upgrade
	 *              type)
	 * @param costs the list of {@link UpgradeCost} objects required for this
	 *              upgrade
	 */
	public UpgradeData(double value, @NotNull List<UpgradeCost> costs) {
		this.value = value;
		this.costs = costs;
	}

	/**
	 * @return the numeric value associated with this upgrade
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * @return the list of costs required to unlock this upgrade
	 */
	@NotNull
	public List<UpgradeCost> getCosts() {
		return costs;
	}
}