package com.swiftlicious.hellblock.challenges;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.challenges.requirement.FarmRequirement;
import com.swiftlicious.hellblock.challenges.requirement.FishRequirement;
import com.swiftlicious.hellblock.challenges.requirement.LevelUpRequirement;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.LevelHandler.LevelProgressContext;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.extras.Action;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;

/**
 * Manages all Hellblock challenge logic, including registration, configuration
 * loading, progression handling, and reward distribution.
 * <p>
 * This class loads all challenges from <code>challenges.yml</code>, parses
 * their requirements and rewards, and handles player progression updates.
 * </p>
 */
public class ChallengeManager implements Reloadable {

	protected final HellblockPlugin instance;

	/** The loaded YAML configuration document for challenges. */
	protected YamlDocument challengeConfig;

	/** All registered challenge types loaded from configuration. */
	protected final Set<ChallengeType> challenges = ConcurrentHashMap.newKeySet();

	/** Actions that are executed globally when any challenge is completed. */
	protected Action<Player>[] challengeGlobalActions;

	/** Factory used to build challenge requirements dynamically. */
	protected ChallengeFactory challengeFactory;

	public ChallengeManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void reload() {
		loadChallengesConfig();

		final Section globalChallengeSection = getChallengeConfig().getSection("global");
		if (globalChallengeSection != null) {
			Section globalChallengeActionsSection = globalChallengeSection.getSection("action");
			if (globalChallengeActionsSection != null) {
				this.challengeGlobalActions = instance.getActionManager(Player.class)
						.parseActions(globalChallengeActionsSection);
			}
		}

		registerChallengesFromConfig();
	}

	/** @return The YAML configuration document backing all challenge data. */
	@NotNull
	public YamlDocument getChallengeConfig() {
		return this.challengeConfig;
	}

