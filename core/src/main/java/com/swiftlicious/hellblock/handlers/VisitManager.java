package com.swiftlicious.hellblock.handlers;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;

import net.kyori.adventure.sound.Sound;

public class VisitManager implements Reloadable {

	protected final HellblockPlugin instance;

	// visit cooldown & activity trackers
	private final Map<UUID, Map<UUID, Long>> visitCooldowns = new ConcurrentHashMap<>();
	private final Map<UUID, Deque<Long>> visitTimestamps = new ConcurrentHashMap<>();
	private final Map<UUID, Deque<UUID>> visitHistory = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> reciprocalVisitCount = new ConcurrentHashMap<>();
	private final Map<UUID, Set<String>> playerIPs = new ConcurrentHashMap<>();
	private final Map<UUID, List<VisitLogEntry>> visitLogs = new ConcurrentHashMap<>();

	// blacklisted players (bots, abusers, etc.)
	private final Set<UUID> blacklisted = ConcurrentHashMap.newKeySet();

	// constants
	private static final long VISIT_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(5);
	private static final long SPAM_WINDOW_MS = 60_000; // 1 minute
	private static final int SPAM_THRESHOLD = 10; // max visits/min
	private static final int MUTUAL_VISIT_THRESHOLD = 5; // rapid reciprocal visits
	private static final int ALT_THRESHOLD = 5; // unique IPs visiting same island/hour
	private static final long OUTLIER_MONITOR_INTERVAL = TimeUnit.MINUTES.toMillis(10);
	private static final int OUTLIER_THRESHOLD = 100; // >100 visits/hour
	private static final int NIGHT_START = 2; // 2 AM
	private static final int NIGHT_END = 5; // 5 AM
	private static final long VISIT_EXPIRY_MS = TimeUnit.DAYS.toMillis(7);

	private SchedulerTask visitResetTask;
	private SchedulerTask visitCleanupTask;
	private SchedulerTask outlierMonitorTask;
	private SchedulerTask featuredCleanupTask;

