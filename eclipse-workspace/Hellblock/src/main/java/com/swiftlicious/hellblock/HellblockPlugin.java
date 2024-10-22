package com.swiftlicious.hellblock;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

import com.swiftlicious.hellblock.api.compatibility.WorldEditHook;
import com.swiftlicious.hellblock.api.compatibility.WorldGuardHook;
import com.swiftlicious.hellblock.commands.CommandManager;
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
import com.swiftlicious.hellblock.generation.HellblockHandler;
import com.swiftlicious.hellblock.generation.IslandGenerator;
import com.swiftlicious.hellblock.gui.market.MarketManager;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureManager;
import com.swiftlicious.hellblock.handlers.ChatCatcherManager;
import com.swiftlicious.hellblock.handlers.GlobalSettings;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.listeners.GlowstoneTree;
import com.swiftlicious.hellblock.listeners.InfiniteLava;
import com.swiftlicious.hellblock.listeners.NetherArmor;
import com.swiftlicious.hellblock.listeners.NetherBrewing;
import com.swiftlicious.hellblock.listeners.NetherFarming;
import com.swiftlicious.hellblock.listeners.NetherTools;
import com.swiftlicious.hellblock.listeners.NetherrackGenerator;
import com.swiftlicious.hellblock.listeners.PlayerListener;
import com.swiftlicious.hellblock.listeners.fishing.FishingManager;
import com.swiftlicious.hellblock.listeners.fishing.HookManager;
import com.swiftlicious.hellblock.listeners.rain.LavaRain;
import com.swiftlicious.hellblock.loot.LootManager;
import com.swiftlicious.hellblock.placeholders.PlaceholderManager;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.scheduler.Scheduler;
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

import xyz.xenondevs.invui.InvUI;

@Getter
public class HellblockPlugin extends JavaPlugin {

	private static HellblockPlugin instance;

	protected GlowstoneTree glowstoneTree;
	protected LavaRain lavaRain;
	protected InfiniteLava infiniteLava;
	protected NetherBrewing netherBrewing;
	protected NetherFarming netherFarming;
	protected NetherTools netherTools;
	protected NetherArmor netherArmor;
	protected PlayerListener playerListener;
	protected NetherrackGenerator netherrackGenerator;
	protected IslandGenerator islandGenerator;
	protected WorldEditHook worldEditHandler;
	protected WorldGuardHook worldGuardHandler;
	protected HellblockHandler hellblockHandler;
	protected CoopManager coopManager;

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
	private ChatCatcherManager chatCatcherManager;

	protected boolean updateAvailable = false;

