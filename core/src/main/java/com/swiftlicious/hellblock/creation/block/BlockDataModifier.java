package com.swiftlicious.hellblock.creation.block;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;

/**
 * The BlockDataModifier interface that provides the logic for applying
 * modifications to block data in a context-specific manner.
 */
@FunctionalInterface
public interface BlockDataModifier {

	/**
	 * Applies modifications to the provided {@link BlockData} based on the given
	 * {@link Context}.
	 *
	 * @param context   the context
	 * @param blockData the block data to be modified
	 */
	void apply(Context<Player> context, BlockData blockData);
}