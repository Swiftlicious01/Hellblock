package com.swiftlicious.hellblock.world;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.world.adapter.WorldAdapter;

/**
 * Interface representing a custom world in the Hellblock plugin
 *
 * @param <W> The type representing the world (e.g., Bukkit World).
 */
public interface CustomWorldInterface<W> {

	/**
	 * Creates a new Hellblock world with the specified world instance and adaptor.
	 *
	 * @param world   The world instance.
	 * @param adapter The world adapter to use for this world.
	 * @param <W>     The type of the world.
	 * @return A new instance of {@link HellblockWorld}.
	 */
	static <W> HellblockWorld<W> create(W world, WorldAdapter<W> adapter) {
		return new HellblockWorld<>(world, adapter);
	}

	/**
	 * Creates a new CustomChunk associated with this world at the specified
	 * position.
	 *
	 * @param pos The position of the chunk.
	 * @return The created {@link CustomChunk}.
	 */
	default CustomChunk createChunk(ChunkPos pos) {
		return new CustomChunk((HellblockWorld<?>) this, pos);
	}

	/**
	 * Restores a CustomChunk with the specified parameters.
	 *
	 * @param pos            The position of the chunk.
	 * @param loadedSeconds  The number of seconds the chunk has been loaded.
	 * @param lastLoadedTime The last time the chunk was loaded.
	 * @param loadedSections The sections loaded in this chunk.
	 * @param queue          The queue of delayed tick tasks.
	 * @param tickedBlocks   The set of blocks that have been ticked.
	 * @return The restored {@link CustomChunk}.
	 */
	default CustomChunk restoreChunk(ChunkPos pos, int loadedSeconds, long lastLoadedTime,
			ConcurrentMap<Integer, CustomSection> loadedSections, PriorityBlockingQueue<DelayedTickTask> queue,
			Set<BlockPos> tickedBlocks) {
		return new CustomChunk((HellblockWorld<?>) this, pos, loadedSeconds, lastLoadedTime, loadedSections, queue,
				tickedBlocks);
	}

	/**
	 * Creates a new CustomRegion associated with this world at the specified
	 * position.
	 *
	 * @param pos The position of the region.
	 * @return The created {@link CustomRegion}.
	 */
	default CustomRegion createRegion(RegionPos pos) {
		return new CustomRegion((HellblockWorld<?>) this, pos);
	}

	/**
	 * Restores a CustomRegion with the specified cached chunks.
	 *
	 * @param pos          The position of the region.
	 * @param cachedChunks The map of cached chunks within the region.
	 * @return The restored {@link CustomRegion}.
	 */
	default CustomRegion restoreRegion(RegionPos pos, ConcurrentMap<ChunkPos, byte[]> cachedChunks) {
		return new CustomRegion((HellblockWorld<?>) this, pos, cachedChunks);
	}

	/**
	 * Gets the world adapter associated with this world.
	 *
	 * @return The {@link WorldAdapter} for this world.
	 */
	WorldAdapter<W> adapter();

	/**
	 * Gets the extra data associated with this world.
	 *
	 * @return The {@link WorldExtraData} instance.
	 */
	WorldExtraData extraData();

	/**
	 * Tests if adding a specified amount of blocks of a certain type would exceed
	 * the chunk limitation for that block type.
	 *
	 * @param pos3   The position to test.
	 * @param clazz  The class of the block type.
	 * @param amount The number of blocks to add.
	 * @return true if it would exceed the limit, false otherwise.
	 */
	boolean testChunkLimitation(Pos3 pos3, Class<? extends CustomBlock> clazz, int amount);

	/**
	 * Checks if a chunk contains any blocks of a specific type.
	 *
	 * @param pos3  The position to check.
	 * @param clazz The class of the block type.
	 * @return true if the chunk contains the block type, false otherwise.
	 */
	boolean doesChunkHaveBlock(Pos3 pos3, Class<? extends CustomBlock> clazz);

	/**
	 * Gets the number of blocks of a specific type in a chunk.
	 *
	 * @param pos3  The position to check.
	 * @param clazz The class of the block type.
	 * @return The number of blocks of the specified type in the chunk.
	 */
	int getChunkBlockAmount(Pos3 pos3, Class<? extends CustomBlock> clazz);

