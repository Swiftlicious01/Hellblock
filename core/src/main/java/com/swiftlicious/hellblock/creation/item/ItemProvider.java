package com.swiftlicious.hellblock.creation.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.addons.ExternalProvider;

/**
 * Interface representing a provider for custom items. This interface allows for
 * building items for players and retrieving item IDs from item stacks.
 */
public interface ItemProvider extends ExternalProvider {

	/**
	 * Builds an ItemStack for a player based on a specified item ID.
	 *
	 * @param player the player for whom the item is being built.
	 * @param id     the ID of the item to build.
	 * @return the built ItemStack.
	 */
	@NotNull
    ItemStack buildItem(@NotNull Context<Player> player, @NotNull String id);

	/**
	 * Retrieves the item ID from a given ItemStack.
	 *
	 * @param itemStack the ItemStack from which to retrieve the item ID.
	 * @return the item ID as a string, or null if the item stack does not have an
	 *         associated ID.
	 */
	@Nullable
	String itemID(@NotNull ItemStack itemStack);
}