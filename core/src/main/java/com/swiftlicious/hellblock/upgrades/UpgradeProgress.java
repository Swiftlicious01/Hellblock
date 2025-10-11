package com.swiftlicious.hellblock.upgrades;

public class UpgradeProgress {
	
	private final int currentTier;
	private final UpgradeData currentData;
	private final int nextTier;
	private final UpgradeData nextData;

	public UpgradeProgress(int currentTier, UpgradeData currentData, int nextTier, UpgradeData nextData) {
		this.currentTier = currentTier;
		this.currentData = currentData;
		this.nextTier = nextTier;
		this.nextData = nextData;
	}

	public int getCurrentTier() {
		return currentTier;
	}

	public UpgradeData getCurrentData() {
		return currentData;
	}

	public int getNextTier() {
		return nextTier;
	}

	public UpgradeData getNextData() {
		return nextData;
	}

	public boolean hasNext() {
		return nextData != null;
	}
}