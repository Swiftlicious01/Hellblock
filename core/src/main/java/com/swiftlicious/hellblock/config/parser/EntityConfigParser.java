package com.swiftlicious.hellblock.config.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.function.BaseEffectParserFunction;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EntityParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EventParserFunction;
import com.swiftlicious.hellblock.config.parser.function.LootParserFunction;
import com.swiftlicious.hellblock.creation.entity.EntityConfig;
import com.swiftlicious.hellblock.creation.entity.EntityConfigInterface;
import com.swiftlicious.hellblock.handlers.EventCarrier;
import com.swiftlicious.hellblock.handlers.EventCarrierInterface;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootInterface;
import com.swiftlicious.hellblock.loot.LootBaseEffect;
import com.swiftlicious.hellblock.loot.LootBaseEffectInterface;
import com.swiftlicious.hellblock.loot.LootType;
import com.swiftlicious.hellblock.mechanics.MechanicType;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class EntityConfigParser {

	private final String id;
	private final List<Consumer<EntityConfig.Builder>> entityBuilderConsumers = new ArrayList<>();
	private final List<Consumer<LootBaseEffect.Builder>> effectBuilderConsumers = new ArrayList<>();
	private final List<Consumer<Loot.Builder>> lootBuilderConsumers = new ArrayList<>();
	private final List<Consumer<EventCarrier.Builder>> eventBuilderConsumers = new ArrayList<>();

	public EntityConfigParser(String id, Section section, Map<String, Node<ConfigParserFunction>> functionMap) {
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
				case ENTITY -> {
					EntityParserFunction entityParserFunction = (EntityParserFunction) function;
					Consumer<EntityConfig.Builder> consumer = entityParserFunction.accept(entry.getValue());
					entityBuilderConsumers.add(consumer);
				}
				case BASE_EFFECT -> {
					BaseEffectParserFunction baseEffectParserFunction = (BaseEffectParserFunction) function;
					Consumer<LootBaseEffect.Builder> consumer = baseEffectParserFunction.accept(entry.getValue());
					effectBuilderConsumers.add(consumer);
				}
				case LOOT -> {
					LootParserFunction lootParserFunction = (LootParserFunction) function;
					Consumer<Loot.Builder> consumer = lootParserFunction.accept(entry.getValue());
					lootBuilderConsumers.add(consumer);
				}
				case EVENT -> {
					EventParserFunction eventParserFunction = (EventParserFunction) function;
					Consumer<EventCarrier.Builder> consumer = eventParserFunction.accept(entry.getValue());
					eventBuilderConsumers.add(consumer);
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

	public EntityConfig getEntity() {
		EntityConfig.Builder builder = EntityConfigInterface.builder().id(id);
		for (Consumer<EntityConfig.Builder> consumer : entityBuilderConsumers) {
			consumer.accept(builder);
		}
		return builder.build();
	}

	private LootBaseEffect getBaseEffect() {
		LootBaseEffect.Builder builder = LootBaseEffectInterface.builder();
		for (Consumer<LootBaseEffect.Builder> consumer : effectBuilderConsumers) {
			consumer.accept(builder);
		}
		return builder.build();
	}

	public Loot getLoot() {
		Loot.Builder builder = LootInterface.builder().id(id).type(LootType.ENTITY).lootBaseEffect(getBaseEffect());
		for (Consumer<Loot.Builder> consumer : lootBuilderConsumers) {
			consumer.accept(builder);
		}
		return builder.build();
	}

	public EventCarrier getEventCarrier() {
		EventCarrier.Builder builder = EventCarrierInterface.builder().id(id).type(MechanicType.LOOT);
		for (Consumer<EventCarrier.Builder> consumer : eventBuilderConsumers) {
			consumer.accept(builder);
		}
		return builder.build();
	}
}