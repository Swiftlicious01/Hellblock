package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.ConfigManager.HellEnchantmentData;
import com.swiftlicious.hellblock.config.ConfigManager.IslandEventData;
import com.swiftlicious.hellblock.config.ConfigManager.TitleScreenInfo;
import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.function.BaseEffectParserFunction;
import com.swiftlicious.hellblock.config.parser.function.BlockParserFunction;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EffectModifierParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EntityParserFunction;
import com.swiftlicious.hellblock.config.parser.function.EventParserFunction;
import com.swiftlicious.hellblock.config.parser.function.HookParserFunction;
import com.swiftlicious.hellblock.config.parser.function.ItemParserFunction;
import com.swiftlicious.hellblock.config.parser.function.LootParserFunction;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.block.BlockConfig;
import com.swiftlicious.hellblock.creation.entity.EntityConfig;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.EventCarrier;
import com.swiftlicious.hellblock.listeners.weather.WeatherType;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootBaseEffect;
import com.swiftlicious.hellblock.loot.operation.WeightOperation;
import com.swiftlicious.hellblock.mechanics.hook.HookConfig;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;
import net.kyori.adventure.sound.Sound;

public abstract class ConfigHandler implements ConfigLoader, Reloadable {

	protected final HellblockPlugin instance;
	protected final Map<String, Node<ConfigParserFunction>> entityFormatFunctions = new HashMap<>();
	protected final Map<String, Node<ConfigParserFunction>> blockFormatFunctions = new HashMap<>();
	protected final Map<String, Node<ConfigParserFunction>> hookFormatFunctions = new HashMap<>();
	protected final Map<String, Node<ConfigParserFunction>> eventFormatFunctions = new HashMap<>();
	protected final Map<String, Node<ConfigParserFunction>> baseEffectFormatFunctions = new HashMap<>();
	protected final Map<String, Node<ConfigParserFunction>> effectModifierFormatFunctions = new HashMap<>();
	protected final Map<String, Node<ConfigParserFunction>> itemFormatFunctions = new HashMap<>();
	protected final Map<String, Node<ConfigParserFunction>> lootFormatFunctions = new HashMap<>();
	protected int placeholderLimit;
	protected boolean redisRanking;
	protected Sound challengeCompletedSound;
	protected Sound linkingHellblockSound;
	protected Sound creatingHellblockSound;
	protected TitleScreenInfo creationTitleScreen;
	protected String serverGroup;
	protected String[] itemDetectOrder = new String[0];
	protected String[] blockDetectOrder = new String[0];
	protected int dataSaveInterval;
	protected boolean logDataSaving;
	protected boolean lockData;
	protected boolean metrics;
	protected boolean checkUpdate;
	protected boolean debug;
	protected boolean lavaFishingEnabled;
	protected int lavaMinTime;
	protected int lavaMaxTime;
	protected int finalLavaMinTime;
	protected int finalLavaMaxTime;
	protected int multipleLootSpawnDelay;
	protected boolean restrictedSizeRange;
	protected List<String> durabilityLore = new ArrayList<>();
	protected EventPriority eventPriority;
	protected Requirement<Player>[] fishingPlayerRequirements;
	protected Requirement<Player>[] autoFishingPlayerRequirements;
	protected Requirement<Integer>[] fishingIslandRequirements;
	protected Requirement<Integer>[] autoFishingIslandRequirements;
	protected boolean baitAnimation;
	protected boolean antiAutoFishingMod;
	protected List<TriConsumer<Effect, Context<Player>, Integer>> globalEffects = new ArrayList<>();

	protected HellEnchantmentData lavaVisionEnchantData;
	protected HellEnchantmentData crimsonThornsEnchantData;
	protected HellEnchantmentData moltenCoreEnchantData;
	protected HellEnchantmentData magmaWalkerEnchantData;

