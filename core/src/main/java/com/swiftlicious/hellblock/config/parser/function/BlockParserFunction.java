package com.swiftlicious.hellblock.config.parser.function;

import java.util.function.Consumer;
import java.util.function.Function;

import com.swiftlicious.hellblock.config.parser.ParserType;
import com.swiftlicious.hellblock.creation.block.BlockConfig;

public class BlockParserFunction implements ConfigParserFunction {

	private final Function<Object, Consumer<BlockConfig.Builder>> function;

	public BlockParserFunction(Function<Object, Consumer<BlockConfig.Builder>> function) {
		this.function = function;
	}

	public Consumer<BlockConfig.Builder> accept(Object object) {
		return function.apply(object);
	}

	@Override
	public ParserType type() {
		return ParserType.BLOCK;
	}
}