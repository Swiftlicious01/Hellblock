package com.swiftlicious.hellblock.context;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.world.CustomBlockState;

public final class BlockContext extends AbstractContext<CustomBlockState> {

	public BlockContext(@NotNull CustomBlockState block, Location location, boolean sync) {
		super(block, sync);
		updateLocation(location);
	}

	@Override
	public String toString() {
		return "BlockContext{" + "args=" + args() + ", block=" + holder() + '}';
	}
}
