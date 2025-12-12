package com.swiftlicious.hellblock.player;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.player.UserData.Builder;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;

/**
 * Interface representing user data. This interface provides methods for
 * accessing and managing user-related information, including earnings,
 * statistics, challenges, and more.
 * 
 * Implementations of this interface serve as a central data container for
 * player-related operations and persistence.
 */
public interface UserDataInterface {

	/**
	 * Retrieves the name of the user.
	 * 
	 * @return the user's name, never null
	 */
	@NotNull
	String getName();

	/**
	 * Retrieves the unique UUID of the user.
	 * 
	 * @return the user's UUID, never null
	 */
	@NotNull
	UUID getUUID();

	/**
	 * Retrieves the associated online player instance, if available.
	 * 
	 * @return the Player instance or null if offline
	 */
	@Nullable
	Player getPlayer();

	/**
	 * Returns the serialized version of this user's data. This is used for
	 * compatibility and migration purposes.
	 *
	 * @return the data version number
	 */
	int getVersion();

	/**
	 * Gets the user's earning data, including balances and earnings-related
	 * information.
	 * 
	 * @return the EarningData object, never null
	 */
	@NotNull
	EarningData getEarningData();

	/**
	 * Gets the user's fishing statistics.
	 * 
	 * @return the FishingStatistics object, never null
	 */
	@NotNull
	FishingStatistics getStatisticData();

	/**
	 * Gets the user's challenge progress and data.
	 * 
	 * @return the ChallengeData object, never null
	 */
	@NotNull
	ChallengeData getChallengeData();

	/**
	 * Gets the user's Hellblock-specific data.
	 * 
	 * @return the HellblockData object, never null
	 */
	@NotNull
	HellblockData getHellblockData();

	/**
	 * Gets the cached location data for the user.
	 * 
	 * @return the LocationCacheData object, never null
	 */
	@NotNull
	LocationCacheData getLocationCacheData();

	/**
	 * Checks if the user is currently online.
	 * 
	 * @return true if the user is online, false otherwise
	 */
	boolean isOnline();

	/**
	 * Indicates whether the user is locked from certain operations.
	 * 
	 * @return true if the user is locked, false otherwise
	 */
	boolean isLocked();

	/**
	 * Retrieves the user's mailbox entries.
	 * 
	 * @return a list of MailboxEntry objects, never null
	 */
	@NotNull
	List<MailboxEntry> getMailbox();

	/**
	 * Gets the user's notification settings.
	 * 
	 * @return the NotificationSettings object
	 */
	NotificationSettings getNotificationSettings();

	/**
	 * Converts the current user data into a PlayerData instance for storage or
	 * manipulation.
	 * 
	 * @return the PlayerData object representing this user
	 */
	@NotNull
	PlayerData toPlayerData();

	/**
	 * Creates a new builder for constructing or modifying UserData instances.
	 * 
	 * @return a new UserData builder
	 */
	static Builder builder() {
		return new UserData.Builder();
	}

	/**
	 * Builder interface for constructing UserData instances in a flexible and
	 * readable manner.
	 */
	interface BuilderInterface {

		/**
		 * Sets the name for the user.
		 * 
		 * @param name the user's name
		 * @return the Builder instance
		 */
		Builder setName(String name);

		/**
		 * Sets the UUID for the user.
		 * 
		 * @param uuid the user's UUID
		 * @return the Builder instance
		 */
		Builder setUUID(UUID uuid);

		/**
		 * Sets the data version for the user.
		 * 
		 * @param version the user's data version
		 * @return the Builder instance
		 */
		Builder setVersion(int version);

		/**
		 * Sets the earning data for the user.
		 * 
		 * @param earningData the EarningData to set
		 * @return the Builder instance
		 */
		Builder setEarningData(EarningData earningData);

		/**
		 * Sets the fishing statistics for the user.
		 * 
		 * @param statisticData the FishingStatistics to set
		 * @return the Builder instance
		 */
		Builder setStatisticData(FishingStatistics statisticData);

		/**
		 * Sets the challenge data for the user.
		 * 
		 * @param challengeData the ChallengeData to set
		 * @return the Builder instance
		 */
		Builder setChallengeData(ChallengeData challengeData);

		/**
		 * Sets the Hellblock data for the user.
		 * 
		 * @param hellblockData the HellblockData to set
		 * @return the Builder instance
		 */
		Builder setHellblockData(HellblockData hellblockData);

		/**
		 * Sets the location cache data for the user.
		 * 
		 * @param locationCacheData the LocationCacheData to set
		 * @return the Builder instance
		 */
		Builder setLocationCacheData(LocationCacheData locationCacheData);

		/**
		 * Sets whether the user is locked.
		 * 
		 * @param isLocked true if the user should be locked
		 * @return the Builder instance
		 */
		Builder setLocked(boolean isLocked);

		/**
		 * Sets the mailbox entries for the user.
		 * 
		 * @param mailbox a list of MailboxEntry objects
		 * @return the Builder instance
		 */
		Builder setMailbox(List<MailboxEntry> mailbox);

		/**
		 * Sets the notification settings for the user.
		 * 
		 * @param notifications the NotificationSettings to set
		 * @return the Builder instance
		 */
		Builder setNotificationSettings(NotificationSettings notifications);

		/**
		 * Initializes the builder with an existing PlayerData object.
		 * 
		 * @param playerData the PlayerData to copy from
		 * @return the Builder instance
		 */
		Builder setData(PlayerData playerData);

		/**
		 * Builds and returns a new UserData instance with the configured properties.
		 * 
		 * @return a new UserData object
		 */
		UserData build();
	}
}