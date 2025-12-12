package com.swiftlicious.hellblock.player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utility class for fetching Minecraft player UUIDs based on their usernames.
 * <p>
 * This class queries Mojang's public API to retrieve UUIDs and caches them in
 * memory for faster future lookups.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> This class uses Java 11+ {@link HttpClient} and
 * requires the org.gson library.
 * </p>
 */
public final class UUIDFetcher {

	private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	// Thread-safe in-memory cache to avoid duplicate lookups
	private static final ConcurrentHashMap<String, UUID> CACHE = new ConcurrentHashMap<>();

	private UUIDFetcher() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
	}

	/**
	 * Retrieves the UUID of the given player using their Player object.
	 *
	 * @param player The Minecraft player.
	 * @return An Optional containing the UUID if found, otherwise Optional.empty().
	 */
	public static Optional<UUID> getUUID(Player player) {
		return getUUID(player.getName());
	}

	/**
	 * Retrieves the UUID of a player by their username. Results are cached after
	 * the first successful fetch.
	 *
	 * @param name The username of the player.
	 * @return An Optional containing the UUID if found, otherwise Optional.empty().
	 */
	public static Optional<UUID> getUUID(String name) {
		if (CACHE.containsKey(name)) {
			return Optional.of(CACHE.get(name));
		}

		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(UUID_URL + name)).GET().build();

			HttpResponse<String> response = CLIENT.send(request,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

			if (response.statusCode() == 200) {
				JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
				String rawId = json.get("id").getAsString();
				UUID uuid = UUID.fromString(insertDashes(rawId));
				CACHE.put(name, uuid);
				return Optional.of(uuid);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace(); // Ideally replace with a logging framework
		} catch (Exception e) {
			e.printStackTrace(); // Handle other exceptions like JSON parsing
		}

		return Optional.empty();
	}

	/**
	 * Inserts dashes into a raw UUID string to format it as a standard UUID.
	 *
	 * @param uuid The raw UUID string without dashes.
	 * @return The formatted UUID string with dashes.
	 */
	private static String insertDashes(String uuid) {
		return uuid.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
	}
}