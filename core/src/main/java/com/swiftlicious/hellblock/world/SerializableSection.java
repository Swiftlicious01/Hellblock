package com.swiftlicious.hellblock.world;

import com.flowpowered.nbt.CompoundTag;

import java.util.List;

public record SerializableSection(int sectionID, List<CompoundTag> blocks) {

	public boolean canPrune() {
		return blocks.isEmpty();
	}
}
