package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.zip.GZIPInputStream;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import com.swiftlicious.hellblock.listeners.weather.WeatherType;
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
import com.swiftlicious.hellblock.utils.EnchantmentUtils;
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
	private final Map<String, YamlDocument> guiConfigs = new HashMap<>();
	private ConfigRegistry registry;

	public YamlDocument getMainConfig() {
		return mainConfig;
	}

	/**
	 * Retrieves a specific GUI configuration by name. Example:
	 * getGUIConfig("upgrades.yml")
	 */
	@Nullable
	public YamlDocument getGUIConfig(String name) {
		return guiConfigs.get(name);
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
			mainConfig = YamlDocument.create(inputStream, getResourceMaybeGz("config.yml"),
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
							.addIgnoredRoute(configVersion, "other-settings.placeholder-register", '.')
							.addIgnoredRoute(configVersion, "piglin-bartering.items", '.')
							.addIgnoredRoute(configVersion, "netherrack-generator-options.generation.blocks", '.')
							.addIgnoredRoute(configVersion, "hellblock.island-options", '.')
							.addIgnoredRoute(configVersion, "hellblock.command-whitelist", '.')
							.addIgnoredRoute(configVersion, "hellblock.display-settings.banned-phrases", '.')
							.addIgnoredRoute(configVersion, "hellblock.upgrades.tiers", '.')
							.addIgnoredRoute(configVersion, "hellblock.starter-chest.items", '.').build());
			mainConfig.save(resolveConfig("config.yml").toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		this.loadSettings();
		this.loadConfigs();
		this.loadAllGUIConfigs();
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

	/**
	 * Loads all GUI configuration files from the plugin’s JAR (supports .yml.gz).
	 */
	public void loadAllGUIConfigs() {
		File guiFolder = new File(instance.getDataFolder(), "gui");

		if (!guiFolder.exists() && !guiFolder.mkdirs()) {
			instance.getPluginLogger().severe("Failed to create GUI folder!");
			return;
		}

		guiConfigs.clear();

		// Ensure default GUI files exist
		String[] defaultGuiFiles = { "main_menu.yml", "biomes.yml", "upgrades.yml", "flags.yml", "party.yml",
				"invites.yml", "challenges.yml", "choices.yml", "notifications.yml", "display.yml", "schematics.yml",
				"visit.yml", "events.yml", "reset.yml", "market.yml" };

		for (String name : defaultGuiFiles) {
			// This automatically extracts the .yml.gz resource from the JAR if missing
			saveResource("gui" + File.separator + name);
		}

		// Now load all YAML files from the GUI folder recursively
		Deque<File> stack = new ArrayDeque<>();
		stack.push(guiFolder);

		while (!stack.isEmpty()) {
			File dir = stack.pop();
			File[] files = dir.listFiles();
			if (files == null)
				continue;

			for (File subFile : files) {
				if (subFile.isDirectory()) {
					stack.push(subFile);
					continue;
				}

				if (!subFile.isFile() || !subFile.getName().endsWith(".yml"))
					continue;

				try {
					YamlDocument doc = createOrLoadGUIConfig(subFile);
					guiConfigs.put(subFile.getName(), doc);
				} catch (Throwable e) {
					instance.getPluginLogger()
							.severe("GUI for " + subFile.getName() + " was unable to load correctly!");
					e.printStackTrace();
				}
			}
		}
	}

	private YamlDocument createOrLoadGUIConfig(File file) throws IOException {
		try (InputStream inputStream = new FileInputStream(file)) {
			// Load defaults from inside the JAR (.yml or .yml.gz)
			InputStream defaults = getResourceMaybeGz("gui" + File.separator + file.getName());
			if (defaults == null) {
				instance.getPluginLogger().warn("No defaults found for: gui" + File.separator + file.getName());
			}

			YamlDocument document = YamlDocument.create(inputStream, defaults,
					GeneralSettings.builder().setRouteSeparator('.').setUseDefaults(false).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder()
							.setScalarFormatter((tag, value, role, def) -> role == NodeRole.KEY ? ScalarStyle.PLAIN
									: (tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN))
							.build());

			document.save(file);
			return document;
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
		config.getStringList("hellblock.island-options", new ArrayList<>()).stream()
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
		config.getStringList("hellblock.command-whitelist", new ArrayList<>()).stream()
				.filter(cmd -> cmd.startsWith("/"))
				.forEach(command -> commandWhitelist.add(command.toLowerCase(Locale.ROOT)));
		bannedWords = config.getStringList("hellblock.display-settings.banned-phrases", new ArrayList<>());

		chestInventoryName = config.getString("hellblock.starter-chest.inventory-name", "Chest");
		final Section chestItemsSection = config.getSection("hellblock.starter-chest.items");
		if (chestItemsSection != null) {
			for (Map.Entry<String, Object> entry : chestItemsSection.getStringRouteMappedValues(false).entrySet()) {
				if (StringUtils.isNotInteger(entry.getKey())) {
					continue;
				}
				final int itemID = Integer.parseInt(entry.getKey());
				if (itemID <= 0) {
					instance.getPluginLogger().warn("Invalid item ID in config (must be positive): " + entry.getKey());
					continue;
				}
				if (entry.getValue() instanceof Section inner) {
					final int slot = inner.getInt("slot", RandomUtils.generateRandomInt(0, 26));
					if (slot < 0) {
						instance.getPluginLogger()
								.warn("Invalid chest slot in config (must be >= 0): " + entry.getKey());
						continue;
					}
					final CustomItem item = new SingleItemParser("chest_item_" + itemID, inner,
							getItemFormatFunctions()).getItem();
					chestItems.putIfAbsent(itemID, Pair.of(slot, item));
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
				if (itemID <= 0) {
					instance.getPluginLogger().warn("Invalid item ID in config (must be positive): " + entry.getKey());
					continue;
				}
				if (entry.getValue() instanceof Section inner) {
					final CustomItem item = new SingleItemParser("barter_item_" + itemID, inner,
							getItemFormatFunctions()).getItem();
					final MathValue<Player> weight = MathValue.auto(inner.getInt("weight", 1));
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

		weatherEnabled = config.getBoolean("nether-weather-options.enable", true);
		config.getStringList("nether-weather-options.supported-types", new ArrayList<>()).stream().forEach(
				weatherType -> supportedWeatherTypes.add(WeatherType.valueOf(weatherType.toUpperCase(Locale.ENGLISH))));
		minTime = config.getInt("nether-weather-options.intervals.min-time", 150);
		maxTime = config.getInt("nether-weather-options.intervals.max-time", 300);
		warnPlayers = config.getBoolean("nether-weather-options.warn-players-beforehand", true);
		radius = Math.abs(config.getInt("nether-weather-options.radius", 16));
		fireChance = Math.abs(config.getInt("nether-weather-options.fire-chance", 1));
		delay = Math.abs(config.getInt("nether-weather-options.task-delay", 3));
		hurtCreatures = config.getBoolean("nether-weather-options.can-hurt-living-creatures", true);
		explodeTNT = config.getBoolean("nether-weather-options.will-tnt-explode", true);

		searchRadius = config.getDouble("netherrack-generator-options.player-search-radius", 4D);
		pistonAutomation = config.getBoolean("netherrack-generator-options.automation.pistons", false);
		final Section genResultSection = config.getSection("netherrack-generator-options.generation.blocks");
		if (genResultSection != null) {
			genResultSection.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
				if (!(entry.getValue() instanceof Number value)) {
					return;
				}
				if (value.doubleValue() <= 0) {
					instance.getPluginLogger()
							.warn("Invalid generation weight for " + entry.getKey() + ": must be > 0");
					return;
				}
				final Material material = Material.matchMaterial(entry.getKey().toUpperCase(Locale.ROOT));
				if (material == null || !material.isBlock()) {
					instance.getPluginLogger().warn("Unknown material (null or not a block) for " + entry.getKey());
					return;
				}
				final MathValue<Player> chance = MathValue.auto(value);
				generationResults.putIfAbsent(material, chance);
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

		durabilityLore = new ArrayList<>(
				config.getStringList("other-settings.custom-durability-format", new ArrayList<>()).stream()
						.map(it -> "<!i>" + it).toList());

		loadEnchantmentConfigs(config);

		challengeCompletedSound = loadSound(config, "hellblock.challenge-completed-sound", "minecraft:item.totem.use",
				"PLAYER", 1.0F, 1.0F);

		linkingHellblockSound = loadSound(config, "hellblock.linking-hellblock-sound",
				"minecraft:block.end_portal.spawn", "PLAYER", 1.0F, 1.0F);

		creatingHellblockSound = loadSound(config, "hellblock.creating-hellblock-sound",
				"minecraft:entity.player.levelup", "PLAYER", 1.0F, 1.0F);

		final Section titleScreenSection = config.getSection("hellblock.creation-title-screen");
		if (titleScreenSection != null) {
			final boolean enabled = titleScreenSection.getBoolean("enable", true);
			final String title = titleScreenSection.getString("title", "");
			final String subtitle = titleScreenSection.getString("subtitle", "");
			final int fadeIn = titleScreenSection.getInt("fadeIn", 3) * 20;
			final int stay = titleScreenSection.getInt("stay", 2) * 20;
			final int fadeOut = titleScreenSection.getInt("fadeOut", 3) * 20;
			creationTitleScreen = new TitleScreenInfo(enabled, title, subtitle, fadeIn, stay, fadeOut);
		}

		final Section eventSection = config.getSection("hellblock.island-events");

		if (eventSection != null) {
			invasionEvent = loadEventData(eventSection, "invasion-settings", 100F, 30, 10);
			witherEvent = loadEventData(eventSection, "wither-settings", 50F, 30, 5);
			skysiegeEvent = loadEventData(eventSection, "skysiege-settings", 150F, 30, 10);
		} else {
			// Use hardcoded defaults if entire section is missing
			invasionEvent = new IslandEventData(true, 100F, 30, 10);
			witherEvent = new IslandEventData(true, 50F, 30, 5);
			skysiegeEvent = new IslandEventData(true, 150F, 30, 10);
		}

		itemDetectOrder = config.getStringList("other-settings.item-detection-order", new ArrayList<>())
				.toArray(new String[0]);
		blockDetectOrder = config.getStringList("other-settings.block-detection-order", new ArrayList<>())
				.toArray(new String[0]);

		eventPriority = EventPriority
				.valueOf(config.getString("other-settings.event-priority", "NORMAL").toUpperCase(Locale.ENGLISH));

		antiAutoFishingMod = config.getBoolean("other-settings.anti-auto-fishing-mod", false);

		RequirementManager<Integer> islandReqs = instance.getRequirementManager(Integer.class);
		RequirementManager<Player> playerReqs = instance.getRequirementManager(Player.class);

		Section fishingReqSection = config.getSection("lava-fishing-options.fishing-requirements");
		if (fishingReqSection != null) {
			SplitRequirements<Player, Integer> fishingReqs = parseFishingRequirements(fishingReqSection, playerReqs,
					islandReqs);

			fishingPlayerRequirements = fishingReqs.playerReqs();
			fishingIslandRequirements = fishingReqs.islandReqs();
		}

		Section autoFishingReqSection = config.getSection("lava-fishing-options.auto-fishing-requirements");
		if (autoFishingReqSection != null) {
			SplitRequirements<Player, Integer> fishingAutoReqs = parseFishingRequirements(autoFishingReqSection,
					playerReqs, islandReqs);

			autoFishingPlayerRequirements = fishingAutoReqs.playerReqs();
			autoFishingIslandRequirements = fishingAutoReqs.islandReqs();
		}

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

	@Nullable
	@Override
	public File saveResource(String filePath) {
		File target = new File(instance.getDataFolder(), filePath);

		// If it already exists, just return it
		if (target.exists())
			return target;

		target.getParentFile().mkdirs();

		// --- Try direct (uncompressed) resource ---
		try (InputStream in = instance.getResource(filePath)) {
			if (in != null) {
				try (OutputStream out = new FileOutputStream(target)) {
					in.transferTo(out);
				}
				return target;
			}
		} catch (IOException ex) {
			instance.getPluginLogger().warn("Failed to write resource " + filePath + ": " + ex.getMessage());
		}

		// --- Try gzip-compressed fallback ---
		try (InputStream gzStream = instance.getResource(filePath + ".gz")) {
			if (gzStream != null) {
				try (GZIPInputStream in = new GZIPInputStream(gzStream);
						OutputStream out = new FileOutputStream(target)) {
					in.transferTo(out);
				}
				return target;
			}
		} catch (IOException ex) {
			instance.getPluginLogger().warn("Failed to decompress gzip resource " + filePath + ": " + ex.getMessage());
		}

		// --- Not found ---
		instance.getPluginLogger().warn("Resource not found in JAR: " + filePath + " or " + filePath + ".gz");
		return null;
	}

	private void loadConfigs() {
		for (ConfigType type : ConfigType.values()) {
			final File typeFolder = new File(instance.getDataFolder(), "contents" + File.separator + type.path());

			if (!typeFolder.exists() && !typeFolder.mkdirs()) {
				instance.getPluginLogger().severe("Failed to create directory for config type: " + type.toString());
				continue;
			}

			// Ensure default file exists
			saveResource("contents" + File.separator + type.path() + File.separator + "default.yml");

			final Map<String, Node<ConfigParserFunction>> nodes = type.parser();
			final Deque<File> stack = new ArrayDeque<>();
			stack.push(typeFolder);

			while (!stack.isEmpty()) {
				final File dir = stack.pop();
				final File[] files = dir.listFiles();
				if (files == null)
					continue;

				for (File subFile : files) {
					if (subFile.isDirectory()) {
						stack.push(subFile);
						continue;
					}

					if (!subFile.isFile() || !subFile.getName().endsWith(".yml"))
						continue;

					try {
						final YamlDocument document = loadData(subFile);
						document.getStringRouteMappedValues(false).forEach((key, value) -> {
							if (value instanceof Section section) {
								try {
									type.parse(key, section, nodes);
								} catch (Exception e) {
									instance.getPluginLogger().warn(
											"Invalid config " + subFile.getPath() + " - failed to parse section " + key,
											e);
								}
							}
						});
					} catch (ConstructorException e) {
						instance.getPluginLogger().warn(
								"Could not load config file: " + subFile.getAbsolutePath() + " (corrupted YAML?)");
					} catch (Exception e) {
						instance.getPluginLogger().warn("Unexpected error loading config " + subFile.getPath(), e);
					}
				}
			}
		}
	}

	@NotNull
	public Map<ItemStack, Character> getCraftingIngredients(Section section) {
		final Map<ItemStack, Character> map = new HashMap<>();

		if (section == null) {
			instance.getPluginLogger().warn("Tried to parse crafting ingredients from a null section!");
			return map;
		}

		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			String key = entry.getKey();
			Object rawValue = entry.getValue();

			// Value should be a one-character string like: "A"
			if (!(rawValue instanceof String valueStr) || valueStr.length() != 1) {
				continue;
			}

			char symbol = valueStr.charAt(0);

			// Normalize key to lowercase and strip namespace if present
			String materialName = key.toLowerCase(Locale.ROOT);
			if (materialName.contains(":")) {
				// e.g. "minecraft:netherrack" -> "netherrack"
				materialName = materialName.substring(materialName.indexOf(':') + 1);
			}

			Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));

			if (material == null) {
				instance.getPluginLogger().warn("Unknown ingredient in crafting recipe: " + key);
				continue;
			}

			map.put(new ItemStack(material), symbol);
		}

		return map;
	}

	@SuppressWarnings("unchecked")
	@NotNull
	public SplitRequirements<Player, Integer> parseFishingRequirements(@NotNull Section config,
			@NotNull RequirementManager<Player> playerReqs, @NotNull RequirementManager<Integer> islandReqs) {
		List<Requirement<Player>> playerList = new ArrayList<>();
		List<Requirement<Integer>> islandList = new ArrayList<>();

		if (config != null) {
			config.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
				Section section = config.getSection(entry.getKey());
				if (section == null || !section.contains("type")) {
					return;
				}

				String target = section.getString("target", "").toLowerCase();
				switch (target) {
				case "player" -> playerList.add(playerReqs.parseRequirement(section, true));
				case "island" -> islandList.add(islandReqs.parseRequirement(section, true));
				default -> {
					instance.getPluginLogger().warn("Unknown or missing target for requirement: " + entry.getKey());
				}
				}
			});
		}

		return new SplitRequirements<>(playerList.toArray(Requirement[]::new), islandList.toArray(Requirement[]::new));
	}

	@NotNull
	private HellEnchantmentData loadEnchantmentData(@NotNull YamlDocument config, @NotNull String enchantmentKey,
			@NotNull String defaultBookName, @NotNull List<String> defaultBookLore,
			@NotNull List<String> defaultArmorLore, int defaultChance) {
		String base = "other-settings.hell-enchantments." + enchantmentKey;

		String bookName = config.getString(base + ".enchantment-book-format.name", defaultBookName);

		List<String> bookLore = new ArrayList<>(
				config.getStringList(base + ".enchantment-book-format.lore", new ArrayList<>())).stream()
				.map(it -> "<!i>" + it).toList();
		if (bookLore.isEmpty())
			bookLore = defaultBookLore;

		List<String> armorLore = new ArrayList<>(
				config.getStringList(base + ".armor-additional-format", new ArrayList<>())).stream()
				.map(it -> "<!i>" + it).toList();
		if (armorLore.isEmpty())
			armorLore = defaultArmorLore;

		int chance = config.getInt(base + ".enchant-table-chance", defaultChance);

		return new HellEnchantmentData(bookName, bookLore, armorLore, chance);
	}

	public void loadEnchantmentConfigs(@NotNull YamlDocument config) {
		magmaWalkerEnchantData = loadEnchantmentData(config, "magma-walker", "Book of Magma Walker {level}",
				List.of("<!i>Walks on lava, creating magma blocks", "<!i>Immune to magma damage"),
				List.of("<!i>Magma Walker {level}"), 3);

		moltenCoreEnchantData = loadEnchantmentData(config, "molten-core", "Book of Molten Core {level}",
				List.of("<!i>Auto-smelts ores when mined", "<!i>Only works on your island"),
				List.of("<!i>Molten Core {level}"), 3);

		lavaVisionEnchantData = loadEnchantmentData(config, "lava-vision", "Book of Lava Vision {level}",
				List.of("<!i>See clearly while submerged in lava", "<!i>Highlights nearby entities"),
				List.of("<!i>Lava Vision {level}"), 2);

		crimsonThornsEnchantData = loadEnchantmentData(config, "crimson-thorns", "Book of Crimson Thorns {level}",
				List.of("<!i>Reflects fire damage and knockback to attackers", "<!i>Only works on your island"),
				List.of("<!i>Crimson Thorns {level}"), 2);
	}

	@Nullable
	private Sound loadSound(@NotNull YamlDocument config, @NotNull String path, @NotNull String defaultKey,
			@NotNull String defaultSource, float defaultVolume, float defaultPitch) {
		final Section section = config.getSection(path);
		if (section == null) {
			return null;
		}

		try {
			String key = section.getString("key", defaultKey);
			String sourceStr = section.getString("source", defaultSource).toUpperCase(Locale.ENGLISH);
			float volume = section.getFloat("volume", defaultVolume);
			float pitch = section.getFloat("pitch", defaultPitch);

			Sound.Source source = Sound.Source.valueOf(sourceStr);

			return Sound.sound(net.kyori.adventure.key.Key.key(key), source, volume, pitch);
		} catch (Exception ex) {
			instance.getPluginLogger().warn("Failed to load sound at " + path + ": " + ex.getMessage());
			return null;
		}
	}

	@NotNull
	private IslandEventData loadEventData(@NotNull Section parent, @NotNull String key, float defaultLevel,
			int defaultCooldown, int defaultDuration) {
		Section section = parent.getSection(key);
		if (section != null) {
			boolean enabled = section.getBoolean("enable", true);
			float levelRequired = section.getFloat("level-required", defaultLevel);
			int cooldown = section.getInt("cooldown", defaultCooldown);
			int maxDuration = section.getInt("max-duration", defaultDuration);
			return new IslandEventData(enabled, levelRequired, cooldown, maxDuration);
		} else {
			return new IslandEventData(true, defaultLevel, defaultCooldown, defaultDuration);
		}
	}

	public Map<Key, Short> getEnchantments(Section section) {
		final Map<Key, Short> map = new HashMap<>();

		if (section == null) {
			instance.getPluginLogger().warn("Tried to parse enchantments from a null section!");
			return map;
		}

		// Whitelisted custom enchantments (case-insensitive)
		final Set<String> allowedCustom = Set.of("hellblock:crimson_thorns", "hellblock:lava_vision",
				"hellblock:magma_walker", "hellblock:molten_core");

		section.getStringRouteMappedValues(false).forEach((key, value) -> {
			if (key == null || key.isBlank())
				return;

			// Convert value safely to integer
			int level = 1;
			if (value instanceof Number num) {
				level = num.intValue();
			} else if (value instanceof String str && str.matches("\\d+")) {
				level = Integer.parseInt(str);
			} else {
				instance.getPluginLogger()
						.warn("Invalid enchantment level '" + value + "' for key '" + key + "'. Using level 1.");
			}

			// Clamp to valid range
			level = Math.min(255, Math.max(1, level));

			// Try vanilla first
			Enchantment enchant = EnchantmentUtils.getCompatibleEnchantment(key);
			if (enchant != null) {
				map.put(Key.of(enchant.getKey().getNamespace(), enchant.getKey().getKey()), (short) level);
				return;
			}

			// Then try custom allowed enchantments
			String lowered = key.toLowerCase(Locale.ROOT);
			if (allowedCustom.contains(lowered)) {
				Key customKey = Key.of(lowered);
				map.put(customKey, (short) level);
				return;
			}

			instance.getPluginLogger().warn("Unknown or unsupported enchantment key '" + key + "' — skipping.");
		});

		return map;
	}

	private List<Tuple<Double, String, Short>> getPossibleEnchantments(Section section) {
		final List<Tuple<Double, String, Short>> list = new ArrayList<>();

		if (section == null) {
			instance.getPluginLogger().warn("Tried to parse possible enchantments from a null section!");
			return list;
		}

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
					final Enchantment enchantment = EnchantmentUtils
							.getCompatibleEnchantment(enchantPair.left().toString());
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
			final Section section = (Section) arg;
			final Map<Key, Short> map = getEnchantments(section);

			return (item, context) -> {
				if (item.getItem().getType() != Material.ENCHANTED_BOOK) {
					instance.getPluginLogger()
							.warn("Ignoring hellblock-stored-enchantments: can only be applied to enchanted books ("
									+ item.getItem().getType().name() + ")");
					return;
				}

				for (Map.Entry<Key, Short> entry : map.entrySet()) {
					Key customKey = entry.getKey();
					short level = entry.getValue();

					if (!"hellblock".equals(customKey.namespace())) {
						// Vanilla enchantments — store normally
						item.storedEnchantments(Map.of(customKey, level));
						continue;
					}

					String value = customKey.value();
					String tagPath = switch (value) {
					case "lava_vision" -> "lava_vision_book";
					case "crimson_thorns" -> "crimson_thorns_book";
					case "molten_core" -> "molten_core_book";
					case "magma_walker" -> "magma_walker_book";
					default -> null;
					};

					if (tagPath == null) {
						instance.getPluginLogger().warn("Unknown hellblock stored enchantment: " + value);
						continue;
					}

					item.setTag((int) level, "custom", tagPath, "level");
					item.setTag(true, "minecraft", "enchantment_glint_override");
				}
			};
		}, 4601, "hellblock-stored-enchantments");
		this.registerItemParser(arg -> {
			final Section section = (Section) arg;
			final Map<Key, Short> map = getEnchantments(section);

			return (item, context) -> {
				for (Map.Entry<Key, Short> entry : map.entrySet()) {
					Key customKey = entry.getKey();
					short level = entry.getValue();

					if ("hellblock".equals(customKey.namespace())) {
						String value = customKey.value();

						// Validate per-item-type
						String type = item.getItem().getType().name();
						switch (value) {
						case "lava_vision" -> {
							if (!type.endsWith("_HELMET")) {
								instance.getPluginLogger()
										.warn("Ignoring lava_vision: can only be applied to helmets (" + type + ")");
								continue;
							}
							item.setTag((int) level, "custom", "lava_vision_helmet", "level");
						}

						case "crimson_thorns" -> {
							if (!type.endsWith("_CHESTPLATE")) {
								instance.getPluginLogger().warn(
										"Ignoring crimson_thorns: can only be applied to chestplates (" + type + ")");
								continue;
							}
							item.setTag((int) level, "custom", "crimson_thorns_chestplate", "level");
						}

						case "molten_core" -> {
							if (!type.endsWith("_LEGGINGS")) {
								instance.getPluginLogger()
										.warn("Ignoring molten_core: can only be applied to leggings (" + type + ")");
								continue;
							}
							item.setTag((int) level, "custom", "molten_core_leggings", "level");
						}

						case "magma_walker" -> {
							if (!type.endsWith("_BOOTS")) {
								instance.getPluginLogger()
										.warn("Ignoring magma_walker: can only be applied to boots (" + type + ")");
								continue;
							}
							item.setTag((int) level, "custom", "magma_walker_boots", "level");
						}

						default -> instance.getPluginLogger().warn("Unknown custom enchantment: " + value);
						}

						// Always give glint
						item.setTag(true, "minecraft", "enchantment_glint_override");
					} else {
						// Vanilla enchantment — apply normally
						item.enchantments(Map.of(customKey, level));
					}
				}
			};
		}, 4501, "hellblock-enchantments");
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

	public class HellEnchantmentData {
		protected final String bookName;
		protected final List<String> bookLore;
		protected final List<String> armorAdditionalLore;
		protected final int enchantmentTableChance;

		public HellEnchantmentData(String bookName, List<String> bookLore, List<String> armorAdditionalLore,
				int enchantmentTableChance) {
			this.bookName = bookName;
			this.bookLore = bookLore;
			this.armorAdditionalLore = armorAdditionalLore;
			this.enchantmentTableChance = enchantmentTableChance;
		}

		public String bookName() {
			return bookName;
		}

		public List<String> bookLore() {
			return bookLore;
		}

		public List<String> armorAdditionalLore() {
			return armorAdditionalLore;
		}

		public int enchantmentTableChance() {
			return enchantmentTableChance;
		}
	}

	public class IslandEventData {
		protected final boolean enabled;
		protected final float levelRequired;
		protected final int cooldown;
		protected final int maxDuration;

		public IslandEventData(boolean enabled, float levelRequired, int cooldown, int maxDuration) {
			this.enabled = enabled;
			this.levelRequired = levelRequired;
			this.cooldown = cooldown;
			this.maxDuration = maxDuration;
		}

		public boolean enabled() {
			return enabled;
		}

		public float levelRequired() {
			return levelRequired;
		}

		public int cooldown() {
			return cooldown;
		}

		public int maxDuration() {
			return maxDuration;
		}
	}

	public class TitleScreenInfo {
		protected final boolean enabled;
		protected final String title;
		protected final String subtitle;
		protected final int fadeIn;
		protected final int stay;
		protected final int fadeOut;

		public TitleScreenInfo(boolean enabled, @NotNull String title, @NotNull String subtitle, int fadeIn, int stay,
				int fadeOut) {
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

		@NotNull
		public String title() {
			return title;
		}

		@NotNull
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

	public record SplitRequirements<T1, T2>(Requirement<T1>[] playerReqs, Requirement<T2>[] islandReqs) {
	}
}