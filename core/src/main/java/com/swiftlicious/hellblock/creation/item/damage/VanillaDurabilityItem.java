package com.swiftlicious.hellblock.creation.item.damage;

import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.creation.item.Item;

public class VanillaDurabilityItem implements DurabilityItem {

	private final Item<ItemStack> item;

	public VanillaDurabilityItem(Item<ItemStack> item) {
		this.item = item;
	}

	@Override
	public void damage(int value) {
		item.damage(value);
	}

	@Override
	public int damage() {
		return item.damage().orElse(0);
	}

	@Override
	public int maxDamage() {
		return item.maxDamage().get();
	}
}