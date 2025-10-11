package com.swiftlicious.hellblock.challenges;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.utils.extras.Action;

public class ChallengeType {

	private final String id;
	private final ChallengeRequirement requirement;
	private final int completionAmount;
	private final ActionType action;
	private final Action<Player>[] rewards;

	public ChallengeType(@NotNull String id, @NotNull ChallengeRequirement requirement, int completionAmount,
			@NotNull ActionType action, @NotNull Action<Player>[] rewards) {
		this.id = id;
		this.requirement = requirement;
		this.completionAmount = completionAmount;
		this.action = action;
		this.rewards = rewards;
	}

	@NotNull
	public String getId() {
		return this.id;
	}

	@NotNull
	public ChallengeRequirement getRequiredData() {
		return this.requirement;
	}

	public int getNeededAmount() {
		return this.completionAmount;
	}

	@NotNull
	public ActionType getChallengeType() {
		return this.action;
	}

	@NotNull
	public Action<Player>[] getRewardActions() {
		return this.rewards;
	}
}