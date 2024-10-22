package com.swiftlicious.hellblock.loot;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.extras.Condition;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.WeightModifier;

public class LootManager implements LootManagerInterface {

	private final HellblockPlugin instance;
	// A map that associates loot IDs with their respective loot configurations.
	private final HashMap<String, Loot> lootMap;
	// A map that associates loot group IDs with lists of loot IDs.
	private final HashMap<String, List<String>> lootGroupMap;

	public LootManager(HellblockPlugin plugin) {
		instance = plugin;
		this.lootMap = new HashMap<>();
		this.lootGroupMap = new HashMap<>();
	}

	public void load() {
		this.loadLootsFromPluginFolder();
	}

	public void unload() {
		this.lootMap.clear();
		this.lootGroupMap.clear();
	}

	public void disable() {
		unload();
	}

	/**
	 * Loads loot configurations from the plugin's content folders. This method
	 * scans the "item," "entity," and "block" subfolders within the plugin's data
	 * folder and loads loot configurations from YAML files. If the subfolders or
	 * default loot files don't exist, it creates them.
	 */
	public void loadLootsFromPluginFolder() {
		Deque<File> fileDeque = new ArrayDeque<>();
		for (String type : List.of("item", "entity", "block")) {
			File typeFolder = new File(instance.getDataFolder() + File.separator + "contents" + File.separator + type);
			if (!typeFolder.exists()) {
				if (!typeFolder.mkdirs())
					return;
				instance.saveResource("contents" + File.separator + type + File.separator + "default.yml", false);
			}
			fileDeque.push(typeFolder);
			while (!fileDeque.isEmpty()) {
				File file = fileDeque.pop();
				File[] files = file.listFiles();
				if (files == null)
					continue;
				for (File subFile : files) {
					if (subFile.isDirectory()) {
						fileDeque.push(subFile);
					} else if (subFile.isFile() && subFile.getName().endsWith(".yml")) {
						loadSingleFile(subFile, type);
					}
				}
			}
		}
	}

	/**
	 * Retrieves a list of loot IDs associated with a loot group key.
	 *
	 * @param key The key of the loot group.
	 * @return A list of loot IDs belonging to the specified loot group, or null if
	 *         not found.
	 */
	@Nullable
	@Override
	public List<String> getLootGroup(String key) {
		return lootGroupMap.get(key);
	}

	/**
	 * Retrieves a loot configuration based on a provided loot key.
	 *
	 * @param key The key of the loot configuration.
	 * @return The Loot object associated with the specified loot key, or null if
	 *         not found.
	 */
	@Nullable
	@Override
	public Loot getLoot(String key) {
		return lootMap.get(key);
	}

	/**
	 * Retrieves a collection of all loot configuration keys.
	 *
	 * @return A collection of all loot configuration keys.
	 */
	@Override
	public Collection<String> getAllLootKeys() {
		return lootMap.keySet();
	}

	/**
	 * Retrieves a collection of all loot configurations.
	 *
	 * @return A collection of all loot configurations.
	 */
	@Override
	public Collection<Loot> getAllLoots() {
		return lootMap.values();
	}

	/**
	 * Retrieves loot configurations with weights based on a given condition.
	 *
	 * @param condition The condition used to filter loot configurations.
	 * @return A mapping of loot configuration keys to their associated weights.
	 */
	@Override
	public HashMap<String, Double> getLootWithWeight(Condition condition) {
		return ((RequirementManager) instance.getRequirementManager()).getLootWithWeight(condition);
	}

	/**
	 * Get a collection of possible loot keys based on a given condition.
	 *
	 * @param condition The condition to determine possible loot.
	 * @return A collection of loot keys.
	 */
	@Override
	public Collection<String> getPossibleLootKeys(Condition condition) {
		return ((RequirementManager) instance.getRequirementManager()).getLootWithWeight(condition).keySet();
	}

