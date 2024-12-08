package com.swiftlicious.hellblock.player;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.HellblockPlugin;

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
	@SerializedName("unsafe")
	protected boolean unsafeLocation;
	@Expose
	@SerializedName("clearinv")
	protected boolean clearItems;

	transient private UUID uuid;
	transient private boolean locked;
	transient private byte[] jsonBytes;

	public PlayerData(UUID uuid, String name, EarningData earningData, StatisticData statisticData,
			ChallengeData challengeData, HellblockData hellblockData, LocationCacheData locationCacheData,
			boolean isLocked, boolean unsafeLocation, boolean clearItems) {
		this.name = name;
		this.earningData = earningData;
		this.statisticData = statisticData;
		this.challengeData = challengeData;
		this.hellblockData = hellblockData;
		this.locationCacheData = locationCacheData;
		this.locked = isLocked;
		this.unsafeLocation = unsafeLocation;
		this.clearItems = clearItems;
		this.uuid = uuid;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static @NotNull PlayerData empty() {
		return new Builder().setName(DEFAULT_NAME).setUUID(new UUID(0, 0)).setLocked(false).setInUnsafeLocation(false)
				.setToClearItems(false).setLocationCacheData(DEFAULT_LOCATION).setEarningData(DEFAULT_EARNING)
				.setStatisticData(DEFAULT_STATISTIC).setHellblockData(DEFAULT_HELLBLOCK)
				.setChallengeData(DEFAULT_CHALLENGE).build();
	}

	public static class Builder {

		private String name = DEFAULT_NAME;
		private EarningData earningData = DEFAULT_EARNING;
		private StatisticData statisticData = DEFAULT_STATISTIC;
		private ChallengeData challengeData = DEFAULT_CHALLENGE;
		private HellblockData hellblockData = DEFAULT_HELLBLOCK;
		private LocationCacheData locationCacheData = DEFAULT_LOCATION;
		private boolean isLocked = false;
		private boolean unsafeLocation = false;
		private boolean clearItems = false;
		private UUID uuid;

		@NotNull
		public Builder setName(@NotNull String name) {
			this.name = name;
			return this;
		}

		@NotNull
		public Builder setUUID(@NotNull UUID uuid) {
			this.uuid = uuid;
			return this;
		}

		@NotNull
		public Builder setLocked(boolean locked) {
			this.isLocked = locked;
			return this;
		}

		@NotNull
		public Builder setInUnsafeLocation(boolean unsafe) {
			this.unsafeLocation = unsafe;
			return this;
		}

		@NotNull
		public Builder setToClearItems(boolean clear) {
			this.clearItems = clear;
			return this;
		}

		@NotNull
		public Builder setEarningData(@Nullable EarningData earningData) {
			this.earningData = earningData;
			return this;
		}

		@NotNull
		public Builder setStatisticData(@Nullable StatisticData statisticData) {
			this.statisticData = statisticData;
			return this;
		}

		@NotNull
		public Builder setChallengeData(@Nullable ChallengeData challengeData) {
			this.challengeData = challengeData;
			return this;
		}

		@NotNull
		public Builder setHellblockData(@Nullable HellblockData hellblockData) {
			this.hellblockData = hellblockData;
			return this;
		}

		@NotNull
		public Builder setLocationCacheData(@Nullable LocationCacheData locationCacheData) {
			this.locationCacheData = locationCacheData;
			return this;
		}

		@NotNull
		public PlayerData build() {
			return new PlayerData(Objects.requireNonNull(this.uuid), this.name, this.earningData, this.statisticData,
					this.challengeData, this.hellblockData, this.locationCacheData, this.isLocked, this.unsafeLocation,
					this.clearItems);
		}
	}

	/**
	 * Converts the player data to a byte array for saving purposes.
	 * 
	 * @return the player data as a byte array.
	 */
	public byte[] toBytes() {
		if (this.jsonBytes == null) {
			this.jsonBytes = HellblockPlugin.getInstance().getStorageManager().toBytes(this);
		}
		return this.jsonBytes;
	}

	/**
	 * Gets the earnings data for the player.
	 *
	 * @return the earnings data.
	 */
	public @NotNull EarningData getEarningData() {
		return this.earningData;
	}

	/**
	 * Gets the statistic data for the player.
	 *
	 * @return the statistic data.
	 */
	public @NotNull StatisticData getStatisticData() {
		return this.statisticData;
	}

	/**
	 * Gets the challenge data for the player.
	 *
	 * @return the challenge data.
	 */
	public @NotNull ChallengeData getChallengeData() {
		return this.challengeData;
	}

	/**
	 * Gets the hellblock data for the player.
	 *
	 * @return the hellblock data.
	 */
	public @NotNull HellblockData getHellblockData() {
		return this.hellblockData;
	}

	/**
	 * Gets the location cache data for the player.
	 *
	 * @return the location cache data.
	 */
	public @NotNull LocationCacheData getLocationCacheData() {
		return this.locationCacheData;
	}

	/**
	 * Gets the name of the player.
	 *
	 * @return the player's name.
	 */
	public @NotNull String getName() {
		return this.name;
	}

	/**
	 * Gets if the location is unsafe
	 *
	 * @return unsafe or not
	 */
	public boolean inUnsafeLocation() {
		return this.unsafeLocation;
	}

	/**
	 * Gets if the inventory is cleared
	 *
	 * @return cleared or not
	 */
	public boolean isClearingItems() {
		return this.clearItems;
	}

	/**
	 * Set if the location is unsafe
	 *
	 * @param unsafe unsafe or not
	 */
	public void setInUnsafeLocation(boolean unsafe) {
		this.unsafeLocation = unsafe;
	}

	/**
	 * Set if the inventory is cleared
	 *
	 * @param clear cleared or not
	 */
	public void setToClearItems(boolean clear) {
		this.clearItems = clear;
	}

	/**
	 * Gets if the data is locked
	 *
	 * @return locked or not
	 */
	public boolean isLocked() {
		return this.locked;
	}

	/**
	 * Set if the data is locked
	 *
	 * @param locked locked or not
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/**
	 * Gets the uuid
	 *
	 * @return uuid
	 */
	public @NotNull UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Set the uuid of the data
	 *
	 * @param uuid uuid
	 */
	public void setUUID(@NotNull UUID uuid) {
		this.uuid = uuid;
	}
}