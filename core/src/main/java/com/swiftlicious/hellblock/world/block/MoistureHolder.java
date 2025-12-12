package com.swiftlicious.hellblock.world.block;

import com.swiftlicious.hellblock.world.CustomBlockState;

public interface MoistureHolder {
	int getMoisture(CustomBlockState state);

	void setMoisture(CustomBlockState state, int moisture);

	int getMaxMoisture(CustomBlockState state);
}