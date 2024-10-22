package com.swiftlicious.hellblock.listeners.fishing;

import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.loot.Loot;

/**
 * Represents a temporary state during fishing that includes an effect,
 * preparation, and loot.
 */
public class TempFishingState {

	private final Effect effect;
	private final FishingPreparation preparation;
	private Loot loot;

	/**
	 * Creates a new instance of TempFishingState.
	 *
	 * @param effect      The effect associated with this state.
	 * @param preparation The fishing preparation associated with this state.
	 * @param loot        The loot associated with this state.
	 */
	public TempFishingState(Effect effect, FishingPreparation preparation, Loot loot) {
		this.effect = effect;
		this.preparation = preparation;
		this.loot = loot;
	}

	/**
	 * Gets the effect associated with this fishing state.
	 *
	 * @return The effect.
	 */
	public Effect getEffect() {
		return effect;
	}

	/**
	 * Gets the fishing preparation associated with this fishing state.
	 *
	 * @return The fishing preparation.
	 */
	public FishingPreparation getPreparation() {
		return preparation;
	}

	/**
	 * Gets the loot associated with this fishing state.
	 *
	 * @return The loot.
	 */
	public Loot getLoot() {
		return loot;
	}

	/**
	 * Set the loot associated with this fishing state.
	 *
	 */
	public void setLoot(Loot loot) {
		this.loot = loot;
	}
}