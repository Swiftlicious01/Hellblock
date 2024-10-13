package com.swiftlicious.hellblock.listeners.fishing;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.effects.FishingEffect;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Condition;

public abstract class FishingPreparationInterface extends Condition {

	public FishingPreparationInterface(Player player) {
		super(player);
	}

	/**
	 * Retrieves the ItemStack representing the fishing rod.
	 *
	 * @return The ItemStack representing the fishing rod.
	 */
	@NotNull
	public abstract ItemStack getRodItemStack();

	/**
	 * Retrieves the ItemStack representing the bait (if available).
	 *
	 * @return The ItemStack representing the bait, or null if no bait is set.
	 */
	@Nullable
	public abstract ItemStack getBaitItemStack();

	/**
	 * Checks if player meet the requirements for fishing gears
	 *
	 * @return True if can fish, false otherwise.
	 */
	public abstract boolean canFish();

	/**
	 * Merges a FishingEffect into this fishing rod, applying effect modifiers.
	 *
	 * @param effect The FishingEffect to merge into this rod.
	 */
	public abstract void mergeEffect(FishingEffect effect);

	/**
	 * Triggers actions associated with a specific action trigger.
	 *
	 * @param actionTrigger The action trigger that initiates the actions.
	 */
	public abstract void triggerActions(ActionTrigger actionTrigger);
}