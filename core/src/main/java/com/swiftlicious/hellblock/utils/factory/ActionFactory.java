package com.swiftlicious.hellblock.utils.factory;

import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;

/**
 * Interface representing a factory for creating actions.
 *
 * @param <T> the type of object that the action will operate on
 */
public interface ActionFactory<T> {

	/**
	 * Constructs an action based on the provided arguments.
	 *
	 * @param args the args containing the arguments needed to build the action
	 * @return the constructed action
	 */
	Action<T> process(Object args, MathValue<T> chance);
}