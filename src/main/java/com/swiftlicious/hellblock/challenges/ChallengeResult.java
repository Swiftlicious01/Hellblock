package com.swiftlicious.hellblock.challenges;

import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;

public class ChallengeResult {

	private CompletionStatus status;
	private int progress;
	private boolean claimedReward;

	public ChallengeResult(CompletionStatus status, int progress, boolean claimedReward) {
		this.status = status;
		this.progress = progress;
		this.claimedReward = claimedReward;
	}

	public CompletionStatus getStatus() {
		return this.status;
	}

	public int getProgress() {
		return this.progress;
	}

	public boolean isRewardClaimed() {
		return this.claimedReward;
	}

	public void setStatus(CompletionStatus status) {
		this.status = status;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setRewardClaimed(boolean claimedReward) {
		this.claimedReward = claimedReward;
	}
}
