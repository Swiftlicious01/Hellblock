package com.swiftlicious.hellblock.config.parser.function;

import java.util.function.Consumer;
import java.util.function.Function;

import com.swiftlicious.hellblock.config.parser.ParserType;
import com.swiftlicious.hellblock.creation.entity.EntityConfig;

public class EntityParserFunction implements ConfigParserFunction {

    private final Function<Object, Consumer<EntityConfig.Builder>> function;

    public EntityParserFunction(Function<Object, Consumer<EntityConfig.Builder>> function) {
        this.function = function;
    }

    public Consumer<EntityConfig.Builder> accept(Object object) {
        return function.apply(object);
    }

    @Override
    public ParserType type() {
        return ParserType.ENTITY;
    }
}