	protected boolean worldguardProtect;
	protected boolean disableGenerationAnimation;
	protected boolean perPlayerWorlds;
	protected boolean transferIslands;
	protected boolean linkHellblocks;
	protected boolean resetInventory;
	protected boolean resetEnderchest;
	protected boolean entryMessageEnabled;
	protected boolean farewellMessageEnabled;
	protected boolean disableBedExplosions;
	protected boolean growNaturalTrees;
	protected boolean useParticleBorder;
	protected boolean voidTeleport;
	protected boolean lightningDeath;
	protected boolean asyncWorldSaving;
	protected String schematicPaster;
	protected String worldName;
	protected String chestInventoryName;
	protected String spawnCommand;
	protected String absoluteWorldPath;
	protected int height;
	protected int abandonTime;
	protected int maxBioCharLength;
	protected int maxNameCharLength;
	protected int maxColorCodes;
	protected int maxNewLines;
	protected int wrapLength;
	protected Set<String> commandWhitelist = new HashSet<>();
	protected List<String> bannedWords = new ArrayList<>();
	protected Set<IslandOptions> islandOptions = new HashSet<>();
	protected Map<Integer, Pair<Integer, CustomItem>> chestItems = new HashMap<>();

	protected IslandEventData invasionEvent;
	protected IslandEventData witherEvent;
	protected IslandEventData skysiegeEvent;

	protected boolean clearDefaultOutcome;
	protected Map<CustomItem, MathValue<Player>> barteringItems = new HashMap<>();

	protected boolean randomStats;
	protected int randomMinHealth;
	protected int randomMaxHealth;
	protected double randomMinStrength;
	protected double randomMaxStrength;
	protected int defaultHealth;
	protected double defaultStrength;

	protected boolean infiniteLavaEnabled;

	protected boolean weatherEnabled;
	protected Set<WeatherType> supportedWeatherTypes = new HashSet<>();
	protected int minTime;
	protected int maxTime;
	protected int radius;
	protected int fireChance;
	protected int delay;
	protected boolean warnPlayers;
	protected boolean hurtCreatures;
	protected boolean explodeTNT;

	protected double searchRadius;
	protected boolean pistonAutomation;
	protected Map<Material, MathValue<Player>> generationResults = new HashMap<>();

	public ConfigHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	public boolean debug() {
		return debug;
	}

	public boolean perPlayerWorlds() {
		return perPlayerWorlds;
	}

	public boolean worldguardProtect() {
		return worldguardProtect;
	}

	public boolean transferIslands() {
		return transferIslands;
	}

	public boolean linkHellblocks() {
		return linkHellblocks;
	}

	public boolean resetInventory() {
		return resetInventory;
	}

	public boolean resetEnderchest() {
		return resetEnderchest;
	}

	public boolean entryMessageEnabled() {
		return entryMessageEnabled;
	}

	public boolean farewellMessageEnabled() {
		return farewellMessageEnabled;
	}

	public boolean disableBedExplosions() {
		return disableBedExplosions;
	}

	public boolean growNaturalTrees() {
		return growNaturalTrees;
	}

	public boolean useParticleBorder() {
		return useParticleBorder;
	}

	public boolean disableGenerationAnimation() {
		return disableGenerationAnimation;
	}

	public boolean voidTeleport() {
		return voidTeleport;
	}

	public boolean lightningOnDeath() {
		return lightningDeath;
	}

	public boolean asyncWorldSaving() {
		return asyncWorldSaving;
	}

	public String schematicPaster() {
		return schematicPaster;
	}

	public String worldName() {
		return worldName;
	}

	public String chestName() {
		return chestInventoryName;
	}

	public String spawnCommand() {
		return spawnCommand;
	}

	public String absoluteWorldPath() {
		return absoluteWorldPath;
	}

	public Map<Integer, Pair<Integer, CustomItem>> chestItems() {
		return chestItems;
	}

	public int height() {
		return height;
	}

	public int abandonAfterDays() {
		return abandonTime;
	}

	public int maxBioCharLength() {
		return maxBioCharLength;
	}

	public int maxNameCharLength() {
		return maxNameCharLength;
	}

	public int maxColorCodes() {
		return maxColorCodes;
	}

	public int maxNewLines() {
		return maxNewLines;
	}

	public int wrapLength() {
		return wrapLength;
	}

	public Set<String> commandWhitelist() {
		return commandWhitelist;
	}

	public List<String> bannedWords() {
		return bannedWords;
	}

	public Set<IslandOptions> islandOptions() {
		return islandOptions;
	}

	public boolean clearDefaultOutcome() {
		return clearDefaultOutcome;
	}

	public Map<CustomItem, MathValue<Player>> barteringItems() {
		return barteringItems;
	}

