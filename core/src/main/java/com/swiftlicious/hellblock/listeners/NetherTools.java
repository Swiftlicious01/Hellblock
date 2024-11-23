package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
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
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Key;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class NetherTools implements Listener {

	protected final HellblockPlugin instance;

	private final Set<Material> netherrackTools, glowstoneTools, quartzTools, netherstarTools;

	private final Map<UserData, Boolean> clickCache;

	public NetherTools(HellblockPlugin plugin) {
		this.instance = plugin;
		this.clickCache = new HashMap<>();
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
			for (Entry<NamespacedKey, ShapedRecipe> recipe : registerNetherTools(
					instance.getConfigManager().getMainConfig().getSection("tools")).entrySet()) {
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

	private Map<NamespacedKey, ShapedRecipe> registerNetherTools(Section section) {
		Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();
		for (MaterialType mat : MaterialType.values()) {
			Section toolSection = section.getSection(mat.toString().toLowerCase());
			if (toolSection.getBoolean("enable")) {
				for (Map.Entry<String, Object> entry : toolSection.getStringRouteMappedValues(false).entrySet()) {
					if (entry.getKey().equals("enable") || entry.getKey().equals("night-vision"))
						continue;
					String toolType = entry.getKey();
					Material material = Material
							.getMaterial(mat.getToolIdentifier().toUpperCase() + "_" + toolType.toUpperCase());
					if (material == null)
						continue;
					Item<ItemStack> tool = instance.getItemManager().wrap(new ItemStack(material, 1));

					if (entry.getValue() instanceof Section inner) {
						tool.displayName(instance.getAdventureManager().miniMessageToJson(inner.getString("name")));

						List<String> lore = new ArrayList<>();
						for (String newLore : inner.getStringList("lore")) {
							lore.add(instance.getAdventureManager().miniMessageToJson(newLore));
						}
						tool.lore(lore);

						for (Entry<String, Object> enchants : inner.getSection("enchantments")
								.getStringRouteMappedValues(false).entrySet()) {
							if (!StringUtils.isNumeric(enchants.getKey()))
								continue;
							if (enchants.getValue() instanceof Section enchantInner) {
								String enchant = enchantInner.getString("enchant");
								int level = enchantInner.getInt("level");
								tool.addEnchantment(Key.fromString(enchant), level);
							}
						}

						ItemStack data = setToolData(tool.getItem(), toolSection.getBoolean("enable"));
						if (mat.getMaterial() == Material.GLOWSTONE)
							data = setNightVisionToolStatus(data, toolSection.getBoolean("night-vision"));

						NamespacedKey key = new NamespacedKey(instance,
								mat.toString().toLowerCase() + toolType.toLowerCase());
						ShapedRecipe recipe = new ShapedRecipe(key, data);
						String[] recipeShape = new String[0];
						switch (toolType) {
						case "pickaxe":
							recipeShape = new String[] { "NNN", " B ", " B " };
						case "axe":
							recipeShape = new String[] { "NN ", "NB ", " B " };
						case "hoe":
							recipeShape = new String[] { "NN ", " B ", " B " };
						case "shovel":
							recipeShape = new String[] { " N ", " B ", " B " };
						case "sword":
							recipeShape = new String[] { " N ", " N ", " B " };
						default:
							break;
						}
						if (recipeShape.length != 0) {
							recipe.shape(recipeShape);
							recipe.setIngredient('N', mat.getMaterial());
							recipe.setIngredient('B', Material.BONE);
							recipes.putIfAbsent(key, recipe);
						}
					}
				}
			} else {
				for (Map.Entry<String, Object> entry : toolSection.getStringRouteMappedValues(false).entrySet()) {
					if (entry.getKey().equals("enable") || entry.getKey().equals("night-vision"))
						continue;
					String toolType = entry.getKey();
					NamespacedKey key = new NamespacedKey(instance,
							mat.toString().toLowerCase() + toolType.toLowerCase());
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
				if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName())) {
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
		if (!instance.getConfigManager().nightVisionTools() || !instance.getConfigManager().glowstoneTools())
			return;
		Player player = event.getPlayer();

		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		if (!instance.getConfigManager().nightVisionTools() || !instance.getConfigManager().glowstoneTools())
			return;
		Player player = event.getPlayer();

		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		if (!instance.getConfigManager().nightVisionTools() || !instance.getConfigManager().glowstoneTools())
			return;

		if (event.getEntity() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		if (!instance.getConfigManager().nightVisionTools() || !instance.getConfigManager().glowstoneTools())
			return;

		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		if (!instance.getConfigManager().nightVisionTools() || !instance.getConfigManager().glowstoneTools())
			return;

		if (event.getResult() != Result.ALLOW) {
			return;
		}

		if (event.getWhoClicked() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		if (!instance.getConfigManager().nightVisionTools() || !instance.getConfigManager().glowstoneTools())
			return;

		if (event.getPlayer() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		return ((instance.getConfigManager().netherrackTools() && this.netherrackTools.contains(mat))
				|| (instance.getConfigManager().glowstoneTools() && this.glowstoneTools.contains(mat))
				|| (instance.getConfigManager().quartzTools() && this.quartzTools.contains(mat))
				|| (instance.getConfigManager().netherstarTools() && this.netherstarTools.contains(mat)));
	}
}
