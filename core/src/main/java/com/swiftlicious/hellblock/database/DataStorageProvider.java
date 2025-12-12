package com.swiftlicious.hellblock.database;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * Interface representing a provider for data storage.
 */
public interface DataStorageProvider {

	/**
	 * Initializes the data storage provider with the given configuration.
	 *
	 * @param config the {@link YamlDocument} configuration for the storage provider
	 */
	void initialize(YamlDocument config);

	/**
	 * Disables the data storage provider, performing any necessary cleanup.
	 */
	void disable();

	/**
	 * Invalidates memory cache for given UUID if used.
	 * 
	 * @param uuid the ID to invalidate.
	 */
	void invalidateCache(UUID uuid);

	/**
	 * Clears memory cache if used.
	 */
	void clearCache();

	/**
	 * Invalidates memory cache for given islandId if used.
	 * 
	 * @param islandId the ID to invalidate.
	 */
	void invalidateIslandCache(int islandId);

	/**
	 * Checks if an insert operation for the specified player UUID is currently in
	 * progress. This is typically true if the player’s data is being created or
	 * inserted into the database, and the operation has not yet completed.
	 *
	 * @param uuid the UUID of the player
	 * @return true if an insert is still pending, false otherwise
	 */
	boolean isPendingInsert(UUID uuid);

	/**
	 * Checks if a player data insert was recently completed for the specified UUID.
	 * "Recent" typically means within a small configurable time window (e.g., 5
	 * seconds), during which reloading or locking the data should be avoided to
	 * prevent race conditions.
	 *
	 * @param uuid the UUID of the player
	 * @return true if the last insert was recent enough to delay follow-up actions
	 */
	boolean isInsertStillRecent(UUID uuid);

	/**
	 * Returns the pending insert future for the specified UUID, if one exists.
	 * <p>
	 * This can be used to wait for a player's data insert to complete before
	 * proceeding with operations like caching or reading from the database,
	 * ensuring data consistency and avoiding race conditions.
	 * </p>
	 *
	 * @param uuid the UUID of the player whose insert operation to track
	 * @return a {@link CompletableFuture} that completes when the insert finishes,
	 *         or a completed future if no insert is currently pending
	 */
	CompletableFuture<Void> getInsertFuture(UUID uuid);

	/**
	 * Returns the elapsed time in milliseconds since the player's data was
	 * inserted, or -1 if no recent insert timestamp exists. Useful for logging or
	 * debugging insert timing.
	 *
	 * @param uuid the UUID of the player
	 * @return time in milliseconds since the last insert, or -1 if unavailable
	 */
	@Nullable
	Long getInsertAge(UUID uuid);

	/**
	 * Retrieves the type of storage used by this provider.
	 *
	 * @return the {@link StorageType} of this provider
	 */
	StorageType getStorageType();

	/**
	 * Retrieves the player data for the specified UUID.
	 *
	 * @param uuid     the UUID of the player
	 * @param lock     whether to lock the player data for exclusive access
	 * @param executor The executor, can be null
	 * @return a {@link CompletableFuture} containing an {@link Optional} with the
	 *         player data, or empty if not found
	 */
	CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor);

	/**
	 * Retrieves the player data for the specified islandId.
	 *
	 * @param islandId the ID of the island
	 * @param lock     whether to lock the player data for exclusive access
	 * @param executor The executor, can be null
	 * @return a {@link CompletableFuture} containing an {@link Optional} with the
	 *         player data, or empty if not found
	 */
	CompletableFuture<Optional<PlayerData>> getPlayerDataByIslandId(int islandId, boolean lock, Executor executor);

	/**
	 * Updates the player data for the specified UUID.
	 *
	 * @param uuid       the UUID of the player
	 * @param playerData the {@link PlayerData} to be updated
	 * @param unlock     whether to unlock the player data after updating
	 * @return a {@link CompletableFuture} containing a boolean indicating success
	 *         or failure
	 */
	CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean unlock);

	/**
	 * Updates or inserts the player data for the specified UUID.
	 *
	 * @param uuid       the UUID of the player
	 * @param playerData the {@link PlayerData} to be updated or inserted
	 * @param unlock     whether to unlock the player data after updating or
	 *                   inserting
	 * @return a {@link CompletableFuture} containing a boolean indicating success
	 *         or failure
	 */
	CompletableFuture<Boolean> updateOrInsertPlayerData(UUID uuid, PlayerData playerData, boolean unlock);

	/**
	 * Updates the data for multiple players.
	 *
	 * @param users  a collection of {@link UserData} to be updated
	 * @param unlock whether to unlock the player data after updating
	 * 
	 * @return a {@link CompletableFuture} containing a boolean indiciating success
	 *         or failure
	 */
	CompletableFuture<Boolean> updateManyPlayersData(Collection<? extends UserData> users, boolean unlock);

	/**
	 * Locks or unlocks the player data for the specified UUID.
	 *
	 * @param uuid the UUID of the player
	 * @param lock whether to lock (true) or unlock (false) the player data
	 */
	void lockOrUnlockPlayerData(UUID uuid, boolean lock);

	/**
	 * Retrieves the set of unique user UUIDs.
	 *
	 * @return a set of unique user UUIDs
	 */
	Set<UUID> getUniqueUsers();
}