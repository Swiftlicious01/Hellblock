package com.swiftlicious.hellblock.player;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.player.UserData.Builder;

/**
 * Interface representing user data. This interface provides methods for
 * accessing and managing user-related information.
 */
public interface UserDataInterface {

	/**
	 * Retrieves the username.
	 *
	 * @return the username as a {@link String}
	 */
	@NotNull
	String getName();

	/**
	 * Retrieves the user's UUID.
	 *
	 * @return the UUID as a {@link UUID}
	 */
	@NotNull
	UUID getUUID();

	/**
	 * Retrieves the {@link Player} instance if the player is online.
	 *
	 * @return the {@link Player} instance, or null if the player is offline
	 */
	@Nullable
	Player getPlayer();

	/**
	 * Retrieves the player's earning data.
	 *
	 * @return the {@link EarningData}
	 */
	@NotNull
	EarningData getEarningData();

	/**
	 * Retrieves the player's statistic data.
	 *
	 * @return the {@link FishingStatistics}
	 */
	@NotNull
	FishingStatistics getStatisticData();

	/**
	 * Retrieves the player's challenge data.
	 *
	 * @return the {@link ChallengeData}
	 */
	@NotNull
	ChallengeData getChallengeData();

	/**
	 * Retrieves the player's hellblock data.
	 *
	 * @return the {@link HellblockData}
	 */
	@NotNull
	HellblockData getHellblockData();

	/**
	 * Retrieves the player's location cache data.
	 *
	 * @return the {@link LocationCacheData}
	 */
	@NotNull
	LocationCacheData getLocationCacheData();

	/**
	 * Checks if the user is online on the current server.
	 *
	 * @return true if the user is online, false otherwise
	 */
	boolean isOnline();

	/**
	 * Checks if the data is locked.
	 *
	 * @return true if the data is locked, false otherwise
	 */
	boolean isLocked();

	/**
	 * Checks if location is unsafe.
	 *
	 * @return true if the location is unsafe, false otherwise
	 */
	boolean inUnsafeLocation();

	/**
	 * Checks if the inventory should be cleared.
	 *
	 * @return true if the inventory should be cleared, false otherwise
	 */
	boolean isClearingInventory();

	/**
	 * Converts the user data to a minimized format that can be saved.
	 *
	 * @return the {@link PlayerData}
	 */
	@NotNull
	PlayerData toPlayerData();

	/**
	 * Creates a new {@link Builder} instance to construct {@link UserData}.
	 *
	 * @return a new {@link Builder} instance
	 */
	static Builder builder() {
		return new UserData.Builder();
	}

	/**
	 * Builder interface for constructing instances of {@link UserData}.
	 */
	interface BuilderInterface {

		/**
		 * Sets the username for the {@link UserData} being built.
		 *
		 * @param name the username to set
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setName(String name);

		/**
		 * Sets the UUID for the {@link UserData} being built.
		 *
		 * @param uuid the UUID to set
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setUUID(UUID uuid);

		/**
		 * Sets the earning data for the {@link UserData} being built.
		 *
		 * @param earningData the {@link EarningData} to set
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setEarningData(EarningData earningData);

		/**
		 * Sets the statistic data for the {@link UserData} being built.
		 *
		 * @param statisticData the {@link FishingStatistics} to set
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setStatisticData(FishingStatistics statisticData);

		/**
		 * Sets the challenge data for the {@link UserData} being built.
		 *
		 * @param challengeData the {@link ChallengeData} to set
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setChallengeData(ChallengeData challengeData);

		/**
		 * Sets the hellblock data for the {@link UserData} being built.
		 *
		 * @param hellblockData the {@link HellblockData} to set
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setHellblockData(HellblockData hellblockData);

		/**
		 * Sets the location cache data for the {@link UserData} being built.
		 *
		 * @param locationCacheData the {@link LocationCacheData} to set
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setLocationCacheData(LocationCacheData locationCacheData);

		/**
		 * Sets whether the data is locked for the {@link UserData} being built.
		 *
		 * @param isLocked true if the data should be locked, false otherwise
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setLocked(boolean isLocked);

		/**
		 * Sets whether the location is unsafe for the {@link UserData} being built.
		 *
		 * @param unsafeLocation true if the location is unsafe, false otherwise
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setUnsafeLocation(boolean unsafeLocation);

		/**
		 * Sets whether the inventory should be cleared for the {@link UserData} being
		 * built.
		 *
		 * @param clearItems true if the items should be cleared, false otherwise
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setClearInventory(boolean clearItems);

		/**
		 * Sets the player data for the {@link UserData} being built.
		 *
		 * @param playerData the {@link PlayerData} to set
		 * @return the current {@link Builder} instance for method chaining
		 */
		Builder setData(PlayerData playerData);

		/**
		 * Builds and returns the {@link UserData} instance based on the current state
		 * of the {@link Builder}.
		 *
		 * @return the constructed {@link UserData} instance
		 */
		UserData build();
	}
}