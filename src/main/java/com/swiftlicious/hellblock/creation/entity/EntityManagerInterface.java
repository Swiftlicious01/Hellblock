package com.swiftlicious.hellblock.creation.entity;

import org.bukkit.Location;

import com.swiftlicious.hellblock.loot.Loot;

public interface EntityManagerInterface {

	/**
	 * Registers an entity library for use in the plugin.
	 *
	 * @param entityLibrary The entity library to register.
	 * @return {@code true} if the entity library was successfully registered,
	 *         {@code false} if it already exists.
	 */
	boolean registerEntityLibrary(EntityLibrary entityLibrary);

	/**
	 * Unregisters an entity library by its identification key.
	 *
	 * @param identification The identification key of the entity library to
	 *                       unregister.
	 * @return {@code true} if the entity library was successfully unregistered,
	 *         {@code false} if it does not exist.
	 */
	boolean unregisterEntityLibrary(String identification);

	/**
	 * Summons an entity based on the given loot configuration to a specified
	 * location.
	 *
	 * @param hookLocation   The location where the entity will be summoned,
	 *                       typically where the fishing hook is.
	 * @param playerLocation The location of the player who triggered the entity
	 *                       summoning.
	 * @param loot           The loot configuration that defines the entity to be
	 *                       summoned.
	 */
	void summonEntity(Location hookLocation, Location playerLocation, Loot loot);
}
