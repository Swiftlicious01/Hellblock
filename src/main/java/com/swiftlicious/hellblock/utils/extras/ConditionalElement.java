package com.swiftlicious.hellblock.utils.extras;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

public class ConditionalElement {

	private final List<Pair<String, WeightModifier>> modifierList;
	private final Map<String, ConditionalElement> subLoots;
	private final Requirement[] requirements;

	public ConditionalElement(Requirement[] requirements, List<Pair<String, WeightModifier>> modifierList,
			Map<String, ConditionalElement> subElements) {
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
	synchronized public void combine(Player player, Map<String, Double> weightMap) {
		for (Pair<String, WeightModifier> modifierPair : this.modifierList) {
			double previous = weightMap.getOrDefault(modifierPair.left(), 0d);
			weightMap.put(modifierPair.left(), modifierPair.right().modify(player, previous));
		}
	}

	public Requirement[] getRequirements() {
		return requirements;
	}

	public Map<String, ConditionalElement> getSubElements() {
		return subLoots;
	}
}
