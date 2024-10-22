package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.PotionBuilder;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
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
		this.nBottle = instance.getConfig("config.yml").getBoolean("brewing.nether-bottle.enable");
		this.nBottleName = instance.getConfig("config.yml").getString("brewing.nether-bottle.potion.name");
		this.nBottleColor = instance.getConfig("config.yml").getString("brewing.nether-bottle.potion.color");
		this.nBottleLore = instance.getConfig("config.yml").getStringList("brewing.nether-bottle.potion.lore");
		addBrewing();
		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	public void addBrewing() {
		try {
			if (nBottle) {
				Bukkit.removeRecipe(brewingKey);
				Bukkit.addRecipe(netherBottle());
			} else {
				Bukkit.removeRecipe(brewingKey);
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
				instance.getAdventureManager().getComponentFromMiniMessage(nBottleName)));

		List<ComponentWrapper> lore = new ArrayList<>();
		for (String newLore : nBottleLore) {
			lore.add(new ShadedAdventureComponentWrapper(
					instance.getAdventureManager().getComponentFromMiniMessage(newLore)));
		}
		potion.setLore(lore);

		PotionBuilder pmeta = new PotionBuilder(potion.get());
		pmeta.setColor(getColor(nBottleColor.toUpperCase()));

		Map<String, Object> data = Map.of("HellblockRecipe", List.of(Map.of("isNetherBottle", nBottle)));
		RtagItem tagItem = new RtagItem(pmeta.get());
		tagItem.set(data);

		return tagItem.load();
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (!nBottle)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		if (player.getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE) {
			List<Block> scan = player.getLineOfSight(null, 5);
			Iterator<Block> iterator = scan.iterator();

			while (true) {
				Block block;
				do {
					if (!iterator.hasNext()) {
						return;
					}

					block = iterator.next();
				} while (block.getType() != Material.LAVA);

				player.getInventory().remove(new ItemStack(Material.GLASS_BOTTLE, 1));
				player.getInventory().addItem(new ItemStack[] { this.getPotionResult(1) });
				player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0F, 1.0F);
				player.updateInventory();
			}
		}
	}

	@EventHandler
	public void onCauldronUpdate(PlayerInteractEvent event) {
		if (!nBottle)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		if (player.getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE
				&& player.getInventory().getItemInMainHand().getAmount() >= 4) {
			Block clicked = event.getClickedBlock();
			if (clicked != null && clicked.getType() == Material.LAVA_CAULDRON) {
				player.getInventory().remove(new ItemStack(Material.GLASS_BOTTLE, 4));
				player.getInventory().addItem(new ItemStack[] { this.getPotionResult(4) });
				player.updateInventory();
				clicked.setType(Material.CAULDRON);
				clicked.getState().update();
				player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0F, 1.0F);
			}
		}
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent event) {
		if (!nBottle)
			return;
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		ItemStack potion = event.getItem();
		RtagItem tagItem = new RtagItem(potion);
		if (checkBrewingData(tagItem)) {
			player.setFireTicks(RandomUtils.generateRandomInt(100, 320));
		}
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (!nBottle)
			return;
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
				return;
			}
			Inventory clicked = event.getClickedInventory();
			if (clicked != null && clicked.getType() == InventoryType.BREWING) {
				ItemStack potion = event.getCurrentItem();
				if (potion != null) {
					RtagItem tagItem = new RtagItem(potion);
					if (checkBrewingData(tagItem)) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (!nBottle)
			return;
		if (!(event.getView().getPlayer() instanceof Player))
			return;
		Player player = (Player) event.getView().getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		Recipe recipe = event.getRecipe();
		ItemStack result = recipe.getResult();
		RtagItem tagItem = new RtagItem(result);
		if (checkBrewingData(tagItem)) {
			if (recipe instanceof CraftingRecipe) {
				CraftingRecipe craft = (CraftingRecipe) recipe;
				player.discoverRecipe(craft.getKey());
			}
		}
	}

	public boolean checkBrewingData(RtagItem tag) {
		if (tag == null || tag.get("HellblockRecipe", 0) == null)
			return false;

		boolean data = false;
		if (tag.get("HellblockRecipe", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellblockRecipe", 0);
			for (String key : map.keySet()) {
				if (key.equals("isNetherBottle")) {
					for (Object value : map.values()) {
						if ((byte) value == 1) {
							data = true;
						}
					}
				}
			}
		}
		return data;
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
}