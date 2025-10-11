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

import com.swiftlicious.hellblock.listeners.AnimalHandler;
import com.swiftlicious.hellblock.listeners.FortressHandler;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;

public class UserData implements UserDataInterface {

	private final String name;
	private final UUID uuid;
	private final EarningData earningData;
	private final FishingStatistics statisticData;
	private final ChallengeData challengeData;
	private final HellblockData hellblockData;
	private final LocationCacheData locationCacheData;
	private final boolean isLocked;
	private final List<MailboxEntry> mailbox;
	private final long lastActivity;
	private final boolean joinNotifications;
	private final boolean inviteNotifications;

	protected transient AnimalHandler animalSpawningTask;
	protected transient FortressHandler fortressSpawningTask;
	protected transient boolean wearingGlowstoneArmor;
	protected transient boolean holdingGlowstoneTool;

	public UserData(String name, UUID uuid, EarningData earningData, FishingStatistics statisticData,
			ChallengeData challengeData, HellblockData hellblockData, LocationCacheData locationCacheData,
			boolean isLocked, List<MailboxEntry> mailbox, boolean joinNotifications, boolean inviteNotifications,
			long lastActivity) {
		this.name = name;
		this.uuid = uuid;
		this.earningData = earningData;
		this.statisticData = statisticData;
		this.challengeData = challengeData;
		this.hellblockData = hellblockData;
		this.locationCacheData = locationCacheData;
		this.isLocked = isLocked;
		this.mailbox = mailbox != null ? mailbox : new ArrayList<>();
		this.joinNotifications = joinNotifications;
		this.inviteNotifications = inviteNotifications;
		this.lastActivity = lastActivity;
	}

	public static class Builder implements BuilderInterface {
		private String name;
		private UUID uuid;
		private EarningData earningData;
		private FishingStatistics statisticData;
		private ChallengeData challengeData;
		private HellblockData hellblockData;
		private LocationCacheData locationCacheData;
		private boolean isLocked;
		private List<MailboxEntry> mailbox = new ArrayList<>();
		private boolean joinNotifications;
		private boolean inviteNotifications;
		private long lastActivity;

		@Override
		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		@Override
		public Builder setUUID(UUID uuid) {
			this.uuid = uuid;
			return this;
		}

		@Override
		public Builder setEarningData(EarningData earningData) {
			this.earningData = earningData.copy();
			return this;
		}

		@Override
		public Builder setStatisticData(FishingStatistics statisticData) {
			this.statisticData = statisticData;
			return this;
		}

		@Override
		public Builder setChallengeData(ChallengeData challengeData) {
			this.challengeData = challengeData.copy();
			return this;
		}

		@Override
		public Builder setHellblockData(HellblockData hellblockData) {
			this.hellblockData = hellblockData.copy();
			return this;
		}

		@Override
		public Builder setLocationCacheData(LocationCacheData locationCacheData) {
			this.locationCacheData = locationCacheData.copy();
			return this;
		}

		@Override
		public Builder setLocked(boolean isLocked) {
			this.isLocked = isLocked;
			return this;
		}

		@Override
		public Builder setMailbox(List<MailboxEntry> mailbox) {
			this.mailbox = mailbox;
			return this;
		}

		@Override
		public Builder setHellblockJoinNotifications(boolean joinNotifications) {
			this.joinNotifications = joinNotifications;
			return this;
		}

		@Override
		public Builder setHellblockInviteNotifications(boolean inviteNotifications) {
			this.inviteNotifications = inviteNotifications;
			return this;
		}

		@Override
		public Builder updateLastActivity() {
			this.lastActivity = System.currentTimeMillis();
			return this;
		}

