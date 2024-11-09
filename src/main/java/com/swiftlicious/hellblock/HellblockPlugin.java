package com.swiftlicious.hellblock;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import org.jetbrains.annotations.NotNull;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.swiftlicious.hellblock.api.compatibility.Metrics;
import com.swiftlicious.hellblock.api.compatibility.WorldEditHook;
import com.swiftlicious.hellblock.api.compatibility.WorldGuardHook;
import com.swiftlicious.hellblock.challenges.ChallengeRewardBuilder;
import com.swiftlicious.hellblock.commands.CommandManager;
import com.swiftlicious.hellblock.commands.sub.HellblockAdminCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockAdminCommand.PurgeCounter;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.coop.CoopManager;
import com.swiftlicious.hellblock.creation.addons.IntegrationManager;
import com.swiftlicious.hellblock.creation.block.BlockManager;
import com.swiftlicious.hellblock.creation.entity.EntityManager;
import com.swiftlicious.hellblock.creation.item.ItemManager;
import com.swiftlicious.hellblock.database.StorageManager;
import com.swiftlicious.hellblock.database.dependency.Dependency;
import com.swiftlicious.hellblock.database.dependency.DependencyManager;
import com.swiftlicious.hellblock.database.dependency.ReflectionClassPathAppender;
import com.swiftlicious.hellblock.effects.EffectManager;
import com.swiftlicious.hellblock.events.fishing.LavaFishingReloadEvent;
import com.swiftlicious.hellblock.generation.BiomeHandler;
import com.swiftlicious.hellblock.generation.HellblockHandler;
import com.swiftlicious.hellblock.generation.IslandChoiceConverter;
import com.swiftlicious.hellblock.generation.IslandGenerator;
import com.swiftlicious.hellblock.gui.market.MarketManager;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureManager;
import com.swiftlicious.hellblock.handlers.ChatCatcherManager;
import com.swiftlicious.hellblock.handlers.GlobalSettings;
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
import com.swiftlicious.hellblock.listeners.rain.LavaRain;
import com.swiftlicious.hellblock.loot.LootManager;
import com.swiftlicious.hellblock.placeholders.PlaceholderManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.OfflineUser;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.protection.IslandProtection;
import com.swiftlicious.hellblock.scheduler.Scheduler;
import com.swiftlicious.hellblock.schematic.SchematicManager;
import com.swiftlicious.hellblock.utils.ConfigUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.NumberUtils;
import com.swiftlicious.hellblock.utils.ParseUtils;
import com.swiftlicious.hellblock.utils.ReflectionUtils;
import com.swiftlicious.hellblock.utils.VersionManager;
import com.swiftlicious.hellblock.utils.WeightUtils;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

import lombok.Getter;
import lombok.NonNull;
import xyz.xenondevs.invui.InvUI;

@Getter
public class HellblockPlugin extends JavaPlugin {

	private static HellblockPlugin instance;

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

	protected ConfigUtils configUtils;
	protected WeightUtils weightUtils;
	protected NumberUtils numberUtils;
	protected ParseUtils parseUtils;

	protected ProtocolManager protocolManager;

	protected Scheduler scheduler;
	protected LootManager lootManager;
	protected ActionManager actionManager;
	protected HookManager hookManager;
	protected FishingManager fishingManager;
	protected AdventureManager adventureManager;
	protected GlobalSettings globalSettings;
	protected CommandManager commandManager;
	protected RequirementManager requirementManager;
	protected EffectManager effectManager;
	protected PlaceholderManager placeholderManager;
	protected EntityManager entityManager;
	protected BlockManager blockManager;
	protected ItemManager itemManager;
	protected IntegrationManager integrationManager;
	protected MarketManager marketManager;
	protected VersionManager versionManager;
	protected StorageManager storageManager;
	protected DependencyManager dependencyManager;
	protected ChatCatcherManager chatCatcherManager;

	protected boolean updateAvailable = false;

	@Override
	public void onLoad() {
		if (!CommandAPI.isLoaded())
			CommandAPI.onLoad(new CommandAPIBukkitConfig(this).usePluginNamespace().shouldHookPaperReload(true)
					.silentLogs(true).verboseOutput(true));
		this.dependencyManager = new DependencyManager(this, new ReflectionClassPathAppender(this.getClassLoader()));
		this.dependencyManager.loadDependencies(new ArrayList<>(
				List.of(Dependency.GSON, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.MYSQL_DRIVER,
						Dependency.MARIADB_DRIVER, Dependency.MONGODB_DRIVER_SYNC, Dependency.MONGODB_DRIVER_CORE,
						Dependency.MONGODB_DRIVER_BSON, Dependency.JEDIS, Dependency.BSTATS_BASE,
						Dependency.BSTATS_BUKKIT, Dependency.H2_DRIVER, Dependency.SQLITE_DRIVER, Dependency.HIKARI)));
	}

