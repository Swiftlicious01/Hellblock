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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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

public class ToolsHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final Map<UserData, Boolean> clickCache = new HashMap<>();

	protected final Map<Material, ToolData> toolMap = new HashMap<>();

	public ToolsHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		addTools();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.clickCache.clear();
		this.toolMap.clear();
	}

	/**
	 * Registers custom Nether tool recipes from config.
	 */
	public void addTools() {
		try {
			final Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> (item, context) -> {
				final Map<NamespacedKey, ShapedRecipe> recipes = registerNetherTools(context,
						instance.getConfigManager().getMainConfig().getSection("tools"));
				recipes.entrySet().forEach(recipe -> {
					Bukkit.removeRecipe(recipe.getKey());
					if (recipe.getValue() != null) {
						Bukkit.addRecipe(recipe.getValue());
					}
				});
			};
			instance.getConfigManager().registerItemParser(f1, 6800, "tools");
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	/**
	 * Builds tool recipes from configuration.
	 */
	private Map<NamespacedKey, ShapedRecipe> registerNetherTools(Context<Player> context, Section section) {
		final Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();

		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (!(entry.getValue() instanceof Section inner)) {
				continue;
			}

			for (ToolType type : ToolType.values()) {
				final Section typeSection = inner.getSection(type.toString().toLowerCase());
				if (typeSection == null) {
					continue;
				}

				final String materialName = typeSection.getString("material");
				if (materialName == null) {
					instance.getPluginLogger()
							.warn("Tool entry " + entry.getKey() + " (" + type + ") missing 'material'.");
					continue;
				}

				final Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
				if (material == null) {
					instance.getPluginLogger().warn("Invalid tool material: " + materialName);
					continue;
				}

				final boolean enabled = typeSection.getBoolean("enable", true);
				final boolean nightVision = typeSection.getBoolean("night-vision", false);
				final NamespacedKey key = new NamespacedKey(instance, materialName.toLowerCase());

				toolMap.putIfAbsent(material, new ToolData(enabled, nightVision));

				if (!enabled) {
					recipes.putIfAbsent(key, null);
					continue;
				}

				// Build tool item
				final CustomItem item = new SingleItemParser(entry.getKey(), inner,
						instance.getConfigManager().getItemFormatFunctions()).getItem();
				ItemStack data = setToolData(item.build(context), true);
				data = setNightVisionToolStatus(data, nightVision);

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
	public void onSlotChange(PlayerItemHeldEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		ItemStack tool = player.getInventory().getItem(event.getNewSlot());
		if (!hasValidGlowstoneTool(tool)) {
			tool = player.getInventory().getItemInOffHand();
		}

		applyToolEffect(player, onlineUser.get(), hasValidGlowstoneTool(tool));
	}

	@EventHandler
	public void onSwapHand(PlayerSwapHandItemsEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}
		ItemStack tool = event.getMainHandItem();
		if (!hasValidGlowstoneTool(tool)) {
			tool = event.getOffHandItem();
		}
		applyToolEffect(player, onlineUser.get(), hasValidGlowstoneTool(tool));
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}
		final ItemStack tool = event.getItem().getItemStack();
		instance.getScheduler().sync().runLater(() -> {
			final ItemStack main = player.getInventory().getItemInMainHand();
			final ItemStack off = player.getInventory().getItemInOffHand();

			final boolean holding = main.isSimilar(tool) || off.isSimilar(tool);

			if (holding) {
				applyToolEffect(player, onlineUser.get(), hasValidGlowstoneTool(tool));
			}
		}, 1, player.getLocation());
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}
		applyToolEffect(player, onlineUser.get(), false);
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}
		if (!(event.getSlotType() == InventoryType.SlotType.QUICKBAR
				|| event.getSlotType() == InventoryType.SlotType.CONTAINER)) {
			return;
		}
		final ItemStack tool = event.getCursor();
		if (tool.getType() != Material.AIR) {
			clickCache.put(onlineUser.get(), hasValidGlowstoneTool(tool));
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent event) {
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
		instance.getScheduler().sync().runLater(() -> {
			if (clickCache.containsKey(onlineUser.get())) {
				applyToolEffect(player, onlineUser.get(), clickCache.get(onlineUser.get()));
				clickCache.remove(onlineUser.get());
			}
		}, 1, player.getLocation());
	}

	@EventHandler
	public void onToolBreak(PlayerItemBreakEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}
		applyToolEffect(player, onlineUser.get(), false);
	}

	private void handleRecipeDiscovery(Player player, @Nullable Recipe recipe) {
		if (recipe == null) {
			return;
		}
		if (!(recipe instanceof CraftingRecipe craft)) {
			return;
		}

		final ItemStack result = recipe.getResult();

		if (!isNetherToolEnabled(result)) {
			return;
		}
		if (!checkToolData(result)) {
			return;
		}
		if (!getToolData(result)) {
			return;
		}

		if (!player.hasDiscoveredRecipe(craft.getKey())) {
			player.discoverRecipe(craft.getKey());
		}
	}

	private void applyToolEffect(Player player, UserData onlineUser, boolean enable) {
		if (enable) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			onlineUser.isHoldingGlowstoneTool(true);
		} else {
			if (onlineUser.hasGlowstoneToolEffect() && player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				onlineUser.isHoldingGlowstoneTool(false);
				if (!onlineUser.hasGlowstoneArmorEffect()) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				}
			}
		}
	}

	private boolean hasValidGlowstoneTool(@Nullable ItemStack item) {
		if (!isNetherToolEnabled(item)) {
			return false;
		}
		if (!isNetherToolNightVisionAllowed(item)) {
			return false;
		}
		if (checkNightVisionToolStatus(item) && getNightVisionToolStatus(item)) {
			return true;
		}
		return false;
	}

	public boolean checkToolData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).hasTag("HellblockRecipe", "isNetherTool");
	}

	public boolean getToolData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).getOptional("HellblockRecipe", "isNetherTool").asBoolean();
	}

	public @Nullable ItemStack setToolData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR) {
			return null;
		}

		final Consumer<RtagItem> rtag = tag -> tag.set(data, "HellblockRecipe", "isNetherTool");
		return RtagItem.edit(item, rtag);
	}

	public boolean isNetherToolEnabled(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return toolMap.containsKey(item.getType()) && toolMap.get(item.getType()).isEnabled();
	}

	public boolean checkNightVisionToolStatus(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).hasTag("HellblockRecipe", "hasNightVision");
	}

	public boolean getNightVisionToolStatus(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).getOptional("HellblockRecipe", "hasNightVision").asBoolean();
	}

	public @Nullable ItemStack setNightVisionToolStatus(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR) {
			return null;
		}

		final Consumer<RtagItem> rtag = tag -> tag.set(data, "HellblockRecipe", "hasNightVision");
		return RtagItem.edit(item, rtag);
	}

	public boolean isNetherToolNightVisionAllowed(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return toolMap.containsKey(item.getType()) && toolMap.get(item.getType()).hasGlowstoneAbility();
	}

	protected class ToolData {
		private final boolean enabled;
		private final boolean glowstone;

		public ToolData(boolean enabled, boolean glowstone) {
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

	protected enum ToolType {
		PICKAXE, AXE, SHOVEL, HOE, SWORD;
	}
}