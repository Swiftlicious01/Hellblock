package com.swiftlicious.hellblock.creation.block;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface BlockLibrary {

	String identification();

	BlockData getBlockData(Player player, String id, List<BlockDataModifier> modifiers);

	@Nullable
	String getBlockID(Block block);
}
