package com.swiftlicious.hellblock.world.block.crop;

import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.block.MoistureHolder;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;

public class CustomFarmlandBlock extends CustomBlock implements MoistureHolder {

	private static final String MOISTURE_TAG = "moisture";
	private static final int MAX_MOISTURE = 7;

	public CustomFarmlandBlock(Key type) {
		super(type);
	}

	@Override
	public int getMoisture(CustomBlockState state) {
		BinaryTag tag = state.get(MOISTURE_TAG);
		return (tag instanceof IntBinaryTag it) ? it.value() : 0;
	}

	@Override
	public void setMoisture(CustomBlockState state, int moisture) {
		state.set(MOISTURE_TAG, IntBinaryTag.intBinaryTag(Math.min(moisture, MAX_MOISTURE)));
	}

	@Override
	public int getMaxMoisture(CustomBlockState state) {
		return MAX_MOISTURE;
	}
}