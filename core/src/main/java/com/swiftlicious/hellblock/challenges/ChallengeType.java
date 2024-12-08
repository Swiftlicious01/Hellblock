package com.swiftlicious.hellblock.challenges;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;

public class ChallengeType {
	private String id;
	private int completionAmount;
	private ActionType action;

	public ChallengeType(@NotNull String id, int completionAmount, @NotNull ActionType action) {
		this.id = id;
		this.completionAmount = completionAmount;
		this.action = action;
	}

	@NotNull
	public String getId() {
		return this.id;
	}

	public int getNeededAmount() {
		return this.completionAmount;
	}

	@NotNull
	public ActionType getAction() {
		return this.action;
	}
}
