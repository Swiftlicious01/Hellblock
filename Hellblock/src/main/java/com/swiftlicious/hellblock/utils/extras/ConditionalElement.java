package com.swiftlicious.hellblock.utils.extras;

import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;

public class ConditionalElement {

	private final List<Pair<String, WeightModifier>> modifierList;
	private final HashMap<String, ConditionalElement> subLoots;
	private final Requirement[] requirements;

	public ConditionalElement(Requirement[] requirements, List<Pair<String, WeightModifier>> modifierList,
			HashMap<String, ConditionalElement> subElements) {
		this.requirements = requirements;
		this.modifierList = modifierList;
		this.subLoots = subElements;
	}

	/**
	 * Combines the weight modifiers for this element.
	 *
	 * @param player    The player for whom the modifiers are applied.
	 * @param weightMap The map of weight modifiers.
	 */
	synchronized public void combine(Player player, HashMap<String, Double> weightMap) {
		for (Pair<String, WeightModifier> modifierPair : this.modifierList) {
			double previous = weightMap.getOrDefault(modifierPair.left(), 0d);
			weightMap.put(modifierPair.left(), modifierPair.right().modify(player, previous));
		}
	}

	public Requirement[] getRequirements() {
		return requirements;
	}

	public HashMap<String, ConditionalElement> getSubElements() {
		return subLoots;
	}
}
