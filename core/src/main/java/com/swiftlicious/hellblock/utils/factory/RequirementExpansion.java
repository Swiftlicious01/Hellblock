package com.swiftlicious.hellblock.utils.factory;

/**
 * An abstract class representing a requirement expansion Requirement expansions
 * are used to define custom requirements for various functionalities.
 */
public abstract class RequirementExpansion<T> {

	/**
	 * Get the version of this requirement expansion.
	 *
	 * @return The version of the expansion.
	 */
	public abstract String getVersion();

	/**
	 * Get the author of this requirement expansion.
	 *
	 * @return The author of the expansion.
	 */
	public abstract String getAuthor();

	/**
	 * Get the type of requirement provided by this expansion.
	 *
	 * @return The type of requirement.
	 */
	public abstract String getRequirementType();

	/**
	 * Get the factory for creating requirements defined by this expansion.
	 *
	 * @return The requirement factory.
	 */
	public abstract RequirementFactory<T> getRequirementFactory();
}