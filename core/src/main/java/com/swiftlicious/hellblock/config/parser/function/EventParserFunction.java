package com.swiftlicious.hellblock.config.parser.function;

import java.util.function.Consumer;
import java.util.function.Function;

import com.swiftlicious.hellblock.config.parser.ParserType;
import com.swiftlicious.hellblock.handlers.EventCarrier;

public class EventParserFunction implements ConfigParserFunction {

    private final Function<Object, Consumer<EventCarrier.Builder>> function;

    public EventParserFunction(Function<Object, Consumer<EventCarrier.Builder>> function) {
        this.function = function;
    }

    public Consumer<EventCarrier.Builder> accept(Object object) {
        return function.apply(object);
    }

    @Override
    public ParserType type() {
        return ParserType.EVENT;
    }
}