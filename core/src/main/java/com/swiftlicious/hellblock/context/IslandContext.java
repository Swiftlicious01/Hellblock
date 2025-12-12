package com.swiftlicious.hellblock.context;

import org.jetbrains.annotations.NotNull;

/**
 * The IslandContext class implements the Context interface specifically for the
 * Island ID. It allows for storing and retrieving arguments related to a
 * island.
 */
public final class IslandContext extends AbstractContext<Integer> {

	public IslandContext(@NotNull Integer islandId, boolean sync) {
		super(islandId, sync);
		if (islandId == -1) {
			return;
		}
		arg(ContextKeys.ISLAND_ID, islandId);
	}

	@Override
	public String toString() {
		return "IslandContext{" + "args=" + args() + ", islandId=" + holder() + '}';
	}
}
