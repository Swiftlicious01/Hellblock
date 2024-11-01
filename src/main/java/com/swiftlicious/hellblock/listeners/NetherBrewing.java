package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.Nullable;

import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.PotionBuilder;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

public class NetherBrewing implements Listener {

	private final HellblockPlugin instance;

	private final NamespacedKey brewingKey;

	public boolean nBottle;
	public String nBottleName;
	public List<String> nBottleLore;
	public String nBottleColor;

	public NetherBrewing(HellblockPlugin plugin) {
		this.instance = plugin;
		this.brewingKey = new NamespacedKey(this.instance, "netherbottle");
		this.nBottle = instance.getConfig("config.yml").getBoolean("brewing.nether-bottle.enable", true);
		this.nBottleName = instance.getConfig("config.yml").getString("brewing.nether-bottle.potion.name");
		this.nBottleColor = instance.getConfig("config.yml").getString("brewing.nether-bottle.potion.color", "RED");
		this.nBottleLore = instance.getConfig("config.yml").getStringList("brewing.nether-bottle.potion.lore");
		addBrewing();
		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	public void addBrewing() {
		try {
			if (this.nBottle) {
				Bukkit.removeRecipe(this.brewingKey);
				Bukkit.addRecipe(netherBottle());
			} else {
				Bukkit.removeRecipe(this.brewingKey);
			}
		} catch (IllegalStateException ignored) {
			// ignored
		}
	}

	public ShapedRecipe netherBottle() {
		ItemStack bottle = new ItemStack(Material.POTION, 1);
		ItemMeta potionMeta = bottle.getItemMeta();
		PotionMeta pmeta = (PotionMeta) potionMeta;
		pmeta.setBasePotionType(PotionType.WATER);
		bottle.setItemMeta(pmeta);

		ShapedRecipe recipe = new ShapedRecipe(brewingKey, getPotionResult(1));
		recipe.shape(new String[] { "GGG", "GBG", "GGG" });
		recipe.setIngredient('G', Material.GLOWSTONE_DUST);
		recipe.setIngredient('B', bottle);
		return recipe;
	}

	public ItemStack getPotionResult(int amount) {
		ItemBuilder potion = new ItemBuilder(Material.POTION, amount);

		potion.setDisplayName(new ShadedAdventureComponentWrapper(
				instance.getAdventureManager().getComponentFromMiniMessage(this.nBottleName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : this.nBottleLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		potion.setLore(lore);

		PotionBuilder pmeta = new PotionBuilder(potion.get());
		pmeta.setColor(getColor(this.nBottleColor.toUpperCase()));

		ItemStack data = setBrewingData(pmeta.get(), this.nBottle);

		return data;
	}

	@EventHandler
	public void onLavaBottle(PlayerInteractEvent event) {
		if (!this.nBottle)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

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
				event.getItem().setAmount(event.getItem().getAmount() > 0 ? event.getItem().getAmount() - 1 : 0);
				if (player.getInventory().firstEmpty() != -1) {
					player.getInventory().addItem(this.getPotionResult(1));
				} else {
					lastBlock.getWorld().dropItemNaturally(player.getLocation(), this.getPotionResult(1));
				}
				player.swingMainHand();
				instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:item.bottle.fill"), 1, 1);
				player.updateInventory();
				HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
				if (!pi.isChallengeActive(ChallengeType.NETHER_BREWING_CHALLENGE)
						&& !pi.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
					pi.beginChallengeProgression(ChallengeType.NETHER_BREWING_CHALLENGE);
				} else {
					pi.updateChallengeProgression(ChallengeType.NETHER_BREWING_CHALLENGE, 1);
					if (pi.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
						pi.completeChallenge(ChallengeType.NETHER_BREWING_CHALLENGE);
					}
				}
			}
		}
	}

	@EventHandler
	public void onCauldronUpdate(PlayerInteractEvent event) {
		if (!this.nBottle)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		if (event.getItem() != null && event.getItem().getType() == Material.GLASS_BOTTLE
				&& (event.getItem().getAmount() >= 4
						|| player.getInventory().containsAtLeast(new ItemStack(Material.GLASS_BOTTLE), 4))) {
			Block clicked = event.getClickedBlock();
			if (clicked != null && clicked.getType() == Material.LAVA_CAULDRON) {
				if (event.getItem().getAmount() >= 4) {
					event.getItem().setAmount(event.getItem().getAmount() >= 4 ? event.getItem().getAmount() - 4 : 0);
				} else {
					removeItems(player.getInventory(), Material.GLASS_BOTTLE, 4);
				}
				if (player.getInventory().firstEmpty() != -1) {
					player.getInventory().addItem(this.getPotionResult(4));
				} else {
					clicked.getWorld().dropItemNaturally(player.getLocation(), this.getPotionResult(4));
				}
				player.swingMainHand();
				instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:item.bottle.fill"), 1, 1);
				player.updateInventory();
				clicked.setType(Material.CAULDRON);
				HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
				if (!pi.isChallengeActive(ChallengeType.NETHER_BREWING_CHALLENGE)
						&& !pi.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
					pi.beginChallengeProgression(ChallengeType.NETHER_BREWING_CHALLENGE);
				} else {
					pi.updateChallengeProgression(ChallengeType.NETHER_BREWING_CHALLENGE, 4);
					if (pi.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
						pi.completeChallenge(ChallengeType.NETHER_BREWING_CHALLENGE);
					}
				}
			}
		}
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent event) {
		if (!this.nBottle)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		ItemStack potion = event.getItem();
		if (checkBrewingData(potion) && getBrewingData(potion)) {
			player.setFireTicks(RandomUtils.generateRandomInt(100, 320));
		}
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (!this.nBottle)
			return;
		if (event.getWhoClicked() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
				return;
			}
			Inventory clicked = event.getClickedInventory();
			if (clicked != null && clicked instanceof BrewerInventory inventory) {
				if (inventory.equals(player.getOpenInventory().getTopInventory())) {
					ItemStack potion = event.getCurrentItem();
					if (potion != null) {
						if (checkBrewingData(potion) && getBrewingData(potion)) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (!this.nBottle)
			return;
		if (event.getView().getPlayer() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
				return;
			}

			Recipe recipe = event.getRecipe();
			ItemStack result = recipe.getResult();
			if (checkBrewingData(result) && getBrewingData(result)) {
				if (recipe instanceof CraftingRecipe craft) {
					player.discoverRecipe(craft.getKey());
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

	public Color getColor(String colorName) {
		switch (colorName) {
		case "AQUA":
			return Color.AQUA;
		case "BLACK":
			return Color.BLACK;
		case "BLUE":
			return Color.BLUE;
		case "FUCHSIA":
			return Color.FUCHSIA;
		case "GRAY":
			return Color.GRAY;
		case "GREEN":
			return Color.GREEN;
		case "LIME":
			return Color.LIME;
		case "MAROON":
			return Color.MAROON;
		case "NAVY":
			return Color.NAVY;
		case "OLIVE":
			return Color.OLIVE;
		case "ORANGE":
			return Color.ORANGE;
		case "PURPLE":
			return Color.PURPLE;
		case "RED":
			return Color.RED;
		case "SILVER":
			return Color.SILVER;
		case "TEAL":
			return Color.TEAL;
		case "YELLOW":
			return Color.YELLOW;
		default:
			return Color.WHITE;
		}
	}

	/**
	 * Removes the items of type from an inventory.
	 * 
	 * @param inventory Inventory to modify
	 * @param type      The type of Material to remove
	 * @param amount    The amount to remove, or {@link Integer.MAX_VALUE} to remove
	 *                  all
	 * @return The amount of items that could not be removed, 0 for success, or -1
	 *         for failures
	 */
	private int removeItems(Inventory inventory, Material type, int amount) {

		if (type == null || inventory == null)
			return -1;
		if (amount <= 0)
			return -1;

		if (amount == Integer.MAX_VALUE) {
			inventory.remove(type);
			return 0;
		}

		HashMap<Integer, ItemStack> retVal = inventory.removeItem(new ItemStack(type, amount));

		int notRemoved = 0;
		for (ItemStack item : retVal.values()) {
			notRemoved += item.getAmount();
		}
		return notRemoved;
	}
}