package com.swiftlicious.hellblock.database;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Enums;
import com.google.common.io.Files;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VisitManager;
import com.swiftlicious.hellblock.handlers.VisitManager.VisitRecord;
import com.swiftlicious.hellblock.player.ChallengeData;
import com.swiftlicious.hellblock.player.CoopChatSetting;
import com.swiftlicious.hellblock.player.DisplaySettings;
import com.swiftlicious.hellblock.player.DisplaySettings.DisplayChoice;
import com.swiftlicious.hellblock.player.EarningData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.InvasionData;
import com.swiftlicious.hellblock.player.LocationCacheData;
import com.swiftlicious.hellblock.player.NotificationSettings;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.SkysiegeData;
import com.swiftlicious.hellblock.player.StatisticData;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.player.WitherData;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.utils.LocationUtils;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * A data storage implementation that uses YAML files to store player data, with
 * support for legacy data.
 */
public class YamlHandler extends AbstractStorage {

	private final File dataFolder;

	private final ConcurrentHashMap<UUID, CompletableFuture<Optional<PlayerData>>> loadingCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> updatingCache = new ConcurrentHashMap<>();

	private final Cache<Integer, UUID> islandIdToUUIDCache = Caffeine.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build();

	private final Cache<UUID, PlayerData> memoryCache = Caffeine.newBuilder().maximumSize(500).build();

	public YamlHandler(HellblockPlugin plugin) {
		super(plugin);
		this.dataFolder = new File(plugin.getDataFolder(), "data");
		if (!dataFolder.exists() && !dataFolder.mkdirs()) {
			plugin.getPluginLogger().warn("Failed to create data folder for YAML storage.");
		}
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.YAML;
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor) {
		final Executor finalExecutor = executor != null ? executor : plugin.getScheduler().async();

		return loadingCache.computeIfAbsent(uuid, id -> {
			plugin.debug("getPlayerData: starting new load for " + id);
			final CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();

			// Ensure the entry is only removed after the future completes (success or
			// failure)
			future.whenComplete((result, throwable) -> loadingCache.remove(uuid));

			finalExecutor.execute(() -> {
				try {
					// Check cache first
					PlayerData cached = memoryCache.getIfPresent(uuid);
					if (cached != null) {
						plugin.debug("YAML cache hit for " + uuid);
						future.complete(Optional.of(cached));
						return;
					}

					plugin.debug("YAML cache miss for " + uuid);

					File file = getPlayerDataFile(uuid);
					if (!file.exists()) {
						PlayerData data = Bukkit.getPlayer(uuid) != null ? PlayerData.empty() : null;
						if (data != null)
							data.setUUID(uuid);
						future.complete(Optional.ofNullable(data));
						return;
					}

					YamlDocument yaml = plugin.getConfigManager().loadData(file);
					PlayerData playerData = parseYamlToPlayerData(uuid, yaml);

					if (playerData != null) {
						memoryCache.put(uuid, playerData);

						// Index the island ID to UUID
						int islandId = playerData.getHellblockData().getIslandId();
						if (islandId > 0) {
							islandIdToUUIDCache.put(islandId, uuid);
						}
					}

					future.complete(Optional.ofNullable(playerData));
				} catch (Exception ex) {
					plugin.getPluginLogger().warn("Failed to load YAML for " + uuid, ex);
					future.completeExceptionally(ex);
				}
			});

			return future;
		});
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerDataByIslandId(int islandId, boolean lock,
			Executor executor) {
		UUID cachedUUID = islandIdToUUIDCache.getIfPresent(islandId);

		if (cachedUUID != null) {
			return getPlayerData(cachedUUID, lock, executor);
		}

		return scanYamlFilesForIslandId(islandId, lock, executor);
	}

	private CompletableFuture<Optional<PlayerData>> scanYamlFilesForIslandId(int islandId, boolean lock,
			Executor executor) {
		final Executor finalExecutor = executor != null ? executor : plugin.getScheduler().async();
		final CompletableFuture<Optional<PlayerData>> future = new CompletableFuture<>();

		finalExecutor.execute(() -> {
			try {
				File dataDir = getPlayerDataFolder();
				File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".yml"));

				if (files == null) {
					future.complete(Optional.empty());
					return;
				}

				for (File file : files) {
					UUID fileUUID;
					try {
						fileUUID = UUID.fromString(file.getName().replace(".yml", ""));
					} catch (IllegalArgumentException e) {
						continue;
					}

					// Fast path: check memory cache first
					PlayerData cached = memoryCache.getIfPresent(fileUUID);
					if (cached != null && cached.getHellblockData().getIslandId() == islandId) {
						islandIdToUUIDCache.put(islandId, fileUUID);
						future.complete(Optional.of(cached));
						return;
					}

					// Load from file
					YamlDocument yaml = plugin.getConfigManager().loadData(file);
					PlayerData parsed = parseYamlToPlayerData(fileUUID, yaml);

					if (parsed != null && parsed.getHellblockData().getIslandId() == islandId) {
						memoryCache.put(fileUUID, parsed);
						islandIdToUUIDCache.put(islandId, fileUUID);
						future.complete(Optional.of(parsed));
						return;
					}
				}

				future.complete(Optional.empty());
			} catch (Exception e) {
				plugin.getPluginLogger().warn("Failed to scan YAML files for islandId=" + islandId, e);
				future.completeExceptionally(e);
			}
		});

