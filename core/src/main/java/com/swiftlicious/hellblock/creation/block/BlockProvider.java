package com.swiftlicious.hellblock.creation.block;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.addons.ExternalProvider;

import java.util.List;

/**
 * Interface for providing custom block data and retrieving block IDs.
 */
public interface BlockProvider extends ExternalProvider {

	/**
	 * Generates BlockData for a given player based on a block ID and a list of
	 * modifiers.
	 *
	 * @param context   The player for whom the block data is generated.
	 * @param id        The unique identifier for the block.
	 * @param modifiers A list of {@link BlockDataModifier} objects to apply to the
	 *                  block data.
	 * @return The generated {@link BlockData} for the specified block ID and
	 *         modifiers.
	 */
	BlockData blockData(@NotNull Context<Player> context, @NotNull String id, List<BlockDataModifier> modifiers);

	/**
	 * Retrieves the unique block ID associated with a given block.
	 *
	 * @param block The block for which the ID is to be retrieved.
	 * @return The unique block ID as a string, or null if no ID is associated with
	 *         the block.
	 */
	@Nullable
	String blockID(@NotNull Block block);
}