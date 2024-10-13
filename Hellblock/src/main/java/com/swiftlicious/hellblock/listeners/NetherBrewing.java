package com.swiftlicious.hellblock.listeners;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent.ChangeReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import com.swiftlicious.hellblock.HellblockPlugin;

public class NetherBrewing implements Listener {

	private final HellblockPlugin instance;

	private final NamespacedKey brewingKey;

	public NetherBrewing(HellblockPlugin plugin) {
		instance = plugin;
		brewingKey = new NamespacedKey(instance, "netherbottle");
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public ItemStack getNetherBottle() {
		ItemStack nbottle = new ItemStack(Material.POTION, 1);
		ItemMeta meta = nbottle.getItemMeta();
		meta.displayName(instance.getAdventureManager().getComponentFromMiniMessage("<red>Nether Bottle"));
		nbottle.setItemMeta(meta);
		return nbottle;
	}

	public void addRecipe() {
		ShapedRecipe recipe = new ShapedRecipe(brewingKey, new ItemStack(Material.POTION, 1));
		recipe.shape(new String[] { "GGG", "GBG", "GGG" });
		recipe.setIngredient('G', Material.GLOWSTONE_DUST);
		recipe.setIngredient('B', Material.POTION);
		Bukkit.getServer().addRecipe(recipe);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		if (player.getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE) {
			List<Block> scan = player.getLineOfSight((Set<Material>) null, 5);
			Iterator<Block> var4 = scan.iterator();

			while (true) {
				Block block;
				do {
					if (!var4.hasNext()) {
						return;
					}

					block = var4.next();
				} while (block.getType() != Material.LAVA);

				player.getInventory().remove(new ItemStack(Material.GLASS_BOTTLE, 1));
				player.getInventory().addItem(new ItemStack[] { this.getNetherBottle() });
				player.updateInventory();
			}
		}
	}

	@EventHandler
	public void onCauldronUpdate(CauldronLevelChangeEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
				return;
			}

			if (event.getReason() != ChangeReason.BOTTLE_FILL) {
				return;
			}

			if (event.getNewState().getBlockData() instanceof Levelled) {
				Levelled state = (Levelled) event.getNewState().getBlockData();
				if (state.getLevel() != state.getMinimumLevel()) {
					if (player.getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE) {
						player.getInventory().remove(new ItemStack(Material.GLASS_BOTTLE, 1));
						player.getInventory().addItem(new ItemStack[] { this.getNetherBottle() });
						player.updateInventory();
						state.setLevel(state.getLevel() - 1);
					}
				}
			}
		}
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}

		if (player.getInventory().getItemInMainHand().equals(this.getNetherBottle())) {
			player.setFireTicks(320);
		}
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
				return;
			}

			if (event.getClickedInventory().getType() == InventoryType.BREWING
					&& event.getCurrentItem().equals(this.getNetherBottle())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onCraft(PrepareItemCraftEvent event) {
		if (event.getRecipe().getResult().getType() == Material.POTION
				&& event.getView().getPlayer() instanceof Player) {
			Player player = (Player) event.getView().getPlayer();
			if (player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
				if (!event.getInventory().contains(this.getNetherBottle())) {
					event.getInventory().setResult(null);
				}
			} else {
				event.getInventory().setResult(null);
			}
		}
	}
}