package com.swiftlicious.hellblock.config.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EffectModifierParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EventParserFunction;
import com.swiftlicious.hellblock.config.parser.function.ItemParserFunction;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.CustomItemInterface;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.effects.EffectModifierInterface;
import com.swiftlicious.hellblock.handlers.EventCarrier;
import com.swiftlicious.hellblock.handlers.EventCarrierInterface;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.utils.extras.PriorityFunction;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class GearConfigParser {

	private final String id;
	private final String material;
	private final List<PriorityFunction<BiConsumer<Item<ItemStack>, Context<Player>>>> tagConsumers = new ArrayList<>();
	private final List<Consumer<EventCarrier.Builder>> eventBuilderConsumers = new ArrayList<>();
	private final List<Consumer<EffectModifier.Builder>> effectBuilderConsumers = new ArrayList<>();

	public GearConfigParser(String id, Section section, Map<String, Node<ConfigParserFunction>> functionMap) {
		this.id = id;
		this.material = section.getString("material");
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
				case ITEM -> {
					final ItemParserFunction propertyFunction = (ItemParserFunction) function;
					final BiConsumer<Item<ItemStack>, Context<Player>> result = propertyFunction
							.accept(entry.getValue());
					tagConsumers.add(new PriorityFunction<>(propertyFunction.getPriority(), result));
					break;
				}
				case EVENT -> {
					final EventParserFunction eventParserFunction = (EventParserFunction) function;
					final Consumer<EventCarrier.Builder> consumerEvent = eventParserFunction.accept(entry.getValue());
					eventBuilderConsumers.add(consumerEvent);
					break;
				}
				case EFFECT_MODIFIER -> {
					final EffectModifierParserFunction effectModifierParserFunction = (EffectModifierParserFunction) function;
					final Consumer<EffectModifier.Builder> consumerEffect = effectModifierParserFunction
							.accept(entry.getValue());
					effectBuilderConsumers.add(consumerEffect);
					break;
				}
				default -> {
					// Handle other types or log a warning if an unexpected type is encountered
					break;
				}
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

	public EffectModifier getEffectModifier() {
		final EffectModifier.Builder builder = EffectModifierInterface.builder().id(id).type(MechanicType.EQUIPMENT);
		effectBuilderConsumers.forEach(consumer -> consumer.accept(builder));
		return builder.build();
	}

	public EventCarrier getEventCarrier() {
		final EventCarrier.Builder builder = EventCarrierInterface.builder().id(id).type(MechanicType.EQUIPMENT);
		eventBuilderConsumers.forEach(consumer -> consumer.accept(builder));
		return builder.build();
	}
}