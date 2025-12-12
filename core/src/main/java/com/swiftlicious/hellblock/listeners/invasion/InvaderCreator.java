package com.swiftlicious.hellblock.listeners.invasion;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

public interface InvaderCreator {

	Mob spawn(Location loc, CustomInvasion invasion);
	
	EntityType getEntityType();
}
