package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.player.Context;

/**
 * An implementation of the Action interface that represents an empty action
 * with no behavior. This class serves as a default action to prevent NPE.
 */
public class EmptyAction<T> implements Action<T> {

	public static <T> EmptyAction<T> instance() {
		return new EmptyAction<>();
	}

	@Override
	public void trigger(Context<T> context) {
	}
}