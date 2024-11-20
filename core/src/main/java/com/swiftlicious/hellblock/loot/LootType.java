package com.swiftlicious.hellblock.loot;

public enum LootType {

	ITEM, ENTITY, BLOCK;

	@Override
	public String toString() {
		return name();
	}
}