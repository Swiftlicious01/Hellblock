package com.swiftlicious.hellblock.creation.entity;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface EntityLibrary {

	String identification();

	Entity spawn(Location location, String id, Map<String, Object> propertyMap);
}