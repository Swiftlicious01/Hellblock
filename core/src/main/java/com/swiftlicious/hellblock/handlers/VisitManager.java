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
import java.util.Objects;
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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
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

/**
 * Manages all aspects of player visit tracking within the Hellblock world.
 *
 * Tracks visit cooldowns, recent activity, abuse detection (spamming, mutual
 * visit boosting, alt usage), and maintains visit statistics like featured
 * islands and recent visit logs.
 *
 * Responsible for scheduling periodic tasks related to visit data cleanup,
 * abuse detection, and featured island expiration.
 */
public class VisitManager implements Reloadable {

	protected final HellblockPlugin instance;

	// visit cooldown & activity trackers
	private final Map<UUID, Map<UUID, Long>> visitCooldowns = new ConcurrentHashMap<>();
	private final Map<UUID, Deque<Long>> visitTimestamps = new ConcurrentHashMap<>();
	private final Map<UUID, Deque<UUID>> visitHistory = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> reciprocalVisitCount = new ConcurrentHashMap<>();
	private final Map<UUID, Set<String>> playerIPs = new ConcurrentHashMap<>();
	private final Map<Integer, List<VisitLogEntry>> visitLogs = new ConcurrentHashMap<>();

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

	/**
	 * Starts the scheduled task that resets player visit counters periodically
	 * based on visit data expiration rules.
	 */
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

	/**
	 * Starts a scheduled task to monitor for abnormally high visit frequencies and
	 * auto-blacklist players if thresholds are exceeded.
	 */
	private void startOutlierMonitorTask() {
		outlierMonitorTask = instance.getScheduler().asyncRepeating(() -> {
			long cutoff = System.currentTimeMillis() - OUTLIER_MONITOR_INTERVAL;
			visitTimestamps.entrySet().forEach(entry -> {
				UUID visitorId = entry.getKey();
				Deque<Long> times = entry.getValue();
				times.removeIf(t -> t < cutoff);

				if (times.size() >= OUTLIER_THRESHOLD) {
					instance.getIslandManager().getLastTrackedIsland(visitorId).filter(Objects::nonNull)
							.ifPresent(islandId -> autoBlacklist(islandId, visitorId,
									"Abnormal visit frequency (> " + OUTLIER_THRESHOLD + " in 10 minutes)"));
					times.clear();
				}
			});
		}, 0L, 5, TimeUnit.MINUTES); // run every 5 minutes
	}

	/**
	 * Starts the scheduled task that performs cleanup of old visit records from
	 * player data (older than 7 days).
	 */
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

	/**
	 * Starts a scheduled task that removes expired "featured" status from islands
	 * once their display time ends.
	 */
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
	 * Determines whether the given visitor can visit the specified island, based on
	 * cooldowns and blacklist status.
	 *
	 * @param visitorId the UUID of the visiting player
	 * @param ownerId   the UUID of the island owner
	 * @return true if the player can visit, false otherwise
	 */
	public boolean canVisit(@NotNull UUID visitorId, @NotNull UUID ownerId) {
		if (blacklisted.contains(visitorId)) {
			return false;
		}

		Map<UUID, Long> cooldowns = visitCooldowns.get(visitorId);
		if (cooldowns == null)
			return true;

		Long lastVisit = cooldowns.get(ownerId);
		return lastVisit == null || System.currentTimeMillis() - lastVisit >= VISIT_COOLDOWN_MS;
	}

