package com.swiftlicious.hellblock.creation.item;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface BuildableItem {

	default ItemStack build() {
		return build(null, new HashMap<>());
	}

	default ItemStack build(Player player) {
		return build(player, new HashMap<>());
	}

	ItemStack build(Player player, Map<String, String> placeholders);

	/**
	 * Whether the item would be removed from cache when reloading
	 */
	boolean persist();
}