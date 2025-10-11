package com.swiftlicious.hellblock.challenges;

public interface ChallengeRequirement {
	/**
	 * Checks if the given context matches this requirement.
	 *
	 * @param context Object (Block, Entity, ItemStack, Integer, etc.)
	 * @return true if it satisfies the requirement
	 */
	boolean matches(Object context);
}