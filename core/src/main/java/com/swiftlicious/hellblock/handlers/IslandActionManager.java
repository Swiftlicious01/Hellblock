package com.swiftlicious.hellblock.handlers;

import com.swiftlicious.hellblock.HellblockPlugin;

public class IslandActionManager extends AbstractActionManager<Integer> {

	public IslandActionManager(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	public void registerBuiltInActions() {
		super.registerBuiltInActions();
		super.registerBundleAction(Integer.class);
	}

	@Override
	public void load() {
		loadExpansions(Integer.class);
	}
}