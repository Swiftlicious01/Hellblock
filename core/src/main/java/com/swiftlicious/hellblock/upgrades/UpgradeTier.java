package com.swiftlicious.hellblock.upgrades;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a specific tier level in the upgrade system.
 * <p>
 * Each tier holds a mapping of {@link IslandUpgradeType} to
 * {@link UpgradeData}, representing the value and cost of each upgrade type at
 * that tier.
 */
public class UpgradeTier {

	private final int tier;
	private final Map<IslandUpgradeType, UpgradeData> upgrades = new HashMap<>();

	/**
	 * Constructs a new {@code UpgradeTier} for the given tier level.
	 *
	 * @param tier the numeric tier level (e.g., 0 for default)
	 */
	public UpgradeTier(int tier) {
		this.tier = tier;
	}

	/**
	 * @return the numeric tier level
	 */
	public int getTier() {
		return tier;
	}

	/**
	 * @return a map of upgrade types and their associated data for this tier
	 */
	@NotNull
	public Map<IslandUpgradeType, UpgradeData> getUpgrades() {
		return upgrades;
	}

	/**
	 * Adds an upgrade entry for the specified type at this tier.
	 *
	 * @param type the upgrade type to add
	 * @param data the associated upgrade data
	 */
	public void addUpgrade(@NotNull IslandUpgradeType type, @NotNull UpgradeData data) {
		upgrades.put(type, data);
	}

	/**
	 * Retrieves the upgrade data for a given type, if defined in this tier.
	 *
	 * @param type the upgrade type to retrieve
	 * @return the {@link UpgradeData} for the type, or {@code null} if not defined
	 */
	@Nullable
	public UpgradeData getUpgrade(@NotNull IslandUpgradeType type) {
		return upgrades.get(type);
	}

	/**
	 * @return a string representation of the tier and its upgrades
	 */
	@Override
	public String toString() {
		return "UpgradeTier{" + "tier=" + tier + ", upgrades=" + upgrades + '}';
	}
}