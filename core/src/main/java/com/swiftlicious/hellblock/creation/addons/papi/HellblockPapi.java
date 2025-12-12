package com.swiftlicious.hellblock.creation.addons.papi;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.CoolDownManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.StringUtils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class HellblockPapi extends PlaceholderExpansion {

	private final HellblockPlugin plugin;
	private final Cache<UUID, Optional<PlayerData>> offlineDataCache;

	public HellblockPapi(HellblockPlugin plugin) {
		this.plugin = plugin;
		this.offlineDataCache = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();
	}

	public void load() {
		super.register();
	}

	public void unload() {
		super.unregister();
	}

	@SuppressWarnings("deprecation")
	@Override
	public @NotNull String getIdentifier() {
		return plugin.getDescription().getName() + "_islandstats";
	}

	@SuppressWarnings("deprecation")
	@Override
	public @NotNull String getAuthor() {
		return plugin.getDescription().getAuthors().getFirst();
	}

	@SuppressWarnings("deprecation")
	@Override
	public @NotNull String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
		final String[] split = params.split("_");

		UUID targetUUID = offlinePlayer.getUniqueId();
		if (split.length >= 3) {
			Player another = Bukkit.getPlayer(split[2]);
			if (another != null) {
				targetUUID = another.getUniqueId();
			}
		}

		switch (split[0]) {
		case "random" -> {
			return String.valueOf(RandomUtils.generateRandomDouble(0, 1));
		}
		case "debug" -> {
			return debugHellblockData(targetUUID) == null ? "" : debugHellblockData(targetUUID);
		}
		case "id" -> {
			return getHellblockId(targetUUID) == -1 ? "" : String.valueOf(getHellblockId(targetUUID));
		}
		case "level" -> {
			return getHellblockLevel(targetUUID) == -1.0F ? "" : String.valueOf(getHellblockLevel(targetUUID));
		}
		case "reset_cooldown", "island_reset_cooldown", "biome_cooldown", "island_biome_cooldown", "transfer_cooldown",
				"island_transfer_cooldown" -> {
			final String result = switch (split[0]) {
			case "reset_cooldown", "island_reset_cooldown" -> getCooldownValue(targetUUID, CooldownType.RESET);
			case "biome_cooldown", "island_biome_cooldown" -> getCooldownValue(targetUUID, CooldownType.BIOME);
			case "transfer_cooldown", "island_transfer_cooldown" -> getCooldownValue(targetUUID, CooldownType.TRANSFER);
			default -> null;
			};
			return result == null ? "" : result;
		}
		case "island_choice", "choice" -> {
			return getHellblockChoice(targetUUID) == null ? ""
					: StringUtils.toCamelCase(getHellblockChoice(targetUUID).toString());
		}
		case "island_owner", "owner" -> {
			return getHellblockOwnerName(targetUUID) == null ? "" : getHellblockOwnerName(targetUUID);
		}
		case "island_schematic", "schematic" -> {
			return getHellblockSchematic(targetUUID) == null ? ""
					: StringUtils.toCamelCase(getHellblockSchematic(targetUUID));
		}
		case "hopper_limit", "protection_range", "max_party_size" -> {
			final int result = switch (split[0]) {
			case "hopper_limit" -> getUpgradeLimit(targetUUID, UpgradeType.HOPPER_LIMIT);
			case "protection_range" -> getUpgradeLimit(targetUUID, UpgradeType.PROTECTION_RANGE);
			case "max_party_size" -> getUpgradeLimit(targetUUID, UpgradeType.PARTY_SIZE);
			default -> -1;
			};
			return result == -1 ? "" : String.valueOf(result);
		}
		case "island_locked", "locked_status" -> {
			return String.valueOf(getHellblockLockedStatus(targetUUID));
		}
		case "island_abandoned", "abandoned_status" -> {
			return String.valueOf(getHellblockAbandonedStatus(targetUUID));
		}
		case "biome" -> {
			return getHellblockBiome(targetUUID) == null ? ""
					: StringUtils.toCamelCase(getHellblockBiome(targetUUID).toString());
		}
		case "created", "creation_time" -> {
			return getHellblockCreationTime(targetUUID) == null ? "" : getHellblockCreationTime(targetUUID);
		}
		case "overall_visits", "total_visits", "daily_visits", "weekly_visits", "monthly_visits" -> {
			final int result = switch (split[0]) {
			case "overall_visits", "total_visits" -> getVisitStat(targetUUID, VisitStatType.OVERALL);
			case "daily_visits" -> getVisitStat(targetUUID, VisitStatType.DAILY);
			case "weekly_visits" -> getVisitStat(targetUUID, VisitStatType.WEEKLY);
			case "monthly_visits" -> getVisitStat(targetUUID, VisitStatType.MONTHLY);
			default -> -1;
			};
			return result == -1 ? "" : String.valueOf(result);
		}
		case "party_count", "trusted_count", "banned_count" -> {
			final int result = switch (split[0]) {
			case "party_count" -> getMemberListCount(targetUUID, MemberListType.PARTY);
			case "trusted_count" -> getMemberListCount(targetUUID, MemberListType.TRUSTED);
			case "banned_count" -> getMemberListCount(targetUUID, MemberListType.BANNED);
			default -> -1;
			};
			return result == -1 ? "" : String.valueOf(result);
		}
		case "market" -> {
			if (split.length < 2)
				return null;
			switch (split[1]) {
			case "limit" -> {
				Player target = Bukkit.getPlayer(targetUUID);
				if (target != null) {
					return "%.2f".formatted(plugin.getMarketManager().earningLimit(Context.player(target)));
				}
				return "";
			}
			case "earnings" -> {
				Double earnings = getEarnings(targetUUID);
				return earnings == null ? "" : "%.2f".formatted(earnings);
			}
			case "canearn" -> {
				Player target = Bukkit.getPlayer(targetUUID);
				if (target != null) {
					Double earnings = getEarnings(targetUUID);
					if (earnings == null)
						return "";
					double limit = plugin.getMarketManager().earningLimit(Context.player(target));
					return "%.2f".formatted(limit - earnings);
				}
				return "";
			}
			}
		}
		}
		return null;
	}

	@Nullable
	private String debugHellblockData(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().toString();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().toString()).orElse(null);
	}

	private boolean getHellblockLockedStatus(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().isLocked();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().isLocked()).orElse(false);
	}

	private boolean getHellblockAbandonedStatus(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().isAbandoned();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().isAbandoned()).orElse(false);
	}

	private int getHellblockId(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().getIslandId();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().getIslandId()).orElse(-1);
	}

	private float getHellblockLevel(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().getIslandLevel();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().getIslandLevel()).orElse(-1.0F);
	}

	@Nullable
	private String getHellblockOwnerName(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().getResolvedOwnerName();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().getResolvedOwnerName()).orElse(null);
	}

	@Nullable
	private String getHellblockCreationTime(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().getCreationTimeFormatted();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().getCreationTimeFormatted()).orElse(null);
	}

	@Nullable
	private String getHellblockSchematic(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().getUsedSchematic();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().getUsedSchematic()).orElse(null);
	}

	@Nullable
	private IslandOptions getHellblockChoice(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().getIslandChoice();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().getIslandChoice()).orElse(null);
	}

	@Nullable
	private HellBiome getHellblockBiome(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getHellblockData().getBiome();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getHellblockData().getBiome()).orElse(null);
	}

	private int getMemberListCount(@NotNull UUID uuid, @NotNull MemberListType type) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			HellblockData hellblockData = online.get().getHellblockData();
			return switch (type) {
			case PARTY -> hellblockData.getPartyMembers().size();
			case TRUSTED -> hellblockData.getTrustedMembers().size();
			case BANNED -> hellblockData.getBannedMembers().size();
			};
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		if (offline.isPresent()) {
			HellblockData hellblockData = offline.get().getHellblockData();
			return switch (type) {
			case PARTY -> hellblockData.getPartyMembers().size();
			case TRUSTED -> hellblockData.getTrustedMembers().size();
			case BANNED -> hellblockData.getBannedMembers().size();
			};
		}
		return -1;
	}

	private int getUpgradeLimit(@NotNull UUID uuid, @NotNull UpgradeType type) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			HellblockData hellblockData = online.get().getHellblockData();
			return switch (type) {
			case PARTY_SIZE -> hellblockData.getMaxPartySize();
			case PROTECTION_RANGE -> hellblockData.getMaxProtectionRange();
			case HOPPER_LIMIT -> hellblockData.getMaxHopperLimit();
			};
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		if (offline.isPresent()) {
			HellblockData hellblockData = offline.get().getHellblockData();
			return switch (type) {
			case PARTY_SIZE -> hellblockData.getMaxPartySize();
			case PROTECTION_RANGE -> hellblockData.getMaxProtectionRange();
			case HOPPER_LIMIT -> hellblockData.getMaxHopperLimit();
			};
		}
		return -1;
	}

	@Nullable
	private String getCooldownValue(@NotNull UUID uuid, @NotNull CooldownType type) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		CoolDownManager cooldownManager = plugin.getCooldownManager();
		if (online.isPresent()) {
			HellblockData hellblockData = online.get().getHellblockData();
			return switch (type) {
			case RESET -> cooldownManager.getFormattedCooldown(hellblockData.getResetCooldown());
			case BIOME -> cooldownManager.getFormattedCooldown(hellblockData.getBiomeCooldown());
			case TRANSFER -> cooldownManager.getFormattedCooldown(hellblockData.getTransferCooldown());
			};
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		if (offline.isPresent()) {
			HellblockData hellblockData = offline.get().getHellblockData();
			return switch (type) {
			case RESET -> cooldownManager.getFormattedCooldown(hellblockData.getResetCooldown());
			case BIOME -> cooldownManager.getFormattedCooldown(hellblockData.getBiomeCooldown());
			case TRANSFER -> cooldownManager.getFormattedCooldown(hellblockData.getTransferCooldown());
			};
		}
		return null;
	}

	private int getVisitStat(@NotNull UUID uuid, @NotNull VisitStatType type) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			VisitData visitData = online.get().getHellblockData().getVisitData();
			return switch (type) {
			case OVERALL -> visitData.getTotalVisits();
			case DAILY -> visitData.getDailyVisits();
			case WEEKLY -> visitData.getWeeklyVisits();
			case MONTHLY -> visitData.getMonthlyVisits();
			};
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		if (offline.isPresent()) {
			VisitData visitData = offline.get().getHellblockData().getVisitData();
			return switch (type) {
			case OVERALL -> visitData.getTotalVisits();
			case DAILY -> visitData.getDailyVisits();
			case WEEKLY -> visitData.getWeeklyVisits();
			case MONTHLY -> visitData.getMonthlyVisits();
			};
		}
		return -1;
	}

	@Nullable
	private Double getEarnings(@NotNull UUID uuid) {
		Optional<UserData> online = plugin.getStorageManager().getOnlineUser(uuid);
		if (online.isPresent()) {
			return online.get().getEarningData().getEarnings();
		}
		Optional<PlayerData> offline = loadOfflineData(uuid);
		return offline.map(data -> data.getEarningData().getEarnings()).orElse(null);
	}

	@NotNull
	private Optional<PlayerData> loadOfflineData(@NotNull UUID uuid) {
		return offlineDataCache.get(uuid, (id) -> {
			final CompletableFuture<Optional<PlayerData>> future = plugin.getStorageManager().getDataSource()
					.getPlayerData(uuid, false, Runnable::run);
			try {
				return future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return Optional.empty();
			}
		});
	}

	private enum VisitStatType {
		OVERALL, DAILY, WEEKLY, MONTHLY;
	}

	private enum MemberListType {
		PARTY, TRUSTED, BANNED;
	}

	private enum CooldownType {
		RESET, BIOME, TRANSFER;
	}

	private enum UpgradeType {
		PARTY_SIZE, PROTECTION_RANGE, HOPPER_LIMIT;
	}
}