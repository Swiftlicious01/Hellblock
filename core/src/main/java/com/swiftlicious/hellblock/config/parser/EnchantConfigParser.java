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
		Map<String, Object> dataMap = section.getStringRouteMappedValues(false);
		for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
			String key = entry.getKey();
			Node<ConfigParserFunction> node = functionMap.get(key);
			if (node == null)
				continue;
			ConfigParserFunction function = node.nodeValue();
			if (function != null) {
				switch (function.type()) {
				case EVENT -> {
					EventParserFunction eventParserFunction = (EventParserFunction) function;
					Consumer<EventCarrier.Builder> consumer = eventParserFunction.accept(entry.getValue());
					eventBuilderConsumers.add(consumer);
				}
				case EFFECT_MODIFIER -> {
					EffectModifierParserFunction effectModifierParserFunction = (EffectModifierParserFunction) function;
					Consumer<EffectModifier.Builder> consumer = effectModifierParserFunction.accept(entry.getValue());
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
		EventCarrier.Builder builder = EventCarrierInterface.builder().id(id).type(MechanicType.ENCHANT);
		for (Consumer<EventCarrier.Builder> consumer : eventBuilderConsumers) {
			consumer.accept(builder);
		}
		return builder.build();
	}

	public EffectModifier getEffectModifier() {
		EffectModifier.Builder builder = EffectModifierInterface.builder().id(id).type(MechanicType.ENCHANT);
		for (Consumer<EffectModifier.Builder> consumer : effectBuilderConsumers) {
			consumer.accept(builder);
		}
		return builder.build();
	}
}