package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.event.EventPriority;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.HellblockProperties;
import com.swiftlicious.hellblock.utils.OffsetUtils;

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

public class HBConfig extends ConfigHandler {

	private static YamlDocument MAIN_CONFIG;

	public static YamlDocument getMainConfig() {
		return MAIN_CONFIG;
	}

	public HBConfig(HellblockPlugin plugin) {
		super(plugin);
	}

	// Debug mode
	public static boolean debug;

	// update checker
	public static boolean updateChecker;

	// BStats
	public static boolean metrics;
	
	// language choice
	public static String language;

	// fishing event priority
	public static EventPriority eventPriority;

	// hellblock settings
	public static String worldName;
	public static int spawnSize;
	public static String paster;
	public static int height;
	public static int distance;
	public static List<String> islandOptions;
	public static boolean worldguardProtected;
	public static boolean disableBedExplosions;
	public static boolean voidTeleport;
	public static boolean resetInventory;
	public static boolean entryMessageEnabled, farewellMessageEnabled;
	public static int protectionRange;
	public static int abandonAfterDays;
	public static String chestInventoryName;
	public static Section chestItems;
	public static List<String> blockLevelSystem;
	public static boolean growNaturalTrees;
	public static int partySizeLimit;
	public static boolean transferIslands;

	// lava rain settings
	public static int lavaRainRadius;
	public static int lavaRainFireChance;
	public static int lavaRainTaskDelay;
	public static boolean lavaRainEnabled;
	public static boolean canHurtLivingCreatures;
	public static boolean willTNTExplode;

	// wither settings
	public static boolean randomWither;
	public static int witherHealthRangeMin;
	public static int witherHealthRangeMax;
	public static double witherStrengthRangeMin;
	public static double witherStrengthRangeMax;
	public static double witherStrength;
	public static int witherHealth;

	// armor settings
	public static boolean nrArmor;
	public static String nrHelmetName;
	public static List<String> nrHelmetLore;
	public static List<String> nrHelmetEnchants;
	public static String nrChestplateName;
	public static List<String> nrChestplateLore;
	public static List<String> nrChestplateEnchants;
	public static String nrLeggingsName;
	public static List<String> nrLeggingsLore;
	public static List<String> nrLeggingsEnchants;
	public static String nrBootsName;
	public static List<String> nrBootsLore;
	public static List<String> nrBootsEnchants;
	public static boolean gsArmor;
	public static boolean gsNightVisionArmor;
	public static String gsHelmetName;
	public static List<String> gsHelmetLore;
	public static List<String> gsHelmetEnchants;
	public static String gsChestplateName;
	public static List<String> gsChestplateLore;
	public static List<String> gsChestplateEnchants;
	public static String gsLeggingsName;
	public static List<String> gsLeggingsLore;
	public static List<String> gsLeggingsEnchants;
	public static String gsBootsName;
	public static List<String> gsBootsLore;
	public static List<String> gsBootsEnchants;
	public static boolean qzArmor;
	public static String qzHelmetName;
	public static List<String> qzHelmetLore;
	public static List<String> qzHelmetEnchants;
	public static String qzChestplateName;
	public static List<String> qzChestplateLore;
	public static List<String> qzChestplateEnchants;
	public static String qzLeggingsName;
	public static List<String> qzLeggingsLore;
	public static List<String> qzLeggingsEnchants;
	public static String qzBootsName;
	public static List<String> qzBootsLore;
	public static List<String> qzBootsEnchants;
	public static boolean nsArmor;
	public static String nsHelmetName;
	public static List<String> nsHelmetLore;
	public static List<String> nsHelmetEnchants;
	public static String nsChestplateName;
	public static List<String> nsChestplateLore;
	public static List<String> nsChestplateEnchants;
	public static String nsLeggingsName;
	public static List<String> nsLeggingsLore;
	public static List<String> nsLeggingsEnchants;
	public static String nsBootsName;
	public static List<String> nsBootsLore;
	public static List<String> nsBootsEnchants;

