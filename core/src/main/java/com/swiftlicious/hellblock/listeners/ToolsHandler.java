package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.event.inventory.InventoryType.SlotType;
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

	private final Map<UserData, Boolean> clickCache;

	protected final Map<Material, ToolData> toolMap;

	public ToolsHandler(HellblockPlugin plugin) {
		this.instance = plugin;
		this.clickCache = new HashMap<>();
		this.toolMap = new HashMap<>();
	}

	@Override
	public void load() {
		addTools();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		this.clickCache.clear();
		this.toolMap.clear();
	}

	public void addTools() {
		try {
			Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> {
				return (item, context) -> {
					for (Map.Entry<NamespacedKey, ShapedRecipe> recipe : registerNetherTools(context,
							instance.getConfigManager().getMainConfig().getSection("tools")).entrySet()) {
						if (recipe.getValue() != null) {
							Bukkit.removeRecipe(recipe.getKey());
							Bukkit.addRecipe(recipe.getValue());
						} else {
							Bukkit.removeRecipe(recipe.getKey());
						}
					}
				};
			};
			instance.getConfigManager().registerItemParser(f1, 6700, "tools");
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	private Map<NamespacedKey, ShapedRecipe> registerNetherTools(Context<Player> context, Section section) {
		Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section inner) {
				for (ToolType type : ToolType.values()) {
					if (inner.getSection(type.toString().toLowerCase()) == null)
						continue;
					boolean enabled = inner.getBoolean(type.toString().toLowerCase() + ".enable", true);
					boolean nightVision = inner.getBoolean(type.toString().toLowerCase() + ".night-vision", false);
					NamespacedKey key = new NamespacedKey(instance,
							inner.getString(type.toString().toLowerCase() + ".material").toLowerCase());
					toolMap.putIfAbsent(
							Material.getMaterial(inner.getString(type.toString().toLowerCase() + ".material")),
							new ToolData(enabled, nightVision));
					if (!enabled) {
						recipes.putIfAbsent(key, null);
						continue;
					}
					CustomItem item = new SingleItemParser(entry.getKey(), inner,
							instance.getConfigManager().getItemFormatFunctions()).getItem();

					ItemStack data = setToolData(item.build(context), enabled);
					data = setNightVisionToolStatus(data, nightVision);

					ShapedRecipe recipe = new ShapedRecipe(key, data);
					recipe.setCategory(CraftingBookCategory.EQUIPMENT);
					String[] shape = inner.getStringList(type.toString().toLowerCase() + ".crafting.recipe")
							.toArray(new String[0]);
					if (shape.length != 3) {
						instance.getPluginLogger().warn(String.format(
								"Recipe for tool item %s needs to include 3 rows for each different crafting slot.",
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
			if (isNetherToolEnabled(result)) {
				if (checkToolData(result) && getToolData(result)) {
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
				if (isNetherToolEnabled(result)) {
					if (checkToolData(result) && getToolData(result)) {
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
	public void onSlotChange(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();

		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		ItemStack tool = player.getInventory().getItem(event.getNewSlot());
		boolean inOffHand = false;
		if (isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool) && checkNightVisionToolStatus(tool)
				&& getNightVisionToolStatus(tool)) {
			inOffHand = false;
		} else {
			tool = player.getInventory().getItemInOffHand();
			if (isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool) && checkNightVisionToolStatus(tool)
					&& getNightVisionToolStatus(tool)) {
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

		if (tool != null && isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool)
				&& checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
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
		Player player = event.getPlayer();

		if (!instance.getHellblockHandler().isInCorrectWorld(player))
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

		if (isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool) && checkNightVisionToolStatus(tool)
				&& getNightVisionToolStatus(tool)) {
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
		if (event.getEntity() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
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
					if (isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool)
							&& checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
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
		Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;
		ItemStack tool = event.getItemDrop().getItemStack();
		if (isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool) && checkNightVisionToolStatus(tool)
				&& getNightVisionToolStatus(tool)) {
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
		if (event.getResult() != Result.ALLOW) {
			return;
		}

		if (event.getWhoClicked() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
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

					if (isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool)
							&& checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
						instance.getScheduler().sync().runLater(() -> {
							ItemStack inHand = player.getInventory().getItemInMainHand();
							if (inHand.getType() == Material.AIR) {
								inHand = player.getInventory().getItemInOffHand();
								if (inHand.getType() == Material.AIR) {
									return;
								}
							}

							if (isNetherToolEnabled(inHand) && isNetherToolNightVisionAllowed(inHand)
									&& checkNightVisionToolStatus(inHand) && getNightVisionToolStatus(inHand)) {
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

					if (isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool)
							&& checkNightVisionToolStatus(tool) && getNightVisionToolStatus(tool)) {
						instance.getScheduler().sync().runLater(() -> {
							ItemStack inHand = player.getInventory().getItemInMainHand();
							if (inHand.getType() == Material.AIR) {
								inHand = player.getInventory().getItemInOffHand();
								if (inHand.getType() == Material.AIR) {
									return;
								}
							}

							if (isNetherToolEnabled(inHand) && isNetherToolNightVisionAllowed(inHand)
									&& checkNightVisionToolStatus(inHand) && getNightVisionToolStatus(inHand)) {
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
		if (event.getPlayer() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				return;

			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;

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
					this.clickCache.remove(onlineUser.get());
				}
			}, 1, player.getLocation());
		}
	}

	@EventHandler
	public void onToolBreak(PlayerItemBreakEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty())
			return;
		ItemStack tool = event.getBrokenItem();
		if (isNetherToolEnabled(tool) && isNetherToolNightVisionAllowed(tool) && checkNightVisionToolStatus(tool)
				&& getNightVisionToolStatus(tool)) {
			if (onlineUser.get().hasGlowstoneToolEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					onlineUser.get().isWearingGlowstoneArmor(false);
					if (!onlineUser.get().hasGlowstoneArmorEffect()) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
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

	public boolean isNetherToolEnabled(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return toolMap.containsKey(item.getType()) && toolMap.get(item.getType()).isEnabled();
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

	public boolean isNetherToolNightVisionAllowed(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return toolMap.containsKey(item.getType()) && toolMap.get(item.getType()).hasGlowstoneAbility();
	}

	protected class ToolData {
		private boolean enabled;
		private boolean glowstone;

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