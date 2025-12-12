package com.swiftlicious.hellblock.challenges;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an in-progress or completed challenge assigned to a player or
 * island.
 * <p>
 * This class encapsulates a specific {@link ChallengeType}, its current
 * {@link CompletionStatus}, and the amount of progress made so far.
 */
public class HellblockChallenge {

	private final ChallengeType type;
	private final CompletionStatus status;
	private final int progress;

	/**
	 * Constructs a new {@link HellblockChallenge} instance.
	 *
	 * @param type     the type of challenge
	 * @param status   the completion status of the challenge
	 * @param progress the current progress toward completion
	 */
	public HellblockChallenge(@NotNull ChallengeType type, @NotNull CompletionStatus status, int progress) {
		this.type = type;
		this.status = status;
		this.progress = progress;
	}

	/**
	 * Returns the type of this challenge.
	 *
	 * @return the associated {@link ChallengeType}
	 */
	@NotNull
	public ChallengeType getChallengeType() {
		return this.type;
	}

	/**
	 * Returns the current completion status of the challenge.
	 *
	 * @return the {@link CompletionStatus}
	 */
	@NotNull
	public CompletionStatus getCompletionStatus() {
		return this.status;
	}

	/**
	 * Returns the current progress value made toward completing the challenge.
	 *
	 * @return progress as an integer
	 */
	public int getProgress() {
		return this.progress;
	}

	/**
	 * Represents the possible completion states of a challenge.
	 */
	public enum CompletionStatus {
		@SerializedName("notStarted")
		NOT_STARTED,

		@SerializedName("inProgress")
		IN_PROGRESS,

		@SerializedName("completed")
		COMPLETED;
	}

	/**
	 * Represents the type of action required to complete a challenge.
	 */
	public enum ActionType {
		BREAK, GROW, FISH, BREED, CRAFT, FARM, LEVELUP, SLAY, BREW, BARTER;
	}
}