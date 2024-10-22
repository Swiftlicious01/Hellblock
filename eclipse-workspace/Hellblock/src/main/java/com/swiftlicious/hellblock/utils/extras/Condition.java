package com.swiftlicious.hellblock.utils.extras;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a condition with associated data
 */
public class Condition {

	protected Location location;
	protected final Player player;
	protected final @NotNull Map<String, String> args;

	/**
	 * Creates a new Condition object based on a player's location.
	 *
	 * @param player The player associated with this condition.
	 */
	public Condition(@NotNull Player player) {
		this(player.getLocation(), player, new HashMap<>());
	}

	/**
	 * Creates a new Condition object with specified arguments.
	 *
	 * @param player The player associated with this condition.
	 * @param args   A map of arguments associated with this condition.
	 */
	public Condition(@NotNull Player player, @NotNull Map<String, String> args) {
		this(player.getLocation(), player, args);
	}

	/**
	 * Creates a new Condition object with a specific location, player, and
	 * arguments.
	 *
	 * @param location The location associated with this condition.
	 * @param player   The player associated with this condition.
	 * @param args     A map of arguments associated with this condition.
	 */
	public Condition(Location location, Player player, @NotNull Map<String, String> args) {
		this.location = location;
		this.player = player;
		this.args = args;
		if (player != null)
			this.args.put("{player}", player.getName());
		if (location != null) {
			this.args.put("{x}", String.valueOf(location.getX()));
			this.args.put("{y}", String.valueOf(location.getY()));
			this.args.put("{z}", String.valueOf(location.getZ()));
			this.args.put("{world}", location.getWorld().getName());
		}
	}

	/**
	 * Sets the location associated with this condition.
	 *
	 * @param location The new location to set.
	 */
	public void setLocation(@NotNull Location location) {
		this.location = location;
		this.args.put("{x}", String.valueOf(location.getX()));
		this.args.put("{y}", String.valueOf(location.getY()));
		this.args.put("{z}", String.valueOf(location.getZ()));
		this.args.put("{world}", location.getWorld().getName());
	}

	/**
	 * Gets the location associated with this condition.
	 *
	 * @return The location associated with this condition.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Gets the player associated with this condition.
	 *
	 * @return The player associated with this condition.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the map of arguments associated with this condition.
	 *
	 * @return A map of arguments associated with this condition.
	 */
	@NotNull
	public Map<String, String> getArgs() {
		return args;
	}

	/**
	 * Gets the value of a specific argument by its key.
	 *
	 * @param key The key of the argument to retrieve.
	 * @return The value of the argument or null if not found.
	 */
	@Nullable
	public String getArg(String key) {
		return args.get(key);
	}

	/**
	 * Inserts or updates an argument with the specified key and value.
	 *
	 * @param key   The key of the argument to insert or update.
	 * @param value The value to set for the argument.
	 */
	public void insertArg(String key, String value) {
		args.put(key, value);
	}

	/**
	 * Deletes an argument with the specified key.
	 *
	 * @param key The key of the argument to delete.
	 * @return The value of the deleted argument or null if not found.
	 */
	public String delArg(String key) {
		return args.remove(key);
	}
}