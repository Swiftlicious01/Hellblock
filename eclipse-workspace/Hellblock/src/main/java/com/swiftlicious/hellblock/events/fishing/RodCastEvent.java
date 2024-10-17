package com.swiftlicious.hellblock.events.fishing;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.listeners.fishing.FishingPreparation;

/**
 * This class represents an event that occurs when a player casts a fishing rod.
 */
public class RodCastEvent extends PlayerEvent implements Cancellable {

	private final Effect effect;
	private boolean isCancelled;
	private final PlayerFishEvent event;
	private final FishingPreparation preparation;
	private static final HandlerList handlerList = new HandlerList();

	/**
	 * Constructs a new RodCastEvent.
	 *
	 * @param event              The original PlayerFishEvent that triggered the rod
	 *                           cast.
	 * @param fishingPreparation The fishing preparation associated with the rod
	 *                           cast.
	 * @param effect             The effect associated with the fishing rod cast.
	 */
	public RodCastEvent(PlayerFishEvent event, FishingPreparation fishingPreparation, Effect effect) {
		super(event.getPlayer());
		this.effect = effect;
		this.event = event;
		this.preparation = fishingPreparation;
	}

	@Override
	public boolean isCancelled() {
		return this.isCancelled;
	}

	/**
	 * Cancelling this event would not cancel the bukkit PlayerFishEvent
	 *
	 * @param cancel true if you wish to cancel this event
	 */
	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	/**
	 * Gets the fishing preparation associated with the rod cast.
	 *
	 * @return The FishingPreparation associated with the rod cast.
	 */
	public FishingPreparation getPreparation() {
		return preparation;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return getHandlerList();
	}

	/**
	 * Gets the effect associated with the fishing rod cast.
	 *
	 * @return The Effect associated with the rod cast.
	 */
	public Effect getEffect() {
		return effect;
	}

	/**
	 * Gets the original PlayerFishEvent that triggered the rod cast.
	 *
	 * @return The original PlayerFishEvent.
	 */
	public PlayerFishEvent getBukkitPlayerFishEvent() {
		return event;
	}
}