	public boolean randomStats() {
		return randomStats;
	}

	public int randomMinHealth() {
		return randomMinHealth;
	}

	public int randomMaxHealth() {
		return randomMaxHealth;
	}

	public double randomMinStrength() {
		return randomMinStrength;
	}

	public double randomMaxStrength() {
		return randomMaxStrength;
	}

	public boolean infiniteLavaEnabled() {
		return infiniteLavaEnabled;
	}

	public int defaultHealth() {
		return defaultHealth;
	}

	public double defaultStrength() {
		return defaultStrength;
	}

	public boolean weatherEnabled() {
		return weatherEnabled;
	}
	
	public Set<WeatherType> supportedWeatherTypes() {
		return supportedWeatherTypes;
	}
	
	public int minWeatherTime() {
		return minTime;
	}
	
	public int maxWeatherTime() {
		return maxTime;
	}

	public int radius() {
		return radius;
	}

	public int fireChance() {
		return fireChance;
	}

	public int delay() {
		return delay;
	}

	public boolean willWarnPlayers() {
		return warnPlayers;
	}

	public boolean canHurtCreatures() {
		return hurtCreatures;
	}

	public boolean canExplodeTNT() {
		return explodeTNT;
	}

	public double searchRadius() {
		return searchRadius;
	}

	public boolean pistonAutomation() {
		return pistonAutomation;
	}

	public Map<Material, MathValue<Player>> generationResults() {
		return generationResults;
	}

	public int placeholderLimit() {
		return placeholderLimit;
	}

	public boolean redisRanking() {
		return redisRanking;
	}

	public String serverGroup() {
		return serverGroup;
	}

	public String[] itemDetectOrder() {
		return itemDetectOrder;
	}

	public String[] blockDetectOrder() {
		return blockDetectOrder;
	}

	public Sound challengeCompleteSound() {
		return challengeCompletedSound;
	}

	public Sound linkingHellblockSound() {
		return linkingHellblockSound;
	}

	public Sound creatingHellblockSound() {
		return creatingHellblockSound;
	}

	public TitleScreenInfo creationTitleScreen() {
		return creationTitleScreen;
	}

	public IslandEventData invasionEventSettings() {
		return invasionEvent;
	}

	public IslandEventData witherEventSettings() {
		return witherEvent;
	}

	public IslandEventData skysiegeEventSettings() {
		return skysiegeEvent;
	}

	public int dataSaveInterval() {
		return dataSaveInterval;
	}

	public boolean logDataSaving() {
		return logDataSaving;
	}

	public boolean lockData() {
		return lockData;
	}

	public boolean metrics() {
		return metrics;
	}

	public boolean checkUpdate() {
		return checkUpdate;
	}

	public boolean lavaFishingEnabled() {
		return lavaFishingEnabled;
	}

	public int lavaMinTime() {
		return lavaMinTime;
	}

	public int lavaMaxTime() {
		return lavaMaxTime;
	}

	public int finalLavaMinTime() {
		return finalLavaMinTime;
	}

	public int finalLavaMaxTime() {
		return finalLavaMaxTime;
	}

	public int multipleLootSpawnDelay() {
		return multipleLootSpawnDelay;
	}

	public boolean restrictedSizeRange() {
		return restrictedSizeRange;
	}

	public boolean baitAnimation() {
		return baitAnimation;
	}

	public boolean antiAutoFishingMod() {
		return antiAutoFishingMod;
	}

	public List<String> durabilityLore() {
		return durabilityLore;
	}

	public HellEnchantmentData lavaVisionData() {
		return lavaVisionEnchantData;
	}

	public HellEnchantmentData crimsonThornsData() {
		return crimsonThornsEnchantData;
	}

	public HellEnchantmentData moltenCoreData() {
		return moltenCoreEnchantData;
	}

	public HellEnchantmentData magmaWalkerData() {
		return magmaWalkerEnchantData;
	}

	public EventPriority eventPriority() {
		return eventPriority;
	}

	public Requirement<Player>[] fishingPlayerRequirements() {
		return fishingPlayerRequirements;
	}

	public Requirement<Integer>[] fishingIslandRequirements() {
		return fishingIslandRequirements;
	}

	public Requirement<Player>[] autoFishingPlayerRequirements() {
		return autoFishingPlayerRequirements;
	}

