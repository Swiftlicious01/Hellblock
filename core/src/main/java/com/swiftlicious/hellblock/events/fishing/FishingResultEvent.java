package com.swiftlicious.hellblock.events.fishing;

import java.util.Optional;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import com.swiftlicious.hellblock.loot.LootInterface;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;

/**
 * This class represents an event that is triggered when a fishing result is
 * determined.
 */
public class FishingResultEvent extends PlayerEvent implements Cancellable {

	private static final HandlerList handlerList = new HandlerList();
	private boolean isCancelled;
	private final Result result;
	private final LootInterface loot;
	private final FishHook fishHook;
	private final Context<Player> context;

	/**
	 * Constructs a new FishingResultEvent.
	 *
	 * @param context  The context in which the fishing result occurs
	 * @param result   The result of the fishing action
	 * @param fishHook The fish hook involved
	 * @param loot     The loot involved
	 */
	public FishingResultEvent(@NotNull Context<Player> context, Result result, FishHook fishHook, LootInterface loot) {
		super(context.holder());
		this.context = context;
		this.result = result;
		this.loot = loot;
		this.fishHook = fishHook;
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
	 * Gets the {@link Result} of the fishing action.
	 *
	 * @return The result of the fishing action
	 */
	public Result getResult() {
		return result;
	}

	/**
	 * Gets the {@link FishHook} involved.
	 *
	 * @return The fish hook
	 */
	public FishHook getFishHook() {
		return fishHook;
	}

	/**
	 * Gets the {@link LootInterface} obtained from the fishing.
	 *
	 * @return The loot
	 */
	public LootInterface getLoot() {
		return loot;
	}

	/**
	 * Gets the {@link Context<Player>}
	 *
	 * @return The context
	 */
	public Context<Player> getContext() {
		return context;
	}

	/**
	 * Gets the amount of loot obtained from the fishing action. If the result is a
	 * failure, the amount is 0.
	 *
	 * @return The amount of loot obtained
	 */
	public int getAmount() {
		if (result == Result.FAILURE)
			return 0;
		return Optional.ofNullable(context.arg(ContextKeys.AMOUNT)).orElse(1);
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return getHandlerList();
	}

	public enum Result {
		SUCCESS, FAILURE
	}
}