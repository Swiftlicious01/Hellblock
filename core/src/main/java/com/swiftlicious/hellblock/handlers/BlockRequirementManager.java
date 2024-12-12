package com.swiftlicious.hellblock.handlers;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.world.HellblockBlockState;

public class BlockRequirementManager extends AbstractRequirementManager<HellblockBlockState> {

	public BlockRequirementManager(HellblockPlugin plugin) {
		super(plugin, HellblockBlockState.class);
	}

	@Override
	protected void registerBuiltInRequirements() {
		super.registerBuiltInRequirements();
	}

	@Override
	public void load() {
		loadExpansions(HellblockBlockState.class);
	}
}