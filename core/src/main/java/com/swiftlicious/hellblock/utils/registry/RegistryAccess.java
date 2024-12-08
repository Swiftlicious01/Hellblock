package com.swiftlicious.hellblock.utils.registry;

import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.world.block.HellblockBlock;

/**
 * Interface defining methods for registering and accessing different types of
 * custom mechanics such as blocks in the Hellblock plugin.
 */
public interface RegistryAccess {

	/**
	 * Registers a new custom block mechanic.
	 *
	 * @param block The custom block to register.
	 */
	void registerBlockMechanic(HellblockBlock block);

	/**
	 * Retrieves the registry containing all registered custom blocks.
	 *
	 * @return the block registry
	 */
	Registry<Key, HellblockBlock> getBlockRegistry();
}