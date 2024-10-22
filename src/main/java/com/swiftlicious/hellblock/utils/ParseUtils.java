package com.swiftlicious.hellblock.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

public class ParseUtils {

	public String setPlaceholders(Player player, String text) {
		return PlaceholderAPI.setPlaceholders(player, text);
	}

	public String setPlaceholders(OfflinePlayer player, String text) {
		return PlaceholderAPI.setPlaceholders(player, text);
	}
}