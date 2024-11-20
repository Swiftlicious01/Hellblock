package com.swiftlicious.hellblock.creation.entity;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.creation.addons.ExternalProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * The EntityProvider interface defines methods to interact with external entity
 * spawning systems, allowing the spawning of entities at specified locations
 * with given properties. Implementations of this interface should provide the
 * logic for spawning entities and managing their properties.
 */
public interface EntityProvider extends ExternalProvider {

	/**
	 * Spawns an entity at the specified location with the given properties.
	 *
	 * @param location    The location where the entity will be spawned.
	 * @param id          The identifier of the entity to be spawned.
	 * @param propertyMap A map containing additional properties for the entity.
	 * @return The spawned entity.
	 */
	@NotNull
	Entity spawn(@NotNull Location location, @NotNull String id, @NotNull Map<String, Object> propertyMap);

	default Entity spawn(@NotNull Location location, @NotNull String id) {
		return spawn(location, id, new HashMap<>());
	}
}