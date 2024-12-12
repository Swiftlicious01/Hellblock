package com.swiftlicious.hellblock.handlers;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.world.HellblockBlockState;

public class BlockActionManager extends AbstractActionManager<HellblockBlockState> {

	public BlockActionManager(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	protected void registerBuiltInActions() {
		super.registerBuiltInActions();
		super.registerBundleAction(HellblockBlockState.class);
	}

	@Override
	public void load() {
		loadExpansions(HellblockBlockState.class);
	}
}