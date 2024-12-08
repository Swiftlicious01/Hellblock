package com.swiftlicious.hellblock.database;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
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
import com.swiftlicious.hellblock.player.ChallengeData;
import com.swiftlicious.hellblock.player.EarningData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.LocationCacheData;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.StatisticData;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.utils.LocationUtils;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * A data storage implementation that uses YAML files to store player data, with
 * support for legacy data.
 */
public class YamlHandler extends AbstractStorage {

	public YamlHandler(HellblockPlugin plugin) {
		super(plugin);
		File folder = new File(plugin.getDataFolder(), "data");
		if (!folder.exists())
			folder.mkdirs();
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
		File dataFile = getPlayerDataFile(uuid);
		if (!dataFile.exists()) {
			if (Bukkit.getPlayer(uuid) != null) {
				return CompletableFuture.completedFuture(Optional.of(PlayerData.empty()));
			} else {
				return CompletableFuture.completedFuture(Optional.empty());
			}
		}
		YamlDocument data = plugin.getConfigManager().loadData(dataFile);

		Set<UUID> party = new HashSet<>(), trusted = new HashSet<>(), banned = new HashSet<>();
		Map<UUID, Long> invitations = new HashMap<>();
		Map<FlagType, AccessType> flags = new HashMap<>();
		Map<ChallengeType, ChallengeResult> challenges = new HashMap<>();
		data.getStringList("party").forEach(id -> party.add(UUID.fromString(id)));
		data.getStringList("trusted").forEach(id -> trusted.add(UUID.fromString(id)));
		data.getStringList("banned").forEach(id -> banned.add(UUID.fromString(id)));
		if (data.getSection("invitations") != null) {
			data.getSection("invitations").getKeys().forEach(key -> {
				UUID invitee = UUID.fromString(key.toString());
				long expirationTime = data.getLong("invitations." + key.toString());
				invitations.put(invitee, expirationTime);
			});
		}
		if (data.getSection("flags") != null) {
			data.getSection("flags").getKeys().forEach(key -> {
				FlagType flag = FlagType.valueOf(key.toString());
				AccessType status = AccessType.valueOf(data.getString("flags." + key.toString()));
				flags.put(flag, status);
			});
		}
		if (data.getSection("challenges") != null) {
			data.getSection("challenges").getKeys().forEach(key -> {
				ChallengeType challenge = HellblockPlugin.getInstance().getChallengeManager().getById(key.toString());
				if (challenge != null) {
					CompletionStatus completion = CompletionStatus
							.valueOf(data.getString("challenges." + key.toString() + ".status"));
					int progress = data.getInt("challenges." + key.toString() + ".progress",
							challenge.getNeededAmount());
					boolean claimedReward = data.getBoolean("challenges." + key.toString() + ".claimed-reward", false);
					challenges.put(challenge, new ChallengeResult(completion, progress, claimedReward));
				}
			});
		}
		BoundingBox bounds = new BoundingBox(data.getDouble("bounds.min-x"), data.getDouble("bounds.min-y"),
				data.getDouble("bounds.min-z"), data.getDouble("bounds.max-x"), data.getDouble("bounds.max-y"),
				data.getDouble("bounds.max-z"));
		Location home = null, location = null;
		if (data.getSection("home") != null)
			home = LocationUtils.deserializeLocation(data.getSection("home"));
		if (data.getSection("location") != null)
			location = LocationUtils.deserializeLocation(data.getSection("location"));
		PlayerData playerData = PlayerData.builder()
				.setEarningData(new EarningData(data.getDouble("earnings", 0.0), data.getInt("date")))
				.setStatisticData(getStatistics(data.getSection("stats")))
				.setHellblockData(new HellblockData(data.getInt("id", 0), data.getFloat("level", 0.0F),
						data.getBoolean("has-hellblock", false),
						data.getString("owner") != null ? UUID.fromString(data.getString("owner")) : null,
						data.getString("linked-hellblock") != null ? UUID.fromString(data.getString("linked-hellblock"))
								: null,
						bounds, party, trusted, banned, invitations, flags, location, home,
						data.getLong("creation-time", 0L), data.getInt("total-visitors", 0),
						HellBiome.valueOf(data.getString("biome", "NETHER_WASTES").toUpperCase()),
						data.getString("island-choice") != null
								? IslandOptions.valueOf(data.getString("island-choice").toUpperCase())
								: null,
						data.getString("schematic"), data.getBoolean("locked", false),
						data.getBoolean("abandoned", false), data.getLong("reset-cooldown", 0L),
						data.getLong("biome-cooldown", 0L), data.getLong("transfer-cooldown", 0L)))
				.setChallengeData(new ChallengeData(challenges))
				.setLocationCacheData(new LocationCacheData(
						plugin.getConfigManager().pistonAutomation() ? data.getStringList("pistons")
								: new ArrayList<>(),
						data.getStringList("level-blocks")))
				.setName(data.getString("name", "")).setInUnsafeLocation(data.getBoolean("unsafe", false))
				.setToClearItems(data.getBoolean("clearinv", false)).build();
		return CompletableFuture.completedFuture(Optional.of(playerData));
	}

	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean ignore) {
		YamlDocument data = plugin.getConfigManager().loadData(getPlayerDataFile(uuid));
		if (playerData.inUnsafeLocation())
			data.set("unsafe", playerData.inUnsafeLocation());
		if (playerData.isClearingItems())
			data.set("clearinv", playerData.isClearingItems());
		data.set("name", playerData.getName());
		if (playerData.getLocationCacheData().getPistonLocations() != null
				&& !playerData.getLocationCacheData().getPistonLocations().isEmpty()
				&& plugin.getConfigManager().pistonAutomation()) {
			Set<String> pistonString = playerData.getLocationCacheData().getPistonLocations().stream()
					.filter(Objects::nonNull).collect(Collectors.toSet());
			if (!pistonString.isEmpty())
				data.set("pistons", pistonString);
		}
		if (playerData.getLocationCacheData().getLevelBlockLocations() != null
				&& !playerData.getLocationCacheData().getLevelBlockLocations().isEmpty()) {
			Set<String> levelBlockString = playerData.getLocationCacheData().getLevelBlockLocations().stream()
					.filter(Objects::nonNull).collect(Collectors.toSet());
			if (!levelBlockString.isEmpty())
				data.set("level-blocks", levelBlockString);
		}
		data.set("date", playerData.getEarningData().getDate());
		if (playerData.getEarningData().getEarnings() > 0.0)
			data.set("earnings", playerData.getEarningData().getEarnings());
		if (data.getSection("stats") == null) {
			Section section = data.createSection("stats");
			if (section.getSection("amount") == null) {
				Section amountSection = section.createSection("amount");
				for (Map.Entry<String, Integer> entry : playerData.getStatisticData().getAmountMap().entrySet()) {
					amountSection.set(entry.getKey(), entry.getValue());
				}
			}
			if (section.getSection("size") == null) {
				Section sizeSection = section.createSection("size");
				for (Map.Entry<String, Float> entry : playerData.getStatisticData().getSizeMap().entrySet()) {
					sizeSection.set(entry.getKey(), entry.getValue());
				}
			}
		}
		if (playerData.getHellblockData().getID() > 0)
			data.set("id", playerData.getHellblockData().getID());
		if (playerData.getHellblockData().getLevel() > HellblockData.DEFAULT_LEVEL)
			data.set("level", playerData.getHellblockData().getLevel());
		if (playerData.getHellblockData().hasHellblock())
			data.set("has-hellblock", playerData.getHellblockData().hasHellblock());
		if (playerData.getHellblockData().getOwnerUUID() != null)
			data.set("owner", playerData.getHellblockData().getOwnerUUID().toString());
		if (playerData.getHellblockData().getLinkedUUID() != null
				&& (playerData.getHellblockData().getOwnerUUID() != null && !playerData.getHellblockData()
						.getLinkedUUID().equals(playerData.getHellblockData().getOwnerUUID()))
				&& !playerData.getHellblockData().getParty().contains(playerData.getHellblockData().getLinkedUUID()))
			data.set("linked-hellblock", playerData.getHellblockData().getLinkedUUID().toString());
		if (playerData.getHellblockData().hasHellblock() && playerData.getHellblockData().getBoundingBox() != null) {
			BoundingBox bounds = playerData.getHellblockData().getBoundingBox();
			data.set("bounds.min-x", bounds.getMinX());
			data.set("bounds.min-y", bounds.getMinY());
			data.set("bounds.min-z", bounds.getMinZ());
			data.set("bounds.max-x", bounds.getMaxX());
			data.set("bounds.max-y", bounds.getMaxY());
			data.set("bounds.max-z", bounds.getMaxZ());
		}
		if (playerData.getHellblockData().getParty() != null && !playerData.getHellblockData().getParty().isEmpty()) {
			Set<String> partyString = playerData.getHellblockData().getParty().stream().filter(Objects::nonNull)
					.map(UUID::toString).collect(Collectors.toSet());
			if (!partyString.isEmpty())
				data.set("party", partyString);
		}
		if (playerData.getHellblockData().getTrusted() != null
				&& !playerData.getHellblockData().getTrusted().isEmpty()) {
			Set<String> trustedString = playerData.getHellblockData().getTrusted().stream().filter(Objects::nonNull)
					.map(UUID::toString).collect(Collectors.toSet());
			if (!trustedString.isEmpty())
				data.set("trusted", trustedString);
		}
		if (playerData.getHellblockData().getBanned() != null && !playerData.getHellblockData().getBanned().isEmpty()) {
			Set<String> bannedString = playerData.getHellblockData().getBanned().stream().filter(Objects::nonNull)
					.map(UUID::toString).collect(Collectors.toSet());
			if (!bannedString.isEmpty())
				data.set("banned", bannedString);
		}
		if (playerData.getHellblockData().getInvitations() != null
				&& !playerData.getHellblockData().getInvitations().isEmpty()) {
			if (data.getSection("invitations") == null)
				data.createSection("invitations");
			for (Map.Entry<UUID, Long> invites : playerData.getHellblockData().getInvitations().entrySet()) {
				if (invites.getValue() == 0)
					continue;
				data.set("invitations." + invites.getKey().toString(), invites.getValue().longValue());
			}
			if (data.getSection("invitations") != null && data.getSection("invitations").getKeys().isEmpty()) {
				data.set("invitations", null);
			}
		}
		if (playerData.getHellblockData().getProtectionFlags() != null
				& !playerData.getHellblockData().getProtectionFlags().isEmpty()) {
			if (data.getSection("flags") == null)
				data.createSection("flags");
			for (Map.Entry<FlagType, AccessType> flags : playerData.getHellblockData().getProtectionFlags()
					.entrySet()) {
				AccessType returnValue = flags.getKey().getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
				if (flags.getValue() == returnValue)
					continue;
				data.set("flags." + flags.getKey().toString(), flags.getValue().toString());
			}
			if (data.getSection("flags") != null && data.getSection("flags").getKeys().isEmpty()) {
				data.set("flags", null);
			}
		}
		if (playerData.getChallengeData().getChallenges() != null
				&& !playerData.getChallengeData().getChallenges().isEmpty()) {
			if (data.getSection("challenges") == null)
				data.createSection("challenges");
			for (Map.Entry<ChallengeType, ChallengeResult> challenges : playerData.getChallengeData().getChallenges()
					.entrySet()) {
				if (challenges.getValue().getStatus() == CompletionStatus.NOT_STARTED)
					continue;
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
				if (data.getSection("location") == null)
					data.createSection("location");
				LocationUtils.serializeLocation(data.getSection("location"),
						playerData.getHellblockData().getHellblockLocation(), false);
			}
			if (playerData.getHellblockData().getHomeLocation() != null) {
				if (data.getSection("home") == null)
					data.createSection("home");
				LocationUtils.serializeLocation(data.getSection("home"),
						playerData.getHellblockData().getHomeLocation(), true);
			}
		}
		if (playerData.getHellblockData().getCreation() > 0)
			data.set("creation-time", playerData.getHellblockData().getCreation());
		if (playerData.getHellblockData().getTotalVisits() > 0)
			data.set("total-visitors", playerData.getHellblockData().getTotalVisits());
		if (playerData.getHellblockData().getBiome() != null
				&& playerData.getHellblockData().getBiome() != HellBiome.NETHER_WASTES)
			data.set("biome", playerData.getHellblockData().getBiome().toString());
		if (playerData.getHellblockData().getIslandChoice() != null)
			data.set("island-choice", playerData.getHellblockData().getIslandChoice().toString());
		if (playerData.getHellblockData().getIslandChoice() == IslandOptions.SCHEMATIC
				&& playerData.getHellblockData().getUsedSchematic() != null)
			data.set("schematic", playerData.getHellblockData().getUsedSchematic());
		if (playerData.getHellblockData().isLocked())
			data.set("locked", playerData.getHellblockData().isLocked());
		if (playerData.getHellblockData().isAbandoned())
			data.set("abandoned", playerData.getHellblockData().isAbandoned());
		if (playerData.getHellblockData().getResetCooldown() > 0)
			data.set("reset-cooldown", playerData.getHellblockData().getResetCooldown());
		if (playerData.getHellblockData().getBiomeCooldown() > 0)
			data.set("biome-cooldown", playerData.getHellblockData().getBiomeCooldown());
		if (playerData.getHellblockData().getTransferCooldown() > 0)
			data.set("transfer-cooldown", playerData.getHellblockData().getTransferCooldown());
		try {
			data.save(getPlayerDataFile(uuid));
		} catch (IOException ex) {
			plugin.getPluginLogger().warn("Failed to save player data.", ex);
		}
		return CompletableFuture.completedFuture(true);
	}

	@Override
	public Set<UUID> getUniqueUsers() {
		File folder = new File(plugin.getDataFolder(), "data");
		Set<UUID> uuids = new HashSet<>();
		if (folder.exists()) {
			File[] files = folder.listFiles();
			if (files != null) {
				for (File file : files) {
					uuids.add(UUID.fromString(Files.getNameWithoutExtension(file.getName())));
				}
			}
		}
		return uuids;
	}

	private StatisticData getStatistics(Section section) {
		HashMap<String, Integer> amountMap = new HashMap<>();
		HashMap<String, Float> sizeMap = new HashMap<>();
		if (section == null) {
			return new StatisticData(amountMap, sizeMap);
		}
		Section amountSection = section.getSection("amount");
		if (amountSection != null) {
			for (Map.Entry<String, Object> entry : amountSection.getStringRouteMappedValues(false).entrySet()) {
				amountMap.put(entry.getKey(), (Integer) entry.getValue());
			}
		}
		Section sizeSection = section.getSection("size");
		if (sizeSection != null) {
			for (Map.Entry<String, Object> entry : sizeSection.getStringRouteMappedValues(false).entrySet()) {
				sizeMap.put(entry.getKey(), ((Double) entry.getValue()).floatValue());
			}
		}
		return new StatisticData(amountMap, sizeMap);
	}
}