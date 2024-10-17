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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

public class NetherTools implements Listener {

	private final HellblockPlugin instance;

	private final NamespacedKey netherrackPickaxeKey, netherrackAxeKey, netherrackShovelKey, netherrackHoeKey,
			netherrackSwordKey, glowstonePickaxeKey, glowstoneAxeKey, glowstoneShovelKey, glowstoneHoeKey,
			glowstoneSwordKey, quartzPickaxeKey, quartzAxeKey, quartzShovelKey, quartzHoeKey, quartzSwordKey,
			netherstarPickaxeKey, netherstarAxeKey, netherstarShovelKey, netherstarHoeKey, netherstarSwordKey;

	public boolean nrTools;
	public String nrPickaxeName;
	public List<String> nrPickaxeLore;
	public List<String> nrPickaxeEnchants;
	public String nrAxeName;
	public List<String> nrAxeLore;
	public List<String> nrAxeEnchants;
	public String nrShovelName;
	public List<String> nrShovelLore;
	public List<String> nrShovelEnchants;
	public String nrHoeName;
	public List<String> nrHoeLore;
	public List<String> nrHoeEnchants;
	public String nrSwordName;
	public List<String> nrSwordLore;
	public List<String> nrSwordEnchants;
	public boolean gsTools;
	public boolean gsNightVision;
	public String gsPickaxeName;
	public List<String> gsPickaxeLore;
	public List<String> gsPickaxeEnchants;
	public String gsAxeName;
	public List<String> gsAxeLore;
	public List<String> gsAxeEnchants;
	public String gsShovelName;
	public List<String> gsShovelLore;
	public List<String> gsShovelEnchants;
	public String gsHoeName;
	public List<String> gsHoeLore;
	public List<String> gsHoeEnchants;
	public String gsSwordName;
	public List<String> gsSwordLore;
	public List<String> gsSwordEnchants;
	public boolean qzTools;
	public String qzPickaxeName;
	public List<String> qzPickaxeLore;
	public List<String> qzPickaxeEnchants;
	public String qzAxeName;
	public List<String> qzAxeLore;
	public List<String> qzAxeEnchants;
	public String qzShovelName;
	public List<String> qzShovelLore;
	public List<String> qzShovelEnchants;
	public String qzHoeName;
	public List<String> qzHoeLore;
	public List<String> qzHoeEnchants;
	public String qzSwordName;
	public List<String> qzSwordLore;
	public List<String> qzSwordEnchants;
	public boolean nsTools;
	public String nsPickaxeName;
	public List<String> nsPickaxeLore;
	public List<String> nsPickaxeEnchants;
	public String nsAxeName;
	public List<String> nsAxeLore;
	public List<String> nsAxeEnchants;
	public String nsShovelName;
	public List<String> nsShovelLore;
	public List<String> nsShovelEnchants;
	public String nsHoeName;
	public List<String> nsHoeLore;
	public List<String> nsHoeEnchants;
	public String nsSwordName;
	public List<String> nsSwordLore;
	public List<String> nsSwordEnchants;

	private final Registry<Enchantment> enchantmentRegistry;

