package com.swiftlicious.hellblock.upgrades;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the types of upgrades that can be applied to a Hellblock island.
 *
 * <p>
 * Each upgrade type modifies a specific aspect of island gameplay or
 * capabilities. Some upgrades use float-based values (e.g., multipliers or
 * percentages), while others use integers (e.g., counts or limits).
 * </p>
 *
 * <ul>
 * <li>{@code PROTECTION_RANGE} – Expands the island's protected radius
 * (integer).</li>
 * <li>{@code PARTY_SIZE} – Increases the maximum number of island party members
 * (integer).</li>
 * <li>{@code HOPPER_LIMIT} – Increases the number of hoppers that can be placed
 * (integer).</li>
 * <li>{@code GENERATOR_CHANCE} – Affects the quality or chance of resources
 * produced by generators (float).</li>
 * <li>{@code PIGLIN_BARTERING} – Alters Piglin bartering efficiency or output
 * (float).</li>
 * <li>{@code CROP_GROWTH} – Increases crop growth speed (float).</li>
 * <li>{@code MOB_SPAWN_RATE} – Enhances the rate of mob spawning (float).</li>
 * </ul>
 */
public enum IslandUpgradeType {
	@SerializedName("protectionRange")
	PROTECTION_RANGE(false),

	@SerializedName("partySize")
	PARTY_SIZE(false),

	@SerializedName("hopperLimit")
	HOPPER_LIMIT(false),

	@SerializedName("generatorChance")
	GENERATOR_CHANCE(true),

	@SerializedName("piglinBartering")
	PIGLIN_BARTERING(true),

	@SerializedName("cropGrowth")
	CROP_GROWTH(true),

	@SerializedName("mobSpawnRate")
	MOB_SPAWN_RATE(true);

	private final boolean isFloat;

	IslandUpgradeType(boolean isFloat) {
		this.isFloat = isFloat;
	}

	/**
	 * Returns whether this upgrade type uses a float value (e.g., a multiplier or
	 * percentage), rather than an integer value.
	 *
	 * @return {@code true} if the upgrade type uses a float, otherwise
	 *         {@code false}.
	 */
	public boolean isFloatType() {
		return isFloat;
	}
}