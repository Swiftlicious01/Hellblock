package com.swiftlicious.hellblock.database;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * An abstract class that implements the {@link DataStorageProvider} and
 * provides common functionality for data storage.
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

	@Override
	public void invalidateCache(UUID uuid) {
		// Overriden based to invalidate each type of cache
	}

	@Override
	public void clearCache() {
		// Overriden based to clear each type of cache
	}

	@Override
	public void invalidateIslandCache(int islandId) {
		// Overriden based to invalidate island cache
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
	public CompletableFuture<Boolean> updateManyPlayersData(Collection<? extends UserData> users, boolean unlock) {
		if (users == null || users.isEmpty()) {
			return CompletableFuture.completedFuture(true);
		}

		List<CompletableFuture<Boolean>> futures = users.stream()
				.map(user -> updatePlayerData(user.getUUID(), user.toPlayerData(), unlock)).toList();

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
			// Check if all futures completed with 'true'
			for (CompletableFuture<Boolean> f : futures) {
				try {
					if (!f.get()) {
						return false;
					}
				} catch (InterruptedException | ExecutionException e) {
					throw new CompletionException(e);
				}
			}
			return true;
		});
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