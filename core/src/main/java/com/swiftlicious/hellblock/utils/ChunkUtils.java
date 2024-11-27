package com.swiftlicious.hellblock.utils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.NotNull;

import io.papermc.lib.PaperLib;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotResult;

public class ChunkUtils {

	/*
	 * PaperLib methods for addons to call
	 */

	/**
	 * Teleports an Entity to the target location, loading the chunk asynchronously
	 * first if needed.
	 * 
	 * @param entity   The Entity to teleport
	 * @param location The Location to Teleport to
	 * @return Future that completes with the result of the teleport
	 */
	@NotNull
	public static CompletableFuture<Boolean> teleportAsync(@NotNull Entity entity, @NotNull Location location) {
		return PaperLib.teleportAsync(entity, location);
	}

	/**
	 * Teleports an Entity to the target location, loading the chunk asynchronously
	 * first if needed.
	 * 
	 * @param entity   The Entity to teleport
	 * @param location The Location to Teleport to
	 * @param cause    The cause for the teleportation
	 * @return Future that completes with the result of the teleport
	 */
	@NotNull
	public static CompletableFuture<Boolean> teleportAsync(@NotNull Entity entity, @NotNull Location location,
			TeleportCause cause) {
		return PaperLib.teleportAsync(entity, location, cause);
	}

	/**
	 * Gets the chunk at the target location, loading it asynchronously if needed.
	 * 
	 * @param loc Location to get chunk for
	 * @return Future that completes with the chunk
	 */
	@NotNull
	public static CompletableFuture<Chunk> getChunkAtAsync(@NotNull Location loc) {
		return getChunkAtAsync(Objects.requireNonNull(loc.getWorld()), loc.getBlockX() >> 4, loc.getBlockZ() >> 4,
				true);
	}

	/**
	 * Gets the chunk at the target location, loading it asynchronously if needed.
	 * 
	 * @param loc Location to get chunk for
	 * @param gen Should the chunk generate or not. Only respected on some MC
	 *            versions, 1.13 for CB, 1.12 for Paper
	 * @return Future that completes with the chunk, or null if the chunk did not
	 *         exists and generation was not requested.
	 */
	@NotNull
	public static CompletableFuture<Chunk> getChunkAtAsync(@NotNull Location loc, boolean gen) {
		return getChunkAtAsync(Objects.requireNonNull(loc.getWorld()), loc.getBlockX() >> 4, loc.getBlockZ() >> 4, gen);
	}

	/**
	 * Gets the chunk at the target location, loading it asynchronously if needed.
	 * 
	 * @param world World to load chunk for
	 * @param x     X coordinate of the chunk to load
	 * @param z     Z coordinate of the chunk to load
	 * @return Future that completes with the chunk
	 */
	@NotNull
	public static CompletableFuture<Chunk> getChunkAtAsync(@NotNull World world, int x, int z) {
		return getChunkAtAsync(world, x, z, true);
	}

	/**
	 * Gets the chunk at the target location, loading it asynchronously if needed.
	 * 
	 * @param world World to load chunk for
	 * @param x     X coordinate of the chunk to load
	 * @param z     Z coordinate of the chunk to load
	 * @param gen   Should the chunk generate or not. Only respected on some MC
	 *              versions, 1.13 for CB, 1.12 for Paper
	 * @return Future that completes with the chunk, or null if the chunk did not
	 *         exists and generation was not requested.
	 */
	@NotNull
	public static CompletableFuture<Chunk> getChunkAtAsync(@NotNull World world, int x, int z, boolean gen) {
		return PaperLib.getChunkAtAsync(world, x, z, gen);
	}

	/**
	 * Checks if the chunk has been generated or not. Only works on Paper 1.12+ or
	 * any 1.13.1+ version
	 * 
	 * @param loc Location to check if the chunk is generated
	 * @return If the chunk is generated or not
	 */
	public static boolean isChunkGenerated(@NotNull Location loc) {
		return isChunkGenerated(Objects.requireNonNull(loc.getWorld()), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
	}

	/**
	 * Checks if the chunk has been generated or not. Only works on Paper 1.12+ or
	 * any 1.13.1+ version
	 * 
	 * @param world World to check for
	 * @param x     X coordinate of the chunk to check
	 * @param z     Z coordinate of the chunk to checl
	 * @return If the chunk is generated or not
	 */
	public static boolean isChunkGenerated(@NotNull World world, int x, int z) {
		return PaperLib.isChunkGenerated(world, x, z);
	}

	/**
	 * Get's a BlockState, optionally not using a snapshot
	 * 
	 * @param block       The block to get a State of
	 * @param useSnapshot Whether or not to use a snapshot when supported
	 * @return The BlockState
	 */
	@NotNull
	public static BlockStateSnapshotResult getBlockState(@NotNull Block block, boolean useSnapshot) {
		return PaperLib.getBlockState(block, useSnapshot);
	}
}
