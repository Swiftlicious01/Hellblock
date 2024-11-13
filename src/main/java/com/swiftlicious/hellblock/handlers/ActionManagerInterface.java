package com.swiftlicious.hellblock.handlers;

import java.util.Map;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Condition;
import com.swiftlicious.hellblock.utils.factory.ActionFactory;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public interface ActionManagerInterface extends Reloadable {

	/**
	 * Registers an ActionFactory for a specific action type. This method allows you
	 * to associate an ActionFactory with a custom action type.
	 *
	 * @param type          The custom action type to register.
	 * @param actionFactory The ActionFactory responsible for creating actions of
	 *                      the specified type.
	 * @return True if the registration was successful (the action type was not
	 *         already registered), false otherwise.
	 */
	boolean registerAction(String type, ActionFactory actionFactory);

	/**
	 * Unregisters an ActionFactory for a specific action type. This method allows
	 * you to remove the association between an action type and its ActionFactory.
	 *
	 * @param type The custom action type to unregister.
	 * @return True if the action type was successfully unregistered, false if it
	 *         was not found.
	 */
	boolean unregisterAction(String type);

	/**
	 * Retrieves an Action object based on the configuration provided in a
	 * Section. This method reads the type of action from the section,
	 * obtains the corresponding ActionFactory, and builds an Action object using
	 * the specified values and chance.
	 * <p>
	 * events: success: action_1: <- section ...
	 *
	 * @param section The Section containing the action configuration.
	 * @return An Action object created based on the configuration, or an
	 *         EmptyAction instance if the action type is invalid.
	 */
	Action getAction(Section section);

	/**
	 * Retrieves a mapping of ActionTriggers to arrays of Actions from a
	 * Section. This method iterates through the provided
	 * Section to extract action triggers and their associated arrays
	 * of Actions.
	 * <p>
	 * events: <- section success: action_1: ...
	 *
	 * @param section The Section containing action mappings.
	 * @return A HashMap where keys are ActionTriggers and values are arrays of
	 *         Action objects.
	 */
	Map<ActionTrigger, Action[]> getActionMap(Section section);

	/**
	 * Retrieves an array of Action objects from a Section. This method
	 * iterates through the provided Section to extract Action
	 * configurations and build an array of Action objects.
	 * <p>
	 * events: success: <- section action_1: ...
	 *
	 * @param section The Section containing action configurations.
	 * @return An array of Action objects created based on the configurations in the
	 *         section.
	 */
	Action[] getActions(Section section);

	/**
	 * Retrieves an ActionFactory associated with a specific action type.
	 *
	 * @param type The action type for which to retrieve the ActionFactory.
	 * @return The ActionFactory associated with the specified action type, or null
	 *         if not found.
	 */
	ActionFactory getActionFactory(String type);

	/**
	 * Retrieves a mapping of success times to corresponding arrays of actions from
	 * a Section.
	 * <p>
	 * events: success-times: <- section 1: action_1: ...
	 *
	 * @param section The Section containing success times actions.
	 * @return A HashMap where success times associated with actions.
	 */
	Map<Integer, Action[]> getTimesActionMap(Section section);

	/**
	 * Triggers a list of actions with the given condition. If the list of actions
	 * is not null, each action in the list is triggered.
	 *
	 * @param actions   The list of actions to trigger.
	 * @param condition The condition associated with the actions.
	 */
	static void triggerActions(Condition condition, Action... actions) {
		if (actions != null)
			for (Action action : actions)
				action.trigger(condition);
	}
}
