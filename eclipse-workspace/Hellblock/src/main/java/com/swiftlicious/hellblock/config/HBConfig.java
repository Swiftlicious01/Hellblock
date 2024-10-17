package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventPriority;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.OffsetUtils;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

public class HBConfig {

    // config version
    public static String configVersion = "1";
    // Debug mode
    public static boolean debug;
    
    // update checker
    public static boolean updateChecker;

    // BStats
    public static boolean metrics;

    // fishing event priority
    public static EventPriority eventPriority;

    // thread pool settings
    public static int corePoolSize;
    public static int maximumPoolSize;
    public static int keepAliveTime;

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

    public static void load() {
        try {
            YamlDocument.create(
                    new File(HellblockPlugin.getInstance().getDataFolder(), "config.yml"),
                    Objects.requireNonNull(HellblockPlugin.getInstance().getResource("config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings
                            .builder()
                            .setAutoUpdate(true)
                            .build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings
                            .builder()
                            .setVersioning(new BasicVersioning("config-version"))
                            .addIgnoredRoute(configVersion, "mechanism.mechanism-requirements", '.')
                            .addIgnoredRoute(configVersion, "mechanism.global-events", '.')
                            .addIgnoredRoute(configVersion, "mechanism.global-effects", '.')
                            .addIgnoredRoute(configVersion, "other-settings.placeholder-register", '.')
                            .build()
            );
            loadSettings(HellblockPlugin.getInstance().getConfig("config.yml"));
        } catch (IOException e) {
            LogUtils.warn(e.getMessage());
        }
    }

    private static void loadSettings(YamlConfiguration config) {
        debug = config.getBoolean("debug", false);

        updateChecker = config.getBoolean("update-checker", true);
        metrics = config.getBoolean("metrics", true);
        eventPriority = EventPriority.valueOf(config.getString("other-settings.event-priority", "NORMAL").toUpperCase(Locale.ENGLISH));

        corePoolSize = config.getInt("other-settings.thread-pool-settings.corePoolSize", 1);
        maximumPoolSize = config.getInt("other-settings.thread-pool-settings.maximumPoolSize", 1);
        keepAliveTime = config.getInt("other-settings.thread-pool-settings.keepAliveTime", 30);

        itemDetectOrder = config.getStringList("other-settings.item-detection-order");
        blockDetectOrder = config.getStringList("other-settings.block-detection-order");

        overrideVanilla = config.getBoolean("mechanism.fishing-wait-time.override-vanilla", false);

        lavaMinTime = config.getInt("mechanism.lava-fishing.min-wait-time", 100);
        lavaMaxTime = config.getInt("mechanism.lava-fishing.max-wait-time", 600);

        restrictedSizeRange = config.getBoolean("mechanism.size.restricted-size-range", true);

        globalShowInFinder = config.getBoolean("mechanism.global-loot-property.show-in-fishfinder", true);

        multipleLootSpawnDelay = config.getInt("mechanism.multiple-loot-spawn-delay", 0);

        dataSaveInterval = config.getInt("other-settings.data-saving-interval", 600);
        logDataSaving = config.getBoolean("other-settings.log-data-saving", true);
        lockData = config.getBoolean("other-settings.lock-data", true);
        legacyColorSupport = config.getBoolean("other-settings.legacy-color-code-support", false);

        durabilityLore = config.getStringList("other-settings.custom-durability-format").stream().map(it -> "<!i>" + it).toList();

        OffsetUtils.loadConfig(config.getConfigurationSection("other-settings.offset-characters"));
    }
}