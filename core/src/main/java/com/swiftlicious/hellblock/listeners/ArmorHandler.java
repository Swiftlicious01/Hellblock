package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
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
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ArmorHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	protected final Map<Material, ArmorData> armorMap = new HashMap<>();

	public ArmorHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		addArmor();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.armorMap.clear();
	}

	/**
	 * Registers custom Nether armor recipes from config.
	 */
	public void addArmor() {
		try {
			final Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> (item, context) -> {
				final Map<NamespacedKey, ShapedRecipe> recipes = registerNetherArmor(context,
						instance.getConfigManager().getMainConfig().getSection("armor"));
				recipes.entrySet().forEach(recipe -> {
					Bukkit.removeRecipe(recipe.getKey());
					if (recipe.getValue() != null) {
						Bukkit.addRecipe(recipe.getValue());
					}
				});
			};
			instance.getConfigManager().registerItemParser(f1, 6800, "armor");
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	/**
	 * Builds armor recipes from configuration.
	 */
	private Map<NamespacedKey, ShapedRecipe> registerNetherArmor(Context<Player> context, Section section) {
		final Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();

		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (!(entry.getValue() instanceof Section inner)) {
				continue;
			}

			for (ArmorType type : ArmorType.values()) {
				final Section typeSection = inner.getSection(type.toString().toLowerCase());
				if (typeSection == null) {
					continue;
				}

				final String materialName = typeSection.getString("material");
				if (materialName == null) {
					instance.getPluginLogger()
							.warn("Armor entry " + entry.getKey() + " (" + type + ") missing 'material'.");
					continue;
				}

				final Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
				if (material == null) {
					instance.getPluginLogger().warn("Invalid armor material: " + materialName);
					continue;
				}

				final boolean enabled = typeSection.getBoolean("enable", true);
				final boolean nightVision = typeSection.getBoolean("night-vision", false);
				final NamespacedKey key = new NamespacedKey(instance, materialName.toLowerCase());

				armorMap.putIfAbsent(material, new ArmorData(enabled, nightVision));

				if (!enabled) {
					recipes.putIfAbsent(key, null);
					continue;
				}

				// Build armor item
				final CustomItem item = new SingleItemParser(entry.getKey(), inner,
						instance.getConfigManager().getItemFormatFunctions()).getItem();
				ItemStack data = setArmorData(item.build(context), true);
				data = setNightVisionArmorStatus(data, nightVision);

				// Build recipe
				final ShapedRecipe recipe = new ShapedRecipe(key, data);
				recipe.setCategory(CraftingBookCategory.EQUIPMENT);

				final String[] shape = typeSection.getStringList("crafting.recipe").toArray(new String[0]);
				if (shape.length != 3) {
					instance.getPluginLogger()
							.warn("Recipe for " + entry.getKey() + " (" + type + ") must have 3 rows.");
					continue;
				}
				recipe.shape(shape);

				final Section craftingIngredients = typeSection.getSection("crafting.materials");
				if (craftingIngredients != null) {
					final Map<ItemStack, Character> craftingMaterials = instance.getConfigManager()
							.getCraftingMaterials(craftingIngredients);

					craftingMaterials.entrySet().forEach(ingredient -> recipe.setIngredient(ingredient.getValue(),
							new RecipeChoice.ExactChoice(ingredient.getKey())));
				}

				recipes.putIfAbsent(key, recipe);
			}
		}

		return recipes;
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (!(event.getView().getPlayer() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		handleRecipeDiscovery(player, event.getRecipe());
		final ItemStack result = event.getRecipe().getResult().clone();
		instance.getStorageManager().getOnlineUser(player.getUniqueId())
				.ifPresent(user -> instance.getChallengeManager().handleChallengeProgression(player, ActionType.CRAFT,
						result, result.getAmount()));
	}

	@EventHandler
	public void onLimitedCrafting(PrepareItemCraftEvent event) {
		if (!(event.getView().getPlayer() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (!player.getWorld().getGameRuleValue(GameRule.DO_LIMITED_CRAFTING)) {
			return;
		}

		handleRecipeDiscovery(player, event.getRecipe());
	}

	@EventHandler
	public void onArmorChange(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		handleArmorEffect(player, onlineUser.get());
	}

	@EventHandler
	public void onArmorEquip(PlayerInteractEvent event) {
		if (event.useItemInHand() == Result.DENY) {
			return;
		}
		if (!(event.getPlayer() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		handleArmorEffect(player, onlineUser.get());
	}

	@EventHandler
	public void onArmorBreak(PlayerItemBreakEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		handleArmorEffect(player, onlineUser.get());
	}

	@EventHandler
	public void onDispenseArmor(BlockDispenseArmorEvent event) {
		if (!(event.getTargetEntity() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		handleArmorEffect(player, onlineUser.get());
	}

	private void handleRecipeDiscovery(Player player, @Nullable Recipe recipe) {
		if (recipe == null) {
			return;
		}
		if (!(recipe instanceof CraftingRecipe craft)) {
			return;
		}

		final ItemStack result = recipe.getResult();

		if (!isNetherArmorEnabled(result)) {
			return;
		}
		if (!checkArmorData(result)) {
			return;
		}
		if (!getArmorData(result)) {
			return;
		}

		if (!player.hasDiscoveredRecipe(craft.getKey())) {
			player.discoverRecipe(craft.getKey());
		}
	}

	private void handleArmorEffect(Player player, UserData onlineUser) {
		if (hasValidGlowstoneArmor(player)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			onlineUser.isWearingGlowstoneArmor(true);
		} else {
			if (onlineUser.hasGlowstoneArmorEffect() && player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				onlineUser.isWearingGlowstoneArmor(false);
				if (!onlineUser.hasGlowstoneToolEffect()) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				}
			}
		}
	}

	private boolean hasValidGlowstoneArmor(Player player) {
		for (ItemStack item : player.getInventory().getArmorContents()) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (!isNetherArmorEnabled(item)) {
				continue;
			}
			if (!isNetherArmorNightVisionAllowed(item)) {
				continue;
			}
			if (checkNightVisionArmorStatus(item) && getNightVisionArmorStatus(item)) {
				return true;
			}
		}
		return false;
	}

	public boolean checkArmorData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).hasTag("HellblockRecipe", "isNetherArmor");
	}

	public boolean getArmorData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).getOptional("HellblockRecipe", "isNetherArmor").asBoolean();
	}

	public @Nullable ItemStack setArmorData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR) {
			return null;
		}

		final Consumer<RtagItem> rtag = tag -> tag.set(data, "HellblockRecipe", "isNetherArmor");
		return RtagItem.edit(item, rtag);
	}

	public boolean isNetherArmorEnabled(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return armorMap.containsKey(item.getType()) && armorMap.get(item.getType()).isEnabled();
	}

	public boolean checkNightVisionArmorStatus(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).hasTag("HellblockRecipe", "wearsNightVision");
	}

	public boolean getNightVisionArmorStatus(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).getOptional("HellblockRecipe", "wearsNightVision").asBoolean();
	}

	public @Nullable ItemStack setNightVisionArmorStatus(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR) {
			return null;
		}

		final Consumer<RtagItem> rtag = tag -> tag.set(data, "HellblockRecipe", "wearsNightVision");
		return RtagItem.edit(item, rtag);
	}

	public boolean isNetherArmorNightVisionAllowed(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return armorMap.containsKey(item.getType()) && armorMap.get(item.getType()).hasGlowstoneAbility();
	}

	protected class ArmorData {

		private final boolean enabled;
		private final boolean glowstone;

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