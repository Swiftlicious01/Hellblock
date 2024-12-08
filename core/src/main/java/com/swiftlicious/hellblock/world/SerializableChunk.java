package com.swiftlicious.hellblock.world;

import java.util.List;

public record SerializableChunk(int x, int z, int loadedSeconds, long lastLoadedTime,
		List<SerializableSection> sections, int[] queuedTasks, int[] ticked) {

	public boolean canPrune() {
		return sections.isEmpty();
	}
}
