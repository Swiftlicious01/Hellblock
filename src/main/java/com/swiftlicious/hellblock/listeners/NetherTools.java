package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

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
	public boolean gsNightVisionTool;
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

	private final Set<Material> netherrackTools, glowstoneTools, quartzTools, netherstarTools;

	private final Map<HellblockPlayer, Boolean> clickCache;

	private final Registry<Enchantment> enchantmentRegistry;

	public NetherTools(HellblockPlugin plugin) {
		this.instance = plugin;
		this.enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
		this.clickCache = new HashMap<>();
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
		this.nrTools = instance.getConfig("config.yml").getBoolean("tools.netherrack.enable", true);
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
		this.gsTools = instance.getConfig("config.yml").getBoolean("tools.glowstone.enable", true);
		this.gsNightVisionTool = instance.getConfig("config.yml").getBoolean("tools.glowstone.night-vision", true);
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
		this.qzTools = instance.getConfig("config.yml").getBoolean("tools.quartz.enable", true);
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
		this.nsTools = instance.getConfig("config.yml").getBoolean("tools.netherstar.enable", true);
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
		this.netherrackTools = Set.of(Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_SHOVEL,
				Material.STONE_HOE, Material.STONE_SWORD);
		this.glowstoneTools = Set.of(Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_SHOVEL,
				Material.GOLDEN_HOE, Material.GOLDEN_SWORD);
		this.quartzTools = Set.of(Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL, Material.IRON_HOE,
				Material.IRON_SWORD);
		this.netherstarTools = Set.of(Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL,
				Material.DIAMOND_HOE, Material.DIAMOND_SWORD);
		addTools();
		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	public void addTools() {
		try {
			if (this.nrTools) {
				Bukkit.removeRecipe(this.netherrackPickaxeKey);
				Bukkit.removeRecipe(this.netherrackAxeKey);
				Bukkit.removeRecipe(this.netherrackShovelKey);
				Bukkit.removeRecipe(this.netherrackHoeKey);
				Bukkit.removeRecipe(this.netherrackSwordKey);
				Bukkit.addRecipe(nrPickaxe());
				Bukkit.addRecipe(nrAxe());
				Bukkit.addRecipe(nrShovel());
				Bukkit.addRecipe(nrHoe());
				Bukkit.addRecipe(nrSword());
			} else {
				Bukkit.removeRecipe(this.netherrackPickaxeKey);
				Bukkit.removeRecipe(this.netherrackAxeKey);
				Bukkit.removeRecipe(this.netherrackShovelKey);
				Bukkit.removeRecipe(this.netherrackHoeKey);
				Bukkit.removeRecipe(this.netherrackSwordKey);
			}

			if (this.gsTools) {
				Bukkit.removeRecipe(this.glowstonePickaxeKey);
				Bukkit.removeRecipe(this.glowstoneAxeKey);
				Bukkit.removeRecipe(this.glowstoneShovelKey);
				Bukkit.removeRecipe(this.glowstoneHoeKey);
				Bukkit.removeRecipe(this.glowstoneSwordKey);
				Bukkit.addRecipe(gsPickaxe());
				Bukkit.addRecipe(gsAxe());
				Bukkit.addRecipe(gsShovel());
				Bukkit.addRecipe(gsHoe());
				Bukkit.addRecipe(gsSword());
			} else {
				Bukkit.removeRecipe(this.glowstonePickaxeKey);
				Bukkit.removeRecipe(this.glowstoneAxeKey);
				Bukkit.removeRecipe(this.glowstoneShovelKey);
				Bukkit.removeRecipe(this.glowstoneHoeKey);
				Bukkit.removeRecipe(this.glowstoneSwordKey);
			}

			if (this.qzTools) {
				Bukkit.removeRecipe(this.quartzPickaxeKey);
				Bukkit.removeRecipe(this.quartzAxeKey);
				Bukkit.removeRecipe(this.quartzShovelKey);
				Bukkit.removeRecipe(this.quartzHoeKey);
				Bukkit.removeRecipe(this.quartzSwordKey);
				Bukkit.addRecipe(qzPickaxe());
				Bukkit.addRecipe(qzAxe());
				Bukkit.addRecipe(qzShovel());
				Bukkit.addRecipe(qzHoe());
				Bukkit.addRecipe(qzSword());
			} else {
				Bukkit.removeRecipe(this.quartzPickaxeKey);
				Bukkit.removeRecipe(this.quartzAxeKey);
				Bukkit.removeRecipe(this.quartzShovelKey);
				Bukkit.removeRecipe(this.quartzHoeKey);
				Bukkit.removeRecipe(this.quartzSwordKey);
			}

			if (this.nsTools) {
				Bukkit.removeRecipe(this.netherstarPickaxeKey);
				Bukkit.removeRecipe(this.netherstarAxeKey);
				Bukkit.removeRecipe(this.netherstarShovelKey);
				Bukkit.removeRecipe(this.netherstarHoeKey);
				Bukkit.removeRecipe(this.netherstarSwordKey);
				Bukkit.addRecipe(nsPickaxe());
				Bukkit.addRecipe(nsAxe());
				Bukkit.addRecipe(nsShovel());
				Bukkit.addRecipe(nsHoe());
				Bukkit.addRecipe(nsSword());
			} else {
				Bukkit.removeRecipe(this.netherstarPickaxeKey);
				Bukkit.removeRecipe(this.netherstarAxeKey);
				Bukkit.removeRecipe(this.netherstarShovelKey);
				Bukkit.removeRecipe(this.netherstarHoeKey);
				Bukkit.removeRecipe(this.netherstarSwordKey);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nrTools);

		ShapedRecipe recipe = new ShapedRecipe(netherrackPickaxeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nrTools);

		ShapedRecipe recipe = new ShapedRecipe(netherrackAxeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nrTools);

		ShapedRecipe recipe = new ShapedRecipe(netherrackShovelKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nrTools);

		ShapedRecipe recipe = new ShapedRecipe(netherrackHoeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nrTools);

		ShapedRecipe recipe = new ShapedRecipe(netherrackSwordKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), gsTools);
		data = setNightVisionToolStatus(data, gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(glowstonePickaxeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), gsTools);
		data = setNightVisionToolStatus(data, gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneAxeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), gsTools);
		data = setNightVisionToolStatus(data, gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneShovelKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), gsTools);
		data = setNightVisionToolStatus(data, gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneHoeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), gsTools);
		data = setNightVisionToolStatus(data, gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(glowstoneSwordKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), qzTools);

		ShapedRecipe recipe = new ShapedRecipe(quartzPickaxeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), qzTools);

		ShapedRecipe recipe = new ShapedRecipe(quartzAxeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), qzTools);

		ShapedRecipe recipe = new ShapedRecipe(quartzShovelKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), qzTools);

		ShapedRecipe recipe = new ShapedRecipe(quartzHoeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), qzTools);

		ShapedRecipe recipe = new ShapedRecipe(quartzSwordKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nsTools);

		ShapedRecipe recipe = new ShapedRecipe(netherstarPickaxeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nsTools);

		ShapedRecipe recipe = new ShapedRecipe(netherstarAxeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nsTools);

		ShapedRecipe recipe = new ShapedRecipe(netherstarShovelKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nsTools);

		ShapedRecipe recipe = new ShapedRecipe(netherstarHoeKey, data);
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
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
			}
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), nsTools);

		ShapedRecipe recipe = new ShapedRecipe(netherstarSwordKey, data);
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (event.getView().getPlayer() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
				return;
			}

			Recipe recipe = event.getRecipe();
			ItemStack result = recipe.getResult();
			if (isNetherToolEnabled(result)) {
				if (checkToolData(result) && getToolData(result)) {
					if (recipe instanceof CraftingRecipe craft) {
						player.discoverRecipe(craft.getKey());
					}
				}
			}
		}
	}

	@EventHandler
	public void onSlotChange(PlayerItemHeldEvent event) {
		if (!this.gsNightVisionTool || !this.gsTools)
			return;
		Player player = event.getPlayer();

		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		ItemStack tool = player.getInventory().getItem(event.getNewSlot());
		boolean inOffHand = false;
		if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
			inOffHand = false;
		} else {
			tool = player.getInventory().getItemInOffHand();
			if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
				inOffHand = true;
			}
		}

		if (tool != null && tool.getType() == Material.AIR) {
			if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
					if (!instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					}
				}
			}
			return;
		}

		if (tool != null && checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(true);
		} else {
			if (!inOffHand) {
				if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
						if (!instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onSwapHand(PlayerSwapHandItemsEvent event) {
		if (!this.gsNightVisionTool || !this.gsTools)
			return;
		Player player = event.getPlayer();

		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		ItemStack tool = event.getMainHandItem();
		if (tool.getType() == Material.AIR) {
			tool = event.getOffHandItem();
			if (tool.getType() == Material.AIR) {
				if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
						if (!instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						}
					}
				}
				return;
			}
		}

		if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(true);
		} else {
			if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
					if (!instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (!this.gsNightVisionTool || !this.gsTools)
			return;

		if (event.getEntity() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;

			ItemStack tool = event.getItem().getItemStack();
			instance.getScheduler().runTaskSyncLater(() -> {
				boolean holdingItem;
				if (player.getInventory().getItemInMainHand().equals(tool)) {
					holdingItem = true;
				} else if (player.getInventory().getItemInOffHand().equals(tool)) {
					holdingItem = true;
				} else {
					holdingItem = false;
				}

				if (holdingItem) {
					if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
						instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(true);
					} else {
						if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
							if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
								instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
								if (!instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
									player.removePotionEffect(PotionEffectType.NIGHT_VISION);
								}
							}
						}
					}
				}
			}, player.getLocation(), 1, TimeUnit.MILLISECONDS);
		}
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (!this.gsNightVisionTool || !this.gsTools)
			return;

		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		ItemStack tool = event.getItemDrop().getItemStack();
		if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
			if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
					if (!instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					}
				}
			}
		}
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (!this.gsNightVisionTool || !this.gsTools)
			return;

		if (event.getResult() != Result.ALLOW) {
			return;
		}

		if (event.getWhoClicked() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;
			if (event.getClickedInventory() != null
					&& event.getClickedInventory().equals(player.getOpenInventory().getBottomInventory())) {
				if (event.getSlotType() == SlotType.QUICKBAR) {

					boolean rightClick = (event.isRightClick() && (event.getAction() == InventoryAction.PLACE_ONE
							|| event.getAction() == InventoryAction.SWAP_WITH_CURSOR));
					boolean leftClick = (event.isLeftClick() && (event.getAction() == InventoryAction.PLACE_ALL
							|| event.getAction() == InventoryAction.SWAP_WITH_CURSOR));
					boolean numPadInteraction = event.getClick() == ClickType.NUMBER_KEY
							&& event.getAction() == InventoryAction.HOTBAR_SWAP;

					ItemStack tool = (rightClick || leftClick ? event.getCursor()
							: event.getHotbarButton() != -1 && numPadInteraction ? event.getCurrentItem() : null);
					if (tool == null || tool.getType() == Material.AIR)
						return;

					if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
						instance.getScheduler().runTaskSyncLater(() -> {
							ItemStack inHand = player.getInventory().getItemInMainHand();
							if (inHand.getType() == Material.AIR) {
								inHand = player.getInventory().getItemInOffHand();
								if (inHand.getType() == Material.AIR) {
									return;
								}
							}

							if (checkNightVisionToolStatus(inHand) && getNightVisionToolStatus(inHand)) {
								this.clickCache.putIfAbsent(instance.getHellblockHandler().getActivePlayer(player),
										true);
							} else {
								this.clickCache.putIfAbsent(instance.getHellblockHandler().getActivePlayer(player),
										true);

							}
						}, player.getLocation(), 1, TimeUnit.MILLISECONDS);
					} else {
						this.clickCache.putIfAbsent(instance.getHellblockHandler().getActivePlayer(player), false);
					}
				} else if (event.getSlotType() == SlotType.CONTAINER) {

					boolean shiftClick = event.isShiftClick()
							&& event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
					boolean numPadInteraction = event.getClick() == ClickType.NUMBER_KEY
							&& event.getAction() == InventoryAction.HOTBAR_SWAP;

					ItemStack tool = (shiftClick ? event.getCurrentItem()
							: event.getHotbarButton() != -1 && numPadInteraction ? event.getCurrentItem() : null);
					if (tool == null || tool.getType() == Material.AIR) {
						return;
					}

					if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
						instance.getScheduler().runTaskSyncLater(() -> {
							ItemStack inHand = player.getInventory().getItemInMainHand();
							if (inHand.getType() == Material.AIR) {
								inHand = player.getInventory().getItemInOffHand();
								if (inHand.getType() == Material.AIR) {
									return;
								}
							}

							if (checkNightVisionToolStatus(inHand) && getNightVisionToolStatus(inHand)) {
								this.clickCache.putIfAbsent(instance.getHellblockHandler().getActivePlayer(player),
										true);
							} else {
								this.clickCache.putIfAbsent(instance.getHellblockHandler().getActivePlayer(player),
										true);

							}
						}, player.getLocation(), 1, TimeUnit.MILLISECONDS);
					} else {
						this.clickCache.putIfAbsent(instance.getHellblockHandler().getActivePlayer(player), false);
					}
				}
			}
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent event) {
		if (!this.gsNightVisionTool || !this.gsTools)
			return;

		if (event.getPlayer() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;

			if (event.getReason() == Reason.PLAYER) {

				ItemStack inHand = player.getInventory().getItemInMainHand();
				if (inHand.getType() == Material.AIR) {
					inHand = player.getInventory().getItemInOffHand();
					if (inHand.getType() == Material.AIR) {
						if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
							if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
								instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
								if (!instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
									player.removePotionEffect(PotionEffectType.NIGHT_VISION);
								}
							}
						}
						return;
					}
				}

				instance.getScheduler().runTaskSyncLater(() -> {
					if (this.clickCache.containsKey(instance.getHellblockHandler().getActivePlayer(player))) {
						boolean glowstoneEffect = this.clickCache
								.get(instance.getHellblockHandler().getActivePlayer(player)).booleanValue();
						if (glowstoneEffect) {
							player.addPotionEffect(
									new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
							instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(true);
						} else {
							if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
								if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
									instance.getHellblockHandler().getActivePlayer(player)
											.isHoldingGlowstoneTool(false);
									if (!instance.getHellblockHandler().getActivePlayer(player)
											.hasGlowstoneArmorEffect()) {
										player.removePotionEffect(PotionEffectType.NIGHT_VISION);
									}
								}
							}
						}
						this.clickCache.remove(instance.getHellblockHandler().getActivePlayer(player));
					}
				}, player.getLocation(), 1, TimeUnit.MILLISECONDS);
			} else {
				if (instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneToolEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						instance.getHellblockHandler().getActivePlayer(player).isHoldingGlowstoneTool(false);
						if (!instance.getHellblockHandler().getActivePlayer(player).hasGlowstoneArmorEffect()) {
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						}
					}
				}
			}
		}
	}

	public boolean checkToolData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellblockRecipe", "isNetherTool");
	}

	public boolean getToolData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).getOptional("HellblockRecipe", "isNetherTool").asBoolean();
	}

	public @Nullable ItemStack setToolData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellblockRecipe", "isNetherTool");
		});
	}

	public boolean checkNightVisionToolStatus(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellblockRecipe", "hasNightVision");
	}

	public boolean getNightVisionToolStatus(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).getOptional("HellblockRecipe", "hasNightVision").asBoolean();
	}

	public @Nullable ItemStack setNightVisionToolStatus(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellblockRecipe", "hasNightVision");
		});
	}

	public boolean isNetherToolEnabled(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		Material mat = item.getType();
		return ((this.nrTools && this.netherrackTools.contains(mat))
				|| (this.gsTools && this.glowstoneTools.contains(mat))
				|| (this.qzTools && this.quartzTools.contains(mat))
				|| (this.nsTools && this.netherstarTools.contains(mat)));
	}
}
