package com.swiftlicious.hellblock.events.invasion;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a new wave of mobs is spawned in an invasion.
 */
public class InvasionSpawnWaveEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final UUID ownerId;
	private final List<LivingEntity> spawnedMobs;
	private final int waveNumber;

	public InvasionSpawnWaveEvent(UUID ownerId, List<LivingEntity> spawnedMobs, int waveNumber) {
		this.ownerId = ownerId;
		this.spawnedMobs = spawnedMobs;
		this.waveNumber = waveNumber;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public List<LivingEntity> getSpawnedMobs() {
		return spawnedMobs;
	}

	public int getWaveNumber() {
		return waveNumber;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}