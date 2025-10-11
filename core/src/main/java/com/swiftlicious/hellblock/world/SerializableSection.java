package com.swiftlicious.hellblock.world;

import java.util.List;

import net.kyori.adventure.nbt.CompoundBinaryTag;

public record SerializableSection(int sectionID, List<CompoundBinaryTag> blocks) {

	public boolean canPrune() {
		return blocks.isEmpty();
	}
}