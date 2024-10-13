package com.swiftlicious.hellblock.utils.extras;

/**
 * Represents an empty requirement that always returns true when checking
 * conditions.
 */
public class EmptyRequirement implements Requirement {

	public static EmptyRequirement EMPTY = new EmptyRequirement();

	@Override
	public boolean isConditionMet(Condition condition) {
		return true;
	}
}
