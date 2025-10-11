package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.events.brewing.PlayerBrewEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class BrewingHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	protected final Map<Material, BrewingData> brewingMap = new HashMap<>();
	protected Item<ItemStack> netherPotion;

	public BrewingHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		addPotions();
		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.netherPotion = null;
		this.brewingMap.clear();
		this.brewingOwners.clear();
	}

	public @Nullable Item<ItemStack> getNetherPotion() {
		return this.netherPotion;
	}

	private Item<ItemStack> getWaterBottle() {
		// Create base potion item
		final ItemStack bottle = new ItemStack(Material.POTION);
		// Wrap with your item manager
		final Item<ItemStack> editedBottle = instance.getItemManager().wrap(bottle);
		// Add the potion_contents component for water
		editedBottle.setTag(Key.key("minecraft:potion_contents"), Map.of("potion", "minecraft:water"));
		return editedBottle;
	}

	public void addPotions() {
		try {
			final Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> (item, context) -> {
				final Map<NamespacedKey, ShapedRecipe> recipes = registerNetherPotions(context,
						instance.getConfigManager().getMainConfig().getSection("potions"));
				recipes.entrySet().forEach(entry -> {
					Bukkit.removeRecipe(entry.getKey());
					if (entry.getValue() != null) {
						Bukkit.addRecipe(entry.getValue());
					}
				});
			};
			instance.getConfigManager().registerItemParser(f1, 6600, "potions");
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	private Map<NamespacedKey, ShapedRecipe> registerNetherPotions(Context<Player> context, Section section) {
		final Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();

		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (!(entry.getValue() instanceof Section inner)) {
				continue;
			}

			final String materialName = inner.getString("material");
			if (materialName == null) {
				instance.getPluginLogger().warn("Potion entry " + entry.getKey() + " missing 'material'.");
				continue;
			}

			final Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
			if (material == null) {
				instance.getPluginLogger().warn("Invalid potion material: " + materialName);
				continue;
			}

			final boolean enabled = inner.getBoolean("enable", true);
			final NamespacedKey key = new NamespacedKey(instance, materialName.toLowerCase());

			brewingMap.putIfAbsent(material, new BrewingData(enabled));

			if (!enabled) {
				recipes.putIfAbsent(key, null);
				continue;
			}

			// Build potion item
			final CustomItem item = new SingleItemParser(entry.getKey(), inner,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			final ItemStack data = setBrewingData(item.build(context), true);

			this.netherPotion = instance.getItemManager().wrap(data); // overwrites each loop

			// Build recipe (this should craft a water bottle)
			final ShapedRecipe recipe = new ShapedRecipe(key, getWaterBottle().loadCopy());
			recipe.setCategory(CraftingBookCategory.MISC);

			final String[] shape = inner.getStringList("crafting.recipe").toArray(new String[0]);
			if (shape.length != 3) {
				instance.getPluginLogger().warn("Recipe for " + entry.getKey() + " must have 3 rows.");
				continue;
			}
			recipe.shape(shape);

			final Section craftingIngredients = inner.getSection("crafting.materials");
			if (craftingIngredients != null) {
				final Map<ItemStack, Character> craftingMaterials = instance.getConfigManager()
						.getCraftingMaterials(craftingIngredients);

				craftingMaterials.entrySet().forEach(ingredient -> recipe.setIngredient(ingredient.getValue(),
						new RecipeChoice.ExactChoice(ingredient.getKey())));
			}

			recipes.putIfAbsent(key, recipe);
		}
		return recipes;
	}

	private final Map<Location, UUID> brewingOwners = new HashMap<>();

	@EventHandler
	public void onBrewIngredientPlace(InventoryClickEvent event) {
		if (!(event.getInventory() instanceof BrewerInventory brewerInv)) {
			return;
		}

		// Slot 3 = ingredient slot
		if (event.getSlot() == 3 && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
			final Location loc = brewerInv.getLocation();

			// If no owner exists, or previous owner was cleared, assign new owner
			brewingOwners.putIfAbsent(loc, event.getWhoClicked().getUniqueId());
		}
	}

	@EventHandler
	public void onPotionTake(InventoryClickEvent event) {
		if (!(event.getInventory() instanceof BrewerInventory brewerInv)) {
			return;
		}

		// Potion slots are 0, 1, 2
		if (event.getSlot() >= 0 && event.getSlot() <= 2 && event.getCurrentItem() != null) {
			final UUID owner = brewingOwners.get(brewerInv.getLocation());
			if (owner == null) {
				return;
			}

			if (owner.equals(event.getWhoClicked().getUniqueId())) {
				final Player player = (Player) event.getWhoClicked();

				final PlayerBrewEvent playerBrewEvent = new PlayerBrewEvent(player, brewerInv.getLocation().getBlock(),
						brewerInv);

				Bukkit.getPluginManager().callEvent(playerBrewEvent);

				if (playerBrewEvent.isCancelled()) {
					event.setCancelled(true);
				}

				// Cleanup after successful claim
				brewingOwners.remove(brewerInv.getLocation());
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		final UUID uuid = event.getPlayer().getUniqueId();
		brewingOwners.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
	}

	@EventHandler
	public void onBrewingStandBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.BREWING_STAND) {
			brewingOwners.remove(event.getBlock().getLocation());
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		Arrays.stream(event.getChunk().getTileEntities()).filter(te -> te.getType() == Material.BREWING_STAND)
				.forEach(te -> brewingOwners.remove(te.getLocation()));
	}

	@EventHandler
	public void onPlayerBrew(PlayerBrewEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		final List<ItemStack> potions = event.getResultPotions();

		for (ItemStack pot : potions) {
			if (pot == null || pot.getType() == Material.AIR) {
				continue;
			}

			instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(
					user -> instance.getChallengeManager().handleChallengeProgression(player, ActionType.BREW, pot));
		}
	}

	@EventHandler
	public void onLavaBottle(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
			return;
		}

		final ItemStack bottle = event.getItem();
		if (bottle == null || bottle.getType() != Material.GLASS_BOTTLE) {
			return;
		}

		final Block lavaBlock = findLavaInRange(player, 5);
		if (lavaBlock == null || lavaBlock.getType() != Material.LAVA) {
			return;
		}

		event.setUseItemInHand(Result.ALLOW);

		if (player.getGameMode() != GameMode.CREATIVE) {
			bottle.setAmount(bottle.getAmount() - 1);
		}

		giveNetherPotion(player, 1, event.getHand());
	}

	@EventHandler
	public void onCauldronUpdate(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final ItemStack bottle = event.getItem();
		if (bottle == null || bottle.getType() != Material.GLASS_BOTTLE) {
			return;
		}
		if (bottle.getAmount() < 4 && !player.getInventory().containsAtLeast(new ItemStack(Material.GLASS_BOTTLE), 4)) {
			return;
		}

		final Block clicked = event.getClickedBlock();
		if (clicked == null || clicked.getType() != Material.LAVA_CAULDRON) {
			return;
		}

		event.setUseItemInHand(Result.ALLOW);

		if (player.getGameMode() != GameMode.CREATIVE) {
			if (bottle.getAmount() >= 4) {
				bottle.setAmount(bottle.getAmount() - 4);
			} else {
				PlayerUtils.removeItems(player.getInventory(), Material.GLASS_BOTTLE, 4);
			}
		}

		giveNetherPotion(player, 4, event.getHand());
		clicked.setType(Material.CAULDRON);
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final ItemStack potion = event.getItem();
		if (!(isNetherBrewingEnabled(potion) && checkBrewingData(potion) && getBrewingData(potion))) {
			return;
		}

		player.setFireTicks(RandomUtils.generateRandomInt(100, 320));
	}

	private Block findLavaInRange(Player player, int range) {
		final BlockIterator iter = new BlockIterator(player, range);
		while (iter.hasNext()) {
			final Block block = iter.next();
			if (block.getType() == Material.LAVA) {
				return block;
			}
		}
		return null;
	}

	private void giveNetherPotion(Player player, int amount, EquipmentSlot hand) {
		final ItemStack potion = getNetherPotion().getItem().clone();
		potion.setAmount(amount);

		if (player.getInventory().firstEmpty() != -1) {
			PlayerUtils.giveItem(player, potion, amount);
		} else {
			PlayerUtils.dropItem(player, potion, false, true, false);
		}

		VersionHelper.getNMSManager().swingHand(player, hand == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
		AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
				Sound.sound(Key.key("minecraft:item.bottle.fill"), Source.PLAYER, 1, 1));
		player.updateInventory();
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Inventory clicked = event.getClickedInventory();
		if (clicked == null) {
			return;
		}
		if (!(player.getOpenInventory().getTopInventory() instanceof BrewerInventory)) {
			return;
		}
		if (!clicked.equals(player.getOpenInventory().getBottomInventory())) {
			return;
		}

		final ItemStack potion = event.getCurrentItem();
		if (potion == null) {
			return;
		}

		if (isNetherBrewingEnabled(potion) && checkBrewingData(potion) && getBrewingData(potion)) {
			event.setCancelled(true);
		}
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

	/**
	 * Shared logic for discovering special brewing recipes.
	 */
	private void handleRecipeDiscovery(Player player, @Nullable Recipe recipe) {
		if (recipe == null) {
			return;
		}
		if (!(recipe instanceof CraftingRecipe craft)) {
			return;
		}

		final ItemStack result = recipe.getResult();

		if (!isNetherBrewingEnabled(result)) {
			return;
		}
		if (!checkBrewingData(result)) {
			return;
		}
		if (!getBrewingData(result)) {
			return;
		}

		if (!player.hasDiscoveredRecipe(craft.getKey())) {
			player.discoverRecipe(craft.getKey());
		}
	}

	public boolean checkBrewingData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).hasTag("HellblockRecipe", "isNetherBottle");
	}

	public boolean getBrewingData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return new RtagItem(item).getOptional("HellblockRecipe", "isNetherBottle").asBoolean();
	}

	public @Nullable ItemStack setBrewingData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR) {
			return null;
		}

		final Consumer<RtagItem> rtag = tag -> tag.set(data, "HellblockRecipe", "isNetherBottle");
		return RtagItem.edit(item, rtag);
	}

	public boolean isNetherBrewingEnabled(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return brewingMap.containsKey(item.getType()) && brewingMap.get(item.getType()).isEnabled();
	}

	protected class BrewingData {

		private final boolean enabled;

		public BrewingData(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return this.enabled;
		}
	}
}