	private void loadChallengesConfig() {
		try (InputStream inputStream = new FileInputStream(
				instance.getConfigManager().resolveConfig("challenges.yml").toFile())) {
			challengeConfig = YamlDocument.create(inputStream,
					instance.getConfigManager().getResourceMaybeGz("challenges.yml"),
					GeneralSettings.builder().setRouteSeparator('.').setUseDefaults(false).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder().setScalarFormatter((tag, value, role, def) -> {
						if (role == NodeRole.KEY) {
							return ScalarStyle.PLAIN;
						} else {
							return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
						}
					}).build());
			challengeConfig.save(instance.getConfigManager().resolveConfig("challenges.yml").toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Parses the <code>challenges.yml</code> configuration and registers all
	 * defined challenges. Each challenge includes a required action type,
	 * completion amount, and associated rewards.
	 */
	private void registerChallengesFromConfig() {
		this.challengeFactory = new ChallengeFactory(instance);

		final Section challengeSection = getChallengeConfig().getSection("challenges");
		if (challengeSection == null) {
			instance.getPluginLogger().warn("challenges section missing from challenges.yml!");
			return;
		}

		this.challenges.clear();

		// --- Register all challenges immediately at startup ---
		Set<ChallengeType> challenges = parseHellblockChallenges(challengeSection);
		instance.debug("A total of " + challenges.size() + " challenge" + (challenges.size() == 1 ? "" : "s")
				+ " have been registered!");
	}

	@NotNull
	public Set<ChallengeType> parseHellblockChallenges(@NotNull Section challengeSection) {
		Map<ActionType, List<String>> foundByType = new LinkedHashMap<>();
		Map<ActionType, List<String>> registeredByType = new LinkedHashMap<>();

		instance.debug("Scanning challenges.yml for challenges to generate...");

		for (Entry<String, Object> entry : challengeSection.getStringRouteMappedValues(false).entrySet()) {
			String id = entry.getKey();

			if (!(entry.getValue() instanceof Section inner)) {
				instance.getPluginLogger().warn("Challenge '" + id + "' is not a valid section, skipping.");
				continue;
			}

			final String actionStr = inner.getString("action");
			if (actionStr == null) {
				instance.getPluginLogger().warn("Action type was not found for challenge '" + id + "'");
				continue;
			}

			ActionType action;

			try {
				// Parse action type
				action = ActionType.valueOf(actionStr.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				instance.getPluginLogger().warn("Invalid action type '" + actionStr + "' for challenge '" + id + "'");
				e.printStackTrace();
				continue;
			}

			foundByType.computeIfAbsent(action, k -> new ArrayList<>()).add(id);

			final int completionAmount = inner.getInt("needed-amount");
			if (completionAmount <= 0) {
				instance.getPluginLogger().warn("Completion amount must be above 0 for challenge '" + id + "'");
				continue;
			}

			ChallengeRequirement requirement;

			try {
				// Parse requirement type
				Object rawData = inner.get("data");
				Section dataSection;

				if (rawData instanceof Section section) {
					// already a section
					dataSection = section;
				} else {
					YamlDocument tempDoc = YamlDocument.create(new ByteArrayInputStream(new byte[0]));
					dataSection = tempDoc.createSection("temp");
					if (rawData != null) {
						dataSection.set("", rawData);
					}
				}

				requirement = challengeFactory.create(action, dataSection);
			} catch (Throwable e) {
				instance.getPluginLogger()
						.warn("Failed to create requirement for challenge '" + id + "': " + e.getMessage());
				e.printStackTrace();
				continue;
			}

			if (requirement == null)
				continue;

			Action<Player>[] rewards = null;

			try {
				// Parse rewards
				Section rewardsSection = inner.getSection("rewards");
				if (rewardsSection != null) {
					rewards = instance.getActionManager(Player.class).parseActions(rewardsSection);
				}
			} catch (Throwable e) {
				instance.getPluginLogger()
						.warn("Failed to parse rewards for challenge '" + id + "': " + e.getMessage());
				e.printStackTrace();
				continue;
			}

			if (rewards == null)
				continue;

			// Register challenge
			final ChallengeType challenge = new ChallengeType(id, requirement, completionAmount, action, rewards);
			this.challenges.add(challenge);
			registeredByType.computeIfAbsent(action, k -> new ArrayList<>()).add(id);
		}

		foundByType.forEach((action, ids) -> instance.debug("Group: {" + action + "} Found: " + ids));

		registeredByType.forEach(
				(action, ids) -> instance.debug("Group: {" + action + "} Registered (" + ids.size() + "): " + ids));
		return this.challenges;
	}

	/**
	 * Retrieves a challenge definition by its unique ID.
	 *
	 * @param challengeId The ID of the challenge to fetch.
	 * @return The matching challenge, or {@code null} if not found.
	 */
	@Nullable
	public ChallengeType getById(@NotNull String challengeId) {
		return this.challenges.stream().filter(ch -> ch.getChallengeId().equalsIgnoreCase(challengeId)).findFirst()
				.orElse(null);
	}

	/**
	 * Retrieves all challenges that match a specific {@link ActionType}.
	 *
	 * @param action The action type to filter by (e.g., BLOCK_BREAK, FISH_CATCH,
	 *               etc.)
	 * @return A list of challenges matching the specified action type.
	 */
	@NotNull
	public List<ChallengeType> getByActionType(@NotNull ActionType action) {
		return this.challenges.stream().filter(ch -> ch.getChallengeType().equals(action)).collect(Collectors.toList());
	}

	/**
	 * Performs all global and per-challenge completion effects, such as sending
	 * messages, playing sounds, and spawning celebratory particles or fireworks.
	 *
	 * @param player    The player who completed the challenge.
	 * @param challenge The completed challenge.
	 */
	public void performChallengeCompletionActions(@NotNull Player player, @NotNull ChallengeType challenge) {
		final Sender audience = instance.getSenderFactory().wrap(player);

		// Send centered completion message
		AdventureHelper.sendCenteredMessage(audience,
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_CHALLENGE_COMPLETED.build()));

		// Play completion sound
		if (instance.getConfigManager().challengeCompleteSound() != null) {
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					instance.getConfigManager().challengeCompleteSound());
		}

		// Spawn celebratory fake firework
		final FakeFirework firework = VersionHelper.getNMSManager().createFakeFirework(player.getLocation(),
				Color.GREEN);
		firework.flightTime(0);
		firework.invisible(true);
		firework.spawn(player);
	}

	/**
	 * Executes the configured reward actions for a completed challenge. This
	 * includes both per-challenge and global rewards.
	 *
	 * @param player    The player receiving the rewards.
	 * @param challenge The completed challenge type.
	 */
	public void performChallengeRewardActions(@NotNull Player player, @NotNull ChallengeType challenge) {
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final Context<Player> context = Context.player(player);
		ActionManager.trigger(context, challenge.getRewardActions());
		onlineUser.get().getChallengeData().setChallengeRewardAsClaimed(challenge, true);

		if (this.challengeGlobalActions != null) {
			ActionManager.trigger(context, this.challengeGlobalActions);
		}
	}

	/**
	 * Handles a challenge progression event — increments progress by 1 unit.
	 *
	 * @param userData The user data associated with the player.
	 * @param type     The challenge action type (e.g., FISH_CATCH).
	 * @param context  The event context used for requirement matching.
	 */
	public void handleChallengeProgression(@NotNull UserData userData, @NotNull ActionType type,
			@NotNull Object context) {
		handleChallengeProgression(userData, type, context, 1);
	}

	/**
	 * Handles challenge progression logic for a specific player and action type.
	 * Checks if the triggered event matches any active challenges and updates their
	 * progress.
	 *
	 * @param userData          The user data of the player.
	 * @param type              The action type performed by the player.
	 * @param context           The event context to check requirement matching
	 *                          against.
	 * @param progressionAmount The amount of progress to add (e.g., +1 per fish
	 *                          caught).
	 */
	public void handleChallengeProgression(@NotNull UserData userData, @NotNull ActionType type,
			@NotNull Object context, double progressionAmount) {
		final HellblockData data = userData.getHellblockData();
		if (!data.hasHellblock()) {
			return; // Skip if the player doesn't have an active Hellblock
		}

		final List<ChallengeType> challenges = getByActionType(type);
		final var challengeData = userData.getChallengeData();

		Player player = userData.getPlayer();
		if (player == null || !player.isOnline()) {
			return;
		}

		final UUID ownerId = data.getOwnerUUID();
		if (ownerId == null) {
			instance.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
					+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
			throw new IllegalStateException(
					"Owner reference was null. This should never happen — please report to the developer.");
		}

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(ownerOptData -> {
					if (ownerOptData.isEmpty()) {
						return;
					}

					UserData ownerData = ownerOptData.get();

					// Build reusable player context for placeholders and requirements
					final Context<Player> playerCtx = Context.player(player);

					for (ChallengeType challenge : challenges) {

						final ChallengeRequirement requirement = challenge.getRequiredData();

						Object requirementContext = context;

						// If this is a relative LEVELUP challenge, wrap context
						if (type == ActionType.LEVELUP && requirement instanceof LevelUpRequirement levelReq
								&& levelReq.isRelative()) {
							double currentLevel = ownerData.getHellblockData().getIslandLevel();

							double startLevel = challengeData.getChallengeMeta(challenge, "startLevel", Double.class)
									.orElse(currentLevel); // if missing (e.g. join check), fallback to current

							requirementContext = new LevelProgressContext(startLevel, currentLevel);
						}

						checkRequirementMatch(requirement, requirementContext, playerCtx).thenAccept(matches -> {
							if (!matches)
								return;

							// Start or update challenge progression
							if (!challengeData.isChallengeActive(challenge)
									&& !challengeData.isChallengeCompleted(challenge)) {
								challengeData.beginChallengeProgression(player, challenge);
							} else {
								if (type == ActionType.LEVELUP && requirement instanceof LevelUpRequirement levelReq
										&& levelReq.isRelative()) {
									double currentLevel = ownerData.getHellblockData().getIslandLevel();
									double startLevel = challengeData
											.getChallengeMeta(challenge, "startLevel", Double.class)
											.orElse(currentLevel);
									double gained = Math.max(0, currentLevel - startLevel);
									challengeData.updateChallengeProgression(player, challenge, gained);
								} else {
									challengeData.updateChallengeProgression(player, challenge, progressionAmount);
								}

								if (challengeData.isChallengeCompleted(challenge)) {
									challengeData.completeChallenge(player, challenge);
								}
							}
						});
					}
				});
	}

	@NotNull
	private CompletableFuture<Boolean> checkRequirementMatch(@NotNull ChallengeRequirement requirement,
			@NotNull Object context, @NotNull Context<Player> playerCtx) {
		if (requirement instanceof FishRequirement fishReq) {
			return CompletableFuture.completedFuture(fishReq.matchesWithContext(context, playerCtx));
		} else if (requirement instanceof FarmRequirement farmReq) {
			return farmReq.matchesAsync(context); // This returns CompletableFuture<Boolean>
		} else if (requirement instanceof AbstractItemRequirement itemReq) {
			return CompletableFuture.completedFuture(itemReq.matchesWithContext(context, playerCtx));
		} else {
			return CompletableFuture.completedFuture(requirement.matches(context));
		}
	}
}