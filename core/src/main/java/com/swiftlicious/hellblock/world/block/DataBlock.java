package com.swiftlicious.hellblock.world.block;

import com.flowpowered.nbt.Tag;
import com.swiftlicious.hellblock.utils.extras.SynchronizedCompoundMap;

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
	 * @return The previous tag associated with the key, or null if there was no
	 *         previous tag.
	 */
	Tag<?> set(String key, Tag<?> tag);

	/**
	 * Retrieves an NBT tag from the data block with the specified key.
	 *
	 * @param key The key of the tag to retrieve.
	 * @return The NBT tag associated with the key, or null if no tag is found.
	 */
	Tag<?> get(String key);

	/**
	 * Removes an NBT tag from the data block with the specified key.
	 *
	 * @param key The key of the tag to remove.
	 * @return The removed NBT tag, or null if no tag was found with the specified
	 *         key.
	 */
	Tag<?> remove(String key);

	/**
	 * Gets the synchronized compound map containing all the NBT data of the block.
	 *
	 * @return The {@link SynchronizedCompoundMap} containing the block's NBT data.
	 */
	SynchronizedCompoundMap compoundMap();
}