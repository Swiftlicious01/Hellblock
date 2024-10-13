package com.swiftlicious.hellblock.utils.factory;

import java.util.List;

import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Requirement;

/**
 * An interface for a requirement factory that builds requirements.
 */
public interface RequirementFactory {

	/**
	 * Build a requirement with the given arguments, not met actions, and check
	 * action flag.
	 *
	 * @param args          The arguments used to build the requirement.
	 * @param notMetActions Actions to be triggered when the requirement is not met
	 *                      (can be null).
	 * @param advanced      Flag indicating whether to check the action when
	 *                      building the requirement.
	 * @return The built requirement.
	 */
	Requirement build(Object args, List<Action> notMetActions, boolean advanced);

	/**
	 * Build a requirement with the given arguments.
	 *
	 * @param args The arguments used to build the requirement.
	 * @return The built requirement.
	 */
	default Requirement build(Object args) {
		return build(args, null, false);
	}
}