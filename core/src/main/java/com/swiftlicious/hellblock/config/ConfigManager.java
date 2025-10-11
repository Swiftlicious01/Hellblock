package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.constructor.ConstructorException;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.ConfigType;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.block.BlockDataModifier;
import com.swiftlicious.hellblock.creation.block.BlockDataModifierFactory;
import com.swiftlicious.hellblock.creation.block.BlockStateModifier;
import com.swiftlicious.hellblock.creation.block.BlockStateModifierFactory;
import com.swiftlicious.hellblock.creation.item.AbstractItem;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.creation.item.ItemEditor;
import com.swiftlicious.hellblock.creation.item.damage.CustomDurabilityItem;
import com.swiftlicious.hellblock.database.dependency.HellblockProperties;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.EffectProperties;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.EventManager;
import com.swiftlicious.hellblock.handlers.EventManagerInterface;
import com.swiftlicious.hellblock.handlers.ExpressionHelper;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.loot.LootInterface;
import com.swiftlicious.hellblock.loot.StatisticsKeys;
import com.swiftlicious.hellblock.loot.operation.AddWeightOperation;
import com.swiftlicious.hellblock.loot.operation.CustomWeightOperation;
import com.swiftlicious.hellblock.loot.operation.DivideWeightOperation;
import com.swiftlicious.hellblock.loot.operation.ModuloWeightOperation;
import com.swiftlicious.hellblock.loot.operation.MultiplyWeightOperation;
import com.swiftlicious.hellblock.loot.operation.ReduceWeightOperation;
import com.swiftlicious.hellblock.loot.operation.WeightOperation;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.schematic.ItemRegistry;
import com.swiftlicious.hellblock.utils.ItemStackUtils;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.OffsetUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.WeightUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;
import net.kyori.adventure.sound.Sound;

public final class ConfigManager extends ConfigHandler {

	private YamlDocument mainConfig;
	private YamlDocument guiConfig;
	private ConfigRegistry registry;

	public YamlDocument getMainConfig() {
		return mainConfig;
	}

	public YamlDocument getGuiConfig() {
		return guiConfig;
	}

	public ConfigRegistry getRegistry() {
		return registry;
	}

	private final Function<String, Boolean> lootValidator = (id) -> instance.getLootManager().getLoot(id).isPresent();

	public ConfigManager(HellblockPlugin plugin) {
		super(plugin);
		this.registerBuiltInItemProperties();
		this.registerBuiltInBaseEffectParser();
		this.registerBuiltInLootParser();
		this.registerBuiltInEntityParser();
		this.registerBuiltInEventParser();
		this.registerBuiltInEffectModifierParser();
		this.registerBuiltInHookParser();
		this.registerBuiltInBlockParser();
		this.registry = new ConfigRegistry(plugin);
	}

