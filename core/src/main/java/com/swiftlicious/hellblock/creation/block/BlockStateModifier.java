package com.swiftlicious.hellblock.creation.block;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.player.Context;

/**
 * Functional interface for modifying a {@link BlockState} based on a given
 * context.
 */
@FunctionalInterface
public interface BlockStateModifier {

	/**
	 * Applies modifications to the provided block state based on the given context.
	 *
	 * @param context    the context containing the player information.
	 * @param blockState the block state to modify.
	 */
	void apply(Context<Player> context, BlockState blockState);
}