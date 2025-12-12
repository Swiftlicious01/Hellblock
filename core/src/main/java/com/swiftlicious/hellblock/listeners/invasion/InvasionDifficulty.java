package com.swiftlicious.hellblock.listeners.invasion;

import java.util.Arrays;

public enum InvasionDifficulty {
	EMBER(1), ASHEN(2), INFERNAL(3), HELLFIRE(4), ABYSSAL(5), NETHERBORN(6), APOCALYPTIC(7), OBLIVION(8), INFERNUM(9),
	GEHENNA(10);

	private final int tier;

	InvasionDifficulty(int tier) {
		this.tier = tier;
	}

	public int getTier() {
		return tier;
	}

	public static InvasionDifficulty getByTier(int tier) {
		return Arrays.stream(values()).filter(d -> d.tier == tier).findFirst().orElse(EMBER);
	}

	public static int getMaxTier() {
		return Arrays.stream(values()).mapToInt(InvasionDifficulty::getTier).max().orElse(1);
	}
}