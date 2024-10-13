package com.swiftlicious.hellblock.utils.extras;

/**
 * An implementation of the Action interface that represents an empty action
 * with no behavior. This class serves as a default action to prevent NPE.
 */
public class EmptyAction implements Action {

	public static EmptyAction EMPTY = new EmptyAction();

	@Override
	public void trigger(Condition condition) {
	}
}