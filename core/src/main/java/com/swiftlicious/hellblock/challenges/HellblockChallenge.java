package com.swiftlicious.hellblock.challenges;

import org.jetbrains.annotations.NotNull;

public class HellblockChallenge {

	private ChallengeType type;
	private CompletionStatus status;
	private int progress;

	public HellblockChallenge(@NotNull ChallengeType type, @NotNull CompletionStatus status, int progress) {
		this.type = type;
		this.status = status;
		this.progress = progress;
	}

	@NotNull
	public ChallengeType getChallengeType() {
		return this.type;
	}

	@NotNull
	public CompletionStatus getCompletionStatus() {
		return this.status;
	}

	public int getProgress() {
		return this.progress;
	}

	public enum CompletionStatus {
		COMPLETED, IN_PROGRESS, NOT_STARTED;
	}

	public enum ActionType {
		BREAK, GROW, FISH, INTERACT, CRAFT, FARM, SPAWN, KILL, BREW, BARTER;
	}
}