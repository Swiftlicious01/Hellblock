package com.swiftlicious.hellblock.challenges;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.VaultHook;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.gui.hellblock.ChallengesMenu;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.utils.FireworkUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

public class ChallengeRewardBuilder {

	private final HellblockPlugin instance;

	@Getter
	private final YamlConfiguration rewardConfig;

	private final Registry<Enchantment> enchantmentRegistry;

	public ChallengeRewardBuilder(HellblockPlugin plugin) {
		instance = plugin;
		this.rewardConfig = instance.getConfig("challenge-rewards.yml");
		this.enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
	}

	public void performChallengeCompletionActions(@NonNull Player player, @NonNull ChallengeType challenge) {
		if (player.getLocation() == null)
			return;
		instance.getAdventureManager().sendCenteredMessage(player,
				"<dark_gray>[+] <gray><strikethrough>--------------------------------------------<reset> <dark_gray>[+]");
		instance.getAdventureManager().sendCenteredMessage(player, " ");
		instance.getAdventureManager().sendCenteredMessage(player,
				"<dark_green>*** <green><bold>Challenge Completed!<reset> <dark_green>***");
		instance.getAdventureManager().sendCenteredMessage(player,
				String.format("<gold>Claim your reward by clicking this challenge in the GUI menu!"));
		instance.getAdventureManager().sendCenteredMessage(player, " ");
		instance.getAdventureManager().sendCenteredMessage(player,
				"<dark_gray>[+] <gray><strikethrough>--------------------------------------------<reset> <dark_gray>[+]");
		instance.getAdventureManager().sendSound(player, Sound.Source.PLAYER,
				Key.key("minecraft:entity.player.levelup"), 1.0F, 1.0F);
		int fireworkID = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
		ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
		FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
		meta.setPower(2);
		meta.addEffect(FireworkEffect.builder().with(Type.BURST).trail(false).flicker(false).withColor(Color.LIME)
				.withFade(Color.LIME).build());
		meta.getPersistentDataContainer().set(new NamespacedKey(instance, "challenge-firework"),
				PersistentDataType.BOOLEAN, true);
		firework.setItemMeta(meta);
		instance.sendPackets(player, FireworkUtils.getSpawnFireworkPacket(fireworkID, player.getLocation()),
				FireworkUtils.getFireworkMetaPacket(fireworkID, firework),
				FireworkUtils.getFireworkStatusPacket(fireworkID), FireworkUtils.getFireworkDestroyPacket(fireworkID));
	}

	public void performChallengeRewardAction(@NonNull Player player, @NonNull ConfigurationSection section,
			@NonNull ChallengeType challenge) {
		OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser == null)
			return;
		RewardType rewardType = RewardType.valueOf(section.getString("reward-type", "ITEM").toUpperCase());
		if (rewardType == RewardType.ITEM) {
			Material material = Material.getMaterial(section.getString("material", "AIR").toUpperCase());
			if (material == null || material == Material.AIR)
				return;
			int amount = section.getInt("amount", 1);
			ItemBuilder reward = new ItemBuilder(material, amount);
			reward.setDisplayName(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(section.getString("name"))));
			List<ComponentWrapper> lore = new ArrayList<>();
			for (String newLore : section.getStringList("lore")) {
				lore.add(new ShadedAdventureComponentWrapper(
						instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
			}
			reward.setLore(lore);
			for (String enchants : section.getStringList("enchantments")) {
				String[] split = enchants.split(":");
				Enchantment enchantment = enchantmentRegistry
						.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
				int level = 1;
				try {
					level = Integer.parseInt(split[1]);
				} catch (NumberFormatException ex) {
					LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
					continue;
				}
				reward.addEnchantment(enchantment, level, false);
			}
			reward.setDamage(section.getInt("damage", material.getMaxDurability()));
			ItemStack data = setChallengeRewardData(reward.get(), true);
			if (player.getInventory().firstEmpty() != -1) {
				player.getInventory().addItem(data);
				player.updateInventory();
				onlineUser.getChallengeData().setChallengeRewardAsClaimed(challenge, true);
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You've claimed your challenge reward!");
				instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
				new ChallengesMenu(player);
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>Please have an empty slot in your inventory to receive the reward!");
			}
		} else if (rewardType == RewardType.EXP) {
			int exp = 0;
			try {
				exp = Integer.parseInt(section.getString("exp"));
			} catch (NumberFormatException ex) {
				LogUtils.warn("The given input isn't a valid number for giving experience.", ex);
				return;
			}
			player.giveExp(exp);
			onlineUser.getChallengeData().setChallengeRewardAsClaimed(challenge, true);
			instance.getAdventureManager().sendMessageWithPrefix(player, "<red>You've claimed your challenge reward!");
			instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
			new ChallengesMenu(player);
		} else if (rewardType == RewardType.MONEY) {
			if (VaultHook.getEconomy() == null) {
				LogUtils.warn("Vault economy not found.");
				return;
			}
			double money = 0.0D;
			try {
				money = Double.parseDouble(section.getString("money"));
			} catch (NumberFormatException ex) {
				LogUtils.warn("The given input isn't a valid number for giving money.", ex);
				return;
			}
			VaultHook.getEconomy().depositPlayer(player, money);
			onlineUser.getChallengeData().setChallengeRewardAsClaimed(challenge, true);
			instance.getAdventureManager().sendMessageWithPrefix(player, "<red>You've claimed your challenge reward!");
			instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
			new ChallengesMenu(player);
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
