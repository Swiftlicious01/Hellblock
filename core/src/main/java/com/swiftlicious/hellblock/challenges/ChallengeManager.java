package com.swiftlicious.hellblock.challenges;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.ActionManagerInterface;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

import net.kyori.adventure.audience.Audience;

public class ChallengeManager implements Reloadable {

	protected final HellblockPlugin instance;

	protected YamlDocument challengeConfig;

	protected List<ChallengeType> challenges;

	protected Action<Player>[] challengeGlobalActions;

	public ChallengeManager(HellblockPlugin plugin) {
		instance = plugin;
		challenges = new ArrayList<>();
	}

	@Override
	public void load() {
		File challengeFile = new File(instance.getDataFolder(), "challenges.yml");
		if (!challengeFile.exists())
			instance.saveResource("challenges.yml", false);
		this.challengeConfig = instance.getConfigManager().loadData(challengeFile);
		Section globalChallengeSection = challengeConfig.getSection("global");
		if (globalChallengeSection != null) {
			this.challengeGlobalActions = instance.getActionManager()
					.parseActions(globalChallengeSection.getSection("action"));
		}
		this.challenges.clear();
		registerChallengesFromConfig();
	}

	public YamlDocument getChallengeConfig() {
		return this.challengeConfig;
	}

	private void registerChallengesFromConfig() {
		Section challengeSection = getChallengeConfig().getSection("challenges");
		if (challengeSection != null) {
			for (Map.Entry<String, Object> entry : challengeSection.getStringRouteMappedValues(false).entrySet()) {
				String id = entry.getKey();
				if (entry.getValue() instanceof Section inner) {
					int completionAmount = inner.getInt("amount");
					ActionType action = ActionType.valueOf(Objects.requireNonNull(inner.getString("action")));
					ChallengeType challenge = new ChallengeType(id, completionAmount, action);
					this.challenges.add(challenge);
				}
			}
		}
	}

	public @Nullable ChallengeType getById(@NotNull String id) {
		return this.challenges.stream().filter(ch -> ch.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
	}

	public @Nullable ChallengeType getByActionType(@NotNull ActionType action) {
		return this.challenges.stream().filter(ch -> ch.getAction().equals(action)).findFirst().orElse(null);
	}

	public void performChallengeCompletionActions(@NotNull Player player, @NotNull ChallengeType challenge) {
		Audience audience = instance.getSenderFactory().getAudience(player);
		AdventureHelper.sendCenteredMessage(audience,
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_CHALLENGE_COMPLETED.build()));
		if (instance.getConfigManager().challengeCompleteSound() != null)
			audience.playSound(instance.getConfigManager().challengeCompleteSound());
		if (player.getLocation() == null)
			return;
		FakeFirework firework = instance.getVersionManager().getNMSManager().createFakeFirework(player.getLocation());
		firework.flightTime(0);
		firework.invisible(true);
		firework.spawn(player);
	}

	public void performChallengeRewardActions(@NotNull Player player, @NotNull ChallengeType challenge) {
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;

		Context<Player> context = Context.player(player);
		Section challengeSection = getChallengeConfig().getSection("rewards." + challenge.toString().toUpperCase());
		if (challengeSection != null) {
			ActionManagerInterface.trigger(context,
					instance.getActionManager().parseActions(challengeSection.getSection("action")));
		}
		onlineUser.get().getChallengeData().setChallengeRewardAsClaimed(challenge, true);
		if (this.challengeGlobalActions != null)
			ActionManagerInterface.trigger(context, this.challengeGlobalActions);
	}
}