	// tool settings
	public static boolean nrTools;
	public static String nrPickaxeName;
	public static List<String> nrPickaxeLore;
	public static List<String> nrPickaxeEnchants;
	public static String nrAxeName;
	public static List<String> nrAxeLore;
	public static List<String> nrAxeEnchants;
	public static String nrShovelName;
	public static List<String> nrShovelLore;
	public static List<String> nrShovelEnchants;
	public static String nrHoeName;
	public static List<String> nrHoeLore;
	public static List<String> nrHoeEnchants;
	public static String nrSwordName;
	public static List<String> nrSwordLore;
	public static List<String> nrSwordEnchants;
	public static boolean gsTools;
	public static boolean gsNightVisionTool;
	public static String gsPickaxeName;
	public static List<String> gsPickaxeLore;
	public static List<String> gsPickaxeEnchants;
	public static String gsAxeName;
	public static List<String> gsAxeLore;
	public static List<String> gsAxeEnchants;
	public static String gsShovelName;
	public static List<String> gsShovelLore;
	public static List<String> gsShovelEnchants;
	public static String gsHoeName;
	public static List<String> gsHoeLore;
	public static List<String> gsHoeEnchants;
	public static String gsSwordName;
	public static List<String> gsSwordLore;
	public static List<String> gsSwordEnchants;
	public static boolean qzTools;
	public static String qzPickaxeName;
	public static List<String> qzPickaxeLore;
	public static List<String> qzPickaxeEnchants;
	public static String qzAxeName;
	public static List<String> qzAxeLore;
	public static List<String> qzAxeEnchants;
	public static String qzShovelName;
	public static List<String> qzShovelLore;
	public static List<String> qzShovelEnchants;
	public static String qzHoeName;
	public static List<String> qzHoeLore;
	public static List<String> qzHoeEnchants;
	public static String qzSwordName;
	public static List<String> qzSwordLore;
	public static List<String> qzSwordEnchants;
	public static boolean nsTools;
	public static String nsPickaxeName;
	public static List<String> nsPickaxeLore;
	public static List<String> nsPickaxeEnchants;
	public static String nsAxeName;
	public static List<String> nsAxeLore;
	public static List<String> nsAxeEnchants;
	public static String nsShovelName;
	public static List<String> nsShovelLore;
	public static List<String> nsShovelEnchants;
	public static String nsHoeName;
	public static List<String> nsHoeLore;
	public static List<String> nsHoeEnchants;
	public static String nsSwordName;
	public static List<String> nsSwordLore;
	public static List<String> nsSwordEnchants;

	// netherrack generator settings
	public static double searchRadius;
	public static List<String> generationResults;
	public static boolean pistonAutomationEnabled;

	// piglin settings
	public static boolean clearPiglinBarterOutcome;
	public static List<String> piglinBarteringItems;

	// infinite lava settings
	public static boolean infiniteLavaEnabled;

	// brewing settings
	public static boolean nBottle;
	public static String nBottleName;
	public static List<String> nBottleLore;
	public static String nBottleColor;

	// detection order for item id
	public static List<String> itemDetectOrder = new ArrayList<>();
	public static List<String> blockDetectOrder = new ArrayList<>();

	// Fishing wait time
	public static boolean overrideVanilla;
	// Lava fishing
	public static int lavaMinTime;
	public static int lavaMaxTime;

	// Data save interval
	public static int dataSaveInterval;
	// Lock data on join
	public static boolean lockData;
	public static boolean logDataSaving;

	public static boolean restrictedSizeRange;

	// Legacy color code support
	public static boolean legacyColorSupport;
	// Durability lore
	public static List<String> durabilityLore;

	public static boolean globalShowInFinder;

	public static int multipleLootSpawnDelay;

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

