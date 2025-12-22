package com.swiftlicious.hellblock.protection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

/**
 * Interface defining utility methods for managing Hellblock island protection,
 * including chunk and block access, flag updates, entity clearing, and spatial
 * checks.
 */
public interface ProtectionManagerInterface {

	/**
	 * Updates a specific protection flag for the given island.
	 *
	 * @param world         the world where the island resides
	 * @param islandOwnerId the UUID of the island owner
	 * @param flag          the {@link HellblockFlag} to change
	 */
	CompletableFuture<Boolean> changeProtectionFlag(@NotNull HellblockWorld<?> world, @NotNull UUID islandOwnerId,
			@NotNull HellblockFlag flag);

	/**
	 * Toggles the lock status of the specified island.
	 *
	 * @param world         the world where the island resides
	 * @param islandOwnerId the UUID of the island owner
	 */
	CompletableFuture<Boolean> changeLockStatus(@NotNull HellblockWorld<?> world, @NotNull UUID islandOwnerId);

	/**
	 * Restores the island's protection and region state, typically after a reset or
	 * major change.
	 *
	 * @param islandOwnerData the {@link HellblockData} of the island to restore
	 */
	CompletableFuture<Boolean> restoreIsland(@NotNull HellblockData islandOwnerData);

	/**
	 * Removes all entities from the specified bounding box on the island. Typically
	 * used during resets or protection enforcement.
	 *
	 * @param world        the world where the island is located
	 * @param islandBounds the {@link BoundingBox} defining the island area
	 */
	void clearHellblockEntities(@NotNull World world, @NotNull BoundingBox islandBounds);

	/**
	 * Gets all chunks occupied by the island with the given numeric ID.
	 *
	 * @param world    the world containing the island
	 * @param islandId the island's numeric ID
	 * @return a {@link CompletableFuture} containing the set of {@link ChunkPos}s
	 */
	CompletableFuture<Set<ChunkPos>> getHellblockChunks(@NotNull HellblockWorld<?> world, int islandId);

	/**
	 * Gets all chunks occupied by the island owned by the given UUID.
	 *
	 * @param world         the world containing the island
	 * @param islandOwnerId the UUID of the island owner
	 * @return a {@link CompletableFuture} containing the set of {@link ChunkPos}s
	 */
	CompletableFuture<Set<ChunkPos>> getHellblockChunks(@NotNull HellblockWorld<?> world, @NotNull UUID islandOwnerId);

	/**
	 * Gets all blocks inside the island region owned by the specified player.
	 * 
	 * @param world         the world containing the island
	 * @param islandOwnerId the UUID of the island owner
	 * @return a {@link CompletableFuture} containing a set of {@link Pos3}s within
	 *         the island
	 */
	CompletableFuture<Set<Pos3>> getHellblockBlocks(@NotNull HellblockWorld<?> world, @NotNull UUID islandOwnerId);

	/**
	 * Gets the bounding box for the island owned by the specified player.
	 *
	 * @param world         the world containing the island
	 * @param islandOwnerId the UUID of the island owner
	 * @return a {@link CompletableFuture} containing the island's
	 *         {@link BoundingBox}, or {@code null} if not found
	 */
	CompletableFuture<@Nullable BoundingBox> getHellblockBounds(@NotNull HellblockWorld<?> world,
			@NotNull UUID islandOwnerId);

	/**
	 * Checks if a location is within the 3D bounds of the specified island.
	 *
	 * @param islandOwnerId the UUID of the island owner
	 * @param location      the location to test
	 * @return a {@link CompletableFuture} resolving to {@code true} if inside
	 *         bounds
	 */
	CompletableFuture<Boolean> isInsideIsland(@NotNull UUID islandOwnerId, @NotNull Location location);

	/**
	 * Checks if a location is within the 2D (X/Z plane) bounds of the specified
	 * island.
	 *
	 * @param islandOwnerId the UUID of the island owner
	 * @param location      the location to test
	 * @return a {@link CompletableFuture} resolving to {@code true} if inside 2D
	 *         bounds
	 */
	CompletableFuture<Boolean> isInsideIsland2D(@NotNull UUID islandOwnerId, @NotNull Location location);
}