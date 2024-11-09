package com.swiftlicious.hellblock.player;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerData {

	@Expose
	@SerializedName("name")
	protected String name;
	@Expose
	@SerializedName("pistons")
	protected List<String> pistonLocations;
	@Expose
	@SerializedName("levelblocks")
	protected List<String> levelBlockLocations;
	@Expose
	@SerializedName("trade")
	protected EarningData earningData;
	@Expose
	@SerializedName("hellblock")
	protected HellblockData hellblockData;
	@Expose
	@SerializedName("challenges")
	protected ChallengeData challengeData;

	public static PlayerData LOCKED = empty();

	public static Builder builder() {
		return new Builder();
	}

	public static PlayerData empty() {
		return new Builder().setName("").setLevelBlockLocations(new ArrayList<>()).setPistonLocations(new ArrayList<>())
				.setEarningData(EarningData.empty()).setHellblockData(HellblockData.empty())
				.setChallengeData(ChallengeData.empty()).build();
	}

	public static class Builder {

		private final PlayerData playerData;

		public Builder() {
			this.playerData = new PlayerData();
		}

		@NotNull
		public Builder setName(@Nullable String name) {
			this.playerData.name = name;
			return this;
		}

		@NotNull
		public Builder setLevelBlockLocations(@Nullable List<String> levelBlockLocations) {
			this.playerData.levelBlockLocations = levelBlockLocations;
			return this;
		}

		@NotNull
		public Builder setPistonLocations(@Nullable List<String> pistonLocations) {
			this.playerData.pistonLocations = pistonLocations;
			return this;
		}

		@NotNull
		public Builder setEarningData(@Nullable EarningData earningData) {
			this.playerData.earningData = earningData;
			return this;
		}

		@NotNull
		public Builder setHellblockData(@Nullable HellblockData hellblockData) {
			this.playerData.hellblockData = hellblockData;
			return this;
		}

		@NotNull
		public Builder setChallengeData(@Nullable ChallengeData challengeData) {
			this.playerData.challengeData = challengeData;
			return this;
		}

		@NotNull
		public PlayerData build() {
			return this.playerData;
		}
	}

	public EarningData getEarningData() {
		return this.earningData;
	}

	public HellblockData getHellblockData() {
		return this.hellblockData;
	}

	public ChallengeData getChallengeData() {
		return this.challengeData;
	}

	public String getName() {
		return this.name;
	}

	public List<String> getPistonLocations() {
		return this.pistonLocations;
	}

	public void setPistonLocations(List<String> pistonLocations) {
		this.pistonLocations = pistonLocations;
	}

	public List<String> getLevelBlockLocations() {
		return this.levelBlockLocations;
	}

	public void setLevelBlockLocations(List<String> levelBlockLocations) {
		this.levelBlockLocations = levelBlockLocations;
	}

	public boolean isLocked() {
		return this == LOCKED;
	}
}
