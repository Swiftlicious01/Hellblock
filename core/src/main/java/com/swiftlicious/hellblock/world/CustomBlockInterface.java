package com.swiftlicious.hellblock.world;

import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public interface CustomBlockInterface {

	/**
	 * Get the key
	 *
	 * @return key
	 */
	Key type();

	/**
	 * Create a CustomBlockState based on this type
	 *
	 * @return CustomBlockState
	 */
	CustomBlockState createBlockState();

	/**
	 * Create a CustomBlockState based on the item id
	 *
	 * @return CustomBlockState
	 */
	@Nullable
	CustomBlockState createBlockState(String itemID);

	/**
	 * Create a CustomBlockState based on this type and provided data
	 *
	 * @return CustomBlockState
	 */
	CustomBlockState createBlockState(CompoundBinaryTag data);
}