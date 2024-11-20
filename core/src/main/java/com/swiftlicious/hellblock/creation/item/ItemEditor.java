package com.swiftlicious.hellblock.creation.item;

import org.bukkit.entity.Player;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.player.Context;

import org.jetbrains.annotations.ApiStatus;

/**
 * Functional interface representing an editor for custom fishing items.
 * Implementations of this interface apply modifications to an {@link RtagItem}
 * using the provided context.
 */
@ApiStatus.Internal
@FunctionalInterface
public interface ItemEditor {

	/**
	 * Applies modifications to the given {@link RtagItem} using the provided
	 * context.
	 *
	 * @param item    the {@link RtagItem} to be modified
	 * @param context the {@link Context} in which the modifications are applied
	 */
	void apply(RtagItem item, Context<Player> context);
}
