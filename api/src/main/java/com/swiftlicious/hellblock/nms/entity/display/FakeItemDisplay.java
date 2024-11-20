package com.swiftlicious.hellblock.nms.entity.display;

import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.nms.entity.FakeEntity;

public interface FakeItemDisplay extends FakeEntity {

	void item(ItemStack itemStack);
}