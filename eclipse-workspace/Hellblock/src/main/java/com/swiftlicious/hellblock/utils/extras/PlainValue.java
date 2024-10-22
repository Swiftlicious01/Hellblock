package com.swiftlicious.hellblock.utils.extras;

import java.util.Map;

import org.bukkit.entity.Player;

public class PlainValue implements Value {

	private final double value;

	public PlainValue(double value) {
		this.value = value;
	}

	@Override
	public double get(Player player, Map<String, String> values) {
		return value;
	}
}
