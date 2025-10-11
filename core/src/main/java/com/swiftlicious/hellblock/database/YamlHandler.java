package com.swiftlicious.hellblock.database;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import com.google.common.io.Files;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeResult;
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.VisitManager;
import com.swiftlicious.hellblock.handlers.VisitManager.VisitRecord;
import com.swiftlicious.hellblock.player.ChallengeData;
import com.swiftlicious.hellblock.player.DisplaySettings;
import com.swiftlicious.hellblock.player.DisplaySettings.DisplayChoice;
import com.swiftlicious.hellblock.player.EarningData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.LocationCacheData;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.StatisticData;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.utils.LocationUtils;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.text.Component;

/**
 * A data storage implementation that uses YAML files to store player data, with
 * support for legacy data.
 */
public class YamlHandler extends AbstractStorage {

	public YamlHandler(HellblockPlugin plugin) {
		super(plugin);
		final File folder = new File(plugin.getDataFolder(), "data");
		if (!folder.exists()) {
			folder.mkdirs();
		}
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.YAML;
	}

	/**
	 * Get the file associated with a player's UUID for storing YAML data.
	 *
	 * @param uuid The UUID of the player.
	 * @return The file for the player's data.
	 */
	public File getPlayerDataFile(UUID uuid) {
		return new File(plugin.getDataFolder(), "data" + File.separator + uuid + ".yml");
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock, Executor executor) {
		final File dataFile = getPlayerDataFile(uuid);
		if (!dataFile.exists()) {
			if (Bukkit.getPlayer(uuid) != null) {
				return CompletableFuture.completedFuture(Optional.of(PlayerData.empty()));
			} else {
				return CompletableFuture.completedFuture(Optional.empty());
			}
		}
		final YamlDocument data = plugin.getConfigManager().loadData(dataFile);

		final Set<UUID> party = new HashSet<>();
		final Set<UUID> trusted = new HashSet<>();
		final Set<UUID> banned = new HashSet<>();
		final Map<UUID, Long> invitations = new HashMap<>();
		final Map<FlagType, AccessType> flags = new HashMap<>();
		final EnumMap<IslandUpgradeType, Integer> upgrades = new EnumMap<>(IslandUpgradeType.class);
		final Map<ChallengeType, ChallengeResult> challenges = new HashMap<>();
		final List<MailboxEntry> mailbox = new ArrayList<>();
		DisplaySettings display = null;
		data.getStringList("party").forEach(id -> party.add(UUID.fromString(id)));
		data.getStringList("trusted").forEach(id -> trusted.add(UUID.fromString(id)));
		data.getStringList("banned").forEach(id -> banned.add(UUID.fromString(id)));
		if (data.getSection("invitations") != null) {
			data.getSection("invitations").getKeys().forEach(key -> {
				final UUID invitee = UUID.fromString(key.toString());
				final long expirationTime = data.getLong("invitations." + key.toString());
				invitations.put(invitee, expirationTime);
			});
		}
		if (data.getSection("flags") != null) {
			data.getSection("flags").getKeys().forEach(key -> {
				final FlagType flag = FlagType.valueOf(key.toString().toUpperCase(Locale.ENGLISH));
				final AccessType status = AccessType
						.valueOf(data.getString("flags." + key.toString()).toUpperCase(Locale.ENGLISH));
				flags.put(flag, status);
			});
		}
		if (data.getSection("upgrades") != null) {
			data.getSection("upgrades").getKeys().forEach(key -> {
				final IslandUpgradeType upgradeType = IslandUpgradeType
						.valueOf(key.toString().toUpperCase(Locale.ENGLISH));
				final int tierLevel = data.getInt("upgrades." + key.toString(), 0);
				upgrades.put(upgradeType, tierLevel);
			});
		}
		if (data.getSection("challenges") != null) {
			data.getSection("challenges").getKeys().forEach(key -> {
				final ChallengeType challenge = HellblockPlugin.getInstance().getChallengeManager()
						.getById(key.toString());
				if (challenge != null) {
					final CompletionStatus completion = CompletionStatus.valueOf(
							data.getString("challenges." + key.toString() + ".status").toUpperCase(Locale.ENGLISH));
					final int progress = data.getInt("challenges." + key.toString() + ".progress",
							challenge.getNeededAmount());
					final boolean claimedReward = data.getBoolean("challenges." + key.toString() + ".claimed-reward",
							false);
					challenges.put(challenge, new ChallengeResult(completion, progress, claimedReward));
				}
			});
		}
		final BoundingBox bounds = new BoundingBox(data.getDouble("bounds.min-x"), data.getDouble("bounds.min-y"),
				data.getDouble("bounds.min-z"), data.getDouble("bounds.max-x"), data.getDouble("bounds.max-y"),
				data.getDouble("bounds.max-z"));
		if (data.getSection("display") != null) {
			String islandName = data.getString("display.name");
			String bio = data.getString("display.bio");
			DisplayChoice displayChoice = DisplayChoice
					.valueOf(data.getString("display.choice").toUpperCase(Locale.ENGLISH));
			display = new DisplaySettings(islandName, bio, displayChoice);
			boolean defaultName = data.getBoolean("display.default-name");
			boolean defaultBio = data.getBoolean("display.default-bio");
			if (defaultName) {
				display.setAsDefaultIslandName();
			} else {
				display.isNotDefaultIslandName();
			}
			if (defaultBio) {
				display.setAsDefaultIslandBio();
			} else {
				display.isNotDefaultIslandBio();
			}
		}
		Location home = null;
		Location location = null;
		if (data.getSection("home") != null) {
			home = LocationUtils.deserializeLocation(data.getSection("home"));
		}
		if (data.getSection("location") != null) {
			location = LocationUtils.deserializeLocation(data.getSection("location"));
		}
		if (data.getSection("mailbox") != null) {
			Section mailboxSection = data.getSection("mailbox");

			mailboxSection.getKeys().stream().map(key -> mailboxSection.getSection(key.toString()))
					.forEach(entrySection -> {
						if (entrySection == null) {
							return;
						}
						String messageKey = entrySection.getString("messageKey");
						List<String> argStrings = entrySection.getStringList("arguments");
						List<Component> arguments = argStrings.stream().map(Component::text)
								.collect(Collectors.toList());
						List<String> flagStrings = entrySection.getStringList("flags");
						Set<MailboxFlag> mailboxFlags = flagStrings.stream().map(s -> {
							try {
								return MailboxFlag.valueOf(s.toUpperCase(Locale.ENGLISH));
							} catch (IllegalArgumentException e) {
								return null;
							}
						}).filter(Objects::nonNull).collect(Collectors.toSet());
						mailbox.add(new MailboxEntry(messageKey, arguments, mailboxFlags));
					});
		}
		VisitData visitData = new VisitData();
		if (data.getSection("visitors") != null) {
			Section visitSection = data.getSection("visitors");
			if (visitSection.getSection("warp") != null) {
				Section warpSection = visitSection.getSection("warp");
				visitData.setWarpLocation(LocationUtils.deserializeLocation(warpSection));
			}
			visitData.setTotalVisits(visitSection.getInt("total", 0));
			visitData.setVisitsToday(visitSection.getInt("daily", 0));
			visitData.setVisitsThisWeek(visitSection.getInt("weekly", 0));
			visitData.setVisitsThisMonth(visitSection.getInt("monthly", 0));
			visitData.setLastVisitReset(visitSection.getLong("last-reset", 0L));
			visitData.setFeaturedUntil(visitSection.getLong("featured-until", 0L));
		}
		List<VisitRecord> recentVisitors = new ArrayList<>();
		if (data.getSection("recent-visitors") != null) {
			Section recentSection = data.getSection("recent-visitors");

			VisitManager visitManager = HellblockPlugin.getInstance().getVisitManager();
			recentSection.getKeys().stream().map(key -> recentSection.getSection(key.toString()))
					.forEach(entrySection -> {
						if (entrySection == null) {
							return;
						}

						String uuidStr = entrySection.getString("visitor");
						long timestamp = entrySection.getLong("timestamp");

						try {
							UUID visitorId = UUID.fromString(uuidStr);
							recentVisitors.add(visitManager.new VisitRecord(visitorId, timestamp));
						} catch (IllegalArgumentException ignored) {
							// skip invalid UUID
						}
					});
		}
		String dateStr = data.getString("date", null);
		LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now(); // or some fallback
		final PlayerData playerData = PlayerData.builder()
				.setEarningData(new EarningData(data.getDouble("earnings", 0.0), date))
				.setStatisticData(getStatistics(data.getSection("stats")))
				.setHellblockData(new HellblockData(data.getInt("id", 0), data.getFloat("level", 0.0F),
						data.getBoolean("has-hellblock", false),
						data.getString("owner") != null ? UUID.fromString(data.getString("owner")) : null,
						data.getString("linked-hellblock") != null ? UUID.fromString(data.getString("linked-hellblock"))
								: null,
						bounds, display, party, trusted, banned, invitations, flags, upgrades, location, home,
						data.getLong("creation-time", 0L), visitData, recentVisitors,
						HellBiome.valueOf(data.getString("biome", "NETHER_WASTES").toUpperCase(Locale.ENGLISH)),
						data.getString("island-choice") != null
								? IslandOptions.valueOf(data.getString("island-choice").toUpperCase(Locale.ENGLISH))
								: null,
						data.getString("schematic"), data.getBoolean("locked", false),
						data.getBoolean("abandoned", false), data.getLong("reset-cooldown", 0L),
						data.getLong("biome-cooldown", 0L), data.getLong("transfer-cooldown", 0L)))
				.setChallengeData(new ChallengeData(challenges))
				.setLocationCacheData(new LocationCacheData(
						plugin.getConfigManager().pistonAutomation() ? data.getStringList("pistons")
								: new ArrayList<>(),
						data.getStringList("level-blocks")))
				.setName(data.getString("name", "")).setMailbox(mailbox)
				.setHellblockInviteNotifications(data.getBoolean("invite-notifications", true))
				.setHellblockJoinNotifications(data.getBoolean("join-notifications", true))
				.setLastActivity(data.getLong("last-activity", 0L)).build();
		return CompletableFuture.completedFuture(Optional.of(playerData));
	}

	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean ignore) {
		final YamlDocument data = plugin.getConfigManager().loadData(getPlayerDataFile(uuid));
		if (!playerData.getMailbox().isEmpty()) {
			if (data.getSection("mailbox") == null) {
				data.createSection("mailbox");
			}
			data.set("mailbox", playerData.getMailbox());
			if (data.getSection("mailbox") != null && data.getSection("mailbox").getKeys().isEmpty()) {
				data.set("mailbox", null);
			}
		}
		if (playerData.getLastActivity() > 0) {
			data.set("last-activity", playerData.getLastActivity());
		}
		if (!playerData.hasHellblockInviteNotifications()) {
			data.set("invite-notifications", playerData.hasHellblockInviteNotifications());
		}
		if (!playerData.hasHellblockJoinNotifications()) {
			data.set("join-notifications", playerData.hasHellblockJoinNotifications());
		}
		data.set("name", playerData.getName());
		if (playerData.getLocationCacheData().getPistonLocations() != null
				&& !playerData.getLocationCacheData().getPistonLocations().isEmpty()
				&& plugin.getConfigManager().pistonAutomation()) {
			final Set<String> pistonString = playerData.getLocationCacheData().getPistonLocations().stream()
					.filter(Objects::nonNull).collect(Collectors.toSet());
			if (!pistonString.isEmpty()) {
				data.set("pistons", pistonString);
			}
		}
		if (playerData.getLocationCacheData().getLevelBlockLocations() != null
				&& !playerData.getLocationCacheData().getLevelBlockLocations().isEmpty()) {
			final Set<String> levelBlockString = playerData.getLocationCacheData().getLevelBlockLocations().stream()
					.filter(Objects::nonNull).collect(Collectors.toSet());
			if (!levelBlockString.isEmpty()) {
				data.set("level-blocks", levelBlockString);
			}
		}
		data.set("date", playerData.getEarningData().getDate().toString());
		if (playerData.getEarningData().getEarnings() > 0.0) {
			data.set("earnings", playerData.getEarningData().getEarnings());
		}
		if (data.getSection("stats") == null) {
			final Section section = data.createSection("stats");
			if (section.getSection("amount") == null) {
				final Section amountSection = section.createSection("amount");
				playerData.getStatisticData().getAmountMap().entrySet()
						.forEach(entry -> amountSection.set(entry.getKey(), entry.getValue()));
			}
			if (section.getSection("size") == null) {
				final Section sizeSection = section.createSection("size");
				playerData.getStatisticData().getSizeMap().entrySet()
						.forEach(entry -> sizeSection.set(entry.getKey(), entry.getValue()));
			}
		}
		if (playerData.getHellblockData().getID() > 0) {
			data.set("id", playerData.getHellblockData().getID());
		}
		if (playerData.getHellblockData().getLevel() > HellblockData.DEFAULT_LEVEL) {
			data.set("level", playerData.getHellblockData().getLevel());
		}
		if (playerData.getHellblockData().hasHellblock()) {
			data.set("has-hellblock", playerData.getHellblockData().hasHellblock());
		}
		if (playerData.getHellblockData().getOwnerUUID() != null) {
			data.set("owner", playerData.getHellblockData().getOwnerUUID().toString());
		}
		if (playerData.getHellblockData().getLinkedUUID() != null
				&& (playerData.getHellblockData().getOwnerUUID() != null && !playerData.getHellblockData()
						.getLinkedUUID().equals(playerData.getHellblockData().getOwnerUUID()))
				&& !playerData.getHellblockData().getParty().contains(playerData.getHellblockData().getLinkedUUID())) {
			data.set("linked-hellblock", playerData.getHellblockData().getLinkedUUID().toString());
		}
		if (playerData.getHellblockData().hasHellblock() && playerData.getHellblockData().getBoundingBox() != null) {
			final BoundingBox bounds = playerData.getHellblockData().getBoundingBox();
			data.set("bounds.min-x", bounds.getMinX());
			data.set("bounds.min-y", bounds.getMinY());
			data.set("bounds.min-z", bounds.getMinZ());
			data.set("bounds.max-x", bounds.getMaxX());
			data.set("bounds.max-y", bounds.getMaxY());
			data.set("bounds.max-z", bounds.getMaxZ());
		}
		DisplaySettings display = playerData.getHellblockData().getDisplaySettings();
		if (!display.getIslandName().equalsIgnoreCase(playerData.getHellblockData().getDefaultIslandName())) {
			data.set("display.name", display.getIslandName());
		}
		if (!display.getIslandBio().equalsIgnoreCase(playerData.getHellblockData().getDefaultIslandBio())) {
			data.set("display.bio", display.getIslandBio());
		}
		if (display.getDisplayChoice() != DisplayChoice.CHAT) {
			data.set("display.choice", display.getDisplayChoice().toString());
		}
		if (!display.isDefaultIslandName()) {
			data.set("display.default-name", display.isDefaultIslandName());
		}
		if (!display.isDefaultIslandBio()) {
			data.set("display.default-bio", display.isDefaultIslandBio());
		}
		if (!playerData.getHellblockData().getParty().isEmpty()) {
			final Set<String> partyString = playerData.getHellblockData().getParty().stream().filter(Objects::nonNull)
					.map(UUID::toString).collect(Collectors.toSet());
			if (!partyString.isEmpty()) {
				data.set("party", partyString);
			}
		}
		if (!playerData.getHellblockData().getTrusted().isEmpty()) {
			final Set<String> trustedString = playerData.getHellblockData().getTrusted().stream()
					.filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toSet());
			if (!trustedString.isEmpty()) {
				data.set("trusted", trustedString);
			}
		}
		if (!playerData.getHellblockData().getBanned().isEmpty()) {
			final Set<String> bannedString = playerData.getHellblockData().getBanned().stream().filter(Objects::nonNull)
					.map(UUID::toString).collect(Collectors.toSet());
			if (!bannedString.isEmpty()) {
				data.set("banned", bannedString);
			}
		}
		if (!playerData.getHellblockData().getInvitations().isEmpty()) {
			if (data.getSection("invitations") == null) {
				data.createSection("invitations");
			}
			for (Map.Entry<UUID, Long> invites : playerData.getHellblockData().getInvitations().entrySet()) {
				if (invites.getValue() <= 0) {
					continue;
				}
				data.set("invitations." + invites.getKey().toString(), invites.getValue().longValue());
			}
			if (data.getSection("invitations") != null && data.getSection("invitations").getKeys().isEmpty()) {
				data.set("invitations", null);
			}
		}
		if (!playerData.getHellblockData().getProtectionFlags().isEmpty()) {
			if (data.getSection("flags") == null) {
				data.createSection("flags");
			}
			for (Map.Entry<FlagType, AccessType> flags : playerData.getHellblockData().getProtectionFlags()
					.entrySet()) {
				final AccessType returnValue = flags.getKey().getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
				if (flags.getValue() == returnValue) {
					continue;
				}
				data.set("flags." + flags.getKey().toString(), flags.getValue().toString());
			}
			if (data.getSection("flags") != null && data.getSection("flags").getKeys().isEmpty()) {
				data.set("flags", null);
			}
		}
		if (!playerData.getHellblockData().getIslandUpgrades().isEmpty()) {
			if (data.getSection("upgrades") == null) {
				data.createSection("upgrades");
			}
			for (Entry<IslandUpgradeType, Integer> upgrades : playerData.getHellblockData().getIslandUpgrades()
					.entrySet()) {
				if (upgrades.getValue().intValue() == 0) {
					continue;
				}
				data.set("upgrades." + upgrades.getKey().toString(), upgrades.getValue().toString());
			}
			if (data.getSection("upgrades") != null && data.getSection("upgrades").getKeys().isEmpty()) {
				data.set("upgrades", null);
			}
		}
		if (!playerData.getChallengeData().getChallenges().isEmpty()) {
			if (data.getSection("challenges") == null) {
				data.createSection("challenges");
			}
			for (Map.Entry<ChallengeType, ChallengeResult> challenges : playerData.getChallengeData().getChallenges()
					.entrySet()) {
				if (challenges.getValue().getStatus() == CompletionStatus.NOT_STARTED) {
					continue;
				}
				data.set("challenges." + challenges.getKey().toString() + ".status",
						challenges.getValue().getStatus().toString());
				if (challenges.getValue().getStatus() == CompletionStatus.IN_PROGRESS) {
					data.set("challenges." + challenges.getKey().toString() + ".progress",
							challenges.getValue().getProgress());
				}
				if (challenges.getValue().getStatus() == CompletionStatus.COMPLETED) {
					data.set("challenges." + challenges.getKey().toString() + ".progress", null);
					if (challenges.getValue().isRewardClaimed()) {
						data.set("challenges." + challenges.getKey().toString() + ".claimed-reward",
								challenges.getValue().isRewardClaimed());
					}
				}
			}
			if (data.getSection("challenges") != null && data.getSection("challenges").getKeys().isEmpty()) {
				data.set("challenges", null);
			}
		}
		if (playerData.getHellblockData().hasHellblock()) {
			if (playerData.getHellblockData().getHellblockLocation() != null) {
				if (data.getSection("location") == null) {
					data.createSection("location");
				}
				LocationUtils.serializeLocation(data.getSection("location"),
						playerData.getHellblockData().getHellblockLocation(), false);
			}
			if (playerData.getHellblockData().getHomeLocation() != null) {
				if (data.getSection("home") == null) {
					data.createSection("home");
				}
				LocationUtils.serializeLocation(data.getSection("home"),
						playerData.getHellblockData().getHomeLocation(), true);
			}
		}
		if (playerData.getHellblockData().getCreation() > 0) {
			data.set("creation-time", playerData.getHellblockData().getCreation());
		}
		VisitData visitData = playerData.getHellblockData().getVisitData();
		if (visitData.getWarpLocation() != null) {
			if (data.getSection("visitors.warp") == null) {
				data.createSection("visitors.warp");
			}
			LocationUtils.serializeLocation(data.getSection("visitors.warp"), visitData.getWarpLocation(), true);
		}
		if (visitData.getTotalVisits() > 0) {
			data.set("visitors.total", visitData.getTotalVisits());
		}
		if (visitData.getDailyVisits() > 0) {
			data.set("visitors.daily", visitData.getDailyVisits());
		}
		if (visitData.getWeeklyVisits() > 0) {
			data.set("visitors.weekly", visitData.getWeeklyVisits());
		}
		if (visitData.getMonthlyVisits() > 0) {
			data.set("visitors.monthly", visitData.getMonthlyVisits());
		}
		if (visitData.getLastVisitReset() > 0L) {
			data.set("visitors.last-reset", visitData.getLastVisitReset());
		}
		if (visitData.getFeaturedUntil() > 0L) {
			data.set("visitors.featured-until", visitData.getFeaturedUntil());
		}
		List<VisitRecord> visitors = playerData.getHellblockData().getRecentVisitors();
		if (!visitors.isEmpty()) {
			if (data.getSection("recent-visitors") == null) {
				data.createSection("recent-visitors");
			}
			Section recentSection = data.getSection("recent-visitors");

			for (int i = 0; i < visitors.size(); i++) {
				VisitRecord record = visitors.get(i);
				Section entry = recentSection.createSection(String.valueOf(i));
				entry.set("visitor", record.getVisitorId().toString());
				entry.set("timestamp", record.getTimestamp());
			}
		}
		if (playerData.getHellblockData().getBiome() != null
				&& playerData.getHellblockData().getBiome() != HellBiome.NETHER_WASTES) {
			data.set("biome", playerData.getHellblockData().getBiome().toString());
		}
		if (playerData.getHellblockData().getIslandChoice() != null) {
			data.set("island-choice", playerData.getHellblockData().getIslandChoice().toString());
		}
		if (playerData.getHellblockData().getIslandChoice() == IslandOptions.SCHEMATIC
				&& playerData.getHellblockData().getUsedSchematic() != null) {
			data.set("schematic", playerData.getHellblockData().getUsedSchematic());
		}
		if (playerData.getHellblockData().isLocked()) {
			data.set("locked", playerData.getHellblockData().isLocked());
		}
		if (playerData.getHellblockData().isAbandoned()) {
			data.set("abandoned", playerData.getHellblockData().isAbandoned());
		}
		if (playerData.getHellblockData().getResetCooldown() > 0) {
			data.set("reset-cooldown", playerData.getHellblockData().getResetCooldown());
		}
		if (playerData.getHellblockData().getBiomeCooldown() > 0) {
			data.set("biome-cooldown", playerData.getHellblockData().getBiomeCooldown());
		}
		if (playerData.getHellblockData().getTransferCooldown() > 0) {
			data.set("transfer-cooldown", playerData.getHellblockData().getTransferCooldown());
		}
		try {
			data.save(getPlayerDataFile(uuid));
		} catch (IOException ex) {
			plugin.getPluginLogger().warn("Failed to save player data.", ex);
		}
		return CompletableFuture.completedFuture(true);
	}

	@Override
	public Set<UUID> getUniqueUsers() {
		final File folder = new File(plugin.getDataFolder(), "data");
		final Set<UUID> uuids = new HashSet<>();
		if (folder.exists()) {
			final File[] files = folder.listFiles();
			if (files != null) {
				for (File file : files) {
					uuids.add(UUID.fromString(Files.getNameWithoutExtension(file.getName())));
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
}