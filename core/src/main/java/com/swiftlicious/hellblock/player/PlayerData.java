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

public class PlayerData {

	public static final String DEFAULT_NAME = "";
	public static final EarningData DEFAULT_EARNING = EarningData.empty();
	public static final StatisticData DEFAULT_STATISTIC = StatisticData.empty();
	public static final ChallengeData DEFAULT_CHALLENGE = ChallengeData.empty();
	public static final HellblockData DEFAULT_HELLBLOCK = HellblockData.empty();
	public static final LocationCacheData DEFAULT_LOCATION = LocationCacheData.empty();

	@Expose
	@SerializedName("name")
	protected String name;
	@Expose
	@SerializedName("trade")
	protected EarningData earningData;
	@Expose
	@SerializedName("statistics")
	protected StatisticData statisticData;
	@Expose
	@SerializedName("challenges")
	protected ChallengeData challengeData;
	@Expose
	@SerializedName("hellblock")
	protected HellblockData hellblockData;
	@Expose
	@SerializedName("cachedlocations")
	protected LocationCacheData locationCacheData;

	@Expose
	@SerializedName("mailbox")
	protected List<MailboxEntry> mailbox = new ArrayList<>();

	@Expose
	@SerializedName("joinnotifications")
	protected boolean joinNotifications;
	@Expose
	@SerializedName("invitenotifications")
	protected boolean inviteNotifications;

	@Expose
	@SerializedName("lastactivity")
	protected long lastActivity;

	private transient UUID uuid;
	private transient boolean locked;
	private transient byte[] jsonBytes;

	public PlayerData(UUID uuid, String name, EarningData earningData, StatisticData statisticData,
			ChallengeData challengeData, HellblockData hellblockData, LocationCacheData locationCacheData,
			boolean isLocked, List<MailboxEntry> mailbox, boolean joinNotifications, boolean inviteNotifications,
			long lastActivity) {
		this.name = name;
		this.earningData = earningData;
		this.statisticData = statisticData;
		this.challengeData = challengeData;
		this.hellblockData = hellblockData;
		this.locationCacheData = locationCacheData;
		this.locked = isLocked;
		this.mailbox = mailbox != null ? mailbox : new ArrayList<>();
		this.joinNotifications = joinNotifications;
		this.inviteNotifications = inviteNotifications;
		this.lastActivity = lastActivity;
		this.uuid = uuid;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static @NotNull PlayerData empty() {
		return new Builder().setName(DEFAULT_NAME).setUUID(new UUID(0, 0)).setLastActivity(0L)
				.setHellblockInviteNotifications(true).setHellblockJoinNotifications(true).setLocked(false)
				.setMailbox(new ArrayList<>()).setLocationCacheData(DEFAULT_LOCATION).setEarningData(DEFAULT_EARNING)
				.setStatisticData(DEFAULT_STATISTIC).setHellblockData(DEFAULT_HELLBLOCK)
				.setChallengeData(DEFAULT_CHALLENGE).build();
	}

	public static class Builder {
		private String name;
		private EarningData earningData;
		private StatisticData statisticData;
		private ChallengeData challengeData;
		private HellblockData hellblockData;
		private LocationCacheData locationCacheData;
		private boolean isLocked;
		private List<MailboxEntry> mailbox = new ArrayList<>();
		private boolean joinNotifications;
		private boolean inviteNotifications;
		private long lastActivity;
		private UUID uuid;

		public Builder setName(@NotNull String name) {
			this.name = name;
			return this;
		}

		public Builder setUUID(@NotNull UUID uuid) {
			this.uuid = uuid;
			return this;
		}

		public Builder setLocked(boolean locked) {
			this.isLocked = locked;
			return this;
		}

		public Builder setMailbox(List<MailboxEntry> mailbox) {
			this.mailbox = mailbox;
			return this;
		}

		public Builder setHellblockJoinNotifications(boolean joinNotifications) {
			this.joinNotifications = joinNotifications;
			return this;
		}

		public Builder setHellblockInviteNotifications(boolean inviteNotifications) {
			this.inviteNotifications = inviteNotifications;
			return this;
		}

		public Builder setLastActivity(long lastActivity) {
			this.lastActivity = lastActivity;
			return this;
		}

		public Builder setEarningData(@Nullable EarningData earningData) {
			this.earningData = earningData;
			return this;
		}

		public Builder setStatisticData(@Nullable StatisticData statisticData) {
			this.statisticData = statisticData;
			return this;
		}

		public Builder setChallengeData(@Nullable ChallengeData challengeData) {
			this.challengeData = challengeData;
			return this;
		}

		public Builder setHellblockData(@Nullable HellblockData hellblockData) {
			this.hellblockData = hellblockData;
			return this;
		}

		public Builder setLocationCacheData(@Nullable LocationCacheData locationCacheData) {
			this.locationCacheData = locationCacheData;
			return this;
		}

		public PlayerData build() {
			return new PlayerData(Objects.requireNonNull(this.uuid), this.name, this.earningData, this.statisticData,
					this.challengeData, this.hellblockData, this.locationCacheData, this.isLocked, this.mailbox,
					this.joinNotifications, this.inviteNotifications, this.lastActivity);
		}
	}

	@NotNull
	public List<MailboxEntry> getMailbox() {
		return this.mailbox;
	}

	public void setMailbox(List<MailboxEntry> mailbox) {
		this.mailbox = mailbox;
	}

	public boolean hasHellblockJoinNotifications() {
		return this.joinNotifications;
	}

	public boolean hasHellblockInviteNotifications() {
		return this.inviteNotifications;
	}

	public void setHellblockJoinNotifications(boolean joinNotifications) {
		this.joinNotifications = joinNotifications;
	}

	public void setHellblockInviteNotifications(boolean inviteNotifications) {
		this.inviteNotifications = inviteNotifications;
	}

	public long getLastActivity() {
		return this.lastActivity;
	}

	public void setLastActivity(long lastActivity) {
		this.lastActivity = lastActivity;
	}

	public boolean isLocked() {
		return this.locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	@NotNull
	public UUID getUUID() {
		return this.uuid;
	}

	public void setUUID(@NotNull UUID uuid) {
		this.uuid = uuid;
	}

	@NotNull
	public String getName() {
		return this.name;
	}

	@NotNull
	public EarningData getEarningData() {
		return this.earningData;
	}

	@NotNull
	public StatisticData getStatisticData() {
		return this.statisticData;
	}

	@NotNull
	public ChallengeData getChallengeData() {
		return this.challengeData;
	}

	@NotNull
	public HellblockData getHellblockData() {
		return this.hellblockData;
	}

	@NotNull
	public LocationCacheData getLocationCacheData() {
		return this.locationCacheData;
	}

	public byte[] toBytes() {
		if (this.jsonBytes == null) {
			this.jsonBytes = HellblockPlugin.getInstance().getStorageManager().toBytes(this);
		}
		return this.jsonBytes;
	}
}