package com.swiftlicious.hellblock.player;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;
import com.swiftlicious.hellblock.challenges.ProgressBar;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;

public class ChallengeData {

	private static final int PROGRESS_BAR_WIDTH = 25;

	@Expose
	@SerializedName("progression")
	protected Map<ChallengeType, ChallengeResult> challenges;

	public ChallengeData(Map<ChallengeType, ChallengeResult> challenges) {
		this.challenges = challenges;
	}

	public @NotNull Map<ChallengeType, ChallengeResult> getChallenges() {
		return this.challenges;
	}

	private ChallengeResult getResult(@NotNull ChallengeType challenge) {
		return challenges.get(challenge);
	}

	public int getChallengeProgress(@NotNull ChallengeType challenge) {
		final ChallengeResult result = getResult(challenge);
		return (result != null && result.getStatus() == CompletionStatus.IN_PROGRESS) ? result.getProgress() : 0;
	}

	public boolean isChallengeActive(@NotNull ChallengeType challenge) {
		final ChallengeResult result = getResult(challenge);
		return result != null && result.getStatus() == CompletionStatus.IN_PROGRESS;
	}

	public boolean isChallengeCompleted(@NotNull ChallengeType challenge) {
		final ChallengeResult result = getResult(challenge);
		return result != null && result.getProgress() >= challenge.getNeededAmount();
	}

	public boolean isChallengeRewardClaimed(@NotNull ChallengeType challenge) {
		final ChallengeResult result = getResult(challenge);
		return result != null && result.getStatus() == CompletionStatus.COMPLETED
				&& result.getProgress() == challenge.getNeededAmount() && result.isRewardClaimed();
	}

	public void setChallenges(@NotNull Map<ChallengeType, ChallengeResult> challenges) {
		this.challenges = challenges;
	}

	public void setChallengeRewardAsClaimed(@NotNull ChallengeType challenge, boolean claimedReward) {
		final ChallengeResult result = getResult(challenge);
		if (result == null || result.getStatus() != CompletionStatus.COMPLETED) {
			return;
		}
		result.setProgress(challenge.getNeededAmount());
		result.setRewardClaimed(true);
	}

	public void beginChallengeProgression(@NotNull Player player, @NotNull ChallengeType challenge) {
		final HellblockChallenge newChallenge = new HellblockChallenge(challenge, CompletionStatus.IN_PROGRESS, 1);
		challenges.putIfAbsent(newChallenge.getChallengeType(),
				new ChallengeResult(newChallenge.getCompletionStatus(), newChallenge.getProgress(), false));

		sendProgressBar(player, challenge);
	}

	public void updateChallengeProgression(@NotNull Player player, @NotNull ChallengeType challenge,
			int progressToAdd) {
		final ChallengeResult result = getResult(challenge);
		if (result == null || result.getStatus() != CompletionStatus.IN_PROGRESS) {
			return;
		}
		result.setProgress(result.getProgress() + progressToAdd);

		sendProgressBar(player, challenge);
	}

	public void completeChallenge(@NotNull Player player, @NotNull ChallengeType challenge) {
		final ChallengeResult result = getResult(challenge);
		if (result == null || result.getStatus() != CompletionStatus.IN_PROGRESS) {
			return;
		}

		challenges.remove(challenge);

		final HellblockChallenge completedChallenge = new HellblockChallenge(challenge, CompletionStatus.COMPLETED,
				challenge.getNeededAmount());

		challenges.putIfAbsent(completedChallenge.getChallengeType(),
				new ChallengeResult(completedChallenge.getCompletionStatus(), challenge.getNeededAmount(), false));

		HellblockPlugin.getInstance().getChallengeManager().performChallengeCompletionActions(player, challenge);
	}

	private void sendProgressBar(@NotNull Player player, @NotNull ChallengeType challenge) {
		final ChallengeResult result = getResult(challenge);
		if (result == null) {
			return;
		}

		VersionHelper.getNMSManager()
				.sendActionBar(player,
						AdventureHelper.componentToJson(HellblockPlugin.getInstance().getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_CHALLENGE_PROGRESS_BAR.arguments(
										AdventureHelper.miniMessage(String.valueOf(result.getProgress())),
										AdventureHelper.miniMessage(String.valueOf(challenge.getNeededAmount())),
										AdventureHelper.miniMessage(ProgressBar.getProgressBar(
												new ProgressBar(challenge.getNeededAmount(), result.getProgress()),
												PROGRESS_BAR_WIDTH)))
										.build())));
	}

	/**
	 * Creates an instance of ChallengeData with default values (empty map).
	 *
	 * @return a new instance of ChallengeData with default values.
	 */
	public static @NotNull ChallengeData empty() {
		return new ChallengeData(new HashMap<>());
	}

	public @NotNull ChallengeData copy() {
		final Map<ChallengeType, ChallengeResult> copy = new HashMap<>();
		// Ensure ChallengeResult has copy()
		challenges.entrySet().forEach(entry -> copy.put(entry.getKey(), entry.getValue().copy()));
		return new ChallengeData(copy);
	}
}