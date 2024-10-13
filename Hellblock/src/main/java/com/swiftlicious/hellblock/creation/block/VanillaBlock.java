package com.swiftlicious.hellblock.creation.block;

import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class VanillaBlock implements BlockLibrary {

	@Override
	public String identification() {
		return "vanilla";
	}

	@Override
	public BlockData getBlockData(Player player, String id, List<BlockDataModifier> modifiers) {
		BlockData blockData = Material.valueOf(id.toUpperCase(Locale.ENGLISH)).createBlockData();
		for (BlockDataModifier modifier : modifiers) {
			modifier.apply(player, blockData);
		}
		return blockData;
	}

	@Override
	public @Nullable String getBlockID(Block block) {
		return block.getType().name();
	}
}