	public Requirement<Integer>[] autoFishingIslandRequirements() {
		return autoFishingIslandRequirements;
	}

	public List<TriConsumer<Effect, Context<Player>, Integer>> globalEffects() {
		return globalEffects;
	}

	public void registerHookParser(Function<Object, Consumer<HookConfig.Builder>> function, String... nodes) {
		registerNodeFunction(nodes, new HookParserFunction(function), hookFormatFunctions);
	}

	public void registerLootParser(Function<Object, Consumer<Loot.Builder>> function, String... nodes) {
		registerNodeFunction(nodes, new LootParserFunction(function), lootFormatFunctions);
	}

	public void registerItemParser(Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> function,
			int priority, String... nodes) {
		registerNodeFunction(nodes, new ItemParserFunction(priority, function), itemFormatFunctions);
	}

	public void registerEffectModifierParser(Function<Object, Consumer<EffectModifier.Builder>> function,
			String... nodes) {
		registerNodeFunction(nodes, new EffectModifierParserFunction(function), effectModifierFormatFunctions);
	}

	public void registerEntityParser(Function<Object, Consumer<EntityConfig.Builder>> function, String... nodes) {
		registerNodeFunction(nodes, new EntityParserFunction(function), entityFormatFunctions);
	}

	public void registerBlockParser(Function<Object, Consumer<BlockConfig.Builder>> function, String... nodes) {
		registerNodeFunction(nodes, new BlockParserFunction(function), blockFormatFunctions);
	}

	public void registerEventParser(Function<Object, Consumer<EventCarrier.Builder>> function, String... nodes) {
		registerNodeFunction(nodes, new EventParserFunction(function), eventFormatFunctions);
	}

	public void registerBaseEffectParser(Function<Object, Consumer<LootBaseEffect.Builder>> function, String... nodes) {
		registerNodeFunction(nodes, new BaseEffectParserFunction(function), baseEffectFormatFunctions);
	}

	public void unregisterNodeFunction(Map<String, Node<ConfigParserFunction>> functionMap, String... nodes) {
		for (int i = 0; i < nodes.length; i++) {
			if (functionMap.containsKey(nodes[i])) {
				final Node<ConfigParserFunction> functionNode = functionMap.get(nodes[i]);
				if (i != nodes.length - 1) {
					if (functionNode.nodeValue() != null) {
						return;
					} else {
						functionMap = functionNode.getChildTree();
					}
				} else {
					if (functionNode.nodeValue() != null) {
						functionMap.remove(nodes[i]);
					}
				}
			}
		}
	}

	public void registerNodeFunction(String[] nodes, ConfigParserFunction configParserFunction,
			Map<String, Node<ConfigParserFunction>> functionMap) {
		for (int i = 0; i < nodes.length; i++) {
			if (functionMap.containsKey(nodes[i])) {
				final Node<ConfigParserFunction> functionNode = functionMap.get(nodes[i]);
				if (functionNode.nodeValue() != null) {
					throw new IllegalArgumentException("Format function '" + nodes[i] + "' already exists");
				}
				functionMap = functionNode.getChildTree();
			} else {
				if (i != nodes.length - 1) {
					final Node<ConfigParserFunction> newNode = new Node<>();
					functionMap.put(nodes[i], newNode);
					functionMap = newNode.getChildTree();
				} else {
					functionMap.put(nodes[i], new Node<>(configParserFunction));
				}
			}
		}
	}

	@Override
	public Path resolveConfig(String filePath) {
		if (filePath == null || filePath.isEmpty()) {
			throw new IllegalArgumentException("ResourcePath cannot be null or empty");
		}

		filePath = filePath.replace('\\', '/');
		final Path configFile = instance.getDataFolder().toPath().toAbsolutePath().resolve(filePath);

		// Create parent directories if missing
		try {
			Files.createDirectories(configFile.getParent());
		} catch (IOException ignored) {
		}

		// If the file doesn't exist yet, copy it from resources
		if (!Files.exists(configFile)) {
			String resourcePath = filePath;
			InputStream is = instance.getResource(resourcePath);

			// fallback to .gz
			boolean gzipped = false;
			if (is == null) {
				is = instance.getResource(resourcePath + ".gz");
				if (is != null) {
					gzipped = true;
				}
			}

			if (is == null) {
				throw new IllegalArgumentException("The embedded resource '%s' cannot be found".formatted(filePath));
			}

			try (InputStream in = gzipped ? new java.util.zip.GZIPInputStream(is) : is) {
				Files.copy(in, configFile);
			} catch (IOException ex) {
				throw new RuntimeException("Failed to copy resource: " + filePath, ex);
			}
		}

		return configFile;
	}

