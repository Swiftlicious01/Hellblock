package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.player.Context;

/**
 * Represents an empty requirement that always returns true when checking
 * conditions.
 */
public class EmptyRequirement<T> implements Requirement<T> {

	public static <T> EmptyRequirement<T> instance() {
		return new EmptyRequirement<>();
	}

	@Override
	public boolean isSatisfied(Context<T> context) {
		return true;
	}
}