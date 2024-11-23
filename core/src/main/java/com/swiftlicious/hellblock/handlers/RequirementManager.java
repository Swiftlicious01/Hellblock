package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.VaultHook;
import com.swiftlicious.hellblock.creation.addons.level.LevelerProvider;
import com.swiftlicious.hellblock.effects.EffectProperties;
import com.swiftlicious.hellblock.loot.LootInterface;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.utils.ListUtils;

import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.factory.RequirementFactory;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import static java.util.Objects.requireNonNull;

public class RequirementManager implements RequirementManagerInterface<Player> {

	protected final HellblockPlugin instance;
	private final Map<String, RequirementFactory<Player>> requirementFactoryMap = new HashMap<>();

	public RequirementManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.registerBuiltInRequirements();
	}

	@Override
	public void disable() {
		this.requirementFactoryMap.clear();
	}

	@Override
	public boolean registerRequirement(@NotNull RequirementFactory<Player> requirementFactory,
			@NotNull String... types) {
		for (String type : types) {
			if (this.requirementFactoryMap.containsKey(type))
				return false;
		}
		for (String type : types) {
			this.requirementFactoryMap.put(type, requirementFactory);
		}
		return true;
	}

	@Override
	public boolean unregisterRequirement(@NotNull String type) {
		return this.requirementFactoryMap.remove(type) != null;
	}

	@Nullable
	@Override
	public RequirementFactory<Player> getRequirementFactory(@NotNull String type) {
		return requirementFactoryMap.get(type);
	}

	@Override
	public boolean hasRequirement(@NotNull String type) {
		return requirementFactoryMap.containsKey(type);
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public Requirement<Player>[] parseRequirements(Section section, boolean runActions) {
		List<Requirement<Player>> requirements = new ArrayList<>();
		if (section != null)
			for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
				String typeOrName = entry.getKey();
				if (hasRequirement(typeOrName)) {
					requirements.add(parseRequirement(typeOrName, entry.getValue()));
				} else {
					requirements.add(parseRequirement(section.getSection(typeOrName), runActions));
				}
			}
		return requirements.toArray(new Requirement[0]);
	}

	@NotNull
	@Override
	public Requirement<Player> parseRequirement(@NotNull Section section, boolean runActions) {
		List<Action<Player>> actionList = new ArrayList<>();
		if (runActions && section.contains("not-met-actions")) {
			actionList.addAll(List.of(
					instance.getActionManager().parseActions(requireNonNull(section.getSection("not-met-actions")))));
		}
		String type = section.getString("type");
		if (type == null) {
			instance.getPluginLogger().warn("No requirement type found at " + section.getRouteAsString());
			return Requirement.empty();
		}
		var factory = getRequirementFactory(type);
		if (factory == null) {
			instance.getPluginLogger().warn("Requirement type: " + type + " not exists");
			return Requirement.empty();
		}
		return factory.process(section.get("value"), actionList, runActions);
	}

	@NotNull
	@Override
	public Requirement<Player> parseRequirement(@NotNull String type, @NotNull Object value) {
		RequirementFactory<Player> factory = getRequirementFactory(type);
		if (factory == null) {
			instance.getPluginLogger().warn("Requirement type: " + type + " doesn't exist.");
			return Requirement.empty();
		}
		return factory.process(value);
	}

	private void registerBuiltInRequirements() {
		this.registerTimeRequirement();
		this.registerYRequirement();
		this.registerInLavaRequirement();
		this.registerAndRequirement();
		this.registerOrRequirement();
		this.registerGroupRequirement();
		this.registerRodRequirement();
		this.registerPAPIRequirement();
		this.registerPermissionRequirement();
		this.registerCoolDownRequirement();
		this.registerDateRequirement();
		this.registerWeatherRequirement();
		this.registerMoneyRequirement();
		this.registerLevelRequirement();
		this.registerRandomRequirement();
		this.registerBiomeRequirement();
		this.registerBaitRequirement();
		this.registerLootRequirement();
		this.registerSizeRequirement();
		this.registerLootTypeRequirement();
		this.registerHasStatsRequirement();
		this.registerHookRequirement();
		this.registerListRequirement();
		this.registerPluginLevelRequirement();
		this.registerItemInHandRequirement();
		this.registerImpossibleRequirement();
		this.registerPotionEffectRequirement();
		this.registerSneakRequirement();
		this.registerGameModeRequirement();
		this.registerEquipmentRequirement();
	}

	private void registerImpossibleRequirement() {
		registerRequirement(((args, actions, runActions) -> context -> {
			if (runActions)
				ActionManagerInterface.trigger(context, actions);
			return false;
		}), "impossible");
	}

	private void registerEquipmentRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				EquipmentSlot slot = EquipmentSlot
						.valueOf(section.getString("slot", "HEAD").toUpperCase(Locale.ENGLISH));
				List<String> items = ListUtils.toList(section.get("item"));
				return context -> {
					ItemStack itemStack = context.holder().getInventory().getItem(slot);
					String id = instance.getItemManager().getItemID(itemStack);
					if (items.contains(id))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at equipment requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "equipment");
	}

	private void registerItemInHandRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				boolean mainOrOff = section.getString("hand", "main").equalsIgnoreCase("main");
				int amount = section.getInt("amount", 1);
				List<String> items = ListUtils.toList(section.get("item"));
				boolean any = items.contains("any") || items.contains("*");
				return context -> {
					ItemStack itemStack = mainOrOff ? context.holder().getInventory().getItemInMainHand()
							: context.holder().getInventory().getItemInOffHand();
					String id = instance.getItemManager().getItemID(itemStack);
					if ((items.contains(id) || any) && itemStack.getAmount() >= amount)
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at item-in-hand requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "item-in-hand");
	}

	private void registerPluginLevelRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				String pluginName = section.getString("plugin");
				int level = section.getInt("level");
				String target = section.getString("target");
				return context -> {
					LevelerProvider levelerProvider = instance.getIntegrationManager().getLevelerProvider(pluginName);
					if (levelerProvider == null) {
						instance.getPluginLogger().warn("Plugin (" + pluginName
								+ "'s) level is not compatible. Please double check if it's a problem caused by pronunciation.");
						return true;
					}
					if (levelerProvider.getLevel(context.holder(), target) >= level)
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at plugin-level requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "plugin-level");
	}

	private void registerTimeRequirement() {
		registerRequirement((args, actions, runActions) -> {
			List<String> list = ListUtils.toList(args);
			List<Pair<Integer, Integer>> timePairs = list.stream().map(line -> {
				String[] split = line.split("~");
				return new Pair<>(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
			}).toList();
			return context -> {
				Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				long time = location.getWorld().getTime();
				for (Pair<Integer, Integer> pair : timePairs)
					if (time >= pair.left() && time <= pair.right())
						return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "time");
	}

	private void registerYRequirement() {
		registerRequirement((args, actions, runActions) -> {
			List<String> list = ListUtils.toList(args);
			List<Pair<Double, Double>> posPairs = list.stream().map(line -> {
				String[] split = line.split("~");
				return new Pair<>(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
			}).toList();
			return context -> {
				Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				double y = location.getY();
				for (Pair<Double, Double> pair : posPairs)
					if (y >= pair.left() && y <= pair.right())
						return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "ypos");
	}

	private void registerOrRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				Requirement<Player>[] requirements = parseRequirements(section, runActions);
				return context -> {
					for (Requirement<Player> requirement : requirements)
						if (requirement.isSatisfied(context))
							return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at || requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "||");
	}

	private void registerAndRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				Requirement<Player>[] requirements = parseRequirements(section, runActions);
				return context -> {
					outer: {
						for (Requirement<Player> requirement : requirements)
							if (!requirement.isSatisfied(context))
								break outer;
						return true;
					}
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at && requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "&&");
	}

	private void registerInLavaRequirement() {
		// Deprecated requirement type
		registerRequirement((args, actions, runActions) -> {
			boolean inLava = (boolean) args;
			if (!inLava) {
				// in water
				return context -> {
					boolean in_water = Optional.ofNullable(context.arg(ContextKeys.SURROUNDING)).orElse("")
							.equals(EffectProperties.WATER_FISHING.key());
					if (in_water)
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			}
			// in lava
			return context -> {
				boolean in_lava = Optional.ofNullable(context.arg(ContextKeys.SURROUNDING)).orElse("")
						.equals(EffectProperties.LAVA_FISHING.key());
				if (in_lava)
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "lava-fishing");
		registerRequirement((args, actions, runActions) -> {
			boolean inLava = (boolean) args;
			return context -> {
				boolean in_lava = Optional.ofNullable(context.arg(ContextKeys.SURROUNDING)).orElse("")
						.equals(EffectProperties.LAVA_FISHING.key());
				if (in_lava == inLava)
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "in-lava");
	}

	private void registerRodRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> rods = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.ROD);
				if (rods.contains(id))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "rod");
		registerRequirement((args, actions, runActions) -> {
			Set<String> rods = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.ROD);
				if (!rods.contains(id))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "!rod");
	}

	private void registerGroupRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> groups = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String lootID = context.arg(ContextKeys.ID);
				Optional<LootInterface> loot = instance.getLootManager().getLoot(lootID);
				if (loot.isEmpty())
					return false;
				String[] group = loot.get().lootGroup();
				if (group != null)
					for (String x : group)
						if (groups.contains(x))
							return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "group");
		registerRequirement((args, actions, runActions) -> {
			Set<String> groups = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String lootID = context.arg(ContextKeys.ID);
				Optional<LootInterface> loot = instance.getLootManager().getLoot(lootID);
				if (loot.isEmpty())
					return false;
				String[] group = loot.get().lootGroup();
				if (group == null)
					return true;
				outer: {
					for (String x : group)
						if (groups.contains(x))
							break outer;
					return true;
				}
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "!group");
	}

	private void registerLootRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> arg = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String lootID = context.arg(ContextKeys.ID);
				if (arg.contains(lootID))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "loot");
		registerRequirement((args, actions, runActions) -> {
			Set<String> arg = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String lootID = context.arg(ContextKeys.ID);
				if (!arg.contains(lootID))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "!loot");
	}

	private void registerHookRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> hooks = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.HOOK);
				if (hooks.contains(id))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "hook");
		registerRequirement((args, actions, runActions) -> {
			Set<String> hooks = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.HOOK);
				if (!hooks.contains(id))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "!hook");
		registerRequirement((args, actions, runActions) -> {
			boolean has = (boolean) args;
			return context -> {
				String id = context.arg(ContextKeys.HOOK);
				if (id != null && has)
					return true;
				if (id == null && !has)
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "has-hook");
	}

	private void registerBaitRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> arg = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.BAIT);
				if (arg.contains(id))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "bait");
		registerRequirement((args, actions, runActions) -> {
			Set<String> arg = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.BAIT);
				if (!arg.contains(id))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "!bait");
		registerRequirement((args, actions, runActions) -> {
			boolean has = (boolean) args;
			return context -> {
				String id = context.arg(ContextKeys.BAIT);
				if (id != null && has)
					return true;
				if (id == null && !has)
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "has-bait");
	}

	private void registerSizeRequirement() {
		registerRequirement((args, actions, runActions) -> {
			boolean has = (boolean) args;
			return context -> {
				float size = Optional.ofNullable(context.arg(ContextKeys.SIZE)).orElse(-1f);
				if (size != -1 && has)
					return true;
				if (size == -1 && !has)
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "has-size");
	}

	private void registerHasStatsRequirement() {
		registerRequirement((args, actions, runActions) -> {
			boolean has = (boolean) args;
			return context -> {
				String loot = context.arg(ContextKeys.ID);
				Optional<LootInterface> lootInstance = instance.getLootManager().getLoot(loot);
				if (lootInstance.isPresent()) {
					if (!lootInstance.get().disableStats() && has)
						return true;
					if (lootInstance.get().disableStats() && !has)
						return true;
				}
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "has-stats");
	}

	private void registerLootTypeRequirement() {
		registerRequirement((args, actions, runActions) -> {
			List<String> types = ListUtils.toList(args);
			return context -> {
				String loot = context.arg(ContextKeys.ID);
				Optional<LootInterface> lootInstance = instance.getLootManager().getLoot(loot);
				if (lootInstance.isPresent()) {
					if (types.contains(lootInstance.get().type().name().toLowerCase(Locale.ENGLISH)))
						return true;
				}
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "loot-type");
		registerRequirement((args, actions, runActions) -> {
			List<String> types = ListUtils.toList(args);
			return context -> {
				String loot = context.arg(ContextKeys.ID);
				Optional<LootInterface> lootInstance = instance.getLootManager().getLoot(loot);
				if (lootInstance.isPresent()) {
					if (!types.contains(lootInstance.get().type().name().toLowerCase(Locale.ENGLISH)))
						return true;
				}
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "!loot-type");
	}

	private void registerListRequirement() {
		registerRequirement((args, actions, runActions) -> {
			instance.getPluginLogger()
					.severe("It seems that you made a mistake where you put \"list\" into \"conditions\" section.");
			instance.getPluginLogger().warn("list:");
			for (String e : ListUtils.toList(args)) {
				instance.getPluginLogger().warn(" - " + e);
			}
			return Requirement.empty();
		}, "list");
	}

	private void registerLevelRequirement() {
		registerRequirement((args, actions, runActions) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				int current = context.holder().getLevel();
				if (current >= value.evaluate(context, true))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "level");
	}

	private void registerMoneyRequirement() {
		registerRequirement((args, actions, runActions) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				double current = VaultHook.getBalance(context.holder());
				if (current >= value.evaluate(context, true))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "money");
	}

	private void registerRandomRequirement() {
		registerRequirement((args, actions, runActions) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() < value.evaluate(context, true))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "random");
	}

	private void registerBiomeRequirement() {
		registerRequirement((args, actions, runActions) -> {
			HashSet<String> biomes = new HashSet<>(ListUtils.toList(args));
			return context -> {
				Location location = requireNonNull(Optional.ofNullable(context.arg(ContextKeys.OTHER_LOCATION))
						.orElse(context.arg(ContextKeys.LOCATION)));
				String currentBiome = instance.getVersionManager().getNMSManager().getBiomeResourceLocation(location);
				if (biomes.contains(currentBiome))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "biome");
		registerRequirement((args, actions, runActions) -> {
			HashSet<String> biomes = new HashSet<>(ListUtils.toList(args));
			return context -> {
				Location location = requireNonNull(Optional.ofNullable(context.arg(ContextKeys.OTHER_LOCATION))
						.orElse(context.arg(ContextKeys.LOCATION)));
				String currentBiome = instance.getVersionManager().getNMSManager().getBiomeResourceLocation(location);
				if (!biomes.contains(currentBiome))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "!biome");
	}

	private void registerWeatherRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> weathers = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String currentWeather;
				if (instance.getLavaRainHandler().getLavaRainTask() != null
						&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining())
					currentWeather = "lavarain";
				else
					currentWeather = "clear";
				if (weathers.contains(currentWeather))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "weather");
	}

	private void registerCoolDownRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				String key = section.getString("key");
				int time = section.getInt("time");
				return context -> {
					if (!instance.getCooldownManager().isCoolDown(context.holder().getUniqueId(), key, time))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at cooldown requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "cooldown");
	}

	private void registerDateRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> dates = new HashSet<>(ListUtils.toList(args));
			return context -> {
				Calendar calendar = Calendar.getInstance();
				String current = (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DATE);
				if (dates.contains(current))
					return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "date");
	}

	private void registerPermissionRequirement() {
		registerRequirement((args, actions, runActions) -> {
			List<String> perms = ListUtils.toList(args);
			return context -> {
				for (String perm : perms)
					if (context.holder().hasPermission(perm))
						return true;
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "permission");
		registerRequirement((args, actions, runActions) -> {
			List<String> perms = ListUtils.toList(args);
			return context -> {
				for (String perm : perms)
					if (context.holder().hasPermission(perm)) {
						if (runActions)
							ActionManagerInterface.trigger(context, actions);
						return false;
					}
				return true;
			};
		}, "!permission");
	}

	private void registerPAPIRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				MathValue<Player> v1 = MathValue.auto(section.get("value1"));
				MathValue<Player> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) < v2.evaluate(context, true))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at < requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "<");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				MathValue<Player> v1 = MathValue.auto(section.get("value1"));
				MathValue<Player> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) <= v2.evaluate(context, true))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at <= requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "<=");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				MathValue<Player> v1 = MathValue.auto(section.get("value1"));
				MathValue<Player> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) != v2.evaluate(context, true))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at != requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "!=");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				MathValue<Player> v1 = MathValue.auto(section.get("value1"));
				MathValue<Player> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) == v2.evaluate(context, true))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at == requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "==", "=");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				MathValue<Player> v1 = MathValue.auto(section.get("value1"));
				MathValue<Player> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) >= v2.evaluate(context, true))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at >= requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, ">=");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				MathValue<Player> v1 = MathValue.auto(section.get("value1"));
				MathValue<Player> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) > v2.evaluate(context, true))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at > requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, ">");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("papi", ""));
				String v2 = section.getString("regex", "");
				return context -> {
					if (v1.render(context, true).matches(v2))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at regex requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "regex");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("value1", ""));
				TextValue<Player> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (v1.render(context, true).startsWith(v2.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at startsWith requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "startsWith");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("value1", ""));
				TextValue<Player> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (!v1.render(context, true).startsWith(v2.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at !startsWith requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "!startsWith");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("value1", ""));
				TextValue<Player> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (v1.render(context, true).endsWith(v2.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at endsWith requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "endsWith");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("value1", ""));
				TextValue<Player> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (!v1.render(context, true).endsWith(v2.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at !endsWith requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "!endsWith");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("value1", ""));
				TextValue<Player> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (v1.render(context, true).contains(v2.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at contains requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "contains");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("value1", ""));
				TextValue<Player> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (!v1.render(context, true).contains(v2.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at !contains requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "!contains");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> papi = TextValue.auto(section.getString("papi", ""));
				List<String> values = ListUtils.toList(section.get("values"));
				return context -> {
					if (values.contains(papi.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at in-list requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "in-list");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> papi = TextValue.auto(section.getString("papi", ""));
				List<String> values = ListUtils.toList(section.get("values"));
				return context -> {
					if (!values.contains(papi.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at !in-list requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "!in-list");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("value1", ""));
				TextValue<Player> v2 = TextValue.auto(section.getString("value2", ""));

				return context -> {
					if (v1.render(context, true).equals(v2.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at equals requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "equals");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				TextValue<Player> v1 = TextValue.auto(section.getString("value1", ""));
				TextValue<Player> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (!v1.render(context, true).equals(v2.render(context, true)))
						return true;
					if (runActions)
						ActionManagerInterface.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at !equals requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "!equals");
	}

	@SuppressWarnings("deprecation")
	private void registerPotionEffectRequirement() {
		registerRequirement((args, actions, runActions) -> {
			String potions = (String) args;
			String[] split = potions.split("(<=|>=|<|>|==)", 2);
			PotionEffectType type = PotionEffectType.getByName(split[0]);
			if (type == null) {
				instance.getPluginLogger().warn("Potion effect doesn't exist: " + split[0]);
				return Requirement.empty();
			}
			int required = Integer.parseInt(split[1]);
			String operator = potions.substring(split[0].length(), potions.length() - split[1].length());
			return context -> {
				int level = -1;
				PotionEffect potionEffect = context.holder().getPotionEffect(type);
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
				case "=", "==" -> {
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
				if (runActions)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "potion-effect");
	}

	private void registerSneakRequirement() {
		registerRequirement((args, actions, advanced) -> {
			boolean sneak = (boolean) args;
			return context -> {
				if (context.holder() == null)
					return false;
				if (sneak) {
					if (context.holder().isSneaking())
						return true;
				} else {
					if (!context.holder().isSneaking())
						return true;
				}
				if (advanced)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "sneak");
	}

	protected void registerGameModeRequirement() {
		registerRequirement((args, actions, advanced) -> {
			List<String> modes = ListUtils.toList(args);
			return context -> {
				if (context.holder() == null)
					return false;
				var name = context.holder().getGameMode().name().toLowerCase(Locale.ENGLISH);
				if (modes.contains(name)) {
					return true;
				}
				if (advanced)
					ActionManagerInterface.trigger(context, actions);
				return false;
			};
		}, "gamemode");
	}
}