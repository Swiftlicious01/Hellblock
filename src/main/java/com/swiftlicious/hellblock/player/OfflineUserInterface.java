package com.swiftlicious.hellblock.player;

import java.util.List;
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
	 * Get the player's hellblock data
	 *
	 * @return hellblock data
	 */
	HellblockData getHellblockData();

	/**
	 * Get the player's challenge data
	 *
	 * @return challenge data
	 */
	ChallengeData getChallengeData();

	/**
	 * Get the player's cached piston locations
	 *
	 * @return piston locations
	 */
	List<String> getPistonLocations();

	/**
	 * Get the player's cached level block locations
	 *
	 * @return level block locations
	 */
	List<String> getLevelBlockLocations();

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