package com.swiftlicious.hellblock.creation.addons;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public interface EnchantmentInterface {

	/**
	 * Get a list of enchantments with level for itemStack format:
	 * plugin:enchantment:level example: minecraft:sharpness:5
	 *
	 * @param itemStack itemStack
	 * @return enchantment list
	 */
	List<String> getEnchants(ItemStack itemStack);
}