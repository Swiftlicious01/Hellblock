package com.swiftlicious.hellblock.challenges;

import java.io.File;
import java.util.Optional;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
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

public class ChallengeRewardBuilder implements Reloadable {

	protected final HellblockPlugin instance;

	protected YamlDocument rewardConfig;

	protected Action<Player>[] challengeGlobalActions;

	public ChallengeRewardBuilder(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		File challengeRewardFile = new File(instance.getDataFolder(), "challenge-rewards.yml");
		if (!challengeRewardFile.exists())
			instance.saveResource("challenge-rewards.yml", false);
		this.rewardConfig = instance.getConfigManager().loadData(challengeRewardFile);
		Section globalChallengeSection = rewardConfig.getSection("global");
		if (globalChallengeSection != null) {
			this.challengeGlobalActions = instance.getActionManager()
					.parseActions(globalChallengeSection.getSection("action"));
		}
	}

	public YamlDocument getRewardConfig() {
		return this.rewardConfig;
	}

	public void performChallengeCompletionActions(@NotNull Player player, @NotNull ChallengeType challenge) {
		if (player.getLocation() == null)
			return;
		Audience audience = instance.getSenderFactory().getAudience(player);
		AdventureHelper.sendCenteredMessage(audience,
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_CHALLENGE_COMPLETED.build()));
		if (instance.getConfigManager().challengeCompleteSound() != null)
			audience.playSound(instance.getConfigManager().challengeCompleteSound());
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
		Section challengeSection = getRewardConfig().getSection("rewards." + challenge.toString().toUpperCase());
		if (challengeSection != null) {
			ActionManagerInterface.trigger(context,
					instance.getActionManager().parseActions(challengeSection.getSection("action")));
		}
		onlineUser.get().getChallengeData().setChallengeRewardAsClaimed(challenge, true);
		if (this.challengeGlobalActions != null)
			ActionManagerInterface.trigger(context, this.challengeGlobalActions);
	}
}