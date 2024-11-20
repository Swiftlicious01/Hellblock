package com.swiftlicious.hellblock.creation.item.damage;

public interface DurabilityItem {

	void damage(int value);

	int damage();

	int maxDamage();
}