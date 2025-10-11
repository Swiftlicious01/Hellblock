package com.swiftlicious.hellblock.handlers;

import org.bukkit.block.data.BlockData;

import com.swiftlicious.hellblock.HellblockPlugin;

public class BlockActionManager extends AbstractActionManager<BlockData> {

	public BlockActionManager(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	public void registerBuiltInActions() {
		super.registerBuiltInActions();
		super.registerBundleAction(BlockData.class);
	}

	@Override
	public void load() {
		loadExpansions(BlockData.class);
	}
}