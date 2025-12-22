package com.swiftlicious.hellblock.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;
import com.swiftlicious.hellblock.challenges.ProgressBar;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * The {@code ChallengeData} class holds the current progress state for various
 * challenges a player is undertaking. It supports operations to begin, update,
 * complete challenges, and check reward status. This class implements
 * {@link EmptyCheck} to allow checking if there are any active challenges.
 * <p>
 * Challenge progress and status are managed using a
 * {@code Map<ChallengeType, ChallengeResult>}.
 */
public class ChallengeData implements EmptyCheck {

	private static final int PROGRESS_BAR_WIDTH = 25;

	@Expose
	@SerializedName("progression")
	protected Map<ChallengeType, ChallengeResult> challenges;

	// Runtime-only metadata for tracking per-challenge contextual data (e.g., start
	// level)
	private final Map<ChallengeType, Map<String, Object>> challengeMeta = new HashMap<>();

	/**
	 * Constructs a new {@code ChallengeData} instance with the given challenges
	 * map.
	 *
	 * @param challenges a map of {@link ChallengeType} to {@link ChallengeResult}
	 */
	public ChallengeData(@NotNull Map<ChallengeType, ChallengeResult> challenges) {
		this.challenges = challenges.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy()));
	}

	/**
	 * Returns the current map of challenges and their results.
	 *
	 * @return a non-null map of challenge types to challenge results
	 */
	@NotNull
	public Map<ChallengeType, ChallengeResult> getChallenges() {
		if (this.challenges == null) {
			return new HashMap<>();
		}
		return this.challenges;
	}

	/**
	 * Returns the {@link ChallengeResult} for the specified challenge.
	 *
	 * @param challengeType the challenge to look up
	 * @return the result associated with the challenge, or null if not present
	 */
	@Nullable
	private ChallengeResult getResult(@NotNull ChallengeType challengeType) {
		return challenges.get(challengeType);
	}

	/**
	 * Gets the current progress value for a given challenge.
	 *
	 * @param challengeType the challenge to query
	 * @return the progress if in progress, otherwise 0
	 */
	public double getChallengeProgress(@NotNull ChallengeType challengeType) {
		final ChallengeResult result = getResult(challengeType);
		return (result != null && result.getStatus() == CompletionStatus.IN_PROGRESS) ? result.getProgress() : 0.0;
	}

	/**
	 * Checks if the specified challenge is currently active (in progress).
	 *
	 * @param challengeType the challenge to check
	 * @return true if the challenge is in progress, false otherwise
	 */
	public boolean isChallengeActive(@NotNull ChallengeType challengeType) {
		final ChallengeResult result = getResult(challengeType);
		return result != null && result.getStatus() == CompletionStatus.IN_PROGRESS;
	}

	/**
	 * Checks if the specified challenge has been completed (progress >= required
	 * amount).
	 *
	 * @param challengeType the challenge to check
	 * @return true if completed, false otherwise
	 */
	public boolean isChallengeCompleted(@NotNull ChallengeType challengeType) {
		final ChallengeResult result = getResult(challengeType);
		return result != null && result.getProgress() >= challengeType.getNeededAmount();
	}

	/**
	 * Checks if the reward for a completed challenge has been claimed.
	 *
	 * @param challengeType the challenge to check
	 * @return true if the reward has been claimed, false otherwise
	 */
	public boolean isChallengeRewardClaimed(@NotNull ChallengeType challengeType) {
		final ChallengeResult result = getResult(challengeType);
		return result != null && result.getStatus() == CompletionStatus.COMPLETED
				&& result.getProgress() == challengeType.getNeededAmount() && result.isRewardClaimed();
	}

	/**
	 * Replaces the current challenge map with the given one.
	 *
	 * @param challenges the new challenge map to set
	 */
	public void setChallenges(@NotNull Map<ChallengeType, ChallengeResult> challenges) {
		this.challenges = challenges;
	}

	/**
	 * Marks the reward for a completed challenge as claimed. Does nothing if the
	 * challenge is not completed.
	 *
	 * @param challengeType the challenge whose reward is to be marked
	 * @param claimedReward true to mark reward as claimed
	 */
	public void setChallengeRewardAsClaimed(@NotNull ChallengeType challengeType, boolean claimedReward) {
		final ChallengeResult result = getResult(challengeType);
		if (result == null || result.getStatus() != CompletionStatus.COMPLETED) {
			return;
		}
		result.setProgress(challengeType.getNeededAmount());
		result.setRewardClaimed(true);
	}

	/**
	 * Starts a new challenge for the player, setting its status to
	 * {@code IN_PROGRESS}.
	 *
	 * @param player    the player starting the challenge
	 * @param challenge the challenge to begin
	 */
	public void beginChallengeProgression(@NotNull Player player, @NotNull ChallengeType challengeType) {
		final HellblockChallenge newChallenge = new HellblockChallenge(challengeType, CompletionStatus.IN_PROGRESS, 1);
		challenges.putIfAbsent(newChallenge.getChallengeType(),
				new ChallengeResult(newChallenge.getCompletionStatus(), newChallenge.getProgress(), false));

		// Store island start level for relative LEVELUP challenges
		if (challengeType.getChallengeType() == ActionType.LEVELUP) {
			Optional<UserData> userDataOpt = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (userDataOpt.isEmpty()) {
				return;
			}

			UserData userData = userDataOpt.get();
			if (!userData.getHellblockData().hasHellblock()) {
				return;
			}

			UUID ownerId = userData.getHellblockData().getOwnerUUID();

			if (ownerId == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
								+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			HellblockPlugin.getInstance().getStorageManager().getCachedUserDataWithFallback(ownerId, false)
					.thenAccept(optData -> {
						if (optData.isEmpty()) {
							return;
						}

						UserData ownerData = optData.get();
						double startLevel = ownerData.getHellblockData().getIslandLevel();
						setChallengeMeta(challengeType, "startLevel", startLevel);
					});
		}

		sendProgressBar(player, challengeType);
	}

	/**
	 * Adds progress to an ongoing challenge and updates the player's action bar.
	 * Ignores the call if the challenge is not in progress.
	 *
	 * @param player        the player progressing the challenge
	 * @param challengeType the challenge to update
	 * @param progressToAdd the amount of progress to add
	 */
	public void updateChallengeProgression(@NotNull Player player, @NotNull ChallengeType challengeType,
			double progressToAdd) {
		final ChallengeResult result = getResult(challengeType);
		if (result == null || result.getStatus() != CompletionStatus.IN_PROGRESS) {
			return;
		}
		result.setProgress(result.getProgress() + progressToAdd);

		sendProgressBar(player, challengeType);
	}

	/**
	 * Completes a challenge for the player, finalizing its progress and performing
	 * completion actions. Replaces the current entry in the challenge map with a
	 * new completed result.
	 *
	 * @param player        the player completing the challenge
	 * @param challengeType the challenge to complete
	 */
	public void completeChallenge(@NotNull Player player, @NotNull ChallengeType challengeType) {
		final ChallengeResult result = getResult(challengeType);
		if (result == null || result.getStatus() != CompletionStatus.IN_PROGRESS) {
			return;
		}

		challenges.remove(challengeType);

		final HellblockChallenge completedChallenge = new HellblockChallenge(challengeType, CompletionStatus.COMPLETED,
				challengeType.getNeededAmount());

		challenges.putIfAbsent(completedChallenge.getChallengeType(),
				new ChallengeResult(completedChallenge.getCompletionStatus(), challengeType.getNeededAmount(), false));

		// Trigger completion effects
		HellblockPlugin.getInstance().getChallengeManager().performChallengeCompletionActions(player, challengeType);

		// Remove temporary metadata
		clearChallengeMeta(challengeType);
	}

	/**
	 * Sends a progress bar update to the player's action bar for the given
	 * challenge.
	 *
	 * @param player        the player to notify
	 * @param challengeType the challenge whose progress to display
	 */
	private void sendProgressBar(@NotNull Player player, @NotNull ChallengeType challengeType) {
		final ChallengeResult result = getResult(challengeType);
		if (result == null) {
			return;
		}

		double progress = result.getProgress();
		int needed = challengeType.getNeededAmount();

		// Determine gradient colors dynamically based on progress percentage
		double percent = (needed == 0) ? 0 : progress / needed;
		List<String> gradient;

		if (percent < 0.33) {
			gradient = List.of("<#FF0000>", "<#FF8000>"); // red → orange
		} else if (percent < 0.66) {
			gradient = List.of("<#FF8000>", "<#FFFF00>"); // orange → yellow
		} else {
			gradient = List.of("<#00FF00>", "<#ADFF2F>", "<#FFFF00>"); // green → lime → yellow
		}

		// Optional animation pulse near completion (>90%)
		double intensity = Math.min(1.0, (percent - 0.9) / 0.1); // 0 → 1 between 90–100%
		double phase = (Math.sin(System.currentTimeMillis() / 500.0) + 1) / 2.0 * intensity;

		String progressBar = (percent >= 0.9)
				? ProgressBar.getAnimatedGradientBar(new ProgressBar(needed, progress), PROGRESS_BAR_WIDTH, gradient,
						phase)
				: ProgressBar.getMultiGradientBar(new ProgressBar(needed, progress), PROGRESS_BAR_WIDTH, gradient);

		// Send fancy action bar
		VersionHelper.getNMSManager()
				.sendActionBar(player,
						AdventureHelper.componentToJson(HellblockPlugin.getInstance().getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_CHALLENGE_PROGRESS_BAR.arguments(
										AdventureHelper.miniMessageToComponent(ProgressBar.formatValue(progress)),
										AdventureHelper.miniMessageToComponent(ProgressBar.formatValue(needed)),
										AdventureHelper.miniMessageToComponent(progressBar)).build())));
	}

	/**
	 * Stores a metadata value associated with a specific challenge.
	 *
	 * @param challengeType the challenge to associate metadata with
	 * @param key           the metadata key
	 * @param value         the value to store
	 */
	public void setChallengeMeta(@NotNull ChallengeType challengeType, @NotNull String key, @NotNull Object value) {
		challengeMeta.computeIfAbsent(challengeType, c -> new HashMap<>()).put(key, value);
	}

	/**
	 * Retrieves a metadata value for a specific challenge.
	 *
	 * @param challengeType the challenge to get metadata for
	 * @param key           the metadata key
	 * @param type          the expected value type
	 * @return an {@link Optional} containing the value if present and of the
	 *         correct type
	 */
	public <T> Optional<T> getChallengeMeta(@NotNull ChallengeType challengeType, @NotNull String key,
			@NotNull Class<T> type) {
		return Optional.ofNullable(challengeMeta.get(challengeType)).map(map -> map.get(key)).filter(type::isInstance)
				.map(type::cast);
	}

	/**
	 * Removes all metadata for a specific challenge (e.g., on completion).
	 *
	 * @param challengeType the challenge whose metadata should be cleared
	 */
	public void clearChallengeMeta(@NotNull ChallengeType challengeType) {
		challengeMeta.remove(challengeType);
	}

	/**
	 * Removes all metadata for all challenges.
	 */
	public void clearChallengeMeta() {
		challengeMeta.clear();
	}

	/**
	 * Creates and returns a new empty {@code ChallengeData} instance.
	 *
	 * @return a {@code ChallengeData} instance with an empty challenge map
	 */
	@NotNull
	public static ChallengeData empty() {
		return new ChallengeData(new HashMap<>());
	}

	/**
	 * Creates a deep copy of this {@code ChallengeData} instance.
	 *
	 * @return a new {@code ChallengeData} with copied challenge data
	 */
	@NotNull
	public final ChallengeData copy() {
		final Map<ChallengeType, ChallengeResult> copy = new HashMap<>();
		challenges.entrySet().forEach(entry -> copy.put(entry.getKey(), entry.getValue().copy()));
		return new ChallengeData(copy);
	}

	/**
	 * Checks whether this {@code ChallengeData} contains any challenges.
	 *
	 * @return true if no challenges are present, false otherwise
	 */
	@Override
	public boolean isEmpty() {
		return this.challenges.isEmpty();
	}
}