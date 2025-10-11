package com.swiftlicious.hellblock.config.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EffectModifierParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EventParserFunction;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.effects.EffectModifierInterface;
import com.swiftlicious.hellblock.handlers.EventCarrier;
import com.swiftlicious.hellblock.handlers.EventCarrierInterface;
import com.swiftlicious.hellblock.mechanics.MechanicType;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class EnchantConfigParser {

	private final String id;
	private final List<Consumer<EventCarrier.Builder>> eventBuilderConsumers = new ArrayList<>();
	private final List<Consumer<EffectModifier.Builder>> effectBuilderConsumers = new ArrayList<>();

	public EnchantConfigParser(String id, Section section, Map<String, Node<ConfigParserFunction>> functionMap) {
		this.id = id;
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

	public EventCarrier getEventCarrier() {
		final EventCarrier.Builder builder = EventCarrierInterface.builder().id(id).type(MechanicType.ENCHANT);
		eventBuilderConsumers.forEach(consumer -> consumer.accept(builder));
		return builder.build();
	}

	public EffectModifier getEffectModifier() {
		final EffectModifier.Builder builder = EffectModifierInterface.builder().id(id).type(MechanicType.ENCHANT);
		effectBuilderConsumers.forEach(consumer -> consumer.accept(builder));
		return builder.build();
	}
}