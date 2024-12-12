package com.swiftlicious.hellblock.world;

import org.jetbrains.annotations.Nullable;

import com.flowpowered.nbt.CompoundMap;
import com.swiftlicious.hellblock.utils.extras.Key;

public interface HellblockBlockInterface {

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
}