	/**
	 * Gets all the loaded chunks in this world.
	 *
	 * @return An array of {@link CustomChunk} representing the loaded chunks.
	 */
	CustomChunk[] loadedChunks();

	/**
	 * Gets all the lazy chunks in this world.
	 *
	 * @return An array of {@link CustomChunk} representing the lazy chunks.
	 */
	CustomChunk[] lazyChunks();

	/**
	 * Gets all the loaded regions in this world.
	 *
	 * @return An array of {@link CustomRegion} representing the loaded regions.
	 */
	CustomRegion[] loadedRegions();

	/**
	 * Gets the block state in a loaded chunk
	 *
	 * @param location location
	 * @return the optional block state
	 */
	@NotNull
	Optional<CustomBlockState> getLoadedBlockState(Pos3 location);

	/**
	 * Gets the block state at a specific location.
	 *
	 * @param location The location of the block state.
	 * @return An {@link Optional} containing the block state if present, otherwise
	 *         empty.
	 */
	@NotNull
	CompletableFuture<Optional<CustomBlockState>> getBlockState(Pos3 location);

	/**
	 * Removes the block state at a specific location.
	 *
	 * @param location The location of the block state to remove.
	 * @return An {@link Optional} containing the removed block state if present,
	 *         otherwise empty.
	 */
	@NotNull
	CompletableFuture<Optional<CustomBlockState>> removeBlockState(Pos3 location);

	/**
	 * Adds a block state at a specific location.
	 *
	 * @param location The location of the block state.
	 * @param block    The block state to add.
	 * @return An {@link Optional} containing the previous block state if replaced,
	 *         otherwise empty.
	 */
	@NotNull
	CompletableFuture<Optional<CustomBlockState>> addBlockState(Pos3 location, CustomBlockState block);

	/**
	 * Updates the internal custom block state at the specified position and
	 * schedules a synchronous visual update in the Bukkit world.
	 *
	 * <p>
	 * This method is intended to be called from asynchronous contexts, such as crop
	 * growth or farmland hydration, where logical world state can be updated safely
	 * without blocking the main thread.
	 * </p>
	 *
	 * <p>
	 * The actual visual rendering (e.g., updating the Bukkit {@link Block}) is
	 * deferred to the main server thread using a scheduled task.
	 * </p>
	 *
	 * @param location the position of the block to update
	 * @param block    the new custom block state to apply at that position
	 */
	CompletableFuture<Void> updateBlockState(Pos3 location, CustomBlockState block);

	/**
	 * Updates the block state at the specified position if the provided
	 * {@link Optional} contains a non-null {@link CustomBlockState}.
	 *
	 * <p>
	 * This is a convenience method that delegates to
	 * {@link #updateBlockState(Pos3, CustomBlockState)} only if a valid block state
	 * is present. It is commonly used when retrieving block states using
	 * {@code world.getBlockState(...)} and wanting to safely apply updates only if
	 * the block exists.
	 * </p>
	 *
	 * @param location the position of the block to update
	 * @param blockOpt an optional containing the block state to update, or empty if
	 *                 none
	 */
	default void updateBlockStateIfPresent(Pos3 location, Optional<CustomBlockState> blockOpt) {
		blockOpt.ifPresent(block -> updateBlockState(location, block));
	}

	/**
	 * Performs a bulk update of multiple custom block states in this world and
	 * schedules a single synchronous batch render on the main server thread.
	 *
	 * <p>
	 * This method is intended for use when many block states must be updated at
	 * once (for example: island-wide crop ticks, mass hydration, or a chunk/region
	 * operation). It applies the logical updates to the world data structures
	 * immediately (async-safe) by storing each state in its chunk, and then queues
	 * a single synchronous task that iterates the same updates and calls
	 * {@link CustomBlockRenderer#render(CustomBlockState, Pos3, World)} for each
	 * entry to update the visible Bukkit world. Doing so reduces the number of
	 * scheduled sync task invocations compared to updating each block individually.
	 * </p>
	 *
	 * <p>
	 * <strong>Behavioral notes:</strong>
	 * </p>
	 * <ul>
	 * <li>If {@code updates} is {@code null} or empty, this method returns
	 * immediately.</li>
	 * <li>Logical updates are applied before the synchronous render task is
	 * scheduled. The render task will reflect the logical state at the time of
	 * scheduling, so callers should avoid mutating the provided {@code Map} after
	 * calling this method.</li>
	 * <li>Rendering is performed on the main thread via the plugin scheduler; the
	 * method itself is safe to call from asynchronous contexts.</li>
	 * <li>Any {@code null} keys or values in the map are ignored.</li>
	 * </ul>
	 *
	 * @param updates a map of positions to {@link CustomBlockState} instances
	 *                representing the desired state for each position. Keys or
	 *                values that are {@code null} will be skipped. The method does
	 *                not mutate this map.
	 */
	CompletableFuture<Void> updateBlockStates(Map<Pos3, CustomBlockState> updates);

