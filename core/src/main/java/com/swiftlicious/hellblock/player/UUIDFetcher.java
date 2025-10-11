package com.swiftlicious.hellblock.player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

/**
 * Helper-class for getting UUIDs of players.
 */
public final class UUIDFetcher {

	private static final String UUID_URL = "https://api.mojang.com/users" + "/profiles/minecraft/";

	private static final Pattern UUID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"(.*?)\"");

	private UUIDFetcher() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the UUID of the searched player.
	 *
	 * @param player The player.
	 * @return The UUID of the given player.
	 */
	public static UUID getUUID(Player player) {
		return getUUID(player.getName());
	}

	/**
	 * Returns the UUID of the searched player.
	 *
	 * @param name The name of the player.
	 * @return The UUID of the given player.
	 */
	public static UUID getUUID(String name) {
		final String output = callURL(UUID_URL + name);
		final Matcher m = UUID_PATTERN.matcher(output);
		if (m.find()) {
			return UUID.fromString(insertDashes(m.group(1)));
		}
		return null;
	}

	/**
	 * Helper method for inserting dashes into unformatted UUID.
	 *
	 * @return Formatted UUID with dashes.
	 */
	public static String insertDashes(String uuid) {
		final StringBuilder sb = new StringBuilder(uuid);
		sb.insert(8, '-');
		sb.insert(13, '-');
		sb.insert(18, '-');
		sb.insert(23, '-');
		return sb.toString();
	}

	private static String callURL(String urlStr) {
		final StringBuilder sb = new StringBuilder();
		final URLConnection conn;
		BufferedReader br = null;
		InputStreamReader in = null;
		try {
			conn = new URI(urlStr).toURL().openConnection();
			if (conn != null) {
				conn.setReadTimeout(60 * 1000);
			}
			if (conn != null && conn.getInputStream() != null) {
				in = new InputStreamReader(conn.getInputStream(), "UTF-8");
				br = new BufferedReader(in);
				String line = br.readLine();
				while (line != null) {
					sb.append(line).append("\n");
					line = br.readLine();
				}
			}
		} catch (Throwable ignored) {
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Throwable ignored) {
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Throwable ignored) {
				}
			}
		}
		return sb.toString();
	}
}