package com.swiftlicious.hellblock.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * The {@code LocationCacheData} class provides a structure to cache
 * location-based data used by the plugin. It stores serialized piston locations
 * per island ID, and block placement counts per block type and world context.
 * <p>
 * This class implements {@link EmptyCheck} to allow checking if the cache is
 * empty.
 */
public class LocationCacheData implements EmptyCheck {

	@Expose
	@SerializedName("cachedPistons")
	// islandId -> List of serialized piston locations
	protected Map<Integer, List<String>> pistonLocationsByIsland;

	@Expose
	@SerializedName("placedBlocks")
	// chunkKey -> (blockData -> count)
	protected Map<String, Map<String, Integer>> placedBlocks;

	/**
	 * Constructs a new {@code LocationCacheData} instance with provided maps.
	 *
	 * @param pistonLocationsByIsland a map from island ID to a list of serialized
	 *                                piston locations
	 * @param placedBlocks            a map from chunk key to block types and their
	 *                                counts
	 */
	public LocationCacheData(@NotNull Map<Integer, List<String>> pistonLocationsByIsland,
			@NotNull Map<String, Map<String, Integer>> placedBlocks) {
		// Deep copy both structures
		this.pistonLocationsByIsland = pistonLocationsByIsland.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
		this.placedBlocks = placedBlocks.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> new HashMap<>(e.getValue())));
	}

	/**
	 * Gets the mapping of island IDs to lists of serialized piston locations.
	 *
	 * @return the map of piston locations by island
	 */
	@NotNull
	public Map<Integer, List<String>> getPistonLocationsByIsland() {
		if (this.pistonLocationsByIsland == null) {
			return new HashMap<>();
		}
		return this.pistonLocationsByIsland;
	}

	/**
	 * Gets the mapping of placed blocks by chunk key and block data.
	 *
	 * @return the map of placed blocks
	 */
	@NotNull
	public Map<String, Map<String, Integer>> getPlacedBlocks() {
		if (this.placedBlocks == null) {
			return new HashMap<>();
		}
		return this.placedBlocks;
	}

	/**
	 * Sets the map of piston locations by island.
	 *
	 * @param pistonLocationsByIsland a map of island IDs to lists of piston
	 *                                locations
	 */
	public void setPistonLocationsByIsland(@NotNull Map<Integer, List<String>> pistonLocationsByIsland) {
		this.pistonLocationsByIsland = pistonLocationsByIsland;
	}

	/**
	 * Sets the map of placed blocks.
	 *
	 * @param placedBlocks a map from chunk key to block data and their counts
	 */
	public void setPlacedBlocks(@NotNull Map<String, Map<String, Integer>> placedBlocks) {
		this.placedBlocks = placedBlocks;
	}

	/**
	 * Clears all cached block data (used for specficially island resets)
	 */
	public void clearBlockData() {
		this.placedBlocks.clear();
		this.pistonLocationsByIsland.clear();
	}

	/**
	 * Creates an instance of {@code LocationCacheData} with default values (empty
	 * maps).
	 *
	 * @return a new instance of {@code LocationCacheData} with no cached data
	 */
	@NotNull
	public static LocationCacheData empty() {
		return new LocationCacheData(new HashMap<>(), new HashMap<>());
	}

	/**
	 * Creates a deep copy of this {@code LocationCacheData}.
	 * <p>
	 * Both outer and inner map/list structures are duplicated to prevent shared
	 * references.
	 *
	 * @return a new {@code LocationCacheData} instance with copied data
	 */
	@NotNull
	public final LocationCacheData copy() {
		// Deep copy of pistonLocationsByIsland (Map<Integer, List<String>>)
		Map<Integer, List<String>> pistonLocationsCopy = new HashMap<>();
		pistonLocationsByIsland.entrySet()
				.forEach(entry -> pistonLocationsCopy.put(entry.getKey(), new ArrayList<>(entry.getValue())));

		// Deep copy of placedBlocks (Map<String, Map<String, Integer>>)
		Map<String, Map<String, Integer>> placedBlocksCopy = new HashMap<>();
		placedBlocks.entrySet().forEach(outerEntry -> {
			Map<String, Integer> innerCopy = new HashMap<>(outerEntry.getValue());
			placedBlocksCopy.put(outerEntry.getKey(), innerCopy);
		});

		return new LocationCacheData(pistonLocationsCopy, placedBlocksCopy);
	}

	/**
	 * Checks whether this cache contains any piston locations or placed block data.
	 *
	 * @return true if both maps are empty, false otherwise
	 */
	@Override
	public boolean isEmpty() {
		return this.pistonLocationsByIsland.isEmpty() && this.placedBlocks.isEmpty();
	}
}