	public VisitManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		startVisitResetTask();
		startVisitCleanupTask();
		startOutlierMonitorTask();
		startFeaturedCleanupTask();
	}

	@Override
	public void unload() {
		if (visitResetTask != null && !visitResetTask.isCancelled()) {
			visitResetTask.cancel();
			visitResetTask = null;
		}
		if (visitCleanupTask != null && !visitCleanupTask.isCancelled()) {
			visitCleanupTask.cancel();
			visitCleanupTask = null;
		}
		if (outlierMonitorTask != null && !outlierMonitorTask.isCancelled()) {
			outlierMonitorTask.cancel();
			outlierMonitorTask = null;
		}
		if (featuredCleanupTask != null && !featuredCleanupTask.isCancelled()) {
			featuredCleanupTask.cancel();
			featuredCleanupTask = null;
		}
	}

	public void startVisitResetTask() {
		visitResetTask = instance.getScheduler().asyncRepeating(() -> {
			long now = System.currentTimeMillis();

			instance.getCoopManager().getCachedIslandOwnerData()
					.thenAccept(userDataSet -> userDataSet.forEach(userData -> {
						VisitData visitData = userData.getHellblockData().getVisitData();
						visitData.resetIfNeeded(now);
					}));
		}, 0L, 5, TimeUnit.MINUTES); // every 5 minutes
	}

	private void startOutlierMonitorTask() {
		outlierMonitorTask = instance.getScheduler().asyncRepeating(() -> {
			long cutoff = System.currentTimeMillis() - OUTLIER_MONITOR_INTERVAL;
			visitTimestamps.entrySet().forEach(entry -> {
				UUID player = entry.getKey();
				Deque<Long> times = entry.getValue();
				times.removeIf(t -> t < cutoff);

				if (times.size() >= OUTLIER_THRESHOLD) {
					autoBlacklist(player, "Abnormal visit frequency (> " + OUTLIER_THRESHOLD + " in 10 minutes)");
					times.clear();
				}
			});
		}, 0L, 5, TimeUnit.MINUTES); // run every 5 minutes
	}

	public void startVisitCleanupTask() {
		visitCleanupTask = instance.getScheduler().asyncRepeating(() -> {
			long cutoff = System.currentTimeMillis() - VISIT_EXPIRY_MS;

			instance.getCoopManager().getCachedIslandOwnerData().thenAccept(users -> users.forEach(user -> {
				HellblockData data = user.getHellblockData();
				data.getVisitData().resetIfNeeded(System.currentTimeMillis());
				data.cleanupOldVisitors(cutoff);
			}));
		}, 0L, 5, TimeUnit.MINUTES); // every 5 minutes
	}

	public void startFeaturedCleanupTask() {
		featuredCleanupTask = instance.getScheduler()
				.asyncRepeating(() -> instance.getCoopManager().getCachedIslandOwnerData().thenAccept(users -> {
					long now = System.currentTimeMillis();
					users.forEach(user -> {
						HellblockData data = user.getHellblockData();
						VisitData visitData = data.getVisitData();

						if (visitData.getFeaturedUntil() > 0 && visitData.getFeaturedUntil() <= now) {
							visitData.removeFeatured();
						}
					});
				}), 0L, 5, TimeUnit.MINUTES);
	}

	/**
	 * Checks if a player can visit a specific island (owner UUID).
	 */
	public boolean canVisit(@NotNull UUID visitor, @NotNull UUID islandOwner) {
		if (blacklisted.contains(visitor)) {
			return false;
		}

		Map<UUID, Long> cooldowns = visitCooldowns.get(visitor);
		if (cooldowns == null)
			return true;

		Long lastVisit = cooldowns.get(islandOwner);
		return lastVisit == null || System.currentTimeMillis() - lastVisit >= VISIT_COOLDOWN_MS;
	}

	public void handleVisit(@NotNull Player visitor, @Nullable UUID ownerUUID) {
		if (ownerUUID == null) {
			throw new IllegalStateException("OwnerUUID was null on handleVisit.");
		}

		HellblockPlugin instance = HellblockPlugin.getInstance();

		CompletableFuture<Optional<UserData>> visitorFuture = instance.getStorageManager()
				.getOfflineUserData(visitor.getUniqueId(), instance.getConfigManager().lockData());

		CompletableFuture<Optional<UserData>> ownerFuture = instance.getStorageManager().getOfflineUserData(ownerUUID,
				instance.getConfigManager().lockData());

		CompletableFuture<Optional<Pair<UserData, UserData>>> combinedFuture = visitorFuture
				.thenCombineAsync(ownerFuture, (visitorOpt, ownerOpt) -> {
					if (visitorOpt.isEmpty() || ownerOpt.isEmpty()) {
						return Optional.empty();
					}
					return Optional.of(Pair.of(visitorOpt.get(), ownerOpt.get()));
				});

		combinedFuture.thenAcceptAsync(optionalPair -> optionalPair.ifPresent(pair -> {
			UserData visitorUser = pair.left();
			UserData ownerUser = pair.right();
			HellblockData ownerData = ownerUser.getHellblockData();
			if (ownerData.isLocked()) {
				instance.getSenderFactory().wrap(visitor).sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LOCKED_FROM_VISITORS
								.arguments(AdventureHelper.miniMessage(ownerUser.getName())).build()));
				return;
			}
			if (ownerData.getBanned().contains(visitorUser.getUUID())) {
				instance.getSenderFactory().wrap(visitor)
						.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY
								.arguments(AdventureHelper.miniMessage(ownerUser.getName())).build()));
				return;
			}
			boolean associated = ownerUUID.equals(visitorUser.getUUID())
					|| ownerData.getParty().contains(visitorUser.getUUID())
					|| ownerData.getTrusted().contains(visitorUser.getUUID());
			boolean eligibleToRecord = !associated && !isBlacklisted(visitorUser.getUUID())
					&& canVisit(visitorUser.getUUID(), ownerUUID);
			Location warp = ownerData.getVisitData().getWarpLocation();
			if (warp == null)
				warp = ownerData.getHomeLocation();
			if (warp == null) {
				instance.getPluginLogger().severe("Null home location in handleVisit for " + ownerUser.getName() + " ("
						+ ownerUser.getUUID() + ")");
				instance.getSenderFactory().wrap(visitor).sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION.build()));
				return;
			}
			final Location finalWarp = warp;
			instance.getWorldManager().ensureHellblockWorldLoaded(ownerData.getID()).thenAcceptAsync(world -> {
				Location corrected = new Location(world.bukkitWorld(), finalWarp.getX(), finalWarp.getY(),
						finalWarp.getZ(), finalWarp.getYaw(), finalWarp.getPitch());
				LocationUtils.isSafeLocationAsync(corrected).thenAcceptAsync(isSafe -> {
					if (!isSafe) {
						instance.getSenderFactory().wrap(visitor).sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_UNSAFE_TO_VISIT.build()));
						return;
					}
					ChunkUtils.teleportAsync(visitor, corrected, TeleportCause.PLUGIN).thenRun(() -> {
						instance.debug(visitor.getName() + " (" + visitor.getUniqueId() + ") visited "
								+ ownerUser.getName() + " (" + ownerUser.getUUID() + ")");
						if (eligibleToRecord) {
							recordVisit(visitor, ownerUUID);
						}
						ownerData.sendDisplayTextTo(visitor);
						instance.getSenderFactory().wrap(visitor).sendMessage(
								instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_VISIT_ENTRY
										.arguments(AdventureHelper.miniMessage(ownerUser.getName())).build()));
					});
				});
			});
		}));
	}

	public void recordVisit(@NotNull Player visitor, @NotNull UUID islandOwner) {
		final UUID visitorId = visitor.getUniqueId();

		if (isBlacklisted(visitorId))
			return;
		if (!canVisit(visitorId, islandOwner))
			return;

		long now = System.currentTimeMillis();

		// 1. Detect visit spam
		checkVisitSpam(visitorId, now);

		// 2. Detect mutual visit boosting
		checkMutualVisitBoosting(visitorId, islandOwner);

		// 3. Detect mass alt visiting (many IPs to same island)
		checkMassAltVisiting(visitor, islandOwner);

		// 4. Detect night-hour repetitive activity
		checkNightActivity(visitorId, now);

		// Normal cooldown tracking
		visitCooldowns.computeIfAbsent(visitorId, __ -> new HashMap<>()).put(islandOwner, now);

		// Track visit for statistical data
		visitTimestamps.computeIfAbsent(visitorId, __ -> new ArrayDeque<>()).addLast(now);
		visitHistory.computeIfAbsent(visitorId, __ -> new ArrayDeque<>()).addLast(islandOwner);

		// Normal visit progression
		instance.getStorageManager().getOfflineUserData(islandOwner, instance.getConfigManager().lockData())
				.thenAccept(optUser -> {
					if (optUser.isEmpty()) {
						return;
					}

					UserData userData = optUser.get();
					HellblockData hb = userData.getHellblockData();

					VisitData visitData = hb.getVisitData();
					visitData.increment();
					hb.addVisitor(visitorId);
				});
	}

	/**
	 * Returns top islands by a selected metric (daily, weekly, total, etc.)
	 */
	public void getTopIslands(Function<VisitData, Integer> visitType, int limit, Consumer<List<VisitEntry>> callback) {
		instance.getCoopManager().getCachedIslandOwnerData().thenAcceptAsync(userDataSet -> {
			List<VisitEntry> list = new ArrayList<>();

			userDataSet.forEach(userData -> {
				HellblockData hb = userData.getHellblockData();
				VisitData visitData = hb.getVisitData();
				if (visitData != null) {
					int count = visitType.apply(visitData);
					if (count > 0) {
						list.add(new VisitEntry(userData.getUUID(), count));
					}
				}
			});

			list.sort(Comparator.comparingInt(VisitEntry::visits).reversed());
			callback.accept(list.stream().limit(limit).toList());
		});
	}

	public CompletableFuture<List<VisitEntry>> getFeaturedIslands(int limit) {
		return instance.getCoopManager().getCachedIslandOwnerData().thenApply(users -> users.stream().filter(user -> {
			VisitData visitData = user.getHellblockData().getVisitData();
			if (visitData.isFeatured()) {
				return true;
			}

			// Lazy cleanup of expired slots
			if (visitData.getFeaturedUntil() > 0) {
				visitData.removeFeatured();
			}

			return false;
		}).sorted(Comparator.comparingLong(user -> -user.getHellblockData().getVisitData().getFeaturedUntil()))
				.limit(limit).map(user -> new VisitEntry(user.getUUID(), 0)).toList());
	}

	public CompletableFuture<Boolean> attemptFeaturedSlotPurchase(Player player, Requirement<Player>[] requirements) {
		UUID uuid = player.getUniqueId();

		CompletableFuture<Boolean> result = new CompletableFuture<>();

		instance.getStorageManager().getOnlineUser(uuid).ifPresentOrElse(user -> {
			HellblockData data = user.getHellblockData();
			// Validate island existence
			if (!data.hasHellblock()) {
				instance.getSenderFactory().wrap(player).sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				result.complete(false);
				return;
			}

			if (data.isAbandoned()) {
				instance.getSenderFactory().wrap(player).sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				result.complete(false);
				return;
			}

			// Ownership check
			final UUID ownerId = data.getOwnerUUID();
			if (ownerId == null) {
				throw new IllegalStateException("Hellblock owner UUID is missing.");
			}

			if (!ownerId.equals(player.getUniqueId())) {
				instance.getSenderFactory().wrap(player).sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				result.complete(false);
				return;
			}

			VisitData visitData = data.getVisitData();

			// Already featured — notify and fail
			if (visitData.isFeatured()) {
				long secondsRemaining = (visitData.getFeaturedUntil() - System.currentTimeMillis()) / 1000;
				String expiresAt = instance.getFormattedCooldown(secondsRemaining);
				instance.getSenderFactory().wrap(player)
						.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_FEATURED_EXISTS
								.arguments(AdventureHelper.miniMessage(expiresAt)).build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				result.complete(false);
				return;
			}

			int maxSlots = instance.getVisitGUIManager().getFilledSlotCount();
			long duration = TimeUnit.HOURS.toMillis(24); // Always 24 hours

			instance.getCoopManager().getCachedIslandOwnerData().thenAccept(users -> {
				long currentFeatured = users.stream().filter(u -> u.getHellblockData().getVisitData().isFeatured())
						.count();

				// Check if slots are full
				if (currentFeatured >= maxSlots) {
					instance.getSenderFactory().wrap(player).sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_FEATURED_FULL.build()));
					AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
							Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
									net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					result.complete(false);
					return;
				}

				// Check requirements (e.g. economy, permission, etc.)
				if (requirements != null && !RequirementManager.isSatisfied(Context.player(player), requirements)) {
					instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_FEATURED_FAILED_PURCHASE.build()));
					AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
							Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
									net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					result.complete(false);
					return;
				}

				// Passed all checks, set featured
				long expiresAt = System.currentTimeMillis() + duration;
				visitData.setFeaturedUntil(expiresAt);

				instance.getSenderFactory().wrap(player)
						.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_FEATURED_SUCCESS
								.arguments(AdventureHelper.miniMessage(instance.getFormattedCooldown(duration / 1000L)))
								.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.yes"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));

				result.complete(true);
			});

		}, () -> {
			instance.getSenderFactory().wrap(player).sendMessage(
					instance.getTranslationManager().render(MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED.build()));
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
			result.complete(false);
		});

		return result;
	}

	private void checkNightActivity(UUID visitor, long now) {
		LocalTime time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime();
		int hour = time.getHour();
		if (hour >= NIGHT_START && hour <= NIGHT_END) {
			autoBlacklist(visitor, "Suspicious night-hour activity (" + hour + ":00)");
		}
	}

	private void checkMassAltVisiting(Player visitor, UUID islandOwner) {
		String ip = visitor.getAddress() != null ? visitor.getAddress().getAddress().getHostAddress() : "unknown";
		playerIPs.computeIfAbsent(islandOwner, __ -> ConcurrentHashMap.newKeySet()).add(ip);

		int unique = playerIPs.get(islandOwner).size();
		if (unique >= ALT_THRESHOLD) {
			autoBlacklist(visitor.getUniqueId(), "Potential alt visit farming (" + unique + " unique IPs)");
			playerIPs.get(islandOwner).clear();
		}
	}

	private void checkMutualVisitBoosting(UUID visitor, UUID islandOwner) {
		Deque<UUID> history = visitHistory.computeIfAbsent(visitor, __ -> new ArrayDeque<>());
		history.addLast(islandOwner);

		Deque<UUID> ownerHistory = visitHistory.computeIfAbsent(islandOwner, __ -> new ArrayDeque<>());

		// if owner has recently visited visitor’s island too
		if (ownerHistory.contains(visitor)) {
			int count = reciprocalVisitCount.merge(visitor, 1, Integer::sum);
			if (count >= MUTUAL_VISIT_THRESHOLD) {
				autoBlacklist(visitor, "Mutual visit boosting detected with " + islandOwner);
				reciprocalVisitCount.put(visitor, 0);
			}
		}
	}

	private void checkVisitSpam(UUID visitor, long now) {
		Deque<Long> timestamps = visitTimestamps.computeIfAbsent(visitor, __ -> new ArrayDeque<>());
		timestamps.addLast(now);
		while (!timestamps.isEmpty() && now - timestamps.peekFirst() > SPAM_WINDOW_MS) {
			timestamps.pollFirst();
		}
		if (timestamps.size() > SPAM_THRESHOLD) {
			autoBlacklist(visitor, "Visit spam detected (> " + SPAM_THRESHOLD + " per minute)");
		}
	}

	public void autoBlacklist(UUID playerId, String reason) {
		if (blacklisted.contains(playerId))
			return;

		blacklisted.add(playerId);
		logVisitAbuse(playerId, reason);

		instance.getPluginLogger().warn("Auto-blacklisted " + playerId + ": " + reason);
	}

	public boolean isBlacklisted(UUID playerId) {
		return blacklisted.contains(playerId);
	}

	public void unblacklist(UUID playerId) {
		blacklisted.remove(playerId);
	}

	private void logVisitAbuse(UUID playerId, String reason) {
		visitLogs.computeIfAbsent(playerId, __ -> new ArrayList<>()).add(new VisitLogEntry(playerId, reason));
	}

	public boolean wasAutoFlagged(UUID playerId) {
		return visitLogs.containsKey(playerId);
	}

	public CompletableFuture<List<VisitRecord>> getIslandVisitLog(UUID islandOwner) {
		return instance.getStorageManager().getOfflineUserData(islandOwner, instance.getConfigManager().lockData())
				.thenApply(userOpt -> userOpt.map(user -> user.getHellblockData().getRecentVisitors())
						.orElse(Collections.emptyList()));
	}

	public List<VisitLogEntry> getIslandLogsForPlayer(UUID target) {
		return visitLogs.getOrDefault(target, Collections.emptyList());
	}

	public record VisitEntry(UUID islandOwner, int visits) {
	}

	public static class VisitLogEntry {
		private final long timestamp;
		private final UUID playerId;
		private final String reason;

		public VisitLogEntry(UUID playerId, String reason) {
			this.timestamp = System.currentTimeMillis();
			this.playerId = playerId;
			this.reason = reason;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public UUID getPlayerId() {
			return playerId;
		}

		public String getReason() {
			return reason;
		}
	}

	public class VisitRecord {
		private final UUID visitorId;
		private final long timestamp;

		// Required for Gson to deserialize
		public VisitRecord(UUID visitorId, long timestamp) {
			this.visitorId = visitorId;
			this.timestamp = timestamp;
		}

		// Constructor for new visits
		public VisitRecord(UUID visitorId) {
			this(visitorId, System.currentTimeMillis());
		}

		public UUID getVisitorId() {
			return visitorId;
		}

		public long getTimestamp() {
			return timestamp;
		}
	}
}