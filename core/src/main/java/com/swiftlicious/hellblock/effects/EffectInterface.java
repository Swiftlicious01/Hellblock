package com.swiftlicious.hellblock.effects;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.utils.extras.Pair;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Represents an effect applied in the fishing.
 */
public interface EffectInterface {

	/**
	 * Retrieves the properties of this effect.
	 *
	 * @return a map of effect properties and their values
	 */
	Map<EffectProperties<?>, Object> properties();

	/**
	 * Put the properties into the effect
	 *
	 * @param properties properties to add
	 * @return the effect instance
	 */
	Effect properties(Map<EffectProperties<?>, Object> properties);

	/**
	 * Sets the specified property to the given value.
	 *
	 * @param key   the property key
	 * @param value the property value
	 * @param <C>   the type of the property value
	 * @return the effect instance with the updated property
	 */
	<C> Effect arg(EffectProperties<C> key, C value);

	/**
	 * Retrieves the value of the specified property.
	 *
	 * @param key the property key
	 * @param <C> the type of the property value
	 * @return the value of the specified property
	 */
	<C> C arg(EffectProperties<C> key);

	/**
	 * Gets the chance of multiple loots.
	 *
	 * @return the multiple loot chance
	 */
	double multipleLootChance();

	/**
	 * Sets the chance of multiple loots.
	 *
	 * @param multipleLootChance the new multiple loot chance
	 * @return the effect instance
	 */
	Effect multipleLootChance(double multipleLootChance);

	/**
	 * Gets the size adder.
	 *
	 * @return the size adder
	 */
	double sizeAdder();

	/**
	 * Sets the size adder.
	 *
	 * @param sizeAdder the new size adder
	 * @return the effect instance
	 */
	Effect sizeAdder(double sizeAdder);

	/**
	 * Gets the size multiplier.
	 *
	 * @return the size multiplier
	 */
	double sizeMultiplier();

	/**
	 * Sets the size multiplier.
	 *
	 * @param sizeMultiplier the new size multiplier
	 * @return the effect instance
	 */
	Effect sizeMultiplier(double sizeMultiplier);

	/**
	 * Gets the wait time adder.
	 *
	 * @return the wait time adder
	 */
	double waitTimeAdder();

	/**
	 * Sets the wait time adder.
	 *
	 * @param waitTimeAdder the new wait time adder
	 * @return the effect instance
	 */
	Effect waitTimeAdder(double waitTimeAdder);

	/**
	 * Gets the wait time multiplier.
	 *
	 * @return the wait time multiplier
	 */
	double waitTimeMultiplier();

	/**
	 * Sets the wait time multiplier.
	 *
	 * @param waitTimeMultiplier the new wait time multiplier
	 * @return the effect instance
	 */
	Effect waitTimeMultiplier(double waitTimeMultiplier);

	/**
	 * Gets the list of weight operations.
	 *
	 * @return the list of weight operations
	 */
	List<Pair<String, BiFunction<Context<Player>, Double, Double>>> weightOperations();

	/**
	 * Adds the list of weight operations.
	 *
	 * @param weightOperations the list of weight operations to add
	 * @return the effect instance
	 */
	Effect weightOperations(List<Pair<String, BiFunction<Context<Player>, Double, Double>>> weightOperations);

	/**
	 * Gets the list of weight operations that are conditions ignored.
	 *
	 * @return the list of weight operations that are conditions ignored
	 */
	List<Pair<String, BiFunction<Context<Player>, Double, Double>>> weightOperationsIgnored();

	/**
	 * Adds the list of weight operations that are conditions ignored.
	 *
	 * @param weightOperations the list of weight operations that are conditions
	 *                         ignored
	 * @return the effect instance
	 */
	Effect weightOperationsIgnored(List<Pair<String, BiFunction<Context<Player>, Double, Double>>> weightOperations);

	/**
	 * Combines this effect with another effect.
	 *
	 * @param effect the effect to combine with
	 */
	void combine(Effect effect);

	/**
	 * Get a copy of the effect
	 *
	 * @return the copied effect
	 */
	Effect copy();

	/**
	 * Creates a new instance of {@link Effect}.
	 *
	 * @return a new {@link Effect} instance
	 */
	static Effect newInstance() {
		return new Effect();
	}
}