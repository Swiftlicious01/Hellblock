package com.swiftlicious.hellblock.creation.block;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

public interface BlockStateModifier {
	void apply(Player player, BlockState blockState);
}
