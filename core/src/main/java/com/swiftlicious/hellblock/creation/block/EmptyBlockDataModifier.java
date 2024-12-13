package com.swiftlicious.hellblock.creation.block;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;

/**
 * A no-operation implementation of the {@link BlockDataModifier} interface.
 * This modifier does nothing when applied.
 */
public class EmptyBlockDataModifier implements BlockDataModifier {

	public static final BlockDataModifier INSTANCE = new EmptyBlockDataModifier();

	@Override
	public void apply(Context<Player> context, BlockData blockData) {
	}
}