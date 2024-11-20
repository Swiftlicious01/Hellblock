package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.handlers.ExpressionHelper;
import com.swiftlicious.hellblock.player.Context;

public class ExpressionMathValue<T> implements MathValue<T> {

	private final TextValue<T> raw;

	public ExpressionMathValue(String raw) {
		this.raw = TextValue.auto(raw);
	}

	@Override
	public double evaluate(Context<T> context) {
		return ExpressionHelper.evaluate(raw.render(context));
	}

	@Override
	public double evaluate(Context<T> context, boolean parseRawPlaceholders) {
		return ExpressionHelper.evaluate(raw.render(context, parseRawPlaceholders));
	}
}