	/**
	 * Handles the logic for teleporting a visitor to an island, checking for bans,
	 * cooldowns, and safety, and optionally recording the visit.
	 *
	 * @param visitor the player attempting to visit
	 * @param ownerId the UUID of the island owner
	 */
	public CompletableFuture<Boolean> handleVisit(@NotNull Player visitor, @NotNull UUID ownerId) {
		CompletableFuture<Optional<UserData>> visitorFuture = instance.getStorageManager()
				.getCachedUserDataWithFallback(visitor.getUniqueId(), false);

		CompletableFuture<Optional<UserData>> ownerFuture = instance.getStorageManager()
				.getCachedUserDataWithFallback(ownerId, false);

		return visitorFuture.thenCombineAsync(ownerFuture, (visitorOpt, ownerOpt) -> {
			if (visitorOpt.isEmpty() || ownerOpt.isEmpty()) {
				return Optional.<Pair<UserData, UserData>>empty();
			}
			return Optional.of(Pair.of(visitorOpt.get(), ownerOpt.get()));
		}).thenCompose(optionalPair -> {
			if (optionalPair.isEmpty())
				return CompletableFuture.completedFuture(false);

			UserData visitorData = optionalPair.get().left();
			UserData ownerData = optionalPair.get().right();
			HellblockData hellblockData = ownerData.getHellblockData();

			if (hellblockData.isLocked()) {
				sendLockedMessage(visitor, ownerData);
				return CompletableFuture.completedFuture(false);
			}

			if (isVisitorBanned(visitor, hellblockData)) {
				sendBannedMessage(visitor, ownerData);
				return CompletableFuture.completedFuture(false);
			}

			boolean associated = isAssociated(visitorData.getUUID(), hellblockData);
			boolean eligibleToRecord = !associated && !isBlacklisted(visitorData.getUUID())
					&& canVisit(visitorData.getUUID(), ownerId);

			Location warp = Optional.ofNullable(hellblockData.getVisitData().getWarpLocation())
					.orElse(hellblockData.getHomeLocation());

			if (warp == null) {
				logMissingHomeLocation(ownerData);
				sendMissingHomeMessage(visitor);
				return CompletableFuture.completedFuture(false);
			}

			final Location finalWarp = warp;

			return instance.getWorldManager().ensureHellblockWorldLoaded(hellblockData.getIslandId())
					.thenCompose(world -> {
						Location corrected = new Location(world.bukkitWorld(), finalWarp.getX(), finalWarp.getY(),
								finalWarp.getZ(), finalWarp.getYaw(), finalWarp.getPitch());

						return LocationUtils.isSafeLocationAsync(corrected).thenCompose(isSafe -> {
							if (!isSafe) {
								sendUnsafeMessage(visitor);
								return CompletableFuture.completedFuture(false);
							}

							return ChunkUtils.teleportAsync(visitor, corrected, TeleportCause.PLUGIN).thenCompose(v -> {
								logVisit(visitor, ownerData);

								CompletableFuture<Boolean> recordFuture = eligibleToRecord
										? recordVisit(hellblockData.getIslandId(), visitor, ownerId)
										: CompletableFuture.completedFuture(false);

								return recordFuture.handle((result, ex) -> {
									if (ex != null) {
										instance.getPluginLogger().warn("Failed to record visit", ex);
										return false;
									}
									// Avoid null boxing
									return Boolean.TRUE.equals(result);
								}).thenApply(success -> {
									hellblockData.sendDisplayTextTo(visitor);
									sendVisitEntryMessage(visitor, ownerData);
									return true;
								});
							});
						});
					});
		});
	}

	private boolean isVisitorBanned(Player visitor, HellblockData ownerData) {
		UUID visitorId = visitor.getUniqueId();
		return ownerData.getBannedMembers().contains(visitorId) && !(visitor.isOp()
				|| visitor.hasPermission("hellblock.admin") || visitor.hasPermission("hellblock.bypass.interact"));
	}

	private boolean isAssociated(UUID visitorId, HellblockData ownerData) {
		return visitorId.equals(ownerData.getOwnerUUID()) || ownerData.getPartyMembers().contains(visitorId)
				|| ownerData.getTrustedMembers().contains(visitorId);
	}

