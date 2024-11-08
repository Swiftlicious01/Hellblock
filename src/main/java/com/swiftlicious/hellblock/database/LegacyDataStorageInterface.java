package com.swiftlicious.hellblock.database;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.swiftlicious.hellblock.player.PlayerData;

public interface LegacyDataStorageInterface extends DataStorageInterface {

	/**
	 * Retrieve legacy player data from the SQL database.
	 *
	 * @param uuid The UUID of the player.
	 * @return A CompletableFuture containing the optional legacy player data.
	 */
	CompletableFuture<Optional<PlayerData>> getLegacyPlayerData(UUID uuid);
}