		@Override
		public Builder setData(PlayerData playerData) {
			this.isLocked = playerData.isLocked();
			this.mailbox = new ArrayList<>(playerData.getMailbox());
			this.lastActivity = playerData.getLastActivity();
			this.joinNotifications = playerData.hasHellblockJoinNotifications();
			this.inviteNotifications = playerData.hasHellblockInviteNotifications();
			this.uuid = playerData.getUUID();
			this.name = playerData.getName();
			this.earningData = playerData.getEarningData().copy();
			this.statisticData = FishingStatisticsInterface.builder()
					.amountMap(playerData.getStatisticData().getAmountMap())
					.sizeMap(playerData.getStatisticData().getSizeMap()).build();
			this.challengeData = playerData.getChallengeData().copy();
			this.hellblockData = playerData.getHellblockData().copy();
			this.locationCacheData = playerData.getLocationCacheData().copy();
			return this;
		}

		@Override
		public UserData build() {
			return new UserData(this.name, this.uuid, this.earningData, this.statisticData, this.challengeData,
					this.hellblockData, this.locationCacheData, this.isLocked, this.mailbox, this.joinNotifications,
					this.inviteNotifications, this.lastActivity);
		}
	}

	@NotNull
	@Override
	public String getName() {
		return this.name;
	}

	@NotNull
	@Override
	public UUID getUUID() {
		return this.uuid;
	}

	@Nullable
	@Override
	public Player getPlayer() {
		return Bukkit.getPlayer(this.uuid);
	}

	@NotNull
	@Override
	public EarningData getEarningData() {
		return this.earningData;
	}

	@NotNull
	@Override
	public FishingStatistics getStatisticData() {
		return this.statisticData;
	}

	@NotNull
	@Override
	public ChallengeData getChallengeData() {
		return this.challengeData;
	}

	@NotNull
	@Override
	public HellblockData getHellblockData() {
		return this.hellblockData;
	}

	@NotNull
	@Override
	public LocationCacheData getLocationCacheData() {
		return this.locationCacheData;
	}

	@Override
	public boolean isOnline() {
		return Optional.ofNullable(Bukkit.getPlayer(this.uuid)).map(OfflinePlayer::isOnline).orElse(false);
	}

	@Override
	public boolean isLocked() {
		return this.isLocked;
	}

	@NotNull
	@Override
	public List<MailboxEntry> getMailbox() {
		return this.mailbox;
	}

	@Override
	public boolean hasHellblockJoinNotifications() {
		return this.joinNotifications;
	}

	@Override
	public boolean hasHellblockInviteNotifications() {
		return this.inviteNotifications;
	}

	@Override
	public long getLastActivity() {
		return this.lastActivity;
	}

	public void startSpawningAnimals() {
		this.animalSpawningTask = new AnimalHandler(this.uuid);
	}

	public void stopSpawningAnimals() {
		if (this.animalSpawningTask == null) {
			return;
		}
		this.animalSpawningTask.stopAnimalSpawning();
		this.animalSpawningTask = null;
	}

	public void startSpawningFortressMobs() {
		this.fortressSpawningTask = new FortressHandler(this.uuid);
	}

	public void stopSpawningFortressMobs() {
		if (this.fortressSpawningTask == null) {
			return;
		}
		this.fortressSpawningTask.stopFortressSpawning();
		this.fortressSpawningTask = null;
	}

	public boolean hasGlowstoneArmorEffect() {
		return this.wearingGlowstoneArmor;
	}

	public boolean hasGlowstoneToolEffect() {
		return this.holdingGlowstoneTool;
	}

	public void isWearingGlowstoneArmor(boolean wearingGlowstoneArmor) {
		this.wearingGlowstoneArmor = wearingGlowstoneArmor;
	}

	public void isHoldingGlowstoneTool(boolean holdingGlowstoneTool) {
		this.holdingGlowstoneTool = holdingGlowstoneTool;
	}

	@NotNull
	@Override
	public PlayerData toPlayerData() {
		return PlayerData.builder().setUUID(this.uuid).setEarningData(this.earningData)
				.setStatisticData(new StatisticData(this.statisticData.amountMap(), this.statisticData.sizeMap()))
				.setChallengeData(this.challengeData).setHellblockData(this.hellblockData)
				.setLocationCacheData(this.locationCacheData).setMailbox(this.mailbox)
				.setHellblockJoinNotifications(this.joinNotifications)
				.setHellblockInviteNotifications(this.inviteNotifications).setName(this.name).build();
	}
}