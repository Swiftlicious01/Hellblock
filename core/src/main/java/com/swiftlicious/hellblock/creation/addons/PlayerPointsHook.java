package com.swiftlicious.hellblock.creation.addons;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;

import com.swiftlicious.hellblock.HellblockPlugin;

public class PlayerPointsHook {

	private static PlayerPointsAPI ppAPI;
	private static boolean isHooked = false;

	public static void register() {
		if (HellblockPlugin.getInstance().isHookedPluginEnabled("PlayerPoints")) {
			ppAPI = PlayerPoints.getInstance().getAPI();
		}

		if (ppAPI != null) {
			PlayerPointsHook.isHooked = true;
		}
	}

	public static boolean isHooked() {
		return isHooked;
	}

	public static boolean hasPoints(OfflinePlayer player, int points) {
		return ppAPI.look(player.getUniqueId()) >= points;
	}

	public static boolean givePoints(OfflinePlayer player, int points) {
		return ppAPI.give(player.getUniqueId(), points);
	}

	public static boolean takePoints(OfflinePlayer player, int points) {
		return ppAPI.take(player.getUniqueId(), points);
	}
}