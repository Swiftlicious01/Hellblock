package com.swiftlicious.hellblock.config.parser.function;

import java.util.function.Consumer;
import java.util.function.Function;

import com.swiftlicious.hellblock.config.parser.ParserType;
import com.swiftlicious.hellblock.loot.Loot;

public class LootParserFunction implements ConfigParserFunction {

	private final Function<Object, Consumer<Loot.Builder>> function;

	public LootParserFunction(Function<Object, Consumer<Loot.Builder>> function) {
		this.function = function;
	}

	public Consumer<Loot.Builder> accept(Object object) {
		return function.apply(object);
	}

	@Override
	public ParserType type() {
		return ParserType.LOOT;
	}
}