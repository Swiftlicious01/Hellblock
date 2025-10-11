package com.swiftlicious.hellblock.world;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;

/**
 * Interface representing a section of a chunk in the Hellblock plugin
 */
public interface CustomSectionInterface {

	/**
	 * Creates a new instance of a CustomSection with the specified section ID.
	 *
	 * @param sectionID The ID of the section to create.
	 * @return A new {@link CustomSection} instance.
	 */
	static CustomSection create(int sectionID) {
		return new CustomSection(sectionID);
	}

	/**
	 * Restores an existing CustomSection from the provided section ID and block
	 * states.
	 *
	 * @param sectionID The ID of the section to restore.
	 * @param blocks    A map of {@link BlockPos} to {@link CustomBlockState}
	 *                  representing the blocks in the section.
	 * @return A restored {@link CustomSection} instance.
	 */
	static CustomSection restore(int sectionID, ConcurrentMap<BlockPos, CustomBlockState> blocks) {
		return new CustomSection(sectionID, blocks);
	}

	/**
	 * Gets the ID of this section.
	 *
	 * @return The section ID.
	 */
	int getSectionID();

	/**
	 * Retrieves the block state at a specific position within this section.
	 *
	 * @param pos The {@link BlockPos} representing the position of the block.
	 * @return An {@link Optional} containing the {@link CustomBlockState} if
	 *         present, otherwise empty.
	 */
	@NotNull
	Optional<CustomBlockState> getBlockState(BlockPos pos);

	/**
	 * Removes the block state at a specific position within this section.
	 *
	 * @param pos The {@link BlockPos} representing the position of the block to
	 *            remove.
	 * @return An {@link Optional} containing the removed
	 *         {@link CustomBlockState} if present, otherwise empty.
	 */
	@NotNull
	Optional<CustomBlockState> removeBlockState(BlockPos pos);

	/**
	 * Adds or replaces a block state at a specific position within this section.
	 *
	 * @param pos   The {@link BlockPos} representing the position where the block
	 *              will be added.
	 * @param block The {@link CustomBlockState} to add.
	 * @return An {@link Optional} containing the previous
	 *         {@link CustomBlockState} if replaced, otherwise empty.
	 */
	@NotNull
	Optional<CustomBlockState> addBlockState(BlockPos pos, CustomBlockState block);

	/**
	 * Checks if the section can be pruned (removed from memory or storage).
	 *
	 * @return true if the section can be pruned, false otherwise.
	 */
	boolean canPrune();

	/**
	 * Gets an array of all block states within this section.
	 *
	 * @return An array of {@link CustomBlockState}.
	 */
	CustomBlockState[] blocks();

	/**
	 * Gets a map of all block positions to their respective block states within
	 * this section.
	 *
	 * @return A {@link Map} of {@link BlockPos} to {@link CustomBlockState}.
	 */
	Map<BlockPos, CustomBlockState> blockMap();
}