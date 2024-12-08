package com.swiftlicious.hellblock.world;

public enum TickMode {

	RANDOM_TICK(1), SCHEDULED_TICK(2), ALL(0);

	private final int mode;

	TickMode(int mode) {
		this.mode = mode;
	}

	public int mode() {
		return mode;
	}
}