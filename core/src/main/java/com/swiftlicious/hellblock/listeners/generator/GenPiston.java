package com.swiftlicious.hellblock.listeners.generator;

import com.swiftlicious.hellblock.world.Pos3;

public class GenPiston {

	private int islandId;
	private Pos3 pos;
	private boolean hasBeenUsed = false;

	public GenPiston(Pos3 pos, int islandId) {
		this.islandId = islandId;
		this.pos = pos;
	}

	public int getIslandId() {
		return islandId;
	}

	public void setIslandId(int islandId) {
		this.islandId = islandId;
	}

	public Pos3 getPos() {
		return pos;
	}

	public void setPos(Pos3 pos) {
		this.pos = pos;
	}

	public boolean hasBeenUsed() {
		return hasBeenUsed;
	}

	public void setHasBeenUsed(boolean hasBeenUsed) {
		this.hasBeenUsed = hasBeenUsed;
	}
}