	@Override
	public void onEnable() {
		if (!isHookedPluginEnabled("ProtocolLib")) {
			LogUtils.severe("ProtocolLib is a dependency to function properly, Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		if (!CommandAPI.isLoaded()) {
			LogUtils.severe("CommandAPI wasn't initialized properly, Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		CommandAPI.onEnable();

		double time = System.currentTimeMillis();

		instance = this;

		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.versionManager = new VersionManager(this);

		// TODO: possible backwards support
		if (!this.versionManager.getSupportedVersions().contains(this.versionManager.getServerVersion())) {
			LogUtils.severe("Hellblock currently only works on v1.20.5+ servers. Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// TODO: add support for spigot
		if (!this.versionManager.isPaper()) {
			LogUtils.severe("Hellblock currently only works on Paper Spigot servers. Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		InvUI.getInstance().setPlugin(this);
		ReflectionUtils.load();
		HBConfig.load();
		HBLocale.load();

		this.scheduler = new Scheduler(this);
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

		this.actionManager = new ActionManager(this);
		this.adventureManager = new AdventureManager();
		this.blockManager = new BlockManager(this);
		this.commandManager = new CommandManager(this);
		this.effectManager = new EffectManager(this);
		this.fishingManager = new FishingManager(this);
		this.itemManager = new ItemManager(this);
		this.lootManager = new LootManager(this);
		this.marketManager = new MarketManager(this);
		this.entityManager = new EntityManager(this);
		this.placeholderManager = new PlaceholderManager(this);
		this.requirementManager = new RequirementManager(this);
		this.storageManager = new StorageManager(this);
		this.playerListener = new PlayerListener(this);
		this.hellblockHandler = new HellblockHandler(this);
		this.integrationManager = new IntegrationManager(this);
		this.hookManager = new HookManager(this);
		this.chatCatcherManager = new ChatCatcherManager(this);
		this.schematicManager = new SchematicManager(this);

		this.netherBrewingHandler = new NetherBrewing(this);
		this.netherToolsHandler = new NetherTools(this);
		this.netherArmorHandler = new NetherArmor(this);
		this.piglinBarterHandler = new PiglinBartering(this);
		this.netherSnowGolemHandler = new NetherSnowGolem(this);

		this.globalSettings = new GlobalSettings();

		this.configUtils = new ConfigUtils();
		this.parseUtils = new ParseUtils();
		this.numberUtils = new NumberUtils();
		this.weightUtils = new WeightUtils();

		reload();

		if (HBConfig.metrics)
			new Metrics(this, 23739);

		if (HBConfig.updateChecker) {
			this.versionManager.checkUpdate().thenAccept(result -> {
				if (!result) {
					LogUtils.info("You are using the latest version.");
				} else {
					LogUtils.info("Update is available: <u>https://github.com/Swiftlicious01/Hellblock/releases<!u>");
					this.updateAvailable = true;
				}
			});
		}

		double time2 = System.currentTimeMillis();
		double time3 = (time2 - time) / 1000;
		LogUtils.info(String.format("Took %s seconds to setup Hellblock!", time3));

		for (OnlineUser onlineUser : getStorageManager().getOnlineUsers()) {
			if (onlineUser == null)
				continue;
			Player player = onlineUser.getPlayer();
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
						instance.getStorageManager()
								.getOfflineUser(onlineUser.getHellblockData().getOwnerUUID(), HBConfig.lockData)
								.thenAccept((owner) -> {
									OfflineUser bannedOwner = owner.orElseThrow();
									getCoopManager().makeHomeLocationSafe(bannedOwner, onlineUser);
								});
					} else {
						getHellblockHandler().teleportToSpawn(player, true);
					}
				}
			}
		}

		int purgeDays = getConfig("config.yml").getInt("hellblock.abandon-after-days", 30);
		if (purgeDays > 0) {
			PurgeCounter purgeCount = HellblockAdminCommand.INSTANCE.new PurgeCounter();
			for (UUID id : getStorageManager().getDataSource().getUniqueUsers(false)) {
				if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
					continue;

				OfflinePlayer player = Bukkit.getOfflinePlayer(id);
				if (player.getLastLogin() == 0)
					continue;
				long millisSinceLastLogin = (System.currentTimeMillis() - player.getLastLogin()) -
				// Account for a timezone difference
						TimeUnit.MILLISECONDS.toHours(19);
				if (millisSinceLastLogin > TimeUnit.DAYS.toMillis(purgeDays)) {
					getStorageManager().getOfflineUser(id, HBConfig.lockData).thenAccept((result) -> {
						OfflineUser offlineUser = result.orElseThrow();
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
										purgeCount.setPurgeCount(purgeCount.getPurgeCount() + 1);
									}
								}
							}
						}
					}).join();
				}
			}
			if (purgeCount.getPurgeCount() > 0)
				LogUtils.info(String.format("A total of %s hellblocks have been set as abandoned.",
						purgeCount.getPurgeCount()));
		}

		getLavaRainHandler().startLavaRainProcess();
		getScheduler().runTaskSyncLater(() -> getHellblockHandler().getHellblockWorld(), null, 5, TimeUnit.SECONDS);
	}

