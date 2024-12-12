package com.swiftlicious.hellblock.utils.factory;

/**
 * Abstract class representing an expansion of an action. This class should be
 * extended to provide specific implementations of actions.
 *
 * @param <T> the type parameter for the action factory
 */
public abstract class ActionExpansion<T> {

	/**
	 * Retrieves the version of this action expansion.
	 *
	 * @return a String representing the version of the action expansion
	 */
	public abstract String getVersion();

	/**
	 * Retrieves the author of this action expansion.
	 *
	 * @return a String representing the author of the action expansion
	 */
	public abstract String getAuthor();

	/**
	 * Retrieves the type of this action.
	 *
	 * @return a String representing the type of action
	 */
	public abstract String getActionType();

	/**
	 * Retrieves the action factory associated with this action expansion.
	 *
	 * @return an ActionFactory of type T that creates instances of the action
	 */
	public abstract ActionFactory<T> getActionFactory();
}