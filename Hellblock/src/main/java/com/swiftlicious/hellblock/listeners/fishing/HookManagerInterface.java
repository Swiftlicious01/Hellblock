package com.swiftlicious.hellblock.listeners.fishing;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface HookManagerInterface {

	/**
	 * Get the hook setting by its ID.
	 *
	 * @param id The ID of the hook setting to retrieve.
	 * @return The hook setting with the given ID, or null if not found.
	 */
	@Nullable
	HookSetting getHookSetting(String id);

	/**
	 * Decreases the durability of a fishing hook by a specified amount and
	 * optionally updates its lore. The hook would be removed if its durability is
	 * lower than 0
	 *
	 * @param rod        The fishing rod ItemStack to modify.
	 * @param amount     The amount by which to decrease the durability.
	 * @param updateLore Whether to update the lore of the fishing rod.
	 */
	void decreaseHookDurability(ItemStack rod, int amount, boolean updateLore);

	/**
	 * Increases the durability of a hook by a specified amount and optionally
	 * updates its lore.
	 *
	 * @param rod        The fishing rod ItemStack to modify.
	 * @param amount     The amount by which to increase the durability.
	 * @param updateLore Whether to update the lore of the fishing rod.
	 */
	void increaseHookDurability(ItemStack rod, int amount, boolean updateLore);

	/**
	 * Sets the durability of a fishing hook to a specific amount and optionally
	 * updates its lore.
	 *
	 * @param rod        The fishing rod ItemStack to modify.
	 * @param amount     The new durability value to set.
	 * @param updateLore Whether to update the lore of the fishing rod.
	 */
	void setHookDurability(ItemStack rod, int amount, boolean updateLore);

	/**
	 * Equips a fishing hook on a fishing rod.
	 *
	 * @param rod  The fishing rod ItemStack.
	 * @param hook The fishing hook ItemStack.
	 * @return True if the hook was successfully equipped, false otherwise.
	 */
	boolean equipHookOnRod(ItemStack rod, ItemStack hook);

	/**
	 * Removes the fishing hook from a fishing rod.
	 *
	 * @param rod The fishing rod ItemStack.
	 * @return The removed fishing hook ItemStack, or null if no hook was found.
	 */
	@Nullable
	ItemStack removeHookFromRod(ItemStack rod);
}