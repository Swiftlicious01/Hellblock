package com.swiftlicious.hellblock.loot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.WeightUtils;
import com.swiftlicious.hellblock.utils.extras.ConditionalElement;
import com.swiftlicious.hellblock.utils.extras.Pair;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class LootManager implements LootManagerInterface {

	protected final HellblockPlugin instance;
	private final Map<String, LootInterface> lootMap = new HashMap<>();
	private final Map<String, List<String>> groupMembersMap = new HashMap<>();
	private final LinkedHashMap<String, ConditionalElement<List<Pair<String, BiFunction<Context<Player>, Double, Double>>>, Player>> lootConditions = new LinkedHashMap<>();

	public LootManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void unload() {
		this.lootMap.clear();
		this.groupMembersMap.clear();
		this.lootConditions.clear();
	}

	@Override
	public void load() {
		instance.debug("Loaded " + lootMap.size() + " loots");
		for (Map.Entry<String, List<String>> entry : groupMembersMap.entrySet()) {
			instance.debug("Group: {" + entry.getKey() + "} Members: " + entry.getValue());
		}
		File file = new File(instance.getDataFolder(), "loot-conditions.yml");
		if (!file.exists()) {
			instance.saveResource("loot-conditions.yml", false);
		}
		YamlDocument lootConditionsConfig = instance.getConfigManager().loadData(file);
		for (Map.Entry<String, Object> entry : lootConditionsConfig.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section section) {
				lootConditions.put(entry.getKey(), parseLootConditions(section));
			}
		}
	}

	private ConditionalElement<List<Pair<String, BiFunction<Context<Player>, Double, Double>>>, Player> parseLootConditions(
			Section section) {
		Section subSection = section.getSection("sub-groups");
		if (subSection == null) {
			return new ConditionalElement<>(
					instance.getConfigManager().parseWeightOperation(section.getStringList("list")), Map.of(),
					instance.getRequirementManager().parseRequirements(section.getSection("conditions"), false));
		} else {
			Map<String, ConditionalElement<List<Pair<String, BiFunction<Context<Player>, Double, Double>>>, Player>> subElements = new HashMap<>();
			for (Map.Entry<String, Object> entry : subSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					subElements.put(entry.getKey(), parseLootConditions(innerSection));
				}
			}
			return new ConditionalElement<>(
					instance.getConfigManager().parseWeightOperation(section.getStringList("list")), subElements,
					instance.getRequirementManager().parseRequirements(section.getSection("conditions"), false));
		}
	}

	@Override
	public boolean registerLoot(@NotNull LootInterface loot) {
		if (lootMap.containsKey(loot.id()))
			return false;
		this.lootMap.put(loot.id(), loot);
		for (String group : loot.lootGroup()) {
			addGroupMember(group, loot.id());
		}
		return true;
	}

	@Override
	public Collection<LootInterface> getRegisteredLoots() {
		return lootMap.values();
	}

	private void addGroupMember(String group, String member) {
		List<String> members = groupMembersMap.get(group);
		if (members == null) {
			members = new ArrayList<>(List.of(member));
			groupMembersMap.put(group, members);
		} else {
			members.add(member);
		}
	}

	@NotNull
	@Override
	public List<String> getGroupMembers(String key) {
		return Optional.ofNullable(groupMembersMap.get(key)).orElse(List.of());
	}

	@NotNull
	@Override
	public Optional<LootInterface> getLoot(String key) {
		return Optional.ofNullable(lootMap.get(key));
	}

	@Override
	public Map<String, Double> getWeightedLoots(Effect effect, Context<Player> context) {
		Map<String, Double> lootWeightMap = new HashMap<>();
		for (ConditionalElement<List<Pair<String, BiFunction<Context<Player>, Double, Double>>>, Player> conditionalElement : lootConditions
				.values()) {
			modifyWeightMap(lootWeightMap, context, conditionalElement);
		}
		for (Pair<String, BiFunction<Context<Player>, Double, Double>> pair : effect.weightOperations()) {
			Double previous = lootWeightMap.get(pair.left());
			if (previous != null) {
				lootWeightMap.put(pair.left(), pair.right().apply(context, previous));
			}
		}
		for (Pair<String, BiFunction<Context<Player>, Double, Double>> pair : effect.weightOperationsIgnored()) {
			double previous = lootWeightMap.getOrDefault(pair.left(), 0d);
			lootWeightMap.put(pair.left(), pair.right().apply(context, previous));
		}
		return lootWeightMap;
	}

	@Nullable
	@Override
	public LootInterface getNextLoot(Effect effect, Context<Player> context) {
		Map<String, Double> lootWeightMap = new HashMap<>();
		for (ConditionalElement<List<Pair<String, BiFunction<Context<Player>, Double, Double>>>, Player> conditionalElement : lootConditions
				.values()) {
			modifyWeightMap(lootWeightMap, context, conditionalElement);
		}
		for (Pair<String, BiFunction<Context<Player>, Double, Double>> pair : effect.weightOperations()) {
			Double previous = lootWeightMap.get(pair.left());
			if (previous != null) {
				lootWeightMap.put(pair.left(), pair.right().apply(context, previous));
			}
		}
		for (Pair<String, BiFunction<Context<Player>, Double, Double>> pair : effect.weightOperationsIgnored()) {
			double previous = lootWeightMap.getOrDefault(pair.left(), 0d);
			lootWeightMap.put(pair.left(), pair.right().apply(context, previous));
		}

		instance.debug(lootWeightMap);
		String lootID = WeightUtils.getRandom(lootWeightMap);
		return Optional.ofNullable(lootID)
				.map(id -> getLoot(lootID).orElseThrow(() -> new NullPointerException("Could not find loot " + lootID)))
				.orElse(null);
	}

	private void modifyWeightMap(Map<String, Double> weightMap, Context<Player> context,
			ConditionalElement<List<Pair<String, BiFunction<Context<Player>, Double, Double>>>, Player> conditionalElement) {
		if (conditionalElement == null)
			return;
		if (RequirementManagerInterface.isSatisfied(context, conditionalElement.getRequirements())) {
			for (Pair<String, BiFunction<Context<Player>, Double, Double>> modifierPair : conditionalElement
					.getElement()) {
				double previous = weightMap.getOrDefault(modifierPair.left(), 0d);
				weightMap.put(modifierPair.left(), modifierPair.right().apply(context, previous));
			}
			for (ConditionalElement<List<Pair<String, BiFunction<Context<Player>, Double, Double>>>, Player> sub : conditionalElement
					.getSubElements().values()) {
				modifyWeightMap(weightMap, context, sub);
			}
		}
	}
}