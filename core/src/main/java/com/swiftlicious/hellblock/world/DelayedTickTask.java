package com.swiftlicious.hellblock.world;

import org.jetbrains.annotations.NotNull;

public class DelayedTickTask implements Comparable<DelayedTickTask> {

	private static int taskID;
	private final int time;
	private final BlockPos blockPos;
	private final int id;

	public DelayedTickTask(int time, BlockPos blockPos) {
		this.time = time;
		this.blockPos = blockPos;
		this.id = taskID++;
	}

	public BlockPos blockPos() {
		return blockPos;
	}

	public int getTime() {
		return time;
	}

	@Override
	public int compareTo(@NotNull DelayedTickTask o) {
		if (this.time > o.time) {
			return 1;
		} else if (this.time < o.time) {
			return -1;
		} else {
			return Integer.compare(this.id, o.id);
		}
	}
}