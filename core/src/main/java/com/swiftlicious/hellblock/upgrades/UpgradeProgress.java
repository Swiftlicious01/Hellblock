package com.swiftlicious.hellblock.upgrades;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the current and upcoming upgrade tier progress for a specific
 * upgrade type.
 * <p>
 * This class is typically used to show players their current tier, the next
 * available upgrade tier (if any), and the associated data.
 */
public class UpgradeProgress {

	private final int currentTier;
	private final UpgradeData currentData;
	private final int nextTier;
	private final UpgradeData nextData;

	/**
	 * Constructs a new {@code UpgradeProgress} object.
	 *
	 * @param currentTier the current tier level
	 * @param currentData the {@link UpgradeData} for the current tier
	 * @param nextTier    the next tier level (or -1 if none)
	 * @param nextData    the {@link UpgradeData} for the next tier (or null if
	 *                    maxed)
	 */
	public UpgradeProgress(int currentTier, @NotNull UpgradeData currentData, int nextTier,
			@Nullable UpgradeData nextData) {
		this.currentTier = currentTier;
		this.currentData = currentData;
		this.nextTier = nextTier;
		this.nextData = nextData;
	}

	/**
	 * @return the player's current tier number
	 */
	public int getCurrentTier() {
		return currentTier;
	}

	/**
	 * @return the upgrade data for the current tier
	 */
	@NotNull
	public UpgradeData getCurrentData() {
		return currentData;
	}

	/**
	 * @return the next available tier number, or -1 if none
	 */
	public int getNextTier() {
		return nextTier;
	}

	/**
	 * @return the upgrade data for the next tier, or null if already at max
	 */
	@Nullable
	public UpgradeData getNextData() {
		return nextData;
	}

	/**
	 * @return {@code true} if a next upgrade tier exists; {@code false} otherwise
	 */
	public boolean hasNext() {
		return nextData != null;
	}
}