package com.swiftlicious.hellblock.world.adapter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.CustomChunk;
import com.swiftlicious.hellblock.world.CustomRegion;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.RegionPos;
import com.swiftlicious.hellblock.world.WorldExtraData;

/**
 * Interface defining methods for adapting different types of worlds (e.g.,
 * Bukkit, Slime) for use with Hellblock. This adapter provides methods to load
 * and save regions and chunks, handle world-specific data, and interact with
 * various world implementations.
 *
 * @param <W> The type of the world that this adapter supports.
 */
public interface WorldAdapter<W> extends Comparable<WorldAdapter<W>> {

	int BUKKIT_WORLD_PRIORITY = 100;
	int SLIME_WORLD_PRIORITY = 200;

	/**
	 * Creates a Hellblock world based on the specified Bukkit world.
	 * 
	 * @param world The world name to create as a Hellblock world.
	 * @return the created HellblockWorld instance.
	 */
	CompletableFuture<HellblockWorld<W>> createWorld(String world);

	/**
	 * Retrieves or loads a Hellblock world based on the specified world name.
	 * 
	 * @param worldName The name of the island whose world is to be retrieved or
	 *                  loaded.
	 * @return a CompletableFuture that will complete with the HellblockWorld
	 *         instance.
	 */
	CompletableFuture<HellblockWorld<W>> getOrLoadIslandWorld(String worldName);

	/**
	 * Retrieves or loads a Hellblock world based on the specified island ID.
	 * 
	 * @param islandId The ID of the island whose world is to be retrieved or
	 *                 loaded.
	 * @return a CompletableFuture that will complete with the HellblockWorld
	 *         instance.
	 */
	CompletableFuture<HellblockWorld<W>> getOrLoadIslandWorld(int islandId);

	/**
	 * Deletes a Hellblock world based on the specified Bukkit world.
	 * 
	 * @param world The world name to delete as a Hellblock world.
	 */
	void deleteWorld(String world);

	/**
	 * Loads extra data associated with the given world.
	 *
	 * @param world The world to load data for.
	 * @return The loaded {@link WorldExtraData} containing extra world-specific
	 *         information.
	 */
	WorldExtraData loadExtraData(W world);

	/**
	 * Performs version-based migration for a world that was saved using an outdated
	 * format.
	 *
	 * <p>
	 * This method is called automatically during world loading when the stored
	 * world version is older than the plugin's {@code CURRENT_WORLD_VERSION}. It
	 * applies any necessary transformations to bring the world data up to date,
	 * including updates to settings, metadata, or format-specific patches.
	 * </p>
	 *
	 * <p>
	 * Implementers should ensure that the stored version number is updated after a
	 * successful migration to avoid repeated migration attempts on future loads.
	 * </p>
	 *
	 * @param world      the world being migrated (Bukkit or Slime, depending on
	 *                   context)
	 * @param oldVersion the saved version of the world before migration
	 */
	void migrateWorld(W world, int oldVersion);

	/**
	 * Saves extra data for the given Hellblock world instance.
	 *
	 * @param world The Hellblock world instance whose extra data is to be saved.
	 */
	void saveExtraData(HellblockWorld<W> world);

	/**
	 * Loads a region from the file or cache. Creates a new region if it doesn't
	 * exist and createIfNotExist is true.
	 *
	 * @param world            The Hellblock world instance to which the region
	 *                         belongs.
	 * @param pos              The position of the region to be loaded.
	 * @param createIfNotExist If true, creates the region if it does not exist.
	 * @return The loaded {@link CustomRegion}, or null if the region could not be
	 *         loaded and createIfNotExist is false.
	 */
	@Nullable
	CustomRegion loadRegion(HellblockWorld<W> world, RegionPos pos, boolean createIfNotExist);

	/**
	 * Loads a chunk from the file or cache. Creates a new chunk if it doesn't exist
	 * and createIfNotExist is true.
	 *
	 * @param world            The Hellblock world instance to which the chunk
	 *                         belongs.
	 * @param pos              The position of the chunk to be loaded.
	 * @param createIfNotExist If true, creates the chunk if it does not exist.
	 * @return The loaded {@link CustomChunk}, or null if the chunk could not be
	 *         loaded and createIfNotExist is false.
	 */
	@Nullable
	CustomChunk loadChunk(HellblockWorld<W> world, ChunkPos pos, boolean createIfNotExist);

	/**
	 * Saves the specified region to a file or cache.
	 *
	 * @param world  The Hellblock world instance to which the region belongs.
	 * @param region The region to be saved.
	 */
	void saveRegion(HellblockWorld<W> world, CustomRegion region);

	/**
	 * Saves the specified chunk to a file or cache.
	 *
	 * @param world The Hellblock world instance to which the chunk belongs.
	 * @param chunk The chunk to be saved.
	 */
	void saveChunk(HellblockWorld<W> world, CustomChunk chunk);

	/**
	 * Retrieves the name of the given world.
	 *
	 * @param world The world instance.
	 * @return The name of the world.
	 */
	String getName(W world);

	/**
	 * Gets the world instance by its name.
	 *
	 * @param worldName The name of the world to retrieve.
	 * @return The world instance, or null if no world with the given name is found.
	 */
	@Nullable
	Optional<W> getWorld(String worldName);

	/**
	 * Gets the loaded HellblockWorld instance by its name.
	 *
	 * @param worldName The name of the Hellblock world to retrieve.
	 * @return The loaded {@link HellblockWorld} instance, or null if no world with
	 *         the given name is found.
	 */
	@Nullable
	HellblockWorld<W> getLoadedHellblockWorld(String worldName);

	/**
	 * Adapts the given object to a HellblockWorld instance if possible.
	 *
	 * @param world The object to adapt.
	 * @return The adapted {@link HellblockWorld} instance.
	 */
	HellblockWorld<W> adapt(Object world);

	/**
	 * Gets the priority of this world adapter. Adapters with lower priority values
	 * are considered before those with higher values.
	 *
	 * @return The priority value of this adapter.
	 */
	int priority();
}