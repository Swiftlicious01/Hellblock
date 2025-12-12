package com.swiftlicious.hellblock.world.block;

import java.util.Optional;

import com.swiftlicious.hellblock.world.CustomBlockState;

public class CustomBlockCapabilities {

	public static Optional<Growable> asGrowable(CustomBlockState state) {
		if (state.type() instanceof Growable growable) {
			return Optional.of(growable);
		}
		return Optional.empty();
	}

	public static Optional<MoistureHolder> asMoistureHolder(CustomBlockState state) {
		if (state.type() instanceof MoistureHolder moist) {
			return Optional.of(moist);
		}
		return Optional.empty();
	}

	public static Optional<Directional> asDirectional(CustomBlockState state) {
		if (state.type() instanceof Directional dir) {
			return Optional.of(dir);
		}
		return Optional.empty();
	}
}