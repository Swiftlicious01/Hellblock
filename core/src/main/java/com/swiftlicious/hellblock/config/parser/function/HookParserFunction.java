package com.swiftlicious.hellblock.config.parser.function;

import java.util.function.Consumer;
import java.util.function.Function;

import com.swiftlicious.hellblock.config.parser.ParserType;
import com.swiftlicious.hellblock.mechanics.hook.HookConfig;

public class HookParserFunction implements ConfigParserFunction {

    private final Function<Object, Consumer<HookConfig.Builder>> function;

    public HookParserFunction(Function<Object, Consumer<HookConfig.Builder>> function) {
        this.function = function;
    }

    public Consumer<HookConfig.Builder> accept(Object object) {
        return function.apply(object);
    }

    @Override
    public ParserType type() {
        return ParserType.HOOK;
    }
}