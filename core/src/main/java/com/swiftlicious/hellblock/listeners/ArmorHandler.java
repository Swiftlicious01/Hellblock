package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ArmorHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	protected final Map<Material, ArmorData> armorMap;

	public ArmorHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.armorMap = new HashMap<>();
	}

	@Override
	public void load() {
		addArmor();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		this.armorMap.clear();
	}

	public void addArmor() {
		try {
			Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> {
				return (item, context) -> {
					for (Map.Entry<NamespacedKey, ShapedRecipe> recipe : registerNetherArmor(context,
							instance.getConfigManager().getMainConfig().getSection("armor")).entrySet()) {
						if (recipe.getValue() != null) {
							Bukkit.removeRecipe(recipe.getKey());
							Bukkit.addRecipe(recipe.getValue());
						} else {
							Bukkit.removeRecipe(recipe.getKey());
						}
					}
				};
			};
			instance.getConfigManager().registerItemParser(f1, 6800, "armor");
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	private Map<NamespacedKey, ShapedRecipe> registerNetherArmor(Context<Player> context, Section section) {
		Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section inner) {
				for (ArmorType type : ArmorType.values()) {
					if (inner.getSection(type.toString().toLowerCase()) == null)
						continue;
					boolean enabled = inner.getBoolean(type.toString().toLowerCase() + ".enable", true);
					boolean nightVision = inner.getBoolean(type.toString().toLowerCase() + ".night-vision", false);
					NamespacedKey key = new NamespacedKey(instance,
							inner.getString(type.toString().toLowerCase() + ".material").toLowerCase());
					armorMap.putIfAbsent(
							Material.getMaterial(inner.getString(type.toString().toLowerCase() + ".material")),
							new ArmorData(enabled, nightVision));
					if (!enabled) {
						recipes.putIfAbsent(key, null);
						continue;
					}
					CustomItem item = new SingleItemParser(entry.getKey(), inner,
							instance.getConfigManager().getItemFormatFunctions()).getItem();

					ItemStack data = setArmorData(item.build(context), enabled);
					data = setNightVisionArmorStatus(data, nightVision);

					ShapedRecipe recipe = new ShapedRecipe(key, data);
					recipe.setCategory(CraftingBookCategory.EQUIPMENT);
					String[] shape = inner.getStringList(type.toString().toLowerCase() + ".crafting.recipe")
							.toArray(new String[0]);
					if (shape.length != 3) {
						instance.getPluginLogger().warn(String.format(
								"Recipe for armor item %s needs to include 3 rows for each different crafting slot.",
								entry.getKey()));
						continue;
					}
					recipe.shape(shape);
					Section craftingIngredients = inner
							.getSection(type.toString().toLowerCase() + ".crafting.materials");
					if (craftingIngredients != null) {
						Map<ItemStack, Character> craftingMaterials = instance.getConfigManager()
								.getCraftingMaterials(craftingIngredients);
						for (Map.Entry<ItemStack, Character> ingredient : craftingMaterials.entrySet()) {
							recipe.setIngredient(ingredient.getValue(),
									new RecipeChoice.ExactChoice(ingredient.getKey()));
						}
					}
					recipes.putIfAbsent(key, recipe);
				}
			}
		}
		return recipes;
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (event.getView().getPlayer() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				return;

			Recipe recipe = event.getRecipe();
			ItemStack result = recipe.getResult();
			if (isNetherArmorEnabled(result)) {
				if (checkArmorData(result) && getArmorData(result)) {
					if (recipe instanceof CraftingRecipe craft) {
						if (!player.hasDiscoveredRecipe(craft.getKey())) {
							Optional<UserData> onlineUser = instance.getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null
									|| !onlineUser.get().getHellblockData().hasHellblock())
								return;
							if (!onlineUser.get().getChallengeData()
									.isChallengeActive(instance.getChallengeManager().getByActionType(ActionType.CRAFT))
									&& !onlineUser.get().getChallengeData().isChallengeCompleted(
											instance.getChallengeManager().getByActionType(ActionType.CRAFT))) {
								onlineUser.get().getChallengeData().beginChallengeProgression(
										onlineUser.get().getPlayer(),
										instance.getChallengeManager().getByActionType(ActionType.CRAFT));
							} else {
								onlineUser.get().getChallengeData().updateChallengeProgression(
										onlineUser.get().getPlayer(),
										instance.getChallengeManager().getByActionType(ActionType.CRAFT), 1);
								if (onlineUser.get().getChallengeData().isChallengeCompleted(
										instance.getChallengeManager().getByActionType(ActionType.CRAFT))) {
									onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
											instance.getChallengeManager().getByActionType(ActionType.CRAFT));
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
		if (event.getView().getPlayer() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				return;

			if (player.getWorld().getGameRuleValue(GameRule.DO_LIMITED_CRAFTING)) {
				Recipe recipe = event.getRecipe();
				ItemStack result = recipe.getResult();
				if (isNetherArmorEnabled(result)) {
					if (checkArmorData(result) && getArmorData(result)) {
						if (recipe instanceof CraftingRecipe craft) {
							if (!player.hasDiscoveredRecipe(craft.getKey())) {
								Optional<UserData> onlineUser = instance.getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null
										|| !onlineUser.get().getHellblockData().hasHellblock())
									return;
								if (!onlineUser.get().getChallengeData().isChallengeActive(
										instance.getChallengeManager().getByActionType(ActionType.CRAFT))
										&& !onlineUser.get().getChallengeData().isChallengeCompleted(
												instance.getChallengeManager().getByActionType(ActionType.CRAFT))) {
									onlineUser.get().getChallengeData().beginChallengeProgression(
											onlineUser.get().getPlayer(),
											instance.getChallengeManager().getByActionType(ActionType.CRAFT));
								} else {
									onlineUser.get().getChallengeData().updateChallengeProgression(
											onlineUser.get().getPlayer(),
											instance.getChallengeManager().getByActionType(ActionType.CRAFT), 1);
									if (onlineUser.get().getChallengeData().isChallengeCompleted(
											instance.getChallengeManager().getByActionType(ActionType.CRAFT))) {
										onlineUser.get().getChallengeData().completeChallenge(
												onlineUser.get().getPlayer(),
												instance.getChallengeManager().getByActionType(ActionType.CRAFT));
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
	public void onArmorChange(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				return;
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;

			ItemStack[] armorSet = player.getInventory().getArmorContents();
			boolean checkArmor = false;
			if (armorSet != null) {
				for (ItemStack item : armorSet) {
					if (item == null || item.getType() == Material.AIR)
						continue;
					if (!isNetherArmorEnabled(item))
						continue;
					if (!isNetherArmorNightVisionAllowed(item))
						continue;
					if (checkNightVisionArmorStatus(item) && getNightVisionArmorStatus(item)) {
						checkArmor = true;
						break;
					}
				}
			}
			if (checkArmor) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				onlineUser.get().isWearingGlowstoneArmor(true);
			} else {
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

	@EventHandler
	public void onArmorEquip(PlayerInteractEvent event) {
		if (event.useItemInHand() == Result.DENY)
			return;
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
			return;
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;
		final ItemStack armor = event.getItem();
		if (armor != null && armor.getType() != Material.AIR) {
			if (isNetherArmorEnabled(armor) && isNetherArmorNightVisionAllowed(armor)
					&& checkNightVisionArmorStatus(armor) && getNightVisionArmorStatus(armor)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				onlineUser.get().isWearingGlowstoneArmor(true);
			}

			ItemStack[] armorSet = player.getInventory().getArmorContents();
			boolean checkArmor = false;
			if (armorSet != null) {
				for (ItemStack item : armorSet) {
					if (item == null || item.getType() == Material.AIR)
						continue;
					if (!isNetherArmorEnabled(item))
						continue;
					if (!isNetherArmorNightVisionAllowed(item))
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
	public void onArmorBreak(PlayerItemBreakEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;
		ItemStack armor = event.getBrokenItem();
		if (isNetherArmorEnabled(armor) && isNetherArmorNightVisionAllowed(armor) && checkNightVisionArmorStatus(armor)
				&& getNightVisionArmorStatus(armor)) {
			ItemStack[] armorSet = player.getInventory().getArmorContents();
			boolean checkArmor = false;
			if (armorSet != null) {
				for (ItemStack item : armorSet) {
					if (item == null || item.getType() == Material.AIR)
						continue;
					if (!isNetherArmorEnabled(item))
						continue;
					if (!isNetherArmorNightVisionAllowed(item))
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
		if (event.getTargetEntity() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				return;
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			ItemStack armor = event.getItem();
			if (isNetherArmorEnabled(armor) && isNetherArmorNightVisionAllowed(armor)
					&& checkNightVisionArmorStatus(armor) && getNightVisionArmorStatus(armor)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
				onlineUser.get().isWearingGlowstoneArmor(true);
			} else {
				ItemStack[] armorSet = player.getInventory().getArmorContents();
				boolean checkArmor = false;
				if (armorSet != null) {
					for (ItemStack item : armorSet) {
						if (item == null || item.getType() == Material.AIR)
							continue;
						if (!isNetherArmorEnabled(item))
							continue;
						if (!isNetherArmorNightVisionAllowed(item))
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

	public boolean isNetherArmorEnabled(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return armorMap.containsKey(item.getType()) && armorMap.get(item.getType()).isEnabled();
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

	public boolean isNetherArmorNightVisionAllowed(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return armorMap.containsKey(item.getType()) && armorMap.get(item.getType()).hasGlowstoneAbility();
	}

	protected class ArmorData {

		private boolean enabled;
		private boolean glowstone;

		public ArmorData(boolean enabled, boolean glowstone) {
			this.enabled = enabled;
			this.glowstone = glowstone;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public boolean hasGlowstoneAbility() {
			return this.glowstone;
		}
	}

	protected enum ArmorType {
		HELMET, CHESTPLATE, LEGGINGS, BOOTS;
	}
}