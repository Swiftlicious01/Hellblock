package com.swiftlicious.hellblock.world;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.world.adapter.WorldAdapter;
import com.swiftlicious.hellblock.world.block.HellblockBlock;

/**
 * Interface representing a custom world in the Hellblock plugin
 *
 * @param <W> The type representing the world (e.g., Bukkit World).
 */
public interface HellblockWorldInterface<W> {

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
	 * Creates a new HellblockChunk associated with this world at the specified
	 * position.
	 *
	 * @param pos The position of the chunk.
	 * @return The created {@link HellblockChunk}.
	 */
	default HellblockChunk createChunk(ChunkPos pos) {
		return new HellblockChunk((HellblockWorld<?>) this, pos);
	}

	/**
	 * Restores a HellblockChunk with the specified parameters.
	 *
	 * @param pos            The position of the chunk.
	 * @param loadedSeconds  The number of seconds the chunk has been loaded.
	 * @param lastLoadedTime The last time the chunk was loaded.
	 * @param loadedSections The sections loaded in this chunk.
	 * @param queue          The queue of delayed tick tasks.
	 * @param tickedBlocks   The set of blocks that have been ticked.
	 * @return The restored {@link HellblockChunk}.
	 */
	default HellblockChunk restoreChunk(ChunkPos pos, int loadedSeconds, long lastLoadedTime,
			ConcurrentMap<Integer, HellblockSection> loadedSections, PriorityBlockingQueue<DelayedTickTask> queue,
			Set<BlockPos> tickedBlocks) {
		return new HellblockChunk((HellblockWorld<?>) this, pos, loadedSeconds, lastLoadedTime, loadedSections, queue,
				tickedBlocks);
	}

	/**
	 * Creates a new HellblockRegion associated with this world at the specified
	 * position.
	 *
	 * @param pos The position of the region.
	 * @return The created {@link HellblockRegion}.
	 */
	default HellblockRegion createRegion(RegionPos pos) {
		return new HellblockRegion((HellblockWorld<?>) this, pos);
	}

	/**
	 * Restores a HellblockRegion with the specified cached chunks.
	 *
	 * @param pos          The position of the region.
	 * @param cachedChunks The map of cached chunks within the region.
	 * @return The restored {@link HellblockRegion}.
	 */
	default HellblockRegion restoreRegion(RegionPos pos, ConcurrentMap<ChunkPos, byte[]> cachedChunks) {
		return new HellblockRegion((HellblockWorld<?>) this, pos, cachedChunks);
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
	boolean testChunkLimitation(Pos3 pos3, Class<? extends HellblockBlock> clazz, int amount);

	/**
	 * Checks if a chunk contains any blocks of a specific type.
	 *
	 * @param pos3  The position to check.
	 * @param clazz The class of the block type.
	 * @return true if the chunk contains the block type, false otherwise.
	 */
	boolean doesChunkHaveBlock(Pos3 pos3, Class<? extends HellblockBlock> clazz);

	/**
	 * Gets the number of blocks of a specific type in a chunk.
	 *
	 * @param pos3  The position to check.
	 * @param clazz The class of the block type.
	 * @return The number of blocks of the specified type in the chunk.
	 */
	int getChunkBlockAmount(Pos3 pos3, Class<? extends HellblockBlock> clazz);

	/**
	 * Gets all the loaded chunks in this world.
	 *
	 * @return An array of {@link HellblockChunk} representing the loaded chunks.
	 */
	HellblockChunk[] loadedChunks();

	/**
	 * Gets all the lazy chunks in this world.
	 *
	 * @return An array of {@link HellblockChunk} representing the lazy chunks.
	 */
	HellblockChunk[] lazyChunks();

	/**
	 * Gets all the loaded regions in this world.
	 *
	 * @return An array of {@link HellblockRegion} representing the loaded regions.
	 */
	HellblockRegion[] loadedRegions();

	/**
	 * Gets the block state in a loaded chunk
	 *
	 * @param location location
	 * @return the optional block state
	 */
	@NotNull
	Optional<HellblockBlockState> getLoadedBlockState(Pos3 location);

	/**
	 * Gets the block state at a specific location.
	 *
	 * @param location The location of the block state.
	 * @return An {@link Optional} containing the block state if present, otherwise
	 *         empty.
	 */
	@NotNull
	Optional<HellblockBlockState> getBlockState(Pos3 location);

	/**
	 * Removes the block state at a specific location.
	 *
	 * @param location The location of the block state to remove.
	 * @return An {@link Optional} containing the removed block state if present,
	 *         otherwise empty.
	 */
	@NotNull
	Optional<HellblockBlockState> removeBlockState(Pos3 location);

	/**
	 * Adds a block state at a specific location.
	 *
	 * @param location The location of the block state.
	 * @param block    The block state to add.
	 * @return An {@link Optional} containing the previous block state if replaced,
	 *         otherwise empty.
	 */
	@NotNull
	Optional<HellblockBlockState> addBlockState(Pos3 location, HellblockBlockState block);

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
	Optional<HellblockChunk> getLoadedChunk(ChunkPos chunkPos);

	/**
	 * Gets a chunk from the cache or loads it from file if not cached.
	 *
	 * @param chunkPos The position of the chunk.
	 * @return An {@link Optional} containing the chunk if present, otherwise empty.
	 */
	@NotNull
	Optional<HellblockChunk> getChunk(ChunkPos chunkPos);

	/**
	 * Gets a chunk from the cache or loads it from file, creating a new one if it
	 * does not exist.
	 *
	 * @param chunkPos The position of the chunk.
	 * @return The {@link HellblockChunk}.
	 */
	@NotNull
	HellblockChunk getOrCreateChunk(ChunkPos chunkPos);

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
	Optional<HellblockRegion> getLoadedRegion(RegionPos regionPos);

	/**
	 * Gets a region from the cache or loads it from file if not cached.
	 *
	 * @param regionPos The position of the region.
	 * @return An {@link Optional} containing the region if present, otherwise
	 *         empty.
	 */
	@NotNull
	Optional<HellblockRegion> getRegion(RegionPos regionPos);

	/**
	 * Gets a region from the cache or loads it from file, creating a new one if it
	 * does not exist.
	 *
	 * @param regionPos The position of the region.
	 * @return The {@link HellblockRegion}.
	 */
	@NotNull
	HellblockRegion getOrCreateRegion(RegionPos regionPos);

	/**
	 * Get the scheduler for this world
	 *
	 * @return the scheduler
	 */
	WorldScheduler scheduler();
}