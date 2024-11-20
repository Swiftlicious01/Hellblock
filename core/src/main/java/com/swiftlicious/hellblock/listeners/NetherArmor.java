package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

public class NetherArmor implements Listener {

	protected final HellblockPlugin instance;

	private final Set<Material> netherrackArmor, glowstoneArmor, quartzArmor, netherstarArmor;

	public NetherArmor(HellblockPlugin plugin) {
		instance = plugin;
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
			for (Entry<NamespacedKey, ShapedRecipe> recipe : registerNetherArmor(
					instance.getConfigManager().getMainConfig().getSection("armor")).entrySet()) {
				if (recipe.getValue() != null) {
					Bukkit.removeRecipe(recipe.getKey());
					Bukkit.addRecipe(recipe.getValue());
				} else {
					Bukkit.removeRecipe(recipe.getKey());
				}
			}
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	private Map<NamespacedKey, ShapedRecipe> registerNetherArmor(Section section) {
		Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();
		for (MaterialType mat : MaterialType.values()) {
			Section armorSection = section.getSection(mat.toString().toLowerCase());
			if (armorSection.getBoolean("enable")) {
				for (Map.Entry<String, Object> entry : armorSection.getStringRouteMappedValues(false).entrySet()) {
					if (entry.getKey().equals("enable") || entry.getKey().equals("night-vision"))
						continue;
					String armorType = entry.getKey();
					ItemBuilder armor = new ItemBuilder(Material
							.getMaterial(mat.getArmorIdentifier().toUpperCase() + "_" + armorType.toUpperCase()), 1);
					if (armor.getMaterial() == null)
						continue;

					if (entry.getValue() instanceof Section inner) {
						armor.setDisplayName(new ShadedAdventureComponentWrapper(
								instance.getAdventureManager().getComponentFromMiniMessage(inner.getString("name"))));

						List<ComponentWrapper> lore = new ArrayList<>();
						for (String newLore : inner.getStringList("lore")) {
							lore.add(new ShadedAdventureComponentWrapper(
									instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
						}
						armor.setLore(lore);

						for (String enchants : inner.getStringList("enchantments")) {
							String[] split = enchants.split(":");
							Enchantment enchantment = instance.getHellblockHandler().getEnchantmentRegistry()
									.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()));
							int level = 1;
							try {
								level = Integer.parseInt(split[1]);
							} catch (NumberFormatException ex) {
								instance.getPluginLogger().severe(String.format("Invalid quantity: %s!", split[1]));
								continue;
							}
							armor.addEnchantment(enchantment, level, false);
						}

						ItemStack data = setArmorData(armor.get(), armorSection.getBoolean("enable"));
						if (mat.getMaterial() == Material.GLOWSTONE)
							data = setNightVisionArmorStatus(data, armorSection.getBoolean("night-vision"));

						NamespacedKey key = new NamespacedKey(instance,
								mat.toString().toLowerCase() + armorType.toLowerCase());
						ShapedRecipe recipe = new ShapedRecipe(key, data);
						String[] recipeShape = new String[0];
						switch (armorType) {
						case "helmet":
							recipeShape = new String[] { "NNN", "N N", "   " };
						case "chestplate":
							recipeShape = new String[] { "N N", "NNN", "NNN" };
						case "leggings":
							recipeShape = new String[] { "NNN", "N N", "N N" };
						case "boots":
							recipeShape = new String[] { "   ", "N N", "N N" };
						default:
							break;
						}
						if (recipeShape.length != 0) {
							recipe.shape(recipeShape);
							recipe.setIngredient('N', mat.getMaterial());
							recipes.putIfAbsent(key, recipe);
						}
					}
				}
			} else {
				for (Map.Entry<String, Object> entry : armorSection.getStringRouteMappedValues(false).entrySet()) {
					if (entry.getKey().equals("enable") || entry.getKey().equals("night-vision"))
						continue;
					String armorType = entry.getKey();
					NamespacedKey key = new NamespacedKey(instance,
							mat.toString().toLowerCase() + armorType.toLowerCase());
					recipes.putIfAbsent(key, null);
				}
			}
		}
		return recipes;
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (event.getView().getPlayer() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName())) {
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
				if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName())) {
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
		if (!instance.getConfigManager().nightVisionArmor() || !instance.getConfigManager().glowstoneArmor())
			return;

		Player player = event.getPlayer();

		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		if (!instance.getConfigManager().nightVisionArmor() || !instance.getConfigManager().glowstoneArmor())
			return;
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		return ((instance.getConfigManager().netherrackArmor() && this.netherrackArmor.contains(mat))
				|| (instance.getConfigManager().glowstoneArmor() && this.glowstoneArmor.contains(mat))
				|| (instance.getConfigManager().quartzArmor() && this.quartzArmor.contains(mat))
				|| (instance.getConfigManager().netherstarArmor() && this.netherstarArmor.contains(mat)));
	}
}
