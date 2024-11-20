package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class RangedIntValue<T> implements MathValue<T> {

    private final MathValue<T> min;
    private final MathValue<T> max;

    public RangedIntValue(String value) {
        String[] split = value.split("~");
        if (split.length != 2) {
            throw new IllegalArgumentException("Correct ranged format `a~b`");
        }
        this.min = MathValue.auto(split[0]);
        this.max = MathValue.auto(split[1]);
    }

    @Override
    public double evaluate(Context<T> context) {
        return RandomUtils.generateRandomInt((int) min.evaluate(context), (int) max.evaluate(context));
    }
}