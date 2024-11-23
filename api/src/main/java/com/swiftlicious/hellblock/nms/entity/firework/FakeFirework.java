package com.swiftlicious.hellblock.nms.entity.firework;

import org.bukkit.inventory.ItemStack;
import com.swiftlicious.hellblock.nms.entity.FakeEntity;

public interface FakeFirework extends FakeEntity {

	void flightTime(int flightTime);
	
	void invisible(boolean invisible);

	abstract ItemStack getFireworkStack();
}