	/**
	 * Saves the world data to a file.
	 *
	 * @param async     async or not
	 * @param disabling is the server disabled
	 */
	void save(boolean async, boolean disabling);

	/**
	 * Sets whether the ticking task is ongoing.
	 *
	 * @param tick true if ticking is ongoing, false otherwise.
	 */
	void setTicking(boolean tick);

	/**
	 * Gets the underlying world instance associated with this Hellblock world.
	 *
	 * @return The world instance of type W.
	 */
	W world();

	/**
	 * Gets the Bukkit World instance associated with this Hellblock world.
	 *
	 * @return The Bukkit {@link World} instance.
	 */
	World bukkitWorld();

	/**
	 * Gets the name of the world.
	 *
	 * @return The world name.
	 */
	String worldName();

	/**
	 * Gets the settings associated with this world.
	 *
	 * @return The {@link WorldSetting} instance.
	 */
	@NotNull
	WorldSetting setting();

	/**
	 * Sets the settings for this world.
	 *
	 * @param setting The {@link WorldSetting} to apply.
	 */
	void setting(WorldSetting setting);

	/**
	 * Checks if a chunk is loaded in this world.
	 *
	 * @param pos The position of the chunk.
	 * @return true if the chunk is loaded, false otherwise.
	 */
	boolean isChunkLoaded(ChunkPos pos);

	/**
	 * Gets a loaded chunk from the cache, if available.
	 *
	 * @param chunkPos The position of the chunk.
	 * @return An {@link Optional} containing the loaded chunk if present, otherwise
	 *         empty.
	 */
	@NotNull
	Optional<CustomChunk> getLoadedChunk(ChunkPos chunkPos);

	/**
	 * Gets a chunk from the cache or loads it from file if not cached.
	 *
	 * @param chunkPos The position of the chunk.
	 * @return An {@link Optional} containing the chunk if present, otherwise empty.
	 */
	@NotNull
	Optional<CustomChunk> getChunk(ChunkPos chunkPos);

	/**
	 * Gets a chunk from the cache or loads it from file, creating a new one if it
	 * does not exist.
	 *
	 * @param chunkPos The position of the chunk.
	 * @return The {@link CustomChunk}.
	 */
	@NotNull
	CompletableFuture<CustomChunk> getOrCreateChunk(ChunkPos chunkPos);

	/**
	 * Checks if a region is loaded in this world.
	 *
	 * @param regionPos The position of the region.
	 * @return true if the region is loaded, false otherwise.
	 */
	boolean isRegionLoaded(RegionPos regionPos);

	/**
	 * Gets a loaded region from the cache, if available.
	 *
	 * @param regionPos The position of the region.
	 * @return An {@link Optional} containing the loaded region if present,
	 *         otherwise empty.
	 */
	@NotNull
	Optional<CustomRegion> getLoadedRegion(RegionPos regionPos);

	/**
	 * Gets a region from the cache or loads it from file if not cached.
	 *
	 * @param regionPos The position of the region.
	 * @return An {@link Optional} containing the region if present, otherwise
	 *         empty.
	 */
	@NotNull
	Optional<CustomRegion> getRegion(RegionPos regionPos);

	/**
	 * Gets a region from the cache or loads it from file, creating a new one if it
	 * does not exist.
	 *
	 * @param regionPos The position of the region.
	 * @return The {@link CustomRegion}.
	 */
	@NotNull
	CustomRegion getOrCreateRegion(RegionPos regionPos);

	/**
	 * Get the scheduler for this world
	 *
	 * @return the scheduler
	 */
	WorldScheduler scheduler();
}