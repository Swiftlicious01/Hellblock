package com.swiftlicious.hellblock.schematic;

import java.io.Serializable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

/**
 * Serializable representation of an entity inside an island snapshot. Stores
 * type, position, and UUID. Can be expanded with metadata later.
 */
public record IslandSnapshotEntity(String type, double x, double y, double z, float yaw, float pitch)
		implements Serializable {

	/**
	 * Creates an IslandSnapshotEntity from a Bukkit Entity.
	 * 
	 * @param entity The entity to snapshot.
	 * @return The IslandSnapshotEntity representation.
	 */
	public static IslandSnapshotEntity fromEntity(Entity entity) {
		final Location loc = entity.getLocation();
		return new IslandSnapshotEntity(entity.getType().name(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(),
				loc.getPitch());
	}

	/**
	 * Restores the entity in the given world at the stored location.
	 * 
	 * @param world The world to spawn the entity in.
	 */
	public void restore(World world) {
		final EntityType entityType = EntityType.valueOf(type);
		final Location loc = new Location(world, x, y, z, yaw, pitch);
		world.spawnEntity(loc, entityType);
	}
}