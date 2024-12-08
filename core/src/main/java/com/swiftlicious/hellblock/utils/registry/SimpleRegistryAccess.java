package com.swiftlicious.hellblock.utils.registry;

import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.world.block.HellblockBlock;

@SuppressWarnings("deprecation")
public class SimpleRegistryAccess implements RegistryAccess {

	private boolean frozen;
	private static SimpleRegistryAccess instance;

	private SimpleRegistryAccess() {
		instance = this;
	}

	public static SimpleRegistryAccess getInstance() {
		if (instance == null) {
			instance = new SimpleRegistryAccess();
		}
		return instance;
	}

	public void freeze() {
		this.frozen = true;
	}

	@Override
	public void registerBlockMechanic(HellblockBlock block) {
		if (frozen)
			throw new RuntimeException("Registries are frozen");
		InternalRegistries.BLOCK.register(block.type(), block);
	}

	@Override
	public Registry<Key, HellblockBlock> getBlockRegistry() {
		return InternalRegistries.BLOCK;
	}
}