package com.swiftlicious.hellblock.loot;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.effects.Effect;

/**
 * Interface for managing loot
 */
public interface LootManagerInterface extends Reloadable {

	/**
	 * Registers a new loot item.
	 *
	 * @param loot the {@link Loot} to be registered
	 * @return true if the loot was successfully registered, false otherwise
	 */
	boolean registerLoot(@NotNull Loot loot);

	/**
	 * Get all the registered loots
	 *
	 * @return registered loots
	 */
	Collection<Loot> getRegisteredLoots();

	/**
	 * Retrieves the members of a loot group identified by the given key.
	 *
	 * @param key the key identifying the loot group
	 * @return a list of member identifiers as strings
	 */
	@NotNull
	List<String> getGroupMembers(String key);

	/**
	 * Retrieves a loot item by its key.
	 *
	 * @param key the key identifying the loot item
	 * @return an {@link Optional} containing the {@link Loot} if found, or an empty
	 *         {@link Optional} if not
	 */
	@NotNull
	Optional<Loot> getLoot(String key);

	/**
	 * Retrieves a map of weighted loots based on the given effect and context.
	 *
	 * @param effect  the {@link Effect} influencing the loot selection
	 * @param context the {@link Context} in which the loot selection occurs
	 * @return a map of loot keys to their respective weights
	 */
	Map<String, Double> getWeightedLoots(Effect effect, Context<Player> context);

	/**
	 * Retrieves the next loot item based on the given effect and context.
	 *
	 * @param effect  the {@link Effect} influencing the loot selection
	 * @param context the {@link Context} in which the loot selection occurs
	 * @return the next {@link Loot} item, or null if no suitable loot is found
	 */
	@Nullable
	Loot getNextLoot(Effect effect, Context<Player> context);
}