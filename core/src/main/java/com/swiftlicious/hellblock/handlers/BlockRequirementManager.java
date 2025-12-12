package com.swiftlicious.hellblock.handlers;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.world.CustomBlockState;

public class BlockRequirementManager extends AbstractRequirementManager<CustomBlockState> {

	public BlockRequirementManager(HellblockPlugin plugin) {
		super(plugin, CustomBlockState.class);
	}

	@Override
	public void registerBuiltInRequirements() {
		super.registerBuiltInRequirements();
	}

	@Override
	public void load() {
		loadExpansions(CustomBlockState.class);
	}
}