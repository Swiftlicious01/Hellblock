package com.swiftlicious.hellblock.listeners.generator;

import java.time.Instant;
import java.util.UUID;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.listeners.NetherGeneratorHandler.LocationKey;

public class GenBlock {
	
	private final LocationKey location;
	private final UUID uuid;
	private final Instant timestamp;
	private boolean pistonPowered = false;

	public GenBlock(LocationKey l, UUID uuid) {
		this(l, uuid, false);
	}

	public GenBlock(LocationKey l, UUID uuid, boolean pistonPowered) {
		this.location = l;
		this.uuid = uuid;
		this.timestamp = Instant.now();
		this.pistonPowered = pistonPowered;
	}

	public LocationKey getLocation() {
		return location;
	}

	public UUID getUUID() {
		return uuid;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public boolean isPistonPowered() {
		return this.pistonPowered;
	}

	public boolean hasExpired() {
		if (this.pistonPowered && HellblockPlugin.getInstance().getConfigManager().pistonAutomation()) {
			return false;
		}
		// Expire entries 4 seconds after they were created
		// It only needs enough time for lava to flow and generator a new block
		if (!(Instant.now().getEpochSecond() >= (timestamp.getEpochSecond() + 4))) {
			return false;
		}
		HellblockPlugin.getInstance().debug("GenMode has expired.");
		return true;
	}
}