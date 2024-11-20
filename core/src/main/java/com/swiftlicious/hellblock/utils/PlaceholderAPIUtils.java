package com.swiftlicious.hellblock.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Utility class for interacting with the PlaceholderAPI. Provides methods to
 * parse placeholders in strings for both online and offline players.
 */
public class PlaceholderAPIUtils {

	/**
	 * Parses placeholders in the provided text for an online player.
	 *
	 * @param player The online player for whom the placeholders should be parsed.
	 * @param text   The text containing placeholders to be parsed.
	 * @return The text with parsed placeholders.
	 */
	public static String parse(Player player, String text) {
		return PlaceholderAPI.setPlaceholders(player, text);
	}

	/**
	 * Parses placeholders in the provided text for an offline player.
	 *
	 * @param player The offline player for whom the placeholders should be parsed.
	 * @param text   The text containing placeholders to be parsed.
	 * @return The text with parsed placeholders.
	 */
	public static String parse(OfflinePlayer player, String text) {
		return PlaceholderAPI.setPlaceholders(player, text);
	}
}