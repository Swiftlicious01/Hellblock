package com.swiftlicious.hellblock.v1_19_r3;

import com.swiftlicious.hellblock.nms.fluid.FallingFluidData;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

public class FallingFluidDataInstance extends FluidDataInstance implements FallingFluidData {

	public FallingFluidDataInstance(final FluidState state) {
		super(state);
	}

	@Override
	public boolean isFalling() {
		return this.getState().getValue(FlowingFluid.FALLING);
	}
}
