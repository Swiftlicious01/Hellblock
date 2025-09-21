package com.swiftlicious.hellblock.loot.operation;

import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.utils.extras.MathValue;

public class MultiplyWeightOperation implements WeightOperation {

	private final MathValue<Player> arg;
	private final boolean forAvailable;

	public MultiplyWeightOperation(MathValue<Player> arg, boolean forAvailable) {
		this.arg = arg;
		this.forAvailable = forAvailable;
	}

	@Override
	public Double apply(Context<Player> context, Double weight, Map<String, Double> weights) {
		if (this.forAvailable && weight <= 0) {
			return weight;
		}
		return weight * arg.evaluate(context);
	}
}