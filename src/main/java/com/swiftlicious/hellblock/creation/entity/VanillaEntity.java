package com.swiftlicious.hellblock.creation.entity;

import java.util.Locale;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class VanillaEntity implements EntityLibrary {

	@Override
	public String identification() {
		return "vanilla";
	}

	@Override
	public Entity spawn(Location location, String id, Map<String, Object> propertyMap) {
		return location.getWorld().spawnEntity(location, EntityType.valueOf(id.toUpperCase(Locale.ENGLISH)));
	}
}