	@Override
	public InputStream getResourceMaybeGz(String filePath) {
		// Normalize slashes
		filePath = filePath.replace('\\', '/');

		// If caller already included ".gz", use as-is
		if (filePath.endsWith(".gz")) {
			InputStream in = instance.getResource(filePath);
			if (in != null) {
				try {
					return new GZIPInputStream(in);
				} catch (IOException e) {
					instance.getPluginLogger().warn("Failed to read compressed resource: " + filePath);
				}
			}
			return null;
		}

		// Try .gz first
		InputStream in = instance.getResource(filePath + ".gz");
		if (in != null) {
			try {
				return new GZIPInputStream(in);
			} catch (IOException e) {
				instance.getPluginLogger()
						.warn("Failed to read compressed resource: " + filePath + ".gz, using plain version.");
			}
		}

		// Then fallback to plain
		return instance.getResource(filePath);
	}

	@Override
	public YamlDocument loadConfig(String filePath) {
		return loadConfig(filePath, '.');
	}

	@Override
	public YamlDocument loadConfig(String filePath, char routeSeparator) {
		try (InputStream inputStream = new FileInputStream(resolveConfig(filePath).toFile())) {
			return YamlDocument.create(inputStream, getResourceMaybeGz(filePath),
					GeneralSettings.builder().setRouteSeparator(routeSeparator).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder().setScalarFormatter((tag, value, role, def) -> {
						if (role == NodeRole.KEY) {
							return ScalarStyle.PLAIN;
						} else {
							return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
						}
					}).build(), UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build());
		} catch (IOException ex) {
			instance.getPluginLogger().severe("Failed to load config %s".formatted(filePath), ex);
			throw new RuntimeException(ex);
		}
	}

	@Override
	public YamlDocument loadData(File file) {
		try (InputStream inputStream = new FileInputStream(file)) {
			return YamlDocument.create(inputStream);
		} catch (IOException ex) {
			instance.getPluginLogger().severe("Failed to load config %s".formatted(file), ex);
			throw new RuntimeException(ex);
		}
	}

	@Override
	public YamlDocument loadData(File file, char routeSeparator) {
		try (InputStream inputStream = new FileInputStream(file)) {
			return YamlDocument.create(inputStream,
					GeneralSettings.builder().setRouteSeparator(routeSeparator).build());
		} catch (IOException ex) {
			instance.getPluginLogger().severe("Failed to load config %s".formatted(file), ex);
			throw new RuntimeException(ex);
		}
	}

	public Map<String, Node<ConfigParserFunction>> getBlockFormatFunctions() {
		return blockFormatFunctions;
	}

	public Map<String, Node<ConfigParserFunction>> getEntityFormatFunctions() {
		return entityFormatFunctions;
	}

	public Map<String, Node<ConfigParserFunction>> getHookFormatFunctions() {
		return hookFormatFunctions;
	}

	public Map<String, Node<ConfigParserFunction>> getEventFormatFunctions() {
		return eventFormatFunctions;
	}

	public Map<String, Node<ConfigParserFunction>> getBaseEffectFormatFunctions() {
		return baseEffectFormatFunctions;
	}

	public Map<String, Node<ConfigParserFunction>> getEffectModifierFormatFunctions() {
		return effectModifierFormatFunctions;
	}

	public Map<String, Node<ConfigParserFunction>> getItemFormatFunctions() {
		return itemFormatFunctions;
	}

	public Map<String, Node<ConfigParserFunction>> getLootFormatFunctions() {
		return lootFormatFunctions;
	}

	public abstract List<Pair<String, WeightOperation>> parseWeightOperation(List<String> ops,
			Function<String, Boolean> validator, Function<String, List<String>> groupProvider);

	public abstract List<Pair<String, WeightOperation>> parseGroupWeightOperation(List<String> gops,
			boolean forAvailable, Function<String, List<String>> groupProvider);
}