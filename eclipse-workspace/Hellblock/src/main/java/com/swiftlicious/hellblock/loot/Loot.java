package com.swiftlicious.hellblock.loot;

import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.effects.BaseEffect;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Condition;

public interface Loot {
	
    /**
     * Check if the loot disables global actions
     */
    boolean disableGlobalAction();

	/**
	 * Get the unique ID of this loot.
	 *
	 * @return The unique ID.
	 */
	String getID();

	/**
	 * Get the type of this loot.
	 *
	 * @return The loot type.
	 */
	LootType getType();

	/**
	 * Get the nickname of this loot.
	 *
	 * @return The nickname.
	 */
	@NotNull
	String getNick();

	/**
	 * Check if this loot should be shown in the finder.
	 *
	 * @return True if it should be shown, false otherwise.
	 */
	boolean showInFinder();

	/**
	 * Get the loot group of this loot.
	 *
	 * @return The loot group.
	 */
	String[] getLootGroup();

	/**
	 * Get the actions triggered by a specific action trigger.
	 *
	 * @param actionTrigger The action trigger.
	 * @return The actions triggered by the given trigger.
	 */
	@Nullable
	Action[] getActions(ActionTrigger actionTrigger);

	/**
	 * Trigger actions associated with a specific action trigger.
	 *
	 * @param actionTrigger The action trigger.
	 * @param condition     The condition under which the actions are triggered.
	 */
	void triggerActions(ActionTrigger actionTrigger, Condition condition);

	/**
	 * Get effects that bond to this loot
	 *
	 * @return effects
	 */
	BaseEffect getBaseEffect();

	/**
	 * Get the actions triggered by a specific number of successes.
	 *
	 * @param times The number of successes.
	 * @return The actions triggered by the specified number of successes.
	 */
	Action[] getSuccessTimesActions(int times);

	/**
	 * Get a map of actions triggered by different numbers of successes.
	 *
	 * @return A map of actions triggered by success times.
	 */
	HashMap<Integer, Action[]> getSuccessTimesActionMap();
}
