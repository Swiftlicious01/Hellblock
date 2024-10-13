package com.swiftlicious.hellblock.utils.extras;

public interface Requirement {

	/**
	 * Is condition met the requirement
	 *
	 * @param condition condition
	 * @return meet or not
	 */
	boolean isConditionMet(Condition condition);
}