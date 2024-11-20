package com.swiftlicious.hellblock.loot;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.loot.LootBaseEffect.Builder;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.extras.MathValue;

/**
 * Represents the base effect applied to loot.
 */
public interface LootBaseEffectInterface {

	MathValue<Player> DEFAULT_WAIT_TIME_ADDER = MathValue.plain(0);
	MathValue<Player> DEFAULT_WAIT_TIME_MULTIPLIER = MathValue.plain(1);

	/**
	 * Gets the adder value for wait time.
	 *
	 * @return the wait time adder value
	 */
	MathValue<Player> waitTimeAdder();

	/**
	 * Gets the multiplier value for wait time.
	 *
	 * @return the wait time multiplier value
	 */
	MathValue<Player> waitTimeMultiplier();

	/**
	 * Creates a new {@link Builder} instance for constructing
	 * {@link LootBaseEffect} objects.
	 *
	 * @return a new {@link Builder} instance
	 */
	static Builder builder() {
		return new LootBaseEffect.Builder();
	}

	/**
	 * Convert the base effect to an effect instance
	 *
	 * @param context player context
	 * @return the effect instance
	 */
	Effect toEffect(Context<Player> context);

	/**
	 * Builder interface for constructing {@link LootBaseEffect} instances.
	 */
	interface BuilderInterface {

		/**
		 * Sets the adder value for wait time.
		 *
		 * @param waitTimeAdder the wait time adder value
		 * @return the builder instance
		 */
		Builder waitTimeAdder(MathValue<Player> waitTimeAdder);

		/**
		 * Sets the multiplier value for wait time.
		 *
		 * @param waitTimeMultiplier the wait time multiplier value
		 * @return the builder instance
		 */
		Builder waitTimeMultiplier(MathValue<Player> waitTimeMultiplier);

		/**
		 * Builds and returns the {@link LootBaseEffect} instance.
		 *
		 * @return the built {@link LootBaseEffect} instance
		 */
		LootBaseEffect build();
	}
}