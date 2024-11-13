package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.event.inventory.PrepareItemCraftEvent;
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
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

public class NetherTools implements Listener {

	protected final HellblockPlugin instance;

	private final NamespacedKey netherrackPickaxeKey, netherrackAxeKey, netherrackShovelKey, netherrackHoeKey,
			netherrackSwordKey, glowstonePickaxeKey, glowstoneAxeKey, glowstoneShovelKey, glowstoneHoeKey,
			glowstoneSwordKey, quartzPickaxeKey, quartzAxeKey, quartzShovelKey, quartzHoeKey, quartzSwordKey,
			netherstarPickaxeKey, netherstarAxeKey, netherstarShovelKey, netherstarHoeKey, netherstarSwordKey;

	private final Set<Material> netherrackTools, glowstoneTools, quartzTools, netherstarTools;

	private final Map<UserData, Boolean> clickCache;

	public NetherTools(HellblockPlugin plugin) {
		this.instance = plugin;
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
			if (HBConfig.nrTools) {
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

			if (HBConfig.gsTools) {
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

			if (HBConfig.qzTools) {
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

			if (HBConfig.nsTools) {
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
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrPickaxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrPickaxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nrPickaxeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nrTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackPickaxeKey, data);
		recipe.shape(new String[] { "NNN", " B ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nrAxe() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_AXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrAxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrAxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nrAxeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nrTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackAxeKey, data);
		recipe.shape(new String[] { "NN ", "NB ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nrShovel() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_SHOVEL, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrShovelName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrShovelLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nrShovelEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nrTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackShovelKey, data);
		recipe.shape(new String[] { " N ", " B ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nrHoe() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_HOE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrHoeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrHoeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nrHoeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nrTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackHoeKey, data);
		recipe.shape(new String[] { "NN ", " B ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nrSword() {
		ItemBuilder tool = new ItemBuilder(Material.STONE_SWORD, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nrSwordName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nrSwordLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nrSwordEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nrTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherrackSwordKey, data);
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.NETHERRACK);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsPickaxe() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_PICKAXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsPickaxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsPickaxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.gsPickaxeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.gsTools);
		data = setNightVisionToolStatus(data, HBConfig.gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstonePickaxeKey, data);
		recipe.shape(new String[] { "NNN", " B ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsAxe() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_AXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsAxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsAxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.gsAxeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.gsTools);
		data = setNightVisionToolStatus(data, HBConfig.gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstoneAxeKey, data);
		recipe.shape(new String[] { "NN ", "NB ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsShovel() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_SHOVEL, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsShovelName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsShovelLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.gsShovelEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.gsTools);
		data = setNightVisionToolStatus(data, HBConfig.gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstoneShovelKey, data);
		recipe.shape(new String[] { " N ", " B ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsHoe() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_HOE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsHoeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsHoeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.gsHoeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.gsTools);
		data = setNightVisionToolStatus(data, HBConfig.gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstoneHoeKey, data);
		recipe.shape(new String[] { "NN ", " B ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe gsSword() {
		ItemBuilder tool = new ItemBuilder(Material.GOLDEN_SWORD, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.gsSwordName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.gsSwordLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.gsSwordEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.gsTools);
		data = setNightVisionToolStatus(data, HBConfig.gsNightVisionTool);

		ShapedRecipe recipe = new ShapedRecipe(this.glowstoneSwordKey, data);
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.GLOWSTONE);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzPickaxe() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_PICKAXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzPickaxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzPickaxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.qzPickaxeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.qzTools);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzPickaxeKey, data);
		recipe.shape(new String[] { "NNN", " B ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzAxe() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_AXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzAxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzAxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.qzAxeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.qzTools);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzAxeKey, data);
		recipe.shape(new String[] { "NN ", "NB ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzShovel() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_SHOVEL, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzShovelName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzShovelLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.qzShovelEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.qzTools);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzShovelKey, data);
		recipe.shape(new String[] { " N ", " B ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzHoe() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_HOE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzHoeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzHoeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.qzHoeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.qzTools);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzHoeKey, data);
		recipe.shape(new String[] { "NN ", " B ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe qzSword() {
		ItemBuilder tool = new ItemBuilder(Material.IRON_SWORD, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.qzSwordName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.qzSwordLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.qzSwordEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.qzTools);

		ShapedRecipe recipe = new ShapedRecipe(this.quartzSwordKey, data);
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.QUARTZ);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsPickaxe() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_PICKAXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsPickaxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsPickaxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nsPickaxeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nsTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarPickaxeKey, data);
		recipe.shape(new String[] { "NNN", " B ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsAxe() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_AXE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsAxeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsAxeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nsAxeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nsTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarAxeKey, data);
		recipe.shape(new String[] { "NN ", "NB ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsShovel() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_SHOVEL, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsShovelName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsShovelLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nsShovelEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nsTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarShovelKey, data);
		recipe.shape(new String[] { " N ", " B ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsHoe() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_HOE, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsHoeName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsHoeLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nsHoeEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nsTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarHoeKey, data);
		recipe.shape(new String[] { "NN ", " B ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
		return recipe;
	}

	public ShapedRecipe nsSword() {
		ItemBuilder tool = new ItemBuilder(Material.DIAMOND_SWORD, 1);

		tool.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(HBConfig.nsSwordName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : HBConfig.nsSwordLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		tool.setLore(lore);

		for (String enchants : HBConfig.nsSwordEnchants) {
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
			tool.addEnchantment(enchantment, level, false);
		}

		ItemStack data = setToolData(tool.get(), HBConfig.nsTools);

		ShapedRecipe recipe = new ShapedRecipe(this.netherstarSwordKey, data);
		recipe.shape(new String[] { " N ", " N ", " B " });
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('B', Material.BONE);
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
			if (isNetherToolEnabled(result)) {
				if (checkToolData(result) && getToolData(result)) {
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
				if (isNetherToolEnabled(result)) {
					if (checkToolData(result) && getToolData(result)) {
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
	public void onSlotChange(PlayerItemHeldEvent event) {
		if (!HBConfig.gsNightVisionTool || !HBConfig.gsTools)
			return;
		Player player = event.getPlayer();

		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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

		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;

		if (tool != null && tool.getType() == Material.AIR) {
			if (onlineUser.get().hasGlowstoneToolEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					onlineUser.get().isHoldingGlowstoneTool(false);
					if (!onlineUser.get().hasGlowstoneArmorEffect()) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					}
				}
			}
			return;
		}

		if (tool != null && checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			onlineUser.get().isHoldingGlowstoneTool(true);
		} else {
			if (!inOffHand) {
				if (onlineUser.get().hasGlowstoneToolEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						onlineUser.get().isHoldingGlowstoneTool(false);
						if (!onlineUser.get().hasGlowstoneArmorEffect()) {
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onSwapHand(PlayerSwapHandItemsEvent event) {
		if (!HBConfig.gsNightVisionTool || !HBConfig.gsTools)
			return;
		Player player = event.getPlayer();

		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;

		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;
		ItemStack tool = event.getMainHandItem();
		if (tool.getType() == Material.AIR) {
			tool = event.getOffHandItem();
			if (tool.getType() == Material.AIR) {
				if (onlineUser.get().hasGlowstoneToolEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						onlineUser.get().isHoldingGlowstoneTool(false);
						if (!onlineUser.get().hasGlowstoneArmorEffect()) {
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						}
					}
				}
				return;
			}
		}

		if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			onlineUser.get().isHoldingGlowstoneTool(true);
		} else {
			if (onlineUser.get().hasGlowstoneToolEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					onlineUser.get().isHoldingGlowstoneTool(false);
					if (!onlineUser.get().hasGlowstoneArmorEffect()) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (!HBConfig.gsNightVisionTool || !HBConfig.gsTools)
			return;

		if (event.getEntity() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
				return;

			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			ItemStack tool = event.getItem().getItemStack();
			instance.getScheduler().sync().runLater(() -> {
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
						onlineUser.get().isHoldingGlowstoneTool(true);
					} else {
						if (onlineUser.get().hasGlowstoneToolEffect()) {
							if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
								onlineUser.get().isHoldingGlowstoneTool(false);
								if (!onlineUser.get().hasGlowstoneArmorEffect()) {
									player.removePotionEffect(PotionEffectType.NIGHT_VISION);
								}
							}
						}
					}
				}
			}, 1, player.getLocation());
		}
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (!HBConfig.gsNightVisionTool || !HBConfig.gsTools)
			return;

		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;

		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;
		ItemStack tool = event.getItemDrop().getItemStack();
		if (checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
			if (onlineUser.get().hasGlowstoneToolEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					onlineUser.get().isHoldingGlowstoneTool(false);
					if (!onlineUser.get().hasGlowstoneArmorEffect()) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					}
				}
			}
		}
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (!HBConfig.gsNightVisionTool || !HBConfig.gsTools)
			return;

		if (event.getResult() != Result.ALLOW) {
			return;
		}

		if (event.getWhoClicked() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
				return;
			if (event.getClickedInventory() != null
					&& event.getClickedInventory().equals(player.getOpenInventory().getBottomInventory())) {
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (onlineUser.isEmpty())
					return;
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
						instance.getScheduler().sync().runLater(() -> {
							ItemStack inHand = player.getInventory().getItemInMainHand();
							if (inHand.getType() == Material.AIR) {
								inHand = player.getInventory().getItemInOffHand();
								if (inHand.getType() == Material.AIR) {
									return;
								}
							}

							if (checkNightVisionToolStatus(inHand) && getNightVisionToolStatus(inHand)) {
								this.clickCache.putIfAbsent(onlineUser.get(), true);
							} else {
								this.clickCache.putIfAbsent(onlineUser.get(), true);
							}
						}, 1, player.getLocation());
					} else {
						this.clickCache.putIfAbsent(onlineUser.get(), false);
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
						instance.getScheduler().sync().runLater(() -> {
							ItemStack inHand = player.getInventory().getItemInMainHand();
							if (inHand.getType() == Material.AIR) {
								inHand = player.getInventory().getItemInOffHand();
								if (inHand.getType() == Material.AIR) {
									return;
								}
							}

							if (checkNightVisionToolStatus(inHand) && getNightVisionToolStatus(inHand)) {
								this.clickCache.putIfAbsent(onlineUser.get(), true);
							} else {
								this.clickCache.putIfAbsent(onlineUser.get(), true);

							}
						}, 1, player.getLocation());
					} else {
						this.clickCache.putIfAbsent(onlineUser.get(), false);
					}
				}
			}
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent event) {
		if (!HBConfig.gsNightVisionTool || !HBConfig.gsTools)
			return;

		if (event.getPlayer() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
				return;

			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;

			if (event.getReason() == Reason.PLAYER) {

				ItemStack inHand = player.getInventory().getItemInMainHand();
				if (inHand.getType() == Material.AIR) {
					inHand = player.getInventory().getItemInOffHand();
					if (inHand.getType() == Material.AIR) {
						if (onlineUser.get().hasGlowstoneToolEffect()) {
							if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
								onlineUser.get().isHoldingGlowstoneTool(false);
								if (!onlineUser.get().hasGlowstoneArmorEffect()) {
									player.removePotionEffect(PotionEffectType.NIGHT_VISION);
								}
							}
						}
						return;
					}
				}

				instance.getScheduler().sync().runLater(() -> {
					if (this.clickCache.containsKey(onlineUser.get())) {
						boolean glowstoneEffect = this.clickCache.get(onlineUser.get()).booleanValue();
						if (glowstoneEffect) {
							player.addPotionEffect(
									new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
							onlineUser.get().isHoldingGlowstoneTool(true);
						} else {
							if (onlineUser.get().hasGlowstoneToolEffect()) {
								if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
									onlineUser.get().isHoldingGlowstoneTool(false);
									if (!onlineUser.get().hasGlowstoneArmorEffect()) {
										player.removePotionEffect(PotionEffectType.NIGHT_VISION);
									}
								}
							}
						}
						this.clickCache.remove(onlineUser.get());
					}
				}, 1, player.getLocation());
			} else {
				if (onlineUser.get().hasGlowstoneToolEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						onlineUser.get().isHoldingGlowstoneTool(false);
						if (!onlineUser.get().hasGlowstoneArmorEffect()) {
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
		return ((HBConfig.nrTools && this.netherrackTools.contains(mat))
				|| (HBConfig.gsTools && this.glowstoneTools.contains(mat))
				|| (HBConfig.qzTools && this.quartzTools.contains(mat))
				|| (HBConfig.nsTools && this.netherstarTools.contains(mat)));
	}
}
