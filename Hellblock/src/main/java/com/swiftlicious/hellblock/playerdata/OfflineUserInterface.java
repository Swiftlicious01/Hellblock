package com.swiftlicious.hellblock.playerdata;

import java.util.UUID;

public interface OfflineUserInterface {

	/**
	 * Get the username
	 *
	 * @return user name
	 */
	String getName();

	/**
	 * Get the user's uuid
	 *
	 * @return uuid
	 */
	UUID getUUID();

	/**
	 * Get the player's earning data
	 *
	 * @return earning data
	 */
	EarningData getEarningData();

	/**
	 * If the user is online on current server
	 *
	 * @return online or not
	 */
	boolean isOnline();

	/**
	 * Get the data in another minimized format that can be saved
	 *
	 * @return player data
	 */
	PlayerData getPlayerData();
}