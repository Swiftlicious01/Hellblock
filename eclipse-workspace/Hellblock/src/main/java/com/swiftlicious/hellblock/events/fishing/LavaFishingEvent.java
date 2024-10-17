package com.swiftlicious.hellblock.events.fishing;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents an event that occurs when a player fishes in lava.
 */
public class LavaFishingEvent extends PlayerEvent implements Cancellable {

	private static final HandlerList handlerList = new HandlerList();
	private final State state;
	private boolean isCancelled;
	private final FishHook hook;

	/**
	 * Constructs a new LavaFishingEvent.
	 *
	 * @param who   The player who triggered the event.
	 * @param state The state of the fishing action (REEL_IN, CAUGHT_FISH, or BITE).
	 * @param hook  The FishHook entity associated with the fishing action.
	 */
	public LavaFishingEvent(@NotNull Player who, State state, FishHook hook) {
		super(who);
		this.state = state;
		this.isCancelled = false;
		this.hook = hook;
	}

	/**
	 * Gets the state of the fishing action.
	 *
	 * @return The fishing state, which can be REEL_IN, CAUGHT_FISH, or BITE.
	 */
	public State getState() {
		return state;
	}

	/**
	 * Gets the FishHook entity associated with the fishing action.
	 *
	 * @return The FishHook entity used in the fishing action.
	 */
	public FishHook getHook() {
		return hook;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return getHandlerList();
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		isCancelled = cancel;
	}

	/**
	 * An enumeration representing possible states of the fishing action (REEL_IN,
	 * CAUGHT_FISH, BITE).
	 */
	public enum State {
		REEL_IN, CAUGHT_FISH, BITE
	}
}