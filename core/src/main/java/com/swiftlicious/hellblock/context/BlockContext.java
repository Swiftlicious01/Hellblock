package com.swiftlicious.hellblock.context;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

public class BlockContext extends AbstractContext<BlockData> {

	public BlockContext(@NotNull BlockData block, Location location, boolean sync) {
		super(block, sync);
		updateLocation(location);
	}

	@Override
	public String toString() {
		return "BlockContext{" + "args=" + args() + ", block=" + holder() + '}';
	}
}
