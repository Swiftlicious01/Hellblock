package com.swiftlicious.hellblock.utils.factory;

import java.util.List;

import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Requirement;

/**
 * Interface representing a factory for creating requirements.
 *
 * @param <T> the type of object that the requirement will operate on
 */
public interface RequirementFactory<T> {

	/**
	 * Build a requirement with the given arguments, not satisfied actions, and
	 * check run actions flag.
	 *
	 * @param args                The arguments used to build the requirement.
	 * @param notSatisfiedActions Actions to be triggered when the requirement is
	 *                            not met (can be null).
	 * @param runActions          Flag indicating whether to run the action if the
	 *                            requirement is not met.
	 * @return The built requirement.
	 */
	Requirement<T> process(Object args, List<Action<T>> notSatisfiedActions, boolean runActions);

	/**
	 * Build a requirement with the given arguments.
	 *
	 * @param args The arguments used to build the requirement.
	 * @return The built requirement.
	 */
	default Requirement<T> process(Object args) {
		return process(args, List.of(), false);
	}
}