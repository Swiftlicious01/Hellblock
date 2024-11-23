package com.swiftlicious.hellblock;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.compatibility.Metrics;
import com.swiftlicious.hellblock.api.compatibility.WorldEditHook;
import com.swiftlicious.hellblock.api.compatibility.WorldGuardHook;
import com.swiftlicious.hellblock.challenges.ChallengeRewardBuilder;
import com.swiftlicious.hellblock.commands.BukkitCommandManager;
import com.swiftlicious.hellblock.config.ConfigManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.locale.TranslationManager;
import com.swiftlicious.hellblock.coop.CoopManager;
import com.swiftlicious.hellblock.creation.addons.IntegrationManager;
import com.swiftlicious.hellblock.creation.block.BlockManager;
import com.swiftlicious.hellblock.creation.entity.EntityManager;
import com.swiftlicious.hellblock.creation.item.ItemManager;
import com.swiftlicious.hellblock.database.StorageManager;
import com.swiftlicious.hellblock.database.dependency.Dependency;
import com.swiftlicious.hellblock.database.dependency.DependencyManager;
import com.swiftlicious.hellblock.database.dependency.classpath.ReflectionClassPathAppender;
import com.swiftlicious.hellblock.database.dependency.relocation.RelocationHandler;
import com.swiftlicious.hellblock.effects.EffectManager;
import com.swiftlicious.hellblock.events.fishing.LavaFishingReloadEvent;
import com.swiftlicious.hellblock.generation.BiomeHandler;
import com.swiftlicious.hellblock.generation.HellblockHandler;
import com.swiftlicious.hellblock.generation.IslandChoiceConverter;
import com.swiftlicious.hellblock.generation.IslandGenerator;
import com.swiftlicious.hellblock.gui.hellblock.HellblockGUIManager;
import com.swiftlicious.hellblock.gui.market.MarketManager;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureManager;
import com.swiftlicious.hellblock.handlers.CoolDownManager;
import com.swiftlicious.hellblock.handlers.EventManager;
import com.swiftlicious.hellblock.handlers.HologramManager;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.listeners.GlowstoneTree;
import com.swiftlicious.hellblock.listeners.InfiniteLava;
import com.swiftlicious.hellblock.listeners.IslandLevelHandler;
import com.swiftlicious.hellblock.listeners.NetherArmor;
import com.swiftlicious.hellblock.listeners.NetherBrewing;
import com.swiftlicious.hellblock.listeners.NetherFarming;
import com.swiftlicious.hellblock.listeners.NetherSnowGolem;
import com.swiftlicious.hellblock.listeners.NetherTools;
import com.swiftlicious.hellblock.listeners.NetherrackGenerator;
import com.swiftlicious.hellblock.listeners.PiglinBartering;
import com.swiftlicious.hellblock.listeners.PlayerListener;
import com.swiftlicious.hellblock.listeners.WitherBoss;
import com.swiftlicious.hellblock.listeners.fishing.FishingManager;
import com.swiftlicious.hellblock.listeners.fishing.HookManager;
import com.swiftlicious.hellblock.listeners.fishing.StatisticsManager;
import com.swiftlicious.hellblock.listeners.rain.LavaRain;
import com.swiftlicious.hellblock.logging.JavaPluginLogger;
import com.swiftlicious.hellblock.logging.PluginLogger;
import com.swiftlicious.hellblock.loot.LootManager;
import com.swiftlicious.hellblock.placeholders.PlaceholderManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.IslandProtection;
import com.swiftlicious.hellblock.scheduler.BukkitSchedulerAdapter;
import com.swiftlicious.hellblock.scheduler.AbstractJavaScheduler;
import com.swiftlicious.hellblock.schematic.SchematicManager;
import com.swiftlicious.hellblock.sender.SenderFactory;
import com.swiftlicious.hellblock.sender.BukkitSenderFactory;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.ReflectionUtils;
import com.swiftlicious.hellblock.utils.VersionManager;

