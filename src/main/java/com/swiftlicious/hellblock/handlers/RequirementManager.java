package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.VaultHook;
import com.swiftlicious.hellblock.creation.addons.LevelInterface;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.utils.LogUtils;

import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Condition;
import com.swiftlicious.hellblock.utils.extras.ConditionalElement;
import com.swiftlicious.hellblock.utils.extras.EmptyRequirement;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.factory.RequirementFactory;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

public class RequirementManager implements RequirementManagerInterface {

	public Requirement[] fishingRequirements;
	private final HellblockPlugin instance;
	private final HashMap<String, RequirementFactory> requirementFactoryMap;
	private final LinkedHashMap<String, ConditionalElement> conditionalLootsMap;

	public RequirementManager(HellblockPlugin plugin) {
		instance = plugin;
		this.requirementFactoryMap = new HashMap<>();
		this.conditionalLootsMap = new LinkedHashMap<>();
		this.registerInbuiltRequirements();
	}

	public void load() {
		loadRequirementGroupFileConfig();
	}

	public void unload() {
		this.conditionalLootsMap.clear();
	}

	public void disable() {
		this.requirementFactoryMap.clear();
		this.conditionalLootsMap.clear();
	}

	/**
	 * Loads requirement group configuration data from various configuration files.
	 */
	public void loadRequirementGroupFileConfig() {
		// Load mechanic requirements from the main configuration file
		YamlConfiguration main = instance.getConfig("config.yml");
		fishingRequirements = getRequirements(main.getConfigurationSection("lava-fishing-options.fishing-requirements"), true);

		// Load conditional loot data from the loot conditions configuration file
		YamlConfiguration config = instance.getConfig("loot-conditions.yml");
		for (Map.Entry<String, Object> entry : config.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection section) {
				conditionalLootsMap.put(entry.getKey(), getConditionalElements(section));
			}
		}
	}

	/**
	 * Registers a custom requirement type with its corresponding factory.
	 *
	 * @param type               The type identifier of the requirement.
	 * @param requirementFactory The factory responsible for creating instances of
	 *                           the requirement.
	 * @return True if registration was successful, false if the type is already
	 *         registered.
	 */
	@Override
	public boolean registerRequirement(String type, RequirementFactory requirementFactory) {
		if (this.requirementFactoryMap.containsKey(type))
			return false;
		this.requirementFactoryMap.put(type, requirementFactory);
		return true;
	}

	/**
	 * Unregisters a custom requirement type.
	 *
	 * @param type The type identifier of the requirement to unregister.
	 * @return True if unregistration was successful, false if the type is not
	 *         registered.
	 */
	@Override
	public boolean unregisterRequirement(String type) {
		return this.requirementFactoryMap.remove(type) != null;
	}

	/**
	 * Retrieves a ConditionalElement from a given ConfigurationSection.
	 *
	 * @param section The ConfigurationSection containing the conditional element
	 *                data.
	 * @return A ConditionalElement instance representing the data in the section.
	 */
	private ConditionalElement getConditionalElements(ConfigurationSection section) {
		var sub = section.getConfigurationSection("sub-groups");
		if (sub == null) {
			return new ConditionalElement(getRequirements(section.getConfigurationSection("conditions"), false),
					instance.getConfigUtils().getModifiers(section.getStringList("list")), null);
		} else {
			HashMap<String, ConditionalElement> subElements = new HashMap<>();
			for (Map.Entry<String, Object> entry : sub.getValues(false).entrySet()) {
				if (entry.getValue() instanceof ConfigurationSection innerSection) {
					subElements.put(entry.getKey(), getConditionalElements(innerSection));
				}
			}
			return new ConditionalElement(getRequirements(section.getConfigurationSection("conditions"), false),
					instance.getConfigUtils().getModifiers(section.getStringList("list")), subElements);
		}
	}

	private void registerInbuiltRequirements() {
		this.registerTimeRequirement();
		this.registerYRequirement();
		this.registerContainRequirement();
		this.registerStartWithRequirement();
		this.registerEndWithRequirement();
		this.registerEqualsRequirement();
		this.registerDateRequirement();
		this.registerPluginLevelRequirement();
		this.registerPermissionRequirement();
		this.registerWeatherRequirement();
		this.registerLavaFishingRequirement();
		this.registerRodRequirement();
		this.registerBaitRequirement();
		this.registerGreaterThanRequirement();
		this.registerAndRequirement();
		this.registerOrRequirement();
		this.registerLevelRequirement();
		this.registerRandomRequirement();
		this.registerGroupRequirement();
		this.registerLootRequirement();
		this.registerLessThanRequirement();
		this.registerNumberEqualRequirement();
		this.registerRegexRequirement();
		this.registerItemInHandRequirement();
		this.registerMoneyRequirement();
		this.registerHookRequirement();
		this.registerListRequirement();
		this.registerPotionEffectRequirement();
		this.registerSizeRequirement();
		this.registerLootTypeRequirement();
		this.registerInListRequirement();
	}

	public HashMap<String, Double> getLootWithWeight(Condition condition) {
		return getString2DoubleMap(condition, conditionalLootsMap);
	}

	/**
	 * Retrieves a mapping of strings to doubles based on conditional elements and a
	 * player's condition.
	 *
	 * @param condition           The player's condition.
	 * @param conditionalGamesMap The map of conditional elements representing
	 *                            loots/games.
	 * @return A HashMap with strings as keys and doubles as values representing
	 *         loot/game weights.
	 */
	@NotNull
	private HashMap<String, Double> getString2DoubleMap(Condition condition,
			LinkedHashMap<String, ConditionalElement> conditionalGamesMap) {
		HashMap<String, Double> lootWeightMap = new HashMap<>();
		Queue<HashMap<String, ConditionalElement>> lootQueue = new LinkedList<>();
		lootQueue.add(conditionalGamesMap);
		Player player = condition.getPlayer();
		while (!lootQueue.isEmpty()) {
			HashMap<String, ConditionalElement> currentLootMap = lootQueue.poll();
			for (ConditionalElement loots : currentLootMap.values()) {
				if (RequirementManagerInterface.isRequirementMet(condition, loots.getRequirements())) {
					loots.combine(player, lootWeightMap);
					if (loots.getSubElements() != null) {
						lootQueue.add(loots.getSubElements());
					}
				}
			}
		}
		return lootWeightMap;
	}

	/**
	 * Retrieves an array of requirements based on a configuration section.
	 *
	 * @param section  The configuration section containing requirement definitions.
	 * @param advanced A flag indicating whether to use advanced requirements.
	 * @return An array of Requirement objects based on the configuration section
	 */
	@NotNull
	@Override
	public Requirement[] getRequirements(ConfigurationSection section, boolean advanced) {
		List<Requirement> requirements = new ArrayList<>();
		if (section == null) {
			return requirements.toArray(new Requirement[0]);
		}
		for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
			String typeOrName = entry.getKey();
			if (hasRequirement(typeOrName)) {
				requirements.add(getRequirement(typeOrName, entry.getValue()));
			} else {
				requirements.add(getRequirement(section.getConfigurationSection(typeOrName), advanced));
			}
		}
		return requirements.toArray(new Requirement[0]);
	}

	@Override
	public boolean hasRequirement(String type) {
		return requirementFactoryMap.containsKey(type);
	}

	/**
	 * Retrieves a Requirement object based on a configuration section and advanced
	 * flag.
	 *
	 * @param section  The configuration section containing requirement definitions.
	 * @param advanced A flag indicating whether to use advanced requirements.
	 * @return A Requirement object based on the configuration section, or an
	 *         EmptyRequirement if the section is null or invalid.
	 */
	@NotNull
	@Override
	public Requirement getRequirement(ConfigurationSection section, boolean advanced) {
		if (section == null)
			return EmptyRequirement.EMPTY;
		List<Action> actionList = null;
		if (advanced) {
			actionList = new ArrayList<>();
			if (section.contains("not-met-actions")) {
				for (Map.Entry<String, Object> entry : Objects
						.requireNonNull(section.getConfigurationSection("not-met-actions")).getValues(false)
						.entrySet()) {
					if (entry.getValue() instanceof MemorySection inner) {
						actionList.add(instance.getActionManager().getAction(inner));
					}
				}
			}
			if (actionList.size() == 0)
				actionList = null;
		}
		String type = section.getString("type");
		if (type == null) {
			LogUtils.warn(String.format("No requirement type found at %s", section.getCurrentPath()));
			return EmptyRequirement.EMPTY;
		}
		var builder = getRequirementFactory(type);
		if (builder == null) {
			return EmptyRequirement.EMPTY;
		}
		return builder.build(section.get("value"), actionList, advanced);
	}

	/**
	 * Gets a requirement based on the provided key and value. If a valid
	 * RequirementFactory is found for the key, it is used to create the
	 * requirement. If no factory is found, a warning is logged, and an empty
	 * requirement instance is returned.
	 *
	 * @param type  The key representing the requirement type.
	 * @param value The value associated with the requirement.
	 * @return A Requirement instance based on the key and value, or an empty
	 *         requirement if not found.
	 */
	@Override
	@NotNull
	public Requirement getRequirement(String type, Object value) {
		RequirementFactory factory = getRequirementFactory(type);
		if (factory == null) {
			LogUtils.warn(String.format("Requirement type: %s doesn't exist.", type));
			return EmptyRequirement.EMPTY;
		}
		return factory.build(value);
	}

	/**
	 * Retrieves a RequirementFactory based on the specified requirement type.
	 *
	 * @param type The requirement type for which to retrieve a factory.
	 * @return A RequirementFactory for the specified type, or null if no factory is
	 *         found.
	 */
	@Override
	@Nullable
	public RequirementFactory getRequirementFactory(String type) {
		return requirementFactoryMap.get(type);
	}

	private void registerTimeRequirement() {
		registerRequirement("time", (args, actions, advanced) -> {
			List<Pair<Integer, Integer>> timePairs = instance.getConfigUtils().stringListArgs(args).stream()
					.map(it -> instance.getConfigUtils().splitStringIntegerArgs(it, "~")).toList();
			return condition -> {
				long time = condition.getLocation().getWorld().getTime();
				for (Pair<Integer, Integer> pair : timePairs)
					if (time >= pair.left() && time <= pair.right())
						return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerGroupRequirement() {
		registerRequirement("group", (args, actions, advanced) -> {
			HashSet<String> arg = new HashSet<>(instance.getConfigUtils().stringListArgs(args));
			return condition -> {
				String lootID = condition.getArg("{loot}");
				Loot loot = instance.getLootManager().getLoot(lootID);
				String[] groups = loot.getLootGroup();
				if (groups != null) {
					for (String g : groups) {
						if (arg.contains(g)) {
							return true;
						}
					}
				}
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("!group", (args, actions, advanced) -> {
			HashSet<String> arg = new HashSet<>(instance.getConfigUtils().stringListArgs(args));
			return condition -> {
				String lootID = condition.getArg("{loot}");
				Loot loot = instance.getLootManager().getLoot(lootID);
				String[] groups = loot.getLootGroup();
				if (groups == null) {
					return true;
				}
				outer: {
					for (String g : groups) {
						if (arg.contains(g)) {
							break outer;
						}
					}
					return true;
				}
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerLootRequirement() {
		registerRequirement("loot", (args, actions, advanced) -> {
			HashSet<String> arg = new HashSet<>(instance.getConfigUtils().stringListArgs(args));
			return condition -> {
				String lootID = condition.getArg("{loot}");
				if (arg.contains(lootID))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("!loot", (args, actions, advanced) -> {
			HashSet<String> arg = new HashSet<>(instance.getConfigUtils().stringListArgs(args));
			return condition -> {
				String lootID = condition.getArg("{loot}");
				if (!arg.contains(lootID))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerYRequirement() {
		registerRequirement("ypos", (args, actions, advanced) -> {
			List<Pair<Integer, Integer>> timePairs = instance.getConfigUtils().stringListArgs(args).stream()
					.map(it -> instance.getConfigUtils().splitStringIntegerArgs(it, "~")).toList();
			return condition -> {
				int y = condition.getLocation().getBlockY();
				for (Pair<Integer, Integer> pair : timePairs)
					if (y >= pair.left() && y <= pair.right())
						return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerOrRequirement() {
		registerRequirement("||", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				Requirement[] requirements = getRequirements(section, advanced);
				return condition -> {
					for (Requirement requirement : requirements) {
						if (requirement.isConditionMet(condition)) {
							return true;
						}
					}
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at || requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerAndRequirement() {
		registerRequirement("&&", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				Requirement[] requirements = getRequirements(section, advanced);
				return condition -> {
					outer: {
						for (Requirement requirement : requirements) {
							if (!requirement.isConditionMet(condition)) {
								break outer;
							}
						}
						return true;
					}
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at && requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerLavaFishingRequirement() {
		registerRequirement("lava-fishing", (args, actions, advanced) -> {
			boolean inLava = (boolean) args;
			return condition -> {
				String current = condition.getArgs().getOrDefault("{lava}", "false");
				if (current.equals(String.valueOf(inLava)))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerLevelRequirement() {
		registerRequirement("level", (args, actions, advanced) -> {
			int level = (int) args;
			return condition -> {
				int current = condition.getPlayer().getLevel();
				if (current >= level)
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerMoneyRequirement() {
		registerRequirement("money", (args, actions, advanced) -> {
			double money = instance.getConfigUtils().getDoubleValue(args);
			return condition -> {
				double current = VaultHook.getEconomy().getBalance(condition.getPlayer());
				if (current >= money)
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerRandomRequirement() {
		registerRequirement("random", (args, actions, advanced) -> {
			double random = instance.getConfigUtils().getDoubleValue(args);
			return condition -> {
				if (Math.random() < random)
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerWeatherRequirement() {
		registerRequirement("weather", (args, actions, advanced) -> {
			List<String> weathers = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String currentWeather;
				if (instance.getLavaRainHandler().getLavaRainTask() != null
						&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining())
					currentWeather = "lavarain";
				else
					currentWeather = "clear";
				for (String weather : weathers)
					if (weather.equalsIgnoreCase(currentWeather))
						return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerDateRequirement() {
		registerRequirement("date", (args, actions, advanced) -> {
			HashSet<String> dates = new HashSet<>(instance.getConfigUtils().stringListArgs(args));
			return condition -> {
				Calendar calendar = Calendar.getInstance();
				String current = (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DATE);
				if (dates.contains(current))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerPermissionRequirement() {
		registerRequirement("permission", (args, actions, advanced) -> {
			List<String> perms = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				for (String perm : perms)
					if (condition.getPlayer().hasPermission(perm))
						return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("!permission", (args, actions, advanced) -> {
			List<String> perms = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				for (String perm : perms)
					if (condition.getPlayer().hasPermission(perm)) {
						if (advanced)
							triggerActions(actions, condition);
						return false;
					}
				return true;
			};
		});
	}

	private void registerGreaterThanRequirement() {
		registerRequirement(">=", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (Double.parseDouble(p1) >= Double.parseDouble(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at >= requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
		registerRequirement(">", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (Double.parseDouble(p1) > Double.parseDouble(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at > requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerRegexRequirement() {
		registerRequirement("regex", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("papi", "");
				String v2 = section.getString("regex", "");
				return condition -> {
					if (instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1).matches(v2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at regex requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerNumberEqualRequirement() {
		registerRequirement("==", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (Double.parseDouble(p1) == Double.parseDouble(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at !startsWith requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
		registerRequirement("!=", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (Double.parseDouble(p1) != Double.parseDouble(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at !startsWith requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerLessThanRequirement() {
		registerRequirement("<", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (Double.parseDouble(p1) < Double.parseDouble(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at < requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
		registerRequirement("<=", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (Double.parseDouble(p1) <= Double.parseDouble(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at <= requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerStartWithRequirement() {
		registerRequirement("startsWith", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (p1.startsWith(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at startsWith requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
		registerRequirement("!startsWith", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (!p1.startsWith(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at !startsWith requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerEndWithRequirement() {
		registerRequirement("endsWith", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (p1.endsWith(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at endsWith requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
		registerRequirement("!endsWith", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (!p1.endsWith(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at !endsWith requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerContainRequirement() {
		registerRequirement("contains", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (p1.contains(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at contains requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
		registerRequirement("!contains", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (!p1.contains(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at !contains requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerInListRequirement() {
		registerRequirement("in-list", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String papi = section.getString("papi", "");
				List<String> values = instance.getConfigUtils().stringListArgs(section.get("values"));
				return condition -> {
					String p1 = papi.startsWith("%")
							? instance.getParseUtils().setPlaceholders(condition.getPlayer(), papi)
							: papi;
					if (values.contains(p1))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at in-list requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
		registerRequirement("!in-list", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String papi = section.getString("papi", "");
				List<String> values = instance.getConfigUtils().stringListArgs(section.get("values"));
				return condition -> {
					String p1 = papi.startsWith("%")
							? instance.getParseUtils().setPlaceholders(condition.getPlayer(), papi)
							: papi;
					if (!values.contains(p1))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at in-list requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerEqualsRequirement() {
		registerRequirement("equals", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (p1.equals(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at equals requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
		registerRequirement("!equals", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String v1 = section.getString("value1", "");
				String v2 = section.getString("value2", "");
				return condition -> {
					String p1 = v1.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v1)
							: v1;
					String p2 = v2.startsWith("%") ? instance.getParseUtils().setPlaceholders(condition.getPlayer(), v2)
							: v2;
					if (!p1.equals(p2))
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at !equals requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerRodRequirement() {
		registerRequirement("rod", (args, actions, advanced) -> {
			List<String> rods = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String id = condition.getArg("{rod}");
				if (rods.contains(id))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("!rod", (args, actions, advanced) -> {
			List<String> rods = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String id = condition.getArg("{rod}");
				if (!rods.contains(id))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerItemInHandRequirement() {
		registerRequirement("item-in-hand", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				boolean mainOrOff = section.getString("hand", "main").equalsIgnoreCase("main");
				int amount = section.getInt("amount", 1);
				List<String> items = instance.getConfigUtils().stringListArgs(section.get("item"));
				return condition -> {
					ItemStack itemStack = mainOrOff ? condition.getPlayer().getInventory().getItemInMainHand()
							: condition.getPlayer().getInventory().getItemInOffHand();
					String id = instance.getItemManager().getAnyPluginItemID(itemStack);
					if (items.contains(id) && itemStack.getAmount() >= amount)
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at item-in-hand requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerBaitRequirement() {
		registerRequirement("bait", (args, actions, advanced) -> {
			List<String> baits = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String id = condition.getArg("{bait}");
				if (baits.contains(id))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("!bait", (args, actions, advanced) -> {
			List<String> baits = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String id = condition.getArg("{bait}");
				if (!baits.contains(id))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("has-bait", (args, actions, advanced) -> {
			boolean has = (boolean) args;
			return condition -> {
				String id = condition.getArg("{bait}");
				if (id != null && has)
					return true;
				if (id == null && !has)
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerSizeRequirement() {
		registerRequirement("has-size", (args, actions, advanced) -> {
			boolean has = (boolean) args;
			return condition -> {
				String size = condition.getArg("{SIZE}");
				if (size != null && has)
					return true;
				if (size == null && !has)
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerLootTypeRequirement() {
		registerRequirement("loot-type", (args, actions, advanced) -> {
			List<String> types = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String loot = condition.getArg("{loot}");
				Loot lootInstance = instance.getLootManager().getLoot(loot);
				if (lootInstance != null) {
					if (types.contains(lootInstance.getType().name().toLowerCase(Locale.ENGLISH)))
						return true;
				}
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("!loot-type", (args, actions, advanced) -> {
			List<String> types = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String loot = condition.getArg("{loot}");
				Loot lootInstance = instance.getLootManager().getLoot(loot);
				if (lootInstance != null) {
					if (!types.contains(lootInstance.getType().name().toLowerCase(Locale.ENGLISH)))
						return true;
				}
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerHookRequirement() {
		registerRequirement("hook", (args, actions, advanced) -> {
			List<String> hooks = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String id = condition.getArg("{hook}");
				if (hooks.contains(id))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("!hook", (args, actions, advanced) -> {
			List<String> hooks = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				String id = condition.getArg("{hook}");
				if (!hooks.contains(id))
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
		registerRequirement("has-hook", (args, actions, advanced) -> {
			boolean has = (boolean) args;
			return condition -> {
				String id = condition.getArg("{hook}");
				if (id != null && has)
					return true;
				if (id == null && !has)
					return true;
				if (advanced)
					triggerActions(actions, condition);
				return false;
			};
		});
	}

	private void registerListRequirement() {
		registerRequirement("list", (args, actions, advanced) -> {
			LogUtils.severe("It seems that you made a mistake where you put \"list\" into \"conditions\" section.");
			List<String> list = instance.getConfigUtils().stringListArgs(args);
			LogUtils.warn("list:");
			for (String e : list) {
				LogUtils.warn(" - " + e);
			}
			return EmptyRequirement.EMPTY;
		});
	}

	private void registerPluginLevelRequirement() {
		registerRequirement("plugin-level", (args, actions, advanced) -> {
			if (args instanceof ConfigurationSection section) {
				String pluginName = section.getString("plugin");
				int level = section.getInt("level");
				String target = section.getString("target");
				return condition -> {
					LevelInterface levelInterface = instance.getIntegrationManager().getLevelPlugin(pluginName);
					if (levelInterface == null) {
						LogUtils.warn(String.format(
								"Plugin (%s's) level is not compatible. Please double check if it's a problem caused by pronunciation.",
								pluginName));
						return true;
					}
					if (levelInterface.getLevel(condition.getPlayer(), target) >= level)
						return true;
					if (advanced)
						triggerActions(actions, condition);
					return false;
				};
			} else {
				LogUtils.warn("Wrong value format found at plugin-level requirement.");
				return EmptyRequirement.EMPTY;
			}
		});
	}

	private void registerPotionEffectRequirement() {
		registerRequirement("potion-effect", (args, actions, advanced) -> {
			String potions = (String) args;
			String[] split = potions.split("(<=|>=|<|>|==)", 2);
			// Fetch the potion effect registry from the registry access
			final Registry<PotionType> potionRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.POTION);
			List<PotionEffect> effectTypes = potionRegistry.getOrThrow(NamespacedKey.fromString(split[0].toLowerCase()))
					.getPotionEffects();
			if (effectTypes.isEmpty()) {
				LogUtils.warn(String.format("Potion effects don't exist: %s", split[0]));
				return EmptyRequirement.EMPTY;
			}
			int required = Integer.parseInt(split[1]);
			String operator = potions.substring(split[0].length(), potions.length() - split[1].length());
			return condition -> {
				int level = -1;
				for (PotionEffect potionEffectType : effectTypes) {
					PotionEffect potionEffect = condition.getPlayer().getPotionEffect(potionEffectType.getType());
					if (potionEffect != null) {
						level = potionEffect.getAmplifier();
					}
					boolean result = false;
					switch (operator) {
					case ">=" -> {
						if (level >= required)
							result = true;
					}
					case ">" -> {
						if (level > required)
							result = true;
					}
					case "==" -> {
						if (level == required)
							result = true;
					}
					case "!=" -> {
						if (level != required)
							result = true;
					}
					case "<=" -> {
						if (level <= required)
							result = true;
					}
					case "<" -> {
						if (level < required)
							result = true;
					}
					}
					if (result) {
						return true;
					}
					if (advanced)
						triggerActions(actions, condition);
					return false;
				}
				return false;
			};
		});
	}

	/**
	 * Triggers a list of actions with the given condition. If the list of actions
	 * is not null, each action in the list is triggered.
	 *
	 * @param actions   The list of actions to trigger.
	 * @param condition The condition associated with the actions.
	 */
	private void triggerActions(List<Action> actions, Condition condition) {
		if (actions != null)
			for (Action action : actions)
				action.trigger(condition);
	}

}
