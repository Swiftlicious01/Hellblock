package com.swiftlicious.hellblock.nms.entity.armorstand;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.nms.entity.FakeNamedEntity;

public interface FakeArmorStand extends FakeNamedEntity {

	void small(boolean small);

	void invisible(boolean invisible);

	void gravity(boolean gravity);

	void basePlate(boolean basePlate);

	void marker(boolean marker);

	void equipment(EquipmentSlot slot, ItemStack itemStack);

	void updateEquipment(Player player);

	void teleport(Location newLocation, Player player);

	Location getLocation();

	boolean isDead();
	
	void setCamera(Player player);
	
	void resetCamera(Player player);
	
	void keepClientCameraStable(Player player);
}