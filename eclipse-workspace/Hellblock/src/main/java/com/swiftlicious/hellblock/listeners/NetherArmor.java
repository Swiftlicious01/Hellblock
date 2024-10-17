package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.jeff_media.armorequipevent.ArmorEquipEvent;
import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

public class NetherArmor implements Listener {

	private final HellblockPlugin instance;

	private final NamespacedKey netherrackHelmetKey, netherrackChestplateKey, netherrackLeggingsKey, netherrackBootsKey,
			glowstoneHelmetKey, glowstoneChestplateKey, glowstoneLeggingsKey, glowstoneBootsKey, quartzHelmetKey,
			quartzChestplateKey, quartzLeggingsKey, quartzBootsKey, netherstarHelmetKey, netherstarChestplateKey,
			netherstarLeggingsKey, netherstarBootsKey;

	public boolean gsNightVision;
	public boolean nrArmor;
	public String nrHelmetName;
	public List<String> nrHelmetLore;
	public List<String> nrHelmetEnchants;
	public String nrChestplateName;
	public List<String> nrChestplateLore;
	public List<String> nrChestplateEnchants;
	public String nrLeggingsName;
	public List<String> nrLeggingsLore;
	public List<String> nrLeggingsEnchants;
	public String nrBootsName;
	public List<String> nrBootsLore;
	public List<String> nrBootsEnchants;
	public boolean gsArmor;
	public String gsHelmetName;
	public List<String> gsHelmetLore;
	public List<String> gsHelmetEnchants;
	public String gsChestplateName;
	public List<String> gsChestplateLore;
	public List<String> gsChestplateEnchants;
	public String gsLeggingsName;
	public List<String> gsLeggingsLore;
	public List<String> gsLeggingsEnchants;
	public String gsBootsName;
	public List<String> gsBootsLore;
	public List<String> gsBootsEnchants;
	public boolean qzArmor;
	public String qzHelmetName;
	public List<String> qzHelmetLore;
	public List<String> qzHelmetEnchants;
	public String qzChestplateName;
	public List<String> qzChestplateLore;
	public List<String> qzChestplateEnchants;
	public String qzLeggingsName;
	public List<String> qzLeggingsLore;
	public List<String> qzLeggingsEnchants;
	public String qzBootsName;
	public List<String> qzBootsLore;
	public List<String> qzBootsEnchants;
	public boolean nsArmor;
	public String nsHelmetName;
	public List<String> nsHelmetLore;
	public List<String> nsHelmetEnchants;
	public String nsChestplateName;
	public List<String> nsChestplateLore;
	public List<String> nsChestplateEnchants;
	public String nsLeggingsName;
	public List<String> nsLeggingsLore;
	public List<String> nsLeggingsEnchants;
	public String nsBootsName;
	public List<String> nsBootsLore;
	public List<String> nsBootsEnchants;

	private final Registry<Enchantment> enchantmentRegistry;

