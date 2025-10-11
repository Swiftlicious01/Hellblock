package com.swiftlicious.hellblock.config.parser;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.function.BaseEffectParserFunction;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EffectModifierParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EventParserFunction;
import com.swiftlicious.hellblock.config.parser.function.ItemParserFunction;
import com.swiftlicious.hellblock.config.parser.function.LootParserFunction;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.CustomItemInterface;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.effects.EffectModifierInterface;
import com.swiftlicious.hellblock.handlers.EventCarrier;
import com.swiftlicious.hellblock.handlers.EventCarrierInterface;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootInterface;
import com.swiftlicious.hellblock.loot.LootBaseEffect;
import com.swiftlicious.hellblock.loot.LootBaseEffectInterface;
import com.swiftlicious.hellblock.loot.LootType;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.utils.extras.PriorityFunction;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RodConfigParser {

	private final String id;
	private final String material;
	private final List<PriorityFunction<BiConsumer<Item<ItemStack>, Context<Player>>>> tagConsumers = new ArrayList<>();
	private final List<Consumer<EventCarrier.Builder>> eventBuilderConsumers = new ArrayList<>();
	private final List<Consumer<EffectModifier.Builder>> effectBuilderConsumers = new ArrayList<>();
	private final List<Consumer<LootBaseEffect.Builder>> baseEffectBuilderConsumers = new ArrayList<>();
	private final List<Consumer<Loot.Builder>> lootBuilderConsumers = new ArrayList<>();

	public RodConfigParser(String id, Section section, Map<String, Node<ConfigParserFunction>> functionMap) {
		this.id = id;
		this.material = section.contains("material") ? section.getString("material") : Material.FISHING_ROD.name();
		if (!section.contains("tag")) {
			section.set("tag", true);
		}
		analyze(section, functionMap);
	}

	private void analyze(Section section, Map<String, Node<ConfigParserFunction>> functionMap) {
		final Map<String, Object> dataMap = section.getStringRouteMappedValues(false);
		for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
			final String key = entry.getKey();
			final Node<ConfigParserFunction> node = functionMap.get(key);
			if (node == null) {
				continue;
			}
			final ConfigParserFunction function = node.nodeValue();
			if (function != null) {
				switch (function.type()) {
				case BASE_EFFECT -> {
					final BaseEffectParserFunction baseEffectParserFunction = (BaseEffectParserFunction) function;
					final Consumer<LootBaseEffect.Builder> consumer = baseEffectParserFunction.accept(entry.getValue());
					baseEffectBuilderConsumers.add(consumer);
				}
				case LOOT -> {
					final LootParserFunction lootParserFunction = (LootParserFunction) function;
					final Consumer<Loot.Builder> consumer = lootParserFunction.accept(entry.getValue());
					lootBuilderConsumers.add(consumer);
				}
				case ITEM -> {
					final ItemParserFunction propertyFunction = (ItemParserFunction) function;
					final BiConsumer<Item<ItemStack>, Context<Player>> result = propertyFunction
							.accept(entry.getValue());
					tagConsumers.add(new PriorityFunction<>(propertyFunction.getPriority(), result));
				}
				case EVENT -> {
					final EventParserFunction eventParserFunction = (EventParserFunction) function;
					final Consumer<EventCarrier.Builder> consumer = eventParserFunction.accept(entry.getValue());
					eventBuilderConsumers.add(consumer);
				}
				case EFFECT_MODIFIER -> {
					final EffectModifierParserFunction effectModifierParserFunction = (EffectModifierParserFunction) function;
					final Consumer<EffectModifier.Builder> consumer = effectModifierParserFunction
							.accept(entry.getValue());
					effectBuilderConsumers.add(consumer);
				}
				default -> throw new IllegalArgumentException("Unexpected value: " + function.type());
				}
				continue;
			}
			if (entry.getValue() instanceof Section innerSection) {
				analyze(innerSection, node.getChildTree());
			}
		}
	}

	public CustomItem getItem() {
		return CustomItemInterface.builder().material(material).id(id).tagConsumers(tagConsumers).build();
	}

	public EventCarrier getEventCarrier() {
		final EventCarrier.Builder builder = EventCarrierInterface.builder().id(id).type(MechanicType.ROD);
		eventBuilderConsumers.forEach(consumer -> consumer.accept(builder));
		return builder.build();
	}

	public EffectModifier getEffectModifier() {
		final EffectModifier.Builder builder = EffectModifierInterface.builder().id(id).type(MechanicType.ROD);
		effectBuilderConsumers.forEach(consumer -> consumer.accept(builder));
		return builder.build();
	}

	private LootBaseEffect getBaseEffect() {
		final LootBaseEffect.Builder builder = LootBaseEffectInterface.builder();
		baseEffectBuilderConsumers.forEach(consumer -> consumer.accept(builder));
		return builder.build();
	}

	public Loot getLoot() {
		final Loot.Builder builder = LootInterface.builder().id(id).type(LootType.ITEM).lootBaseEffect(getBaseEffect());
		lootBuilderConsumers.forEach(consumer -> consumer.accept(builder));
		return builder.build();
	}
}