	/**
	 * Get a map of possible loot keys with their corresponding weights, considering
	 * fishing effect and condition.
	 *
	 * @param effect    The effect to apply weight modifiers.
	 * @param condition The condition to determine possible loot.
	 * @return A map of loot keys and their weights.
	 */
	@NotNull
	@Override
	public Map<String, Double> getPossibleLootKeysWithWeight(Effect effect, Condition condition) {
		Map<String, Double> lootWithWeight = ((RequirementManager) instance.getRequirementManager())
				.getLootWithWeight(condition);
		Player player = condition.getPlayer();
		for (Pair<String, WeightModifier> pair : effect.getWeightModifier()) {
			Double previous = lootWithWeight.get(pair.left());
			if (previous != null)
				lootWithWeight.put(pair.left(), pair.right().modify(player, previous));
		}
		for (Pair<String, WeightModifier> pair : effect.getWeightModifierIgnored()) {
			double previous = lootWithWeight.getOrDefault(pair.left(), 0d);
			lootWithWeight.put(pair.left(), pair.right().modify(player, previous));
		}
		return lootWithWeight;
	}

	/**
	 * Get the next loot item based on fishing effect and condition.
	 *
	 * @param effect    The effect to apply weight modifiers.
	 * @param condition The condition to determine possible loot.
	 * @return The next loot item, or null if it doesn't exist.
	 */
	@Override
	@Nullable
	public Loot getNextLoot(Effect effect, Condition condition) {
		String key = instance.getWeightUtils().getRandom(getPossibleLootKeysWithWeight(effect, condition));
		if (key == null) {
			return null;
		}
		Loot loot = getLoot(key);
		if (loot == null) {
			LogUtils.warn(String.format("Loot %s doesn't exist in any of the subfolders[item/entity/block].", key));
			return null;
		}
		return loot;
	}

	/**
	 * Loads loot configurations from a single YAML file and populates the lootMap
	 * and lootGroupMap.
	 *
	 * @param file      The YAML file containing loot configurations.
	 * @param namespace The namespace indicating the type of loot (e.g., "item,"
	 *                  "entity," "block").
	 */
	private void loadSingleFile(File file, String namespace) {
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		for (Map.Entry<String, Object> entry : yaml.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection section) {
				var loot = getSingleSectionItem(file.getPath(), section, namespace, entry.getKey());
				// Check for duplicate loot configurations and log an error if found.
				if (lootMap.containsKey(entry.getKey())) {
					LogUtils.severe(String.format("Duplicated loot found: %s.", entry.getKey()));
				} else {
					lootMap.put(entry.getKey(), loot);
				}
				String[] group = loot.getLootGroup();
				// If the loot configuration belongs to one or more groups, update lootGroupMap.
				if (group != null) {
					for (String g : group) {
						List<String> groupMembers = lootGroupMap.computeIfAbsent(g, k -> new ArrayList<>());
						groupMembers.add(loot.getID());
					}
				}
			}
		}
	}

	/**
	 * Creates a single loot configuration item from a ConfigurationSection.
	 *
	 * @param section   The ConfigurationSection containing loot configuration data.
	 * @param namespace The namespace indicating the type of loot (e.g., "item,"
	 *                  "entity," "block").
	 * @param key       The unique key identifying the loot configuration.
	 * @return A HBLoot object representing the loot configuration.
	 */
	private HBLoot getSingleSectionItem(String filePath, ConfigurationSection section, String namespace, String key) {
		return new HBLoot.Builder(key, LootType.valueOf(namespace.toUpperCase(Locale.ENGLISH))).filePath(filePath)
				.showInFinder(section.getBoolean("show-in-fishfinder", HBConfig.globalShowInFinder))
				.baseEffect(instance.getEffectManager().getBaseEffect(section.getConfigurationSection("effects")))
				.lootGroup(instance.getConfigUtils().stringListArgs(section.get("group")).toArray(new String[0]))
				.nick(section.getString("nick", section.getString("display.name", key)))
				.addActions(instance.getActionManager().getActionMap(section.getConfigurationSection("events")))
				.addTimesActions(instance.getActionManager()
						.getTimesActionMap(section.getConfigurationSection("events.success-times")))
				.build();
	}
}