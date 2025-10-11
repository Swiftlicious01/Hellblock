package com.swiftlicious.hellblock.creation.addons.papi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.FishingStatistics;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.StatisticData;
import com.swiftlicious.hellblock.player.UserData;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class StatisticsPapi extends PlaceholderExpansion {

	private final Cache<UUID, Optional<PlayerData>> offlineDataCache;

	private final HellblockPlugin plugin;

	public StatisticsPapi(HellblockPlugin plugin) {
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
		return plugin.getDescription().getName() + "_fishingstats";
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
	public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
		final Optional<UserData> onlineUser = plugin.getStorageManager().getOnlineUser(player.getUniqueId());
		final String[] split = params.split("_", 2);
		if (onlineUser.isPresent()) {
			final UserData data = onlineUser.get();
			final FishingStatistics statistics = data.getStatisticData();
			switch (split[0]) {
			case "total" -> {
				return String.valueOf(statistics.amountOfFishCaught());
			}
			case "hascaught" -> {
				if (split.length == 1) {
					return "Invalid format";
				}
				return String.valueOf(statistics.getAmount(split[1]) != 0);
			}
			case "amount" -> {
				if (split.length == 1) {
					return "Invalid format";
				}
				return String.valueOf(statistics.getAmount(split[1]));
			}
			case "size-record" -> {
				final float size = statistics.getMaxSize(split[1]);
				return "%.2f".formatted(size < 0 ? 0 : size);
			}
			case "category" -> {
				if (split.length == 1) {
					return "Invalid format";
				}
				final String[] categorySplit = split[1].split("_", 2);
				if (categorySplit.length == 1) {
					return "Invalid format";
				}
				final List<String> category = plugin.getStatisticsManager().getCategoryMembers(categorySplit[1]);
				if ("total".equals(categorySplit[0])) {
					int total = 0;
					for (String loot : category) {
						total += statistics.getAmount(loot);
					}
					return String.valueOf(total);
				} else if ("progress".equals(categorySplit[0])) {
					final int size = category.size();
					int unlocked = 0;
					for (String loot : category) {
						if (statistics.getAmount(loot) != 0) {
							unlocked++;
						}
					}
					final double percent = ((double) unlocked * 100) / size;
					final String progress = "%.1f".formatted(percent);
					return "100.0".equals(progress) ? "100" : progress;
				}
			}
			}
			return null;
		} else {
			final Optional<PlayerData> optional = offlineDataCache.get(player.getUniqueId(), (uuid) -> {
				final CompletableFuture<Optional<PlayerData>> data = plugin.getStorageManager().getDataSource()
						.getPlayerData(player.getUniqueId(), false, Runnable::run);
				try {
					return data.get();
				} catch (ExecutionException | InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
			if (optional.isPresent()) {
				final PlayerData playerData = optional.get();
				final StatisticData statistics = playerData.getStatisticData();
				switch (split[0]) {
				case "total" -> {
					final int total = statistics.amountMap.values().stream().mapToInt(Integer::intValue).sum();
					return String.valueOf(total);
				}
				case "hascaught" -> {
					if (split.length == 1) {
						return "Invalid format";
					}
					return String.valueOf(statistics.amountMap.getOrDefault(split[1], 0) != 0);
				}
				case "amount" -> {
					if (split.length == 1) {
						return "Invalid format";
					}
					return String.valueOf(statistics.amountMap.getOrDefault(split[1], 0));
				}
				case "size-record" -> {
					final float size = statistics.sizeMap.getOrDefault(split[1], 0f);
					return "%.2f".formatted(size < 0 ? 0 : size);
				}
				case "category" -> {
					if (split.length == 1) {
						return "Invalid format";
					}
					final String[] categorySplit = split[1].split("_", 2);
					if (categorySplit.length == 1) {
						return "Invalid format";
					}
					final List<String> category = plugin.getStatisticsManager().getCategoryMembers(categorySplit[1]);
					if ("total".equals(categorySplit[0])) {
						int total = 0;
						for (String loot : category) {
							total += statistics.amountMap.getOrDefault(loot, 0);
						}
						return String.valueOf(total);
					} else if ("progress".equals(categorySplit[0])) {
						final int size = category.size();
						int unlocked = 0;
						for (String loot : category) {
							if (statistics.amountMap.getOrDefault(loot, 0) != 0) {
								unlocked++;
							}
						}
						final double percent = ((double) unlocked * 100) / size;
						final String progress = "%.1f".formatted(percent);
						return "100.0".equals(progress) ? "100" : progress;
					}
				}
				}
				return null;
			} else {
				return "";
			}
		}
	}
}