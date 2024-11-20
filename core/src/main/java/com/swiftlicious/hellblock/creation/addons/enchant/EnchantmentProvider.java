package com.swiftlicious.hellblock.creation.addons.enchant;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.creation.addons.ExternalProvider;
import com.swiftlicious.hellblock.utils.extras.Pair;

/**
 * The EnchantmentProvider interface defines methods to interact with external
 * enchantment systems, allowing retrieval of enchantments for specific items.
 * Implementations of this interface should provide the logic to fetch
 * enchantments and their respective levels for a given item.
 */
public interface EnchantmentProvider extends ExternalProvider {

	/**
	 * Get a list of enchantments with level for itemStack
	 *
	 * @param itemStack itemStack
	 * @return enchantment list
	 */
	List<Pair<String, Short>> getEnchants(@NotNull ItemStack itemStack);
}