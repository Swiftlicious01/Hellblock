package com.swiftlicious.hellblock.api;

/**
 * Abstract base class for information. Contains common properties and methods
 * for info.
 */
public abstract class AbstractInfo {

	protected int refreshRate;
	protected int switchInterval;
	protected boolean showToAll;
	protected String[] texts;

	/**
	 * Get the refresh rate for updating information.
	 *
	 * @return The refresh rate in ticks.
	 */
	public int getRefreshRate() {
		return refreshRate;
	}

	/**
	 * Get the switch interval for displaying different texts.
	 *
	 * @return The switch interval in ticks.
	 */
	public int getSwitchInterval() {
		return switchInterval;
	}

	/**
	 * Check if information should be shown to all players.
	 *
	 * @return True if information is shown to all players, otherwise only to
	 *         participants.
	 */
	public boolean isShowToAll() {
		return showToAll;
	}

	/**
	 * Get an array of information texts.
	 *
	 * @return An array of information texts.
	 */
	public String[] getTexts() {
		return texts;
	}
}