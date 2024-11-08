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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;

import com.google.common.io.Files;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeData;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.player.EarningData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

/**
 * A data storage implementation that uses YAML files to store player data, with
 * support for legacy data.
 */
public class YamlHandler extends AbstractStorage implements LegacyDataStorageInterface {

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
		return new File(HellblockPlugin.getInstance().getDataFolder(), "data" + File.separator + uuid + ".yml");
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid, boolean lock) {
		File dataFile = getPlayerDataFile(uuid);
		if (!dataFile.exists()) {
			if (Bukkit.getPlayer(uuid) != null) {
				return CompletableFuture.completedFuture(Optional.of(PlayerData.empty()));
			} else {
				return CompletableFuture.completedFuture(Optional.empty());
			}
		}
		YamlConfiguration data = HellblockPlugin.getInstance().getConfigUtils().readData(dataFile);

		Set<UUID> party = new HashSet<>(), trusted = new HashSet<>(), banned = new HashSet<>();
		Map<UUID, Long> invitations = new HashMap<>();
		Map<FlagType, AccessType> flags = new HashMap<>();
		Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges = new HashMap<>();
		data.getStringList("party").forEach(id -> party.add(UUID.fromString(id)));
		data.getStringList("trusted").forEach(id -> trusted.add(UUID.fromString(id)));
		data.getStringList("banned").forEach(id -> banned.add(UUID.fromString(id)));
		if (data.getConfigurationSection("invitations") != null) {
			data.getConfigurationSection("invitations").getKeys(false).forEach(key -> {
				UUID invitee = UUID.fromString(key);
				long expirationTime = data.getLong("invitations." + key);
				invitations.put(invitee, expirationTime);
			});
		}
		if (data.getConfigurationSection("flags") != null) {
			data.getConfigurationSection("flags").getKeys(false).forEach(key -> {
				FlagType flag = FlagType.valueOf(key);
				AccessType status = AccessType.valueOf(data.getString("flags." + key));
				flags.put(flag, status);
			});
		}
		if (data.getConfigurationSection("challenges") != null) {
			data.getConfigurationSection("challenges").getKeys(false).forEach(key -> {
				ChallengeType challenge = ChallengeType.valueOf(key);
				CompletionStatus completion = CompletionStatus.valueOf(data.getString("challenges." + key + ".status"));
				int progress = data.getInt("challenges." + key + ".progress", challenge.getNeededAmount());
				boolean claimedReward = data.getBoolean("challenges." + key + ".claimed-reward", false);
				challenges.put(challenge, new SimpleEntry<CompletionStatus, ChallengeData>(completion,
						new ChallengeData(progress, claimedReward)));
			});
		}
		BoundingBox bounds = new BoundingBox(data.getDouble("bounds.min-x"),
				HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld().getMinHeight(),
				data.getDouble("bounds.min-z"), data.getDouble("bounds.max-x"),
				HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld().getMaxHeight(),
				data.getDouble("bounds.max-z"));
		Location home = null, location = null;
		if (data.getConfigurationSection("home") != null)
			home = LocationUtils.deserializeLocation(data.getConfigurationSection("home"));
		if (data.getConfigurationSection("location") != null)
			location = LocationUtils.deserializeLocation(data.getConfigurationSection("location"));
		PlayerData playerData = PlayerData.builder()
				.setEarningData(new EarningData(data.getDouble("earnings", 0.0), data.getInt("date")))
				.setHellblockData(new HellblockData(data.getInt("id", 0), (float) data.getDouble("level", 0.0F),
						data.getBoolean("has-hellblock", false),
						data.getString("owner") != null ? UUID.fromString(data.getString("owner")) : null,
						data.getString("linked-hellblock") != null ? UUID.fromString(data.getString("linked-hellblock"))
								: null,
						bounds, party, trusted, banned, invitations, flags, challenges, location, home,
						data.getLong("creation-time", 0L), data.getInt("total-visitors", 0),
						HellBiome.valueOf(data.getString("biome", "NETHER_WASTES").toUpperCase()),
						data.getString("island-choice") != null
								? IslandOptions.valueOf(data.getString("island-choice").toUpperCase())
								: null,
						data.getString("schematic"), data.getBoolean("locked", false),
						data.getBoolean("abandoned", false), data.getLong("reset-cooldown", 0L),
						data.getLong("biome-cooldown", 0L), data.getLong("transfer-cooldown", 0L)))
				.setName(data.getString("name"))
				.setPistonLocations(instance.getConfig("config.yml")
						.getConfigurationSection("netherrack-generator-options.automation").getBoolean("pistons", false)
								? data.getStringList("pistons")
								: new ArrayList<>())
				.setLevelBlockLocations(data.getStringList("level-blocks")).build();
		return CompletableFuture.completedFuture(Optional.of(playerData));
	}

	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean ignore) {
		YamlConfiguration data = new YamlConfiguration();
		data.set("name", playerData.getName());
		if (playerData.getPistonLocations() != null && !playerData.getPistonLocations().isEmpty()
				&& instance.getConfig("config.yml").getConfigurationSection("netherrack-generator-options.automation")
						.getBoolean("pistons", false)) {
			Set<String> pistonString = playerData.getPistonLocations().stream().filter(Objects::nonNull)
					.collect(Collectors.toSet());
			if (!pistonString.isEmpty())
				data.set("pistons", pistonString);
		}
		if (playerData.getLevelBlockLocations() != null && !playerData.getLevelBlockLocations().isEmpty()) {
			Set<String> levelBlockString = playerData.getLevelBlockLocations().stream().filter(Objects::nonNull)
					.collect(Collectors.toSet());
			if (!levelBlockString.isEmpty())
				data.set("level-blocks", levelBlockString);
		}
		data.set("date", playerData.getEarningData().getDate());
		data.set("name", playerData.getName());
		if (playerData.getEarningData().getEarnings() > 0.0)
			data.set("earnings", playerData.getEarningData().getEarnings());
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
			data.set("bounds.min-x", playerData.getHellblockData().getBoundingBox().getMinX());
			data.set("bounds.min-z", playerData.getHellblockData().getBoundingBox().getMinZ());
			data.set("bounds.max-x", playerData.getHellblockData().getBoundingBox().getMaxX());
			data.set("bounds.max-z", playerData.getHellblockData().getBoundingBox().getMaxZ());
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
			if (data.getConfigurationSection("invitations") == null)
				data.createSection("invitations");
			for (Map.Entry<UUID, Long> invites : playerData.getHellblockData().getInvitations().entrySet()) {
				if (invites.getValue() == 0)
					continue;
				data.set("invitations." + invites.getKey().toString(), invites.getValue().longValue());
			}
			if (data.getConfigurationSection("invitations") != null
					&& data.getConfigurationSection("invitations").getKeys(false).isEmpty()) {
				data.set("invitations", null);
			}
		}
		if (playerData.getHellblockData().getProtectionFlags() != null
				& !playerData.getHellblockData().getProtectionFlags().isEmpty()) {
			if (data.getConfigurationSection("flags") == null)
				data.createSection("flags");
			for (Map.Entry<FlagType, AccessType> flags : playerData.getHellblockData().getProtectionFlags()
					.entrySet()) {
				AccessType returnValue = flags.getKey().getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
				if (flags.getValue() == returnValue)
					continue;
				data.set("flags." + flags.getKey().toString(), flags.getValue().toString());
			}
			if (data.getConfigurationSection("flags") != null
					&& data.getConfigurationSection("flags").getKeys(false).isEmpty()) {
				data.set("flags", null);
			}
		}
		if (playerData.getHellblockData().getChallenges() != null
				&& !playerData.getHellblockData().getChallenges().isEmpty()) {
			if (data.getConfigurationSection("challenges") == null)
				data.createSection("challenges");
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : playerData.getHellblockData()
					.getChallenges().entrySet()) {
				if (challenges.getValue().getKey() == CompletionStatus.NOT_STARTED)
					continue;
				data.set("challenges." + challenges.getKey().toString() + ".status",
						challenges.getValue().getKey().toString());
				if (challenges.getValue().getKey() == CompletionStatus.IN_PROGRESS) {
					data.set("challenges." + challenges.getKey().toString() + ".progress",
							challenges.getValue().getValue().getProgress());
				}
				if (challenges.getValue().getKey() == CompletionStatus.COMPLETED) {
					data.set("challenges." + challenges.getKey().toString() + ".progress", null);
					if (challenges.getValue().getValue().isRewardClaimed()) {
						data.set("challenges." + challenges.getKey().toString() + ".claimed-reward",
								challenges.getValue().getValue().isRewardClaimed());
					}
				}
			}
			if (data.getConfigurationSection("challenges") != null
					&& data.getConfigurationSection("challenges").getKeys(false).isEmpty()) {
				data.set("challenges", null);
			}
		}
		if (playerData.getHellblockData().hasHellblock()) {
			if (playerData.getHellblockData().getHellblockLocation() != null) {
				if (data.getConfigurationSection("location") == null)
					data.createSection("location");
				LocationUtils.serializeLocation(data.getConfigurationSection("location"),
						playerData.getHellblockData().getHellblockLocation(), false);
			}
			if (playerData.getHellblockData().getHomeLocation() != null) {
				if (data.getConfigurationSection("home") == null)
					data.createSection("home");
				LocationUtils.serializeLocation(data.getConfigurationSection("home"),
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
		} catch (IOException e) {
			LogUtils.warn("Failed to save player data.", e);
		}
		return CompletableFuture.completedFuture(true);
	}

	@Override
	public Set<UUID> getUniqueUsers(boolean legacy) {
		File folder;
		if (legacy) {
			folder = new File(HellblockPlugin.getInstance().getDataFolder(), "data/hellblock");
		} else {
			folder = new File(HellblockPlugin.getInstance().getDataFolder(), "data");
		}
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

	@Override
	public CompletableFuture<Optional<PlayerData>> getLegacyPlayerData(UUID uuid) {
		// Retrieve legacy player data (YAML format) for a given UUID.
		var builder = PlayerData.builder().setName("");

		File hbFile = new File(HellblockPlugin.getInstance().getDataFolder(), "data/hb/" + uuid + ".yml");
		if (hbFile.exists()) {
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(hbFile);
			Set<UUID> party = new HashSet<>(), trusted = new HashSet<>(), banned = new HashSet<>();
			Map<UUID, Long> invitations = new HashMap<>();
			Map<FlagType, AccessType> flags = new HashMap<>();
			Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges = new HashMap<>();
			yaml.getStringList("party").forEach(s -> party.add(UUID.fromString(s)));
			yaml.getStringList("trusted").forEach(s -> trusted.add(UUID.fromString(s)));
			yaml.getStringList("banned").forEach(s -> banned.add(UUID.fromString(s)));
			if (yaml.getConfigurationSection("invitations") != null) {
				yaml.getConfigurationSection("invitations").getKeys(false).forEach(key -> {
					UUID invitee = UUID.fromString(key);
					long expirationTime = yaml.getLong("invitations." + key);
					invitations.put(invitee, expirationTime);
				});
			}
			if (yaml.getConfigurationSection("flags") != null) {
				yaml.getConfigurationSection("flags").getKeys(false).forEach(key -> {
					FlagType flag = FlagType.valueOf(key);
					AccessType status = AccessType.valueOf(yaml.getString("flags." + key));
					flags.put(flag, status);
				});
			}
			if (yaml.getConfigurationSection("challenges") != null) {
				yaml.getConfigurationSection("challenges").getKeys(false).forEach(key -> {
					ChallengeType challenge = ChallengeType.valueOf(key);
					CompletionStatus completion = CompletionStatus
							.valueOf(yaml.getString("challenges." + key + ".status"));
					int progress = yaml.getInt("challenges." + key + ".progress", challenge.getNeededAmount());
					boolean claimedReward = yaml.getBoolean("challenges." + key + ".claimed-reward", false);
					challenges.put(challenge, new SimpleEntry<CompletionStatus, ChallengeData>(completion,
							new ChallengeData(progress, claimedReward)));
				});
			}
			BoundingBox bounds = new BoundingBox(yaml.getDouble("bounds.min-x"),
					HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld().getMinHeight(),
					yaml.getDouble("bounds.min-z"), yaml.getDouble("bounds.max-x"),
					HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld().getMaxHeight(),
					yaml.getDouble("bounds.max-z"));
			Location home = null, location = null;
			if (yaml.getConfigurationSection("home") != null)
				home = LocationUtils.deserializeLocation(yaml.getConfigurationSection("home"));
			if (yaml.getConfigurationSection("location") != null)
				location = LocationUtils.deserializeLocation(yaml.getConfigurationSection("location"));
			builder.setEarningData(new EarningData(yaml.getDouble("earnings", 0.0), yaml.getInt("date")));
			builder.setName(yaml.getString("name"));
			builder.setPistonLocations(
					instance.getConfig("config.yml").getConfigurationSection("netherrack-generator-options.automation")
							.getBoolean("pistons", false) ? yaml.getStringList("pistons") : new ArrayList<>());
			builder.setLevelBlockLocations(yaml.getStringList("level-blocks"));
			builder.setHellblockData(new HellblockData(yaml.getInt("id", 0), (float) yaml.getDouble("level", 0.0F),
					yaml.getBoolean("has-hellblock", false),
					yaml.getString("owner") != null ? UUID.fromString(yaml.getString("owner")) : null,
					yaml.getString("linked-hellblock") != null ? UUID.fromString(yaml.getString("linked-hellblock"))
							: null,
					bounds, party, trusted, banned, invitations, flags, challenges, location, home,
					yaml.getLong("creation-time", 0L), yaml.getInt("total-visitors", 0),
					yaml.getString("biome") != null ? HellBiome.valueOf(yaml.getString("biome").toUpperCase()) : null,
					yaml.getString("island-choice") != null
							? IslandOptions.valueOf(yaml.getString("island-choice").toUpperCase())
							: null,
					yaml.getString("schematic"), yaml.getBoolean("locked", false), yaml.getBoolean("abandoned", false),
					yaml.getLong("reset-cooldown", 0L), yaml.getLong("biome-cooldown", 0L),
					yaml.getLong("transfer-cooldown", 0L)));
		} else {
			builder.setEarningData(EarningData.empty());
			builder.setHellblockData(HellblockData.empty());
		}

		return CompletableFuture.completedFuture(Optional.of(builder.build()));
	}
}