	public NetherArmor(HellblockPlugin plugin) {
		this.instance = plugin;
		this.enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
		this.netherrackHelmetKey = new NamespacedKey(this.instance, "netherrackhelmet");
		this.netherrackChestplateKey = new NamespacedKey(this.instance, "netherrackchestplate");
		this.netherrackLeggingsKey = new NamespacedKey(this.instance, "netherrackleggings");
		this.netherrackBootsKey = new NamespacedKey(this.instance, "netherrackboots");
		this.glowstoneHelmetKey = new NamespacedKey(this.instance, "glowstonehelmet");
		this.glowstoneChestplateKey = new NamespacedKey(this.instance, "glowstonechestplate");
		this.glowstoneLeggingsKey = new NamespacedKey(this.instance, "glowstoneleggings");
		this.glowstoneBootsKey = new NamespacedKey(this.instance, "glowstoneboots");
		this.quartzHelmetKey = new NamespacedKey(this.instance, "quartzhelmet");
		this.quartzChestplateKey = new NamespacedKey(this.instance, "quartzchestplate");
		this.quartzLeggingsKey = new NamespacedKey(this.instance, "quartzleggings");
		this.quartzBootsKey = new NamespacedKey(this.instance, "quartzboots");
		this.netherstarHelmetKey = new NamespacedKey(this.instance, "netherstarhelmet");
		this.netherstarChestplateKey = new NamespacedKey(this.instance, "netherstarchestplate");
		this.netherstarLeggingsKey = new NamespacedKey(this.instance, "netherstarleggings");
		this.netherstarBootsKey = new NamespacedKey(this.instance, "netherstarboots");
		this.nrArmor = instance.getConfig("config.yml").getBoolean("armor.netherrack.enable");
		this.nrHelmetName = instance.getConfig("config.yml").getString("armor.netherrack.helmet.name");
		this.nrHelmetLore = instance.getConfig("config.yml").getStringList("armor.netherrack.helmet.lore");
		this.nrHelmetEnchants = instance.getConfig("config.yml").getStringList("armor.netherrack.helmet.enchantments");
		this.nrChestplateName = instance.getConfig("config.yml").getString("armor.netherrack.chestplate.name");
		this.nrChestplateLore = instance.getConfig("config.yml").getStringList("armor.netherrack.chestplate.lore");
		this.nrChestplateEnchants = instance.getConfig("config.yml")
				.getStringList("armor.netherrack.chestplate.enchantments");
		this.nrLeggingsName = instance.getConfig("config.yml").getString("armor.netherrack.leggings.name");
		this.nrLeggingsLore = instance.getConfig("config.yml").getStringList("armor.netherrack.leggings.lore");
		this.nrLeggingsEnchants = instance.getConfig("config.yml")
				.getStringList("armor.netherrack.leggings.enchantments");
		this.nrBootsName = instance.getConfig("config.yml").getString("armor.netherrack.boots.name");
		this.nrBootsLore = instance.getConfig("config.yml").getStringList("armor.netherrack.boots.lore");
		this.nrBootsEnchants = instance.getConfig("config.yml").getStringList("armor.netherrack.boots.enchantments");
		this.gsArmor = instance.getConfig("config.yml").getBoolean("armor.glowstone.enable");
		this.gsNightVision = instance.getConfig("config.yml").getBoolean("armor.glowstone.night-vision");
		this.gsHelmetName = instance.getConfig("config.yml").getString("armor.glowstone.helmet.name");
		this.gsHelmetLore = instance.getConfig("config.yml").getStringList("armor.glowstone.helmet.lore");
		this.gsHelmetEnchants = instance.getConfig("config.yml").getStringList("armor.glowstone.helmet.enchantments");
		this.gsChestplateName = instance.getConfig("config.yml").getString("armor.glowstone.chestplate.name");
		this.gsChestplateLore = instance.getConfig("config.yml").getStringList("armor.glowstone.chestplate.lore");
		this.gsChestplateEnchants = instance.getConfig("config.yml")
				.getStringList("armor.glowstone.chestplate.enchantments");
		this.gsLeggingsName = instance.getConfig("config.yml").getString("armor.glowstone.leggings.name");
		this.gsLeggingsLore = instance.getConfig("config.yml").getStringList("armor.glowstone.leggings.lore");
		this.gsLeggingsEnchants = instance.getConfig("config.yml")
				.getStringList("armor.glowstone.leggings.enchantments");
		this.gsBootsName = instance.getConfig("config.yml").getString("armor.glowstone.boots.name");
		this.gsBootsLore = instance.getConfig("config.yml").getStringList("armor.glowstone.boots.lore");
		this.gsBootsEnchants = instance.getConfig("config.yml").getStringList("armor.glowstone.boots.enchantments");
		this.qzArmor = instance.getConfig("config.yml").getBoolean("armor.quartz.enable");
		this.qzHelmetName = instance.getConfig("config.yml").getString("armor.quartz.helmet.name");
		this.qzHelmetLore = instance.getConfig("config.yml").getStringList("armor.quartz.helmet.lore");
		this.qzHelmetEnchants = instance.getConfig("config.yml").getStringList("armor.quartz.helmet.enchantments");
		this.qzChestplateName = instance.getConfig("config.yml").getString("armor.quartz.chestplate.name");
		this.qzChestplateLore = instance.getConfig("config.yml").getStringList("armor.quartz.chestplate.lore");
		this.qzChestplateEnchants = instance.getConfig("config.yml")
				.getStringList("armor.quartz.chestplate.enchantments");
		this.qzLeggingsName = instance.getConfig("config.yml").getString("armor.quartz.leggings.name");
		this.qzLeggingsLore = instance.getConfig("config.yml").getStringList("armor.quartz.leggings.lore");
		this.qzLeggingsEnchants = instance.getConfig("config.yml").getStringList("armor.quartz.leggings.enchantments");
		this.qzBootsName = instance.getConfig("config.yml").getString("armor.quartz.boots.name");
		this.qzBootsLore = instance.getConfig("config.yml").getStringList("armor.quartz.boots.lore");
		this.qzBootsEnchants = instance.getConfig("config.yml").getStringList("armor.quartz.boots.enchantments");
		this.nsArmor = instance.getConfig("config.yml").getBoolean("armor.netherstar.enable");
		this.nsHelmetName = instance.getConfig("config.yml").getString("armor.netherstar.helmet.name");
		this.nsHelmetLore = instance.getConfig("config.yml").getStringList("armor.netherstar.helmet.lore");
		this.nsHelmetEnchants = instance.getConfig("config.yml").getStringList("armor.netherstar.helmet.enchantments");
		this.nsChestplateName = instance.getConfig("config.yml").getString("armor.netherstar.chestplate.name");
		this.nsChestplateLore = instance.getConfig("config.yml").getStringList("armor.netherstar.chestplate.lore");
		this.nsChestplateEnchants = instance.getConfig("config.yml")
				.getStringList("armor.netherstar.chestplate.enchantments");
		this.nsLeggingsName = instance.getConfig("config.yml").getString("armor.netherstar.leggings.name");
		this.nsLeggingsLore = instance.getConfig("config.yml").getStringList("armor.netherstar.leggings.lore");
		this.nsLeggingsEnchants = instance.getConfig("config.yml")
				.getStringList("armor.netherstar.leggings.enchantments");
		this.nsBootsName = instance.getConfig("config.yml").getString("armor.netherstar.boots.name");
		this.nsBootsLore = instance.getConfig("config.yml").getStringList("armor.netherstar.boots.lore");
		this.nsBootsEnchants = instance.getConfig("config.yml").getStringList("armor.netherstar.boots.enchantments");
		addArmor();
		ArmorEquipEvent.registerListener(instance);
		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	public void addArmor() {
		try {
			if (nrArmor) {
				Bukkit.removeRecipe(netherrackHelmetKey);
				Bukkit.removeRecipe(netherrackChestplateKey);
				Bukkit.removeRecipe(netherrackLeggingsKey);
				Bukkit.removeRecipe(netherrackBootsKey);
				Bukkit.addRecipe(nrHelmet());
				Bukkit.addRecipe(nrChestplate());
				Bukkit.addRecipe(nrLeggings());
				Bukkit.addRecipe(nrBoots());
			} else {
				Bukkit.removeRecipe(netherrackHelmetKey);
				Bukkit.removeRecipe(netherrackChestplateKey);
				Bukkit.removeRecipe(netherrackLeggingsKey);
				Bukkit.removeRecipe(netherrackBootsKey);
			}

			if (gsArmor) {
				Bukkit.removeRecipe(glowstoneHelmetKey);
				Bukkit.removeRecipe(glowstoneChestplateKey);
				Bukkit.removeRecipe(glowstoneLeggingsKey);
				Bukkit.removeRecipe(glowstoneBootsKey);
				Bukkit.addRecipe(gsHelmet());
				Bukkit.addRecipe(gsChestplate());
				Bukkit.addRecipe(gsLeggings());
				Bukkit.addRecipe(gsBoots());
			} else {
				Bukkit.removeRecipe(glowstoneHelmetKey);
				Bukkit.removeRecipe(glowstoneChestplateKey);
				Bukkit.removeRecipe(glowstoneLeggingsKey);
				Bukkit.removeRecipe(glowstoneBootsKey);
			}

			if (qzArmor) {
				Bukkit.removeRecipe(quartzHelmetKey);
				Bukkit.removeRecipe(quartzChestplateKey);
				Bukkit.removeRecipe(quartzLeggingsKey);
				Bukkit.removeRecipe(quartzBootsKey);
				Bukkit.addRecipe(qzHelmet());
				Bukkit.addRecipe(qzChestplate());
				Bukkit.addRecipe(qzLeggings());
				Bukkit.addRecipe(qzBoots());
			} else {
				Bukkit.removeRecipe(quartzHelmetKey);
				Bukkit.removeRecipe(quartzChestplateKey);
				Bukkit.removeRecipe(quartzLeggingsKey);
				Bukkit.removeRecipe(quartzBootsKey);
			}

			if (nsArmor) {
				Bukkit.removeRecipe(netherstarHelmetKey);
				Bukkit.removeRecipe(netherstarChestplateKey);
				Bukkit.removeRecipe(netherstarLeggingsKey);
				Bukkit.removeRecipe(netherstarBootsKey);
				Bukkit.addRecipe(nsHelmet());
				Bukkit.addRecipe(nsChestplate());
				Bukkit.addRecipe(nsLeggings());
				Bukkit.addRecipe(nsBoots());
			} else {
				Bukkit.removeRecipe(netherstarHelmetKey);
				Bukkit.removeRecipe(netherstarChestplateKey);
				Bukkit.removeRecipe(netherstarLeggingsKey);
				Bukkit.removeRecipe(netherstarBootsKey);
			}
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	public ShapedRecipe nrHelmet() {
		ItemBuilder armor = new ItemBuilder(Material.LEATHER_HELMET, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrHelmetName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrHelmetLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : nrHelmetEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", nrArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackHelmetKey, tagItem.load());
		recipe.shape(new String[] { "NNN", "N N", "   " });
		recipe.setIngredient('N', Material.NETHERRACK);
		return recipe;
	}

	public ShapedRecipe nrChestplate() {
		ItemBuilder armor = new ItemBuilder(Material.LEATHER_CHESTPLATE, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrChestplateName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrChestplateLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : nrChestplateEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", nrArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackChestplateKey, tagItem.load());
		recipe.shape(new String[] { "N N", "NNN", "NNN" });
		recipe.setIngredient('N', Material.NETHERRACK);
		return recipe;
	}

	public ShapedRecipe nrLeggings() {
		ItemBuilder armor = new ItemBuilder(Material.LEATHER_LEGGINGS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrLeggingsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrLeggingsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : nrLeggingsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", nrArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackLeggingsKey, tagItem.load());
		recipe.shape(new String[] { "NNN", "N N", "N N" });
		recipe.setIngredient('N', Material.NETHERRACK);
		return recipe;
	}

	public ShapedRecipe nrBoots() {
		ItemBuilder armor = new ItemBuilder(Material.LEATHER_BOOTS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrBootsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrBootsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : nrBootsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", nrArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackBootsKey, tagItem.load());
		recipe.shape(new String[] { "   ", "N N", "N N" });
		recipe.setIngredient('N', Material.NETHERRACK);
		return recipe;
	}

	public ShapedRecipe gsHelmet() {
		ItemBuilder armor = new ItemBuilder(Material.GOLDEN_HELMET, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsHelmetName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsHelmetLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : gsHelmetEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherArmor", gsArmor), Map.of("wearsNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneHelmetKey, tagItem.load());
		recipe.shape(new String[] { "NNN", "N N", "   " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		return recipe;
	}

	public ShapedRecipe gsChestplate() {
		ItemBuilder armor = new ItemBuilder(Material.GOLDEN_CHESTPLATE, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsChestplateName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsChestplateLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : gsChestplateEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherArmor", gsArmor), Map.of("wearsNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneChestplateKey, tagItem.load());
		recipe.shape(new String[] { "N N", "NNN", "NNN" });
		recipe.setIngredient('N', Material.GLOWSTONE);
		return recipe;
	}

	public ShapedRecipe gsLeggings() {
		ItemBuilder armor = new ItemBuilder(Material.GOLDEN_LEGGINGS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsLeggingsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsLeggingsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : gsLeggingsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherArmor", gsArmor), Map.of("wearsNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneLeggingsKey, tagItem.load());
		recipe.shape(new String[] { "NNN", "N N", "N N" });
		recipe.setIngredient('N', Material.GLOWSTONE);
		return recipe;
	}

	public ShapedRecipe gsBoots() {
		ItemBuilder armor = new ItemBuilder(Material.GOLDEN_BOOTS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsBootsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsBootsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : gsBootsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherArmor", gsArmor), Map.of("wearsNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneBootsKey, tagItem.load());
		recipe.shape(new String[] { "   ", "N N", "N N" });
		recipe.setIngredient('N', Material.GLOWSTONE);
		return recipe;
	}

	public ShapedRecipe qzHelmet() {
		ItemBuilder armor = new ItemBuilder(Material.IRON_HELMET, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzHelmetName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzHelmetLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : qzHelmetEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", qzArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzHelmetKey, tagItem.load());
		recipe.shape(new String[] { "NNN", "N N", "   " });
		recipe.setIngredient('N', Material.QUARTZ);
		return recipe;
	}

	public ShapedRecipe qzChestplate() {
		ItemBuilder armor = new ItemBuilder(Material.IRON_CHESTPLATE, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzChestplateName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzChestplateLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : qzChestplateEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", qzArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzChestplateKey, tagItem.load());
		recipe.shape(new String[] { "N N", "NNN", "NNN" });
		recipe.setIngredient('N', Material.QUARTZ);
		return recipe;
	}

	public ShapedRecipe qzLeggings() {
		ItemBuilder armor = new ItemBuilder(Material.IRON_LEGGINGS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzLeggingsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzLeggingsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : qzLeggingsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", qzArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzLeggingsKey, tagItem.load());
		recipe.shape(new String[] { "NNN", "N N", "N N" });
		recipe.setIngredient('N', Material.QUARTZ);
		return recipe;
	}

	public ShapedRecipe qzBoots() {
		ItemBuilder armor = new ItemBuilder(Material.IRON_BOOTS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzBootsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzBootsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : qzBootsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", qzArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzBootsKey, tagItem.load());
		recipe.shape(new String[] { "   ", "N N", "N N" });
		recipe.setIngredient('N', Material.QUARTZ);
		return recipe;
	}

	public ShapedRecipe nsHelmet() {
		ItemBuilder armor = new ItemBuilder(Material.DIAMOND_HELMET, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsHelmetName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsHelmetLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : nsHelmetEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", nsArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarHelmetKey, tagItem.load());
		recipe.shape(new String[] { "NNN", "N N", "   " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		return recipe;
	}

	public ShapedRecipe nsChestplate() {
		ItemBuilder armor = new ItemBuilder(Material.DIAMOND_CHESTPLATE, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsChestplateName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsChestplateLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : nsChestplateEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", nsArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarChestplateKey, tagItem.load());
		recipe.shape(new String[] { "N N", "NNN", "NNN" });
		recipe.setIngredient('N', Material.NETHER_STAR);
		return recipe;
	}

	public ShapedRecipe nsLeggings() {
		ItemBuilder armor = new ItemBuilder(Material.DIAMOND_LEGGINGS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsLeggingsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsLeggingsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : nsLeggingsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", nsArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarLeggingsKey, tagItem.load());
		recipe.shape(new String[] { "NNN", "N N", "N N" });
		recipe.setIngredient('N', Material.NETHER_STAR);
		return recipe;
	}

	public ShapedRecipe nsBoots() {
		ItemBuilder armor = new ItemBuilder(Material.DIAMOND_BOOTS, 1);

		armor.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsBootsName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsBootsLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		armor.setLore(lore);

		for (String enchants : nsBootsEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			armor.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherArmor", nsArmor)));
		RtagItem tagItem = new RtagItem(armor.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarBootsKey, tagItem.load());
		recipe.shape(new String[] { "   ", "N N", "N N" });
		recipe.setIngredient('N', Material.NETHER_STAR);
		return recipe;
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (!(nrArmor) && !(gsArmor) && !(qzArmor) && !(nsArmor)) {
			return;
		}
		if (!(event.getView().getPlayer() instanceof Player))
			return;
		Player player = (Player) event.getView().getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		Recipe recipe = event.getRecipe();
		ItemStack result = recipe.getResult();
		RtagItem tagItem = new RtagItem(result);
		if (checkArmorData(tagItem)) {
			if (recipe instanceof CraftingRecipe) {
				CraftingRecipe craft = (CraftingRecipe) recipe;
				player.discoverRecipe(craft.getKey());
			}
		}
	}

	@EventHandler
	public void onArmorChange(PlayerArmorChangeEvent event) {
		if (!this.gsNightVision)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		ItemStack armor = event.getNewItem();
		HellblockPlayer hbPlayer = new HellblockPlayer(player.getUniqueId());
		if (armor != null && armor.getType() != Material.AIR) {
			RtagItem tagItem = new RtagItem(armor);
			if (checkNightVisionArmorStatus(tagItem)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				hbPlayer.isWearingGlowstoneArmor(true);
			} else {
				if (hbPlayer.hasGlowstoneArmorEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						hbPlayer.isWearingGlowstoneArmor(false);
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
					RtagItem tag = new RtagItem(item);
					if (checkNightVisionArmorStatus(tag)) {
						checkArmor = true;
						break;
					}
				}
			}
			if (!checkArmor && hbPlayer.hasGlowstoneArmorEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					hbPlayer.isWearingGlowstoneArmor(false);
				}
			}
		}
	}

	@EventHandler
	public void onArmorEquip(ArmorEquipEvent event) {
		if (!this.gsNightVision)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		ItemStack armor = event.getOldArmorPiece();
		HellblockPlayer hbPlayer = new HellblockPlayer(player.getUniqueId());
		if (armor == null) {
			ItemStack glowStoneArmor = event.getNewArmorPiece();
			if (glowStoneArmor != null && glowStoneArmor.getType() != Material.AIR) {
				RtagItem tagItem = new RtagItem(glowStoneArmor);
				if (checkNightVisionArmorStatus(tagItem)) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
					hbPlayer.isWearingGlowstoneArmor(true);
				} else {
					if (hbPlayer.hasGlowstoneArmorEffect()) {
						if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);
							hbPlayer.isWearingGlowstoneArmor(false);
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
						RtagItem tag = new RtagItem(item);
						if (checkNightVisionArmorStatus(tag)) {
							checkArmor = true;
							break;
						}
					}
				}
				if (!checkArmor && hbPlayer.hasGlowstoneArmorEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						hbPlayer.isWearingGlowstoneArmor(false);
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
					RtagItem tag = new RtagItem(item);
					if (checkNightVisionArmorStatus(tag)) {
						checkArmor = true;
						break;
					}
				}
			}
			if (!checkArmor && hbPlayer.hasGlowstoneArmorEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					hbPlayer.isWearingGlowstoneArmor(false);
				}
			}
		}
	}

	public boolean checkArmorData(RtagItem tag) {
		if (tag == null || tag.get("HellblockRecipe", 0) == null)
			return false;

		boolean data = false;
		if (tag.get("HellblockRecipe", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellblockRecipe", 0);
			for (String key : map.keySet()) {
				if (key.equals("isNetherArmor")) {
					for (Object value : map.values()) {
						if ((byte) value == 1) {
							data = true;
						}
					}
				}
			}
		}
		return data;
	}

	public boolean checkNightVisionArmorStatus(RtagItem tag) {
		if (tag == null || !gsNightVision || tag.get("HellblockRecipe", 1) == null)
			return false;

		boolean data = false;
		if (tag.get("HellblockRecipe", 1) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellblockRecipe", 1);
			for (String key : map.keySet()) {
				if (key.equals("wearsNightVision")) {
					for (Object value : map.values()) {
						if ((byte) value == 1) {
							data = true;
						}
					}
				}
			}
		}
		return data;
	}
}
