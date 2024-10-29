package com.swiftlicious.hellblock.challenges;

public class ChallengeData {

	private int progress;
	private boolean claimedReward;

	public ChallengeData(int progress, boolean claimedReward) {
		this.progress = progress;
		this.claimedReward = claimedReward;
	}

	public int getProgress() {
		return this.progress;
	}

	public boolean isRewardClaimed() {
		return this.claimedReward;
	}
}
