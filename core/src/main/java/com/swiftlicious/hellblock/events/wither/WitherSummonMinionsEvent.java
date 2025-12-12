package com.swiftlicious.hellblock.events.wither;

import org.bukkit.entity.Wither;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an Enhanced Wither attempts to summon a wave of minions. This
 * event is cancellable. If cancelled, no mobs are spawned.
 */
public class WitherSummonMinionsEvent extends Event implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();

	private final @NotNull Wither wither;
	private final int islandId;
	private final int waveNumber;

	private boolean cancelled = false;

	public WitherSummonMinionsEvent(int islandId, @NotNull Wither wither, int waveNumber) {
		this.islandId = islandId;
		this.wither = wither;
		this.waveNumber = waveNumber;
	}

	public @NotNull Wither getWither() {
		return wither;
	}

	public int getIslandId() {
		return islandId;
	}

	/**
	 * Wave number (1 or 2)
	 */
	public int getWaveNumber() {
		return waveNumber;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static @NotNull HandlerList getHandlerList() {
		return HANDLERS;
	}
}