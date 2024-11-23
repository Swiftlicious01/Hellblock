package com.swiftlicious.hellblock.config.parser;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.config.parser.function.ItemParserFunction;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.CustomItemInterface;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.PriorityFunction;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class SingleItemParser {

	private final String id;
	private final String material;
	private final MathValue<Player> amount;
	private final List<PriorityFunction<BiConsumer<Item<ItemStack>, Context<Player>>>> tagConsumers = new ArrayList<>();

	public SingleItemParser(String id, Section section, Map<String, Node<ConfigParserFunction>> functionMap) {
		this.id = id;
		if (section == null) {
			this.material = "AIR";
			this.amount = MathValue.plain(1);
			return;
		}
		this.amount = MathValue.auto(section.get("amount", 1), true);
		this.material = section.getString("material");
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
				if (function instanceof ItemParserFunction propertyFunction) {
					BiConsumer<Item<ItemStack>, Context<Player>> result = propertyFunction.accept(entry.getValue());
					tagConsumers.add(new PriorityFunction<>(propertyFunction.getPriority(), result));
				}
				continue;
			}
			if (entry.getValue() instanceof Section innerSection) {
				analyze(innerSection, node.getChildTree());
			}
		}
	}

	public CustomItem getItem() {
		return CustomItemInterface.builder().material(material).id(id).amount(amount).tagConsumers(tagConsumers)
				.build();
	}
}