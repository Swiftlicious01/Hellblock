package com.swiftlicious.hellblock.world.block.crop;

import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.block.Growable;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;

public abstract class BaseGrowableBlock extends CustomBlock implements Growable {

	private final String ageKey;
	private final int maxAge;

	protected BaseGrowableBlock(Key type, String ageKey, int maxAge) {
		super(type);
		this.ageKey = ageKey;
		this.maxAge = maxAge;
	}

	@Override
	public int getAge(CustomBlockState state) {
		BinaryTag tag = state.get(ageKey);
		return tag instanceof IntBinaryTag it ? it.value() : 0;
	}

	@Override
	public void setAge(CustomBlockState state, int age) {
		state.set(ageKey, IntBinaryTag.intBinaryTag(Math.min(age, maxAge)));
	}

	@Override
	public int getMaxAge(CustomBlockState state) {
		return maxAge;
	}
}