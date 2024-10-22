package com.swiftlicious.hellblock.effects;

import java.util.List;

import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.WeightModifier;

public interface Effect {

	boolean canLavaFishing();

	double getMultipleLootChance();

	double getSize();

	double getSizeMultiplier();

	double getWaitTime();

	double getWaitTimeMultiplier();

	List<Pair<String, WeightModifier>> getWeightModifier();

	List<Pair<String, WeightModifier>> getWeightModifierIgnored();

	void merge(Effect effect);
}