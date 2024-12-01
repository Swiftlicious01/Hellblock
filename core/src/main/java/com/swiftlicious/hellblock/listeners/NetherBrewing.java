package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class NetherBrewing implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	protected final Map<Material, BrewingData> brewingMap;
	protected Item<ItemStack> netherPotion;

	public NetherBrewing(HellblockPlugin plugin) {
		this.instance = plugin;
		this.brewingMap = new HashMap<>();
	}

	@Override
	public void load() {
		addPotions();
		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	@Override
	public void unload() {
		this.netherPotion = null;
		this.brewingMap.clear();
	}

	public @Nullable Item<ItemStack> getNetherPotion() {
		return this.netherPotion;
	}

	public void addPotions() {
		try {
			Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> {
				return (item, context) -> {
					for (Entry<NamespacedKey, ShapedRecipe> recipe : registerNetherPotions(context,
							instance.getConfigManager().getMainConfig().getSection("potions")).entrySet()) {
						if (recipe.getValue() != null) {
							Bukkit.removeRecipe(recipe.getKey());
							Bukkit.addRecipe(recipe.getValue());
						} else {
							Bukkit.removeRecipe(recipe.getKey());
						}
					}
				};
			};
			instance.getConfigManager().registerItemParser(f1, 6600, "potions");
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	private Map<NamespacedKey, ShapedRecipe> registerNetherPotions(Context<Player> context, Section section) {
		Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section inner) {
				boolean enabled = inner.getBoolean("enable", true);
				NamespacedKey key = new NamespacedKey(instance, inner.getString("material").toLowerCase());
				brewingMap.putIfAbsent(Material.getMaterial(inner.getString("material")), new BrewingData(enabled));
				if (!enabled) {
					recipes.putIfAbsent(key, null);
					continue;
				}
				CustomItem item = new SingleItemParser(entry.getKey(), inner,
						instance.getConfigManager().getItemFormatFunctions()).getItem();

				ItemStack data = setBrewingData(item.build(context), enabled);

				this.netherPotion = instance.getItemManager().wrap(data);

				ShapedRecipe recipe = new ShapedRecipe(key, data);
				recipe.setCategory(CraftingBookCategory.MISC);
				String[] shape = inner.getStringList("crafting.recipe").toArray(new String[0]);
				if (shape.length != 3) {
					instance.getPluginLogger().warn(String.format(
							"Recipe for potion item %s needs to include 3 rows for each different crafting slot.",
							entry.getKey()));
					continue;
				}
				recipe.shape(shape);
				Section craftingIngredients = inner.getSection("crafting.materials");
				if (craftingIngredients != null) {
					Map<ItemStack, Character> craftingMaterials = instance.getConfigManager()
							.getCraftingMaterials(craftingIngredients);
					for (Map.Entry<ItemStack, Character> ingredient : craftingMaterials.entrySet()) {
						recipe.setIngredient(ingredient.getValue(), new RecipeChoice.ExactChoice(ingredient.getKey()));
					}
				}
				recipes.putIfAbsent(key, recipe);
			}
		}
		return recipes;
	}

	@EventHandler
	public void onLavaBottle(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
			return;
		}

		if (event.getItem() != null && event.getItem().getType() == Material.GLASS_BOTTLE) {
			BlockIterator iter = new BlockIterator(player, 5);
			Block lastBlock = iter.next();
			while (iter.hasNext()) {
				lastBlock = iter.next();
				if (lastBlock.getType() != Material.LAVA) {
					continue;
				}
				break;
			}
			if (lastBlock.getType() == Material.LAVA) {
				event.setUseItemInHand(Result.ALLOW);
				if (player.getGameMode() != GameMode.CREATIVE)
					event.getItem().setAmount(event.getItem().getAmount() > 0 ? event.getItem().getAmount() - 1 : 0);
				if (player.getInventory().firstEmpty() != -1) {
					PlayerUtils.giveItem(player, getNetherPotion().getItem(), 1);
				} else {
					PlayerUtils.dropItem(player, getNetherPotion().getItem(), false, true, false);
				}
				instance.getVersionManager().getNMSManager().swingHand(player,
						event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
				instance.getSenderFactory().getAudience(player)
						.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:item.bottle.fill"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				player.updateInventory();
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null
						|| !onlineUser.get().getHellblockData().hasHellblock())
					return;
				if (!onlineUser.get().getChallengeData().isChallengeActive(ChallengeType.NETHER_BREWING_CHALLENGE)
						&& !onlineUser.get().getChallengeData()
								.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
					onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
							ChallengeType.NETHER_BREWING_CHALLENGE);
				} else {
					onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
							ChallengeType.NETHER_BREWING_CHALLENGE, 1);
					if (onlineUser.get().getChallengeData()
							.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
						onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
								ChallengeType.NETHER_BREWING_CHALLENGE);
					}
				}
			}
		}
	}

	@EventHandler
	public void onCauldronUpdate(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		if (event.getItem() != null && event.getItem().getType() == Material.GLASS_BOTTLE
				&& (event.getItem().getAmount() >= 4
						|| player.getInventory().containsAtLeast(new ItemStack(Material.GLASS_BOTTLE), 4))) {
			Block clicked = event.getClickedBlock();
			if (clicked != null && clicked.getType() == Material.LAVA_CAULDRON) {
				event.setUseItemInHand(Result.ALLOW);
				if (player.getGameMode() != GameMode.CREATIVE) {
					if (event.getItem().getAmount() >= 4) {
						event.getItem()
								.setAmount(event.getItem().getAmount() >= 4 ? event.getItem().getAmount() - 4 : 0);
					} else {
						PlayerUtils.removeItems(player.getInventory(), Material.GLASS_BOTTLE, 4);
					}
				}
				if (player.getInventory().firstEmpty() != -1) {
					PlayerUtils.giveItem(player, getNetherPotion().getItem(), 4);
				} else {
					ItemStack potion = getNetherPotion().getItem();
					potion.setAmount(4);
					PlayerUtils.dropItem(player, potion, false, true, false);
				}
				instance.getVersionManager().getNMSManager().swingHand(player,
						event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
				instance.getSenderFactory().getAudience(player)
						.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:item.bottle.fill"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				player.updateInventory();
				clicked.setType(Material.CAULDRON);
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null
						|| !onlineUser.get().getHellblockData().hasHellblock())
					return;
				if (!onlineUser.get().getChallengeData().isChallengeActive(ChallengeType.NETHER_BREWING_CHALLENGE)
						&& !onlineUser.get().getChallengeData()
								.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
					onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
							ChallengeType.NETHER_BREWING_CHALLENGE);
				} else {
					onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
							ChallengeType.NETHER_BREWING_CHALLENGE, 4);
					if (onlineUser.get().getChallengeData()
							.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
						onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
								ChallengeType.NETHER_BREWING_CHALLENGE);
					}
				}
			}
		}
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		ItemStack potion = event.getItem();
		if (isNetherBrewingEnabled(potion)) {
			if (checkBrewingData(potion) && getBrewingData(potion)) {
				player.setFireTicks(RandomUtils.generateRandomInt(100, 320));
			}
		}
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				return;

			Inventory clicked = event.getClickedInventory();
			if (clicked != null && player.getOpenInventory().getTopInventory() instanceof BrewerInventory) {
				if (clicked.equals(player.getOpenInventory().getBottomInventory())) {
					ItemStack potion = event.getCurrentItem();
					if (potion != null) {
						if (isNetherBrewingEnabled(potion)) {
							if (checkBrewingData(potion) && getBrewingData(potion)) {
								event.setCancelled(true);
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (event.getView().getPlayer() instanceof Player player) {
			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				return;

			Recipe recipe = event.getRecipe();
			ItemStack result = recipe.getResult();
			if (isNetherBrewingEnabled(result)) {
				if (checkBrewingData(result) && getBrewingData(result)) {
					if (recipe instanceof CraftingRecipe craft) {
						if (!player.hasDiscoveredRecipe(craft.getKey()))
							player.discoverRecipe(craft.getKey());
					}
				}
			}
		}
	}

	@EventHandler
	public void onLimitedCrafting(PrepareItemCraftEvent event) {
		if (instance.getHellblockHandler().getHellblockWorld().getGameRuleValue(GameRule.DO_LIMITED_CRAFTING)) {
			if (event.getView().getPlayer() instanceof Player player) {
				if (!instance.getHellblockHandler().isInCorrectWorld(player))
					return;

				Recipe recipe = event.getRecipe();
				ItemStack result = recipe.getResult();
				if (isNetherBrewingEnabled(result)) {
					if (checkBrewingData(result) && getBrewingData(result)) {
						if (recipe instanceof CraftingRecipe craft) {
							if (!player.hasDiscoveredRecipe(craft.getKey()))
								player.discoverRecipe(craft.getKey());
						}
					}
				}
			}
		}
	}

	public boolean checkBrewingData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellblockRecipe", "isNetherBottle");
	}

	public boolean getBrewingData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).getOptional("HellblockRecipe", "isNetherBottle").asBoolean();
	}

	public @Nullable ItemStack setBrewingData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellblockRecipe", "isNetherBottle");
		});
	}

	public boolean isNetherBrewingEnabled(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return brewingMap.containsKey(item.getType()) && brewingMap.get(item.getType()).isEnabled();
	}

	protected class BrewingData {

		private boolean enabled;

		public BrewingData(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return this.enabled;
		}
	}
}