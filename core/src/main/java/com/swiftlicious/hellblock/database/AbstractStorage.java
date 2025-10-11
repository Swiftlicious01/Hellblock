package com.swiftlicious.hellblock.database;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * An abstract class that implements the {@link DataStorageProvider} and provides
 * common functionality for data storage.
 */
public abstract class AbstractStorage implements DataStorageProvider {

	protected HellblockPlugin plugin;

	public AbstractStorage(HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void initialize(YamlDocument config) {
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
	public void updateManyPlayersData(Collection<? extends UserData> users, boolean unlock) {
		users.forEach((UserData user) -> this.updatePlayerData(user.getUUID(), user.toPlayerData(), unlock));
	}

	@Override
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