package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

public class NetherArmor implements Listener {

	protected final HellblockPlugin instance;

	private final NamespacedKey netherrackHelmetKey, netherrackChestplateKey, netherrackLeggingsKey, netherrackBootsKey,
			glowstoneHelmetKey, glowstoneChestplateKey, glowstoneLeggingsKey, glowstoneBootsKey, quartzHelmetKey,
			quartzChestplateKey, quartzLeggingsKey, quartzBootsKey, netherstarHelmetKey, netherstarChestplateKey,
			netherstarLeggingsKey, netherstarBootsKey;

	private final Set<Material> netherrackArmor, glowstoneArmor, quartzArmor, netherstarArmor;

	public NetherArmor(HellblockPlugin plugin) {
		instance = plugin;
		this.netherrackHelmetKey = new NamespacedKey(instance, "netherrackhelmet");
		this.netherrackChestplateKey = new NamespacedKey(instance, "netherrackchestplate");
		this.netherrackLeggingsKey = new NamespacedKey(instance, "netherrackleggings");
		this.netherrackBootsKey = new NamespacedKey(instance, "netherrackboots");
		this.glowstoneHelmetKey = new NamespacedKey(instance, "glowstonehelmet");
		this.glowstoneChestplateKey = new NamespacedKey(instance, "glowstonechestplate");
		this.glowstoneLeggingsKey = new NamespacedKey(instance, "glowstoneleggings");
		this.glowstoneBootsKey = new NamespacedKey(instance, "glowstoneboots");
		this.quartzHelmetKey = new NamespacedKey(instance, "quartzhelmet");
		this.quartzChestplateKey = new NamespacedKey(instance, "quartzchestplate");
		this.quartzLeggingsKey = new NamespacedKey(instance, "quartzleggings");
		this.quartzBootsKey = new NamespacedKey(instance, "quartzboots");
		this.netherstarHelmetKey = new NamespacedKey(instance, "netherstarhelmet");
		this.netherstarChestplateKey = new NamespacedKey(instance, "netherstarchestplate");
		this.netherstarLeggingsKey = new NamespacedKey(instance, "netherstarleggings");
		this.netherstarBootsKey = new NamespacedKey(instance, "netherstarboots");
		this.netherrackArmor = Set.of(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS,
				Material.LEATHER_BOOTS);
		this.glowstoneArmor = Set.of(Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS,
				Material.GOLDEN_BOOTS);
		this.quartzArmor = Set.of(Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS,
				Material.IRON_BOOTS);
		this.netherstarArmor = Set.of(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS,
				Material.DIAMOND_BOOTS);
		addArmor();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public void addArmor() {
		try {
			if (HBConfig.nrArmor) {
				Bukkit.removeRecipe(this.netherrackHelmetKey);
				Bukkit.removeRecipe(this.netherrackChestplateKey);
				Bukkit.removeRecipe(this.netherrackLeggingsKey);
				Bukkit.removeRecipe(this.netherrackBootsKey);
				Bukkit.addRecipe(nrHelmet());
				Bukkit.addRecipe(nrChestplate());
				Bukkit.addRecipe(nrLeggings());
				Bukkit.addRecipe(nrBoots());
			} else {
				Bukkit.removeRecipe(this.netherrackHelmetKey);
				Bukkit.removeRecipe(this.netherrackChestplateKey);
				Bukkit.removeRecipe(this.netherrackLeggingsKey);
				Bukkit.removeRecipe(this.netherrackBootsKey);
			}

			if (HBConfig.gsArmor) {
				Bukkit.removeRecipe(this.glowstoneHelmetKey);
				Bukkit.removeRecipe(this.glowstoneChestplateKey);
				Bukkit.removeRecipe(this.glowstoneLeggingsKey);
				Bukkit.removeRecipe(this.glowstoneBootsKey);
				Bukkit.addRecipe(gsHelmet());
				Bukkit.addRecipe(gsChestplate());
				Bukkit.addRecipe(gsLeggings());
				Bukkit.addRecipe(gsBoots());
			} else {
				Bukkit.removeRecipe(this.glowstoneHelmetKey);
				Bukkit.removeRecipe(this.glowstoneChestplateKey);
				Bukkit.removeRecipe(this.glowstoneLeggingsKey);
				Bukkit.removeRecipe(this.glowstoneBootsKey);
			}

			if (HBConfig.qzArmor) {
				Bukkit.removeRecipe(this.quartzHelmetKey);
				Bukkit.removeRecipe(this.quartzChestplateKey);
				Bukkit.removeRecipe(this.quartzLeggingsKey);
				Bukkit.removeRecipe(this.quartzBootsKey);
				Bukkit.addRecipe(qzHelmet());
				Bukkit.addRecipe(qzChestplate());
				Bukkit.addRecipe(qzLeggings());
				Bukkit.addRecipe(qzBoots());
			} else {
				Bukkit.removeRecipe(this.quartzHelmetKey);
				Bukkit.removeRecipe(this.quartzChestplateKey);
				Bukkit.removeRecipe(this.quartzLeggingsKey);
				Bukkit.removeRecipe(this.quartzBootsKey);
			}

			if (HBConfig.nsArmor) {
				Bukkit.removeRecipe(this.netherstarHelmetKey);
				Bukkit.removeRecipe(this.netherstarChestplateKey);
				Bukkit.removeRecipe(this.netherstarLeggingsKey);
				Bukkit.removeRecipe(this.netherstarBootsKey);
				Bukkit.addRecipe(nsHelmet());
				Bukkit.addRecipe(nsChestplate());
				Bukkit.addRecipe(nsLeggings());
				Bukkit.addRecipe(nsBoots());
			} else {
				Bukkit.removeRecipe(this.netherstarHelmetKey);
				Bukkit.removeRecipe(this.netherstarChestplateKey);
				Bukkit.removeRecipe(this.netherstarLeggingsKey);
				Bukkit.removeRecipe(this.netherstarBootsKey);
			}
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	public ShapedRecipe nrHelmet() {
		ItemBuilder armor = new ItemBuilder(Material.LEATHER_HELMET, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrHelmetName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrHelmetLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.nrHelmetEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.nrArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackHelmetKey, data);
		recipe.shape(new String[] { "NNN", "N N", "   " });
		recipe.setIngredient('N', Material.NETHERRACK);
		return recipe;
	}

	public ShapedRecipe nrChestplate() {
		ItemBuilder armor = new ItemBuilder(Material.LEATHER_CHESTPLATE, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrChestplateName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrChestplateLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.nrChestplateEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.nrArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackChestplateKey, data);
		recipe.shape(new String[] { "N N", "NNN", "NNN" });
		recipe.setIngredient('N', Material.NETHERRACK);
		return recipe;
	}

	public ShapedRecipe nrLeggings() {
		ItemBuilder armor = new ItemBuilder(Material.LEATHER_LEGGINGS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrLeggingsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrLeggingsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.nrLeggingsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.nrArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackLeggingsKey, data);
		recipe.shape(new String[] { "NNN", "N N", "N N" });
		recipe.setIngredient('N', Material.NETHERRACK);
		return recipe;
	}

	public ShapedRecipe nrBoots() {
		ItemBuilder armor = new ItemBuilder(Material.LEATHER_BOOTS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrBootsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrBootsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.nrBootsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.nrArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackBootsKey, data);
		recipe.shape(new String[] { "   ", "N N", "N N" });
		recipe.setIngredient('N', Material.NETHERRACK);
		return recipe;
	}

	public ShapedRecipe gsHelmet() {
		ItemBuilder armor = new ItemBuilder(Material.GOLDEN_HELMET, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsHelmetName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsHelmetLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.gsHelmetEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.gsArmor);
		data = setNightVisionArmorStatus(data, HBConfig.gsNightVisionArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstoneHelmetKey, data);
		recipe.shape(new String[] { "NNN", "N N", "   " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		return recipe;
	}

	public ShapedRecipe gsChestplate() {
		ItemBuilder armor = new ItemBuilder(Material.GOLDEN_CHESTPLATE, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsChestplateName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsChestplateLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.gsChestplateEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.gsArmor);
		data = setNightVisionArmorStatus(data, HBConfig.gsNightVisionArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstoneChestplateKey, data);
		recipe.shape(new String[] { "N N", "NNN", "NNN" });
		recipe.setIngredient('N', Material.GLOWSTONE);
		return recipe;
	}

	public ShapedRecipe gsLeggings() {
		ItemBuilder armor = new ItemBuilder(Material.GOLDEN_LEGGINGS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsLeggingsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsLeggingsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.gsLeggingsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.gsArmor);
		data = setNightVisionArmorStatus(data, HBConfig.gsNightVisionArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstoneLeggingsKey, data);
		recipe.shape(new String[] { "NNN", "N N", "N N" });
		recipe.setIngredient('N', Material.GLOWSTONE);
		return recipe;
	}

	public ShapedRecipe gsBoots() {
		ItemBuilder armor = new ItemBuilder(Material.GOLDEN_BOOTS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsBootsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsBootsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.gsBootsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.gsArmor);
		data = setNightVisionArmorStatus(data, HBConfig.gsNightVisionArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstoneBootsKey, data);
		recipe.shape(new String[] { "   ", "N N", "N N" });
		recipe.setIngredient('N', Material.GLOWSTONE);
		return recipe;
	}

	public ShapedRecipe qzHelmet() {
		ItemBuilder armor = new ItemBuilder(Material.IRON_HELMET, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzHelmetName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzHelmetLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.qzHelmetEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.qzArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzHelmetKey, data);
		recipe.shape(new String[] { "NNN", "N N", "   " });
		recipe.setIngredient('N', Material.QUARTZ);
		return recipe;
	}

	public ShapedRecipe qzChestplate() {
		ItemBuilder armor = new ItemBuilder(Material.IRON_CHESTPLATE, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzChestplateName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzChestplateLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.qzChestplateEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.qzArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzChestplateKey, data);
		recipe.shape(new String[] { "N N", "NNN", "NNN" });
		recipe.setIngredient('N', Material.QUARTZ);
		return recipe;
	}

	public ShapedRecipe qzLeggings() {
		ItemBuilder armor = new ItemBuilder(Material.IRON_LEGGINGS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzLeggingsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzLeggingsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.qzLeggingsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.qzArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzLeggingsKey, data);
		recipe.shape(new String[] { "NNN", "N N", "N N" });
		recipe.setIngredient('N', Material.QUARTZ);
		return recipe;
	}

	public ShapedRecipe qzBoots() {
		ItemBuilder armor = new ItemBuilder(Material.IRON_BOOTS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzBootsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzBootsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.qzBootsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.qzArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzBootsKey, data);
		recipe.shape(new String[] { "   ", "N N", "N N" });
		recipe.setIngredient('N', Material.QUARTZ);
		return recipe;
	}

	public ShapedRecipe nsHelmet() {
		ItemBuilder armor = new ItemBuilder(Material.DIAMOND_HELMET, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsHelmetName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsHelmetLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.nsHelmetEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.nsArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarHelmetKey, data);
		recipe.shape(new String[] { "NNN", "N N", "   " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		return recipe;
	}

	public ShapedRecipe nsChestplate() {
		ItemBuilder armor = new ItemBuilder(Material.DIAMOND_CHESTPLATE, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsChestplateName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsChestplateLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.nsChestplateEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.nsArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarChestplateKey, data);
		recipe.shape(new String[] { "N N", "NNN", "NNN" });
		recipe.setIngredient('N', Material.NETHER_STAR);
		return recipe;
	}

	public ShapedRecipe nsLeggings() {
		ItemBuilder armor = new ItemBuilder(Material.DIAMOND_LEGGINGS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsLeggingsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsLeggingsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.nsLeggingsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.nsArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarLeggingsKey, data);
		recipe.shape(new String[] { "NNN", "N N", "N N" });
		recipe.setIngredient('N', Material.NETHER_STAR);
		return recipe;
	}

	public ShapedRecipe nsBoots() {
		ItemBuilder armor = new ItemBuilder(Material.DIAMOND_BOOTS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsBootsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsBootsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : HBConfig.nsBootsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
					.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			armor.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setArmorData(armor.get(), HBConfig.nsArmor);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarBootsKey, data);
		recipe.shape(new String[] { "   ", "N N", "N N" });
		recipe.setIngredient('N', Material.NETHER_STAR);
		return recipe;
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (event.getView().getPlayer() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
				return;
			}

			Recipe recipe = event.getRecipe();
			ItemStack result = recipe.getResult();
			if (isNetherArmorEnabled(result)) {
				if (checkArmorData(result) && getArmorData(result)) {
					if (recipe instanceof CraftingRecipe craft) {
						if (!player.hasDiscoveredRecipe(craft.getKey())) {
							Optional<UserData> onlineUser = instance.getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null)
								return;
							if (!onlineUser.get().getChallengeData()
									.isChallengeActive(ChallengeType.NETHER_CRAFTING_CHALLENGE)
									&& !onlineUser.get().getChallengeData()
											.isChallengeCompleted(ChallengeType.NETHER_CRAFTING_CHALLENGE)) {
								onlineUser.get().getChallengeData().beginChallengeProgression(
										onlineUser.get().getPlayer(), ChallengeType.NETHER_CRAFTING_CHALLENGE);
							} else {
								onlineUser.get().getChallengeData().updateChallengeProgression(
										onlineUser.get().getPlayer(), ChallengeType.NETHER_CRAFTING_CHALLENGE, 1);
								if (onlineUser.get().getChallengeData()
										.isChallengeCompleted(ChallengeType.NETHER_CRAFTING_CHALLENGE)) {
									onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
											ChallengeType.NETHER_CRAFTING_CHALLENGE);
								}
							}
							player.discoverRecipe(craft.getKey());
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onLimitedCrafting(PrepareItemCraftEvent event) {
		if (instance.getHellblockHandler().getHellblockWorld().getGameRuleValue(GameRule.DO_LIMITED_CRAFTING)) {
			if (event.getView().getPlayer() instanceof Player player) {
				if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
					return;
				}

				Recipe recipe = event.getRecipe();
				ItemStack result = recipe.getResult();
				if (isNetherArmorEnabled(result)) {
					if (checkArmorData(result) && getArmorData(result)) {
						if (recipe instanceof CraftingRecipe craft) {
							if (!player.hasDiscoveredRecipe(craft.getKey())) {
								Optional<UserData> onlineUser = instance.getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null)
									return;
								if (!onlineUser.get().getChallengeData()
										.isChallengeActive(ChallengeType.NETHER_CRAFTING_CHALLENGE)
										&& !onlineUser.get().getChallengeData()
												.isChallengeCompleted(ChallengeType.NETHER_CRAFTING_CHALLENGE)) {
									onlineUser.get().getChallengeData().beginChallengeProgression(
											onlineUser.get().getPlayer(), ChallengeType.NETHER_CRAFTING_CHALLENGE);
								} else {
									onlineUser.get().getChallengeData().updateChallengeProgression(
											onlineUser.get().getPlayer(), ChallengeType.NETHER_CRAFTING_CHALLENGE, 1);
									if (onlineUser.get().getChallengeData()
											.isChallengeCompleted(ChallengeType.NETHER_CRAFTING_CHALLENGE)) {
										onlineUser.get().getChallengeData().completeChallenge(
												onlineUser.get().getPlayer(), ChallengeType.NETHER_CRAFTING_CHALLENGE);
									}
								}
								player.discoverRecipe(craft.getKey());
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onArmorChange(PlayerArmorChangeEvent event) {
		if (!HBConfig.gsNightVisionArmor || !HBConfig.gsArmor)
			return;

		Player player = event.getPlayer();

		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;

		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;

		ItemStack armor = event.getNewItem();
		if (armor != null && armor.getType() != Material.AIR) {
			if (checkNightVisionArmorStatus(armor) && getNightVisionArmorStatus(armor)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				onlineUser.get().isWearingGlowstoneArmor(true);
			} else {
				ItemStack[] armorSet = player.getInventory().getArmorContents();
				boolean checkArmor = false;
				if (armorSet != null) {
					for (ItemStack item : armorSet) {
						if (item == null || item.getType() == Material.AIR)
							continue;
						if (checkNightVisionArmorStatus(item) && getNightVisionArmorStatus(item)) {
							checkArmor = true;
							break;
						}
					}
				}
				if (!checkArmor && onlineUser.get().hasGlowstoneArmorEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						onlineUser.get().isWearingGlowstoneArmor(false);
						if (!onlineUser.get().hasGlowstoneToolEffect()) {
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						}
					}
				}
			}
		} else {
			ItemStack[] armorSet = player.getInventory().getArmorContents();
			boolean checkArmor = false;
			if (armorSet != null) {
				for (ItemStack item : armorSet) {
					if (item == null || item.getType() == Material.AIR)
						continue;
					if (checkNightVisionArmorStatus(item) && getNightVisionArmorStatus(item)) {
						checkArmor = true;
						break;
					}
				}
			}
			if (!checkArmor && onlineUser.get().hasGlowstoneArmorEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					onlineUser.get().isWearingGlowstoneArmor(false);
					if (!onlineUser.get().hasGlowstoneToolEffect()) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					}
				}
			}
		}
	}

	@EventHandler
	public void onDispenseArmor(BlockDispenseArmorEvent event) {
		if (!HBConfig.gsNightVisionArmor || !HBConfig.gsArmor)
			return;
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;

		if (event.getTargetEntity() instanceof Player player) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			ItemStack armor = event.getItem();
			if (checkNightVisionArmorStatus(armor) && getNightVisionArmorStatus(armor)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				onlineUser.get().isWearingGlowstoneArmor(true);
			} else {
				ItemStack[] armorSet = player.getInventory().getArmorContents();
				boolean checkArmor = false;
				if (armorSet != null) {
					for (ItemStack item : armorSet) {
						if (item == null || item.getType() == Material.AIR)
							continue;
						if (checkNightVisionArmorStatus(item) && getNightVisionArmorStatus(item)) {
							checkArmor = true;
							break;
						}
					}
				}
				if (!checkArmor && onlineUser.get().hasGlowstoneArmorEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						onlineUser.get().isWearingGlowstoneArmor(false);
						if (!onlineUser.get().hasGlowstoneToolEffect()) {
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						}
					}
				}
			}
		}
	}

	public boolean checkArmorData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellblockRecipe", "isNetherArmor");
	}

	public boolean getArmorData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).getOptional("HellblockRecipe", "isNetherArmor").asBoolean();
	}

	public @Nullable ItemStack setArmorData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellblockRecipe", "isNetherArmor");
		});
	}

	public boolean checkNightVisionArmorStatus(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellblockRecipe", "wearsNightVision");
	}

	public boolean getNightVisionArmorStatus(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).getOptional("HellblockRecipe", "wearsNightVision").asBoolean();
	}

	public @Nullable ItemStack setNightVisionArmorStatus(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellblockRecipe", "wearsNightVision");
		});
	}

	public boolean isNetherArmorEnabled(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		Material mat = item.getType();
		return ((HBConfig.nrArmor && this.netherrackArmor.contains(mat))
				|| (HBConfig.gsArmor && this.glowstoneArmor.contains(mat))
				|| (HBConfig.qzArmor && this.quartzArmor.contains(mat))
				|| (HBConfig.nsArmor && this.netherstarArmor.contains(mat)));
	}
}
