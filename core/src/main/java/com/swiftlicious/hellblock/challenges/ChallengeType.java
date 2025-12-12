package com.swiftlicious.hellblock.challenges;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.utils.extras.Action;

/**
 * Describes the definition of a specific challenge.
 * <p>
 * Each challenge has a unique ID, a set of requirements, a goal (amount), an
 * action type, and a set of rewards to be granted upon completion.
 * <p>
 * This class is partially serializable. Only the challenge ID is serialized for
 * persistence, while other fields (requirement, amount, action, rewards) are
 * typically configured or restored at runtime.
 */
public class ChallengeType {

	@Expose
	@SerializedName("challengeId")
	private final String id;

	private transient final ChallengeRequirement requirement;
	private transient final int completionAmount;
	private transient final ActionType action;
	private transient final Action<Player>[] rewards;

	/**
	 * Constructs a new {@link ChallengeType} instance.
	 *
	 * @param id               the unique ID for this challenge
	 * @param requirement      the logic requirement that determines progression
	 * @param completionAmount the amount required to complete this challenge
	 * @param action           the type of action tracked for completion
	 * @param rewards          the list of actions to execute as rewards
	 */
	public ChallengeType(@NotNull String id, @NotNull ChallengeRequirement requirement, int completionAmount,
			@NotNull ActionType action, @NotNull Action<Player>[] rewards) {
		this.id = id;
		this.requirement = requirement;
		this.completionAmount = completionAmount;
		this.action = action;
		this.rewards = rewards;
	}

	/**
	 * Returns the unique challenge ID.
	 *
	 * @return the challenge identifier string
	 */
	@NotNull
	public String getChallengeId() {
		return this.id;
	}

	/**
	 * Returns the challenge's requirement logic.
	 *
	 * @return the {@link ChallengeRequirement} instance
	 */
	@NotNull
	public ChallengeRequirement getRequiredData() {
		return this.requirement;
	}

	/**
	 * Returns the amount needed to complete this challenge.
	 *
	 * @return the required amount
	 */
	public int getNeededAmount() {
		return this.completionAmount;
	}

	/**
	 * Returns the type of action this challenge tracks.
	 *
	 * @return the {@link ActionType}
	 */
	@NotNull
	public ActionType getChallengeType() {
		return this.action;
	}

	/**
	 * Returns the actions to be executed as rewards when the challenge is
	 * completed.
	 *
	 * @return an array of reward actions
	 */
	@NotNull
	public Action<Player>[] getRewardActions() {
		return this.rewards;
	}
}