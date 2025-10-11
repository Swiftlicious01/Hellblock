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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.listeners.rain.LavaRainTask;
import com.swiftlicious.hellblock.utils.ClassUtils;
import com.swiftlicious.hellblock.utils.ListUtils;

import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.factory.RequirementExpansion;
import com.swiftlicious.hellblock.utils.factory.RequirementFactory;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class AbstractRequirementManager<T> implements RequirementManager<T> {

	protected final HellblockPlugin instance;
	private final Map<String, RequirementFactory<T>> requirementFactoryMap = new HashMap<>();
	private static final String EXPANSION_FOLDER = "expansions/requirement";
	protected Class<T> tClass;

	public AbstractRequirementManager(HellblockPlugin plugin, Class<T> tClass) {
		this.instance = plugin;
		this.tClass = tClass;
	}

	public void registerBuiltInRequirements() {
		this.registerEnvironmentRequirement();
		this.registerYRequirement();
		this.registerAndRequirement();
		this.registerOrRequirement();
		this.registerPAPIRequirement();
		this.registerDateRequirement();
		this.registerWeatherRequirement();
		this.registerRandomRequirement();
		this.registerBiomeRequirement();
		this.registerTimeRequirement();
		this.registerListRequirement();
		this.registerImpossibleRequirement();
		this.registerLightRequirement();
		this.registerTemperatureRequirement();
		this.registerMoistureRequirement();
		this.registerLiquidDepthRequirement();
	}

	@Override
	public boolean registerRequirement(@NotNull RequirementFactory<T> requirementFactory, @NotNull String... types) {
		for (String type : types) {
			if (this.requirementFactoryMap.containsKey(type)) {
				return false;
			}
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

	@Override
	public boolean hasRequirement(@NotNull String type) {
		return requirementFactoryMap.containsKey(type);
	}

	@Nullable
	@Override
	public RequirementFactory<T> getRequirementFactory(@NotNull String type) {
		return requirementFactoryMap.get(type);
	}

	@NotNull
	@SuppressWarnings("unchecked")
	@Override
	public Requirement<T>[] parseRequirements(Section section, boolean runActions) {
		final List<Requirement<T>> requirements = new ArrayList<>();
		if (section != null) {
			section.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
				final String typeOrName = entry.getKey();
				if (hasRequirement(typeOrName)) {
					requirements.add(parseRequirement(typeOrName, entry.getValue()));
				} else {
					final Section inner = section.getSection(typeOrName);
					if (inner != null) {
						requirements.add(parseRequirement(inner, runActions));
					} else {
						instance.getPluginLogger()
								.warn("Section " + section.getRouteAsString() + "." + typeOrName + " is misconfigured");
					}
				}
			});
		}
		return requirements.toArray(Requirement[]::new);
	}

	@NotNull
	@Override
	public Requirement<T> parseRequirement(@NotNull Section section, boolean runActions) {
		final List<Action<T>> actionList = new ArrayList<>();
		if (runActions && section.contains("not-met-actions")) {
			final Action<T>[] actions = instance.getActionManager(tClass)
					.parseActions(requireNonNull(section.getSection("not-met-actions")));
			actionList.addAll(List.of(actions));
		}
		final String type = section.getString("type");
		if (type == null) {
			instance.getPluginLogger().warn("No requirement type found at " + section.getRouteAsString());
			return Requirement.empty();
		}
		final var factory = getRequirementFactory(type);
		if (factory != null) {
			return factory.process(section.get("value"), actionList, runActions);
		}
		instance.getPluginLogger().warn("Requirement type: " + type + " doesn't exist");
		return Requirement.empty();
	}

	@NotNull
	@Override
	public Requirement<T> parseRequirement(@NotNull String type, @NotNull Object value) {
		final RequirementFactory<T> factory = getRequirementFactory(type);
		if (factory != null) {
			return factory.process(value);
		}
		instance.getPluginLogger().warn("Requirement type: " + type + " doesn't exist.");
		return Requirement.empty();
	}

	@SuppressWarnings({ "unchecked" })
	protected void loadExpansions(Class<T> tClass) {
		final File expansionFolder = new File(instance.getDataFolder(), EXPANSION_FOLDER);
		if (!expansionFolder.exists()) {
			expansionFolder.mkdirs();
		}
		final List<Class<? extends RequirementExpansion<T>>> classes = new ArrayList<>();
		final File[] expansionJars = expansionFolder.listFiles();
		if (expansionJars == null) {
			return;
		}
		for (File expansionJar : expansionJars) {
			if (expansionJar.getName().endsWith(".jar")) {
				try {
					final Class<? extends RequirementExpansion<T>> expansionClass = (Class<? extends RequirementExpansion<T>>) ClassUtils
							.findClass(expansionJar, RequirementExpansion.class, tClass);
					classes.add(expansionClass);
				} catch (IOException | ClassNotFoundException e) {
					instance.getPluginLogger().warn("Failed to load expansion: " + expansionJar.getName(), e);
				}
			}
		}
		try {
			for (Class<? extends RequirementExpansion<T>> expansionClass : classes) {
				final RequirementExpansion<T> expansion = expansionClass.getDeclaredConstructor().newInstance();
				unregisterRequirement(expansion.getRequirementType());
				registerRequirement(expansion.getRequirementFactory(), expansion.getRequirementType());
				instance.getPluginLogger().info("Loaded requirement expansion: " + expansion.getRequirementType() + "["
						+ expansion.getVersion() + "]" + " by " + expansion.getAuthor());
			}
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException
				| NoSuchMethodException e) {
			instance.getPluginLogger().warn("Error occurred when creating expansion instance.", e);
		}
	}

	protected void registerImpossibleRequirement() {
		registerRequirement((args, actions, advanced) -> context -> false, "impossible");
	}

	protected void registerEnvironmentRequirement() {
		registerRequirement((args, actions, advanced) -> {
			final List<String> environments = ListUtils.toList(args);
			return context -> {
				final Location location = context.arg(ContextKeys.LOCATION);
				if (location == null) {
					return false;
				}
				final var name = location.getWorld().getEnvironment().name().toLowerCase(Locale.ENGLISH);
				if (environments.contains(name)) {
					return true;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "environment");
		registerRequirement((args, actions, advanced) -> {
			final List<String> environments = ListUtils.toList(args);
			return context -> {
				final Location location = context.arg(ContextKeys.LOCATION);
				if (location == null) {
					return false;
				}
				final var name = location.getWorld().getEnvironment().name().toLowerCase(Locale.ENGLISH);
				if (!environments.contains(name)) {
					return true;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "!environment");
	}

	protected void registerYRequirement() {
		registerRequirement((args, actions, runActions) -> {
			final List<String> list = ListUtils.toList(args);
			final List<Pair<Double, Double>> posPairs = list.stream().map(line -> {
				final String[] split = line.split("~");
				return new Pair<>(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
			}).toList();
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final double y = location.getY();
				for (Pair<Double, Double> pair : posPairs) {
					if (y >= pair.left() && y <= pair.right()) {
						return true;
					}
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "ypos");
	}

	protected void registerOrRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				final Requirement<T>[] requirements = parseRequirements(section, runActions);
				return context -> {
					for (Requirement<T> requirement : requirements) {
						if (requirement.isSatisfied(context)) {
							return true;
						}
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at || requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "||");
	}

	protected void registerAndRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				final Requirement<T>[] requirements = parseRequirements(section, runActions);
				return context -> {
					outer: {
						for (Requirement<T> requirement : requirements) {
							if (!requirement.isSatisfied(context)) {
								break outer;
							}
						}
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at && requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "&&");
	}

	protected void registerRandomRequirement() {
		registerRequirement((args, actions, runActions) -> {
			final MathValue<T> value = MathValue.auto(args);
			return context -> {
				if (Math.random() < value.evaluate(context, true)) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "random");
	}

	protected void registerTimeRequirement() {
		registerRequirement((args, actions, runActions) -> {
			final List<String> list = ListUtils.toList(args);
			final List<Pair<Integer, Integer>> timePairs = list.stream().map(line -> {
				final String[] split = line.split("~");
				return new Pair<>(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
			}).toList();
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final long time = location.getWorld().getTime();
				for (Pair<Integer, Integer> pair : timePairs) {
					if (time >= pair.left() && time <= pair.right()) {
						return true;
					}
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "time");
	}

	protected void registerBiomeRequirement() {
		registerRequirement((args, actions, runActions) -> {
			final Set<String> biomes = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final String currentBiome = VersionHelper.getNMSManager().getBiomeResourceLocation(location);
				if (biomes.contains(currentBiome)) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "biome");
		registerRequirement((args, actions, runActions) -> {
			final Set<String> biomes = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final String currentBiome = VersionHelper.getNMSManager().getBiomeResourceLocation(location);
				if (!biomes.contains(currentBiome)) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "!biome");
	}

	protected void registerWorldRequirement() {
		registerRequirement((args, actions, runActions) -> {
			final Set<String> worlds = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				if (worlds.contains(location.getWorld().getName())) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "world");
		registerRequirement((args, actions, runActions) -> {
			final Set<String> worlds = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				if (!worlds.contains(location.getWorld().getName())) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "!world");
	}

	protected void registerWeatherRequirement() {
		registerRequirement((args, actions, runActions) -> {
			final Set<String> weathers = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final String currentWeather;
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final World world = location.getWorld();
				final Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
						.filter(task -> world.getName().equalsIgnoreCase(task.getWorld().worldName())).findAny();
				if (lavaRain.isPresent() && lavaRain.get().isLavaRaining()) {
					currentWeather = "lavarain";
				} else {
					currentWeather = "clear";
				}
				if (weathers.contains(currentWeather)) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "weather");
		registerRequirement((args, actions, runActions) -> {
			final Set<String> weathers = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final String currentWeather;
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final World world = location.getWorld();
				final Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
						.filter(task -> world.getName().equalsIgnoreCase(task.getWorld().worldName())).findAny();
				if (!lavaRain.isPresent() && !lavaRain.get().isLavaRaining()) {
					currentWeather = "lavarain";
				} else {
					currentWeather = "clear";
				}
				if (weathers.contains(currentWeather)) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "!weather");
	}

	protected void registerLiquidDepthRequirement() {
		registerRequirement((args, actions, advanced) -> {
			final String depthRange = (String) args;
			final String[] split = depthRange.split("~");
			if (split.length != 2) {
				instance.getPluginLogger().warn("Invalid value found(" + depthRange
						+ "). `liquid-depth` requires a range in the format 'min~max' (e.g., 1~10).");
				return Requirement.empty();
			}
			final int min;
			final int max;
			try {
				min = Integer.parseInt(split[0]);
				max = Integer.parseInt(split[1]);
			} catch (NumberFormatException e) {
				instance.getPluginLogger().warn("Invalid number format for range: " + depthRange, e);
				return Requirement.empty();
			}
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.OTHER_LOCATION));
				final Location startLocation = location.getBlock().isLiquid() ? location.clone()
						: location.clone().subtract(0, 1, 0);
				int depth = 0;
				while (startLocation.getBlock().isLiquid()) {
					startLocation.subtract(0, 1, 0);
					depth++;
				}
				if (depth >= min && depth <= max) {
					return true;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "liquid-depth");
	}

	protected void registerDateRequirement() {
		registerRequirement((args, actions, runActions) -> {
			final Set<String> dates = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final Calendar calendar = Calendar.getInstance();
				final String current = (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DATE);
				if (dates.contains(current)) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "date");
	}

	private void registerListRequirement() {
		registerRequirement((args, actions, runActions) -> {
			instance.getPluginLogger()
					.severe("It seems that you made a mistake where you put \"list\" into \"conditions\" section.");
			instance.getPluginLogger().warn("list:");
			ListUtils.toList(args).forEach(e -> instance.getPluginLogger().warn(" - " + e));
			return Requirement.empty();
		}, "list");
	}

	protected void registerLightRequirement() {
		registerRequirement((args, actions, advanced) -> {
			final List<String> list = ListUtils.toList(args);
			final List<Pair<Integer, Integer>> lightPairs = list.stream().map(line -> {
				final String[] split = line.split("~");
				return new Pair<>(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
			}).toList();
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final int temp = location.getBlock().getLightLevel();
				for (Pair<Integer, Integer> pair : lightPairs) {
					if (temp >= pair.left() && temp <= pair.right()) {
						return true;
					}
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "light");
		registerRequirement((args, actions, advanced) -> {
			final List<String> list = ListUtils.toList(args);
			final List<Pair<Integer, Integer>> lightPairs = list.stream().map(line -> {
				final String[] split = line.split("~");
				return new Pair<>(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
			}).toList();
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final int temp = location.getBlock().getLightFromSky();
				for (Pair<Integer, Integer> pair : lightPairs) {
					if (temp >= pair.left() && temp <= pair.right()) {
						return true;
					}
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "natural-light", "skylight");
		registerRequirement((args, actions, advanced) -> {
			final int value = (int) args;
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final int light = location.getBlock().getLightFromSky();
				if (light > value) {
					return true;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "skylight_more_than", "skylight-more-than", "natural_light_more_than", "natural-light-more-than");
		registerRequirement((args, actions, advanced) -> {
			final int value = (int) args;
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final int light = location.getBlock().getLightFromSky();
				if (light < value) {
					return true;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "skylight_less_than", "skylight-less-than", "natural_light_less_than", "natural-light-less-than");
		registerRequirement((args, actions, advanced) -> {
			final int value = (int) args;
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final int light = location.getBlock().getLightLevel();
				if (light > value) {
					return true;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "light_more_than", "light-more-than");
		registerRequirement((args, actions, advanced) -> {
			final int value = (int) args;
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final int light = location.getBlock().getLightLevel();
				if (light < value) {
					return true;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "light_less_than", "light-less-than");
	}

	protected void registerTemperatureRequirement() {
		registerRequirement((args, actions, advanced) -> {
			final List<String> list = ListUtils.toList(args);
			final List<Pair<Integer, Integer>> temperaturePairs = list.stream().map(line -> {
				final String[] split = line.split("~");
				return new Pair<>(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
			}).toList();
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
				final double temp = location.getWorld().getTemperature(location.getBlockX(), location.getBlockY(),
						location.getBlockZ());
				for (Pair<Integer, Integer> pair : temperaturePairs) {
					if (temp >= pair.left() && temp <= pair.right()) {
						return true;
					}
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "temperature");
	}

	protected void registerMoistureRequirement() {
		registerRequirement((args, actions, advanced) -> {
			final int value;
			final int y;
			if (args instanceof Integer integer) {
				y = -1;
				value = integer;
			} else if (args instanceof Section section) {
				y = section.getInt("y");
				value = section.getInt("value");
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at moisture-more-than requirement which is expected be `Section`");
				return Requirement.empty();
			}
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION)).clone().add(0, y, 0);
				final Block block = location.getBlock();
				if (block.getBlockData() instanceof Farmland farmland) {
					return farmland.getMoisture() > value;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "moisture-more-than", "moisture_more_than");
		registerRequirement((args, actions, advanced) -> {
			final int value;
			final int y;
			if (args instanceof Integer integer) {
				y = -1;
				value = integer;
			} else if (args instanceof Section section) {
				y = section.getInt("y");
				value = section.getInt("value");
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at moisture-less-than requirement which is expected be `Section`");
				return Requirement.empty();
			}
			return context -> {
				final Location location = requireNonNull(context.arg(ContextKeys.LOCATION)).clone().add(0, y, 0);
				final Block block = location.getBlock();
				if (block.getBlockData() instanceof Farmland farmland) {
					return farmland.getMoisture() < value;
				}
				if (advanced) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "moisture-less-than", "moisture_less_than");
	}

	protected void registerPAPIRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				final MathValue<T> v1 = MathValue.auto(section.get("value1"));
				final MathValue<T> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) < v2.evaluate(context, true)) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final MathValue<T> v1 = MathValue.auto(section.get("value1"));
				final MathValue<T> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) <= v2.evaluate(context, true)) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final MathValue<T> v1 = MathValue.auto(section.get("value1"));
				final MathValue<T> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) != v2.evaluate(context, true)) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final MathValue<T> v1 = MathValue.auto(section.get("value1"));
				final MathValue<T> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) == v2.evaluate(context, true)) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final MathValue<T> v1 = MathValue.auto(section.get("value1"));
				final MathValue<T> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) >= v2.evaluate(context, true)) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final MathValue<T> v1 = MathValue.auto(section.get("value1"));
				final MathValue<T> v2 = MathValue.auto(section.get("value2"));
				return context -> {
					if (v1.evaluate(context, true) > v2.evaluate(context, true)) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("papi", ""));
				final String v2 = section.getString("regex", "");
				return context -> {
					if (v1.render(context, true).matches(v2)) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("value1", ""));
				final TextValue<T> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (v1.render(context, true).startsWith(v2.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("value1", ""));
				final TextValue<T> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (!v1.render(context, true).startsWith(v2.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("value1", ""));
				final TextValue<T> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (v1.render(context, true).endsWith(v2.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("value1", ""));
				final TextValue<T> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (!v1.render(context, true).endsWith(v2.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("value1", ""));
				final TextValue<T> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (v1.render(context, true).contains(v2.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("value1", ""));
				final TextValue<T> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (!v1.render(context, true).contains(v2.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> papi = TextValue.auto(section.getString("papi", ""));
				final List<String> values = ListUtils.toList(section.get("values"));
				return context -> {
					if (values.contains(papi.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> papi = TextValue.auto(section.getString("papi", ""));
				final List<String> values = ListUtils.toList(section.get("values"));
				return context -> {
					if (!values.contains(papi.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("value1", ""));
				final TextValue<T> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (v1.render(context, true).equals(v2.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
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
				final TextValue<T> v1 = TextValue.auto(section.getString("value1", ""));
				final TextValue<T> v2 = TextValue.auto(section.getString("value2", ""));
				return context -> {
					if (!v1.render(context, true).equals(v2.render(context, true))) {
						return true;
					}
					if (runActions) {
						ActionManager.trigger(context, actions);
					}
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at !equals requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "!equals");
	}
}