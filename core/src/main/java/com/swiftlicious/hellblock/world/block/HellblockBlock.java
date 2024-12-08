package com.swiftlicious.hellblock.world.block;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import com.flowpowered.nbt.CompoundMap;
import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.utils.extras.NamedTextColor;
import com.swiftlicious.hellblock.world.HellblockBlockState;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;
import com.swiftlicious.hellblock.world.wrapper.WrappedBreakEvent;
import com.swiftlicious.hellblock.world.wrapper.WrappedInteractEvent;
import com.swiftlicious.hellblock.world.wrapper.WrappedPlaceEvent;

public interface HellblockBlock {

	/**
	 * Get the key
	 *
	 * @return key
	 */
	Key type();

	/**
	 * Create a HellblockBlockState based on this type
	 *
	 * @return HellblockBlockState
	 */
	HellblockBlockState createBlockState();

	/**
	 * Create a HellblockBlockState based on the item id
	 *
	 * @return HellblockBlockState
	 */
	@Nullable
	HellblockBlockState createBlockState(String itemID);

	/**
	 * Create a HellblockBlockState based on this type and provided data
	 *
	 * @return HellblockBlockState
	 */
	HellblockBlockState createBlockState(CompoundMap data);

	/**
	 * Runs scheduled tick tasks
	 */
	void scheduledTick(HellblockBlockState state, HellblockWorld<?> world, Pos3 location, boolean offlineTick);

	/**
	 * Runs random tick tasks
	 */
	void randomTick(HellblockBlockState state, HellblockWorld<?> world, Pos3 location, boolean offlineTick);

	/**
	 * Handles interactions
	 */
	void onInteract(WrappedInteractEvent event);

	/**
	 * Handles breaks
	 */
	void onBreak(WrappedBreakEvent event);

	/**
	 * Handles placement
	 */
	void onPlace(WrappedPlaceEvent event);

	/**
	 * Checks if the id is an instance of this block type
	 *
	 * @param id id
	 * @return is instance or not
	 */
	boolean isInstance(String id);

	/**
	 * Restores the bukkit block state or furniture based on the given block state
	 *
	 * @param location the location of the block
	 * @param state    the provided state
	 */
	void restore(Location location, HellblockBlockState state);

	/**
	 * Get the color on insight mode
	 *
	 * @return the color
	 */
	NamedTextColor insightColor();
}