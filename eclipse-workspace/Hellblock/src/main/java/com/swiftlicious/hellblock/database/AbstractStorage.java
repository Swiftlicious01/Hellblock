package com.swiftlicious.hellblock.database;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.OfflineUser;
import com.swiftlicious.hellblock.playerdata.PlayerData;

/**
 * An abstract class that implements the DataStorageInterface and provides
 * common functionality for data storage.
 */
public abstract class AbstractStorage implements DataStorageInterface {

	protected HellblockPlugin instance;

	public AbstractStorage(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void initialize() {
		// This method can be overridden in subclasses to perform initialization tasks
		// specific to the storage type.
	}

	@Override
	public void disable() {
		// This method can be overridden in subclasses to perform cleanup or shutdown
		// tasks specific to the storage type.
	}

	/**
	 * Get the current time in seconds since the Unix epoch.
	 *
	 * @return The current time in seconds.
	 */
	public int getCurrentSeconds() {
		return (int) Instant.now().getEpochSecond();
	}

	@Override
	public void updateManyPlayersData(Collection<? extends OfflineUser> users, boolean unlock) {
		// Update data for multiple players by iterating through the collection of
		// OfflineUser objects.
		for (OfflineUser user : users) {
			this.updatePlayerData(user.getUUID(), user.getPlayerData(), unlock);
		}
	}

	/**
	 * Lock or unlock player data based on the provided UUID and lock flag.
	 *
	 * @param uuid The UUID of the player.
	 * @param lock True to lock the player data, false to unlock it.
	 */
	public void lockOrUnlockPlayerData(UUID uuid, boolean lock) {
		// Note: Only remote database would override this method
	}

	@Override
	public CompletableFuture<Boolean> updateOrInsertPlayerData(UUID uuid, PlayerData playerData, boolean unlock) {
		// By default, delegate to the updatePlayerData method to update or insert
		// player data.
		return updatePlayerData(uuid, playerData, unlock);
	}
}
