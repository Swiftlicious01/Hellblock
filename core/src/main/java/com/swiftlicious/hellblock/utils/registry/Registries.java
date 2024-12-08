package com.swiftlicious.hellblock.utils.registry;

import org.jetbrains.annotations.ApiStatus;

import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.world.block.HellblockBlock;

@ApiStatus.Internal
public class Registries {

	public static final ClearableRegistry<String, HellblockBlock> BLOCKS = new ClearableMappedRegistry<>(
			Key.of("internal", "blocks"));
}