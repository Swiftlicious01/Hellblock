package com.swiftlicious.hellblock.world;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Interface representing a region in the Hellblock plugin
 */
public interface HellblockRegionInterface {

	/**
	 * Checks if the region is currently loaded.
	 *
	 * @return true if the region is loaded, false otherwise.
	 */
	boolean isLoaded();

	/**
	 * Unloads the region, freeing up resources.
	 */
	void unload();

	/**
	 * Loads the region into memory, preparing it for operations.
	 */
	void load();

	/**
	 * Gets the world associated with this region.
	 *
	 * @return The {@link HellblockWorld} instance representing the world.
	 */
	@NotNull
	HellblockWorld<?> getWorld();

	/**
	 * Retrieves the cached data of a chunk within this region by its position.
	 *
	 * @param pos The {@link ChunkPos} representing the position of the chunk.
	 * @return A byte array representing the cached data of the chunk, or null if no
	 *         data is cached.
	 */
	byte[] getCachedChunkBytes(ChunkPos pos);

	/**
	 * Gets the position of this region.
	 *
	 * @return The {@link RegionPos} representing the region's position.
	 */
	@NotNull
	RegionPos regionPos();

	/**
	 * Removes the cached data of a chunk from this region by its position.
	 *
	 * @param pos The {@link ChunkPos} representing the position of the chunk.
	 * @return true if the cached data was removed successfully, false otherwise.
	 */
	boolean removeCachedChunk(ChunkPos pos);

	/**
	 * Caches the data of a chunk within this region at the specified position.
	 *
	 * @param pos  The {@link ChunkPos} representing the position of the chunk.
	 * @param data A byte array representing the data to cache for the chunk.
	 */
	void setCachedChunk(ChunkPos pos, byte[] data);

	/**
	 * Retrieves a map of all chunks and their data that need to be saved.
	 *
	 * @return A {@link Map} where the key is {@link ChunkPos} and the value is a
	 *         byte array of the chunk data.
	 */
	Map<ChunkPos, byte[]> dataToSave();

	/**
	 * Checks if the region can be pruned (removed from memory or storage).
	 *
	 * @return true if the region can be pruned, false otherwise.
	 */
	boolean canPrune();
}