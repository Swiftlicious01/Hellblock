package com.swiftlicious.hellblock.nms;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.hellblock.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;

public interface NMSHandler {

	/**
	 * Retrieves the fishing loot for the defined hook and rod used.
	 * 
	 * @param player the player fishing.
	 * @param hook   the hook that caught the loot.
	 * @param rod    the fishing rod used.
	 * @return the loot that was captured.
	 */
	List<ItemStack> getFishingLoot(Player player, FishHook hook, ItemStack rod);

	/**
	 * Checks whether or not a fish hook is bit.
	 * 
	 * @param hook the hook to check if it is bit.
	 * @return whether or not the hook is bit.
	 */
	boolean isFishingHookBit(FishHook hook);

	/**
	 * Sets the vanilla fishing wait time.
	 * 
	 * @param hook  the hook to set the wait time for.
	 * @param ticks the amount of ticks the hook needs to wait for.
	 */
	default void setWaitTime(FishHook hook, int ticks) {
		hook.setWaitTime(ticks);
	}

	/**
	 * Gets the vanilla fishing wait time.
	 * 
	 * @param hook the hook to get the wait time for.
	 * @return the amount of ticks before the wait time is over.
	 */
	default int getWaitTime(FishHook hook) {
		return hook.getWaitTime();
	}

	/**
	 * Get the enchantment map for this item.
	 * 
	 * @param item the item to retrieve the enchantment map from.
	 * @return the enchantment map for this item.
	 */
	default Map<String, Integer> itemEnchantmentsToMap(Object item) {
		return Map.of();
	}

	/**
	 * Perform a use item action for the given itemstack.
	 * 
	 * @param player    the player to perform the action for.
	 * @param handSlot  the hand slot the item is in.
	 * @param itemStack the item to perform the use action on.
	 */
	void useItem(Player player, HandSlot handSlot, @Nullable ItemStack itemStack);

	/**
	 * Creates a fake armor stand.
	 * 
	 * @param location the location to create the fake armor stand.
	 * @return the fake armor stand instance.
	 */
	public abstract FakeArmorStand createFakeArmorStand(Location location);

	/**
	 * Creates a fake item display.
	 * 
	 * @param location the location to create the fake item display.
	 * @return the fake item display instance.
	 */
	public abstract FakeItemDisplay createFakeItemDisplay(Location location);

	/**
	 * Creates a fake text display.
	 * 
	 * @param location the location to create the fake text display.
	 * @return the fake text display instance.
	 */
	public abstract FakeTextDisplay createFakeTextDisplay(Location location);
}