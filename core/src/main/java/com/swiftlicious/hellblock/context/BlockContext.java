package com.swiftlicious.hellblock.context;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.world.HellblockBlockState;

public class BlockContext extends AbstractContext<HellblockBlockState> {

	public BlockContext(@NotNull HellblockBlockState block, Location location, boolean sync) {
		super(block, sync);
		updateLocation(location);
	}

	@Override
	public String toString() {
		return "BlockContext{" + "args=" + args() + ", block=" + holder() + '}';
	}
}
