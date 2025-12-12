package com.swiftlicious.hellblock.world.block;

import com.swiftlicious.hellblock.world.CustomBlockState;

public interface Growable {
	int getAge(CustomBlockState state);

	void setAge(CustomBlockState state, int age);

	int getMaxAge(CustomBlockState state);
}