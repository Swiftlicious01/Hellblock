package com.swiftlicious.hellblock.database;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

/**
 * Interface for managing storage.
 */
public interface StorageManagerInterface extends Reloadable {

	/**
	 * Retrieves the server ID.
	 *
	 * @return the server ID as a String
	 */
	@NotNull
	String getServerID();

	/**
	 * Retrieves the user data for an online user by their UUID.
	 *
	 * @param uuid the UUID of the user
	 * @return an {@link Optional} containing the {@link UserData} if the user is
	 *         online, or empty if not
	 */
	@NotNull
	Optional<UserData> getOnlineUser(UUID uuid);

	/**
	 * Retrieves a collection of all online users.
	 *
	 * @return a collection of {@link UserData} for all online users
	 */
	@NotNull
	Collection<UserData> getOnlineUsers();

	/**
	 * Cache all island owner user data.
	 * 
	 * @return the preloading tasks to complete.
	 */
	CompletableFuture<Void> preloadCachedIslandOwners();

	/**
	 * Retrieves the cached user data from either online or offline if not possible.
	 * 
	 * @param uuid the UUID of the user
	 * @return an {@link Optional} containing the {@link UserData} if the user is
	 *         found.
	 */
	@NotNull
	Optional<UserData> getCachedUserData(UUID uuid);

	/**
	 * Attempts to load a {@link UserData} from cache or offline storage.
	 *
	 * @param uuid The UUID of the user to load.
	 * @param lock Whether to lock the data on retrieval.
	 * @return A {@link CompletableFuture} containing the {@link Optional} of
	 *         {@link UserData}.
	 */
	@NotNull
	CompletableFuture<Optional<UserData>> getCachedUserDataWithFallback(UUID uuid, boolean lock);

	/**
	 * Invalidate cache user data on certain events.
	 * 
	 * @param uuid the UUID of the user
	 */
	void invalidateCachedUserData(@NotNull UUID uuid);

	/**
	 * Retrieves the user data for an offline user by their UUID.
	 *
	 * @param uuid the UUID of the user
	 * @param lock whether to lock the user data for exclusive access
	 * @return a {@link CompletableFuture} containing an {@link Optional} with the
	 *         user data, or empty if not found
	 */
	CompletableFuture<Optional<UserData>> getOfflineUserData(UUID uuid, boolean lock);

	/**
	 * Retrieves the user data for an offline user by their island ID.
	 *
	 * @param islandId the islandId of the data to retrieve
	 * @param lock     whether to lock the user data for exclusive access
	 * @return a {@link CompletableFuture} containing an {@link Optional} with the
	 *         user data, or empty if not found
	 */
	CompletableFuture<Optional<UserData>> getOfflineUserDataByIslandId(int islandId, boolean lock);

	/**
	 * Saves the user data.
	 *
	 * @param userData the {@link UserData} to be saved
	 * @param unlock   whether to unlock the user data after saving
	 * @return a {@link CompletableFuture} containing a boolean indicating success
	 *         or failure
	 */
	CompletableFuture<Boolean> saveUserData(UserData userData, boolean unlock);

	/**
	 * Unlocks the user data.
	 *
	 * @param uuid the {@link UUID} to be unlocked
	 * @return a {@link CompletableFuture} when finished unlocking
	 */
	CompletableFuture<Void> unlockUserData(UUID uuid);

	/**
	 * Retrieves the data storage provider.
	 *
	 * @return the {@link DataStorageProvider} instance
	 */
	@NotNull
	DataStorageProvider getDataSource();

	/**
	 * Checks if Redis is enabled for data storage.
	 *
	 * @return true if Redis is enabled, false otherwise
	 */
	boolean isRedisEnabled();

	/**
	 * Converts {@link PlayerData} to a byte array.
	 *
	 * @param data the {@link PlayerData} to be converted
	 * @return the byte array representation of {@link PlayerData}
	 */
	byte[] toBytes(@NotNull PlayerData data);

	/**
	 * Converts {@link PlayerData} to JSON format.
	 *
	 * @param data the {@link PlayerData} to be converted
	 * @return the JSON string representation of {@link PlayerData}
	 */
	@NotNull
	String toJson(@NotNull PlayerData data);

	/**
	 * Converts a JSON string to {@link PlayerData}.
	 *
	 * @param json the JSON string to be converted
	 * @return the {@link PlayerData} object
	 */
	@NotNull
	PlayerData fromJson(String json);

	/**
	 * Converts a byte array to {@link PlayerData}.
	 *
	 * @param data the byte array to be converted
	 * @return the {@link PlayerData} object
	 */
	@NotNull
	PlayerData fromBytes(byte[] data);
}