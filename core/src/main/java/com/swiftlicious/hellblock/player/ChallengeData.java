package com.swiftlicious.hellblock.player;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.HellblockChallenge;
import com.swiftlicious.hellblock.challenges.ProgressBar;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;

import lombok.NonNull;

public class ChallengeData {

	@Expose
	@SerializedName("progression")
	protected Map<ChallengeType, ChallengeResult> challenges;

	public ChallengeData(Map<ChallengeType, ChallengeResult> challenges) {
		this.challenges = challenges;
	}

	public @Nullable Map<ChallengeType, ChallengeResult> getChallenges() {
		return this.challenges;
	}

	public int getChallengeProgress(@NonNull ChallengeType challenge) {
		int progress = 0;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, ChallengeResult> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					if (challenges.getValue().getStatus() == CompletionStatus.IN_PROGRESS) {
						progress = challenges.getValue().getProgress();
						break;
					}
				}
			}
		}
		return progress;
	}

	public boolean isChallengeActive(@NonNull ChallengeType challenge) {
		boolean active = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, ChallengeResult> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					active = challenges.getValue().getStatus() == CompletionStatus.IN_PROGRESS;
					break;
				}
			}
		}
		return active;
	}

	public boolean isChallengeCompleted(@NonNull ChallengeType challenge) {
		boolean completed = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, ChallengeResult> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					completed = challenges.getValue().getProgress() >= challenge.getNeededAmount();
					break;
				}
			}
		}
		return completed;
	}

	public boolean isChallengeRewardClaimed(@NonNull ChallengeType challenge) {
		boolean claimed = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, ChallengeResult> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					if (challenges.getValue().getStatus() == CompletionStatus.COMPLETED) {
						if (challenges.getValue().getProgress() == challenge.getNeededAmount()) {
							claimed = challenges.getValue().isRewardClaimed();
							break;
						}
					}
				}
			}
		}
		return claimed;
	}

	public void setChallenges(@Nullable Map<ChallengeType, ChallengeResult> challenges) {
		this.challenges = challenges;
	}

	public void setChallengeRewardAsClaimed(@NonNull ChallengeType challenge, boolean claimedReward) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getStatus() == CompletionStatus.COMPLETED) {
			this.challenges.get(challenge).setProgress(challenge.getNeededAmount());
			this.challenges.get(challenge).setRewardClaimed(true);
		}
	}

	public void beginChallengeProgression(@NonNull Player player, @NonNull ChallengeType challenge) {
		HellblockChallenge newChallenge = new HellblockChallenge(challenge, CompletionStatus.IN_PROGRESS, 1);
		this.challenges.putIfAbsent(newChallenge.getChallengeType(),
				new ChallengeResult(newChallenge.getCompletionStatus(), newChallenge.getProgress(), false));
		HellblockPlugin.getInstance().getAdventureManager().sendActionbar(player, String.format(
				"<yellow>Progress <gold>(%s/%s)<gray>: %s", this.challenges.get(challenge).getProgress(),
				challenge.getNeededAmount(),
				ProgressBar.getProgressBar(
						new ProgressBar(challenge.getNeededAmount(), this.challenges.get(challenge).getProgress()),
						25)));
	}

	public void updateChallengeProgression(@NonNull Player player, @NonNull ChallengeType challenge,
			int progressToAdd) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getStatus() == CompletionStatus.IN_PROGRESS) {
			this.challenges.get(challenge).setProgress(this.challenges.get(challenge).getProgress() + progressToAdd);
			HellblockPlugin.getInstance().getAdventureManager().sendActionbar(player, String.format(
					"<yellow>Progress <gold>(%s/%s)<gray>: %s", this.challenges.get(challenge).getProgress(),
					challenge.getNeededAmount(),
					ProgressBar.getProgressBar(
							new ProgressBar(challenge.getNeededAmount(), this.challenges.get(challenge).getProgress()),
							25)));
		}
	}

	public void completeChallenge(@NonNull Player player, @NonNull ChallengeType challenge) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getStatus() == CompletionStatus.IN_PROGRESS) {
			this.challenges.remove(challenge);
			HellblockChallenge completedChallenge = new HellblockChallenge(challenge, CompletionStatus.COMPLETED,
					challenge.getNeededAmount());
			this.challenges.putIfAbsent(completedChallenge.getChallengeType(),
					new ChallengeResult(completedChallenge.getCompletionStatus(), challenge.getNeededAmount(), false));
			HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeCompletionActions(player,
					challenge);
		}
	}

	/**
	 * Creates an instance of ChallengeData with default values (empty map).
	 *
	 * @return a new instance of ChallengeData with default values.
	 */
	public static @NonNull ChallengeData empty() {
		return new ChallengeData(new HashMap<>());
	}

	public @NonNull ChallengeData copy() {
		return new ChallengeData(challenges);
	}
}
