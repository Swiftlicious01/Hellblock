package com.swiftlicious.hellblock.listeners.generator;

import java.util.UUID;

import com.swiftlicious.hellblock.listeners.NetherGeneratorHandler.LocationKey;

public class GenPiston {

	private UUID uuid;
	private LocationKey loc;
	private boolean hasBeenUsed = false;

	public GenPiston(LocationKey loc, UUID uuid) {
		this.uuid = uuid;
		this.loc = loc;
	}

	public UUID getUUID() {
		return uuid;
	}

	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	public LocationKey getLoc() {
		return loc;
	}

	public void setLoc(LocationKey loc) {
		this.loc = loc;
	}

	public boolean hasBeenUsed() {
		return hasBeenUsed;
	}

	public void setHasBeenUsed(boolean hasBeenUsed) {
		this.hasBeenUsed = hasBeenUsed;
	}
}