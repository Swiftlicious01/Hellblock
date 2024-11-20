package com.swiftlicious.hellblock.config.parser.function;

import java.util.function.Consumer;
import java.util.function.Function;

import com.swiftlicious.hellblock.config.parser.ParserType;
import com.swiftlicious.hellblock.loot.LootBaseEffect;

public class BaseEffectParserFunction implements ConfigParserFunction {

	private final Function<Object, Consumer<LootBaseEffect.Builder>> function;

	public BaseEffectParserFunction(Function<Object, Consumer<LootBaseEffect.Builder>> function) {
		this.function = function;
	}

	public Consumer<LootBaseEffect.Builder> accept(Object object) {
		return function.apply(object);
	}

	@Override
	public ParserType type() {
		return ParserType.BASE_EFFECT;
	}
}