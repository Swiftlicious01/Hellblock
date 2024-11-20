package com.swiftlicious.hellblock.mechanics.fishing.hook;

import com.swiftlicious.hellblock.effects.Effect;

/**
 * Interface for managing the mechanics of a fishing hook.
 */
public interface HookMechanic {

	/**
	 * Determines if the mechanic can start.
	 *
	 * @return true if the mechanic can start, false otherwise.
	 */
	boolean canStart();

	/**
	 * Determines if the mechanic should stop.
	 *
	 * @return true if the mechanic should stop, false otherwise.
	 */
	boolean shouldStop();

	/**
	 * Performs pre-start operations for the fishing hook.
	 */
	void preStart();

	/**
	 * Starts the fishing hook mechanic with a given effect.
	 *
	 * @param finalEffect the effect to apply when starting the hook.
	 */
	void start(Effect finalEffect);

	/**
	 * Checks if the fishing hook is hooked.
	 *
	 * @return true if the fishing hook is hooked, false otherwise.
	 */
	boolean isHooked();

	/**
	 * Destroys the mechanic.
	 */
	void destroy();

	/**
	 * Freezes the mechanic.
	 */
	void freeze();

	/**
	 * Unfreezes the mechanic.
	 *
	 * @param finalEffect the effect to apply when unfreezing
	 */
	void unfreeze(Effect finalEffect);
}