	@Override
	public void load() {
		final String configVersion = HellblockProperties.getValue("config");
		try (InputStream inputStream = new FileInputStream(resolveConfig("config.yml").toFile())) {
			mainConfig = YamlDocument.create(inputStream, instance.getResource("config.yml".replace("\\", "/")),
					GeneralSettings.builder().setRouteSeparator('.').setUseDefaults(false).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder().setScalarFormatter((tag, value, role, def) -> {
						if (role == NodeRole.KEY) {
							return ScalarStyle.PLAIN;
						} else {
							return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
						}
					}).build(),
					UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
							.addIgnoredRoute(configVersion, "worlds.settings._WORLDS_", '.')
							.addIgnoredRoute(configVersion, "lava-fishing-options.fishing-requirements", '.')
							.addIgnoredRoute(configVersion, "lava-fishing-options.auto-fishing-requirements", '.')
							.addIgnoredRoute(configVersion, "lava-fishing-options.global-events", '.')
							.addIgnoredRoute(configVersion, "lava-fishing-options.global-effects", '.')
							.addIgnoredRoute(configVersion, "lava-fishing-options.market.item-price", '.')
							.addIgnoredRoute(configVersion, "lava-fishing-options.market.sell-all-icons", '.')
							.addIgnoredRoute(configVersion, "lava-fishing-options.market.sell-icons", '.')
							.addIgnoredRoute(configVersion, "lava-fishing-options.market.decorative-icons", '.')
							.addIgnoredRoute(configVersion, "other-settings.placeholder-register", '.')
							.addIgnoredRoute(configVersion, "level-system.blocks", '.')
							.addIgnoredRoute(configVersion, "piglin-bartering.items", '.')
							.addIgnoredRoute(configVersion, "netherrack-generator-options.generation.blocks", '.')
							.addIgnoredRoute(configVersion, "hellblock.island-options", '.')
							.addIgnoredRoute(configVersion, "hellblock.starter-chest.items", '.').build());
			mainConfig.save(resolveConfig("config.yml").toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		this.loadSettings();
		this.loadConfigs();
		this.createGUIConfig();
		this.loadGlobalEffects();
		this.registry.load();
	}

	private void loadGlobalEffects() {
		final YamlDocument config = getMainConfig();
		globalEffects = new ArrayList<>();
		final Section globalEffectSection = config.getSection("lava-fishing-options.global-effects");
		if (globalEffectSection != null) {
			globalEffectSection.getStringRouteMappedValues(false).entrySet().stream()
					.filter(entry -> entry.getValue() instanceof Section).map(entry -> (Section) entry.getValue())
					.forEach(innerSection -> globalEffects
							.add(parseEffect(innerSection, id -> instance.getLootManager().getGroupMembers(id))));
		}
	}

	private void createGUIConfig() {
		try (InputStream inputStream = new FileInputStream(resolveConfig("gui.yml").toFile())) {
			guiConfig = YamlDocument.create(inputStream, instance.getResource("gui.yml".replace("\\", "/")),
					GeneralSettings.builder().setRouteSeparator('.').setUseDefaults(false).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder().setScalarFormatter((tag, value, role, def) -> {
						if (role == NodeRole.KEY) {
							return ScalarStyle.PLAIN;
						} else {
							return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
						}
					}).build());
			guiConfig.save(resolveConfig("gui.yml").toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void loadSettings() {
		final YamlDocument config = getMainConfig();

		instance.getTranslationManager()
				.forceLocale(instance.getTranslationManager().parseLocale(config.getString("force-locale", "")));
		AdventureHelper.legacySupport = config.getBoolean("other-settings.legacy-color-code-support", true);

		metrics = config.getBoolean("metrics", true);
		checkUpdate = config.getBoolean("update-checker", true);
		debug = config.getBoolean("debug", false);

		worldName = config.getString("general.worlds.world", "hellworld");
		spawnCommand = config.getString("general.spawn-command", "/spawn");
		if (!spawnCommand.startsWith("/")) {
			spawnCommand = "/spawn";
			instance.getPluginLogger().warn(
					"The defined general.spawn-command field in the config.yml was not a valid command. Defaulting to /spawn.");
		}
		absoluteWorldPath = config.getString("general.worlds.absolute-world-folder-path", "");
		asyncWorldSaving = config.getBoolean("other-settings.async-world-saving", true);
		perPlayerWorlds = config.getBoolean("general.worlds.per-player-worlds", false);

		transferIslands = config.getBoolean("hellblock.can-transfer-islands", true);
		disableGenerationAnimation = config.getBoolean("hellblock.disable-generation-animation", false);
		linkHellblocks = config.getBoolean("hellblock.can-link-hellblocks", true);
		schematicPaster = config.getString("hellblock.schematic-paster", "worldedit");
		config.getStringList("hellblock.island-options")
				.forEach(option -> islandOptions.add(IslandOptions.valueOf(option.toUpperCase(Locale.ENGLISH))));
		height = config.getInt("hellblock.height", 150);
		worldguardProtect = config.getBoolean("hellblock.use-worldguard-protection", false);
		resetInventory = config.getBoolean("hellblock.clear-inventory-on-reset", true);
		resetEnderchest = config.getBoolean("hellblock.clear-enderchest-on-reset", true);
		abandonTime = config.getInt("hellblock.abandon-after-days", 30);
		entryMessageEnabled = config.getBoolean("hellblock.entry-message-enabled", true);
		farewellMessageEnabled = config.getBoolean("hellblock.farewell-message-enabled", true);
		disableBedExplosions = config.getBoolean("hellblock.disable-bed-explosions", true);
		growNaturalTrees = config.getBoolean("hellblock.grow-natural-trees", false);
		useParticleBorder = config.getBoolean("hellblock.use-particle-border", false);
		voidTeleport = config.getBoolean("hellblock.void-teleport", true);
		lightningDeath = config.getBoolean("hellblock.lightning-on-death", true);

		maxBioCharLength = config.getInt("hellblock.display-settings.max-bio-character-length", 80);
		maxNameCharLength = config.getInt("hellblock.display-settings.max-name-character-length", 30);
		maxColorCodes = config.getInt("hellblock.display-settings.max-color-codes", 10);
		maxNewLines = config.getInt("hellblock.display-settings.max-new-lines", 3);
		wrapLength = config.getInt("hellblock.display-settings.wrap-length", 55);
		bannedWords = config.getStringList("hellblock.display-settings.banned-phrases", new ArrayList<>());

		chestInventoryName = config.getString("hellblock.starter-chest.inventory-name", "Chest");
		final Section chestItemsSection = config.getSection("hellblock.starter-chest.items");
		if (chestItemsSection != null) {
			for (Map.Entry<String, Object> entry : chestItemsSection.getStringRouteMappedValues(false).entrySet()) {
				if (StringUtils.isNotInteger(entry.getKey())) {
					continue;
				}
				final int itemID = Integer.parseInt(entry.getKey());
				if (entry.getValue() instanceof Section inner) {
					final int slot = inner.getInt("slot", RandomUtils.generateRandomInt(0, 26));
					final CustomItem item = new SingleItemParser("chest_item_" + itemID, inner,
							getItemFormatFunctions()).getItem();
					chestItems.putIfAbsent(itemID, Pair.of(slot, item));
				}
			}
		}

		final Section levelSystemSection = config.getSection("level-system.blocks");
		if (levelSystemSection != null) {
			int i = 0;
			for (Map.Entry<String, Object> entry : levelSystemSection.getStringRouteMappedValues(false).entrySet()) {
				final MathValue<Player> level = MathValue.auto(((Number) entry.getValue()).floatValue());
				final Material material;
				EntityType entity = null;
				if (entry.getKey().contains(":")) {
					final String[] split = entry.getKey().split(":");
					material = Material.matchMaterial(split[0].toUpperCase(Locale.ROOT));
					if (material != null && material == Material.SPAWNER) {
						entity = EntityType.valueOf(split[1].toUpperCase(Locale.ROOT));
					}
				} else {
					material = Material.matchMaterial(entry.getKey().toUpperCase(Locale.ROOT));
				}
				if (material != null && material.isBlock()) {
					levelSystem.putIfAbsent(i++, Tuple.of(material, entity, level));
				}
			}
		}

		clearDefaultOutcome = config.getBoolean("piglin-bartering.clear-default-outcome", true);
		final Section barteringSection = config.getSection("piglin-bartering.items");
		if (barteringSection != null) {
			for (Map.Entry<String, Object> entry : barteringSection.getStringRouteMappedValues(false).entrySet()) {
				if (StringUtils.isNotInteger(entry.getKey())) {
					continue;
				}
				final int itemID = Integer.parseInt(entry.getKey());
				if (entry.getValue() instanceof Section inner) {
					final CustomItem item = new SingleItemParser("barter_item_" + itemID, inner,
							getItemFormatFunctions()).getItem();
					final MathValue<Player> weight = MathValue.auto(inner.getInt("weight"));
					barteringItems.putIfAbsent(item, weight);
				}
			}
		}

		randomStats = config.getBoolean("wither-stats.random-stats", true);
		randomMinHealth = config.getInt("wither-stats.random-min-health", 200);
		randomMaxHealth = config.getInt("wither-stats.random-max-health", 200);
		if (randomMinHealth <= 0) {
			randomMinHealth = 200;
		}
		if (randomMaxHealth <= 0) {
			randomMaxHealth = 500;
		}
		randomMinStrength = config.getDouble("wither-stats.random-min-strength", 0.5);
		randomMaxStrength = config.getDouble("wither-stats.random-max-strength", 2.5);
		if (randomMinStrength <= 0) {
			randomMinStrength = 0.5;
		}
		if (randomMaxStrength <= 0) {
			randomMaxStrength = 2.5;
		}
		defaultStrength = config.getDouble("wither-stats.default-strength", 1.25);
		defaultHealth = config.getInt("wither-stats.default-health", 300);

		infiniteLavaEnabled = config.getBoolean("infinite-lava-options.enable", true);

		lavaRainEnabled = config.getBoolean("lava-rain-options.enable", true);
		warnPlayers = config.getBoolean("lava-rain-options.warn-players-beforehand", true);
		radius = Math.abs(config.getInt("lava-rain-options.radius", 16));
		fireChance = Math.abs(config.getInt("lava-rain-options.fire-chance", 1));
		delay = Math.abs(config.getInt("lava-rain-options.task-delay", 3));
		hurtCreatures = config.getBoolean("lava-rain-options.can-hurt-living-creatures", true);
		explodeTNT = config.getBoolean("lava-rain-options.will-tnt-explode", true);

		searchRadius = config.getDouble("netherrack-generator-options.player-search-radius", 4D);
		pistonAutomation = config.getBoolean("netherrack-generator-options.automation.pistons", false);
		final Section genResultSection = config.getSection("netherrack-generator-options.generation.blocks");
		if (genResultSection != null) {
			genResultSection.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
				final Material material = Material.matchMaterial(entry.getKey().toUpperCase(Locale.ROOT));
				final MathValue<Player> chance = MathValue.auto(entry.getValue());
				if (material != null) {
					generationResults.putIfAbsent(material, chance);
				}
			});
		}

		lavaFishingEnabled = config.getBoolean("lava-fishing-options.enable", true);
		lavaMinTime = config.getInt("lava-fishing-options.lava-fishing.min-wait-time", 100);
		if (lavaMinTime < 0) {
			lavaMinTime = 0;
		}
		lavaMaxTime = config.getInt("lava-fishing-options.lava-fishing.max-wait-time", 600);
		if (lavaMaxTime < 0) {
			lavaMaxTime = 0;
		}
		finalLavaMinTime = config.getInt("lava-fishing-options.final-min-wait-time", 50);
		finalLavaMaxTime = config.getInt("lava-fishing-options.final-max-wait-time", 1200);

		restrictedSizeRange = config.getBoolean("lava-fishing-options.size.restricted-size-range", true);

		placeholderLimit = config.getInt("general.redis-synchronization.placeholder-limit", 3);
		serverGroup = config.getString("general.redis-synchronization.server-group", "default");
		redisRanking = config.getBoolean("general.redis-synchronization.redis-ranking", false);

		dataSaveInterval = config.getInt("other-settings.data-saving-interval", 600);
		logDataSaving = config.getBoolean("other-settings.log-data-saving", true);
		lockData = config.getBoolean("other-settings.lock-data", true);

		durabilityLore = new ArrayList<>(config.getStringList("other-settings.custom-durability-format").stream()
				.map(it -> "<!i>" + it).toList());

		magmaWalkerBookName = config.getString("other-settings.magma-walker.enchantment-book-format.name",
				"Book of Magma Walker {level}");
		magmaWalkerBookLore = new ArrayList<>(
				config.getStringList("other-settings.magma-walker.enchantment-book-format.lore").stream()
						.map(it -> "<!i>" + it).toList());

		magmaWalkerBootsLore = new ArrayList<>(config.getStringList("other-settings.magma-walker.boots-format").stream()
				.map(it -> "<!i>" + it).toList());

		final Section challengeSoundSection = config.getSection("hellblock.challenge-completed-sound");
		if (challengeSoundSection != null) {
			challengeCompletedSound = Sound.sound(
					net.kyori.adventure.key.Key.key(challengeSoundSection.getString("key")),
					Sound.Source
							.valueOf(challengeSoundSection.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH)),
					challengeSoundSection.getFloat("volume", 1.0F).floatValue(),
					challengeSoundSection.getFloat("pitch", 1.0F).floatValue());
		}

		final Section linkingSoundSection = config.getSection("hellblock.linking-hellblock-sound");
		if (linkingSoundSection != null) {
			linkingHellblockSound = Sound.sound(net.kyori.adventure.key.Key.key(linkingSoundSection.getString("key")),
					Sound.Source.valueOf(linkingSoundSection.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH)),
					linkingSoundSection.getFloat("volume", 1.0F).floatValue(),
					linkingSoundSection.getFloat("pitch", 1.0F).floatValue());
		}

		final Section creatingSoundSection = config.getSection("hellblock.creating-hellblock-sound");
		if (creatingSoundSection != null) {
			creatingHellblockSound = Sound.sound(net.kyori.adventure.key.Key.key(creatingSoundSection.getString("key")),
					Sound.Source
							.valueOf(creatingSoundSection.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH)),
					creatingSoundSection.getFloat("volume", 1.0F).floatValue(),
					creatingSoundSection.getFloat("pitch", 1.0F).floatValue());
		}

		final Section titleScreenSection = config.getSection("hellblock.creation-title-screen");
		if (titleScreenSection != null) {
			final boolean enabled = titleScreenSection.getBoolean("enable", true);
			final String title = titleScreenSection.getString("title");
			final String subtitle = titleScreenSection.getString("subtitle");
			final int fadeIn = titleScreenSection.getInt("fadeIn", 3) * 20;
			final int stay = titleScreenSection.getInt("stay", 2) * 20;
			final int fadeOut = titleScreenSection.getInt("fadeOut", 3) * 20;
			creationTitleScreen = new TitleScreenInfo(enabled, title, subtitle, fadeIn, stay, fadeOut);
		}

		itemDetectOrder = config.getStringList("other-settings.item-detection-order").toArray(new String[0]);
		blockDetectOrder = config.getStringList("other-settings.block-detection-order").toArray(new String[0]);

		eventPriority = EventPriority
				.valueOf(config.getString("other-settings.event-priority", "NORMAL").toUpperCase(Locale.ENGLISH));

		antiAutoFishingMod = config.getBoolean("other-settings.anti-auto-fishing-mod", false);

		fishingRequirements = instance.getRequirementManager(Player.class)
				.parseRequirements(config.getSection("lava-fishing-options.fishing-requirements"), true);
		autoFishingRequirements = instance.getRequirementManager(Player.class)
				.parseRequirements(config.getSection("lava-fishing-options.auto-fishing-requirements"), true);

		baitAnimation = config.getBoolean("lava-fishing-options.show-bait-animation", true);

		multipleLootSpawnDelay = config.getInt("lava-fishing-options.multiple-loot-spawn-delay", 4);

		LootInterface.DefaultProperties.DEFAULT_DISABLE_STATS = config
				.getBoolean("lava-fishing-options.global-loot-property.disable-stat", false);
		LootInterface.DefaultProperties.DEFAULT_SHOW_IN_FINDER = config
				.getBoolean("lava-fishing-options.global-loot-property.show-in-fishfinder", true);

		final Section placeholderSection = config.getSection("other-settings.placeholder-register");
		if (placeholderSection != null) {
			placeholderSection.getStringRouteMappedValues(false).entrySet().stream()
					.filter(entry -> entry.getValue() instanceof String).forEach(entry -> {
						final String original = (String) entry.getValue();
						instance.getPlaceholderManager().registerCustomPlaceholder(entry.getKey(), original);
					});
		}

		OffsetUtils.load(config.getSection("other-settings.offset-characters"));

		EventManagerInterface.GLOBAL_ACTIONS.clear();
		EventManagerInterface.GLOBAL_TIMES_ACTION.clear();
		final Section globalEvents = config.getSection("lava-fishing-options.global-events");
		if (globalEvents != null) {
			globalEvents.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
				final MechanicType type = MechanicType.index().value(entry.getKey());
				if (entry.getValue() instanceof Section inner) {
					final Map<ActionTrigger, Action<Player>[]> actionMap = new HashMap<>();
					final Map<ActionTrigger, TreeMap<Integer, Action<Player>[]>> actionTimesMap = new HashMap<>();
					inner.getStringRouteMappedValues(false).entrySet().forEach(innerEntry -> {
						if (innerEntry.getValue() instanceof Section actionSection) {
							final String trigger = innerEntry.getKey().toUpperCase(Locale.ENGLISH);
							if ("SUCCESS_TIMES".equals(trigger) || "SUCCESS-TIMES".equals(trigger)) {
								actionTimesMap.put(ActionTrigger.SUCCESS,
										instance.getActionManager(Player.class).parseTimesActions(actionSection));
							} else {
								actionMap.put(ActionTrigger.valueOf(trigger),
										instance.getActionManager(Player.class).parseActions(actionSection));
							}
						}
					});
					EventManager.GLOBAL_TIMES_ACTION.put(type, actionTimesMap);
				}
			});
		}
	}

	@Override
	public void saveResource(String filePath) {
		if (!new File(instance.getDataFolder(), filePath).exists()) {
			instance.saveResource(filePath, false);
		}
	}

	private void loadConfigs() {
		final Deque<File> fileDeque = new ArrayDeque<>();
		for (ConfigType type : ConfigType.values()) {
			final File typeFolder = new File(instance.getDataFolder(), "contents" + File.separator + type.path());
			if (!typeFolder.exists()) {
				if (!typeFolder.mkdirs()) {
					return;
				}
				instance.saveResource("contents" + File.separator + type.path() + File.separator + "default.yml",
						false);
			}
			final Map<String, Node<ConfigParserFunction>> nodes = type.parser();
			fileDeque.push(typeFolder);
			while (!fileDeque.isEmpty()) {
				final File file = fileDeque.pop();
				final File[] files = file.listFiles();
				if (files == null) {
					continue;
				}
				for (File subFile : files) {
					if (subFile.isDirectory()) {
						fileDeque.push(subFile);
					} else if (subFile.isFile() && subFile.getName().endsWith(".yml")) {
						try {
							final YamlDocument document = loadData(subFile);
							document.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
								try {
									if (entry.getValue() instanceof Section section) {
										type.parse(entry.getKey(), section, nodes);
									}
								} catch (Exception e) {
									instance.getPluginLogger().warn("Invalid config " + subFile.getPath()
											+ " - Failed to parse section " + entry.getKey(), e);
								}
							});
						} catch (ConstructorException e) {
							instance.getPluginLogger().warn("Could not load config file: " + subFile.getAbsolutePath()
									+ ". Is it a corrupted file?");
						}
					}
				}
			}
		}
	}

	public Map<ItemStack, Character> getCraftingMaterials(Section section) {
		final Map<ItemStack, Character> map = new HashMap<>();
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (!(entry.getValue() instanceof Character)) {
				continue;
			}
			final char symbol = (char) entry.getValue();
			// SPECIAL CASE
			if ("NETHER_POTION".equalsIgnoreCase(entry.getKey())) {
				map.put(instance.getNetherBrewingHandler().getNetherPotion().load(), symbol);
			} else {
				final Material material = Material.matchMaterial(entry.getKey().toUpperCase(Locale.ROOT));
				if (material != null) {
					map.put(new ItemStack(material), symbol);
				}
			}
		}
		return map;
	}

	public Map<Key, Short> getEnchantments(Section section) {
		final Map<Key, Short> map = new HashMap<>();
		section.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
			final int level = Math.min(255, Math.max(1, (int) entry.getValue()));
			if (ItemRegistry.getEnchantment(entry.getKey()) != null) {
				map.put(Key.fromString(entry.getKey()), (short) level);
			}
		});
		return map;
	}

	private List<Tuple<Double, String, Short>> getPossibleEnchantments(Section section) {
		final List<Tuple<Double, String, Short>> list = new ArrayList<>();
		section.getStringRouteMappedValues(false).entrySet().stream()
				.filter(entry -> entry.getValue() instanceof Section).map(entry -> (Section) entry.getValue())
				.forEach(inner -> {
					final Tuple<Double, String, Short> tuple = Tuple.of(inner.getDouble("chance"),
							inner.getString("enchant"), Short.valueOf(String.valueOf(inner.getInt("level"))));
					list.add(tuple);
				});
		return list;
	}

	private Pair<Key, Short> getEnchantmentPair(String enchantmentWithLevel) {
		final String[] split = enchantmentWithLevel.split(":", 3);
		return Pair.of(Key.of(split[0], split[1]), Short.parseShort(split[2]));
	}

	@SuppressWarnings("unchecked")
	private void registerBuiltInItemProperties() {
		final Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> {
			final Section section = (Section) arg;
			final boolean stored = Objects.equals(section.getNameAsString(), "stored-enchantment-pool");
			final Section amountSection = section.getSection("amount");
			final Section enchantSection = section.getSection("pool");
			final List<Pair<Integer, MathValue<Player>>> amountList = new ArrayList<>();
			amountSection.getStringRouteMappedValues(false).entrySet().forEach(entry -> amountList
					.add(Pair.of(Integer.parseInt(entry.getKey()), MathValue.auto(entry.getValue()))));
			final List<Pair<Pair<Key, Short>, MathValue<Player>>> enchantPoolPair = new ArrayList<>();
			enchantSection.getStringRouteMappedValues(false).entrySet().forEach(entry -> enchantPoolPair
					.add(Pair.of(getEnchantmentPair(entry.getKey()), MathValue.auto(entry.getValue()))));
			if (amountList.isEmpty() || enchantPoolPair.isEmpty()) {
				throw new RuntimeException("Both `pool` and `amount` should not be empty");
			}
			return (item, context) -> {
				final List<Pair<Integer, Double>> parsedAmountPair = new ArrayList<>(amountList.size());
				amountList.forEach(
						rawValue -> parsedAmountPair.add(Pair.of(rawValue.left(), rawValue.right().evaluate(context))));
				final int amount = WeightUtils.getRandom(parsedAmountPair);
				if (amount <= 0) {
					return;
				}
				final Set<Enchantment> addedEnchantments = new HashSet<>();
				final List<Pair<Pair<Key, Short>, Double>> cloned = new ArrayList<>(enchantPoolPair.size());
				enchantPoolPair
						.forEach(rawValue -> cloned.add(Pair.of(rawValue.left(), rawValue.right().evaluate(context))));
				int i = 0;
				outer: while (i < amount && !cloned.isEmpty()) {
					final Pair<Key, Short> enchantPair = WeightUtils.getRandom(cloned);
					final Enchantment enchantment = ItemRegistry.getEnchantment(enchantPair.left().toString());
					if (enchantment == null) {
						instance.getPluginLogger().warn("Enchantment: " + enchantPair.left() + " doesn't exist.");
						return;
					}
					if (!stored) {
						for (Enchantment added : addedEnchantments) {
							if (enchantment.conflictsWith(added)) {
								cloned.removeIf(pair -> pair.left().left().equals(enchantPair.left()));
								continue outer;
							}
						}
					}
					if (stored) {
						item.addStoredEnchantment(enchantPair.left(), enchantPair.right());
					} else {
						item.addEnchantment(enchantPair.left(), enchantPair.right());
					}
					addedEnchantments.add(enchantment);
					cloned.removeIf(pair -> pair.left().left().equals(enchantPair.left()));
					i++;
				}
			};
		};
		this.registerItemParser(f1, 4800, "stored-enchantment-pool");
		this.registerItemParser(f1, 4700, "enchantment-pool");
		final Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f2 = arg -> {
			final Section section = (Section) arg;
			final boolean stored = Objects.equals(section.getNameAsString(), "random-stored-enchantments");
			final List<Tuple<Double, String, Short>> enchantments = getPossibleEnchantments(section);
			return (item, context) -> {
				final Set<String> ids = new HashSet<>();
				enchantments.stream().filter(pair -> Math.random() < pair.left() && !ids.contains(pair.mid()))
						.forEach(pair -> {
							if (stored) {
								item.addStoredEnchantment(Key.fromString(pair.mid()), pair.right());
							} else {
								item.addEnchantment(Key.fromString(pair.mid()), pair.right());
							}
							ids.add(pair.mid());
						});
			};
		};
		this.registerItemParser(f2, 4850, "random-stored-enchantments");
		this.registerItemParser(f2, 4750, "random-enchantments");
		this.registerItemParser(arg -> {
			final Section section = (Section) arg;
			final Map<Key, Short> map = getEnchantments(section);
			return (item, context) -> item.storedEnchantments(map);
		}, 4600, "stored-enchantments");
		this.registerItemParser(arg -> {
			final Section section = (Section) arg;
			final Map<Key, Short> map = getEnchantments(section);
			return (item, context) -> item.enchantments(map);
		}, 4500, "enchantments");
		this.registerItemParser(arg -> {
			final String base64 = (String) arg;
			return (item, context) -> item.skull(base64);
		}, 5200, "head64");
		this.registerItemParser(arg -> {
			final boolean glint = (boolean) arg;
			return (item, context) -> item.glint(glint);
		}, 5500, "glowing");
		this.registerItemParser(arg -> {
			final List<String> args = ListUtils.toList(arg);
			return (item, context) -> item.itemFlags(args);
		}, 5100, "item-flags");
		this.registerItemParser(arg -> {
			final MathValue<Player> mathValue = MathValue.auto(arg);
			return (item, context) -> item.customModelData((int) mathValue.evaluate(context));
		}, 5000, "custom-model-data");
		this.registerItemParser(arg -> {
			final TextValue<Player> textValue = TextValue.auto("<!i><white>" + arg);
			return (item, context) -> item.displayName(AdventureHelper.miniMessageToJson(textValue.render(context)));
		}, 4000, "display", "name");
		this.registerItemParser(arg -> {
			final List<String> list = ListUtils.toList(arg);
			final List<TextValue<Player>> lore = new ArrayList<>();
			list.forEach(text -> lore.add(TextValue.auto("<!i><white>" + text)));
			return (item, context) -> item
					.lore(lore.stream().map(it -> AdventureHelper.miniMessageToJson(it.render(context))).toList());
		}, 3_000, "display", "lore");
		this.registerItemParser(arg -> {
			final boolean enable = (boolean) arg;
			return (item, context) -> {
				if (!enable) {
					return;
				}
				item.setTag(context.arg(ContextKeys.ID), "HellblockItem", "id");
			};
		}, 2_000, "tag");
		this.registerItemParser(arg -> {
			final boolean enable = (boolean) arg;
			return (item, context) -> item.unbreakable(enable);
		}, 2_211, "unbreakable");
		this.registerItemParser(arg -> {
			final boolean enable = (boolean) arg;
			return (item, context) -> {
				if (enable) {
					return;
				}
				item.setTag(UUID.randomUUID(), "HellblockItem", "uuid");
			};
		}, 2_222, "stackable");
		this.registerItemParser(arg -> {
			final boolean enable = (boolean) arg;
			return (item, context) -> item.setTag(enable ? 1 : 0, "HellblockItem", "placeable");
		}, 2_335, "placeable");
		this.registerItemParser(arg -> {
			final String sizePair = (String) arg;
			final String[] split = sizePair.split("~", 2);
			final MathValue<Player> min = MathValue.auto(split[0]);
			final MathValue<Player> max = split.length == 2 ? MathValue.auto(split[1]) : MathValue.auto(split[0]);
			return (item, context) -> {
				final double minSize = min.evaluate(context);
				final double maxSize = max.evaluate(context);
				float size = (float) RandomUtils.generateRandomDouble(minSize, maxSize);
				Double sm = context.arg(ContextKeys.SIZE_MULTIPLIER);
				if (sm == null) {
					sm = 1.0;
				}
				Double sa = context.arg(ContextKeys.SIZE_ADDER);
				if (sa == null) {
					sa = 0.0;
				}
				size = (float) (sm * size + sa);
				if (this.restrictedSizeRange()) {
					if (size > maxSize) {
						size = (float) maxSize;
					}
					if (size < minSize) {
						size = (float) minSize;
					}
				}
				item.setTag(size, "HellblockItem", "size");
				context.arg(ContextKeys.SIZE, size);
				context.arg(ContextKeys.MIN_SIZE, minSize);
				context.arg(ContextKeys.MAX_SIZE, maxSize);
				context.arg(ContextKeys.SIZE_FORMATTED, "%.2f".formatted(size));
			};
		}, 1_000, "size");
		this.registerItemParser(arg -> {
			final Section section = (Section) arg;
			final MathValue<Player> base = MathValue.auto(section.get("base", "0"));
			final MathValue<Player> bonus = MathValue.auto(section.get("bonus", "0"));
			return (item, context) -> {
				final double basePrice = base.evaluate(context);
				context.arg(ContextKeys.BASE, basePrice);
				final double bonusPrice = bonus.evaluate(context);
				context.arg(ContextKeys.BONUS, bonusPrice);
				final String formula = instance.getMarketManager().getFormula();
				final TextValue<Player> playerTextValue = TextValue.auto(formula);
				String rendered = playerTextValue.render(context);
				final List<String> unparsed = instance.getPlaceholderManager().resolvePlaceholders(rendered);
				for (String unparsedValue : unparsed) {
					rendered = rendered.replace(unparsedValue, "0");
				}
				final double price = ExpressionHelper.evaluate(rendered);
				item.setTag(price, "Price");
				context.arg(ContextKeys.PRICE, price);
				context.arg(ContextKeys.PRICE_FORMATTED, "%.2f".formatted(price));
			};
		}, 1_500, "price");
		this.registerItemParser(arg -> {
			final boolean random = (boolean) arg;
			return (item, context) -> {
				if (!random) {
					return;
				}
				if (item.hasTag("HellblockItem", "max_dur")) {
					final CustomDurabilityItem durabilityItem = new CustomDurabilityItem(item);
					durabilityItem.damage(RandomUtils.generateRandomInt(0, durabilityItem.maxDamage() - 1));
				} else {
					item.damage(RandomUtils.generateRandomInt(0, item.maxDamage().get() - 1));
				}
			};
		}, 3200, "random-durability");
		this.registerItemParser(arg -> {
			final MathValue<Player> mathValue = MathValue.auto(arg);
			return (item, context) -> {
				final int max = (int) mathValue.evaluate(context);
				item.setTag(max, "HellblockItem", "max_dur");
				item.setTag(max, "HellblockItem", "cur_dur");
				final CustomDurabilityItem customDurabilityItem = new CustomDurabilityItem(item);
				customDurabilityItem.damage(0);
			};
		}, 3100, "max-durability");
		this.registerItemParser(arg -> {
			final Section section = (Section) arg;
			final List<ItemEditor> editors = new ArrayList<>();
			ItemStackUtils.sectionToTagEditor(section, editors);
			return (item, context) -> editors
					.forEach(editor -> editor.apply(((AbstractItem<RtagItem, ItemStack>) item).getRTagItem(), context));
		}, 10_050, "nbt");
		if (VersionHelper.isVersionNewerThan1_20_5()) {
			this.registerItemParser(arg -> {
				final Section section = (Section) arg;
				final List<ItemEditor> editors = new ArrayList<>();
				ItemStackUtils.sectionToComponentEditor(section, editors);
				return (item, context) -> editors.forEach(
						editor -> editor.apply(((AbstractItem<RtagItem, ItemStack>) item).getRTagItem(), context));
			}, 10_075, "components");
		}
	}

	private void registerBuiltInEffectModifierParser() {
		this.registerEffectModifierParser(object -> {
			final Section section = (Section) object;
			return builder -> builder.requirements(
					List.of(instance.getRequirementManager(Player.class).parseRequirements(section, true)));
		}, "requirements");
		this.registerEffectModifierParser(object -> {
			final Section section = (Section) object;
			final List<TriConsumer<Effect, Context<Player>, Integer>> property = new ArrayList<>();
			section.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
				if (entry.getValue() instanceof Section innerSection) {
					property.add(parseEffect(innerSection, id -> instance.getLootManager().getGroupMembers(id)));
				}
			});
			return builder -> builder.modifiers(property);
		}, "effects");
	}

	public TriConsumer<Effect, Context<Player>, Integer> parseEffect(Section section,
			Function<String, List<String>> groupProvider) {
		if (!section.contains("type")) {
			throw new RuntimeException(section.getRouteAsString());
		}
		final Action<Player>[] actions = instance.getActionManager(Player.class)
				.parseActions(section.getSection("actions"));
		final String type = section.getString("type");
		if (type == null) {
			throw new RuntimeException(section.getRouteAsString());
		}
		switch (type) {
		case "lava-fishing" -> {
			return (((effect, context, phase) -> {
				if (phase == 0) {
					effect.properties().put(EffectProperties.LAVA_FISHING, true);
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "weight-mod" -> {
			final var op = parseWeightOperation(section.getStringList("value"), lootValidator, groupProvider);
			return (((effect, context, phase) -> {
				if (phase == 1) {
					effect.weightOperations(op);
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "weight-mod-ignore-conditions" -> {
			final var op = parseWeightOperation(section.getStringList("value"), lootValidator, groupProvider);
			return (((effect, context, phase) -> {
				if (phase == 1) {
					effect.weightOperationsIgnored(op);
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "group-mod", "group_mod" -> {
			final var op = parseGroupWeightOperation(section.getStringList("value"), true, groupProvider);
			return (((effect, context, phase) -> {
				if (phase == 1) {
					effect.weightOperations(op);
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "group-mod-ignore-conditions", "group_mod_ignore_conditions" -> {
			final var op = parseGroupWeightOperation(section.getStringList("value"), false, groupProvider);
			return (((effect, context, phase) -> {
				if (phase == 1) {
					effect.weightOperationsIgnored(op);
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "wait-time", "wait_time" -> {
			final MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.waitTimeAdder(effect.waitTimeAdder() + value.evaluate(context));
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "hook-time", "hook_time", "wait-time-multiplier", "wait_time_multiplier" -> {
			final MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.waitTimeMultiplier(effect.waitTimeMultiplier() - 1 + value.evaluate(context));
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "size" -> {
			final MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.sizeAdder(effect.sizeAdder() + value.evaluate(context));
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "size-multiplier", "size-bonus" -> {
			final MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.sizeMultiplier(effect.sizeMultiplier() - 1 + value.evaluate(context));
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "multiple-loot" -> {
			final MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.multipleLootChance(effect.multipleLootChance() + value.evaluate(context));
					ActionManager.trigger(context, actions);
				}
			}));
		}
		case "conditional" -> {
			final Requirement<Player>[] requirements = instance.getRequirementManager(Player.class)
					.parseRequirements(section.getSection("conditions"), true);
			final Section effectSection = section.getSection("effects");
			final List<TriConsumer<Effect, Context<Player>, Integer>> effects = new ArrayList<>();
			if (effectSection != null) {
				effectSection.getStringRouteMappedValues(false).entrySet().stream()
						.filter(entry -> entry.getValue() instanceof Section).map(entry -> (Section) entry.getValue())
						.forEach(inner -> effects.add(parseEffect(inner, groupProvider)));
			}
			return (((effect, context, phase) -> {
				if (!RequirementManager.isSatisfied(context, requirements)) {
					return;
				}
				effects.forEach(consumer -> consumer.accept(effect, context, phase));
			}));
		}
		default -> {
			return (((effect, context, phase) -> {
			}));
		}
		}
	}

	private WeightOperation parseSharedGroupWeight(String op, int memberCount, boolean forAvailable,
			Function<String, List<String>> groupProvider) {
		switch (op.charAt(0)) {
		case '-' -> {
			final MathValue<Player> arg = MathValue.auto(op.substring(1));
			return new ReduceWeightOperation(arg, memberCount, forAvailable);
		}
		case '+' -> {
			final MathValue<Player> arg = MathValue.auto(op.substring(1));
			return new AddWeightOperation(arg, memberCount, forAvailable);
		}
		case '=' -> {
			final String expression = op.substring(1);
			final MathValue<Player> arg = MathValue.auto(expression);
			final List<String> placeholders = instance.getPlaceholderManager().resolvePlaceholders(expression);
			final List<String> otherEntries = new ArrayList<>();
			final List<Pair<String, String[]>> otherGroups = new ArrayList<>();
			for (String placeholder : placeholders) {
				if (placeholder.startsWith("{entry_")) {
					otherEntries.add(placeholder.substring("{entry_".length(), placeholder.length() - 1));
				} else if (placeholder.startsWith("{group")) {
					// only for loots
					final String groupExpression = placeholder.substring("{group_".length(), placeholder.length() - 1);
					final List<String> members = getGroupMembers(groupExpression, groupProvider);
					if (members.isEmpty()) {
						instance.getPluginLogger().warn(
								"Failed to load expression: " + expression + ". Invalid group: " + groupExpression);
						continue;
					}
					otherGroups.add(Pair.of(groupExpression, members.toArray(new String[0])));
				}
			}
			return new CustomWeightOperation(arg, expression.contains("{1}"), otherEntries, otherGroups, memberCount,
					forAvailable);
		}
		default -> throw new IllegalArgumentException("Invalid shared weight operation: " + op);
		}
	}

	private WeightOperation parseWeightOperation(String op, boolean forAvailable,
			Function<String, List<String>> groupProvider) {
		switch (op.charAt(0)) {
		case '/' -> {
			final MathValue<Player> arg = MathValue.auto(op.substring(1));
			return new DivideWeightOperation(arg, forAvailable);
		}
		case '*' -> {
			final MathValue<Player> arg = MathValue.auto(op.substring(1));
			return new MultiplyWeightOperation(arg, forAvailable);
		}
		case '-' -> {
			final MathValue<Player> arg = MathValue.auto(op.substring(1));
			return new ReduceWeightOperation(arg, 1, forAvailable);
		}
		case '%' -> {
			final MathValue<Player> arg = MathValue.auto(op.substring(1));
			return new ModuloWeightOperation(arg, forAvailable);
		}
		case '+' -> {
			final MathValue<Player> arg = MathValue.auto(op.substring(1));
			return new AddWeightOperation(arg, 1, forAvailable);
		}
		case '=' -> {
			final String expression = op.substring(1);
			final MathValue<Player> arg = MathValue.auto(expression);
			final List<String> placeholders = instance.getPlaceholderManager().resolvePlaceholders(expression);
			final List<String> otherEntries = new ArrayList<>();
			final List<Pair<String, String[]>> otherGroups = new ArrayList<>();
			for (String placeholder : placeholders) {
				if (placeholder.startsWith("{entry_")) {
					otherEntries.add(placeholder.substring("{entry_".length(), placeholder.length() - 1));
				} else if (placeholder.startsWith("{group")) {
					// only for loots
					final String groupExpression = placeholder.substring("{group_".length(), placeholder.length() - 1);
					final List<String> members = getGroupMembers(groupExpression, groupProvider);
					if (members.isEmpty()) {
						instance.getPluginLogger().warn(
								"Failed to load expression: " + expression + ". Invalid group: " + groupExpression);
						continue;
					}
					otherGroups.add(Pair.of(groupExpression, members.toArray(new String[0])));
				}
			}
			return new CustomWeightOperation(arg, expression.contains("{1}"), otherEntries, otherGroups, 1,
					forAvailable);
		}
		default -> throw new IllegalArgumentException("Invalid weight operation: " + op);
		}
	}

	@Override
	public List<Pair<String, WeightOperation>> parseWeightOperation(List<String> ops,
			Function<String, Boolean> validator, Function<String, List<String>> groupProvider) {
		final List<Pair<String, WeightOperation>> result = new ArrayList<>();
		for (String op : ops) {
			final String[] split = op.split(":", 3);
			if (split.length < 2) {
				instance.getPluginLogger().warn("Illegal weight operation: " + op);
				continue;
			}
			if (split.length == 2) {
				final String id = split[0];
				if (!validator.apply(id)) {
					instance.getPluginLogger().warn("Illegal weight operation: " + op + ". Id " + id + " is not valid");
					continue;
				}
				result.add(Pair.of(id, parseWeightOperation(split[1], false, groupProvider)));
			} else {
				final String type = split[0];
				final String id = split[1];
				switch (type) {
				case "group_for_each" -> {
					final List<String> members = getGroupMembers(id, groupProvider);
					if (members.isEmpty()) {
						instance.getPluginLogger().warn("Failed to load expression: " + op + ". Invalid group: " + id);
						continue;
					}
					final WeightOperation operation = parseWeightOperation(split[2], false, groupProvider);
					members.forEach(member -> result.add(Pair.of(member, operation)));
				}
				case "group_available_for_each" -> {
					final List<String> members = getGroupMembers(id, groupProvider);
					if (members.isEmpty()) {
						instance.getPluginLogger().warn("Failed to load expression: " + op + ". Invalid group: " + id);
						continue;
					}
					final WeightOperation operation = parseWeightOperation(split[2], true, groupProvider);
					members.forEach(member -> result.add(Pair.of(member, operation)));
				}
				case "group_total" -> {
					final List<String> members = getGroupMembers(id, groupProvider);
					if (members.isEmpty()) {
						instance.getPluginLogger().warn("Failed to load expression: " + op + ". Invalid group: " + id);
						continue;
					}
					final WeightOperation operation = parseSharedGroupWeight(split[2], members.size(), false,
							groupProvider);
					members.forEach(member -> result.add(Pair.of(member, operation)));
				}
				case "group_available_total" -> {
					final List<String> members = getGroupMembers(id, groupProvider);
					if (members.isEmpty()) {
						instance.getPluginLogger().warn("Failed to load expression: " + op + ". Invalid group: " + id);
						continue;
					}
					final WeightOperation operation = parseSharedGroupWeight(split[2], members.size(), true,
							groupProvider);
					members.forEach(member -> result.add(Pair.of(member, operation)));
				}
				case "loot_available" -> {
					if (!validator.apply(id)) {
						instance.getPluginLogger()
								.warn("Illegal weight operation: " + op + ". Id " + id + " is not valid");
						continue;
					}
					result.add(Pair.of(id, parseWeightOperation(split[2], true, groupProvider)));
				}
				default -> {
					if (!validator.apply(id)) {
						instance.getPluginLogger()
								.warn("Illegal weight operation: " + op + ". Id " + id + " is not valid");
						continue;
					}
					result.add(Pair.of(id, parseWeightOperation(split[2], false, groupProvider)));
				}
				}
			}
		}
		return result;
	}

	@Override
	public List<Pair<String, WeightOperation>> parseGroupWeightOperation(List<String> gops, boolean forAvailable,
			Function<String, List<String>> groupProvider) {
		final List<Pair<String, WeightOperation>> result = new ArrayList<>();
		for (String gop : gops) {
			final String[] split = gop.split(":", 2);
			if (split.length < 2) {
				instance.getPluginLogger().warn("Illegal weight operation: " + gop);
				continue;
			}
			final WeightOperation operation = parseWeightOperation(split[1], forAvailable, groupProvider);
			final String groupExpression = split[0];
			getGroupMembers(groupExpression, groupProvider).forEach(member -> result.add(Pair.of(member, operation)));
		}
		return result;
	}

	private List<String> getGroupMembers(String groupExpression, Function<String, List<String>> groupProvider) {
		if (groupExpression.contains("&")) {
			final String[] groups = groupExpression.split("&");
			final List<Set<String>> groupSets = new ArrayList<>();
			for (String group : groups) {
				groupSets.add(new HashSet<>(groupProvider.apply(group)));
			}
			final Set<String> intersection = groupSets.get(0);
			for (int i = 1; i < groupSets.size(); i++) {
				intersection.retainAll(groupSets.get(i));
			}
			return new ArrayList<>(intersection);
		} else if (groupExpression.contains("|")) {
			final Set<String> members = new HashSet<>();
			final String[] groups = groupExpression.split("\\|");
			for (String group : groups) {
				members.addAll(groupProvider.apply(group));
			}
			return new ArrayList<>(members);
		} else {
			return groupProvider.apply(groupExpression);
		}
	}

	private void registerBuiltInHookParser() {
		this.registerHookParser(object -> {
			final List<String> lore = ListUtils.toList(object);
			return builder -> builder.lore(lore.stream().map(it -> "<!i>" + it).toList());
		}, "lore-on-rod");
	}

	private void registerBuiltInBaseEffectParser() {
		this.registerBaseEffectParser(object -> {
			final MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.waitTimeAdder(mathValue);
		}, "base-effects", "wait-time-adder");
		this.registerBaseEffectParser(object -> {
			final MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.waitTimeMultiplier(mathValue);
		}, "base-effects", "wait-time-multiplier");
	}

	private void registerBuiltInBlockParser() {
		this.registerBlockParser(object -> {
			final String block = (String) object;
			return builder -> builder.blockID(block);
		}, "block");
		this.registerBlockParser(object -> {
			final Section section = (Section) object;
			final List<BlockDataModifier> dataModifiers = new ArrayList<>();
			final List<BlockStateModifier> stateModifiers = new ArrayList<>();
			for (Map.Entry<String, Object> innerEntry : section.getStringRouteMappedValues(false).entrySet()) {
				final BlockDataModifierFactory dataModifierFactory = instance.getBlockManager()
						.getBlockDataModifierFactory(innerEntry.getKey());
				if (dataModifierFactory != null) {
					dataModifiers.add(dataModifierFactory.process(innerEntry.getValue()));
					continue;
				}
				final BlockStateModifierFactory stateModifierFactory = instance.getBlockManager()
						.getBlockStateModifierFactory(innerEntry.getKey());
				if (stateModifierFactory != null) {
					stateModifiers.add(stateModifierFactory.process(innerEntry.getValue()));
				}
			}
			return builder -> {
				builder.dataModifierList(dataModifiers);
				builder.stateModifierList(stateModifiers);
			};
		}, "properties");
		this.registerBlockParser(object -> {
			final MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.horizontalVector(mathValue);
		}, "velocity", "horizontal");
		this.registerBlockParser(object -> {
			final MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.verticalVector(mathValue);
		}, "velocity", "vertical");
	}

	private void registerBuiltInEntityParser() {
		this.registerEntityParser(object -> {
			final String entity = (String) object;
			return builder -> builder.entityID(entity);
		}, "entity");
		this.registerEntityParser(object -> {
			final MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.horizontalVector(mathValue);
		}, "velocity", "horizontal");
		this.registerEntityParser(object -> {
			final MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.verticalVector(mathValue);
		}, "velocity", "vertical");
		this.registerEntityParser(object -> {
			final Section section = (Section) object;
			return builder -> builder.propertyMap(section.getStringRouteMappedValues(false));
		}, "properties");
	}

	private void registerBuiltInEventParser() {
		this.registerEventParser(object -> {
			final boolean disable = (boolean) object;
			return builder -> builder.disableGlobalActions(disable);
		}, "disable-global-event");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.LURE, actions);
		}, "events", "lure");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.ESCAPE, actions);
		}, "events", "escape");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.SUCCESS, actions);
		}, "events", "success");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.ACTIVATE, actions);
		}, "events", "activate");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.FAILURE, actions);
		}, "events", "failure");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.HOOK, actions);
		}, "events", "hook");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.CONSUME, actions);
		}, "events", "consume");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.CAST, actions);
		}, "events", "cast");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.BITE, actions);
		}, "events", "bite");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.LAND, actions);
		}, "events", "land");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.TIMER, actions);
		}, "events", "timer");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.INTERACT, actions);
		}, "events", "interact");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.REEL, actions);
		}, "events", "reel");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.NEW_SIZE_RECORD, actions);
		}, "events", "new_size_record");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final Action<Player>[] actions = instance.getActionManager(Player.class).parseActions(section);
			return builder -> builder.action(ActionTrigger.NEW_SIZE_RECORD, actions);
		}, "events", "new-size-record");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final TreeMap<Integer, Action<Player>[]> actions = instance.getActionManager(Player.class)
					.parseTimesActions(section);
			return builder -> builder.actionTimes(ActionTrigger.SUCCESS, actions);
		}, "events", "success_times");
		this.registerEventParser(object -> {
			final Section section = (Section) object;
			final TreeMap<Integer, Action<Player>[]> actions = instance.getActionManager(Player.class)
					.parseTimesActions(section);
			return builder -> builder.actionTimes(ActionTrigger.SUCCESS, actions);
		}, "events", "success-times");
	}

	private void registerBuiltInLootParser() {
		this.registerLootParser(object -> {
			final Section section = (Section) object;
			final Map<String, TextValue<Player>> data = new LinkedHashMap<>();
			section.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
				if (entry.getValue() instanceof String str) {
					data.put(entry.getKey(), TextValue.auto(str));
				} else {
					data.put(entry.getKey(), TextValue.auto(entry.getValue().toString()));
				}
			});
			return builder -> builder.customData(data);
		}, "custom-data");
		this.registerLootParser(object -> {
			if (object instanceof Boolean b) {
				return builder -> builder.toInventory(MathValue.plain(b ? 1 : 0));
			} else {
				return builder -> builder.toInventory(MathValue.auto(object));
			}
		}, "to-inventory");
		this.registerLootParser(object -> {
			final boolean value = (boolean) object;
			return builder -> builder.preventGrabbing(value);
		}, "prevent-grabbing");
		this.registerLootParser(object -> {
			final String string = (String) object;
			return builder -> builder.nick(string);
		}, "nick");
		this.registerLootParser(object -> {
			final boolean value = (boolean) object;
			return builder -> builder.showInFinder(value);
		}, "show-in-fishfinder");
		this.registerLootParser(object -> {
			final boolean value = (boolean) object;
			return builder -> builder.disableStatistics(value);
		}, "disable-stat");
		this.registerLootParser(object -> {
			final List<String> args = ListUtils.toList(object);
			return builder -> builder.groups(args.toArray(new String[0]));
		}, "group");
		this.registerLootParser(object -> {
			final Section section = (Section) object;
			final StatisticsKeys keys = new StatisticsKeys(section.getString("amount"), section.getString("size"));
			return builder -> builder.statisticsKeys(keys);
		}, "statistics");
	}

	public class TitleScreenInfo {

		protected final boolean enabled;
		protected final String title;
		protected final String subtitle;
		protected final int fadeIn;
		protected final int stay;
		protected final int fadeOut;

		public TitleScreenInfo(boolean enabled, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
			this.enabled = enabled;
			this.title = title;
			this.subtitle = subtitle;
			this.fadeIn = fadeIn;
			this.stay = stay;
			this.fadeOut = fadeOut;
		}

		public boolean enabled() {
			return enabled;
		}

		public String title() {
			return title;
		}

		public String subtitle() {
			return subtitle;
		}

		public int fadeIn() {
			return fadeIn;
		}

		public int stay() {
			return stay;
		}

		public int fadeOut() {
			return fadeOut;
		}
	}
}