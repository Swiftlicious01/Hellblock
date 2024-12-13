package com.swiftlicious.hellblock.creation.block;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;

/**
 * A no-operation implementation of the {@link BlockStateModifier} interface.
 * This modifier does nothing when applied.
 */
public class EmptyBlockStateModifier implements BlockStateModifier {

	public static final EmptyBlockStateModifier INSTANCE = new EmptyBlockStateModifier();

	@Override
	public void apply(Context<Player> context, BlockState blockState) {
	}
}