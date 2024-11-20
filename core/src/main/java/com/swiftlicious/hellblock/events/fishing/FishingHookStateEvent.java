package com.swiftlicious.hellblock.events.fishing;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents an event that occurs when the state of a fishing hook
 * changes. It is triggered by various states of the fishing hook such as when a
 * fish bites, escapes, is lured, or is landed.
 */
public class FishingHookStateEvent extends PlayerEvent {

	private static final HandlerList handlerList = new HandlerList();

	private final FishHook fishHook;
	private final State state;

	/**
	 * Constructs a new FishingHookStateEvent.
	 *
	 * @param who   The player associated with this event
	 * @param hook  The fishing hook involved in this event
	 * @param state The state of the fishing hook
	 */
	public FishingHookStateEvent(@NotNull Player who, FishHook hook, State state) {
		super(who);
		this.fishHook = hook;
		this.state = state;
	}

	/**
	 * Gets the {@link FishHook} involved in this event.
	 *
	 * @return The FishHook involved in this event
	 */
	public FishHook getFishHook() {
		return fishHook;
	}

	/**
	 * Gets the {@link State} of the fishing hook.
	 *
	 * @return The state of the fishing hook
	 */
	public State getState() {
		return state;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return getHandlerList();
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	public enum State {
		BITE, ESCAPE, LURE, LAND
	}
}