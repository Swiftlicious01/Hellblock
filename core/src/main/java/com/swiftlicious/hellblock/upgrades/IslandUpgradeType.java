package com.swiftlicious.hellblock.upgrades;

public enum IslandUpgradeType {
	GENERATOR_CHANCE(true), PIGLIN_BARTERING(true), CROP_GROWTH(true), MOB_SPAWN_RATE(true), PROTECTION_RANGE(false),
	PARTY_SIZE(false), HOPPER_LIMIT(false);

	private final boolean isFloat;

	IslandUpgradeType(boolean isFloat) {
		this.isFloat = isFloat;
	}

	public boolean isFloatType() {
		return isFloat;
	}
}