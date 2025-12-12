package com.swiftlicious.hellblock.handlers;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.world.CustomBlockState;

public class BlockActionManager extends AbstractActionManager<CustomBlockState> {

	public BlockActionManager(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	public void registerBuiltInActions() {
		super.registerBuiltInActions();
		super.registerBundleAction(CustomBlockState.class);
	}

	@Override
	public void load() {
		loadExpansions(CustomBlockState.class);
	}
}