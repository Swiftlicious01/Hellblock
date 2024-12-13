package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
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
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootBaseEffect;
import com.swiftlicious.hellblock.mechanics.hook.HookConfig;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;
import com.swiftlicious.hellblock.utils.extras.Tuple;

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
	protected int multipleLootSpawnDelay;
	protected boolean restrictedSizeRange;
	protected List<String> durabilityLore;
	protected EventPriority eventPriority;
	protected Requirement<Player>[] fishingRequirements;
	protected Requirement<Player>[] autoFishingRequirements;
	protected boolean baitAnimation;
	protected boolean antiAutoFishingMod;
	protected List<TriConsumer<Effect, Context<Player>, Integer>> globalEffects;

	protected boolean worldguardProtect;
	protected boolean perPlayerWorlds;
	protected boolean transferIslands;
	protected boolean linkHellblocks;
	protected boolean resetInventory;
	protected boolean resetEnderchest;
	protected boolean entryMessageEnabled;
	protected boolean farewellMessageEnabled;
	protected boolean disableBedExplosions;
	protected boolean growNaturalTrees;
	protected boolean voidTeleport;
	protected boolean asyncWorldSaving;
	protected String schematicPaster;
	protected String worldName;
	protected String chestInventoryName;
	protected String spawnCommand;
	protected String absoluteWorldPath;
	protected int partySize;
	protected int distance;
	protected int height;
	protected int protectionRange;
	protected int abandonTime;
	protected Set<IslandOptions> islandOptions = new HashSet<>();
	protected Map<Integer, Pair<Integer, CustomItem>> chestItems = new HashMap<>();

	protected Map<Integer, Tuple<Material, EntityType, Float>> levelSystem = new HashMap<>();

	protected boolean clearDefaultOutcome;
	protected Set<CustomItem> barteringItems = new HashSet<>();

	protected boolean randomStats;
	protected int randomMinHealth;
	protected int randomMaxHealth;
	protected double randomMinStrength;
	protected double randomMaxStrength;
	protected int defaultHealth;
	protected double defaultStrength;

	protected boolean infiniteLavaEnabled;

	protected boolean lavaRainEnabled;
	protected int radius;
	protected int fireChance;
	protected int delay;
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

	public boolean voidTeleport() {
		return voidTeleport;
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

	public int partySize() {
		return partySize;
	}

	public int protectionRange() {
		return protectionRange;
	}

	public int height() {
		return height;
	}

	public int distance() {
		return distance;
	}

	public int abandonAfterDays() {
		return abandonTime;
	}

	public Set<IslandOptions> islandOptions() {
		return islandOptions;
	}

	public Map<Integer, Tuple<Material, EntityType, Float>> levelSystem() {
		return levelSystem;
	}

	public boolean clearDefaultOutcome() {
		return clearDefaultOutcome;
	}

	public Set<CustomItem> barteringItems() {
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

	public boolean lavaRainEnabled() {
		return lavaRainEnabled;
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

	public EventPriority eventPriority() {
		return eventPriority;
	}

	public Requirement<Player>[] fishingRequirements() {
		return fishingRequirements;
	}

	public Requirement<Player>[] autoFishingRequirements() {
		return autoFishingRequirements;
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
				Node<ConfigParserFunction> functionNode = functionMap.get(nodes[i]);
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
				Node<ConfigParserFunction> functionNode = functionMap.get(nodes[i]);
				if (functionNode.nodeValue() != null) {
					throw new IllegalArgumentException("Format function '" + nodes[i] + "' already exists");
				}
				functionMap = functionNode.getChildTree();
			} else {
				if (i != nodes.length - 1) {
					Node<ConfigParserFunction> newNode = new Node<>();
					functionMap.put(nodes[i], newNode);
					functionMap = newNode.getChildTree();
				} else {
					functionMap.put(nodes[i], new Node<>(configParserFunction));
				}
			}
		}
	}

	protected Path resolveConfig(String filePath) {
		if (filePath == null || filePath.isEmpty()) {
			throw new IllegalArgumentException("ResourcePath cannot be null or empty");
		}
		filePath = filePath.replace('\\', '/');
		Path configFile = instance.getDataFolder().toPath().toAbsolutePath().resolve(filePath);
		// if the config doesn't exist, create it based on the template in the resources
		// dir
		if (!Files.exists(configFile)) {
			try {
				Files.createDirectories(configFile.getParent());
			} catch (IOException ignored) {
				// ignore
			}
			try (InputStream is = instance.getResource(filePath.replace("\\", "/"))) {
				if (is == null) {
					throw new IllegalArgumentException(
							String.format("The embedded resource '%s' cannot be found", filePath));
				}
				Files.copy(is, configFile);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		return configFile;
	}

	@Override
	public YamlDocument loadConfig(String filePath) {
		return loadConfig(filePath, '.');
	}

	@Override
	public YamlDocument loadConfig(String filePath, char routeSeparator) {
		try (InputStream inputStream = new FileInputStream(resolveConfig(filePath).toFile())) {
			return YamlDocument.create(inputStream, instance.getResource(filePath.replace("\\", "/")),
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
			instance.getPluginLogger().severe(String.format("Failed to load config %s", filePath), ex);
			throw new RuntimeException(ex);
		}
	}

	@Override
	public YamlDocument loadData(File file) {
		try (InputStream inputStream = new FileInputStream(file)) {
			return YamlDocument.create(inputStream);
		} catch (IOException ex) {
			instance.getPluginLogger().severe(String.format("Failed to load config %s", file), ex);
			throw new RuntimeException(ex);
		}
	}

	@Override
	public YamlDocument loadData(File file, char routeSeparator) {
		try (InputStream inputStream = new FileInputStream(file)) {
			return YamlDocument.create(inputStream,
					GeneralSettings.builder().setRouteSeparator(routeSeparator).build());
		} catch (IOException ex) {
			instance.getPluginLogger().severe(String.format("Failed to load config %s", file), ex);
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

	public abstract List<Pair<String, BiFunction<Context<Player>, Double, Double>>> parseWeightOperation(
			List<String> ops);

	public abstract List<Pair<String, BiFunction<Context<Player>, Double, Double>>> parseGroupWeightOperation(
			List<String> gops);
}