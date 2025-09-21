package com.swiftlicious.hellblock.loot.operation;

import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.utils.extras.TriFunction;

@FunctionalInterface
public interface WeightOperation extends TriFunction<Context<Player>, Double, Map<String, Double>, Double> {
}