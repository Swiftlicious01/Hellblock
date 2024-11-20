package com.swiftlicious.hellblock.nms.entity.armorstand;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.nms.entity.FakeNamedEntity;

public interface FakeArmorStand extends FakeNamedEntity {

	void small(boolean small);

	void invisible(boolean invisible);

	void equipment(EquipmentSlot slot, ItemStack itemStack);

	void updateEquipment(Player player);
}