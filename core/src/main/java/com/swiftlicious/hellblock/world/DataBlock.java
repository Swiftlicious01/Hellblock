package com.swiftlicious.hellblock.world;

import com.swiftlicious.hellblock.utils.extras.SynchronizedNBTCompound;

import net.kyori.adventure.nbt.BinaryTag;

/**
 * Interface representing a data block that can store, retrieve, and manipulate
 * NBT data.
 */
public interface DataBlock {

	/**
	 * Sets an NBT tag in the data block with the specified key.
	 *
	 * @param key The key for the tag to set.
	 * @param tag The NBT tag to set.
	 */
	void set(String key, BinaryTag tag);

	/**
	 * Retrieves an NBT tag from the data block with the specified key.
	 *
	 * @param key The key of the tag to retrieve.
	 * @return The NBT tag associated with the key, or null if no tag is found.
	 */
	BinaryTag get(String key);

	/**
	 * Removes an NBT tag from the data block with the specified key.
	 *
	 * @param key The key of the tag to remove.
	 */
	void remove(String key);

	/**
	 * Gets the synchronized compound containing all the NBT data of the block.
	 *
	 * @return The {@link SynchronizedNBTCompound} containing the block's NBT data.
	 */
	SynchronizedNBTCompound compound();
}