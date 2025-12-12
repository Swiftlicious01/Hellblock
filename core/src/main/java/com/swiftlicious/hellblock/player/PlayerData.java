package com.swiftlicious.hellblock.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;

/**
 * Represents a container for storing all persistent player-related data.
 * <p>
 * Includes statistics, challenge progress, earning data, mailbox entries,
 * notification preferences, and more. Supports JSON serialization through Gson
 * and provides a builder for safe construction.
 */
public class PlayerData {

	// Default values for empty PlayerData initialization
	public static final String DEFAULT_NAME = "";
	public static final EarningData DEFAULT_EARNING = EarningData.empty();
	public static final StatisticData DEFAULT_STATISTIC = StatisticData.empty();
	public static final ChallengeData DEFAULT_CHALLENGE = ChallengeData.empty();
	public static final HellblockData DEFAULT_HELLBLOCK = HellblockData.empty();
	public static final LocationCacheData DEFAULT_LOCATION = LocationCacheData.empty();
	public static final NotificationSettings DEFAULT_NOTIFICATIONS = NotificationSettings.empty();

	/** The player's name (can be empty string, never null) */
	@Expose
	@SerializedName("name")
	protected String name;

	/** Earnings and financial data */
	@Expose
	@SerializedName("earningData")
	protected EarningData earningData;

	/** General player statistics */
	@Expose
	@SerializedName("statisticData")
	protected StatisticData statisticData;

	/** Challenge progress and achievements */
	@Expose
	@SerializedName("challengeData")
	protected ChallengeData challengeData;

	/** Hellblock-specific player data */
	@Expose
	@SerializedName("hellblockData")
	protected HellblockData hellblockData;

	/** Cached location information for the player */
	@Expose
	@SerializedName("cachedLocationData")
	protected LocationCacheData locationCacheData;

	/** Player's in-game mailbox entries */
	@Expose
	@SerializedName("mailboxEntries")
	protected List<MailboxEntry> mailbox;

	/** Notification preferences and settings */
	@Expose
	@SerializedName("notificationSettings")
	protected NotificationSettings notificationSettings;

	@Expose
	@SerializedName("dataVersion")
	private int version = CURRENT_VERSION;

	public static final int CURRENT_VERSION = 1;

	/** Unique identifier for the player (transient, not serialized) */
	private transient UUID uuid;

	/** Lock status used for restricting updates or actions (transient) */
	private transient boolean locked;

	/** Cached serialized byte representation of this object (transient) */
	private transient byte[] jsonBytes;

	/**
	 * Constructs a PlayerData instance with full initialization.
	 *
	 * @param uuid                 Unique player UUID
	 * @param name                 Player's name
	 * @param earningData          Player's earnings data
	 * @param statisticData        Player statistics
	 * @param challengeData        Challenge-related data
	 * @param hellblockData        Hellblock-specific data
	 * @param locationCacheData    Cached player location
	 * @param isLocked             Whether the player is locked
	 * @param mailbox              Player's mailbox entries
	 * @param notificationSettings Notification preferences
	 */
	public PlayerData(UUID uuid, String name, EarningData earningData, StatisticData statisticData,
			ChallengeData challengeData, HellblockData hellblockData, LocationCacheData locationCacheData,
			boolean isLocked, List<MailboxEntry> mailbox, NotificationSettings notificationSettings) {
		this.name = name;
		this.earningData = earningData;
		this.statisticData = statisticData;
		this.challengeData = challengeData;
		this.hellblockData = hellblockData;
		this.locationCacheData = locationCacheData;
		this.locked = isLocked;
		this.mailbox = mailbox;
		this.notificationSettings = notificationSettings;
		this.uuid = uuid;
	}

	/**
	 * Creates a new Builder for constructing a PlayerData object.
	 *
	 * @return a new PlayerData.Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Returns a PlayerData instance with all default values.
	 *
	 * @return an empty PlayerData instance
	 */
	public static @NotNull PlayerData empty() {
		return new Builder().setName(DEFAULT_NAME).setUUID(new UUID(0, 0))
				.setNotificationSettings(DEFAULT_NOTIFICATIONS).setLocked(false).setMailbox(new ArrayList<>())
				.setLocationCacheData(DEFAULT_LOCATION).setEarningData(DEFAULT_EARNING)
				.setStatisticData(DEFAULT_STATISTIC).setHellblockData(DEFAULT_HELLBLOCK)
				.setChallengeData(DEFAULT_CHALLENGE).build();
	}

	/**
	 * Builder class for safely constructing PlayerData instances.
	 */
	public static class Builder {
		private String name;
		private int version = CURRENT_VERSION;
		private EarningData earningData;
		private StatisticData statisticData;
		private ChallengeData challengeData;
		private HellblockData hellblockData;
		private LocationCacheData locationCacheData;
		private List<MailboxEntry> mailbox;
		private NotificationSettings notificationSettings;
		private boolean isLocked;
		private UUID uuid;

