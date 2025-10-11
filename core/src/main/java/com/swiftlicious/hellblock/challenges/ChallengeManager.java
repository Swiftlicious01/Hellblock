package com.swiftlicious.hellblock.challenges;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.challenges.requirement.FishRequirement;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.extras.Action;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ChallengeManager implements Reloadable {

	protected final HellblockPlugin instance;

	protected YamlDocument challengeConfig;

	protected final List<ChallengeType> challenges = new ArrayList<>();

	protected Action<Player>[] challengeGlobalActions;

	protected ChallengeFactory challengeFactory;

	public ChallengeManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		final File challengeFile = new File(instance.getDataFolder(), "challenges.yml");
		if (!challengeFile.exists()) {
			instance.saveResource("challenges.yml", false);
		}
		this.challengeConfig = instance.getConfigManager().loadData(challengeFile);
		final Section globalChallengeSection = challengeConfig.getSection("global");
		if (globalChallengeSection != null) {
			this.challengeGlobalActions = instance.getActionManager(Player.class)
					.parseActions(globalChallengeSection.getSection("action"));
		}
		this.challenges.clear();
		registerChallengesFromConfig();
	}

	public YamlDocument getChallengeConfig() {
		return this.challengeConfig;
	}

	private void registerChallengesFromConfig() {
		this.challengeFactory = new ChallengeFactory(instance);
		final Section challengeSection = getChallengeConfig().getSection("challenges");
		if (challengeSection == null) {
			instance.getPluginLogger().warn("No 'challenges' section found in config.");
			return;
		}

		final Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> (item,
				context) -> challengeSection.getStringRouteMappedValues(false).forEach((id, value) -> {
					if (!(value instanceof Section inner)) {
						instance.getPluginLogger().warn("Challenge '" + id + "' is not a valid section, skipping.");
						return;
					}
					final int completionAmount = inner.getInt("needed-amount");
					final String actionStr = inner.getString("action");
					final ActionType action;
					try {
						action = ActionType.valueOf(Objects.requireNonNull(actionStr).toUpperCase(Locale.ROOT));
					} catch (IllegalArgumentException e) {
						instance.getPluginLogger()
								.warn("Invalid action type '" + actionStr + "' for challenge '" + id + "'");
						return;
					}
					final ChallengeRequirement requirement;
					try {
						requirement = challengeFactory.create(action, inner.getSection("data"));
					} catch (Exception e) {
						instance.getPluginLogger()
								.warn("Failed to create requirement for challenge '" + id + "': " + e.getMessage());
						return;
					}
					final Action<Player>[] rewards;
					try {
						rewards = instance.getActionManager(Player.class).parseActions(inner.getSection("rewards"));
					} catch (Exception e) {
						instance.getPluginLogger()
								.warn("Failed to parse rewards for challenge '" + id + "': " + e.getMessage());
						return;
					}
					final ChallengeType challenge = new ChallengeType(id, requirement, completionAmount, action,
							rewards);
					this.challenges.add(challenge);
					instance.getPluginLogger().info("Registered challenge: " + id + " (" + action + ")");
				});

		instance.getConfigManager().registerItemParser(f1, 67_00, "challenges");
	}

	public @Nullable ChallengeType getById(@NotNull String id) {
		return this.challenges.stream().filter(ch -> ch.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
	}

	public @NotNull List<ChallengeType> getByActionType(@NotNull ActionType action) {
		return this.challenges.stream().filter(ch -> ch.getChallengeType().equals(action)).collect(Collectors.toList());
	}

	public void performChallengeCompletionActions(@NotNull Player player, @NotNull ChallengeType challenge) {
		final Sender audience = instance.getSenderFactory().wrap(player);
		AdventureHelper.sendCenteredMessage(audience,
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_CHALLENGE_COMPLETED.build()));
		if (instance.getConfigManager().challengeCompleteSound() != null) {
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					instance.getConfigManager().challengeCompleteSound());
		}
		final FakeFirework firework = VersionHelper.getNMSManager().createFakeFirework(player.getLocation(),
				Color.GREEN);
		firework.flightTime(0);
		firework.invisible(true);
		firework.spawn(player);
	}

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

	public void handleChallengeProgression(@NotNull Player player, @NotNull ActionType type, @NotNull Object context) {
		handleChallengeProgression(player, type, context, 1);
	}

	public void handleChallengeProgression(@NotNull Player player, @NotNull ActionType type, @NotNull Object context,
			int progressionAmount) {
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final UserData user = onlineUser.get();
		final HellblockData data = user.getHellblockData();
		if (!data.hasHellblock()) {
			return;
		}

		final List<ChallengeType> challenges = getByActionType(type);
		final var challengeData = user.getChallengeData();

		// Build reusable player context
		final Context<Player> playerCtx = Context.player(player);

		for (ChallengeType challenge : challenges) {
			final ChallengeRequirement requirement = challenge.getRequiredData();

			final boolean matches;
			// If it's an AbstractItemRequirement, resolve with player context
		    if (requirement instanceof FishRequirement fishReq) {
		        matches = fishReq.matchesWithContext(context, playerCtx);
		    } else if (requirement instanceof AbstractItemRequirement itemReq) {
		        matches = itemReq.matchesWithContext(context, playerCtx);
		    } else {
		        matches = requirement.matches(context);
		    }

			if (!matches) {
				continue;
			}

			if (!challengeData.isChallengeActive(challenge) && !challengeData.isChallengeCompleted(challenge)) {
				challengeData.beginChallengeProgression(player, challenge);
			} else {
				challengeData.updateChallengeProgression(player, challenge, progressionAmount);

				if (challengeData.isChallengeCompleted(challenge)) {
					challengeData.completeChallenge(player, challenge);
				}
			}
		}
	}
}