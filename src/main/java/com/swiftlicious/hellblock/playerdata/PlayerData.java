package com.swiftlicious.hellblock.playerdata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.SerializedName;

public class PlayerData {

	@SerializedName("name")
	protected String name;
	@SerializedName("trade")
	protected EarningData earningData;

	public static PlayerData LOCKED = empty();

	public static Builder builder() {
		return new Builder();
	}

	public static PlayerData empty() {
		return new Builder().setEarningData(EarningData.empty()).build();
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
		public Builder setEarningData(@Nullable EarningData earningData) {
			this.playerData.earningData = earningData;
			return this;
		}

		@NotNull
		public PlayerData build() {
			return this.playerData;
		}
	}

	public EarningData getEarningData() {
		return earningData;
	}

	public String getName() {
		return name;
	}

	public boolean isLocked() {
		return this == LOCKED;
	}
}
