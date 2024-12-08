package com.swiftlicious.hellblock.utils.registry;

import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.world.block.HellblockBlock;

@DoNotUse(message = "Internal use only. Avoid using this class directly.")
@Deprecated(since = "3.6", forRemoval = false)
public class InternalRegistries {

	public static final WriteableRegistry<Key, HellblockBlock> BLOCK = new MappedRegistry<>(
			Key.of("mechanism", "block"));
}