		/**
		 * Sets the player's name.
		 */
		public Builder setName(@NotNull String name) {
			this.name = name;
			return this;
		}

		/**
		 * Sets the player's UUID.
		 */
		public Builder setUUID(@NotNull UUID uuid) {
			this.uuid = uuid;
			return this;
		}

		/**
		 * Sets the data version for this player.
		 */
		public Builder setVersion(int version) {
			this.version = version;
			return this;
		}

		/**
		 * Sets whether the player is locked.
		 */
		public Builder setLocked(boolean locked) {
			this.isLocked = locked;
			return this;
		}

		/**
		 * Sets the earnings data.
		 */
		public Builder setEarningData(@Nullable EarningData earningData) {
			this.earningData = earningData;
			return this;
		}

		/**
		 * Sets the statistics data.
		 */
		public Builder setStatisticData(@Nullable StatisticData statisticData) {
			this.statisticData = statisticData;
			return this;
		}

		/**
		 * Sets the challenge data.
		 */
		public Builder setChallengeData(@Nullable ChallengeData challengeData) {
			this.challengeData = challengeData;
			return this;
		}

		/**
		 * Sets the Hellblock data.
		 */
		public Builder setHellblockData(@Nullable HellblockData hellblockData) {
			this.hellblockData = hellblockData;
			return this;
		}

		/**
		 * Sets the location cache data.
		 */
		public Builder setLocationCacheData(@Nullable LocationCacheData locationCacheData) {
			this.locationCacheData = locationCacheData;
			return this;
		}

		/**
		 * Sets the mailbox entries.
		 */
		public Builder setMailbox(@Nullable List<MailboxEntry> mailbox) {
			this.mailbox = mailbox;
			return this;
		}

		/**
		 * Sets the notification settings.
		 */
		public Builder setNotificationSettings(@Nullable NotificationSettings notificationSettings) {
			this.notificationSettings = notificationSettings;
			return this;
		}

		/**
		 * Builds and returns a new PlayerData instance.
		 */
		public PlayerData build() {
			PlayerData data = new PlayerData(Objects.requireNonNull(this.uuid), this.name, this.earningData,
					this.statisticData, this.challengeData, this.hellblockData, this.locationCacheData, this.isLocked,
					this.mailbox, this.notificationSettings);
			data.setVersion(this.version);
			return data;
		}
	}

	/** @return True if the player is currently locked */
	public boolean isLocked() {
		return this.locked;
	}

	/** @param locked Sets the player's locked status */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/** @return The player's UUID */
	@NotNull
	public UUID getUUID() {
		return this.uuid;
	}

	/** @param uuid Sets the player's UUID */
	public void setUUID(@NotNull UUID uuid) {
		this.uuid = uuid;
	}

	/** @return the version number */
	public int getVersion() {
		return this.version;
	}

	/** @param version the version number to assign */
	public void setVersion(int version) {
		this.version = version;
	}

	/** @return The player's name */
	@NotNull
	public String getName() {
		return this.name;
	}

	/** @return Earning data, or default if null */
	@NotNull
	public EarningData getEarningData() {
		if (this.earningData == null) {
			this.earningData = EarningData.empty();
		}
		return this.earningData;
	}

	/** @return Statistic data, or default if null */
	@NotNull
	public StatisticData getStatisticData() {
		if (this.statisticData == null) {
			this.statisticData = StatisticData.empty();
		}
		return this.statisticData;
	}

	/** @return Challenge data, or default if null */
	@NotNull
	public ChallengeData getChallengeData() {
		if (this.challengeData == null) {
			this.challengeData = ChallengeData.empty();
		}
		return this.challengeData;
	}

	/** @return Hellblock data, or default if null */
	@NotNull
	public HellblockData getHellblockData() {
		if (this.hellblockData == null) {
			this.hellblockData = HellblockData.empty();
		}
		return this.hellblockData;
	}

	/** @return Cached location data, or default if null */
	@NotNull
	public LocationCacheData getLocationCacheData() {
		if (this.locationCacheData == null) {
			this.locationCacheData = LocationCacheData.empty();
		}
		return this.locationCacheData;
	}

	/** @return Mailbox entries, or an empty list if null */
	@NotNull
	public List<MailboxEntry> getMailbox() {
		if (this.mailbox == null) {
			this.mailbox = new ArrayList<>();
		}
		return this.mailbox;
	}

	/** @return Notification settings, or default if null */
	@NotNull
	public NotificationSettings getNotificationSettings() {
		if (this.notificationSettings == null) {
			this.notificationSettings = NotificationSettings.empty();
		}
		return this.notificationSettings;
	}

	/**
	 * Serializes the PlayerData instance to a byte array using the storage manager.
	 * The result is cached after the first call.
	 *
	 * @return the serialized byte array
	 */
	public byte[] toBytes() {
		if (this.jsonBytes == null) {
			this.jsonBytes = HellblockPlugin.getInstance().getStorageManager().toBytes(this);
		}
		return this.jsonBytes;
	}
}