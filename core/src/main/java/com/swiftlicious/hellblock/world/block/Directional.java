package com.swiftlicious.hellblock.world.block;

import org.bukkit.block.BlockFace;

import com.swiftlicious.hellblock.world.CustomBlockState;

public interface Directional {
	BlockFace getFacing(CustomBlockState state);

	void setFacing(CustomBlockState state, BlockFace face);
}