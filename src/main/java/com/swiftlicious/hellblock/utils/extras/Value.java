package com.swiftlicious.hellblock.utils.extras;

import java.util.Map;

import org.bukkit.entity.Player;

public interface Value {

	double get(Player player, Map<String, String> values);
}
