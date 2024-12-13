package com.swiftlicious.hellblock.config.parser.function;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.config.parser.ParserType;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.Item;

public class ItemParserFunction implements ConfigParserFunction {

    private final Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> function;
    private final int priority;

    public ItemParserFunction(int priority, Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> function) {
        this.function = function;
        this.priority = priority;
    }

    public BiConsumer<Item<ItemStack>, Context<Player>> accept(Object object) {
        return function.apply(object);
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public ParserType type() {
        return ParserType.ITEM;
    }
}