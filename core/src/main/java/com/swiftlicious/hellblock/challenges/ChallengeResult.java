package com.swiftlicious.hellblock.challenges;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;

/**
 * Represents the persistent result of a challenge for a player or island.
 * <p>
 * This includes the current {@link CompletionStatus} of the challenge, the
 * amount of progress made, and whether the associated reward has been claimed.
 * <p>
 * This class is typically serialized as part of {@code ChallengeData}.
 */
public class ChallengeResult {

	@Expose
	@SerializedName("completionStatus")
	protected CompletionStatus status;

	@Expose
	@SerializedName("progress")
	protected double progress;

	@Expose
	@SerializedName("claimedReward")
	protected boolean claimedReward;

	/**
	 * Constructs a new {@link ChallengeResult} with the given status, progress, and
	 * reward claim state.
	 *
	 * @param status        the current completion status of the challenge
	 * @param progress      the current progress value
	 * @param claimedReward whether the reward has already been claimed
	 */
	public ChallengeResult(CompletionStatus status, double progress, boolean claimedReward) {
		this.status = status;
		this.progress = progress;
		this.claimedReward = claimedReward;
	}

	/**
	 * Returns the current status of the challenge.
	 *
	 * @return the {@link CompletionStatus}
	 */
	public CompletionStatus getStatus() {
		return this.status;
	}

	/**
	 * Returns the current progress toward the challenge goal.
	 *
	 * @return the progress value
	 */
	public double getProgress() {
		return this.progress;
	}

	/**
	 * Returns whether the challenge reward has been claimed.
	 *
	 * @return {@code true} if claimed, {@code false} otherwise
	 */
	public boolean isRewardClaimed() {
		return this.claimedReward;
	}

	/**
	 * Sets the current status of the challenge.
	 *
	 * @param status the new {@link CompletionStatus}
	 */
	public void setStatus(CompletionStatus status) {
		this.status = status;
	}

	/**
	 * Sets the progress value toward the challenge.
	 *
	 * @param progress the new progress value
	 */
	public void setProgress(double progress) {
		this.progress = progress;
	}

	/**
	 * Sets whether the reward for this challenge has been claimed.
	 *
	 * @param claimedReward {@code true} if claimed, {@code false} otherwise
	 */
	public void setRewardClaimed(boolean claimedReward) {
		this.claimedReward = claimedReward;
	}

	/**
	 * Creates a deep copy of this {@link ChallengeResult}.
	 *
	 * @return a new {@link ChallengeResult} with the same values
	 */
	public final ChallengeResult copy() {
		return new ChallengeResult(this.status, this.progress, this.claimedReward);
	}
}