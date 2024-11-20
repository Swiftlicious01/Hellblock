package com.swiftlicious.hellblock.loot;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.EffectInterface;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.extras.MathValue;

public class LootBaseEffect implements LootBaseEffectInterface {

	private final MathValue<Player> waitTimeAdder;
	private final MathValue<Player> waitTimeMultiplier;

	public LootBaseEffect(MathValue<Player> waitTimeAdder, MathValue<Player> waitTimeMultiplier) {
		this.waitTimeAdder = waitTimeAdder;
		this.waitTimeMultiplier = waitTimeMultiplier;
	}

	@Override
	public MathValue<Player> waitTimeAdder() {
		return waitTimeAdder;
	}

	@Override
	public MathValue<Player> waitTimeMultiplier() {
		return waitTimeMultiplier;
	}

	@Override
	public Effect toEffect(Context<Player> context) {
		Effect effect = EffectInterface.newInstance();
		effect.waitTimeAdder(waitTimeAdder.evaluate(context));
		effect.waitTimeMultiplier(waitTimeMultiplier.evaluate(context));
		return effect;
	}

	public static class Builder implements BuilderInterface {
		private MathValue<Player> waitTimeAdder = DEFAULT_WAIT_TIME_ADDER;
		private MathValue<Player> waitTimeMultiplier = DEFAULT_WAIT_TIME_MULTIPLIER;

		@Override
		public Builder waitTimeAdder(MathValue<Player> waitTimeAdder) {
			this.waitTimeAdder = waitTimeAdder;
			return this;
		}

		@Override
		public Builder waitTimeMultiplier(MathValue<Player> waitTimeMultiplier) {
			this.waitTimeMultiplier = waitTimeMultiplier;
			return this;
		}

		@Override
		public LootBaseEffect build() {
			return new LootBaseEffect(waitTimeAdder, waitTimeMultiplier);
		}
	}
}