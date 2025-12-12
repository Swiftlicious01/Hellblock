package com.swiftlicious.hellblock.listeners.generator;

import java.time.Instant;
import java.util.UUID;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.world.Pos3;

public class GenBlock {

	private final Pos3 pos;
	private final UUID uuid;
	private final Instant timestamp;
	private boolean pistonPowered = false;

	public GenBlock(Pos3 pos, UUID uuid) {
		this(pos, uuid, false);
	}

	public GenBlock(Pos3 pos, UUID uuid, boolean pistonPowered) {
		this.pos = pos;
		this.uuid = uuid;
		this.timestamp = Instant.now();
		this.pistonPowered = pistonPowered;
	}

	public Pos3 getPosition() {
		return pos;
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