import io.papermc.lib.PaperLib;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class HellblockPlugin extends JavaPlugin {

	protected static HellblockPlugin instance;

	protected GlowstoneTree glowstoneTreeHandler;
	protected LavaRain lavaRainHandler;
	protected InfiniteLava infiniteLavaHandler;
	protected NetherBrewing netherBrewingHandler;
	protected NetherFarming netherFarmingHandler;
	protected NetherTools netherToolsHandler;
	protected NetherArmor netherArmorHandler;
	protected PiglinBartering piglinBarterHandler;
	protected NetherSnowGolem netherSnowGolemHandler;
	protected WitherBoss witherBossHandler;
	protected PlayerListener playerListener;
	protected NetherrackGenerator netherrackGeneratorHandler;
	protected IslandGenerator islandGenerator;
	protected WorldEditHook worldEditHandler;
	protected WorldGuardHook worldGuardHandler;
	protected HellblockHandler hellblockHandler;
	protected BiomeHandler biomeHandler;
	protected IslandChoiceConverter islandChoiceConverter;
	protected CoopManager coopManager;
	protected IslandProtection islandProtectionManager;
	protected IslandLevelHandler islandLevelManager;
	protected SchematicManager schematicManager;
	protected ChallengeRewardBuilder challengeRewardBuilder;
	protected ConfigManager configManager;
	protected PluginLogger pluginLogger;
	protected SenderFactory<HellblockPlugin, CommandSender> senderFactory;

	protected AbstractJavaScheduler<Location> scheduler;
	protected LootManager lootManager;
	protected ActionManager actionManager;
	protected HologramManager hologramManager;
	protected HookManager hookManager;
	protected FishingManager fishingManager;
	protected EventManager eventManager;
	protected AdventureManager adventureManager;
	protected BukkitCommandManager commandManager;
	protected CoolDownManager cooldownManager;
	protected RequirementManager requirementManager;
	protected EffectManager effectManager;
	protected PlaceholderManager placeholderManager;
	protected EntityManager entityManager;
	protected BlockManager blockManager;
	protected ItemManager itemManager;
	protected IntegrationManager integrationManager;
	protected StatisticsManager statisticsManager;
	protected MarketManager marketManager;
	protected HellblockGUIManager hellblockGUIManager;
	protected VersionManager versionManager;
	protected StorageManager storageManager;
	protected DependencyManager dependencyManager;
	protected TranslationManager translationManager;

	protected boolean updateAvailable = false;
	protected Consumer<Supplier<String>> debugger = (supplier -> {
	});

	@Override
	public void onLoad() {
		instance = this;
		this.pluginLogger = new JavaPluginLogger(this.getLogger());
		this.versionManager = new VersionManager(this);
		this.scheduler = new BukkitSchedulerAdapter(this);
		this.dependencyManager = new DependencyManager(this, new ReflectionClassPathAppender(this.getClassLoader()));
		Set<Dependency> dependencies = new HashSet<>(Arrays.asList(Dependency.values()));
		dependencies.removeAll(RelocationHandler.DEPENDENCIES);
		getPluginLogger().info(String.format("Preloading %s dependencies...", dependencies.size()));
		this.dependencyManager.loadDependencies(dependencies);
	}

	@Override
	public void onEnable() {
		double startTime = System.currentTimeMillis();

		// TODO: possible backwards support up to 1.17
		if (!this.versionManager.getSupportedVersions().contains(this.versionManager.getServerVersion())) {
			getPluginLogger().severe(
					"Hellblock only supports legacy versions down to 1.17. Please update your server to be able to properly use this plugin.");
			getPluginLogger().severe("Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// TODO: add support for spigot
		if (!this.versionManager.isPaper()) {
			getPluginLogger().severe("Hellblock is more suited towards Paper, but will still work with Spigot.");
			PaperLib.suggestPaper(this);
		}

		ReflectionUtils.load();
		this.configManager = new ConfigManager(this);
		this.configManager.load();
		// after ConfigManager
		this.debugger = getConfigManager().debug() ? (s) -> pluginLogger.info("[DEBUG] " + s.get()) : (s) -> {
		};

		this.netherrackGeneratorHandler = new NetherrackGenerator(this);
		this.lavaRainHandler = new LavaRain(this);
		this.netherFarmingHandler = new NetherFarming(this);
		this.islandGenerator = new IslandGenerator(this);
		this.glowstoneTreeHandler = new GlowstoneTree(this);
		this.infiniteLavaHandler = new InfiniteLava(this);
		this.witherBossHandler = new WitherBoss(this);
		this.worldEditHandler = new WorldEditHook();
		this.worldGuardHandler = new WorldGuardHook(this);
		this.biomeHandler = new BiomeHandler(this);
		this.islandChoiceConverter = new IslandChoiceConverter(this);
		this.coopManager = new CoopManager(this);
		this.islandLevelManager = new IslandLevelHandler(this);
		this.islandProtectionManager = new IslandProtection(this);
		this.challengeRewardBuilder = new ChallengeRewardBuilder(this);

		this.senderFactory = new BukkitSenderFactory(this);
		this.actionManager = new ActionManager(this);
		this.adventureManager = new AdventureManager(this);
		this.blockManager = new BlockManager(this);
		this.effectManager = new EffectManager(this);
		this.fishingManager = new FishingManager(this);
		this.eventManager = new EventManager(this);
		this.itemManager = new ItemManager(this);
		this.lootManager = new LootManager(this);
		this.marketManager = new MarketManager(this);
		this.hellblockGUIManager = new HellblockGUIManager(this);
		this.entityManager = new EntityManager(this);
		this.placeholderManager = new PlaceholderManager(this);
		this.requirementManager = new RequirementManager(this);
		this.storageManager = new StorageManager(this);
		this.cooldownManager = new CoolDownManager(this);
		this.playerListener = new PlayerListener(this);
		this.hellblockHandler = new HellblockHandler(this);
		this.integrationManager = new IntegrationManager(this);
		this.statisticsManager = new StatisticsManager(this);
		this.hologramManager = new HologramManager(this);
		this.hookManager = new HookManager(this);
		this.schematicManager = new SchematicManager(this);
		this.translationManager = new TranslationManager(this);
		this.commandManager = new BukkitCommandManager(this);
		this.commandManager.registerDefaultFeatures();

		this.netherBrewingHandler = new NetherBrewing(this);
		this.netherToolsHandler = new NetherTools(this);
		this.netherArmorHandler = new NetherArmor(this);
		this.piglinBarterHandler = new PiglinBartering(this);
		this.netherSnowGolemHandler = new NetherSnowGolem(this);

		reload();

		if (getConfigManager().metrics())
			new Metrics(this, 23739);

		if (getConfigManager().checkUpdate()) {
			this.versionManager.checkUpdate.apply(this).thenAccept(result -> {
				String link = "https://github.com/Swiftlicious01/Hellblock/releases";
				if (!result) {
					getPluginLogger().info("You are using the latest version.");
				} else {
					getPluginLogger().info(String.format("Update is available: %s", link));
					this.updateAvailable = true;
				}
			});
		}

		double finishedTime = System.currentTimeMillis();
		double finalTime = (finishedTime - startTime) / 1000;
		getPluginLogger().info(String.format("Took %s seconds to setup Hellblock!", finalTime));

		for (UserData onlineUser : getStorageManager().getOnlineUsers()) {
			Player player = onlineUser.getPlayer();
			if (player == null)
				continue;
			UUID id = player.getUniqueId();
			onlineUser.showBorder();
			onlineUser.startSpawningAnimals();
			getNetherFarmingHandler().trackNetherFarms(onlineUser);
			getIslandLevelManager().loadCache(id);
			getNetherrackGeneratorHandler().loadPistons(id);
			if (getCoopManager().getHellblockOwnerOfVisitingIsland(player) != null) {
				if (getCoopManager().trackBannedPlayer(getCoopManager().getHellblockOwnerOfVisitingIsland(player),
						id)) {
					if (onlineUser.getHellblockData().hasHellblock()) {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						getStorageManager().getOfflineUserData(onlineUser.getHellblockData().getOwnerUUID(),
								getConfigManager().lockData()).thenAccept((owner) -> {
									UserData bannedOwner = owner.orElseThrow();
									getCoopManager().makeHomeLocationSafe(bannedOwner, onlineUser);
								});
					} else {
						getHellblockHandler().teleportToSpawn(player, true);
					}
				}
			}
		}

		int purgeDays = getConfigManager().abandonAfterDays();
		if (purgeDays > 0) {
			AtomicInteger purgeCount = new AtomicInteger(0);
			for (UUID id : getStorageManager().getDataSource().getUniqueUsers()) {
				if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
					continue;

				OfflinePlayer player = Bukkit.getOfflinePlayer(id);
				if (player.getLastLogin() == 0)
					continue;
				long millisSinceLastLogin = (System.currentTimeMillis() - player.getLastLogin()) -
				// Account for a timezone difference
						TimeUnit.MILLISECONDS.toHours(19);
				if (millisSinceLastLogin > TimeUnit.DAYS.toMillis(purgeDays)) {
					getStorageManager().getOfflineUserData(id, getConfigManager().lockData()).thenAccept((result) -> {
						UserData offlineUser = result.orElseThrow();
						if (offlineUser.getHellblockData().hasHellblock()
								&& offlineUser.getHellblockData().getOwnerUUID() != null) {
							if (getHellblockHandler().isHellblockOwner(id,
									offlineUser.getHellblockData().getOwnerUUID())) {
								float level = offlineUser.getHellblockData().getLevel();
								if (level == HellblockData.DEFAULT_LEVEL) {

									offlineUser.getHellblockData().setAsAbandoned(true);
									int hellblockID = offlineUser.getHellblockData().getID();
									if (getWorldGuardHandler().getRegion(offlineUser.getHellblockData().getOwnerUUID(),
											hellblockID) != null) {
										getWorldGuardHandler()
												.updateHellblockMessages(offlineUser.getHellblockData().getOwnerUUID(),
														getWorldGuardHandler().getRegion(
																offlineUser.getHellblockData().getOwnerUUID(),
																hellblockID));
										getWorldGuardHandler()
												.abandonIsland(offlineUser.getHellblockData().getOwnerUUID(),
														getWorldGuardHandler().getRegion(
																offlineUser.getHellblockData().getOwnerUUID(),
																hellblockID));
										purgeCount.getAndIncrement();
									}
								}
							}
						}
					}).join();
				}
			}
			if (purgeCount.get() > 0)
				getPluginLogger()
						.info(String.format("A total of %s hellblocks have been set as abandoned.", purgeCount.get()));
		}

		getLavaRainHandler().startLavaRainProcess();
		getScheduler().sync().runLater(() -> getHellblockHandler().getHellblockWorld(), 5 * 20, null);
	}

	@Override
	public void onDisable() {
		if (getStorageManager() != null) {
			Iterator<UserData> iterator = getStorageManager().getOnlineUsers().iterator();

			while (iterator.hasNext()) {
				UserData onlineUser = iterator.next();
				if (!onlineUser.isOnline())
					continue;
				onlineUser.hideBorder();
				onlineUser.stopSpawningAnimals();
				getIslandLevelManager().saveCache(onlineUser.getUUID());
				getNetherrackGeneratorHandler().savePistons(onlineUser.getUUID());
				Player player = onlineUser.getPlayer();
				if (onlineUser.hasGlowstoneToolEffect() || onlineUser.hasGlowstoneArmorEffect()) {
					if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						player.removePotionEffect(PotionEffectType.NIGHT_VISION);
						onlineUser.isHoldingGlowstoneTool(false);
						onlineUser.isWearingGlowstoneArmor(false);
					}
				}
			}
		}

		if (this.lavaRainHandler != null)
			this.lavaRainHandler.stopLavaRainProcess();
		if (this.blockManager != null)
			this.blockManager.disable();
		if (this.effectManager != null)
			this.effectManager.disable();
		if (this.fishingManager != null)
			this.fishingManager.disable();
		if (this.itemManager != null)
			this.itemManager.disable();
		if (this.lootManager != null)
			this.lootManager.disable();
		if (this.marketManager != null)
			this.marketManager.disable();
		if (this.hellblockGUIManager != null)
			this.hellblockGUIManager.disable();
		if (this.entityManager != null)
			this.entityManager.disable();
		if (this.requirementManager != null)
			this.requirementManager.disable();
		if (this.integrationManager != null)
			this.integrationManager.disable();
		if (this.storageManager != null)
			this.storageManager.disable();
		if (this.placeholderManager != null)
			this.placeholderManager.disable();
		if (this.actionManager != null)
			this.actionManager.disable();
		if (this.hookManager != null)
			this.hookManager.disable();
		if (this.cooldownManager != null)
			this.cooldownManager.disable();
		if (this.commandManager != null)
			this.commandManager.unregisterFeatures();
		if (this.scheduler != null) {
			this.scheduler.shutdownScheduler();
			this.scheduler.shutdownExecutor();
		}
		HandlerList.unregisterAll(this);

		if (instance != null)
			instance = null;
	}

	/**
	 * Reload the plugin
	 */
	public void reload() {
		this.lavaRainHandler.stopLavaRainProcess();
		this.configManager.reload();
		this.debugger = getConfigManager().debug() ? (s) -> pluginLogger.info("[DEBUG] " + s.get()) : (s) -> {
		};
		this.worldGuardHandler.reload();
		this.requirementManager.reload();
		this.actionManager.reload();
		this.statisticsManager.reload();
		this.itemManager.reload();
		this.lootManager.reload();
		this.cooldownManager.reload();
		this.fishingManager.reload();
		this.effectManager.reload();
		this.marketManager.reload();
		this.hellblockGUIManager.reload();
		this.blockManager.reload();
		this.entityManager.reload();
		this.storageManager.reload();
		this.hookManager.reload();
		this.hologramManager.reload();
		this.schematicManager.reload();
		this.translationManager.reload();
		EventUtils.fireAndForget(new LavaFishingReloadEvent(this));
	}

	public static HellblockPlugin getInstance() {
		if (instance == null) {
			throw new IllegalArgumentException("Plugin not initialized");
		}
		return instance;
	}

	public @NonNull String getFormattedCooldown(long seconds) {
		long hours = (seconds - seconds % 3600) / 3600;
		long minutes = (seconds % 3600 - seconds % 3600 % 60) / 60;
		seconds = seconds % 3600 % 60;
		String hourFormat = getTranslationManager().miniMessageTranslation(MessageConstants.FORMAT_HOUR.build().key());
		String minuteFormat = getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_MINUTE.build().key());
		String secondFormat = getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_SECOND.build().key());
		String formattedTime = String.format("%s%s%s", hours > 0 ? hours + hourFormat : "",
				minutes > 0 ? minutes + minuteFormat : "", seconds > 0 ? seconds + secondFormat : "");
		return formattedTime;
	}

	/**
	 * Checks if a specified plugin is enabled on the Bukkit server.
	 *
	 * @param plugin The name of the plugin to check.
	 * @return True if the plugin is enabled, false otherwise.
	 */
	public boolean isHookedPluginEnabled(@NonNull String plugin) {
		return Bukkit.getPluginManager().isPluginEnabled(plugin);
	}

	/**
	 * Outputs a debugging message if the debug mode is enabled.
	 *
	 * @param message The debugging message to be logged.
	 */
	public void debug(Object message) {
		this.debugger.accept(message::toString);
	}

	/**
	 * Outputs a debugging message if the debug mode is enabled.
	 *
	 * @param message The debugging message to be logged.
	 */
	public void debug(Supplier<String> messageSupplier) {
		this.debugger.accept(messageSupplier);
	}

	public @Nullable String identifyClassLoader(ClassLoader classLoader) throws ReflectiveOperationException {
		Class<?> pluginClassLoaderClass = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
		if (pluginClassLoaderClass.isInstance(classLoader)) {
			Method getPluginMethod = pluginClassLoaderClass.getDeclaredMethod("getPlugin");
			getPluginMethod.setAccessible(true);

			JavaPlugin plugin = (JavaPlugin) getPluginMethod.invoke(classLoader);
			return plugin.getName();
		}
		return null;
	}
}
