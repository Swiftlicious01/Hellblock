package com.swiftlicious.hellblock.player;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.BorderHandler;
import com.swiftlicious.hellblock.listeners.AnimalHandler;
import com.swiftlicious.hellblock.listeners.FortressHandler;

public class UserData implements UserDataInterface {

	private final String name;
	private final UUID uuid;
	private final EarningData earningData;
	private final FishingStatistics statisticData;
	private final ChallengeData challengeData;
	private final HellblockData hellblockData;
	private final LocationCacheData locationCacheData;
	private final boolean isLocked;
	private final boolean unsafeLocation;
	private final boolean clearItems;

	protected transient BorderHandler borderTask;
	protected transient AnimalHandler animalSpawningTask;
	protected transient FortressHandler fortressSpawningTask;
	protected transient boolean wearingGlowstoneArmor, holdingGlowstoneTool;

	public UserData(String name, UUID uuid, EarningData earningData, FishingStatistics statisticData,
			ChallengeData challengeData, HellblockData hellblockData, LocationCacheData locationCacheData,
			boolean isLocked, boolean unsafeLocation, boolean clearItems) {
		this.name = name;
		this.uuid = uuid;
		this.earningData = earningData;
		this.statisticData = statisticData;
		this.challengeData = challengeData;
		this.hellblockData = hellblockData;
		this.locationCacheData = locationCacheData;
		this.isLocked = isLocked;
		this.unsafeLocation = unsafeLocation;
		this.clearItems = clearItems;
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
		private boolean unsafeLocation;
		private boolean clearItems;

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
		public Builder setUnsafeLocation(boolean unsafeLocation) {
			this.unsafeLocation = unsafeLocation;
			return this;
		}

		@Override
		public Builder setClearInventory(boolean clearItems) {
			this.clearItems = clearItems;
			return this;
		}

		@Override
		public Builder setData(PlayerData playerData) {
			this.isLocked = playerData.isLocked();
			this.unsafeLocation = playerData.inUnsafeLocation();
			this.clearItems = playerData.isClearingItems();
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
					this.hellblockData, this.locationCacheData, this.isLocked, this.unsafeLocation, this.clearItems);
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

	@Override
	public boolean inUnsafeLocation() {
		return this.unsafeLocation;
	}

	@Override
	public boolean isClearingInventory() {
		return this.clearItems;
	}

	public void showBorder() {
		this.borderTask = new BorderHandler(HellblockPlugin.getInstance(), this.uuid);
	}

	public void hideBorder() {
		if (this.borderTask != null) {
			this.borderTask.cancelBorderShowcase();
			this.borderTask = null;
		}
	}

	public void startSpawningAnimals() {
		this.animalSpawningTask = new AnimalHandler(HellblockPlugin.getInstance(), this.uuid);
	}

	public void stopSpawningAnimals() {
		if (this.animalSpawningTask != null) {
			this.animalSpawningTask.stopAnimalSpawning();
			this.animalSpawningTask = null;
		}
	}

	public void startSpawningFortressMobs() {
		this.fortressSpawningTask = new FortressHandler(HellblockPlugin.getInstance(), this.uuid);
	}

	public void stopSpawningFortressMobs() {
		if (this.fortressSpawningTask != null) {
			this.fortressSpawningTask.stopFortressSpawning();
			this.fortressSpawningTask = null;
		}
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
				.setLocationCacheData(this.locationCacheData).setName(this.name).build();
	}
}