		return future;
	}

	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean ignore) {
		return updatingCache.computeIfAbsent(uuid, id -> {
			final CompletableFuture<Boolean> future = new CompletableFuture<>();
			final Executor executor = plugin.getScheduler().async();

			// Ensure cleanup happens only once after complete
			future.whenComplete((result, throwable) -> updatingCache.remove(uuid));

			executor.execute(() -> {
				try {
					File file = getPlayerDataFile(uuid);
					YamlDocument yaml = plugin.getConfigManager().loadData(file);

					serializePlayerDataToYaml(playerData, yaml);

					yaml.save(file);
					memoryCache.put(uuid, playerData);
					future.complete(true);
				} catch (IOException ex) {
					plugin.getPluginLogger().warn("Failed to save YAML data for " + uuid, ex);
					future.completeExceptionally(ex);
				} catch (Exception ex) {
					plugin.getPluginLogger().warn("Unexpected error while saving YAML for " + uuid, ex);
					future.completeExceptionally(ex);
				}
			});

			return future;
		});
	}

	private PlayerData parseYamlToPlayerData(UUID uuid, YamlDocument data) {
		// Basic initialization
		final Set<UUID> party = readUUIDSet(data.getStringList("hellblockData.partyMembers"));
		final Set<UUID> trusted = readUUIDSet(data.getStringList("hellblockData.trustedMembers"));
		final Set<UUID> banned = readUUIDSet(data.getStringList("hellblockData.bannedMembers"));
		final Map<UUID, Long> invitations = readUUIDLongMap(data.getSection("hellblockData.islandInvitations"));
		EnumMap<FlagType, HellblockFlag> flags = new EnumMap<>(FlagType.class);
		Section section = data.getSection("hellblockData.protectionFlags");
		if (section != null) {
			for (String key : section.getRoutesAsStrings(false)) {
				FlagType flagType = Enums.getIfPresent(FlagType.class, key).orNull();
				if (flagType == null)
					continue;

				Section flagSection = section.getSection(key);
				if (flagSection != null) {
					AccessType status = Enums.getIfPresent(AccessType.class, flagSection.getString("allowedStatus", ""))
							.or(flagType.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY);

					// Read optional string data if present
					String dataString = flagSection.contains("stringData") ? flagSection.getString("stringData") : null;

					// Use constructor with data if data is present or flag supports it
					if ((flagType == FlagType.GREET_MESSAGE || flagType == FlagType.FAREWELL_MESSAGE)) {
						flags.put(flagType, new HellblockFlag(flagType, status, dataString));
					} else {
						flags.put(flagType, new HellblockFlag(flagType, status));
					}
				}
			}
		}
		final EnumMap<IslandUpgradeType, Integer> upgrades = readEnumIntMap(
				data.getSection("hellblockData.islandUpgrades"), IslandUpgradeType.class);
		final Map<ChallengeType, ChallengeResult> challenges = parseChallengeData(
				data.getSection("challengeData.challenges"));
		final List<MailboxEntry> mailbox = parseMailboxEntries(data.getSection("mailboxEntries"));
		final VisitData visitData = parseVisitData(data.getSection("hellblockData.visitData"));
		final List<VisitRecord> recentVisitors = parseRecentVisitors(data.getSection("hellblockData.recentVisitors"));
		final InvasionData invasionData = parseInvasionData(data.getSection("hellblockData.invasionData"));
		final WitherData witherData = parseWitherData(data.getSection("hellblockData.witherData"));
		final SkysiegeData skysiegeData = parseSkysiegeData(data.getSection("hellblockData.skysiegeData"));
		final DisplaySettings display = parseDisplaySettings(data.getSection("hellblockData.displaySettings"));
		final BoundingBox bounds = parseBoundingBox(data.getSection("hellblockData.islandBounds"));
		final Location home = safeDeserializeLocation(data, "hellblockData.homeLocation");
		final Location location = safeDeserializeLocation(data, "hellblockData.hellblockLocation");

		// Hellblock metadata
		final boolean hasHellblock = data.getBoolean("hellblockData.hellblockExists", false);
		final UUID ownerUUID = tryParseUUID(data.getString("hellblockData.ownerUUID"));
		final UUID linkedPortalUUID = tryParseUUID(data.getString("hellblockData.linkedPortalUUID"));
		final int hellblockId = data.getInt("hellblockData.islandId", 0);
		final float level = data.getFloat("hellblockData.islandLevel", 0.0F);
		final long creation = data.getLong("hellblockData.creationTime", 0L);
		final HellBiome biome = tryParseEnum(data.getString("hellblockData.islandBiome"), HellBiome.class,
				HellBiome.NETHER_WASTES);
		final IslandOptions islandChoice = tryParseEnum(data.getString("hellblockData.islandChoice"),
				IslandOptions.class, null);
		final CoopChatSetting chatSetting = tryParseEnum(data.getString("hellblockData.chatPreference"),
				CoopChatSetting.class, CoopChatSetting.GLOBAL);
		final String schematic = data.getString("hellblockData.usedSchematic", null);
		final boolean locked = data.getBoolean("hellblockData.isLocked", false);
		final boolean abandoned = data.getBoolean("hellblockData.isAbandoned", false);
		final long resetCooldown = data.getLong("hellblockData.resetCooldown", 0L);
		final long biomeCooldown = data.getLong("hellblockData.biomeCooldown", 0L);
		final long transferCooldown = data.getLong("hellblockData.transferCooldown", 0L);
		final long lastIslandActivity = data.getLong("hellblockData.lastIslandActivity", 0L);
		final long lastWorldAccess = data.getLong("hellblockData.lastWorldAccess", 0L);

		// Name and date
		final String name = data.getString("name", "");
		final int dataVersion = data.getInt("dataVersion", PlayerData.CURRENT_VERSION);
		final String dateStr = data.getString("earningData.date", null);
		final LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : LocalDate.now();

		// Construct player data
		final PlayerData playerData = PlayerData.builder().setUUID(uuid).setName(name).setVersion(dataVersion)
				.setEarningData(new EarningData(data.getDouble("earningData.earnings", 0.0), date))
				.setStatisticData(getStatistics(data.getSection("statisticData.stats"))).setMailbox(mailbox)
				.setChallengeData(new ChallengeData(challenges))
				.setLocationCacheData(new LocationCacheData(
						plugin.getConfigManager().pistonAutomation()
								? readIntegerListMap(data.getSection("locationCacheData.cachedPistons"))
								: new HashMap<>(),
						ownerUUID != null && ownerUUID.equals(uuid)
								? readNestedMap(data.getSection("locationCacheData.placedBlocks"))
								: new HashMap<>()))
				.setNotificationSettings(
						new NotificationSettings(data.getBoolean("notificationSettings.joinNotifications", true),
								data.getBoolean("notificationSettings.inviteNotifications", true)))
				.setHellblockData(new HellblockData(hellblockId, level, hasHellblock, ownerUUID, linkedPortalUUID,
						bounds, display, chatSetting, party, trusted, banned, invitations, flags, upgrades, location,
						home, creation, visitData, recentVisitors, biome, islandChoice, schematic, locked, abandoned,
						resetCooldown, biomeCooldown, transferCooldown, lastIslandActivity, lastWorldAccess,
						invasionData, witherData, skysiegeData))
				.build();

		return playerData;
	}

	private Location safeDeserializeLocation(YamlDocument data, String path) {
		Section section = data.getSection(path);
		return (section != null) ? LocationUtils.deserializeLocation(section) : null;
	}

	private Map<ChallengeType, ChallengeResult> parseChallengeData(Section section) {
		Map<ChallengeType, ChallengeResult> map = new HashMap<>();
		if (section == null)
			return map;

		section.getKeys().forEach(key -> {
			ChallengeType type = plugin.getChallengeManager().getById(key.toString());
			if (type == null)
				return;

			CompletionStatus status = tryParseEnum(section.getString(key + ".completionStatus"), CompletionStatus.class,
					CompletionStatus.NOT_STARTED);
			int progress = section.getInt(key + ".progress", type.getNeededAmount());
			boolean claimed = section.getBoolean(key + ".claimedReward", false);

			map.put(type, new ChallengeResult(status, progress, claimed));
		});

		return map;
	}

	private List<MailboxEntry> parseMailboxEntries(Section section) {
		List<MailboxEntry> list = new ArrayList<>();
		if (section == null)
			return list;

		MiniMessage mini = AdventureHelper.getMiniMessage();

		section.getKeys().stream().map(key -> section.getSection(key.toString())).forEach(entry -> {
			if (entry == null)
				return;
			String messageKey = entry.getString("messageKey");
			List<Component> args = entry.getStringList("arguments").stream().map(mini::deserialize).toList();
			Set<MailboxFlag> flags = entry.getStringList("flags").stream().map(s -> {
				try {
					return MailboxFlag.valueOf(s.toUpperCase(Locale.ENGLISH));
				} catch (IllegalArgumentException e) {
					return null;
				}
			}).filter(Objects::nonNull).collect(Collectors.toSet());
			list.add(new MailboxEntry(messageKey, args, flags));
		});

		return list;
	}

	private VisitData parseVisitData(Section section) {
		VisitData data = new VisitData();
		if (section == null)
			return data;

		Section warp = section.getSection("warpLocation");
		if (warp != null)
			data.setWarpLocation(LocationUtils.deserializeLocation(warp));

		data.setTotalVisits(section.getInt("totalVisits", 0));
		data.setVisitsToday(section.getInt("visitsToday", 0));
		data.setVisitsThisWeek(section.getInt("visitsThisWeek", 0));
		data.setVisitsThisMonth(section.getInt("visitsThisMonth", 0));
		data.setLastVisitReset(section.getLong("lastVisitReset", 0L));
		data.setFeaturedUntil(section.getLong("featuredUntil", 0L));

		return data;
	}

	private List<VisitRecord> parseRecentVisitors(Section section) {
		List<VisitRecord> list = new ArrayList<>();
		if (section == null)
			return list;

		VisitManager visitManager = plugin.getVisitManager();
		section.getKeys().stream().map(key -> section.getSection(key.toString())).forEach(entry -> {
			if (entry == null)
				return;
			UUID id = tryParseUUID(entry.getString("visitorId"));
			long time = entry.getLong("timestamp");
			if (id != null)
				list.add(visitManager.new VisitRecord(id, time));
		});

		return list;
	}

	private DisplaySettings parseDisplaySettings(Section section) {
		DisplaySettings settings = new DisplaySettings("", "", DisplayChoice.CHAT);
		if (section == null)
			return settings;

		settings.setIslandName(section.getString("islandName", ""));
		settings.setIslandBio(section.getString("islandBio", ""));

		DisplayChoice choice = tryParseEnum(section.getString("displayChoice"), DisplayChoice.class,
				DisplayChoice.CHAT);
		settings.setDisplayChoice(choice);

		if (section.getBoolean("isDefaultIslandName", true))
			settings.setAsDefaultIslandName();
		else
			settings.isNotDefaultIslandName();

		if (section.getBoolean("isDefaultIslandBio", true))
			settings.setAsDefaultIslandBio();
		else
			settings.isNotDefaultIslandBio();

		return settings;
	}

	private BoundingBox parseBoundingBox(Section section) {
		if (section == null)
			return null;

		double minX = section.getDouble("minX");
		double minY = section.getDouble("minY");
		double minZ = section.getDouble("minZ");
		double maxX = section.getDouble("maxX");
		double maxY = section.getDouble("maxY");
		double maxZ = section.getDouble("maxZ");

		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private InvasionData parseInvasionData(Section section) {
		InvasionData data = new InvasionData();
		if (section == null)
			return data;

		data.setTotalInvasions(section.getInt("totalInvasions", 0));
		data.setSuccessfulInvasions(section.getInt("successfulInvasions", 0));
		data.setFailedInvasions(section.getInt("failedInvasions", 0));
		data.setBossKills(section.getInt("bossKills", 0));
		data.setCurrentStreak(section.getInt("currentStreak", 0));
		data.setLastInvasionTime(section.getLong("lastInvasionTime", 0L));
		data.setHighestDifficultyTierReached(section.getInt("highestDifficultyTierReached", 0));

		return data;
	}

	private WitherData parseWitherData(Section section) {
		WitherData data = new WitherData();
		if (section == null)
			return data;

		data.setTotalSpawns(section.getInt("totalSpawns", 0));
		data.setKills(section.getInt("kills", 0));
		data.setTotalMinionWaves(section.getInt("totalMinionWaves", 0));
		data.setTotalHeals(section.getInt("totalHeals", 0));
		data.setDespawns(section.getInt("despawns", 0));
		data.setShortestFightMillis(section.getLong("shortestFightMillis", 0L));
		data.setLongestFightMillis(section.getLong("longestFightMillis", 0L));
		data.setLastSpawnTime(section.getLong("lastSpawnTime", 0L));

		return data;
	}

	private SkysiegeData parseSkysiegeData(Section section) {
		SkysiegeData data = new SkysiegeData();
		if (section == null)
			return data;

		data.setTotalSkysieges(section.getInt("totalSkysieges", 0));
		data.setSuccessfulSkysieges(section.getInt("successfulSkysieges", 0));
		data.setFailedSkysieges(section.getInt("failedSkysieges", 0));
		data.setQueenKills(section.getInt("queenKills", 0));
		data.setTotalGhastsKilled(section.getInt("totalGhastsKilled", 0));
		data.setTotalWavesCompleted(section.getInt("totalWavesCompleted", 0));
		data.setShortestDurationMillis(section.getLong("shortestDurationMillis", 0L));
		data.setLongestDurationMillis(section.getLong("longestDurationMillis", 0L));
		data.setLastSkysiegeTime(section.getLong("lastSkysiegeTime", 0L));

		return data;
	}

	private void serializePlayerDataToYaml(PlayerData playerData, YamlDocument data) {
		// General Info
		data.set("name", playerData.getName());
		data.set("dataVersion", playerData.getVersion());
		if (playerData.getEarningData().getEarnings() > 0.0D)
			data.set("earningData.date", playerData.getEarningData().getDate().toString());
		setIfNotZero(data, "earningData.earnings", playerData.getEarningData().getEarnings());

		// Only serialize placed blocks / pistons for the island owner
		if (playerData.getHellblockData().getOwnerUUID() != null
				&& playerData.getUUID().equals(playerData.getHellblockData().getOwnerUUID())) {
			serializeLevelBlocks(data, playerData.getLocationCacheData().getPlacedBlocks());
			cleanIfEmpty(data, "locationCacheData.placedBlocks");

			Map<Integer, List<String>> pistonsByIsland = playerData.getLocationCacheData().getPistonLocationsByIsland();
			if (pistonsByIsland != null && !pistonsByIsland.isEmpty()) {
				Section pistonSection = ensureSection(data, "locationCacheData.cachedPistons");

				pistonsByIsland.forEach((islandId, pistons) -> {
					if (pistons != null && !pistons.isEmpty()) {
						pistonSection.set(String.valueOf(islandId), pistons);
					}
				});

				cleanIfEmpty(data, "locationCacheData.cachedPistons");
			}
		}

		setIfNotDefault(data, "notificationSettings.joinNotifications",
				playerData.getNotificationSettings().hasJoinNotifications(), true);
		setIfNotDefault(data, "notificationSettings.inviteNotifications",
				playerData.getNotificationSettings().hasInviteNotifications(), true);

		// Mailbox
		if (!playerData.getMailbox().isEmpty()) {
			data.set("mailboxEntries", playerData.getMailbox());
			cleanIfEmpty(data, "mailboxEntries");
		}

		// Stats
		Section statSection = ensureSection(data, "statisticData.stats");
		writeMapToSection(statSection.createSection("amount"), playerData.getStatisticData().getAmountMap());
		writeMapToSection(statSection.createSection("size"), playerData.getStatisticData().getSizeMap());

		// Invitations
		if (!playerData.getHellblockData().getInvitations().isEmpty()) {
			Section inviteSection = ensureSection(data, "hellblockData.islandInvitations");
			playerData.getHellblockData().getInvitations().forEach((uuid, time) -> {
				if (time > 0)
					inviteSection.set(uuid.toString(), time);
			});
			cleanIfEmpty(data, "hellblockData.islandInvitations");
		}

		setIfNotZero(data, "hellblockData.lastIslandActivity", playerData.getHellblockData().getLastIslandActivity());

		// Challenges
		Map<ChallengeType, ChallengeResult> challenges = playerData.getChallengeData().getChallenges();
		if (!challenges.isEmpty()) {
			Section challengeSection = ensureSection(data, "challengeData.challenges");
			for (Map.Entry<ChallengeType, ChallengeResult> entry : challenges.entrySet()) {
				ChallengeResult result = entry.getValue();
				if (result.getStatus() == CompletionStatus.NOT_STARTED)
					continue;

				String key = entry.getKey().toString();
				challengeSection.set(key + ".completionStatus", result.getStatus().toString());

				if (result.getStatus() == CompletionStatus.IN_PROGRESS) {
					challengeSection.set(key + ".progress", result.getProgress());
				} else if (result.getStatus() == CompletionStatus.COMPLETED && result.isRewardClaimed()) {
					challengeSection.set(key + ".claimedReward", true);
				}
			}
			cleanIfEmpty(data, "challengeData.challenges");
		}

		// Hellblock
		if (!playerData.getHellblockData().hasHellblock())
			return;
		HellblockData hellblock = playerData.getHellblockData();
		data.set("hellblockData.hellblockExists", true);
		setIfNotNull(data, "hellblockData.ownerUUID", hellblock.getOwnerUUID());
		setIfNotZero(data, "hellblockData.islandId", hellblock.getIslandId());
		setIfNotZero(data, "hellblockData.islandLevel", hellblock.getIslandLevel());
		setIfNotNull(data, "hellblockData.linkedPortalUUID", hellblock.getLinkedPortalUUID());
		setIfNotZero(data, "hellblockData.lastWorldAccess", hellblock.getLastWorldAccess());

		// Party / Trusted / Banned
		setIfNotEmpty(data, "hellblockData.partyMembers", uuidSetToString(hellblock.getPartyMembers()));
		setIfNotEmpty(data, "hellblockData.trustedMembers", uuidSetToString(hellblock.getTrustedMembers()));
		setIfNotEmpty(data, "hellblockData.bannedMembers", uuidSetToString(hellblock.getBannedMembers()));

		// Flags
		Section flagSection = ensureSection(data, "hellblockData.protectionFlags");
		hellblock.getProtectionFlags().forEach((type, flag) -> {
			if (!flag.isDefault()) {
				Section sub = flagSection.createSection(type.toString());
				sub.set("allowedStatus", flag.getStatus().toString());
				setIfNotNull(sub, "stringData", flag.getData());
			}
		});
		cleanIfEmpty(data, "hellblockData.protectionFlags");

		// Upgrades
		Section upgradeSection = ensureSection(data, "hellblockData.islandUpgrades");
		hellblock.getIslandUpgrades().forEach((type, tier) -> {
			if (tier > 0)
				upgradeSection.set(type.toString(), tier);
		});
		cleanIfEmpty(data, "hellblockData.islandUpgrades");

		// Bounding Box
		if (hellblock.getBoundingBox() != null) {
			BoundingBox box = hellblock.getBoundingBox();
			data.set("hellblockData.islandBounds.minX", box.getMinX());
			data.set("hellblockData.islandBounds.minY", box.getMinY());
			data.set("hellblockData.islandBounds.minZ", box.getMinZ());
			data.set("hellblockData.islandBounds.maxX", box.getMaxX());
			data.set("hellblockData.islandBounds.maxY", box.getMaxY());
			data.set("hellblockData.islandBounds.maxZ", box.getMaxZ());
		}

		// Display
		DisplaySettings display = hellblock.getDisplaySettings();
		setIfNotEqual(data, "hellblockData.displaySettings.islandName", display.getIslandName(),
				hellblock.getDefaultIslandName());
		setIfNotEqual(data, "hellblockData.displaySettings.islandBio", display.getIslandBio(),
				hellblock.getDefaultIslandBio());
		setIfNotEqual(data, "hellblockData.displaySettings.displayChoice", display.getDisplayChoice(),
				DisplayChoice.CHAT);
		setIfNotDefault(data, "hellblockData.displaySettings.isDefaultIslandName", display.isDefaultIslandName(), true);
		setIfNotDefault(data, "hellblockData.displaySettings.isDefaultIslandBio", display.isDefaultIslandBio(), true);

		// Locations
		if (hellblock.getHomeLocation() != null)
			LocationUtils.serializeLocation(ensureSection(data, "hellblockData.homeLocation"),
					hellblock.getHomeLocation(), true);
		if (hellblock.getHellblockLocation() != null)
			LocationUtils.serializeLocation(ensureSection(data, "hellblockData.hellblockLocation"),
					hellblock.getHellblockLocation(), false);

		// Creation & Timers
		setIfNotZero(data, "hellblockData.creationTime", hellblock.getCreationTime());
		setIfNotZero(data, "hellblockData.resetCooldown", hellblock.getResetCooldown());
		setIfNotZero(data, "hellblockData.biomeCooldown", hellblock.getBiomeCooldown());
		setIfNotZero(data, "hellblockData.transferCooldown", hellblock.getTransferCooldown());
		setIfTrue(data, "hellblockData.isLocked", hellblock.isLocked());
		setIfTrue(data, "hellblockData.isAbandoned", hellblock.isAbandoned());

		// Biome / Island Choice
		if (hellblock.getBiome() != null && hellblock.getBiome() != HellBiome.NETHER_WASTES)
			data.set("hellblockData.islandBiome", hellblock.getBiome().toString());

		if (hellblock.getChatSetting() != CoopChatSetting.GLOBAL)
			data.set("hellblockData.chatPreference", hellblock.getChatSetting().toString());

		setIfNotNull(data, "hellblockData.islandChoice", safeToString(hellblock.getIslandChoice()));
		setIfNotNull(data, "hellblockData.usedSchematic", hellblock.getUsedSchematic());

		// Visitors
		serializeVisitData(hellblock.getVisitData(), data);
		serializeRecentVisitors(hellblock.getRecentVisitors(), data);

		// Events
		serializeInvasionData(hellblock.getInvasionData(), data);
		serializeWitherData(hellblock.getWitherData(), data);
		serializeSkysiegeData(hellblock.getSkysiegeData(), data);
	}

	private void setIfNotZero(YamlDocument yaml, String path, int value) {
		if (value != 0)
			yaml.set(path, value);
	}

	private void setIfNotZero(YamlDocument yaml, String path, long value) {
		if (value != 0L)
			yaml.set(path, value);
	}

	private void setIfNotZero(YamlDocument yaml, String path, double value) {
		if (value != 0.0)
			yaml.set(path, value);
	}

	private void setIfNotZero(YamlDocument yaml, String path, float value) {
		if (value != 0.0f)
			yaml.set(path, value);
	}

	private void setIfNotZero(Section section, String path, int value) {
		if (value != 0)
			section.set(path, value);
	}

	private void setIfNotZero(Section section, String path, long value) {
		if (value != 0L)
			section.set(path, value);
	}

	private void setIfTrue(YamlDocument yaml, String path, boolean condition) {
		if (condition)
			yaml.set(path, true);
	}

	private void setIfNotDefault(YamlDocument yaml, String path, boolean condition, boolean defaultValue) {
		if (condition != defaultValue)
			yaml.set(path, condition);
	}

	private void setIfNotNull(YamlDocument yaml, String path, Object value) {
		if (value != null)
			yaml.set(path, value.toString());
	}

	private void setIfNotNull(Section section, String path, Object value) {
		if (value != null)
			section.set(path, value.toString());
	}

	private void setIfNotEmpty(YamlDocument yaml, String path, Collection<?> collection) {
		if (collection != null && !collection.isEmpty()) {
			yaml.set(path, collection);
		}
	}

	private void setIfNotEqual(YamlDocument yaml, String path, Object value, Object defaultValue) {
		if (value != null && !value.equals(defaultValue)) {
			yaml.set(path, value.toString());
		}
	}

	private void cleanIfEmpty(YamlDocument yaml, String section) {
		Section s = yaml.getSection(section);
		if (s != null && s.getKeys().isEmpty()) {
			yaml.set(section, null);
		}
	}

	private Section ensureSection(YamlDocument yaml, String path) {
		Section section = yaml.getSection(path);
		return (section != null) ? section : yaml.createSection(path);
	}

	private UUID tryParseUUID(String str) {
		try {
			return str != null ? UUID.fromString(str) : null;
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private <T extends Enum<T>> T tryParseEnum(String str, Class<T> enumClass, T defaultValue) {
		try {
			return (str != null) ? Enum.valueOf(enumClass, str.toUpperCase(Locale.ENGLISH)) : defaultValue;
		} catch (IllegalArgumentException ex) {
			return defaultValue;
		}
	}

	private String safeToString(Enum<?> e) {
		return e != null ? e.toString() : null;
	}

	private Set<UUID> readUUIDSet(List<String> list) {
		return list.stream().map(this::tryParseUUID).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private Map<String, Map<String, Integer>> readNestedMap(Section root) {
		Map<String, Map<String, Integer>> result = new HashMap<>();
		if (root == null)
			return result;

		for (String chunkKey : root.getRoutesAsStrings(false)) {
			Section chunkSection = root.getSection(chunkKey);
			if (chunkSection == null)
				continue;

			Map<String, Integer> blockMap = new HashMap<>();
			chunkSection.getRoutesAsStrings(false)
					.forEach(blockKey -> blockMap.put(blockKey, chunkSection.getInt(blockKey)));
			result.put(chunkKey, blockMap);
		}

		return result;
	}

	private Map<Integer, List<String>> readIntegerListMap(Section root) {
		Map<Integer, List<String>> result = new HashMap<>();
		if (root == null)
			return result;

		root.getRoutesAsStrings(false).forEach(key -> {
			try {
				int islandId = Integer.parseInt(key);
				List<String> pistons = root.getStringList(key);
				if (!pistons.isEmpty()) {
					result.put(islandId, pistons);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid keys
			}
		});
		return result;
	}

	private Map<UUID, Long> readUUIDLongMap(Section section) {
		Map<UUID, Long> result = new HashMap<>();
		if (section != null) {
			section.getKeys().forEach(key -> {
				UUID uuid = tryParseUUID(key.toString());
				long time = section.getLong(key.toString(), 0L);
				if (uuid != null && time > 0L)
					result.put(uuid, time);
			});
		}
		return result;
	}

	private <K extends Enum<K>> EnumMap<K, Integer> readEnumIntMap(Section section, Class<K> keyType) {
		EnumMap<K, Integer> map = new EnumMap<>(keyType);
		if (section != null) {
			section.getKeys().forEach(key -> {
				try {
					K enumKey = Enum.valueOf(keyType, key.toString().toUpperCase(Locale.ENGLISH));
					map.put(enumKey, section.getInt(key.toString(), 0));
				} catch (Exception ignored) {
				}
			});
		}
		return map;
	}

	private List<String> uuidSetToString(Set<UUID> set) {
		return set.stream().filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toList());
	}

	private void writeMapToSection(Section section, Map<String, ?> map) {
		map.forEach(section::set);
	}

	private void serializeLevelBlocks(YamlDocument data, Map<String, Map<String, Integer>> placedBlocks) {
		if (placedBlocks == null || placedBlocks.isEmpty())
			return;

		Section levelBlocksSection = ensureSection(data, "locationCacheData.placedBlocks");

		placedBlocks.entrySet().forEach(chunkEntry -> {
			String chunkKey = chunkEntry.getKey();
			Section chunkSection = levelBlocksSection.createSection(chunkKey);

			chunkEntry.getValue().entrySet()
					.forEach(blockEntry -> chunkSection.set(blockEntry.getKey(), blockEntry.getValue()));
		});
	}

	private void serializeVisitData(VisitData visitData, YamlDocument data) {
		if (visitData == null)
			return;

		if (visitData.getWarpLocation() != null) {
			Section warpSection = ensureSection(data, "hellblockData.visitData.warpLocation");
			LocationUtils.serializeLocation(warpSection, visitData.getWarpLocation(), true);
		}
		setIfNotZero(data, "hellblockData.visitData.totalVisits", visitData.getTotalVisits());
		setIfNotZero(data, "hellblockData.visitData.visitsToday", visitData.getDailyVisits());
		setIfNotZero(data, "hellblockData.visitData.visitsThisWeek", visitData.getWeeklyVisits());
		setIfNotZero(data, "hellblockData.visitData.visitsThisMonth", visitData.getMonthlyVisits());
		if (visitData.hasVisits())
			setIfNotZero(data, "hellblockData.visitData.lastVisitReset", visitData.getLastVisitReset());
		setIfNotZero(data, "hellblockData.visitData.featuredUntil", visitData.getFeaturedUntil());

		// Clean up empty section
		cleanIfEmpty(data, "hellblockData.visitData");
	}

	private void serializeRecentVisitors(List<VisitRecord> visitors, YamlDocument data) {
		if (visitors == null || visitors.isEmpty())
			return;

		Section section = ensureSection(data, "hellblockData.recentVisitors");

		for (int i = 0; i < visitors.size(); i++) {
			VisitRecord record = visitors.get(i);
			Section entry = section.createSection(String.valueOf(i));
			entry.set("visitorId", record.getVisitorId().toString());
			entry.set("timestamp", record.getTimestamp());
		}
	}

	private void serializeInvasionData(InvasionData dataModel, YamlDocument data) {
		if (dataModel == null)
			return;

		Section section = ensureSection(data, "hellblockData.invasionData");
		setIfNotZero(section, "totalInvasions", dataModel.getTotalInvasions());
		setIfNotZero(section, "successfulInvasions", dataModel.getSuccessfulInvasions());
		setIfNotZero(section, "failedInvasions", dataModel.getFailedInvasions());
		setIfNotZero(section, "bossKills", dataModel.getBossKills());
		setIfNotZero(section, "currentStreak", dataModel.getCurrentStreak());
		setIfNotZero(section, "lastInvasionTime", dataModel.getLastInvasionTime());
		setIfNotZero(section, "highestDifficultyTierReached", dataModel.getHighestDifficultyTierReached());

		cleanIfEmpty(data, "hellblockData.invasionData");
	}

	private void serializeWitherData(WitherData dataModel, YamlDocument data) {
		if (dataModel == null)
			return;

		Section section = ensureSection(data, "hellblockData.witherData");
		setIfNotZero(section, "totalSpawns", dataModel.getTotalSpawns());
		setIfNotZero(section, "kills", dataModel.getKills());
		setIfNotZero(section, "totalMinionWaves", dataModel.getTotalMinionWaves());
		setIfNotZero(section, "totalHeals", dataModel.getTotalHeals());
		setIfNotZero(section, "despawns", dataModel.getDespawns());
		setIfNotZero(section, "shortestFightMillis", dataModel.getShortestFightMillis());
		setIfNotZero(section, "longestFightMillis", dataModel.getLongestFightMillis());
		setIfNotZero(section, "lastSpawnTime", dataModel.getLastSpawnTime());

		cleanIfEmpty(data, "hellblockData.witherData");
	}

	private void serializeSkysiegeData(SkysiegeData dataModel, YamlDocument data) {
		if (dataModel == null)
			return;

		Section section = ensureSection(data, "hellblockData.skysiegeData");
		setIfNotZero(section, "totalSkysieges", dataModel.getTotalSkysieges());
		setIfNotZero(section, "successfulSkysieges", dataModel.getSuccessfulSkysieges());
		setIfNotZero(section, "failedSkysieges", dataModel.getFailedSkysieges());
		setIfNotZero(section, "queenKills", dataModel.getQueenKills());
		setIfNotZero(section, "totalGhastsKilled", dataModel.getTotalGhastsKilled());
		setIfNotZero(section, "totalWavesCompleted", dataModel.getTotalWavesCompleted());
		setIfNotZero(section, "shortestDurationMillis", dataModel.getShortestDurationMillis());
		setIfNotZero(section, "longestDurationMillis", dataModel.getLongestDurationMillis());
		setIfNotZero(section, "lastSkysiegeTime", dataModel.getLastSkysiegeTime());

		cleanIfEmpty(data, "hellblockData.skysiegeData");
	}

	/**
	 * Get the file associated with a player's UUID for storing YAML data.
	 *
	 * @param uuid The UUID of the player.
	 * @return The file for the player's data.
	 */
	public File getPlayerDataFile(UUID uuid) {
		return new File(dataFolder, uuid + ".yml");
	}

	public File getPlayerDataFolder() {
		return this.dataFolder;
	}

	@Override
	public Set<UUID> getUniqueUsers() {
		final Set<UUID> uuids = new HashSet<>();
		if (dataFolder.exists()) {
			File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
			if (files != null) {
				for (File file : files) {
					try {
						String uuidStr = Files.getNameWithoutExtension(file.getName());
						uuids.add(UUID.fromString(uuidStr));
					} catch (IllegalArgumentException ex) {
						plugin.getPluginLogger().warn("Invalid UUID filename in YAML data: " + file.getName());
						file.delete(); // optional cleanup
					}
				}
			}
		}
		return uuids;
	}

	private StatisticData getStatistics(Section section) {
		final Map<String, Integer> amountMap = new HashMap<>();
		final Map<String, Float> sizeMap = new HashMap<>();
		if (section == null) {
			return new StatisticData(amountMap, sizeMap);
		}
		final Section amountSection = section.getSection("amount");
		if (amountSection != null) {
			amountSection.getStringRouteMappedValues(false).entrySet()
					.forEach(entry -> amountMap.put(entry.getKey(), (Integer) entry.getValue()));
		}
		final Section sizeSection = section.getSection("size");
		if (sizeSection != null) {
			sizeSection.getStringRouteMappedValues(false).entrySet()
					.forEach(entry -> sizeMap.put(entry.getKey(), ((Double) entry.getValue()).floatValue()));
		}
		return new StatisticData(amountMap, sizeMap);
	}

	@Override
	public void invalidateCache(UUID uuid) {
		memoryCache.invalidate(uuid);
	}

	@Override
	public void clearCache() {
		memoryCache.invalidateAll();
	}

	@Override
	public void invalidateIslandCache(int islandId) {
		islandIdToUUIDCache.invalidate(islandId);
	}

	@Override
	public boolean isPendingInsert(UUID uuid) {
		return false;
	}

	@Override
	public boolean isInsertStillRecent(UUID uuid) {
		return false;
	}

	@Override
	public Long getInsertAge(UUID uuid) {
		return null;
	}

	@Override
	public CompletableFuture<Void> getInsertFuture(UUID uuid) {
		return CompletableFuture.completedFuture(null);
	}
}