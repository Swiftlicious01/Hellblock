package com.swiftlicious.hellblock.loot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import static java.util.Objects.requireNonNull;

public class Loot implements LootInterface {

	private final LootType type;
	private final boolean disableStatistics;
	private final boolean showInFinder;
	private final boolean preventGrabbing;
	private final String id;
	private final String nick;
	private final StatisticsKeys statisticsKeys;
	private final String[] groups;
	private final LootBaseEffect lootBaseEffect;
	private final MathValue<Player> toInventory;
	private final Map<String, TextValue<Player>> customData;

	public Loot(LootType type, boolean disableStatistics, boolean showInFinder, boolean preventGrabbing, String id,
			String nick, StatisticsKeys statisticsKeys, String[] groups, LootBaseEffect lootBaseEffect,
			MathValue<Player> toInventory, Map<String, TextValue<Player>> customData) {
		this.type = type;
		this.disableStatistics = disableStatistics;
		this.showInFinder = showInFinder;
		this.id = id;
		this.nick = nick;
		this.statisticsKeys = statisticsKeys;
		this.groups = groups;
		this.lootBaseEffect = lootBaseEffect;
		this.preventGrabbing = preventGrabbing;
		this.toInventory = toInventory;
		this.customData = customData;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public LootType type() {
		return type;
	}

	@NotNull
	@Override
	public String nick() {
		return nick;
	}

	@Override
	public StatisticsKeys statisticKey() {
		return statisticsKeys;
	}

	@Override
	public boolean showInFinder() {
		return showInFinder;
	}

	@Override
	public boolean preventGrabbing() {
		return preventGrabbing;
	}

	@Override
	public MathValue<Player> toInventory() {
		return toInventory;
	}

	@Override
	public boolean disableStats() {
		return disableStatistics;
	}

	@Override
	public String[] lootGroup() {
		return groups;
	}

	@Override
	public LootBaseEffect baseEffect() {
		return lootBaseEffect;
	}

	@Override
	public Map<String, TextValue<Player>> customData() {
		return customData;
	}

	public static class Builder implements BuilderInterface {

		private LootType type = DEFAULT_TYPE;
		private boolean disableStatistics = LootInterface.DefaultProperties.DEFAULT_DISABLE_STATS;
		private boolean showInFinder = LootInterface.DefaultProperties.DEFAULT_SHOW_IN_FINDER;
		private boolean preventGrabbing = false;
		private String id = null;
		private String nick = "UNDEFINED";
		private StatisticsKeys statisticsKeys = null;
		private String[] groups = new String[0];
		private LootBaseEffect lootBaseEffect = null;
		private MathValue<Player> toInventory = MathValue.plain(0);
		private final Map<String, TextValue<Player>> customData = new LinkedHashMap<>();

		@Override
		public Builder type(LootType type) {
			this.type = type;
			return this;
		}

		@Override
		public Builder preventGrabbing(boolean preventGrabbing) {
			this.preventGrabbing = preventGrabbing;
			return this;
		}

		@Override
		public Builder disableStatistics(boolean disableStatistics) {
			this.disableStatistics = disableStatistics;
			return this;
		}

		@Override
		public Builder showInFinder(boolean showInFinder) {
			this.showInFinder = showInFinder;
			return this;
		}

		@Override
		public Builder id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public Builder nick(String nick) {
			this.nick = nick;
			return this;
		}

		@Override
		public Builder statisticsKeys(StatisticsKeys statisticsKeys) {
			this.statisticsKeys = statisticsKeys;
			return this;
		}

		@Override
		public Builder groups(String[] groups) {
			this.groups = groups;
			return this;
		}

		@Override
		public Builder lootBaseEffect(LootBaseEffect lootBaseEffect) {
			this.lootBaseEffect = lootBaseEffect;
			return this;
		}

		@Override
		public Builder customData(Map<String, TextValue<Player>> customData) {
			this.customData.putAll(customData);
			return this;
		}

		@Override
		public Builder toInventory(MathValue<Player> toInventory) {
			this.toInventory = toInventory;
			return this;
		}

		@Override
		public Loot build() {
			return new Loot(type, disableStatistics, showInFinder, preventGrabbing, requireNonNull(id),
					Optional.ofNullable(nick).orElse(id),
					Optional.ofNullable(statisticsKeys).orElse(new StatisticsKeys(id, id)), groups,
					requireNonNull(lootBaseEffect), toInventory, customData);
		}
	}
}