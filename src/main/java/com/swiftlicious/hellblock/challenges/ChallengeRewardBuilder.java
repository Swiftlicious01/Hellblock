package com.swiftlicious.hellblock.challenges;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.NonNull;
import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

public class ChallengeRewardBuilder {

	private final HellblockPlugin instance;

	private final Registry<Enchantment> enchantmentRegistry;

	public ChallengeRewardBuilder(HellblockPlugin plugin) {
		instance = plugin;
		this.enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
	}

	public @Nullable ItemStack createChallengeReward(@NonNull ConfigurationSection section) {
		Material material = Material.getMaterial(section.getString("material").toUpperCase());
		if (material == null)
			return null;
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
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			reward.addEnchantment(enchantment, level, false);
		}
		reward.setDamage(section.getInt("damage", material.getMaxDurability()));
		ItemStack data = setChallengeRewardData(reward.get(), true);
		return data;
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
}
