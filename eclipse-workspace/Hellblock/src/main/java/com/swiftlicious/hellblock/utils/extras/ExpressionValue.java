package com.swiftlicious.hellblock.utils.extras;

import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;

public class ExpressionValue implements Value {

	private final String expression;

	public ExpressionValue(String expression) {
		this.expression = expression;
	}

	@Override
	public double get(Player player, Map<String, String> values) {
		return HellblockPlugin.getInstance().getConfigUtils().getExpressionValue(player, expression, values);
	}
}
