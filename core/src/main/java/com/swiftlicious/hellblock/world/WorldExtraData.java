package com.swiftlicious.hellblock.world;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * A class representing additional data associated with a world, such as the any
 * extra arbitrary data stored as key-value pairs. This class provides methods
 * for adding, removing, and retrieving extra data.
 */
public class WorldExtraData {

	@SerializedName("extra")
	private final Map<String, Object> extra;

	/**
	 * Constructs a new WorldExtraData instance with empty extra data. Initializes
	 * an empty HashMap for extra data.
	 */
	public WorldExtraData() {
		this.extra = new HashMap<>();
	}

	/**
	 * Creates an empty WorldExtraData instance with default values.
	 *
	 * @return A new WorldExtraData instance with nothing in it.
	 */
	public static WorldExtraData empty() {
		return new WorldExtraData();
	}

	/**
	 * Adds extra data to the world data storage.
	 *
	 * @param key   The key under which the data will be stored.
	 * @param value The value to store.
	 */
	public void addExtraData(String key, Object value) {
		this.extra.put(key, value);
	}

	/**
	 * Removes extra data from the world data storage.
	 *
	 * @param key The key of the data to remove.
	 */
	public void removeExtraData(String key) {
		this.extra.remove(key);
	}

	/**
	 * Retrieves extra data from the world data storage.
	 *
	 * @param key The key of the data to retrieve.
	 * @return The data associated with the key, or null if the key does not exist.
	 */
	@Nullable
	public Object getExtraData(String key) {
		return this.extra.get(key);
	}

	/**
	 * Returns a string representation of the WorldExtraData.
	 *
	 * @return A string containing the extra information.
	 */
	@Override
	public String toString() {
		return "WorldExtraData{" + "extra=" + extra + '}';
	}
}