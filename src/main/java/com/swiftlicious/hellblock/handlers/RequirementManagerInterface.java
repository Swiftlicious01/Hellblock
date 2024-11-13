package com.swiftlicious.hellblock.handlers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.utils.extras.Condition;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.factory.RequirementFactory;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public interface RequirementManagerInterface extends Reloadable {

	/**
	 * Registers a custom requirement type with its corresponding factory.
	 *
	 * @param type               The type identifier of the requirement.
	 * @param requirementFactory The factory responsible for creating instances of
	 *                           the requirement.
	 * @return True if registration was successful, false if the type is already
	 *         registered.
	 */
	boolean registerRequirement(String type, RequirementFactory requirementFactory);

	/**
	 * Unregisters a custom requirement type.
	 *
	 * @param type The type identifier of the requirement to unregister.
	 * @return True if unregistration was successful, false if the type is not
	 *         registered.
	 */
	boolean unregisterRequirement(String type);

	/**
	 * Retrieves an array of requirements based on a configuration section.
	 *
	 * @param section  The configuration section containing requirement definitions.
	 * @param advanced A flag indicating whether to use advanced requirements.
	 * @return An array of Requirement objects based on the configuration section
	 */
	@Nullable
	Requirement[] getRequirements(Section section, boolean advanced);

	/**
	 * If a requirement type exists
	 *
	 * @param type type
	 * @return exists or not
	 */
	boolean hasRequirement(String type);

	/**
	 * Retrieves a Requirement object based on a configuration section and advanced
	 * flag.
	 * <p>
	 * requirement_1: <- section type: xxx value: xxx
	 *
	 * @param section  The configuration section containing requirement definitions.
	 * @param advanced A flag indicating whether to use advanced requirements.
	 * @return A Requirement object based on the configuration section, or an
	 *         EmptyRequirement if the section is null or invalid.
	 */
	@NotNull
	Requirement getRequirement(Section section, boolean advanced);

	/**
	 * Gets a requirement based on the provided type and value. If a valid
	 * RequirementFactory is found for the type, it is used to create the
	 * requirement. If no factory is found, a warning is logged, and an empty
	 * requirement instance is returned.
	 * <p>
	 * world: <- type - world <- value
	 *
	 * @param type  The type representing the requirement type.
	 * @param value The value associated with the requirement.
	 * @return A Requirement instance based on the type and value, or an
	 *         EmptyRequirement if the type is invalid.
	 */
	@NotNull
	Requirement getRequirement(String type, Object value);

	/**
	 * Retrieves a RequirementFactory based on the specified requirement type.
	 *
	 * @param type The requirement type for which to retrieve a factory.
	 * @return A RequirementFactory for the specified type, or null if no factory is
	 *         found.
	 */
	@Nullable
	RequirementFactory getRequirementFactory(String type);

	/**
	 * Checks if an array of requirements is met for a given condition.
	 *
	 * @param condition    The Condition object to check against the requirements.
	 * @param requirements An array of Requirement instances to be evaluated.
	 * @return True if all requirements are met, false otherwise. Returns true if
	 *         the requirements array is null.
	 */
	static boolean isRequirementMet(Condition condition, Requirement... requirements) {
		if (requirements == null)
			return true;
		for (Requirement requirement : requirements) {
			if (!requirement.isConditionMet(condition)) {
				return false;
			}
		}
		return true;
	}
}