package com.swiftlicious.hellblock.creation.block;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public interface BlockDataModifier {
	void apply(Player player, BlockData blockData);
}
