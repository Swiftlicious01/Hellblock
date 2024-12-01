package com.swiftlicious.hellblock.nms;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.hellblock.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;

public interface NMSHandler {

	/**
	 * Retrieve the fluid data from the defined location.
	 * 
	 * @param location the location to retrieve the fluid data from.
	 * @return the fluid data reference.
	 */
	abstract FluidData getFluidData(Location location);

	/**
	 * Retrieves the biome from the defined location.
	 * 
	 * @param location the location to retrieve the biome from.
	 * @return the biome that resides in the given location.
	 */
	abstract String getBiomeResourceLocation(Location location);

	/**
	 * Opens an custom inventory using packets.
	 * 
	 * @param player    the player to open an inventory for.
	 * @param inventory the inventory to open.
	 * @param jsonTitle the title of the inventory.
	 */
	abstract void openCustomInventory(Player player, Inventory inventory, String jsonTitle);

	/**
	 * Updates the inventory's title.
	 * 
	 * @param player    the player to update the title for.
	 * @param jsonTitle the title to change it to.
	 */
	abstract void updateInventoryTitle(Player player, String jsonTitle);

	/**
	 * Retrieves the fishing loot for the defined hook and rod used.
	 * 
	 * @param player the player fishing.
	 * @param hook   the hook that caught the loot.
	 * @param rod    the fishing rod used.
	 * @return the loot that was captured.
	 */
	abstract List<ItemStack> getFishingLoot(Player player, FishHook hook, ItemStack rod);

	/**
	 * Checks whether or not a fish hook is bit.
	 * 
	 * @param hook the hook to check if it is bit.
	 * @return whether or not the hook is bit.
	 */
	abstract boolean isFishingHookBit(FishHook hook);

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
	 * Swing a player's hand using packets.
	 * 
	 * @param player the player to swing their hand for.
	 * @param slot   the offhand or main hand to swing.
	 */
	abstract void swingHand(Player player, HandSlot slot);

	/**
	 * Perform a use item action for the given itemstack.
	 * 
	 * @param player    the player to perform the action for.
	 * @param handSlot  the hand slot the item is in.
	 * @param itemStack the item to perform the use action on.
	 */
	abstract void useItem(Player player, HandSlot handSlot, @Nullable ItemStack itemStack);

	/**
	 * Removes the entity using packets.
	 * 
	 * @param player    the player to show packet for.
	 * @param entityIDs an array of entities to remove.
	 */
	abstract void removeClientSideEntity(Player player, int... entityIDs);

	/**
	 * Teleports an entity using packets.
	 * 
	 * @param player    the player to show the packet to.
	 * @param location  the location the entity will teleport to.
	 * @param motion    the vector motion of the entity.
	 * @param onGround  whether or not the entity is on the ground.
	 * @param entityIDs an array of entities to teleport.
	 */
	public abstract void sendClientSideTeleportEntity(Player player, Location location, Vector motion, boolean onGround,
			int... entityIDs);

	default void sendClientSideTeleportEntity(Player player, Location location, boolean onGround, int... entityIDs) {
		this.sendClientSideTeleportEntity(player, location, new Vector(0, 0, 0), onGround, entityIDs);
	}

	/**
	 * Moves an entity using packets.
	 * 
	 * @param player    the player to show the packet to.
	 * @param vector    the vector amount to move the entity.
	 * @param entityIDs an array of entities to move.
	 */
	abstract void sendClientSideEntityMotion(Player player, Vector vector, int... entityIDs);

	/**
	 * Drops a fake packet item at the provided location.
	 * 
	 * @param player    the player to see the fake item.
	 * @param itemStack the itemstack to drop.
	 * @param location  the location to place it at.
	 * @return the id of the itemstack.
	 */
	abstract int dropFakeItem(Player player, ItemStack itemStack, Location location);

	/**
	 * Creates a fake armor stand.
	 * 
	 * @param location the location to create the fake armor stand.
	 * @return the fake armor stand instance.
	 */
	abstract FakeArmorStand createFakeArmorStand(Location location);

	/**
	 * Creates a fake item display.
	 * 
	 * @param location the location to create the fake item display.
	 * @return the fake item display instance.
	 */
	abstract FakeItemDisplay createFakeItemDisplay(Location location);

	/**
	 * Creates a fake text display.
	 * 
	 * @param location the location to create the fake text display.
	 * @return the fake text display instance.
	 */
	abstract FakeTextDisplay createFakeTextDisplay(Location location);

	/**
	 * Create a fake firework.
	 * 
	 * @param location the location to create the fake firework.
	 * @return the fake firework instance.
	 */
	abstract FakeFirework createFakeFirework(Location location);
}