	private void sendLockedMessage(Player visitor, UserData owner) {
		instance.getSenderFactory().wrap(visitor)
				.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LOCKED_FROM_VISITORS
						.arguments(AdventureHelper.miniMessageToComponent(owner.getName())).build()));
	}

	private void sendBannedMessage(Player visitor, UserData owner) {
		instance.getSenderFactory().wrap(visitor)
				.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY
						.arguments(AdventureHelper.miniMessageToComponent(owner.getName())).build()));
	}

	private void sendMissingHomeMessage(Player visitor) {
		instance.getSenderFactory().wrap(visitor).sendMessage(
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION.build()));
	}

	private void logMissingHomeLocation(UserData owner) {
		instance.getPluginLogger().severe("Null home location for " + owner.getName() + " (" + owner.getUUID() + ")");
	}

	private void sendUnsafeMessage(Player visitor) {
		instance.getSenderFactory().wrap(visitor).sendMessage(
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UNSAFE_TO_VISIT.build()));
	}

	private void sendVisitEntryMessage(Player visitor, UserData owner) {
		instance.getSenderFactory().wrap(visitor)
				.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_VISIT_ENTRY
						.arguments(AdventureHelper.miniMessageToComponent(owner.getName())).build()));
	}

	private void logVisit(Player visitor, UserData owner) {
		instance.debug(visitor.getName() + " (" + visitor.getUniqueId() + ") visited " + owner.getName() + " ("
				+ owner.getUUID() + ")");
	}

	/**
	 * Records a visit to an island, applies anti-abuse checks, and updates
	 * visit-related counters and cooldowns.
	 *
	 * @param islandId the ID of the island
	 * @param visitor  the player who visited
	 * @param ownerId  the UUID of the island owner
	 */
	private CompletableFuture<Boolean> recordVisit(int islandId, @NotNull Player visitor, @NotNull UUID ownerId) {
		final UUID visitorId = visitor.getUniqueId();

		if (isBlacklisted(visitorId))
			return CompletableFuture.completedFuture(false);
		if (!canVisit(visitorId, ownerId))
			return CompletableFuture.completedFuture(false);

		long now = System.currentTimeMillis();

		// 1. Detect visit spam
		checkVisitSpam(islandId, visitorId, now);

		// 2. Detect mutual visit boosting
		checkMutualVisitBoosting(islandId, visitorId, ownerId);

		// 3. Detect mass alt visiting (many IPs to same island)
		checkMassAltVisiting(islandId, visitor, ownerId);

		// 4. Detect night-hour repetitive activity
		checkNightActivity(islandId, visitorId, now);

		// Normal cooldown tracking
		visitCooldowns.computeIfAbsent(visitorId, __ -> new HashMap<>()).put(ownerId, now);

		// Track visit for statistical data
		visitTimestamps.computeIfAbsent(visitorId, __ -> new ArrayDeque<>()).addLast(now);
		visitHistory.computeIfAbsent(visitorId, __ -> new ArrayDeque<>()).addLast(ownerId);

		// Normal visit progression
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenApply(optData -> {
			if (optData.isEmpty()) {
				return false;
			}

			UserData userData = optData.get();
			HellblockData hellblockData = userData.getHellblockData();

			VisitData visitData = hellblockData.getVisitData();
			visitData.increment();
			hellblockData.addVisitor(visitorId);
			return true;
		}).handle((result, ex) -> {
			if (ex != null) {
				instance.getPluginLogger()
						.warn("Error during visit recording for owner " + ownerId + ": " + ex.getMessage(), ex);
			}
			return true; // Continue to unlock either way
		}).thenCompose(v -> instance.getStorageManager().unlockUserData(ownerId).thenApply(x -> true));
	}

	/**
	 * Retrieves a ranked list of islands based on the specified visit metric (e.g.,
	 * total, daily, weekly) and passes it to the given callback.
	 *
	 * @param visitType function to extract the visit count metric from VisitData
	 * @param limit     max number of top islands to include
	 * @param callback  consumer to receive the sorted result
	 */
	public void getTopIslands(@NotNull Function<VisitData, Integer> visitType, int limit,
			@NotNull Consumer<List<VisitEntry>> callback) {
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

	/**
	 * Asynchronously retrieves the list of currently featured islands, sorted by
	 * the newest expiration time.
	 *
	 * @param limit max number of featured islands to return
	 * @return a future containing the list of featured island entries
	 */
	@NotNull
	public CompletableFuture<List<VisitEntry>> getFeaturedIslands(int limit) {
		return instance.getCoopManager().getCachedIslandOwnerData()
				.thenApplyAsync(users -> users.stream().filter(user -> {
					VisitData visitData = user.getHellblockData().getVisitData();
					long until = visitData.getFeaturedUntil();

					if (until <= System.currentTimeMillis()) {
						visitData.removeFeatured(); // Lazy cleanup of expired entries
						return false; // Exclude from featured list
					}

					return true; // Still active — include
				})
						// Sort by newest expiry
						.sorted(Comparator.comparingLong(
								userData -> -userData.getHellblockData().getVisitData().getFeaturedUntil()))
						.limit(limit) // Apply limit AFTER cleanup
						.map(userData -> new VisitEntry(userData.getUUID(), 0)) // Map to display entry
						.toList());
	}

	/**
	 * Attempts to purchase a featured island slot for the given player. Performs
	 * all validation checks (ownership, featured status, requirements, etc.) and
	 * updates island status if successful.
	 *
	 * @param player       the player attempting the purchase
	 * @param requirements optional array of requirements to check before purchase
	 * @return a future resolving to true if the purchase was successful, false
	 *         otherwise
	 */
	@NotNull
	public CompletableFuture<Boolean> attemptFeaturedSlotPurchase(@NotNull Player player,
			@Nullable Requirement<Player>[] requirements) {
		UUID uuid = player.getUniqueId();

		CompletableFuture<Boolean> result = new CompletableFuture<>();

		instance.getStorageManager().getOnlineUser(uuid).ifPresentOrElse(userData -> {
			HellblockData data = userData.getHellblockData();
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
				instance.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
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
				String expiresAt = instance.getCooldownManager().getFormattedCooldown(secondsRemaining);
				instance.getSenderFactory().wrap(player)
						.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_FEATURED_EXISTS
								.arguments(AdventureHelper.miniMessageToComponent(expiresAt)).build()));
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
						.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_FEATURED_SUCCESS
										.arguments(AdventureHelper.miniMessageToComponent(
												instance.getCooldownManager().getFormattedCooldown(duration / 1000L)))
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

	/**
	 * Checks if a player is visiting during night hours and blacklists them if such
	 * behavior is detected as suspicious.
	 *
	 * @param islandId  the ID of the island being visited
	 * @param visitorId the UUID of the visiting player
	 * @param now       the current timestamp in milliseconds
	 */
	private void checkNightActivity(int islandId, @NotNull UUID visitorId, long now) {
		LocalTime time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime();
		int hour = time.getHour();
		if (hour >= NIGHT_START && hour <= NIGHT_END) {
			autoBlacklist(islandId, visitorId, "Suspicious night-hour activity (" + hour + ":00)");
		}
	}

	/**
	 * Checks if a player is attempting to boost visits using multiple IPs (likely
	 * alt accounts) and blacklists them if the threshold is exceeded.
	 *
	 * @param islandId the ID of the island being visited
	 * @param visitor  the visiting player
	 * @param ownerId  the UUID of the island owner
	 */
	private void checkMassAltVisiting(int islandId, Player visitor, @NotNull UUID ownerId) {
		String ip = visitor.getAddress() != null ? visitor.getAddress().getAddress().getHostAddress() : "unknown";
		playerIPs.computeIfAbsent(ownerId, __ -> ConcurrentHashMap.newKeySet()).add(ip);

		int unique = playerIPs.get(ownerId).size();
		if (unique >= ALT_THRESHOLD) {
			autoBlacklist(islandId, visitor.getUniqueId(), "Potential alt visit farming (" + unique + " unique IPs)");
			playerIPs.get(ownerId).clear();
		}
	}

	/**
	 * Detects mutual visit boosting between two players (e.g., collusion for
	 * rewards) and blacklists if suspicious reciprocal visits exceed a configured
	 * threshold.
	 *
	 * @param islandId  the ID of the island involved
	 * @param visitorId the visiting player's UUID
	 * @param ownerId   the island owner's UUID
	 */
	private void checkMutualVisitBoosting(int islandId, @NotNull UUID visitorId, @NotNull UUID ownerId) {
		Deque<UUID> history = visitHistory.computeIfAbsent(visitorId, __ -> new ArrayDeque<>());
		history.addLast(ownerId);

		Deque<UUID> ownerHistory = visitHistory.computeIfAbsent(ownerId, __ -> new ArrayDeque<>());

		// Check if the owner has recently visited the visitor’s island
		if (ownerHistory.contains(visitorId)) {
			int count = reciprocalVisitCount.merge(visitorId, 1, Integer::sum);
			if (count >= MUTUAL_VISIT_THRESHOLD) {
				autoBlacklist(islandId, visitorId, "Mutual visit boosting detected with " + ownerId);
				reciprocalVisitCount.put(visitorId, 0);
			}
		}
	}

	/**
	 * Detects rapid, repeated visits from the same player in a short time window,
	 * and blacklists them if the spam threshold is exceeded.
	 *
	 * @param islandId  the ID of the island being visited
	 * @param visitorId the UUID of the visiting player
	 * @param now       the current timestamp in milliseconds
	 */
	private void checkVisitSpam(int islandId, @NotNull UUID visitorId, long now) {
		Deque<Long> timestamps = visitTimestamps.computeIfAbsent(visitorId, __ -> new ArrayDeque<>());
		timestamps.addLast(now);
		while (!timestamps.isEmpty() && now - timestamps.peekFirst() > SPAM_WINDOW_MS) {
			timestamps.pollFirst();
		}
		if (timestamps.size() > SPAM_THRESHOLD) {
			autoBlacklist(islandId, visitorId, "Visit spam detected (> " + SPAM_THRESHOLD + " per minute)");
		}
	}

	/**
	 * Automatically blacklists a player from visiting due to detected abuse, and
	 * logs the event for audit purposes.
	 *
	 * @param islandId  the ID of the island where abuse was detected
	 * @param visitorId the UUID of the player to blacklist
	 * @param reason    the reason for blacklisting
	 */
	public void autoBlacklist(int islandId, @NotNull UUID visitorId, @NotNull String reason) {
		if (blacklisted.contains(visitorId))
			return;

		blacklisted.add(visitorId);
		logVisitAbuse(islandId, visitorId, reason);

		instance.getPluginLogger().warn("Auto-blacklisted " + visitorId + ": " + reason);
	}

	/**
	 * Checks if the specified player is currently blacklisted from visiting
	 * islands.
	 *
	 * @param visitorId the player's UUID
	 * @return true if the player is blacklisted, false otherwise
	 */
	public boolean isBlacklisted(@NotNull UUID visitorId) {
		return blacklisted.contains(visitorId);
	}

	/**
	 * Removes the specified player from the visit blacklist.
	 *
	 * @param visitorId the player's UUID
	 */
	public void unblacklist(@NotNull UUID visitorId) {
		blacklisted.remove(visitorId);
	}

	/**
	 * Records a visit abuse event in the audit log for a specific island.
	 *
	 * @param islandId  the ID of the island where the abuse occurred
	 * @param visitorId the UUID of the player flagged
	 * @param reason    the reason for flagging the player
	 */
	private void logVisitAbuse(int islandId, @NotNull UUID visitorId, @NotNull String reason) {
		visitLogs.computeIfAbsent(islandId, __ -> new ArrayList<>()).add(new VisitLogEntry(visitorId, reason));
	}

	/**
	 * Checks whether a specific island has any historical visit abuse logs.
	 *
	 * @param islandId the ID of the island to check
	 * @return true if abuse logs exist for the island, false otherwise
	 */
	public boolean wasAutoFlagged(int islandId) {
		return visitLogs.containsKey(islandId);
	}

	/**
	 * Retrieves the list of recent visit records for a specific island, potentially
	 * for UI display or moderation purposes.
	 *
	 * @param ownerId the UUID of the island owner
	 * @return a CompletableFuture with the list of visit records
	 */
	@NotNull
	public CompletableFuture<List<VisitRecord>> getIslandVisitLog(@NotNull UUID ownerId) {
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenApply(optData -> optData
				.map(userData -> userData.getHellblockData().getRecentVisitors()).orElse(Collections.emptyList()));
	}

	/**
	 * Returns the list of visit abuse log entries associated with a specific
	 * island.
	 *
	 * @param islandId the ID of the island
	 * @return list of visit log entries, or an empty list if none exist
	 */
	@NotNull
	public List<VisitLogEntry> getVisitLogsForIslandId(int islandId) {
		return visitLogs.getOrDefault(islandId, Collections.emptyList());
	}

	/**
	 * Represents a summary entry for an island in a visit leaderboard.
	 *
	 * @param ownerId the UUID of the island owner
	 * @param visits  the number of visits recorded by the selected metric
	 */
	public record VisitEntry(@NotNull UUID ownerId, int visits) {
	}

	/**
	 * Stores metadata about an auto-blacklist or abuse detection event related to
	 * player visits.
	 */
	public class VisitLogEntry {
		private final long timestamp;
		private final UUID visitorId;
		private final String reason;

		/**
		 * Creates a new visit log entry for the specified visitor and reason.
		 *
		 * @param visitorId the UUID of the visitor involved
		 * @param reason    the reason for flagging
		 */
		public VisitLogEntry(@NotNull UUID visitorId, @NotNull String reason) {
			this.timestamp = System.currentTimeMillis();
			this.visitorId = visitorId;
			this.reason = reason;
		}

		/**
		 * Returns the timestamp of the visit log.
		 * 
		 * @return the timestamp when the log entry was created (in ms)
		 */
		public long getTimestamp() {
			return timestamp;
		}

		/**
		 * Returns the UUID of the visitor.
		 * 
		 * @return the UUID of the visitor who was flagged
		 */
		@NotNull
		public UUID getVisitorId() {
			return visitorId;
		}

		/**
		 * Returns the reason of the visit log.
		 * 
		 * @return the reason for the visit log entry (e.g., type of abuse)
		 */
		@NotNull
		public String getReason() {
			return reason;
		}
	}

	/**
	 * Represents a single visit record to an island.
	 * <p>
	 * Stores the visitor's UUID and the timestamp (in epoch millis) of their visit.
	 * Used for logging recent visits and displaying visit history in GUIs.
	 */
	public class VisitRecord {

		@Expose
		@SerializedName("visitorId")
		private final UUID visitorId;

		@Expose
		@SerializedName("timestamp")
		private final long timestamp;

		/**
		 * Constructs a new {@link VisitRecord} with the given visitor and timestamp.
		 * Used primarily for deserialization.
		 *
		 * @param visitorId the UUID of the visiting player
		 * @param timestamp the time the visit occurred, in epoch milliseconds
		 */
		public VisitRecord(@NotNull UUID visitorId, long timestamp) {
			this.visitorId = visitorId;
			this.timestamp = timestamp;
		}

		/**
		 * Constructs a new {@link VisitRecord} using the current system time as the
		 * timestamp. Used when logging a fresh visit.
		 *
		 * @param visitorId the UUID of the visiting player
		 */
		public VisitRecord(@NotNull UUID visitorId) {
			this(visitorId, System.currentTimeMillis());
		}

		/**
		 * Returns the UUID of the visiting player.
		 *
		 * @return the visitor's UUID
		 */
		@NotNull
		public UUID getVisitorId() {
			return visitorId;
		}

		/**
		 * Returns the timestamp of this visit in epoch milliseconds.
		 *
		 * @return the time the visit occurred
		 */
		public long getTimestamp() {
			return timestamp;
		}

		/**
		 * Creates a copy of this {@link VisitRecord}. Since fields are immutable, this
		 * is effectively a new instance with the same values.
		 *
		 * @return a new VisitRecord with identical visitor ID and timestamp
		 */
		@NotNull
		public final VisitRecord copy() {
			return new VisitRecord(visitorId, timestamp);
		}
	}
}