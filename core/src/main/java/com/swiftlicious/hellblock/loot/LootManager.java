package com.swiftlicious.hellblock.loot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.loot.operation.WeightOperation;
import com.swiftlicious.hellblock.utils.WeightUtils;
import com.swiftlicious.hellblock.utils.extras.ConditionalElement;
import com.swiftlicious.hellblock.utils.extras.Pair;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class LootManager implements LootManagerInterface {

	protected final HellblockPlugin instance;
	private final Map<String, Loot> lootMap = new HashMap<>();
	private final Map<String, List<String>> groupMembersMap = new HashMap<>();
	private final LinkedHashMap<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> lootConditions = new LinkedHashMap<>();

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
		groupMembersMap.entrySet()
				.forEach(entry -> instance.debug("Group: {" + entry.getKey() + "} Members: " + entry.getValue()));
		final File file = new File(instance.getDataFolder(), "loot-conditions.yml");
		if (!file.exists()) {
			instance.saveResource("loot-conditions.yml", false);
		}
		final YamlDocument lootConditionsConfig = instance.getConfigManager().loadData(file);
		lootConditionsConfig.getStringRouteMappedValues(false).entrySet().stream()
				.filter(entry -> entry.getValue() instanceof Section).forEach(entry -> {
					final Section section = (Section) entry.getValue();
					lootConditions.put(entry.getKey(), parseLootConditions(section));
				});
	}

	private ConditionalElement<List<Pair<String, WeightOperation>>, Player> parseLootConditions(Section section) {
		final Section subSection = section.getSection("sub-groups");
		if (subSection == null) {
			return new ConditionalElement<>(
					instance.getConfigManager().parseWeightOperation(section.getStringList("list"),
							(id) -> getLoot(id).isPresent(), this::getGroupMembers),
					Map.of(), instance.getRequirementManager(Player.class)
							.parseRequirements(section.getSection("conditions"), false));
		} else {
			final Map<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> subElements = new HashMap<>();
			subSection.getStringRouteMappedValues(false).entrySet().stream()
					.filter(entry -> entry.getValue() instanceof Section).forEach(entry -> {
						final Section innerSection = (Section) entry.getValue();
						subElements.put(entry.getKey(), parseLootConditions(innerSection));
					});
			return new ConditionalElement<>(
					instance.getConfigManager().parseWeightOperation(section.getStringList("list"),
							(id) -> getLoot(id).isPresent(), this::getGroupMembers),
					subElements, instance.getRequirementManager(Player.class)
							.parseRequirements(section.getSection("conditions"), false));
		}
	}

	@Override
	public boolean registerLoot(@NotNull Loot loot) {
		if (lootMap.containsKey(loot.id())) {
			return false;
		}
		this.lootMap.put(loot.id(), loot);
		for (String group : loot.lootGroup()) {
			addGroupMember(group, loot.id());
		}
		return true;
	}

	@Override
	public Collection<Loot> getRegisteredLoots() {
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
	public Optional<Loot> getLoot(String key) {
		return Optional.ofNullable(lootMap.get(key));
	}

	@Override
	public Map<String, Double> getWeightedLoots(Effect effect, Context<Player> context) {
		final Map<String, Double> lootWeightMap = new HashMap<>();
		lootConditions.values()
				.forEach(conditionalElement -> modifyWeightMap(lootWeightMap, context, conditionalElement));
		effect.weightOperations().forEach(pair -> {
			final Double previous = lootWeightMap.get(pair.left());
			if (previous != null) {
				lootWeightMap.put(pair.left(), pair.right().apply(context, previous, lootWeightMap));
			}
		});
		effect.weightOperationsIgnored().forEach(pair -> {
			final double previous = lootWeightMap.getOrDefault(pair.left(), 0d);
			lootWeightMap.put(pair.left(), pair.right().apply(context, previous, lootWeightMap));
		});
		return lootWeightMap;
	}

	@Nullable
	@Override
	public Loot getNextLoot(Effect effect, Context<Player> context) {
		final Map<String, Double> weightMap = new HashMap<>();
		lootConditions.values().forEach(conditionalElement -> modifyWeightMap(weightMap, context, conditionalElement));
		effect.weightOperations().forEach(pair -> {
			final Double previous = weightMap.get(pair.left());
			if (previous != null) {
				weightMap.put(pair.left(), pair.right().apply(context, previous, weightMap));
			}
		});
		effect.weightOperationsIgnored().forEach(pair -> {
			final double previous = weightMap.getOrDefault(pair.left(), 0d);
			weightMap.put(pair.left(), pair.right().apply(context, previous, weightMap));
		});

		instance.debug(weightMap::toString);
		final String lootID = WeightUtils.getRandom(weightMap);
		return Optional.ofNullable(lootID)
				.map(id -> getLoot(lootID).orElseThrow(() -> new NullPointerException("Could not find loot " + lootID)))
				.orElse(null);
	}

	private void modifyWeightMap(Map<String, Double> weightMap, Context<Player> context,
			ConditionalElement<List<Pair<String, WeightOperation>>, Player> conditionalElement) {
		if (conditionalElement == null) {
			return;
		}
		if (!RequirementManager.isSatisfied(context, conditionalElement.getRequirements())) {
			return;
		}
		conditionalElement.getElement().forEach(modifierPair -> {
			final double previous = weightMap.getOrDefault(modifierPair.left(), 0d);
			weightMap.put(modifierPair.left(), modifierPair.right().apply(context, previous, weightMap));
		});
		conditionalElement.getSubElements().values().forEach(sub -> modifyWeightMap(weightMap, context, sub));
	}
}