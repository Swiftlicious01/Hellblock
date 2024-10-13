package com.swiftlicious.hellblock.loot;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.utils.extras.Condition;

public interface LootManagerInterface {

	/**
	 * Retrieves a list of loot IDs associated with a loot group key.
	 *
	 * @param key The key of the loot group.
	 * @return A list of loot IDs belonging to the specified loot group, or null if
	 *         not found.
	 */
	@Nullable
	List<String> getLootGroup(String key);

	/**
	 * Retrieves a loot configuration based on a provided loot key.
	 *
	 * @param key The key of the loot configuration.
	 * @return The Loot object associated with the specified loot key, or null if
	 *         not found.
	 */
	@Nullable
	Loot getLoot(String key);

	/**
	 * Retrieves a collection of all loot configuration keys.
	 *
	 * @return A collection of all loot configuration keys.
	 */
	Collection<String> getAllLootKeys();

	/**
	 * Retrieves a collection of all loot configurations.
	 *
	 * @return A collection of all loot configurations.
	 */
	Collection<Loot> getAllLoots();

	/**
	 * Retrieves loot configurations with weights based on a given condition.
	 *
	 * @param condition The condition used to filter loot configurations.
	 * @return A mapping of loot configuration keys to their associated weights.
	 */
	HashMap<String, Double> getLootWithWeight(Condition condition);

	/**
	 * Get a collection of possible loot keys based on a given condition.
	 *
	 * @param condition The condition to determine possible loot.
	 * @return A collection of loot keys.
	 */
	Collection<String> getPossibleLootKeys(Condition condition);

	/**
	 * Get a map of possible loot keys with their corresponding weights, considering
	 * fishing effect and condition.
	 *
	 * @param initialEffect The effect to apply weight modifiers.
	 * @param condition     The condition to determine possible loot.
	 * @return A map of loot keys and their weights.
	 */
	@NotNull
	Map<String, Double> getPossibleLootKeysWithWeight(Effect initialEffect, Condition condition);

	/**
	 * Get the next loot item based on fishing effect and condition.
	 *
	 * @param effect    The effect to apply weight modifiers.
	 * @param condition The condition to determine possible loot.
	 * @return The next loot item, or null if it doesn't exist.
	 */
	@Nullable
	Loot getNextLoot(Effect effect, Condition condition);
}
