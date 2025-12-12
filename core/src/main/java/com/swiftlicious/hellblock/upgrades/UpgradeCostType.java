package com.swiftlicious.hellblock.upgrades;

import org.jetbrains.annotations.NotNull;

public enum UpgradeCostType {
	MONEY, EXP, EXPERIENCE, XP, ITEM, POINTS, NONE;

	public static boolean isExpUpgradeCostType(@NotNull UpgradeCostType costType) {
		return costType == EXP || costType == EXPERIENCE || costType == XP;
	}
}