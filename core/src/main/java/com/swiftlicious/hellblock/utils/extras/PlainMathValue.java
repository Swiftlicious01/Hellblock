package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.context.Context;

public class PlainMathValue<T> implements MathValue<T> {

	private final double value;

	public PlainMathValue(double value) {
		this.value = value;
	}

	@Override
	public double evaluate(Context<T> context) {
		return value;
	}
}