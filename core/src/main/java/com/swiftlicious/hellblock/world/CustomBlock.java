package com.swiftlicious.hellblock.world;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public class CustomBlock implements CustomBlockInterface {

	private final Key type;

	public CustomBlock(Key type) {
		this.type = type;
	}

	@Override
	public Key type() {
		return type;
	}

	@Override
	public CustomBlockState createBlockState() {
		return CustomBlockStateInterface.create(this, CompoundBinaryTag.empty());
	}

	@Override
	public CustomBlockState createBlockState(CompoundBinaryTag compound) {
		return CustomBlockStateInterface.create(this, compound);
	}

	@Override
	public CustomBlockState createBlockState(String itemID) {
		return createBlockState(); // Placeholder behavior
	}

	public String id(CustomBlockState state) {
		final BinaryTag tag = state.get("key");
		if (tag instanceof StringBinaryTag stringTag) {
			return stringTag.value();
		}
		return "";
	}

	public void id(CustomBlockState state, String id) {
		state.set("key", StringBinaryTag.stringBinaryTag(id));
	}

	protected boolean canTick(CustomBlockState state, int interval) {
		if (interval <= 0) {
			return false;
		}
		if (interval == 1) {
			return true;
		}

		final BinaryTag tag = state.get("tick");
		int tick = 0;

		if (tag instanceof IntBinaryTag intTag) {
			tick = intTag.value();
		}

		tick++;

		if (tick >= interval) {
			state.set("tick", IntBinaryTag.intBinaryTag(0));
			return true;
		} else {
			state.set("tick", IntBinaryTag.intBinaryTag(tick));
			return false;
		}
	}
}