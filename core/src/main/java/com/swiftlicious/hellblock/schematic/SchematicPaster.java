package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the contract for schematic pasters responsible for loading and
 * pasting island schematics into a world.
 *
 * <p>
 * Implementations may use different backends (e.g., FAWE, WorldEdit, internal
 * async) to perform the paste operation, and may also handle animation, block
 * filtering, and player-specific tracking.
 *
 * <p>
 * Each paster must support asynchronous pasting and allow progress tracking and
 * cancellation.
 *
 * @see SchematicMetadata
 * @see FastAsyncWorldEditHook
 * @see WorldEditHook
 * @see SchematicAsync
 */
public interface SchematicPaster {

	/**
	 * Pastes a schematic into the world at the specified location asynchronously.
	 *
	 * @param playerId       the UUID of the player requesting the paste; used for
	 *                       progress and cancellation tracking
	 * @param file           the schematic file to paste
	 * @param location       the world location where the schematic should be placed
	 * @param ignoreAirBlock if true, air blocks in the schematic will not overwrite
	 *                       existing blocks
	 * @param metadata       the metadata defining special points (e.g., home,
	 *                       container, biome) to apply during paste
	 * @param animated       if true, the schematic is pasted with an animation
	 *                       effect (if supported)
	 * @return a {@link CompletableFuture} that completes with the computed spawn
	 *         {@link Location} once paste is finished
	 */
	CompletableFuture<@NotNull Location> pasteHellblock(@NotNull UUID playerId, @NotNull File file,
			@NotNull Location location, boolean ignoreAirBlock, @NotNull SchematicMetadata metadata, boolean animated);

	/**
	 * Attempts to cancel an ongoing paste operation for the given player.
	 *
	 * @param playerId the UUID of the player whose paste task should be cancelled
	 * @return {@code true} if a paste was cancelled; {@code false} if no active
	 *         paste was found
	 */
	boolean cancelPaste(@NotNull UUID playerId);

	/**
	 * Gets the paste progress for the specified player.
	 *
	 * @param playerId the UUID of the player
	 * @return an integer representing paste progress (implementation-defined; e.g.,
	 *         percentage or block count)
	 */
	int getPasteProgress(@NotNull UUID playerId);

	/**
	 * Clears any internal caches maintained by the paster (e.g., parsed schematic
	 * structures).
	 *
	 * <p>
	 * Useful for memory management or force-reloading schematics from disk.
	 */
	void clearCache();
}
