package com.swiftlicious.hellblock.database;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.EarningData;
import com.swiftlicious.hellblock.playerdata.PlayerData;
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
				return CompletableFuture
						.completedFuture(Optional.of(PlayerData.empty()));
			} else {
				return CompletableFuture.completedFuture(Optional.empty());
			}
		}
		YamlConfiguration data = HellblockPlugin.getInstance().getConfigUtils().readData(dataFile);

		PlayerData playerData = PlayerData.builder()
				.setEarningData(new EarningData(data.getDouble("earnings"), data.getInt("date")))
				.setName(data.getString("name")).build();
		return CompletableFuture.completedFuture(Optional.of(playerData));
	}

	@Override
	public CompletableFuture<Boolean> updatePlayerData(UUID uuid, PlayerData playerData, boolean ignore) {
		YamlConfiguration data = new YamlConfiguration();
		data.set("name", playerData.getName());
		data.set("date", playerData.getEarningData().date);
		data.set("earnings", playerData.getEarningData().earnings);
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
					uuids.add(UUID.fromString(file.getName().substring(0, file.getName().length() - 4)));
				}
			}
		}
		return uuids;
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> getLegacyPlayerData(UUID uuid) {
		// Retrieve legacy player data (YAML format) for a given UUID.
		var builder = PlayerData.builder().setName("");

		File sellFile = new File(HellblockPlugin.getInstance().getDataFolder(), "data/sell/" + uuid + ".yml");
		if (sellFile.exists()) {
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(sellFile);
			builder.setEarningData(new EarningData(yaml.getDouble("earnings"), yaml.getInt("date")));
		} else {
			builder.setEarningData(EarningData.empty());
		}

		return CompletableFuture.completedFuture(Optional.of(builder.build()));
	}
}