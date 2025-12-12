package com.swiftlicious.hellblock.events.wither;

import org.bukkit.entity.Wither;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an Enhanced Wither falls below 25% health and attempts to heal
 * itself. This event is cancellable. If cancelled, the Wither will not heal.
 */
public class WitherLowHealthEvent extends Event implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();

	private final @NotNull Wither wither;
	private final double currentHealth;
	private final double maxHealth;
	private final double healAmount;

	private boolean cancelled = false;

	public WitherLowHealthEvent(@NotNull Wither wither, double currentHealth, double maxHealth, double healAmount) {
		this.wither = wither;
		this.currentHealth = currentHealth;
		this.maxHealth = maxHealth;
		this.healAmount = healAmount;
	}

	public @NotNull Wither getWither() {
		return wither;
	}

	public double getCurrentHealth() {
		return currentHealth;
	}

	public double getMaxHealth() {
		return maxHealth;
	}

	public double getHealAmount() {
		return healAmount;
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