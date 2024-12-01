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
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.yaml.snakeyaml.constructor.ConstructorException;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.ConfigType;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.creation.block.BlockDataModifier;
import com.swiftlicious.hellblock.creation.block.BlockDataModifierFactory;
import com.swiftlicious.hellblock.creation.block.BlockStateModifier;
import com.swiftlicious.hellblock.creation.block.BlockStateModifierFactory;
import com.swiftlicious.hellblock.creation.item.AbstractItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.creation.item.ItemEditor;
import com.swiftlicious.hellblock.creation.item.damage.CustomDurabilityItem;
import com.swiftlicious.hellblock.database.dependency.HellblockProperties;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.EffectProperties;
import com.swiftlicious.hellblock.handlers.ActionManagerInterface;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.EventManagerInterface;
import com.swiftlicious.hellblock.handlers.ExpressionHelper;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.loot.LootInterface;
import com.swiftlicious.hellblock.loot.StatisticsKeys;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.utils.ItemStackUtils;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.OffsetUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
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

public class ConfigManager extends ConfigHandler {

	private YamlDocument MAIN_CONFIG;

	public YamlDocument getMainConfig() {
		return MAIN_CONFIG;
	}

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
	}

	@Override
	public void load() {
		String configVersion = HellblockProperties.getValue("config");
		try (InputStream inputStream = new FileInputStream(resolveConfig("config.yml").toFile())) {
			MAIN_CONFIG = YamlDocument.create(inputStream, instance.getResource("config.yml".replace("\\", "/")),
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
							.addIgnoredRoute(configVersion, "piglin-bartering.materials", '.')
							.addIgnoredRoute(configVersion, "netherrack-generator-options.generation.blocks", '.')
							.addIgnoredRoute(configVersion, "hellblock.island-options", '.')
							.addIgnoredRoute(configVersion, "hellblock.starter-chest.items", '.').build());
			MAIN_CONFIG.save(resolveConfig("config.yml").toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		this.loadSettings();
		this.loadConfigs();
		this.loadGlobalEffects();
	}

	private void loadGlobalEffects() {
		YamlDocument config = getMainConfig();
		globalEffects = new ArrayList<>();
		Section globalEffectSection = config.getSection("lava-fishing-options.global-effects");
		if (globalEffectSection != null) {
			for (Map.Entry<String, Object> entry : globalEffectSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					globalEffects.add(parseEffect(innerSection));
				}
			}
		}
	}

	private void loadSettings() {
		YamlDocument config = getMainConfig();

		metrics = config.getBoolean("metrics", true);
		checkUpdate = config.getBoolean("update-checker", true);
		debug = config.getBoolean("debug", false);

		worldName = config.getString("general.world", "hellworld");
		perPlayerWorlds = config.getBoolean("general.per-player-worlds", false);
		spawnSize = config.getInt("general.spawn-size", 25);

		transferIslands = config.getBoolean("hellblock.can-transfer-islands", true);
		linkHellblocks = config.getBoolean("hellblock.can-link-hellblocks", true);
		partySize = config.getInt("hellblock.max-party-size", 4);
		schematicPaster = config.getString("hellblock.schematic-paster", "worldedit");
		islandOptions = config.getStringList("hellblock.island-options");
		height = config.getInt("hellblock.height", 150);
		distance = config.getInt("hellblock.distance", 110);
		worldguardProtect = config.getBoolean("hellblock.use-worldguard-protection", false);
		protectionRange = config.getInt("hellblock.protection-range", 105);
		resetInventory = config.getBoolean("hellblock.clear-inventory-on-reset", true);
		resetEnderchest = config.getBoolean("hellblock.clear-enderchest-on-reset", true);
		abandonTime = config.getInt("hellblock.abandon-after-days", 30);
		entryMessageEnabled = config.getBoolean("hellblock.entry-message-enabled", true);
		farewellMessageEnabled = config.getBoolean("hellblock.farewell-message-enabled", true);
		disableBedExplosions = config.getBoolean("hellblock.disable-bed-explosions", true);
		growNaturalTrees = config.getBoolean("hellblock.grow-natural-trees", false);
		voidTeleport = config.getBoolean("hellblock.void-teleport", true);

		chestInventoryName = config.getString("hellblock.starter-chest.inventory-name", "Chest");
		chestItems = config.getSection("hellblock.starter-chest.items");

		levelSystem = config.getStringList("level-system.blocks");

		clearDefaultOutcome = config.getBoolean("piglin-bartering.clear-default-outcome", true);
		barteringItems = config.getStringList("piglin-bartering.materials");

		randomStats = config.getBoolean("wither-stats.random-stats", true);
		randomMinHealth = config.getInt("wither-stats.random-min-health", 200);
		randomMaxHealth = config.getInt("wither-stats.random-max-health", 200);
		if (randomMinHealth <= 0)
			randomMinHealth = 200;
		if (randomMaxHealth <= 0)
			randomMaxHealth = 500;
		randomMinStrength = config.getDouble("wither-stats.random-min-strength", 0.5);
		randomMaxStrength = config.getDouble("wither-stats.random-max-strength", 2.5);
		if (randomMinStrength <= 0)
			randomMinStrength = 0.5;
		if (randomMaxStrength <= 0)
			randomMaxStrength = 2.5;
		defaultStrength = config.getDouble("wither-stats.default-strength", 1.25);
		defaultHealth = config.getInt("wither-stats.default-health", 300);

		infiniteLavaEnabled = config.getBoolean("infinite-lava-options.enabled", true);

		lavaRainEnabled = config.getBoolean("lava-rain-options.enabled", true);
		radius = Math.abs(config.getInt("lava-rain-options.radius", 16));
		fireChance = Math.abs(config.getInt("lava-rain-options.fire-chance", 1));
		delay = Math.abs(config.getInt("lava-rain-options.task-delay", 3));
		hurtCreatures = config.getBoolean("lava-rain-options.can-hurt-living-creatures", true);
		explodeTNT = config.getBoolean("lava-rain-options.will-tnt-explode", true);

		searchRadius = config.getDouble("netherrack-generator-options.player-search-radius", 4D);
		pistonAutomation = config.getBoolean("netherrack-generator-options.automation.pistons", false);
		generationResults = config.getStringList("netherrack-generator-options.generation.blocks");

		lavaFishingEnabled = config.getBoolean("lava-fishing-options.enabled", true);
		lavaMinTime = config.getInt("lava-fishing-options.lava-fishing.min-wait-time", 100);
		lavaMaxTime = config.getInt("lava-fishing-options.lava-fishing.max-wait-time", 600);

		restrictedSizeRange = config.getBoolean("lava-fishing-options.size.restricted-size-range", true);

		placeholderLimit = config.getInt("general.redis-synchronization.placeholder-limit", 3);
		serverGroup = config.getString("general.redis-synchronization.server-group", "default");
		redisRanking = config.getBoolean("general.redis-synchronization.redis-ranking", false);

		AdventureHelper.legacySupport = config.getBoolean("other-settings.legacy-color-code-support", true);
		dataSaveInterval = config.getInt("other-settings.data-saving-interval", 600);
		logDataSaving = config.getBoolean("other-settings.log-data-saving", true);
		lockData = config.getBoolean("other-settings.lock-data", true);

		durabilityLore = new ArrayList<>(config.getStringList("other-settings.custom-durability-format").stream()
				.map(it -> "<!i>" + it).toList());

		Section challengeSoundSection = config.getSection("hellblock.challenge-completed-sound");
		if (challengeSoundSection != null) {
			challengeCompletedSound = Sound.sound(
					net.kyori.adventure.key.Key.key(challengeSoundSection.getString("key")),
					Sound.Source
							.valueOf(challengeSoundSection.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH)),
					challengeSoundSection.getFloat("volume", 1.0F).floatValue(),
					challengeSoundSection.getFloat("pitch", 1.0F).floatValue());
		}

		Section linkingSoundSection = config.getSection("hellblock.linking-hellblock-sound");
		if (linkingSoundSection != null) {
			linkingHellblockSound = Sound.sound(net.kyori.adventure.key.Key.key(linkingSoundSection.getString("key")),
					Sound.Source.valueOf(linkingSoundSection.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH)),
					linkingSoundSection.getFloat("volume", 1.0F).floatValue(),
					linkingSoundSection.getFloat("pitch", 1.0F).floatValue());
		}

		Section creatingSoundSection = config.getSection("hellblock.creating-hellblock-sound");
		if (creatingSoundSection != null) {
			creatingHellblockSound = Sound.sound(net.kyori.adventure.key.Key.key(creatingSoundSection.getString("key")),
					Sound.Source
							.valueOf(creatingSoundSection.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH)),
					creatingSoundSection.getFloat("volume", 1.0F).floatValue(),
					creatingSoundSection.getFloat("pitch", 1.0F).floatValue());
		}

		Section titleScreenSection = config.getSection("hellblock.creation-title-screen");
		if (titleScreenSection != null) {
			boolean enabled = titleScreenSection.getBoolean("enabled", true);
			String title = titleScreenSection.getString("title");
			String subtitle = titleScreenSection.getString("subtitle");
			int fadeIn = titleScreenSection.getInt("fadeIn") * 20;
			int stay = titleScreenSection.getInt("stay") * 20;
			int fadeOut = titleScreenSection.getInt("fadeOut") * 20;
			creationTitleScreen = new TitleScreenInfo(enabled, title, subtitle, fadeIn, stay, fadeOut);
		}

		itemDetectOrder = config.getStringList("other-settings.item-detection-order").toArray(new String[0]);
		blockDetectOrder = config.getStringList("other-settings.block-detection-order").toArray(new String[0]);

		eventPriority = EventPriority
				.valueOf(config.getString("other-settings.event-priority", "NORMAL").toUpperCase(Locale.ENGLISH));

		antiAutoFishingMod = config.getBoolean("other-settings.anti-auto-fishing-mod", false);

		fishingRequirements = instance.getRequirementManager()
				.parseRequirements(config.getSection("lava-fishing-options.fishing-requirements"), true);
		autoFishingRequirements = instance.getRequirementManager()
				.parseRequirements(config.getSection("lava-fishing-options.auto-fishing-requirements"), true);

		baitAnimation = config.getBoolean("lava-fishing-options.bait-animation", true);

		multipleLootSpawnDelay = config.getInt("lava-fishing-options.multiple-loot-spawn-delay", 4);

		LootInterface.DefaultProperties.DEFAULT_DISABLE_STATS = config
				.getBoolean("lava-fishing-options.global-loot-property.disable-stat", false);
		LootInterface.DefaultProperties.DEFAULT_SHOW_IN_FINDER = config
				.getBoolean("lava-fishing-options.global-loot-property.show-in-fishfinder", true);

		Section placeholderSection = config.getSection("other-settings.placeholder-register");
		if (placeholderSection != null) {
			for (Map.Entry<String, Object> entry : placeholderSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof String original) {
					instance.getPlaceholderManager().registerCustomPlaceholder(entry.getKey(), original);
				}
			}
		}

		OffsetUtils.load(config.getSection("other-settings.offset-characters"));

		EventManagerInterface.GLOBAL_ACTIONS.clear();
		EventManagerInterface.GLOBAL_TIMES_ACTION.clear();
		Section globalEvents = config.getSection("lava-fishing-options.global-events");
		if (globalEvents != null) {
			for (Map.Entry<String, Object> entry : globalEvents.getStringRouteMappedValues(false).entrySet()) {
				MechanicType type = MechanicType.index().value(entry.getKey());
				if (entry.getValue() instanceof Section inner) {
					Map<ActionTrigger, Action<Player>[]> actionMap = new HashMap<>();
					for (Map.Entry<String, Object> innerEntry : inner.getStringRouteMappedValues(false).entrySet()) {
						if (innerEntry.getValue() instanceof Section actionSection) {
							actionMap.put(ActionTrigger.valueOf(innerEntry.getKey().toUpperCase(Locale.ENGLISH)),
									instance.getActionManager().parseActions(actionSection));
						}
					}
					EventManagerInterface.GLOBAL_ACTIONS.put(type, actionMap);
				}
			}
		}

		instance.getTranslationManager()
				.forceLocale(instance.getTranslationManager().parseLocale(config.getString("force-locale", "")));
	}

	@Override
	public void saveResource(String filePath) {
		if (!new File(instance.getDataFolder(), filePath).exists()) {
			instance.saveResource(filePath, false);
		}
	}

	private void loadConfigs() {
		Deque<File> fileDeque = new ArrayDeque<>();
		for (ConfigType type : ConfigType.values()) {
			File typeFolder = new File(instance.getDataFolder(), "contents" + File.separator + type.path());
			if (!typeFolder.exists()) {
				if (!typeFolder.mkdirs())
					return;
				instance.saveResource("contents" + File.separator + type.path() + File.separator + "default.yml",
						false);
			}
			Map<String, Node<ConfigParserFunction>> nodes = type.parser();
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
						try {
							YamlDocument document = instance.getConfigManager().loadData(subFile);
							for (Map.Entry<String, Object> entry : document.getStringRouteMappedValues(false)
									.entrySet()) {
								if (entry.getValue() instanceof Section section) {
									type.parse(entry.getKey(), section, nodes);
								}
							}
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
		Map<ItemStack, Character> map = new HashMap<>();
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			Character symbol = (Character) entry.getValue();
			if (entry.getKey().equals("WATER_BOTTLE")) {
				ItemStack bottle = new ItemStack(Material.POTION);
				ItemMeta potionMeta = bottle.getItemMeta();
				PotionMeta pmeta = (PotionMeta) potionMeta;
				pmeta.setBasePotionType(PotionType.WATER);
				bottle.setItemMeta(pmeta);
				map.put(bottle, symbol);
			} else {
				if (Material.getMaterial(entry.getKey()) != null) {
					map.put(new ItemStack(Material.getMaterial(entry.getKey())), symbol);
				}
			}
		}
		return map;
	}

	public Map<Key, Short> getEnchantments(Section section) {
		Map<Key, Short> map = new HashMap<>();
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			int level = Math.min(255, Math.max(1, (int) entry.getValue()));
			if (Registry.ENCHANTMENT.get(Objects.requireNonNull(NamespacedKey.fromString(entry.getKey()))) != null) {
				map.put(Key.fromString(entry.getKey()), (short) level);
			}
		}
		return map;
	}

	private List<Tuple<Double, String, Short>> getPossibleEnchantments(Section section) {
		List<Tuple<Double, String, Short>> list = new ArrayList<>();
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section inner) {
				Tuple<Double, String, Short> tuple = Tuple.of(inner.getDouble("chance"), inner.getString("enchant"),
						Short.valueOf(String.valueOf(inner.getInt("level"))));
				list.add(tuple);
			}
		}
		return list;
	}

	private Pair<Key, Short> getEnchantmentPair(String enchantmentWithLevel) {
		String[] split = enchantmentWithLevel.split(":", 3);
		return Pair.of(Key.of(split[0], split[1]), Short.parseShort(split[2]));
	}

	private void registerBuiltInItemProperties() {
		Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f1 = arg -> {
			Section section = (Section) arg;
			boolean stored = Objects.equals(section.getNameAsString(), "stored-enchantment-pool");
			Section amountSection = section.getSection("amount");
			Section enchantSection = section.getSection("pool");
			List<Pair<Integer, MathValue<Player>>> amountList = new ArrayList<>();
			for (Map.Entry<String, Object> entry : amountSection.getStringRouteMappedValues(false).entrySet()) {
				amountList.add(Pair.of(Integer.parseInt(entry.getKey()), MathValue.auto(entry.getValue())));
			}
			List<Pair<Pair<Key, Short>, MathValue<Player>>> enchantPoolPair = new ArrayList<>();
			for (Map.Entry<String, Object> entry : enchantSection.getStringRouteMappedValues(false).entrySet()) {
				enchantPoolPair.add(Pair.of(getEnchantmentPair(entry.getKey()), MathValue.auto(entry.getValue())));
			}
			if (amountList.isEmpty() || enchantPoolPair.isEmpty()) {
				throw new RuntimeException("Both `pool` and `amount` should not be empty");
			}
			return (item, context) -> {
				List<Pair<Integer, Double>> parsedAmountPair = new ArrayList<>(amountList.size());
				for (Pair<Integer, MathValue<Player>> rawValue : amountList) {
					parsedAmountPair.add(Pair.of(rawValue.left(), rawValue.right().evaluate(context)));
				}
				int amount = WeightUtils.getRandom(parsedAmountPair);
				if (amount <= 0)
					return;
				Set<Enchantment> addedEnchantments = new HashSet<>();
				List<Pair<Pair<Key, Short>, Double>> cloned = new ArrayList<>(enchantPoolPair.size());
				for (Pair<Pair<Key, Short>, MathValue<Player>> rawValue : enchantPoolPair) {
					cloned.add(Pair.of(rawValue.left(), rawValue.right().evaluate(context)));
				}
				int i = 0;
				outer: while (i < amount && !cloned.isEmpty()) {
					Pair<Key, Short> enchantPair = WeightUtils.getRandom(cloned);
					Enchantment enchantment = Registry.ENCHANTMENT
							.get(Objects.requireNonNull(NamespacedKey.fromString(enchantPair.left().toString())));
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
		Function<Object, BiConsumer<Item<ItemStack>, Context<Player>>> f2 = arg -> {
			Section section = (Section) arg;
			boolean stored = Objects.equals(section.getNameAsString(), "random-stored-enchantments");
			List<Tuple<Double, String, Short>> enchantments = getPossibleEnchantments(section);
			return (item, context) -> {
				Set<String> ids = new HashSet<>();
				for (Tuple<Double, String, Short> pair : enchantments) {
					if (Math.random() < pair.left() && !ids.contains(pair.mid())) {
						if (stored) {
							item.addStoredEnchantment(Key.fromString(pair.mid()), pair.right());
						} else {
							item.addEnchantment(Key.fromString(pair.mid()), pair.right());
						}
						ids.add(pair.mid());
					}
				}
			};
		};
		this.registerItemParser(f2, 4850, "random-stored-enchantments");
		this.registerItemParser(f2, 4750, "random-enchantments");
		this.registerItemParser(arg -> {
			Section section = (Section) arg;
			Map<Key, Short> map = getEnchantments(section);
			return (item, context) -> item.storedEnchantments(map);
		}, 4600, "stored-enchantments");
		this.registerItemParser(arg -> {
			Section section = (Section) arg;
			Map<Key, Short> map = getEnchantments(section);
			return (item, context) -> item.enchantments(map);
		}, 4500, "enchantments");
		this.registerItemParser(arg -> {
			String base64 = (String) arg;
			return (item, context) -> item.skull(base64);
		}, 5200, "head64");
		this.registerItemParser(arg -> {
			String effect = (String) arg;
			return (item, context) -> item.potionEffect(effect);
		}, 5300, "potion", "effect");
		this.registerItemParser(arg -> {
			int color = (int) arg;
			return (item, context) -> item.potionColor(color);
		}, 5400, "potion", "color");
		this.registerItemParser(arg -> {
			boolean glint = (boolean) arg;
			return (item, context) -> item.glint(glint);
		}, 5500, "glowing");
		this.registerItemParser(arg -> {
			List<String> args = ListUtils.toList(arg);
			return (item, context) -> item.itemFlags(args);
		}, 5100, "item-flags");
		this.registerItemParser(arg -> {
			MathValue<Player> mathValue = MathValue.auto(arg);
			return (item, context) -> item.customModelData((int) mathValue.evaluate(context));
		}, 5000, "custom-model-data");
		this.registerItemParser(arg -> {
			TextValue<Player> textValue = TextValue.auto("<!i><white>" + arg);
			return (item, context) -> {
				item.displayName(AdventureHelper.miniMessageToJson(textValue.render(context)));
			};
		}, 4000, "display", "name");
		this.registerItemParser(arg -> {
			List<String> list = ListUtils.toList(arg);
			List<TextValue<Player>> lore = new ArrayList<>();
			for (String text : list) {
				lore.add(TextValue.auto("<!i><white>" + text));
			}
			return (item, context) -> {
				item.lore(lore.stream().map(it -> AdventureHelper.miniMessageToJson(it.render(context))).toList());
			};
		}, 3_000, "display", "lore");
		this.registerItemParser(arg -> {
			boolean enable = (boolean) arg;
			return (item, context) -> {
				if (!enable)
					return;
				item.setTag(context.arg(ContextKeys.ID), "HellblockItem", "id");
			};
		}, 2_000, "tag");
		this.registerItemParser(arg -> {
			boolean enable = (boolean) arg;
			return (item, context) -> {
				item.unbreakable(enable);
			};
		}, 2_211, "unbreakable");
		this.registerItemParser(arg -> {
			boolean enable = (boolean) arg;
			return (item, context) -> {
				if (enable)
					return;
				item.setTag(UUID.randomUUID(), "HellblockItem", "uuid");
			};
		}, 2_222, "stackable");
		this.registerItemParser(arg -> {
			boolean enable = (boolean) arg;
			return (item, context) -> item.setTag(enable ? 1 : 0, "HellblockItem", "placeable");
		}, 2_335, "placeable");
		this.registerItemParser(arg -> {
			String sizePair = (String) arg;
			String[] split = sizePair.split("~", 2);
			MathValue<Player> min = MathValue.auto(split[0]);
			MathValue<Player> max = split.length == 2 ? MathValue.auto(split[1]) : MathValue.auto(split[0]);
			return (item, context) -> {
				double minSize = min.evaluate(context);
				double maxSize = max.evaluate(context);
				float size = (float) RandomUtils.generateRandomDouble(minSize, maxSize);
				Double sm = context.arg(ContextKeys.SIZE_MULTIPLIER);
				if (sm == null)
					sm = 1.0;
				Double sa = context.arg(ContextKeys.SIZE_ADDER);
				if (sa == null)
					sa = 0.0;
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
				context.arg(ContextKeys.SIZE_FORMATTED, String.format("%.2f", size));
			};
		}, 1_000, "size");
		this.registerItemParser(arg -> {
			Section section = (Section) arg;
			MathValue<Player> base = MathValue.auto(section.get("base", "0"));
			MathValue<Player> bonus = MathValue.auto(section.get("bonus", "0"));
			return (item, context) -> {
				double basePrice = base.evaluate(context);
				context.arg(ContextKeys.BASE, basePrice);
				double bonusPrice = bonus.evaluate(context);
				context.arg(ContextKeys.BONUS, bonusPrice);
				String formula = instance.getMarketManager().getFormula();
				TextValue<Player> playerTextValue = TextValue.auto(formula);
				String rendered = playerTextValue.render(context);
				List<String> unparsed = instance.getPlaceholderManager().resolvePlaceholders(rendered);
				for (String unparsedValue : unparsed) {
					rendered = rendered.replace(unparsedValue, "0");
				}
				double price = ExpressionHelper.evaluate(rendered);
				item.setTag(price, "Price");
				context.arg(ContextKeys.PRICE, price);
				context.arg(ContextKeys.PRICE_FORMATTED, String.format("%.2f", price));
			};
		}, 1_500, "price");
		this.registerItemParser(arg -> {
			boolean random = (boolean) arg;
			return (item, context) -> {
				if (!random)
					return;
				if (item.hasTag("HellblockItem", "max_dur")) {
					CustomDurabilityItem durabilityItem = new CustomDurabilityItem(item);
					durabilityItem.damage(RandomUtils.generateRandomInt(0, durabilityItem.maxDamage() - 1));
				} else {
					item.damage(RandomUtils.generateRandomInt(0, item.maxDamage().get() - 1));
				}
			};
		}, 3200, "random-durability");
		this.registerItemParser(arg -> {
			MathValue<Player> mathValue = MathValue.auto(arg);
			return (item, context) -> {
				int max = (int) mathValue.evaluate(context);
				item.setTag(max, "HellblockItem", "max_dur");
				item.setTag(max, "HellblockItem", "cur_dur");
				CustomDurabilityItem customDurabilityItem = new CustomDurabilityItem(item);
				customDurabilityItem.damage(0);
			};
		}, 3100, "max-durability");
		this.registerItemParser(arg -> {
			Section section = (Section) arg;
			List<ItemEditor> editors = new ArrayList<>();
			ItemStackUtils.sectionToTagEditor(section, editors);
			return (item, context) -> {
				for (ItemEditor editor : editors) {
					editor.apply(((AbstractItem<RtagItem, ItemStack>) item).getRTagItem(), context);
				}
			};
		}, 10_050, "nbt");
		if (instance.getVersionManager().isVersionNewerThan1_20_5()) {
			this.registerItemParser(arg -> {
				Section section = (Section) arg;
				List<ItemEditor> editors = new ArrayList<>();
				ItemStackUtils.sectionToComponentEditor(section, editors);
				return (item, context) -> {
					for (ItemEditor editor : editors) {
						editor.apply(((AbstractItem<RtagItem, ItemStack>) item).getRTagItem(), context);
					}
				};
			}, 10_075, "components");
		}
	}

	private void registerBuiltInEffectModifierParser() {
		this.registerEffectModifierParser(object -> {
			Section section = (Section) object;
			return builder -> builder
					.requirements(List.of(instance.getRequirementManager().parseRequirements(section, true)));
		}, "requirements");
		this.registerEffectModifierParser(object -> {
			Section section = (Section) object;
			List<TriConsumer<Effect, Context<Player>, Integer>> property = new ArrayList<>();
			for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					property.add(parseEffect(innerSection));
				}
			}
			return builder -> {
				builder.modifiers(property);
			};
		}, "effects");
	}

	public TriConsumer<Effect, Context<Player>, Integer> parseEffect(Section section) {
		if (!section.contains("type")) {
			throw new RuntimeException(section.getRouteAsString());
		}
		Action<Player>[] actions = instance.getActionManager().parseActions(section.getSection("actions"));
		String type = section.getString("type");
		if (type == null) {
			throw new RuntimeException(section.getRouteAsString());
		}
		switch (type) {
		case "lava-fishing" -> {
			return (((effect, context, phase) -> {
				if (phase == 0) {
					effect.properties().put(EffectProperties.LAVA_FISHING, true);
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "weight-mod" -> {
			var op = parseWeightOperation(section.getStringList("value"));
			return (((effect, context, phase) -> {
				if (phase == 1) {
					effect.weightOperations(op);
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "weight-mod-ignore-conditions" -> {
			var op = parseWeightOperation(section.getStringList("value"));
			return (((effect, context, phase) -> {
				if (phase == 1) {
					effect.weightOperationsIgnored(op);
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "group-mod", "group_mod" -> {
			var op = parseGroupWeightOperation(section.getStringList("value"));
			return (((effect, context, phase) -> {
				if (phase == 1) {
					effect.weightOperations(op);
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "group-mod-ignore-conditions", "group_mod_ignore_conditions" -> {
			var op = parseGroupWeightOperation(section.getStringList("value"));
			return (((effect, context, phase) -> {
				if (phase == 1) {
					effect.weightOperationsIgnored(op);
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "wait-time", "wait_time" -> {
			MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.waitTimeAdder(effect.waitTimeAdder() + value.evaluate(context));
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "hook-time", "hook_time", "wait-time-multiplier", "wait_time_multiplier" -> {
			MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.waitTimeMultiplier(effect.waitTimeMultiplier() - 1 + value.evaluate(context));
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "size" -> {
			MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.sizeAdder(effect.sizeAdder() + value.evaluate(context));
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "size-multiplier", "size-bonus" -> {
			MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.sizeMultiplier(effect.sizeMultiplier() - 1 + value.evaluate(context));
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "multiple-loot" -> {
			MathValue<Player> value = MathValue.auto(section.get("value"));
			return (((effect, context, phase) -> {
				if (phase == 2) {
					effect.multipleLootChance(effect.multipleLootChance() + value.evaluate(context));
					ActionManagerInterface.trigger(context, actions);
				}
			}));
		}
		case "conditional" -> {
			Requirement<Player>[] requirements = instance.getRequirementManager()
					.parseRequirements(section.getSection("conditions"), true);
			Section effectSection = section.getSection("effects");
			List<TriConsumer<Effect, Context<Player>, Integer>> effects = new ArrayList<>();
			if (effectSection != null)
				for (Map.Entry<String, Object> entry : effectSection.getStringRouteMappedValues(false).entrySet())
					if (entry.getValue() instanceof Section inner)
						effects.add(parseEffect(inner));
			return (((effect, context, phase) -> {
				if (!RequirementManagerInterface.isSatisfied(context, requirements))
					return;
				for (TriConsumer<Effect, Context<Player>, Integer> consumer : effects) {
					consumer.accept(effect, context, phase);
				}
			}));
		}
		default -> {
			return (((effect, context, phase) -> {
			}));
		}
		}
	}

	private BiFunction<Context<Player>, Double, Double> parseWeightOperation(String op) {
		switch (op.charAt(0)) {
		case '/' -> {
			MathValue<Player> arg = MathValue.auto(op.substring(1));
			return (context, weight) -> weight / arg.evaluate(context);
		}
		case '*' -> {
			MathValue<Player> arg = MathValue.auto(op.substring(1));
			return (context, weight) -> weight * arg.evaluate(context);
		}
		case '-' -> {
			MathValue<Player> arg = MathValue.auto(op.substring(1));
			return (context, weight) -> weight - arg.evaluate(context);
		}
		case '%' -> {
			MathValue<Player> arg = MathValue.auto(op.substring(1));
			return (context, weight) -> weight % arg.evaluate(context);
		}
		case '+' -> {
			MathValue<Player> arg = MathValue.auto(op.substring(1));
			return (context, weight) -> weight + arg.evaluate(context);
		}
		case '=' -> {
			MathValue<Player> arg = MathValue.auto(op.substring(1));
			return (context, weight) -> {
				context.arg(ContextKeys.WEIGHT, weight);
				return arg.evaluate(context);
			};
		}
		default -> throw new IllegalArgumentException("Invalid weight operation: " + op);
		}
	}

	public List<Pair<String, BiFunction<Context<Player>, Double, Double>>> parseWeightOperation(List<String> ops) {
		List<Pair<String, BiFunction<Context<Player>, Double, Double>>> result = new ArrayList<>();
		for (String op : ops) {
			String[] split = op.split(":", 2);
			if (split.length < 2) {
				instance.getPluginLogger().warn("Illegal weight operation: " + op);
				continue;
			}
			result.add(Pair.of(split[0], parseWeightOperation(split[1])));
		}
		return result;
	}

	public List<Pair<String, BiFunction<Context<Player>, Double, Double>>> parseGroupWeightOperation(
			List<String> gops) {
		List<Pair<String, BiFunction<Context<Player>, Double, Double>>> result = new ArrayList<>();
		for (String gop : gops) {
			String[] split = gop.split(":", 2);
			if (split.length < 2) {
				instance.getPluginLogger().warn("Illegal weight operation: " + gop);
				continue;
			}
			BiFunction<Context<Player>, Double, Double> operation = parseWeightOperation(split[1]);
			for (String member : instance.getLootManager().getGroupMembers(split[0])) {
				result.add(Pair.of(member, operation));
			}
		}
		return result;
	}

	private void registerBuiltInHookParser() {
		this.registerHookParser(object -> {
			List<String> lore = ListUtils.toList(object);
			return builder -> builder.lore(lore.stream().map(it -> "<!i>" + it).toList());
		}, "lore-on-rod");
	}

	private void registerBuiltInBaseEffectParser() {
		this.registerBaseEffectParser(object -> {
			MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.waitTimeAdder(mathValue);
		}, "base-effects", "wait-time-adder");
		this.registerBaseEffectParser(object -> {
			MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.waitTimeMultiplier(mathValue);
		}, "base-effects", "wait-time-multiplier");
	}

	private void registerBuiltInBlockParser() {
		this.registerBlockParser(object -> {
			String block = (String) object;
			return builder -> builder.blockID(block);
		}, "block");
		this.registerBlockParser(object -> {
			Section section = (Section) object;
			List<BlockDataModifier> dataModifiers = new ArrayList<>();
			List<BlockStateModifier> stateModifiers = new ArrayList<>();
			for (Map.Entry<String, Object> innerEntry : section.getStringRouteMappedValues(false).entrySet()) {
				BlockDataModifierFactory dataModifierFactory = instance.getBlockManager()
						.getBlockDataModifierFactory(innerEntry.getKey());
				if (dataModifierFactory != null) {
					dataModifiers.add(dataModifierFactory.process(innerEntry.getValue()));
					continue;
				}
				BlockStateModifierFactory stateModifierFactory = instance.getBlockManager()
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
			MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.horizontalVector(mathValue);
		}, "velocity", "horizontal");
		this.registerBlockParser(object -> {
			MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.verticalVector(mathValue);
		}, "velocity", "vertical");
	}

	private void registerBuiltInEntityParser() {
		this.registerEntityParser(object -> {
			String entity = (String) object;
			return builder -> builder.entityID(entity);
		}, "entity");
		this.registerEntityParser(object -> {
			MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.horizontalVector(mathValue);
		}, "velocity", "horizontal");
		this.registerEntityParser(object -> {
			MathValue<Player> mathValue = MathValue.auto(object);
			return builder -> builder.verticalVector(mathValue);
		}, "velocity", "vertical");
		this.registerEntityParser(object -> {
			Section section = (Section) object;
			return builder -> builder.propertyMap(section.getStringRouteMappedValues(false));
		}, "properties");
	}

	private void registerBuiltInEventParser() {
		this.registerEventParser(object -> {
			boolean disable = (boolean) object;
			return builder -> builder.disableGlobalActions(disable);
		}, "disable-global-event");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.LURE, actions);
		}, "events", "lure");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.ESCAPE, actions);
		}, "events", "escape");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.SUCCESS, actions);
		}, "events", "success");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.ACTIVATE, actions);
		}, "events", "activate");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.FAILURE, actions);
		}, "events", "failure");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.HOOK, actions);
		}, "events", "hook");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.CONSUME, actions);
		}, "events", "consume");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.CAST, actions);
		}, "events", "cast");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.BITE, actions);
		}, "events", "bite");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.LAND, actions);
		}, "events", "land");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.TIMER, actions);
		}, "events", "timer");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.INTERACT, actions);
		}, "events", "interact");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.REEL, actions);
		}, "events", "reel");
		this.registerEventParser(object -> {
			Section section = (Section) object;
			Action<Player>[] actions = instance.getActionManager().parseActions(section);
			return builder -> builder.action(ActionTrigger.NEW_SIZE_RECORD, actions);
		}, "events", "new_size_record");
	}

	private void registerBuiltInLootParser() {
		this.registerLootParser(object -> {
			Section section = (Section) object;
			Map<String, TextValue<Player>> data = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof String str) {
					data.put(entry.getKey(), TextValue.auto(str));
				} else {
					data.put(entry.getKey(), TextValue.auto(entry.getValue().toString()));
				}
			}
			return builder -> {
				builder.customData(data);
			};
		}, "custom-data");
		this.registerLootParser(object -> {
			if (object instanceof Boolean b) {
				return builder -> builder.toInventory(MathValue.plain(b ? 1 : 0));
			} else {
				return builder -> builder.toInventory(MathValue.auto(object));
			}
		}, "to-inventory");
		this.registerLootParser(object -> {
			boolean value = (boolean) object;
			return builder -> builder.preventGrabbing(value);
		}, "prevent-grabbing");
		this.registerLootParser(object -> {
			String string = (String) object;
			return builder -> builder.nick(string);
		}, "nick");
		this.registerLootParser(object -> {
			boolean value = (boolean) object;
			return builder -> builder.showInFinder(value);
		}, "show-in-fishfinder");
		this.registerLootParser(object -> {
			boolean value = (boolean) object;
			return builder -> builder.disableStatistics(value);
		}, "disable-stat");
		this.registerLootParser(object -> {
			List<String> args = ListUtils.toList(object);
			return builder -> builder.groups(args.toArray(new String[0]));
		}, "group");
		this.registerLootParser(object -> {
			Section section = (Section) object;
			StatisticsKeys keys = new StatisticsKeys(section.getString("amount"), section.getString("size"));
			return builder -> builder.statisticsKeys(keys);
		}, "statistics");
	}

	public class TitleScreenInfo {

		private boolean enabled;
		private String title;
		private String subtitle;
		private int fadeIn;
		private int stay;
		private int fadeOut;

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