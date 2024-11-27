package com.swiftlicious.hellblock.challenges;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.api.compatibility.VaultHook;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.ActionManagerInterface;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.PlayerUtils;
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
		Section globalChallengeSection = rewardConfig.getSection("general");
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

	public void performChallengeRewardAction(@NotNull Player player, @NotNull Section section,
			@NotNull ChallengeType challenge) {
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;
		RewardType rewardType = RewardType.valueOf(section.getString("reward-type", "ITEM").toUpperCase());
		if (rewardType == RewardType.ITEM) {
			Material material = Material.getMaterial(section.getString("material", "AIR").toUpperCase());
			if (material == null || material == Material.AIR)
				return;
			int amount = section.getInt("amount", 1);
			Item<ItemStack> reward = instance.getItemManager().wrap(new ItemStack(material, amount));
			reward.displayName(AdventureHelper.miniMessageToJson(section.getString("name")));
			List<String> lore = new ArrayList<>();
			for (String newLore : section.getStringList("lore")) {
				lore.add(AdventureHelper.miniMessageToJson(newLore));
			}
			reward.lore(lore);
			for (Entry<String, Object> enchants : section.getSection("enchantments").getStringRouteMappedValues(false)
					.entrySet()) {
				if (!StringUtils.isNumeric(enchants.getKey()))
					continue;
				if (enchants.getValue() instanceof Section enchantInner) {
					String enchant = enchantInner.getString("enchant");
					int level = enchantInner.getInt("level");
					reward.addEnchantment(com.swiftlicious.hellblock.utils.extras.Key.fromString(enchant), level);
				}
			}
			reward.damage(section.getInt("damage", (int) material.getMaxDurability()));
			ItemStack data = setChallengeRewardData(reward.getItem(), true);
			if (player.getInventory().firstEmpty() != -1) {
				PlayerUtils.putItemsToInventory(player.getInventory(), data, amount);
				player.updateInventory();
				onlineUser.get().getChallengeData().setChallengeRewardAsClaimed(challenge, true);
				ActionManagerInterface.trigger(Context.player(player), this.challengeGlobalActions);
			} else {
				PlayerUtils.dropItem(player, data, false, true, false);
			}
		} else if (rewardType == RewardType.EXP) {
			int exp = 0;
			try {
				exp = Integer.parseInt(section.getString("exp"));
			} catch (NumberFormatException ex) {
				instance.getPluginLogger().warn("The given input isn't a valid number for giving experience.", ex);
				return;
			}
			player.giveExp(exp);
			onlineUser.get().getChallengeData().setChallengeRewardAsClaimed(challenge, true);
			ActionManagerInterface.trigger(Context.player(player), this.challengeGlobalActions);
		} else if (rewardType == RewardType.MONEY) {
			if (!VaultHook.isHooked()) {
				instance.getPluginLogger().warn("Vault economy not found.");
				return;
			}
			double money = 0.0D;
			try {
				money = Double.parseDouble(section.getString("money"));
			} catch (NumberFormatException ex) {
				instance.getPluginLogger().warn("The given input isn't a valid number for giving money.", ex);
				return;
			}
			VaultHook.deposit(player, money);
			onlineUser.get().getChallengeData().setChallengeRewardAsClaimed(challenge, true);
			ActionManagerInterface.trigger(Context.player(player), this.challengeGlobalActions);
		} else {
			return;
		}
	}

	public boolean checkChallengeRewardData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellblockChallenge", "isChallengeReward");
	}

	public boolean getChallengeRewardData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).getOptional("HellblockChallenge", "isChallengeReward").asBoolean();
	}

	public @Nullable ItemStack setChallengeRewardData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellblockChallenge", "isChallengeReward");
		});
	}

	public enum RewardType {
		ITEM, MONEY, EXP;
	}
}
