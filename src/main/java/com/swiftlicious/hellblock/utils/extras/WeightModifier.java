package com.swiftlicious.hellblock.utils.extras;

import org.bukkit.entity.Player;

public interface WeightModifier {

	double modify(Player player, double weight);
}