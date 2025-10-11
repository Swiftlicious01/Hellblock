package com.swiftlicious.hellblock.handlers;

import org.bukkit.block.data.BlockData;

import com.swiftlicious.hellblock.HellblockPlugin;

public class BlockRequirementManager extends AbstractRequirementManager<BlockData> {

	public BlockRequirementManager(HellblockPlugin plugin) {
		super(plugin, BlockData.class);
	}

	@Override
	public void registerBuiltInRequirements() {
		super.registerBuiltInRequirements();
	}

	@Override
	public void load() {
		loadExpansions(BlockData.class);
	}
}