	@Override
	public void onDisable() {
		if (getStorageManager() != null) {
			Iterator<OnlineUser> iterator = getStorageManager().getOnlineUsers().iterator();

			while (iterator.hasNext()) {
				OnlineUser onlineUser = iterator.next();
				if (onlineUser == null)
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

		CommandAPI.onDisable();
		if (getLavaRainHandler() != null)
			getLavaRainHandler().stopLavaRainProcess();
		if (this.blockManager != null)
			((BlockManager) this.blockManager).disable();
		if (this.effectManager != null)
			((EffectManager) this.effectManager).disable();
		if (this.fishingManager != null)
			((FishingManager) this.fishingManager).disable();
		if (this.itemManager != null)
			((ItemManager) this.itemManager).disable();
		if (this.lootManager != null)
			((LootManager) this.lootManager).disable();
		if (this.marketManager != null)
			((MarketManager) this.marketManager).disable();
		if (this.entityManager != null)
			((EntityManager) this.entityManager).disable();
		if (this.requirementManager != null)
			((RequirementManager) this.requirementManager).disable();
		if (this.scheduler != null)
			((Scheduler) this.scheduler).shutdown();
		if (this.integrationManager != null)
			((IntegrationManager) this.integrationManager).disable();
		if (this.storageManager != null)
			((StorageManager) this.storageManager).disable();
		if (this.placeholderManager != null)
			((PlaceholderManager) this.placeholderManager).disable();
		if (this.actionManager != null)
			((ActionManager) this.actionManager).disable();
		if (this.hookManager != null)
			((HookManager) this.hookManager).disable();
		if (this.commandManager != null)
			this.commandManager.unload();
		if (this.chatCatcherManager != null)
			this.chatCatcherManager.disable();
		HandlerList.unregisterAll(this);

		if (instance != null)
			instance = null;
	}

	/**
	 * Reload the plugin
	 */
	public void reload() {
		HBConfig.load();
		getLavaRainHandler().stopLavaRainProcess();
		((Scheduler) this.scheduler).reload();
		((RequirementManager) this.requirementManager).unload();
		((RequirementManager) this.requirementManager).load();
		((ActionManager) this.actionManager).unload();
		((ActionManager) this.actionManager).load();
		((ItemManager) this.itemManager).unload();
		((ItemManager) this.itemManager).load();
		((LootManager) this.lootManager).unload();
		((LootManager) this.lootManager).load();
		((FishingManager) this.fishingManager).unload();
		((FishingManager) this.fishingManager).load();
		((EffectManager) this.effectManager).unload();
		((EffectManager) this.effectManager).load();
		((MarketManager) this.marketManager).unload();
		((MarketManager) this.marketManager).load();
		((BlockManager) this.blockManager).unload();
		((BlockManager) this.blockManager).load();
		((EntityManager) this.entityManager).unload();
		((EntityManager) this.entityManager).load();
		((StorageManager) this.storageManager).reload();
		((HookManager) this.hookManager).unload();
		((HookManager) this.hookManager).load();
		this.commandManager.unload();
		this.commandManager.load();
		this.chatCatcherManager.unload();
		this.chatCatcherManager.load();
		this.schematicManager.reload();

		LavaFishingReloadEvent event = new LavaFishingReloadEvent(this);
		Bukkit.getPluginManager().callEvent(event);
	}

	public static HellblockPlugin getInstance() {
		return instance;
	}

	public @NonNull String getFormattedCooldown(long seconds) {
		long hours = (seconds - seconds % 3600) / 3600;
		long minutes = (seconds % 3600 - seconds % 3600 % 60) / 60;
		seconds = seconds % 3600 % 60;
		String formattedTime = String.format("%s%s%s", hours > 0 ? hours + HBLocale.FORMAT_Hour : "",
				minutes > 0 ? minutes + HBLocale.FORMAT_Minute : "",
				seconds > 0 ? seconds + HBLocale.FORMAT_Second : "");
		return formattedTime;
	}

	/**
	 * Retrieves a YAML configuration from a file within the plugin's data folder.
	 *
	 * @param file The name of the configuration file.
	 * @return A YamlConfiguration object representing the configuration.
	 */
	public @NonNull YamlConfiguration getConfig(@NonNull String file) {
		File config = new File(this.getDataFolder(), file);
		if (!config.exists())
			this.saveResource(file, false);
		return YamlConfiguration.loadConfiguration(config);
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
	public void debug(@NonNull String message) {
		if (!HBConfig.debug)
			return;
		LogUtils.info(message);
	}

	/**
	 * Retrieves the ProtocolManager instance used for managing packets.
	 *
	 * @return The ProtocolManager instance.
	 */
	@NotNull
	public @NonNull ProtocolManager getProtocolManager() {
		return this.protocolManager;
	}

	public void sendPacket(@NonNull Player player, @NonNull PacketContainer packet) {
		this.protocolManager.sendServerPacket(player, packet);
	}

	public void sendPackets(@NonNull Player player, @NonNull PacketContainer... packets) {
		List<PacketContainer> bundle = new ArrayList<>(Arrays.asList(packets));
		PacketContainer bundlePacket = new PacketContainer(PacketType.Play.Server.BUNDLE);
		bundlePacket.getPacketBundles().write(0, bundle);
		sendPacket(player, bundlePacket);
	}
}
