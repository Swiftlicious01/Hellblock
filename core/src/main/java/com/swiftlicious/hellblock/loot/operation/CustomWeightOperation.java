package com.swiftlicious.hellblock.loot.operation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;

public class CustomWeightOperation implements WeightOperation {

	private final MathValue<Player> arg;
	private final boolean hasTotalWeight;
	private final List<String> otherEntries;
	private final List<Pair<String, String[]>> otherGroups;
	private final int sharedMembers;
	private final boolean forAvailable;

	public CustomWeightOperation(MathValue<Player> arg, boolean hasTotalWeight, List<String> otherEntries,
			List<Pair<String, String[]>> otherGroups, int sharedMembers, boolean forAvailable) {
		this.arg = arg;
		this.hasTotalWeight = hasTotalWeight;
		this.otherEntries = otherEntries;
		this.otherGroups = otherGroups;
		this.sharedMembers = sharedMembers;
		this.forAvailable = forAvailable;
	}

	@Override
	public Double apply(Context<Player> context, Double weight, Map<String, Double> weights) {
		if (this.forAvailable && weight <= 0) {
			return weight;
		}
		context.arg(ContextKeys.WEIGHT, weight);
		if (hasTotalWeight) {
			context.arg(ContextKeys.TOTAL_WEIGHT, getValidTotalWeight(weights.values()));
		}
		if (!otherEntries.isEmpty()) {
			otherEntries.forEach(otherWeight -> context.arg(ContextKeys.of("entry_" + otherWeight, Double.class), weights.get(otherWeight)));
		}
		if (!otherGroups.isEmpty()) {
			otherGroups.forEach(otherGroup -> {
				double totalWeight = 0;
				for (String id : otherGroup.right()) {
					totalWeight += weights.getOrDefault(id, 0d);
				}
				context.arg(ContextKeys.of("group_" + otherGroup.left(), Double.class), totalWeight);
			});
		}
		return arg.evaluate(context) / sharedMembers;
	}

	private double getValidTotalWeight(Collection<Double> weights) {
		double totalWeight = 0;
		for (Double weight : weights) {
			if (weight > 0) {
				totalWeight += weight;
			}
		}
		return totalWeight;
	}
}