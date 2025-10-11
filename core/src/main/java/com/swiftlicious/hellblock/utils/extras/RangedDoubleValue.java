package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class RangedDoubleValue<T> implements MathValue<T> {

	private final MathValue<T> min;
	private final MathValue<T> max;

	public RangedDoubleValue(String value) {
		final String[] split = value.split("~");
		if (split.length != 2) {
			throw new IllegalArgumentException("Correct ranged format `a~b`");
		}
		this.min = MathValue.auto(split[0]);
		this.max = MathValue.auto(split[1]);
	}

	@Override
	public double evaluate(Context<T> context) {
		return RandomUtils.generateRandomDouble(min.evaluate(context), max.evaluate(context));
	}
}