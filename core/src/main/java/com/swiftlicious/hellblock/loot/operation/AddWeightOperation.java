package com.swiftlicious.hellblock.loot.operation;

import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.utils.extras.MathValue;

public class AddWeightOperation implements WeightOperation {

	private final MathValue<Player> arg;
	private final int sharedMembers;
	private final boolean forAvailable;

	public AddWeightOperation(MathValue<Player> arg, int sharedMembers, boolean forAvailable) {
		this.arg = arg;
		this.sharedMembers = sharedMembers;
		this.forAvailable = forAvailable;
	}

	@Override
	public Double apply(Context<Player> context, Double weight, Map<String, Double> weights) {
		if (this.forAvailable && weight <= 0) {
			return weight;
		}
		return weight + arg.evaluate(context) / sharedMembers;
	}
}