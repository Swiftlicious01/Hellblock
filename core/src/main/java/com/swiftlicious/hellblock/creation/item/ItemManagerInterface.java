package com.swiftlicious.hellblock.creation.item;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.Context;

/**
 * Interface for managing custom items
 */
public interface ItemManagerInterface extends Reloadable {

	/**
	 * Registers a new custom item.
	 *
	 * @param item the {@link CustomItem} to be registered
	 * @return true if the item was successfully registered, false otherwise
	 */
	boolean registerItem(@NotNull CustomItem item);

	/**
	 * Builds an item using the given context and item ID.
	 *
	 * @param context the {@link Context} in which the item is built
	 * @param id      the ID of the item to be built
	 * @return the built {@link ItemStack}
	 * @throws NullPointerException if the item ID is not found
	 */
	@Nullable
	@ApiStatus.Internal
	ItemStack buildInternal(@NotNull Context<Player> context, @NotNull String id) throws NullPointerException;

	/**
	 * Builds a custom item using the given context and item definition.
	 *
	 * @param context the {@link Context} in which the item is built
	 * @param item    the {@link CustomItem} definition
	 * @return the built {@link ItemStack}
	 */
	ItemStack build(@NotNull Context<Player> context, @NotNull CustomItem item);

	/**
	 * Builds any item using the given context and item ID. Example:
	 * {@code getCustomItemID:ID}
	 *
	 * @param context the {@link Context} in which the item is built
	 * @param id      the ID of the item to be built
	 * @return the built {@link ItemStack}, or null if the item ID is not found
	 */
	@Nullable
	ItemStack buildAny(@NotNull Context<Player> context, @NotNull String id);

	/**
	 * Retrieves the item ID of the given item stack. If it's a vanilla item, the
	 * returned value would be capitalized for instance {@code PAPER}. If it's a
	 * custom item, the returned value would be the ID for instance
	 * {@code beginner_rod}. If it's an item from other plugins, the returned value
	 * would be the id from that plugin for instance {@code itemsadder_namespace:id}
	 * / {@code oraxen_item_id}
	 *
	 * @param itemStack the {@link ItemStack} to be checked
	 * @return the custom item ID, or null if the item stack is not a custom fishing
	 *         item
	 */
	@NotNull
	String getItemID(@NotNull ItemStack itemStack);

	@NotNull
	String getIDFromLocation(Location location);

	/**
	 * Retrieves the custom item ID if the given item stack is a custom item.
	 *
	 * @param itemStack the {@link ItemStack} to be checked
	 * @return the custom item ID, or null if the item stack is not a custom item
	 */
	@Nullable
	String getCustomItemID(@NotNull ItemStack itemStack);

	/**
	 * Gets the loot by providing the context
	 *
	 * @param context context
	 * @param rod     rod
	 * @param hook    hook
	 * @return the loot
	 */
	@NotNull
	ItemStack getItemLoot(@NotNull Context<Player> context, ItemStack rod, FishHook hook);

	/**
	 * Drops a custom item as loot.
	 *
	 * @param context the {@link Context} in which the item is dropped
	 * @param rod     the fishing rod {@link ItemStack}
	 * @param hook    the {@link FishHook} entity
	 * @return the dropped {@link Item} entity
	 */
	@Nullable
	Item dropItemLoot(@NotNull Context<Player> context, ItemStack rod, FishHook hook);

	/**
	 * Checks if the given item stack has custom durability.
	 *
	 * @param itemStack the {@link ItemStack} to be checked
	 * @return true if the item stack has custom durability, false otherwise
	 */
	boolean hasCustomMaxDamage(ItemStack itemStack);

	/**
	 * Gets the maximum damage value for the given item stack.
	 *
	 * @param itemStack the {@link ItemStack} to be checked
	 * @return the maximum damage value
	 */
	int getMaxDamage(ItemStack itemStack);

	/**
	 * Decreases the damage of the given item stack.
	 *
	 * @param player    the {@link Player} holding the item
	 * @param itemStack the {@link ItemStack} to be modified
	 * @param amount    the amount to decrease the damage by
	 */
	void decreaseDamage(Player player, ItemStack itemStack, int amount);

	/**
	 * Increases the damage of the given item stack.
	 *
	 * @param player         the {@link Player} holding the item
	 * @param itemStack      the {@link ItemStack} to be modified
	 * @param amount         the amount to increase the damage by
	 * @param incorrectUsage true if the damage increase is due to incorrect usage,
	 *                       false otherwise
	 */
	void increaseDamage(Player player, ItemStack itemStack, int amount, boolean incorrectUsage);

	/**
	 * Sets the damage of the given item stack.
	 *
	 * @param player    the {@link Player} holding the item
	 * @param itemStack the {@link ItemStack} to be modified
	 * @param damage    the new damage value
	 */
	void setDamage(Player player, ItemStack itemStack, int damage);

	/**
	 * Returns the item factory used to create custom items.
	 *
	 * @return the {@link ItemFactory} instance
	 */
	ItemFactory<HellblockPlugin, RtagItem, ItemStack> getFactory();

	/**
	 * Returns an array of item providers used to manage custom items.
	 *
	 * @return an array of {@link ItemProvider} instances
	 */
	ItemProvider[] getItemProviders();

	/**
	 * Returns a collection of all registered item IDs.
	 *
	 * @return a collection of item ID strings
	 */
	Collection<String> getItemIDs();

	/**
	 * Wraps the given item stack in a custom item wrapper.
	 *
	 * @param itemStack the {@link ItemStack} to be wrapped
	 * @return the wrapped {@link com.swiftlicious.hellblock.creation.item.Item}
	 *         instance
	 */
	com.swiftlicious.hellblock.creation.item.Item<ItemStack> wrap(ItemStack itemStack);
}