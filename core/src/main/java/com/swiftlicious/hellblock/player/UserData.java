package com.swiftlicious.hellblock.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;

/**
 * Represents a comprehensive data container for a user within the plugin
 * ecosystem.
 * <p>
 * This class implements {@link UserDataInterface} and provides immutable,
 * thread-safe storage of all relevant player data, including earnings,
 * statistics, challenges, location caching, and notifications. It also provides
 * transient runtime flags for special states (e.g., glowstone armor/tool
 * effects).
 */
public class UserData implements UserDataInterface {

	/** The user's in-game name. */
	private final String name;

	/** The unique identifier (UUID) for the user. */
	private final UUID uuid;

	/** The version of the serialized player data. */
	private final int version;

	/** The user's earnings and balance data. */
	private final EarningData earningData;

	/** The user's fishing statistics. */
	private final FishingStatistics statisticData;

	/** The user's challenge progress and completion data. */
	private final ChallengeData challengeData;

	/** The user's Hellblock-related data. */
	private final HellblockData hellblockData;

	/** Cached locations associated with the user. */
	private final LocationCacheData locationCacheData;

	/** The user's mailbox entries. */
	private final List<MailboxEntry> mailbox;

	/** The user's notification preferences and settings. */
	private final NotificationSettings notifications;

	/** Indicates if the user's data is locked from modification. */
	private final boolean isLocked;

	/** Whether the user is currently wearing glowstone armor (transient). */
	protected transient boolean wearingGlowstoneArmor;

	/** Whether the user is currently holding a glowstone tool (transient). */
	protected transient boolean holdingGlowstoneTool;

	/**
	 * Constructs a fully initialized {@code UserData} instance.
	 *
	 * @param name              the user's name
	 * @param uuid              the user's unique identifier
	 * @param version           the user's data version
	 * @param earningData       the user's earnings data
	 * @param statisticData     the user's fishing statistics
	 * @param challengeData     the user's challenge data
	 * @param hellblockData     the user's Hellblock data
	 * @param locationCacheData cached location data
	 * @param isLocked          whether the user is locked
	 * @param mailbox           the user's mailbox entries
	 * @param notifications     the user's notification settings
	 */
	public UserData(String name, UUID uuid, int version, EarningData earningData, FishingStatistics statisticData,
			ChallengeData challengeData, HellblockData hellblockData, LocationCacheData locationCacheData,
			boolean isLocked, List<MailboxEntry> mailbox, NotificationSettings notifications) {
		this.name = name;
		this.uuid = uuid;
		this.version = version;
		this.earningData = earningData;
		this.statisticData = statisticData;
		this.challengeData = challengeData;
		this.hellblockData = hellblockData;
		this.locationCacheData = locationCacheData;
		this.isLocked = isLocked;
		this.mailbox = mailbox;
		this.notifications = notifications;
	}

	/**
	 * Builder class for constructing {@link UserData} instances.
	 * <p>
	 * Provides a fluent, chainable interface for safely populating user data fields
	 * prior to final object creation.
	 */
	public static class Builder implements BuilderInterface {

		private String name;
		private UUID uuid;
		private int version = PlayerData.CURRENT_VERSION; // default to current
		private EarningData earningData;
		private FishingStatistics statisticData;
		private ChallengeData challengeData;
		private HellblockData hellblockData;
		private LocationCacheData locationCacheData;
		private boolean isLocked;
		private List<MailboxEntry> mailbox;
		private NotificationSettings notifications;

