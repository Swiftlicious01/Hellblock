package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.player.Context;

/**
 * Interface representing a requirement that must be met. This can be used to
 * define conditions that need to be satisfied within a given context.
 *
 * @param <T> the type parameter for the context
 */
public interface Requirement<T> {

	/**
	 * Evaluates whether the requirement is met within the given context.
	 *
	 * @param context the context in which the requirement is evaluated
	 * @return true if the requirement is met, false otherwise
	 */
	boolean isSatisfied(Context<T> context);

	static <T> Requirement<T> empty() {
		return EmptyRequirement.instance();
	}
}