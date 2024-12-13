package com.swiftlicious.hellblock.creation.entity;

import java.util.Optional;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.context.Context;

/**
 * EntityManager interface for managing custom entities in the plugin.
 */
public interface EntityManagerInterface extends Reloadable {

	/**
	 * Retrieves the configuration for a custom entity by its identifier.
	 *
	 * @param id The unique identifier of the entity configuration.
	 * @return An Optional containing the EntityConfig if found, or an empty
	 *         Optional if not found.
	 */
	Optional<EntityConfig> getEntity(String id);

	/**
	 * Registers a custom entity configuration.
	 *
	 * @param entity The entity configuration to register.
	 * @return True if the entity was registered successfully, false otherwise.
	 */
	boolean registerEntity(EntityConfig entity);

	/**
	 * Summons an entity as loot based on the given context.
	 *
	 * @param context The context of the player.
	 * @return The summoned entity.
	 */
	@NotNull
	Entity summonEntityLoot(Context<Player> context);
}