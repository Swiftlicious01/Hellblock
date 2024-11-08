package com.swiftlicious.hellblock.player;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * Implementation of the OfflineUser interface for representing offline player
 * data.
 */
public class OfflineUser implements OfflineUserInterface {

	private final UUID uuid;
	private final String name;
	private final EarningData earningData;
	private final HellblockData hellblockData;
	private final List<String> pistonLocations, levelBlockLocations;
	private boolean unsafeLocation;
	public static OfflineUser LOCKED_USER = new OfflineUser(UUID.randomUUID(), "-locked-", PlayerData.empty());

	/**
	 * Constructor to create an OfflineUser instance.
	 *
	 * @param uuid       The UUID of the player.
	 * @param name       The name of the player.
	 * @param playerData The player's data, including bag contents, earnings, and
	 *                   statistics.
	 */
	public OfflineUser(UUID uuid, String name, PlayerData playerData) {
		this.name = name;
		this.uuid = uuid;
		this.earningData = playerData.getEarningData();
		this.hellblockData = playerData.getHellblockData();
		this.pistonLocations = playerData.getPistonLocations();
		this.levelBlockLocations = playerData.getLevelBlockLocations();
		int date = HellblockPlugin.getInstance().getMarketManager().getDate();
		if (earningData.date != date) {
			earningData.date = date;
			earningData.earnings = 0d;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public EarningData getEarningData() {
		return earningData;
	}

	@Override
	public HellblockData getHellblockData() {
		return hellblockData;
	}

	@Override
	public List<String> getPistonLocations() {
		return pistonLocations;
	}

	@Override
	public List<String> getLevelBlockLocations() {
		return levelBlockLocations;
	}

	@Override
	public boolean isOnline() {
		Player player = Bukkit.getPlayer(uuid);
		return player != null && player.isOnline();
	}

	public boolean inUnsafeLocation() {
		return unsafeLocation;
	}

	public void setInUnsafeLocation(boolean unsafe) {
		unsafeLocation = unsafe;
	}

	@Override
	public PlayerData getPlayerData() {
		// Create a new PlayerData instance based on the stored information
		return PlayerData.builder().setName(name).setLevelBlockLocations(levelBlockLocations)
				.setPistonLocations(pistonLocations).setEarningData(earningData).setHellblockData(hellblockData)
				.build();
	}
}