	public NetherTools(HellblockPlugin plugin) {
		this.instance = plugin;
		this.enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
		this.netherrackPickaxeKey = new NamespacedKey(this.instance, "netherrackpickaxe");
		this.netherrackAxeKey = new NamespacedKey(this.instance, "netherrackaxe");
		this.netherrackShovelKey = new NamespacedKey(this.instance, "netherrackshovel");
		this.netherrackHoeKey = new NamespacedKey(this.instance, "netherrackhoe");
		this.netherrackSwordKey = new NamespacedKey(this.instance, "netherracksword");
		this.glowstonePickaxeKey = new NamespacedKey(this.instance, "glowstonepickaxe");
		this.glowstoneAxeKey = new NamespacedKey(this.instance, "glowstoneaxe");
		this.glowstoneShovelKey = new NamespacedKey(this.instance, "glowstoneshovel");
		this.glowstoneHoeKey = new NamespacedKey(this.instance, "glowstonehoe");
		this.glowstoneSwordKey = new NamespacedKey(this.instance, "glowstonesword");
		this.quartzPickaxeKey = new NamespacedKey(this.instance, "quartzpickaxe");
		this.quartzAxeKey = new NamespacedKey(this.instance, "quartzaxe");
		this.quartzShovelKey = new NamespacedKey(this.instance, "quartzshovel");
		this.quartzHoeKey = new NamespacedKey(this.instance, "quartzhoe");
		this.quartzSwordKey = new NamespacedKey(this.instance, "quartzsword");
		this.netherstarPickaxeKey = new NamespacedKey(this.instance, "netherstarpickaxe");
		this.netherstarAxeKey = new NamespacedKey(this.instance, "netherstaraxe");
		this.netherstarShovelKey = new NamespacedKey(this.instance, "netherstarshovel");
		this.netherstarHoeKey = new NamespacedKey(this.instance, "netherstarhoe");
		this.netherstarSwordKey = new NamespacedKey(this.instance, "netherstarsword");
		this.nrTools = instance.getConfig("config.yml").getBoolean("tools.netherrack.enable");
		this.nrPickaxeName = instance.getConfig("config.yml").getString("tools.netherrack.pickaxe.name");
		this.nrPickaxeLore = instance.getConfig("config.yml").getStringList("tools.netherrack.pickaxe.lore");
		this.nrPickaxeEnchants = instance.getConfig("config.yml")
				.getStringList("tools.netherrack.pickaxe.enchantments");
		this.nrAxeName = instance.getConfig("config.yml").getString("tools.netherrack.axe.name");
		this.nrAxeLore = instance.getConfig("config.yml").getStringList("tools.netherrack.axe.lore");
		this.nrAxeEnchants = instance.getConfig("config.yml").getStringList("tools.netherrack.axe.enchantments");
		this.nrShovelName = instance.getConfig("config.yml").getString("tools.netherrack.shovel.name");
		this.nrShovelLore = instance.getConfig("config.yml").getStringList("tools.netherrack.shovel.lore");
		this.nrShovelEnchants = instance.getConfig("config.yml").getStringList("tools.netherrack.shovel.enchantments");
		this.nrHoeName = instance.getConfig("config.yml").getString("tools.netherrack.hoe.name");
		this.nrHoeLore = instance.getConfig("config.yml").getStringList("tools.netherrack.hoe.lore");
		this.nrHoeEnchants = instance.getConfig("config.yml").getStringList("tools.netherrack.hoe.enchantments");
		this.nrSwordName = instance.getConfig("config.yml").getString("tools.netherrack.sword.name");
		this.nrSwordLore = instance.getConfig("config.yml").getStringList("tools.netherrack.sword.lore");
		this.nrSwordEnchants = instance.getConfig("config.yml").getStringList("tools.netherrack.sword.enchantments");
		this.gsTools = instance.getConfig("config.yml").getBoolean("tools.glowstone.enable");
		this.gsNightVision = instance.getConfig("config.yml").getBoolean("tools.glowstone.night-vision");
		this.gsPickaxeName = instance.getConfig("config.yml").getString("tools.glowstone.pickaxe.name");
		this.gsPickaxeLore = instance.getConfig("config.yml").getStringList("tools.glowstone.pickaxe.lore");
		this.gsPickaxeEnchants = instance.getConfig("config.yml").getStringList("tools.glowstone.pickaxe.enchantments");
		this.gsAxeName = instance.getConfig("config.yml").getString("tools.glowstone.axe.name");
		this.gsAxeLore = instance.getConfig("config.yml").getStringList("tools.glowstone.axe.lore");
		this.gsAxeEnchants = instance.getConfig("config.yml").getStringList("tools.glowstone.axe.enchantments");
		this.gsShovelName = instance.getConfig("config.yml").getString("tools.glowstone.shovel.name");
		this.gsShovelLore = instance.getConfig("config.yml").getStringList("tools.glowstone.shovel.lore");
		this.gsShovelEnchants = instance.getConfig("config.yml").getStringList("tools.glowstone.shovel.enchantments");
		this.gsHoeName = instance.getConfig("config.yml").getString("tools.glowstone.hoe.name");
		this.gsHoeLore = instance.getConfig("config.yml").getStringList("tools.glowstone.hoe.lore");
		this.gsHoeEnchants = instance.getConfig("config.yml").getStringList("tools.glowstone.hoe.enchantments");
		this.gsSwordName = instance.getConfig("config.yml").getString("tools.glowstone.sword.name");
		this.gsSwordLore = instance.getConfig("config.yml").getStringList("tools.glowstone.sword.lore");
		this.gsSwordEnchants = instance.getConfig("config.yml").getStringList("tools.glowstone.sword.enchantments");
		this.qzTools = instance.getConfig("config.yml").getBoolean("tools.quartz.enable");
		this.qzPickaxeName = instance.getConfig("config.yml").getString("tools.quartz.pickaxe.name");
		this.qzPickaxeLore = instance.getConfig("config.yml").getStringList("tools.quartz.pickaxe.lore");
		this.qzPickaxeEnchants = instance.getConfig("config.yml").getStringList("tools.quartz.pickaxe.enchantments");
		this.qzAxeName = instance.getConfig("config.yml").getString("tools.quartz.axe.name");
		this.qzAxeLore = instance.getConfig("config.yml").getStringList("tools.quartz.axe.lore");
		this.qzAxeEnchants = instance.getConfig("config.yml").getStringList("tools.quartz.axe.enchantments");
		this.qzShovelName = instance.getConfig("config.yml").getString("tools.quartz.shovel.name");
		this.qzShovelLore = instance.getConfig("config.yml").getStringList("tools.quartz.shovel.lore");
		this.qzShovelEnchants = instance.getConfig("config.yml").getStringList("tools.quartz.shovel.enchantments");
		this.qzHoeName = instance.getConfig("config.yml").getString("tools.quartz.hoe.name");
		this.qzHoeLore = instance.getConfig("config.yml").getStringList("tools.quartz.hoe.lore");
		this.qzHoeEnchants = instance.getConfig("config.yml").getStringList("tools.quartz.hoe.enchantments");
		this.qzSwordName = instance.getConfig("config.yml").getString("tools.quartz.sword.name");
		this.qzSwordLore = instance.getConfig("config.yml").getStringList("tools.quartz.sword.lore");
		this.qzSwordEnchants = instance.getConfig("config.yml").getStringList("tools.quartz.sword.enchantments");
		this.nsTools = instance.getConfig("config.yml").getBoolean("tools.netherstar.enable");
		this.nsPickaxeName = instance.getConfig("config.yml").getString("tools.netherstar.pickaxe.name");
		this.nsPickaxeLore = instance.getConfig("config.yml").getStringList("tools.netherstar.pickaxe.lore");
		this.nsPickaxeEnchants = instance.getConfig("config.yml")
				.getStringList("tools.netherstar.pickaxe.enchantments");
		this.nsAxeName = instance.getConfig("config.yml").getString("tools.netherstar.axe.name");
		this.nsAxeLore = instance.getConfig("config.yml").getStringList("tools.netherstar.axe.lore");
		this.nsAxeEnchants = instance.getConfig("config.yml").getStringList("tools.netherstar.axe.enchantments");
		this.nsShovelName = instance.getConfig("config.yml").getString("tools.netherstar.shovel.name");
		this.nsShovelLore = instance.getConfig("config.yml").getStringList("tools.netherstar.shovel.lore");
		this.nsShovelEnchants = instance.getConfig("config.yml").getStringList("tools.netherstar.shovel.enchantments");
		this.nsHoeName = instance.getConfig("config.yml").getString("tools.netherstar.hoe.name");
		this.nsHoeLore = instance.getConfig("config.yml").getStringList("tools.netherstar.hoe.lore");
		this.nsHoeEnchants = instance.getConfig("config.yml").getStringList("tools.netherstar.hoe.enchantments");
		this.nsSwordName = instance.getConfig("config.yml").getString("tools.netherstar.sword.name");
		this.nsSwordLore = instance.getConfig("config.yml").getStringList("tools.netherstar.sword.lore");
		this.nsSwordEnchants = instance.getConfig("config.yml").getStringList("tools.netherstar.sword.enchantments");
		addTools();
		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	public void addTools() {
		try {
			if (nrTools) {
				Bukkit.removeRecipe(netherrackPickaxeKey);
				Bukkit.removeRecipe(netherrackAxeKey);
				Bukkit.removeRecipe(netherrackShovelKey);
				Bukkit.removeRecipe(netherrackHoeKey);
				Bukkit.removeRecipe(netherrackSwordKey);
				Bukkit.addRecipe(nrPickaxe());
				Bukkit.addRecipe(nrAxe());
				Bukkit.addRecipe(nrShovel());
				Bukkit.addRecipe(nrHoe());
				Bukkit.addRecipe(nrSword());
			} else {
				Bukkit.removeRecipe(netherrackPickaxeKey);
				Bukkit.removeRecipe(netherrackAxeKey);
				Bukkit.removeRecipe(netherrackShovelKey);
				Bukkit.removeRecipe(netherrackHoeKey);
				Bukkit.removeRecipe(netherrackSwordKey);
			}

			if (gsTools) {
				Bukkit.removeRecipe(glowstonePickaxeKey);
				Bukkit.removeRecipe(glowstoneAxeKey);
				Bukkit.removeRecipe(glowstoneShovelKey);
				Bukkit.removeRecipe(glowstoneHoeKey);
				Bukkit.removeRecipe(glowstoneSwordKey);
				Bukkit.addRecipe(gsPickaxe());
				Bukkit.addRecipe(gsAxe());
				Bukkit.addRecipe(gsShovel());
				Bukkit.addRecipe(gsHoe());
				Bukkit.addRecipe(gsSword());
			} else {
				Bukkit.removeRecipe(glowstonePickaxeKey);
				Bukkit.removeRecipe(glowstoneAxeKey);
				Bukkit.removeRecipe(glowstoneShovelKey);
				Bukkit.removeRecipe(glowstoneHoeKey);
				Bukkit.removeRecipe(glowstoneSwordKey);
			}

			if (qzTools) {
				Bukkit.removeRecipe(quartzPickaxeKey);
				Bukkit.removeRecipe(quartzAxeKey);
				Bukkit.removeRecipe(quartzShovelKey);
				Bukkit.removeRecipe(quartzHoeKey);
				Bukkit.removeRecipe(quartzSwordKey);
				Bukkit.addRecipe(qzPickaxe());
				Bukkit.addRecipe(qzAxe());
				Bukkit.addRecipe(qzShovel());
				Bukkit.addRecipe(qzHoe());
				Bukkit.addRecipe(qzSword());
			} else {
				Bukkit.removeRecipe(quartzPickaxeKey);
				Bukkit.removeRecipe(quartzAxeKey);
				Bukkit.removeRecipe(quartzShovelKey);
				Bukkit.removeRecipe(quartzHoeKey);
				Bukkit.removeRecipe(quartzSwordKey);
			}

			if (nsTools) {
				Bukkit.removeRecipe(netherstarPickaxeKey);
				Bukkit.removeRecipe(netherstarAxeKey);
				Bukkit.removeRecipe(netherstarShovelKey);
				Bukkit.removeRecipe(netherstarHoeKey);
				Bukkit.removeRecipe(netherstarSwordKey);
				Bukkit.addRecipe(nsPickaxe());
				Bukkit.addRecipe(nsAxe());
				Bukkit.addRecipe(nsShovel());
				Bukkit.addRecipe(nsHoe());
				Bukkit.addRecipe(nsSword());
			} else {
				Bukkit.removeRecipe(netherstarPickaxeKey);
				Bukkit.removeRecipe(netherstarAxeKey);
				Bukkit.removeRecipe(netherstarShovelKey);
				Bukkit.removeRecipe(netherstarHoeKey);
				Bukkit.removeRecipe(netherstarSwordKey);
			}
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	public ShapedRecipe nrPickaxe() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_PICKAXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrPickaxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrPickaxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nrPickaxeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nrTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackPickaxeKey, tagItem.load());
		recipe.shape(new String[] { "NNN", " B ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nrAxe() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_AXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrAxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrAxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nrAxeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nrTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackAxeKey, tagItem.load());
		recipe.shape(new String[] { "NN ", "NB ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nrShovel() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_SHOVEL, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrShovelName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrShovelLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nrShovelEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nrTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackShovelKey, tagItem.load());
		recipe.shape(new String[] { " N ", " B ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nrHoe() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_HOE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrHoeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrHoeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nrHoeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nrTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackHoeKey, tagItem.load());
		recipe.shape(new String[] { "NN ", " B ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nrSword() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_SWORD, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nrSwordName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nrSwordLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nrSwordEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nrTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherrackSwordKey, tagItem.load());
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsPickaxe() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_PICKAXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsPickaxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsPickaxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : gsPickaxeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherTool", gsTools), Map.of("hasNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstonePickaxeKey, tagItem.load());
		recipe.shape(new String[] { "NNN", " B ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsAxe() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_AXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsAxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsAxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : gsAxeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherTool", gsTools), Map.of("hasNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneAxeKey, tagItem.load());
		recipe.shape(new String[] { "NN ", "NB ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsShovel() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_SHOVEL, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsShovelName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsShovelLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : gsShovelEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherTool", gsTools), Map.of("hasNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneShovelKey, tagItem.load());
		recipe.shape(new String[] { " N ", " B ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsHoe() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_HOE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsHoeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsHoeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : gsHoeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherTool", gsTools), Map.of("hasNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneHoeKey, tagItem.load());
		recipe.shape(new String[] { "NN ", " B ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsSword() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_SWORD, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(gsSwordName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : gsSwordLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : gsSwordEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe",
				List.of(Map.of("isNetherTool", gsTools), Map.of("hasNightVision", gsNightVision)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneSwordKey, tagItem.load());
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzPickaxe() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_PICKAXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzPickaxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzPickaxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : qzPickaxeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", qzTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzPickaxeKey, tagItem.load());
		recipe.shape(new String[] { "NNN", " B ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzAxe() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_AXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzAxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzAxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : qzAxeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", qzTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzAxeKey, tagItem.load());
		recipe.shape(new String[] { "NN ", "NB ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzShovel() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_SHOVEL, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzShovelName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzShovelLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : qzShovelEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", qzTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzShovelKey, tagItem.load());
		recipe.shape(new String[] { " N ", " B ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzHoe() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_HOE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzHoeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzHoeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : qzHoeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", qzTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzHoeKey, tagItem.load());
		recipe.shape(new String[] { "NN ", " B ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzSword() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_SWORD, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(qzSwordName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : qzSwordLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : qzSwordEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", qzTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(quartzSwordKey, tagItem.load());
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsPickaxe() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_PICKAXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsPickaxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsPickaxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nsPickaxeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nsTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarPickaxeKey, tagItem.load());
		recipe.shape(new String[] { "NNN", " B ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsAxe() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_AXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsAxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsAxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nsAxeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nsTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarAxeKey, tagItem.load());
		recipe.shape(new String[] { "NN ", "NB ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsShovel() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_SHOVEL, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsShovelName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsShovelLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nsShovelEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nsTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarShovelKey, tagItem.load());
		recipe.shape(new String[] { " N ", " B ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsHoe() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_HOE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsHoeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsHoeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nsHoeEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nsTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarHoeKey, tagItem.load());
		recipe.shape(new String[] { "NN ", " B ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsSword() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_SWORD, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(nsSwordName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nsSwordLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : nsSwordEnchants) {
			String[] split = enchants.split(":");
			Enchantment enchantment = enchantmentRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
			int level = 1;
			try {
				level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe("Invalid quantity: " + split[1] + "!");
			}
			tool.addEnchantment(enchantment, level, false);
		}

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherTool", nsTools)));
		RtagItem tagItem = new RtagItem(tool.get());
		tagItem.set(data);

		ShapedRecipe recipe = new ShapedRecipe(netherstarSwordKey, tagItem.load());
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (!(nrTools) && !(gsTools) && !(qzTools) && !(nsTools)) {
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
		if (checkToolData(tagItem)) {
			if (recipe instanceof CraftingRecipe) {
				CraftingRecipe craft = (CraftingRecipe) recipe;
				player.discoverRecipe(craft.getKey());
			}
		}
	}

	@EventHandler
	public void onSlotChange(PlayerItemHeldEvent event) {
		if (!this.gsNightVision)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		ItemStack tool = player.getInventory().getItem(event.getNewSlot());
		HellblockPlayer hbPlayer = new HellblockPlayer(player.getUniqueId());
		if (tool != null && tool.getType() != Material.AIR) {
			RtagItem tagItem = new RtagItem(tool);
			if (checkNightVisionToolStatus(tagItem)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				hbPlayer.isHoldingGlowstoneTool(true);
			} else {
				if (hbPlayer.hasGlowstoneToolEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						hbPlayer.isHoldingGlowstoneTool(false);
					}
				}
			}
		} else {
			if (hbPlayer.hasGlowstoneToolEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					hbPlayer.isHoldingGlowstoneTool(false);
				}
			}
		}
	}

	public boolean checkToolData(RtagItem tag) {
		if (tag == null || tag.get("HellblockRecipe", 0) == null)
			return false;

		boolean data = false;
		if (tag.get("HellblockRecipe", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellblockRecipe", 0);
			for (String key : map.keySet()) {
				if (key.equals("isNetherTool")) {
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

	public boolean checkNightVisionToolStatus(RtagItem tag) {
		if (tag == null || !gsNightVision || tag.get("HellblockRecipe", 1) == null)
			return false;

		boolean data = false;
		if (tag.get("HellblockRecipe", 1) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellblockRecipe", 1);
			for (String key : map.keySet()) {
				if (key.equals("hasNightVision")) {
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
