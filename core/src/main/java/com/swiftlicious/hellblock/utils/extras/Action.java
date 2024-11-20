package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.player.Context;

/**
 * The Action interface defines a generic action that can be triggered based on
 * a provided context.
 *
 * @param <T> the type of the object that is used in the context for triggering
 *            the action.
 */
public interface Action<T> {

	/**
	 * Triggers the action based on the provided condition.
	 *
	 * @param context the context
	 */
	void trigger(Context<T> context);

	static <T> Action<T> empty() {
		return EmptyAction.instance();
	}
}