	@Override
	public void onLoad() {
		if (!CommandAPI.isLoaded())
			CommandAPI.onLoad(new CommandAPIBukkitConfig(this).usePluginNamespace().shouldHookPaperReload(true)
					.silentLogs(true).verboseOutput(true));
		this.dependencyManager = new DependencyManager(this, new ReflectionClassPathAppender(this.getClassLoader()));
		this.dependencyManager.loadDependencies(new ArrayList<>(
				List.of(Dependency.GSON, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.COMMAND_API,
						Dependency.BOOSTED_YAML, Dependency.RTAG, Dependency.EXP4J, Dependency.MYSQL_DRIVER,
						Dependency.MARIADB_DRIVER, Dependency.MONGODB_DRIVER_SYNC, Dependency.MONGODB_DRIVER_CORE,
						Dependency.MONGODB_DRIVER_BSON, Dependency.JEDIS, Dependency.COMMONS_POOL_2,
						Dependency.H2_DRIVER, Dependency.SQLITE_DRIVER, Dependency.BSTATS_BASE, Dependency.HIKARI,
						Dependency.BSTATS_BUKKIT, Dependency.INV_UI, Dependency.INV_UI_ACCESS, Dependency.INV_UI_NMS)));
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
		if (!this.versionManager.getServerVersion().equals("1.21.1")) {
			LogUtils.severe("Hellblock currently only works on v1.21.1 servers. Disabling plugin...");
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

		this.netherrackGenerator = new NetherrackGenerator(this);
		this.lavaRain = new LavaRain(this);
		this.netherFarming = new NetherFarming(this);
		this.islandGenerator = new IslandGenerator(this);
		this.glowstoneTree = new GlowstoneTree(this);
		this.infiniteLava = new InfiniteLava(this);
		this.playerListener = new PlayerListener(this);
		this.worldEditHandler = new WorldEditHook();
		this.worldGuardHandler = new WorldGuardHook(this);
		this.hellblockHandler = new HellblockHandler(this);
		this.coopManager = new CoopManager(this);

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
		this.scheduler = new Scheduler(this);
		this.storageManager = new StorageManager(this);
		this.integrationManager = new IntegrationManager(this);
		this.hookManager = new HookManager(this);
		this.chatCatcherManager = new ChatCatcherManager(this);

		this.netherBrewing = new NetherBrewing(this);
		this.netherTools = new NetherTools(this);
		this.netherArmor = new NetherArmor(this);

		this.globalSettings = new GlobalSettings();

		this.configUtils = new ConfigUtils();
		this.parseUtils = new ParseUtils();
		this.numberUtils = new NumberUtils();
		this.weightUtils = new WeightUtils();

		reload();

		if (HBConfig.metrics)
			new Metrics(this, 42152);

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
		LogUtils.info("Took " + time3 + " seconds to setup Hellblock!");

		getHellblockHandler().getActivePlayers().clear();
		Player[] var4;
		int var3 = (var4 = Bukkit.getOnlinePlayers().toArray(new Player[0])).length;

		for (int var2 = 0; var2 < var3; ++var2) {
			Player player = var4[var2];
			UUID id = player.getUniqueId();
			HellblockPlayer pi = new HellblockPlayer(id);
			getHellblockHandler().addActivePlayer(player, pi);
			getNetherFarming().trackNetherFarms(pi);
		}

		getLavaRain().startLavaRainProcess();
		getHellblockHandler().getHellblockWorld();
	}

	@Override
	public void onDisable() {
		CommandAPI.onDisable();
		if (getLavaRain() != null)
			getLavaRain().stopLavaRainProcess();
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

		if (getHellblockHandler() != null && getHellblockHandler().getActivePlayers() != null) {
			Iterator<UUID> var2 = getHellblockHandler().getActivePlayers().keySet().iterator();

			while (var2.hasNext()) {
				UUID id = (UUID) var2.next();
				HellblockPlayer pi = (HellblockPlayer) getHellblockHandler().getActivePlayers().get(id);
				if (pi == null)
					continue;
				pi.saveHellblockPlayer();
			}
		}

		if (instance != null)
			instance = null;
	}

	/**
	 * Reload the plugin
	 */
	public void reload() {
		HBConfig.load();
		getLavaRain().stopLavaRainProcess();
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

		LavaFishingReloadEvent event = new LavaFishingReloadEvent(this);
		Bukkit.getPluginManager().callEvent(event);
	}

	public static HellblockPlugin getInstance() {
		return instance;
	}

	/**
	 * Retrieves a YAML configuration from a file within the plugin's data folder.
	 *
	 * @param file The name of the configuration file.
	 * @return A YamlConfiguration object representing the configuration.
	 */
	public YamlConfiguration getConfig(String file) {
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
	public boolean isHookedPluginEnabled(String plugin) {
		return Bukkit.getPluginManager().isPluginEnabled(plugin);
	}

	/**
	 * Outputs a debugging message if the debug mode is enabled.
	 *
	 * @param message The debugging message to be logged.
	 */
	public void debug(String message) {
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
	public ProtocolManager getProtocolManager() {
		return this.protocolManager;
	}

	public void sendPacket(Player player, PacketContainer packet) {
		this.protocolManager.sendServerPacket(player, packet);
	}

	public void sendPackets(Player player, PacketContainer... packets) {
		List<PacketContainer> bundle = new ArrayList<>(Arrays.asList(packets));
		PacketContainer bundlePacket = new PacketContainer(PacketType.Play.Server.BUNDLE);
		bundlePacket.getPacketBundles().write(0, bundle);
		sendPacket(player, bundlePacket);
	}
}
