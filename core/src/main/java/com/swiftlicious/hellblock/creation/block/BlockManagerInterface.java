package com.swiftlicious.hellblock.creation.block;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.context.Context;

/**
 * Interface for managing custom block loots.
 */
public interface BlockManagerInterface extends Reloadable {

	/**
	 * Get the {@link BlockDataModifierFactory} by ID
	 *
	 * @param id the id of the factory
	 * @return the factory instance
	 */
	@Nullable
	BlockDataModifierFactory getBlockDataModifierFactory(@NotNull String id);

	/**
	 * Get the {@link BlockStateModifierFactory} by ID
	 *
	 * @param id the id of the factory
	 * @return the factory instance
	 */
	@Nullable
	BlockStateModifierFactory getBlockStateModifierFactory(@NotNull String id);

	/**
	 * Registers a block loot.
	 *
	 * @param block the block configuration to register.
	 * @return true if registration is successful, false otherwise.
	 */
	boolean registerBlock(@NotNull BlockConfig block);

	/**
	 * Summons block loot based on the context.
	 *
	 * @param context the context of the player.
	 * @return the summoned falling block.
	 */
	@NotNull
	FallingBlock summonBlockLoot(@NotNull Context<Player> context);

	/**
	 * Retrieves the ID of the custom block at the specified location.
	 *
	 * @param location The location of the block.
	 * @return The ID of the block.
	 */
	@NotNull
	default String getBlockID(@NotNull Location location) {
		return getBlockID(location.getBlock());
	}

	/**
	 * Retrieves the ID of a block.
	 *
	 * @param block the block to get the ID from.
	 * @return the block ID.
	 */
	@NotNull
	String getBlockID(@NotNull Block block);
}