		/** {@inheritDoc} */
		@Override
		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setUUID(UUID uuid) {
			this.uuid = uuid;
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setVersion(int version) {
			this.version = version;
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setEarningData(EarningData earningData) {
			this.earningData = earningData.copy();
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setStatisticData(FishingStatistics statisticData) {
			this.statisticData = statisticData;
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setChallengeData(ChallengeData challengeData) {
			this.challengeData = challengeData.copy();
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setHellblockData(HellblockData hellblockData) {
			this.hellblockData = hellblockData.copy();
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setLocationCacheData(LocationCacheData locationCacheData) {
			this.locationCacheData = locationCacheData.copy();
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setLocked(boolean isLocked) {
			this.isLocked = isLocked;
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setMailbox(List<MailboxEntry> mailbox) {
			this.mailbox = mailbox;
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setNotificationSettings(NotificationSettings notifications) {
			this.notifications = notifications.copy();
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public Builder setData(PlayerData playerData) {
			this.isLocked = playerData.isLocked();
			this.mailbox = new ArrayList<>(playerData.getMailbox());
			this.notifications = playerData.getNotificationSettings().copy();
			this.uuid = playerData.getUUID();
			this.name = playerData.getName();
			this.version = playerData.getVersion();
			this.earningData = playerData.getEarningData().copy();
			this.statisticData = FishingStatisticsInterface.builder()
					.amountMap(playerData.getStatisticData().getAmountMap())
					.sizeMap(playerData.getStatisticData().getSizeMap()).build();
			this.challengeData = playerData.getChallengeData().copy();
			this.hellblockData = playerData.getHellblockData().copy();
			this.locationCacheData = playerData.getLocationCacheData().copy();
			return this;
		}

		/**
		 * Builds and returns a new {@link UserData} instance using the currently
		 * configured builder fields.
		 *
		 * @return a new immutable {@code UserData} instance
		 */
		@Override
		public UserData build() {
			return new UserData(this.name, this.uuid, this.version, this.earningData, this.statisticData,
					this.challengeData, this.hellblockData, this.locationCacheData, this.isLocked, this.mailbox,
					this.notifications);
		}
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public String getName() {
		return this.name;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public UUID getUUID() {
		return this.uuid;
	}

	/** {@inheritDoc} */
	@Nullable
	@Override
	public Player getPlayer() {
		return Bukkit.getPlayer(this.uuid);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version reflects the schema used when this user data was saved.
	 */
	@Override
	public int getVersion() {
		return this.version;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public EarningData getEarningData() {
		return this.earningData;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public FishingStatistics getStatisticData() {
		return this.statisticData;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public ChallengeData getChallengeData() {
		return this.challengeData;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public HellblockData getHellblockData() {
		return this.hellblockData;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public LocationCacheData getLocationCacheData() {
		return this.locationCacheData;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isOnline() {
		return Optional.ofNullable(Bukkit.getPlayer(this.uuid)).map(OfflinePlayer::isOnline).orElse(false);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isLocked() {
		return this.isLocked;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public List<MailboxEntry> getMailbox() {
		return this.mailbox;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public NotificationSettings getNotificationSettings() {
		return this.notifications;
	}

	/**
	 * Checks if the user currently has the glowstone armor effect applied.
	 *
	 * @return true if the user is wearing glowstone armor, false otherwise
	 */
	public boolean hasGlowstoneArmorEffect() {
		return this.wearingGlowstoneArmor;
	}

	/**
	 * Checks if the user currently has the glowstone tool effect applied.
	 *
	 * @return true if the user is holding a glowstone tool, false otherwise
	 */
	public boolean hasGlowstoneToolEffect() {
		return this.holdingGlowstoneTool;
	}

	/**
	 * Updates the flag for whether the user is wearing glowstone armor.
	 *
	 * @param wearingGlowstoneArmor true if the user is wearing glowstone armor
	 */
	public void isWearingGlowstoneArmor(boolean wearingGlowstoneArmor) {
		this.wearingGlowstoneArmor = wearingGlowstoneArmor;
	}

	/**
	 * Updates the flag for whether the user is holding a glowstone tool.
	 *
	 * @param holdingGlowstoneTool true if the user is holding a glowstone tool
	 */
	public void isHoldingGlowstoneTool(boolean holdingGlowstoneTool) {
		this.holdingGlowstoneTool = holdingGlowstoneTool;
	}

	/** {@inheritDoc} */
	@NotNull
	@Override
	public PlayerData toPlayerData() {
		return PlayerData.builder().setUUID(this.uuid).setVersion(this.version).setEarningData(this.earningData)
				.setStatisticData(new StatisticData(this.statisticData.amountMap(), this.statisticData.sizeMap()))
				.setChallengeData(this.challengeData).setHellblockData(this.hellblockData)
				.setLocationCacheData(this.locationCacheData).setMailbox(this.mailbox)
				.setNotificationSettings(this.notifications).setName(this.name).build();
	}
}