		loadSettings();
	}

	private void loadSettings() {
		YamlDocument config = getMainConfig();

		debug = config.getBoolean("debug", false);
		
		language = config.getString("lang", "en");

		updateChecker = config.getBoolean("update-checker", true);
		metrics = config.getBoolean("metrics", true);
		eventPriority = EventPriority
				.valueOf(config.getString("other-settings.event-priority", "NORMAL").toUpperCase(Locale.ENGLISH));

		worldName = config.getString("general.world", "hellworld");
		spawnSize = config.getInt("general.spawn-size", 25);
		paster = config.getString("hellblock.schematic-paster", "worldedit");
		chestItems = config.getSection("hellblock.starter-chest.items");
		chestInventoryName = config.getString("hellblock.starter-chest.inventory-name");
		blockLevelSystem = config.getStringList("level-system.blocks");
		height = config.getInt("hellblock.height", 150);
		islandOptions = config.getStringList("hellblock.island-options");
		distance = config.getInt("hellblock.distance", 110);
		worldguardProtected = config.getBoolean("hellblock.use-worldguard-protection");
		resetInventory = config.getBoolean("hellblock.clear-inventory-on-reset", true);
		entryMessageEnabled = config.getBoolean("hellblock.entry-message-enabled", true);
		farewellMessageEnabled = config.getBoolean("hellblock.farewell-message-enabled", true);
		disableBedExplosions = config.getBoolean("hellblock.disable-bed-explosions", true);
		voidTeleport = config.getBoolean("hellblock.void-teleport", true);
		protectionRange = config.getInt("hellblock.protection-range", 105);
		abandonAfterDays = config.getInt("hellblock.abandon-after-days", 30);
		partySizeLimit = config.getInt("hellblock.party-size", 4);
		transferIslands = config.getBoolean("hellblock.can-transfer-islands", true);
		growNaturalTrees = config.getBoolean("hellblock.grow-natural-trees", false);

		lavaRainRadius = Math.abs(config.getInt("lava-rain-options.radius", 16));
		lavaRainFireChance = Math.abs(config.getInt("lava-rain-options.fire-chance", 1));
		lavaRainTaskDelay = Math.abs(config.getInt("lava-rain-options.task-delay", 3));
		lavaRainEnabled = config.getBoolean("lava-rain-options.enabled", true);
		canHurtLivingCreatures = config.getBoolean("lava-rain-options.can-hurt-living-creatures", true);
		willTNTExplode = config.getBoolean("lava-rain-options.will-tnt-explode", true);

		nrArmor = config.getBoolean("armor.netherrack.enable", true);
		nrHelmetName = config.getString("armor.netherrack.helmet.name");
		nrHelmetLore = config.getStringList("armor.netherrack.helmet.lore");
		nrHelmetEnchants = config.getStringList("armor.netherrack.helmet.enchantments");
		nrChestplateName = config.getString("armor.netherrack.chestplate.name");
		nrChestplateLore = config.getStringList("armor.netherrack.chestplate.lore");
		nrChestplateEnchants = config.getStringList("armor.netherrack.chestplate.enchantments");
		nrLeggingsName = config.getString("armor.netherrack.leggings.name");
		nrLeggingsLore = config.getStringList("armor.netherrack.leggings.lore");
		nrLeggingsEnchants = config.getStringList("armor.netherrack.leggings.enchantments");
		nrBootsName = config.getString("armor.netherrack.boots.name");
		nrBootsLore = config.getStringList("armor.netherrack.boots.lore");
		nrBootsEnchants = config.getStringList("armor.netherrack.boots.enchantments");
		gsArmor = config.getBoolean("armor.glowstone.enable", true);
		gsNightVisionArmor = config.getBoolean("armor.glowstone.night-vision", true);
		gsHelmetName = config.getString("armor.glowstone.helmet.name");
		gsHelmetLore = config.getStringList("armor.glowstone.helmet.lore");
		gsHelmetEnchants = config.getStringList("armor.glowstone.helmet.enchantments");
		gsChestplateName = config.getString("armor.glowstone.chestplate.name");
		gsChestplateLore = config.getStringList("armor.glowstone.chestplate.lore");
		gsChestplateEnchants = config.getStringList("armor.glowstone.chestplate.enchantments");
		gsLeggingsName = config.getString("armor.glowstone.leggings.name");
		gsLeggingsLore = config.getStringList("armor.glowstone.leggings.lore");
		gsLeggingsEnchants = config.getStringList("armor.glowstone.leggings.enchantments");
		gsBootsName = config.getString("armor.glowstone.boots.name");
		gsBootsLore = config.getStringList("armor.glowstone.boots.lore");
		gsBootsEnchants = config.getStringList("armor.glowstone.boots.enchantments");
		qzArmor = config.getBoolean("armor.quartz.enable", true);
		qzHelmetName = config.getString("armor.quartz.helmet.name");
		qzHelmetLore = config.getStringList("armor.quartz.helmet.lore");
		qzHelmetEnchants = config.getStringList("armor.quartz.helmet.enchantments");
		qzChestplateName = config.getString("armor.quartz.chestplate.name");
		qzChestplateLore = config.getStringList("armor.quartz.chestplate.lore");
		qzChestplateEnchants = config.getStringList("armor.quartz.chestplate.enchantments");
		qzLeggingsName = config.getString("armor.quartz.leggings.name");
		qzLeggingsLore = config.getStringList("armor.quartz.leggings.lore");
		qzLeggingsEnchants = config.getStringList("armor.quartz.leggings.enchantments");
		qzBootsName = config.getString("armor.quartz.boots.name");
		qzBootsLore = config.getStringList("armor.quartz.boots.lore");
		qzBootsEnchants = config.getStringList("armor.quartz.boots.enchantments");
		nsArmor = config.getBoolean("armor.netherstar.enable", true);
		nsHelmetName = config.getString("armor.netherstar.helmet.name");
		nsHelmetLore = config.getStringList("armor.netherstar.helmet.lore");
		nsHelmetEnchants = config.getStringList("armor.netherstar.helmet.enchantments");
		nsChestplateName = config.getString("armor.netherstar.chestplate.name");
		nsChestplateLore = config.getStringList("armor.netherstar.chestplate.lore");
		nsChestplateEnchants = config.getStringList("armor.netherstar.chestplate.enchantments");
		nsLeggingsName = config.getString("armor.netherstar.leggings.name");
		nsLeggingsLore = config.getStringList("armor.netherstar.leggings.lore");
		nsLeggingsEnchants = config.getStringList("armor.netherstar.leggings.enchantments");
		nsBootsName = config.getString("armor.netherstar.boots.name");
		nsBootsLore = config.getStringList("armor.netherstar.boots.lore");
		nsBootsEnchants = config.getStringList("armor.netherstar.boots.enchantments");

		nrTools = config.getBoolean("tools.netherrack.enable", true);
		nrPickaxeName = config.getString("tools.netherrack.pickaxe.name");
		nrPickaxeLore = config.getStringList("tools.netherrack.pickaxe.lore");
		nrPickaxeEnchants = config.getStringList("tools.netherrack.pickaxe.enchantments");
		nrAxeName = config.getString("tools.netherrack.axe.name");
		nrAxeLore = config.getStringList("tools.netherrack.axe.lore");
		nrAxeEnchants = config.getStringList("tools.netherrack.axe.enchantments");
		nrShovelName = config.getString("tools.netherrack.shovel.name");
		nrShovelLore = config.getStringList("tools.netherrack.shovel.lore");
		nrShovelEnchants = config.getStringList("tools.netherrack.shovel.enchantments");
		nrHoeName = config.getString("tools.netherrack.hoe.name");
		nrHoeLore = config.getStringList("tools.netherrack.hoe.lore");
		nrHoeEnchants = config.getStringList("tools.netherrack.hoe.enchantments");
		nrSwordName = config.getString("tools.netherrack.sword.name");
		nrSwordLore = config.getStringList("tools.netherrack.sword.lore");
		nrSwordEnchants = config.getStringList("tools.netherrack.sword.enchantments");
		gsTools = config.getBoolean("tools.glowstone.enable", true);
		gsNightVisionTool = config.getBoolean("tools.glowstone.night-vision", true);
		gsPickaxeName = config.getString("tools.glowstone.pickaxe.name");
		gsPickaxeLore = config.getStringList("tools.glowstone.pickaxe.lore");
		gsPickaxeEnchants = config.getStringList("tools.glowstone.pickaxe.enchantments");
		gsAxeName = config.getString("tools.glowstone.axe.name");
		gsAxeLore = config.getStringList("tools.glowstone.axe.lore");
		gsAxeEnchants = config.getStringList("tools.glowstone.axe.enchantments");
		gsShovelName = config.getString("tools.glowstone.shovel.name");
		gsShovelLore = config.getStringList("tools.glowstone.shovel.lore");
		gsShovelEnchants = config.getStringList("tools.glowstone.shovel.enchantments");
		gsHoeName = config.getString("tools.glowstone.hoe.name");
		gsHoeLore = config.getStringList("tools.glowstone.hoe.lore");
		gsHoeEnchants = config.getStringList("tools.glowstone.hoe.enchantments");
		gsSwordName = config.getString("tools.glowstone.sword.name");
		gsSwordLore = config.getStringList("tools.glowstone.sword.lore");
		gsSwordEnchants = config.getStringList("tools.glowstone.sword.enchantments");
		qzTools = config.getBoolean("tools.quartz.enable", true);
		qzPickaxeName = config.getString("tools.quartz.pickaxe.name");
		qzPickaxeLore = config.getStringList("tools.quartz.pickaxe.lore");
		qzPickaxeEnchants = config.getStringList("tools.quartz.pickaxe.enchantments");
		qzAxeName = config.getString("tools.quartz.axe.name");
		qzAxeLore = config.getStringList("tools.quartz.axe.lore");
		qzAxeEnchants = config.getStringList("tools.quartz.axe.enchantments");
		qzShovelName = config.getString("tools.quartz.shovel.name");
		qzShovelLore = config.getStringList("tools.quartz.shovel.lore");
		qzShovelEnchants = config.getStringList("tools.quartz.shovel.enchantments");
		qzHoeName = config.getString("tools.quartz.hoe.name");
		qzHoeLore = config.getStringList("tools.quartz.hoe.lore");
		qzHoeEnchants = config.getStringList("tools.quartz.hoe.enchantments");
		qzSwordName = config.getString("tools.quartz.sword.name");
		qzSwordLore = config.getStringList("tools.quartz.sword.lore");
		qzSwordEnchants = config.getStringList("tools.quartz.sword.enchantments");
		nsTools = config.getBoolean("tools.netherstar.enable", true);
		nsPickaxeName = config.getString("tools.netherstar.pickaxe.name");
		nsPickaxeLore = config.getStringList("tools.netherstar.pickaxe.lore");
		nsPickaxeEnchants = config.getStringList("tools.netherstar.pickaxe.enchantments");
		nsAxeName = config.getString("tools.netherstar.axe.name");
		nsAxeLore = config.getStringList("tools.netherstar.axe.lore");
		nsAxeEnchants = config.getStringList("tools.netherstar.axe.enchantments");
		nsShovelName = config.getString("tools.netherstar.shovel.name");
		nsShovelLore = config.getStringList("tools.netherstar.shovel.lore");
		nsShovelEnchants = config.getStringList("tools.netherstar.shovel.enchantments");
		nsHoeName = config.getString("tools.netherstar.hoe.name");
		nsHoeLore = config.getStringList("tools.netherstar.hoe.lore");
		nsHoeEnchants = config.getStringList("tools.netherstar.hoe.enchantments");
		nsSwordName = config.getString("tools.netherstar.sword.name");
		nsSwordLore = config.getStringList("tools.netherstar.sword.lore");
		nsSwordEnchants = config.getStringList("tools.netherstar.sword.enchantments");

		generationResults = config.getSection("netherrack-generator-options.generation").getStringList("blocks");
		searchRadius = config.getDouble("netherrack-generator-options.player-search-radius", 4D);
		pistonAutomationEnabled = config.getSection("netherrack-generator-options.automation").getBoolean("pistons",
				false);

		clearPiglinBarterOutcome = config.getBoolean("piglin-bartering.clear-default-outcome", true);
		piglinBarteringItems = config.getStringList("piglin-bartering.materials");

		infiniteLavaEnabled = config.getBoolean("infinite-lava-options.enabled", true);

		randomWither = config.getBoolean("wither-stats.random-stats", true);
		witherHealthRangeMin = config.getInt("wither-stats.random-min-health", 200);
		witherHealthRangeMax = config.getInt("wither-stats.random-max-health", 500);
		if (witherHealthRangeMin <= 0)
			witherHealthRangeMin = 200;
		if (witherHealthRangeMax <= 0)
			witherHealthRangeMax = 500;
		witherStrengthRangeMin = config.getDouble("wither-stats.random-min-strength", 0.5);
		witherStrengthRangeMax = config.getDouble("wither-stats.random-max-strength", 2.5);
		if (witherStrengthRangeMin <= 0)
			witherStrengthRangeMin = 0.5;
		if (witherStrengthRangeMax <= 0)
			witherStrengthRangeMax = 2.5;
		witherStrength = config.getDouble("wither-stats.default-strength", 1.25);
		witherHealth = config.getInt("wither-stats.default-health", 300);

		nBottle = config.getBoolean("brewing.nether-bottle.enable", true);
		nBottleName = config.getString("brewing.nether-bottle.potion.name");
		nBottleColor = config.getString("brewing.nether-bottle.potion.color", "RED");
		nBottleLore = config.getStringList("brewing.nether-bottle.potion.lore");

		itemDetectOrder = config.getStringList("other-settings.item-detection-order");
		blockDetectOrder = config.getStringList("other-settings.block-detection-order");

		overrideVanilla = config.getBoolean("lava-fishing-options.fishing-wait-time.override-vanilla", false);

		lavaMinTime = config.getInt("lava-fishing-options.lava-fishing.min-wait-time", 100);
		lavaMaxTime = config.getInt("lava-fishing-options.lava-fishing.max-wait-time", 600);

		restrictedSizeRange = config.getBoolean("lava-fishing-options.size.restricted-size-range", true);

		globalShowInFinder = config.getBoolean("lava-fishing-options.global-loot-property.show-in-fishfinder", true);

		multipleLootSpawnDelay = config.getInt("lava-fishing-options.multiple-loot-spawn-delay", 0);

		dataSaveInterval = config.getInt("other-settings.data-saving-interval", 600);
		logDataSaving = config.getBoolean("other-settings.log-data-saving", true);
		lockData = config.getBoolean("other-settings.lock-data", true);
		legacyColorSupport = config.getBoolean("other-settings.legacy-color-code-support", false);

		durabilityLore = config.getStringList("other-settings.custom-durability-format").stream().map(it -> "<!i>" + it)
				.toList();

		OffsetUtils.loadConfig(config.getSection("other-settings.offset-characters"));
	}

	@Override
	public void saveResource(String filePath) {
		if (!new File(instance.getDataFolder(), filePath).exists()) {
			instance.saveResource(filePath, false);
		}
	}
}