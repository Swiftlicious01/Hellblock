package com.swiftlicious.hellblock;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Metrics;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.api.TpsMonitor;
import com.swiftlicious.hellblock.challenges.ChallengeManager;
import com.swiftlicious.hellblock.challenges.requirement.FishRequirement;
import com.swiftlicious.hellblock.commands.BukkitCommandManager;
import com.swiftlicious.hellblock.commands.CloudDependencyHelper;
import com.swiftlicious.hellblock.commands.sub.DebugWorldsCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockIslandBioCommand;
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
import com.swiftlicious.hellblock.enchantment.MagmaWalker;
import com.swiftlicious.hellblock.events.HellblockReloadEvent;
import com.swiftlicious.hellblock.generation.BiomeHandler;
import com.swiftlicious.hellblock.generation.BorderHandler;
import com.swiftlicious.hellblock.generation.HellblockHandler;
import com.swiftlicious.hellblock.generation.IslandChoiceConverter;
import com.swiftlicious.hellblock.generation.IslandGenerator;
import com.swiftlicious.hellblock.generation.IslandPlacementDetector;
import com.swiftlicious.hellblock.gui.biome.BiomeGUIManager;
import com.swiftlicious.hellblock.gui.challenges.ChallengesGUIManager;
import com.swiftlicious.hellblock.gui.choice.IslandChoiceGUIManager;
import com.swiftlicious.hellblock.gui.display.DisplaySettingsGUIManager;
import com.swiftlicious.hellblock.gui.flags.FlagsGUIManager;
import com.swiftlicious.hellblock.gui.hellblock.HellblockGUIManager;
import com.swiftlicious.hellblock.gui.invite.InviteGUIManager;
import com.swiftlicious.hellblock.gui.market.MarketManager;
import com.swiftlicious.hellblock.gui.party.PartyGUIManager;
import com.swiftlicious.hellblock.gui.reset.ResetConfirmGUIManager;
import com.swiftlicious.hellblock.gui.schematic.SchematicGUIManager;
import com.swiftlicious.hellblock.gui.upgrade.UpgradeGUIManager;
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager;
import com.swiftlicious.hellblock.handlers.AbstractActionManager;
import com.swiftlicious.hellblock.handlers.AbstractRequirementManager;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.BlockActionManager;
import com.swiftlicious.hellblock.handlers.BlockRequirementManager;
import com.swiftlicious.hellblock.handlers.CoolDownManager;
import com.swiftlicious.hellblock.handlers.EventManager;
import com.swiftlicious.hellblock.handlers.HologramManager;
import com.swiftlicious.hellblock.handlers.PlayerActionManager;
import com.swiftlicious.hellblock.handlers.PlayerRequirementManager;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.handlers.VisitManager;
import com.swiftlicious.hellblock.listeners.ArmorHandler;
import com.swiftlicious.hellblock.listeners.BrewingHandler;
import com.swiftlicious.hellblock.listeners.FarmingHandler;
import com.swiftlicious.hellblock.listeners.GlowTreeHandler;
import com.swiftlicious.hellblock.listeners.GolemHandler;
import com.swiftlicious.hellblock.listeners.HopperHandler;
import com.swiftlicious.hellblock.listeners.InfiniteLavaHandler;
import com.swiftlicious.hellblock.listeners.LevelHandler;
import com.swiftlicious.hellblock.listeners.MinionHandler;
import com.swiftlicious.hellblock.listeners.MobSpawnHandler;
import com.swiftlicious.hellblock.listeners.NetherGeneratorHandler;
import com.swiftlicious.hellblock.listeners.PiglinBarterHandler;
import com.swiftlicious.hellblock.listeners.PlayerListener;
import com.swiftlicious.hellblock.listeners.ToolsHandler;
import com.swiftlicious.hellblock.listeners.WitherHandler;
import com.swiftlicious.hellblock.listeners.WraithHandler;
import com.swiftlicious.hellblock.listeners.fishing.FishingManager;
import com.swiftlicious.hellblock.listeners.fishing.HookManager;
import com.swiftlicious.hellblock.listeners.fishing.StatisticsManager;
import com.swiftlicious.hellblock.listeners.rain.RainHandler;
import com.swiftlicious.hellblock.logging.JavaPluginLogger;
import com.swiftlicious.hellblock.logging.PluginLogger;
import com.swiftlicious.hellblock.loot.LootManager;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.placeholders.PlaceholderManager;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.mailbox.MailboxManager;
import com.swiftlicious.hellblock.protection.ProtectionManager;
import com.swiftlicious.hellblock.scheduler.BukkitSchedulerAdapter;
import com.swiftlicious.hellblock.schematic.IslandBackupManager;
import com.swiftlicious.hellblock.schematic.SchematicManager;
import com.swiftlicious.hellblock.sender.BukkitSenderFactory;
import com.swiftlicious.hellblock.sender.SenderFactory;
import com.swiftlicious.hellblock.upgrades.UpgradeManager;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.WorldManager;

import io.papermc.lib.PaperLib;

public class HellblockPlugin extends JavaPlugin {

	protected static HellblockPlugin instance;

	protected GlowTreeHandler glowstoneTreeHandler;
	protected RainHandler lavaRainHandler;
	protected InfiniteLavaHandler infiniteLavaHandler;
	protected BrewingHandler netherBrewingHandler;
	protected FarmingHandler farmingManager;
	protected ToolsHandler netherToolsHandler;
	protected ArmorHandler netherArmorHandler;
	protected PiglinBarterHandler piglinBarterHandler;
	protected HopperHandler hopperHandler;
	protected GolemHandler hellGolemHandler;
	protected WraithHandler wraithHandler;
	protected WitherHandler witherHandler;
	protected MinionHandler minionHandler;
	protected MobSpawnHandler mobSpawnHandler;
	protected PlayerListener playerListener;
	protected NetherGeneratorHandler netherrackGeneratorHandler;
	protected IslandGenerator islandGenerator;
	protected UpgradeManager upgradeManager;
	protected ProtectionManager protectionManager;
	protected HellblockHandler hellblockHandler;
	protected BiomeHandler biomeHandler;
	protected IslandChoiceConverter islandChoiceConverter;
	protected CoopManager coopManager;
	protected LevelHandler islandLevelManager;
	protected SchematicManager schematicManager;
	protected WorldManager worldManager;
	protected IslandPlacementDetector placementDetector;
	protected IslandBackupManager backupManager;
	protected ChallengeManager challengeManager;
	protected BorderHandler borderHandler;
	protected MailboxManager mailboxManager;
	protected ConfigManager configManager;

	protected PluginLogger pluginLogger;
	protected SenderFactory<HellblockPlugin, CommandSender> senderFactory;
	protected BukkitSchedulerAdapter scheduler;

	protected VisitManager visitManager;
	protected LootManager lootManager;
	protected HologramManager hologramManager;
	protected HookManager hookManager;
	protected FishingManager fishingManager;
	protected EventManager eventManager;
	protected BukkitCommandManager commandManager;
	protected CoolDownManager cooldownManager;
	protected EffectManager effectManager;
	protected PlaceholderManager placeholderManager;
	protected EntityManager entityManager;
	protected BlockManager blockManager;
	protected ItemManager itemManager;
	protected IntegrationManager integrationManager;
	protected StatisticsManager statisticsManager;
	protected MarketManager marketManager;
	protected HellblockGUIManager hellblockGUIManager;
	protected BiomeGUIManager biomeGUIManager;
	protected FlagsGUIManager flagsGUIManager;
	protected UpgradeGUIManager upgradeGUIManager;
	protected PartyGUIManager partyGUIManager;
	protected InviteGUIManager inviteGUIManager;
	protected VisitGUIManager visitGUIManager;
	protected ChallengesGUIManager challengesGUIManager;
	protected DisplaySettingsGUIManager displaySettingsGUIManager;
	protected IslandChoiceGUIManager islandChoiceGUIManager;
	protected SchematicGUIManager schematicGUIManager;
	protected ResetConfirmGUIManager resetConfirmGUIManager;
	protected StorageManager storageManager;
	protected DependencyManager dependencyManager;
	protected TranslationManager translationManager;
	protected CloudDependencyHelper cloudDependencyHelper;

	protected MagmaWalker magmaWalker;

	protected TpsMonitor tpsMonitor;

	protected final Map<Class<?>, ActionManager<?>> actionManagers = new HashMap<>();
	protected final Map<Class<?>, RequirementManager<?>> requirementManagers = new HashMap<>();

	protected boolean updateAvailable = false;
	protected boolean isReloading = false;
	protected Consumer<Supplier<String>> debugger = (supplier -> {
	});

	/**
	 * Preloads the plugins and all of its dependencies.
	 */
	@Override
	public void onLoad() {
		instance = this;
		this.pluginLogger = new JavaPluginLogger(this.getLogger());
		VersionHelper.init(Bukkit.getBukkitVersion().split("-")[0]);
		this.scheduler = new BukkitSchedulerAdapter(this);
		this.dependencyManager = new DependencyManager(this, new ReflectionClassPathAppender(this.getClassLoader()));
		final Set<Dependency> dependencies = new HashSet<>(Arrays.asList(Dependency.values()));
		dependencies.removeAll(RelocationHandler.DEPENDENCIES);
		getPluginLogger().info("Preloading %s dependencies...".formatted(dependencies.size()));
		this.dependencyManager.loadDependencies(dependencies);
		this.cloudDependencyHelper = new CloudDependencyHelper(this);
	}

	/**
	 * Enables the plugin and initializes its components.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		final double startTime = System.currentTimeMillis();

		// TODO: test support from 1.17 to 1.21.10+
		if (!VersionHelper.getSupportedVersions().contains(VersionHelper.getServerVersion())) {
			getPluginLogger().severe(
					"Hellblock only supports legacy versions down to 1.17. Please update your server to be able to properly use this plugin.");
			getPluginLogger().severe("Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// TODO: test spigot
		if (!VersionHelper.isPaper()) {
			getPluginLogger().severe("Hellblock is more suited towards Paper, but will still work with Spigot.");
			PaperLib.suggestPaper(this);
		}

		if (getScheduler() == null) {
			getPluginLogger().severe("Hellblock was unable to setup correctly.");
			getPluginLogger().severe("Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		tpsMonitor = new TpsMonitor();
		// Schedule a repeating task every tick to record TPS samples
		getScheduler().sync().runRepeating(() -> this.tpsMonitor.onTick(), 1L, 1L,
				LocationUtils.getAnyLocationInstance());

		this.translationManager = new TranslationManager(this);
		registerManager(Player.class, new PlayerRequirementManager(this), new PlayerActionManager(this));
		registerManager(BlockData.class, new BlockRequirementManager(this), new BlockActionManager(this));
		this.placeholderManager = new PlaceholderManager(this);
		this.lootManager = new LootManager(this);
		this.effectManager = new EffectManager(this);
		this.eventManager = new EventManager(this);
		this.itemManager = new ItemManager(this);
		this.entityManager = new EntityManager(this);
		this.blockManager = new BlockManager(this);
		this.hookManager = new HookManager(this);
		this.configManager = new ConfigManager(this);
		this.itemManager.init();
		this.entityManager.init();
		this.blockManager.init();
		// after ConfigManager
		this.debugger = getConfigManager().debug() ? (s) -> pluginLogger.info("[DEBUG] " + s.get()) : (s) -> {
		};

		this.storageManager = new StorageManager(this);
		this.storageManager.init();
		this.storageManager.reload();
		this.magmaWalker = new MagmaWalker(this);
		this.visitManager = new VisitManager(this);
		this.mailboxManager = new MailboxManager(this);
		this.netherrackGeneratorHandler = new NetherGeneratorHandler(this);
		this.lavaRainHandler = new RainHandler(this);
		this.farmingManager = new FarmingHandler(this);
		this.islandGenerator = new IslandGenerator(this);
		this.netherBrewingHandler = new BrewingHandler(this);
		this.glowstoneTreeHandler = new GlowTreeHandler(this);
		this.infiniteLavaHandler = new InfiniteLavaHandler(this);
		this.witherHandler = new WitherHandler(this);
		this.wraithHandler = new WraithHandler(this);
		this.minionHandler = new MinionHandler(this);
		this.biomeHandler = new BiomeHandler(this);
		this.borderHandler = new BorderHandler(this);
		this.mobSpawnHandler = new MobSpawnHandler(this);
		this.islandChoiceConverter = new IslandChoiceConverter(this);
		this.placementDetector = new IslandPlacementDetector(this);
		this.coopManager = new CoopManager(this);
		this.hopperHandler = new HopperHandler(this);
		this.islandLevelManager = new LevelHandler(this);
		this.protectionManager = new ProtectionManager(this);
		this.challengeManager = new ChallengeManager(this);
		this.senderFactory = new BukkitSenderFactory(this);
		this.fishingManager = new FishingManager(this);
		this.backupManager = new IslandBackupManager(this, getDataFolder());
		this.hellblockGUIManager = new HellblockGUIManager(this);
		this.biomeGUIManager = new BiomeGUIManager(this);
		this.flagsGUIManager = new FlagsGUIManager(this);
		this.upgradeGUIManager = new UpgradeGUIManager(this);
		this.partyGUIManager = new PartyGUIManager(this);
		this.visitGUIManager = new VisitGUIManager(this);
		this.displaySettingsGUIManager = new DisplaySettingsGUIManager(this);
		this.challengesGUIManager = new ChallengesGUIManager(this);
		this.islandChoiceGUIManager = new IslandChoiceGUIManager(this);
		this.schematicGUIManager = new SchematicGUIManager(this);
		this.inviteGUIManager = new InviteGUIManager(this);
		this.resetConfirmGUIManager = new ResetConfirmGUIManager(this);
		this.cooldownManager = new CoolDownManager(this);
		this.cooldownManager.init();
		this.playerListener = new PlayerListener(this);
		this.integrationManager = new IntegrationManager(this);
		this.integrationManager.init();
		this.upgradeManager = new UpgradeManager(this);
		this.hellblockHandler = new HellblockHandler(this);
		this.marketManager = new MarketManager(this);
		this.statisticsManager = new StatisticsManager(this);
		this.hologramManager = new HologramManager(this);
		this.schematicManager = new SchematicManager(this);
		this.worldManager = new WorldManager(this);
		this.commandManager = new BukkitCommandManager(this);
		// Run after plugin fully loads, ensures main loader context
		getScheduler().executeSync(() -> this.commandManager.registerDefaultFeatures());
		this.netherToolsHandler = new ToolsHandler(this);
		this.netherArmorHandler = new ArmorHandler(this);
		this.piglinBarterHandler = new PiglinBarterHandler(this);
		this.hellGolemHandler = new GolemHandler(this);

		this.placementDetector.initialize();

		DebugWorldsCommand.startCachingTileEntities(5);

		if (getConfigManager().metrics()) {
			new Metrics(this, 23739);
		}

		if (getConfigManager().checkUpdate()) {
			VersionHelper.checkUpdate.apply(this).thenAccept(result -> {
				final String link = "https://github.com/Swiftlicious01/Hellblock/releases";
				if (!result) {
					getPluginLogger().info("You are using the latest version.");
				} else {
					getPluginLogger().info("Update is available: %s".formatted(link));
					this.updateAvailable = true;
				}
			});
		}

		final double finishedTime = System.currentTimeMillis();
		final double finalTime = (finishedTime - startTime) / 1000;
		getPluginLogger().info("Took %s seconds to setup Hellblock!".formatted(finalTime));

		for (UserData onlineUser : getStorageManager().getOnlineUsers()) {
			final Player player = onlineUser.getPlayer();
			if (player == null) {
				continue;
			}
			final UUID playerId = player.getUniqueId();

			// Synchronous setup
			onlineUser.startSpawningAnimals();
			onlineUser.startSpawningFortressMobs();
			getFarmingManager().updateCrops(player.getWorld(), player);
			getIslandLevelManager().loadCache(playerId);
			getNetherrackGeneratorHandler().loadPistons(playerId);

			// Handle visiting-island bans asynchronously
			getHellblockHandler().handleVisitingIsland(player, onlineUser);
		}

		getHellblockHandler().purgeInactiveHellblocks();

		// Case 1: Owner online
		// Case 2: Coop member online (async lookup)
		// only need one online coop member
		// 15 minutes = 15 * 60 seconds * 20 ticks
		// divide across # of owners
		getScheduler().asyncRepeating(() -> getCoopManager().getAllIslandOwners().thenAccept(ownerIds -> {
			if (ownerIds.isEmpty()) {
				return;
			}
			final int total = ownerIds.size();
			final long intervalSeconds = (15 * 60) / total;
			final AtomicInteger index = new AtomicInteger(0);
			for (UUID ownerId : ownerIds) {
				final long delay = intervalSeconds * index.getAndIncrement();
				if (Bukkit.getPlayer(ownerId) != null) {
					getScheduler().asyncLater(() -> {
						instance.getPluginLogger()
								.info("Scheduled snapshot for island owner " + ownerId + " (owner online)");
						getIslandBackupManager().maybeSnapshot(ownerId);
					}, delay, TimeUnit.SECONDS);
					continue;
				}
				getCoopManager().getAllCoopMembers(ownerId).thenAccept(members -> {
					for (UUID memberId : members) {
						if (Bukkit.getPlayer(memberId) != null) {
							getScheduler().asyncLater(() -> {
								instance.getPluginLogger().info("Scheduled snapshot for island owner " + ownerId
										+ " (coop member online: " + memberId + ")");
								getIslandBackupManager().maybeSnapshot(ownerId);
							}, delay, TimeUnit.SECONDS);
							break;
						}
					}
				});
			}
		}), 15, 15, TimeUnit.MINUTES);
		// every 15 minutes

		// Use scheduler to allow other plugins to load before reloading to prevent
		// conflicts
		if (VersionHelper.isFolia()) {
			Bukkit.getGlobalRegionScheduler().run(this, (scheduledTask) -> this.reload());
		} else {
			Bukkit.getScheduler().runTask(this, this::reload);
		}

	}

	/**
	 * Disables the plugin and performs necessary cleanup operations.
	 */
	@Override
	public void onDisable() {
		if (getStorageManager() != null) {
			final Iterator<UserData> iterator = getStorageManager().getOnlineUsers().iterator();

			while (iterator.hasNext()) {
				final UserData onlineUser = iterator.next();
				if (!onlineUser.isOnline()) {
					continue;
				}
				onlineUser.stopSpawningAnimals();
				onlineUser.stopSpawningFortressMobs();
				getIslandLevelManager().saveCache(onlineUser.getUUID());
				getNetherrackGeneratorHandler().savePistons(onlineUser.getUUID());
				final Player player = onlineUser.getPlayer();
				if ((onlineUser.hasGlowstoneToolEffect() || onlineUser.hasGlowstoneArmorEffect())
						&& player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					onlineUser.isHoldingGlowstoneTool(false);
					onlineUser.isWearingGlowstoneArmor(false);
				}
			}
		}

		Bukkit.getWorlds().forEach(world -> {
			if (VersionHelper.isPaper()) {
				try {
					Method removeTickets = World.class.getMethod("removePluginChunkTickets", Plugin.class);
					removeTickets.invoke(world, this);
					debug(() -> "Cleared plugin chunk tickets for world: " + world.getName());
				} catch (Exception e) {
					// Optional: log failure or ignore silently
				}
			}
		});

		DebugWorldsCommand.stopCachingTileEntities();

		if (this.lavaRainHandler != null) {
			this.lavaRainHandler.disable();
		}
		if (this.playerListener != null) {
			this.playerListener.disable();
		}
		if (this.blockManager != null) {
			this.blockManager.disable();
		}
		if (this.effectManager != null) {
			this.effectManager.disable();
		}
		if (this.fishingManager != null) {
			this.fishingManager.disable();
		}
		if (this.itemManager != null) {
			this.itemManager.disable();
		}
		if (this.lootManager != null) {
			this.lootManager.disable();
		}
		if (this.marketManager != null) {
			this.marketManager.disable();
		}
		if (this.hellblockGUIManager != null) {
			this.hellblockGUIManager.disable();
		}
		if (this.visitGUIManager != null) {
			this.visitGUIManager.disable();
		}
		if (this.biomeGUIManager != null) {
			this.biomeGUIManager.disable();
		}
		if (this.upgradeGUIManager != null) {
			this.upgradeGUIManager.disable();
		}
		if (this.flagsGUIManager != null) {
			this.flagsGUIManager.disable();
		}
		if (this.partyGUIManager != null) {
			this.partyGUIManager.disable();
		}
		if (this.inviteGUIManager != null) {
			this.inviteGUIManager.disable();
		}
		if (this.challengesGUIManager != null) {
			this.challengesGUIManager.disable();
		}
		if (this.islandChoiceGUIManager != null) {
			this.islandChoiceGUIManager.disable();
		}
		if (this.schematicGUIManager != null) {
			this.schematicGUIManager.disable();
		}
		if (this.resetConfirmGUIManager != null) {
			this.resetConfirmGUIManager.disable();
		}
		if (this.displaySettingsGUIManager != null) {
			this.displaySettingsGUIManager.disable();
		}
		if (this.entityManager != null) {
			this.entityManager.disable();
		}
		if (this.integrationManager != null) {
			this.integrationManager.disable();
		}
		if (this.storageManager != null) {
			this.storageManager.disable();
		}
		if (this.worldManager != null) {
			this.worldManager.disable();
		}
		if (this.placeholderManager != null) {
			this.placeholderManager.disable();
		}
		if (this.hookManager != null) {
			this.hookManager.disable();
		}
		if (this.cooldownManager != null) {
			this.cooldownManager.disable();
		}
		if (this.commandManager != null) {
			this.commandManager.unregisterFeatures();
		}
		if (this.scheduler != null) {
			this.scheduler.shutdownScheduler();
			this.scheduler.shutdownExecutor();
		}
		HandlerList.unregisterAll(this);

		if (instance != null) {
			instance = null;
		}
	}

	/**
	 * Reloads the plugin's configuration and components.
	 */
	public void reload() {
		this.isReloading = true;
		MechanicType.reset();
		this.itemManager.unload();
		this.eventManager.unload();
		this.entityManager.unload();
		this.lootManager.unload();
		this.blockManager.unload();
		this.effectManager.unload();
		this.hookManager.unload();
		this.actionManagers.values().forEach(Reloadable::reload);
		this.requirementManagers.values().forEach(Reloadable::reload);
		// before ConfigManager
		this.placeholderManager.reload();
		this.configManager.reload();
		// after ConfigManager
		this.debugger = getConfigManager().debug() ? (s) -> pluginLogger.info("[DEBUG] " + s.get()) : (s) -> {
		};
		HellblockIslandBioCommand.precompileBannedWordPatterns(configManager.bannedWords());
		this.lavaRainHandler.reload();
		this.cooldownManager.reload();
		this.translationManager.reload();
		this.visitManager.reload();
		this.hellblockGUIManager.reload();
		this.biomeGUIManager.reload();
		this.upgradeGUIManager.reload();
		this.flagsGUIManager.reload();
		this.partyGUIManager.reload();
		this.inviteGUIManager.reload();
		this.challengesGUIManager.reload();
		this.islandChoiceGUIManager.reload();
		this.displaySettingsGUIManager.reload();
		this.schematicGUIManager.reload();
		this.visitGUIManager.reload();
		this.resetConfirmGUIManager.reload();
		this.marketManager.reload();
		this.statisticsManager.reload();
		this.storageManager.reload();
		this.fishingManager.reload();
		this.hologramManager.reload();
		this.itemManager.load();
		this.eventManager.load();
		this.entityManager.load();
		this.lootManager.load();
		this.blockManager.load();
		this.effectManager.load();
		this.hookManager.load();
		this.magmaWalker.reload();
		this.mobSpawnHandler.reload();
		this.playerListener.reload();
		this.islandGenerator.reload();
		this.hellGolemHandler.reload();
		this.witherHandler.reload();
		this.minionHandler.reload();
		this.coopManager.reload();
		this.borderHandler.reload();
		this.glowstoneTreeHandler.reload();
		this.infiniteLavaHandler.reload();
		this.piglinBarterHandler.reload();
		this.netherrackGeneratorHandler.reload();
		this.wraithHandler.reload();
		this.placementDetector.reload();
		this.islandLevelManager.reload();
		this.challengeManager.reload();
		this.netherToolsHandler.reload();
		this.netherArmorHandler.reload();
		this.netherBrewingHandler.reload();
		this.hopperHandler.reload();
		this.farmingManager.reload();
		this.upgradeManager.reload();
		this.schematicManager.reload();
		this.worldManager.reload();
		this.protectionManager.reload();
		FishRequirement.reloadContents(this);
		EventUtils.fireAndForget(new HellblockReloadEvent(this));
		this.isReloading = false;
	}

	/**
	 * Gets the instance of the hellblock plugin for anything related to the plugin.
	 *
	 * @return the hellblock plugin instance
	 */
	@Nullable
	public static HellblockPlugin getInstance() {
		if (instance == null) {
			throw new IllegalArgumentException("Plugin not initialized");
		}
		return instance;
	}

	/**
	 * Gets the config manager for handling config-related functionality.
	 *
	 * @return the config manager
	 */
	@NotNull
	public ConfigManager getConfigManager() {
		return this.configManager;
	}

	/**
	 * Gets the translation manager for handling translation-related functionality.
	 *
	 * @return the translation manager
	 */
	@NotNull
	public TranslationManager getTranslationManager() {
		return this.translationManager;
	}

	/**
	 * Gets the storage manager for handling storage-related functionality.
	 *
	 * @return the storage manager
	 */
	@NotNull
	public StorageManager getStorageManager() {
		return this.storageManager;
	}

	/**
	 * Gets the schematic manager for handling schematic-related functionality.
	 *
	 * @return the schematic manager
	 */
	@NotNull
	public SchematicManager getSchematicManager() {
		return this.schematicManager;
	}

	/**
	 * Gets the world manager for handling world-related functionality.
	 *
	 * @return the world manager
	 */
	@NotNull
	public WorldManager getWorldManager() {
		return this.worldManager;
	}

	/**
	 * Gets the upgrade manager for handling upgrade-related functionality.
	 *
	 * @return the upgrade manager
	 */
	@NotNull
	public UpgradeManager getUpgradeManager() {
		return this.upgradeManager;
	}

	/**
	 * Gets the dependency manager for handling dependency-related functionality.
	 *
	 * @return the dependency manager
	 */
	@NotNull
	public DependencyManager getDependencyManager() {
		return this.dependencyManager;
	}

	/**
	 * Gets the placement detector for handling spacing between islands.
	 * 
	 * @return the placement detector
	 */
	@NotNull
	public IslandPlacementDetector getPlacementDetector() {
		return this.placementDetector;
	}

	/**
	 * Gets the tps monitor for handling tps-related functionality.
	 *
	 * @return the tps monitor
	 */
	@NotNull
	public TpsMonitor getTpsMonitor() {
		return this.tpsMonitor;
	}

	@NotNull
	public MailboxManager getMailboxManager() {
		return this.mailboxManager;
	}

	@NotNull
	public CloudDependencyHelper getCloudDependencyHelper() {
		return this.cloudDependencyHelper;
	}

	@NotNull
	public MobSpawnHandler getMobSpawnHandler() {
		return this.mobSpawnHandler;
	}

	/**
	 * Retrieves an ActionManager for a specific type.
	 *
	 * @param type the class type of the action
	 * @return the {@link ActionManager} for the specified type
	 * @throws IllegalArgumentException if the type is null
	 */
	@SuppressWarnings("unchecked")
	public <T> ActionManager<T> getActionManager(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("Type cannot be null");
		}
		return (ActionManager<T>) actionManagers.get(type);
	}

	/**
	 * Retrieves a RequirementManager for a specific type.
	 *
	 * @param type the class type of the requirement
	 * @return the {@link RequirementManager} for the specified type
	 * @throws IllegalArgumentException if the type is null
	 */
	@SuppressWarnings("unchecked")
	public <T> RequirementManager<T> getRequirementManager(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("Type cannot be null");
		}
		return (RequirementManager<T>) requirementManagers.get(type);
	}

	private <T> void registerManager(Class<T> type, AbstractRequirementManager<T> requirementManager,
			AbstractActionManager<T> actionManager) {
		this.requirementManagers.put(type, requirementManager);
		this.actionManagers.put(type, actionManager);
		requirementManager.registerBuiltInRequirements();
		actionManager.registerBuiltInActions();
	}

	/**
	 * Gets the fishing manager for handling fishing-related functionality.
	 *
	 * @return the fishing manager
	 */
	@NotNull
	public FishingManager getFishingManager() {
		return this.fishingManager;
	}

	/**
	 * Gets the hook manager for handling fishing hook-related functionality.
	 *
	 * @return the hook manager
	 */
	@NotNull
	public HookManager getHookManager() {
		return this.hookManager;
	}

	/**
	 * Gets the loot manager for handling loot-related functionality.
	 *
	 * @return the loot manager
	 */
	@NotNull
	public LootManager getLootManager() {
		return this.lootManager;
	}

	/**
	 * Gets the backup manager for handling backup-related functionality.
	 *
	 * @return the backup manager
	 */
	@NotNull
	public IslandBackupManager getIslandBackupManager() {
		return this.backupManager;
	}

	/**
	 * Gets the effect manager for handling effect-related functionality.
	 *
	 * @return the effect manager
	 */
	@NotNull
	public EffectManager getEffectManager() {
		return this.effectManager;
	}

	/**
	 * Gets the item manager for handling item-related functionality.
	 *
	 * @return the item manager
	 */
	@NotNull
	public ItemManager getItemManager() {
		return this.itemManager;
	}

	/**
	 * Gets the entity manager for handling entity-related functionality.
	 *
	 * @return the entity manager
	 */
	@NotNull
	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	/**
	 * Gets the block manager for handling block-related functionality.
	 *
	 * @return the block manager
	 */
	@NotNull
	public BlockManager getBlockManager() {
		return this.blockManager;
	}

	/**
	 * Gets the command manager for handling command-related functionality.
	 *
	 * @return the command manager
	 */
	@NotNull
	public BukkitCommandManager getCommandManager() {
		return this.commandManager;
	}

	/**
	 * Gets the manager for handling placeholder-related functionality.
	 *
	 * @return the placeholder manager
	 */
	@NotNull
	public PlaceholderManager getPlaceholderManager() {
		return this.placeholderManager;
	}

	/**
	 * Gets the manager for handling integration-related functionality.
	 *
	 * @return the integration manager
	 */
	@NotNull
	public IntegrationManager getIntegrationManager() {
		return this.integrationManager;
	}

	/**
	 * Gets the manager for handling market-related functionality.
	 *
	 * @return the market manager
	 */
	@NotNull
	public MarketManager getMarketManager() {
		return this.marketManager;
	}

	/**
	 * Gets the manager for handling statistics-related functionality.
	 *
	 * @return the statistics manager
	 */
	@NotNull
	public StatisticsManager getStatisticsManager() {
		return this.statisticsManager;
	}

	/**
	 * Gets the manager for handling event-related functionality.
	 *
	 * @return the event manager
	 */
	@NotNull
	public EventManager getEventManager() {
		return this.eventManager;
	}

	/**
	 * Gets the manager for handling hologram-related functionality.
	 *
	 * @return the hologram manager
	 */
	@NotNull
	public HologramManager getHologramManager() {
		return this.hologramManager;
	}

	/**
	 * Gets the manager for handling cooldown-related functionality.
	 *
	 * @return the cooldown manager
	 */
	@NotNull
	public CoolDownManager getCooldownManager() {
		return this.cooldownManager;
	}

	/**
	 * Gets the manager for handling visit-related functionality.
	 *
	 * @return the visit manager
	 */
	@NotNull
	public VisitManager getVisitManager() {
		return this.visitManager;
	}

	/**
	 * Gets the manager for handling Hellblock-related GUI functionality.
	 *
	 * @return the Hellblock GUI manager
	 */
	@NotNull
	public HellblockGUIManager getHellblockGUIManager() {
		return this.hellblockGUIManager;
	}

	/**
	 * Gets the manager for handling biome-related GUI functionality.
	 *
	 * @return the biome GUI manager
	 */
	@NotNull
	public BiomeGUIManager getBiomeGUIManager() {
		return this.biomeGUIManager;
	}

	/**
	 * Gets the manager for handling visit-related GUI functionality.
	 *
	 * @return the visit GUI manager
	 */
	@NotNull
	public VisitGUIManager getVisitGUIManager() {
		return this.visitGUIManager;
	}

	/**
	 * Gets the manager for handling upgrade-related GUI functionality.
	 *
	 * @return the upgrade GUI manager
	 */
	@NotNull
	public UpgradeGUIManager getUpgradeGUIManager() {
		return this.upgradeGUIManager;
	}

	/**
	 * Gets the manager for handling display settings-related GUI functionality.
	 *
	 * @return the display settings GUI manager
	 */
	@NotNull
	public DisplaySettingsGUIManager getDisplaySettingsGUIManager() {
		return this.displaySettingsGUIManager;
	}

	/**
	 * Gets the manager for handling flags-related GUI functionality.
	 *
	 * @return the flags GUI manager
	 */
	@NotNull
	public FlagsGUIManager getFlagsGUIManager() {
		return this.flagsGUIManager;
	}

	/**
	 * Gets the manager for handling party-related GUI functionality.
	 *
	 * @return the party GUI manager
	 */
	@NotNull
	public PartyGUIManager getPartyGUIManager() {
		return this.partyGUIManager;
	}

	/**
	 * Gets the manager for handling invite-related GUI functionality.
	 *
	 * @return the invite GUI manager
	 */
	@NotNull
	public InviteGUIManager getInviteGUIManager() {
		return this.inviteGUIManager;
	}

	/**
	 * Gets the manager for handling challenges-related GUI functionality.
	 *
	 * @return the challenges GUI manager
	 */
	@NotNull
	public ChallengesGUIManager getChallengesGUIManager() {
		return this.challengesGUIManager;
	}

	/**
	 * Gets the manager for handling island choice-related GUI functionality.
	 *
	 * @return the island choice GUI manager
	 */
	@NotNull
	public IslandChoiceGUIManager getIslandChoiceGUIManager() {
		return this.islandChoiceGUIManager;
	}

	/**
	 * Gets the manager for handling schematic-related GUI functionality.
	 *
	 * @return the schematic GUI manager
	 */
	@NotNull
	public SchematicGUIManager getSchematicGUIManager() {
		return this.schematicGUIManager;
	}

	/**
	 * Gets the manager for handling reset confirmation GUI functionality.
	 *
	 * @return the reset confirmation GUI manager
	 */
	@NotNull
	public ResetConfirmGUIManager getResetConfirmGUIManager() {
		return this.resetConfirmGUIManager;
	}

	/**
	 * Gets the manager for handling co-op-related functionality.
	 *
	 * @return the co-op manager
	 */
	@NotNull
	public CoopManager getCoopManager() {
		return this.coopManager;
	}

	/**
	 * Gets the island generator for managing island generation-related
	 * functionality.
	 *
	 * @return the island generator
	 */
	@NotNull
	public IslandGenerator getIslandGenerator() {
		return this.islandGenerator;
	}

	/**
	 * Gets the handler for managing wraith-related functionality.
	 *
	 * @return the wraith handler
	 */
	@NotNull
	public WraithHandler getWraithHandler() {
		return this.wraithHandler;
	}

	/**
	 * Gets the handler for managing minion-related functionality.
	 *
	 * @return the minion handler
	 */
	@NotNull
	public MinionHandler getMinionHandler() {
		return this.minionHandler;
	}

	/**
	 * Gets the handler for managing island level-related functionality.
	 *
	 * @return the island level handler
	 */
	@NotNull
	public LevelHandler getIslandLevelManager() {
		return this.islandLevelManager;
	}

	/**
	 * Gets the converter for managing island choice-related functionality.
	 *
	 * @return the island choice converter
	 */
	@NotNull
	public IslandChoiceConverter getIslandChoiceConverter() {
		return this.islandChoiceConverter;
	}

	/**
	 * Gets the handler for managing glowstone tree-related functionality.
	 *
	 * @return the glowstone tree handler
	 */
	@NotNull
	public GlowTreeHandler getGlowstoneTreeHandler() {
		return this.glowstoneTreeHandler;
	}

	/**
	 * Gets the handler for managing border-related functionality.
	 *
	 * @return the border handler
	 */
	@NotNull
	public BorderHandler getBorderHandler() {
		return this.borderHandler;
	}

	/**
	 * Gets the handler for managing infinite lava-related functionality.
	 *
	 * @return the infinite lava handler
	 */
	@NotNull
	public InfiniteLavaHandler getInfiniteLavaHandler() {
		return this.infiniteLavaHandler;
	}

	/**
	 * Gets the handler for managing Netherrack generator-related functionality.
	 *
	 * @return the Netherrack generator handler
	 */
	@NotNull
	public NetherGeneratorHandler getNetherrackGeneratorHandler() {
		return this.netherrackGeneratorHandler;
	}

	/**
	 * Gets the handler for managing lava rain-related functionality.
	 *
	 * @return the lava rain handler
	 */
	@NotNull
	public RainHandler getLavaRainHandler() {
		return this.lavaRainHandler;
	}

	/**
	 * Gets the handler for managing Piglin barter-related functionality.
	 *
	 * @return the Piglin barter handler
	 */
	@NotNull
	public PiglinBarterHandler getPiglinBarterHandler() {
		return this.piglinBarterHandler;
	}

	/**
	 * Gets the handler for managing Wither boss-related functionality.
	 *
	 * @return the Wither boss handler
	 */
	@NotNull
	public WitherHandler getWitherHandler() {
		return this.witherHandler;
	}

	/**
	 * Gets the handler for managing Nether brewing-related functionality.
	 *
	 * @return the Nether brewing handler
	 */
	@NotNull
	public BrewingHandler getNetherBrewingHandler() {
		return this.netherBrewingHandler;
	}

	/**
	 * Gets the handler for managing farming-related functionality.
	 *
	 * @return the farming handler
	 */
	@NotNull
	public FarmingHandler getFarmingManager() {
		return this.farmingManager;
	}

	/**
	 * Gets the handler for managing Nether armor-related functionality.
	 *
	 * @return the Nether armor handler
	 */
	@NotNull
	public ArmorHandler getNetherArmorHandler() {
		return this.netherArmorHandler;
	}

	/**
	 * Gets the handler for managing Nether tool-related functionality.
	 *
	 * @return the Nether tool handler
	 */
	@NotNull
	public ToolsHandler getNetherToolsHandler() {
		return this.netherToolsHandler;
	}

	/**
	 * Gets the manager for handling challenges.
	 *
	 * @return the challenge manager
	 */
	@NotNull
	public ChallengeManager getChallengeManager() {
		return this.challengeManager;
	}

	/**
	 * Gets the manager for handling island protection.
	 *
	 * @return the protection manager
	 */
	@NotNull
	public ProtectionManager getProtectionManager() {
		return this.protectionManager;
	}

	/**
	 * Gets the handler for managing Hellblock-related functionality.
	 *
	 * @return the Hellblock handler
	 */
	@NotNull
	public HellblockHandler getHellblockHandler() {
		return this.hellblockHandler;
	}

	/**
	 * Gets the hopper for managing hopper-related functionality.
	 *
	 * @return the hopper handler
	 */
	@NotNull
	public HopperHandler getHopperHandler() {
		return this.hopperHandler;
	}

	/**
	 * Gets the handler for managing biome-related functionality.
	 *
	 * @return the biome handler
	 */
	@NotNull
	public BiomeHandler getBiomeHandler() {
		return this.biomeHandler;
	}

	/**
	 * Gets the handler for managing Hell Golem-related functionality.
	 *
	 * @return the Hell Golem handler
	 */
	@NotNull
	public GolemHandler getGolemHandler() {
		return this.hellGolemHandler;
	}

	/**
	 * Gets the player listener for handling player-related events.
	 *
	 * @return the player listener
	 */
	@NotNull
	public PlayerListener getPlayerListener() {
		return this.playerListener;
	}

	/**
	 * Gets the scheduler adapter for scheduling tasks.
	 *
	 * @return the scheduler adapter
	 */
	@NotNull
	public BukkitSchedulerAdapter getScheduler() {
		return this.scheduler;
	}

	/**
	 * Gets the sender factory for creating command senders.
	 *
	 * @return the sender factory
	 */
	@NotNull
	public SenderFactory<HellblockPlugin, CommandSender> getSenderFactory() {
		return this.senderFactory;
	}

	/**
	 * Gets the plugin logger for logging messages.
	 *
	 * @return the plugin logger
	 */
	@NotNull
	public PluginLogger getPluginLogger() {
		return this.pluginLogger;
	}

	/**
	 * Gets the debugger consumer for logging debug messages.
	 *
	 * @return the debugger consumer
	 */
	@NotNull
	public Consumer<Supplier<String>> getDebugger() {
		return this.debugger;
	}

	/**
	 * Checks if an update is available for the plugin.
	 *
	 * @return true if an update is available, false otherwise
	 */
	public boolean isUpdateAvailable() {
		return this.updateAvailable;
	}

	/**
	 * Checks if the plugin is currently in the process of reloading.
	 *
	 * @return true if the plugin is reloading, false otherwise
	 */
	public boolean isReloading() {
		return this.isReloading;
	}

	/**
	 * Formats a cooldown time in seconds into a human-readable string.
	 *
	 * @param seconds the cooldown time in seconds
	 * @return a formatted string representing the cooldown time
	 */
	public @NotNull String getFormattedCooldown(long seconds) {
		final long days = seconds / 86400;
		final long hours = (seconds % 86400) / 3600;
		final long minutes = (seconds % 3600) / 60;
		final long remainingSeconds = seconds % 60;

		final String dayFormat = getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_DAY.build().key());
		final String hourFormat = getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_HOUR.build().key());
		final String minuteFormat = getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_MINUTE.build().key());
		final String secondFormat = getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_SECOND.build().key());

		final StringBuilder formattedTime = new StringBuilder();
		if (days > 0) {
			formattedTime.append(days).append(dayFormat);
		}
		if (hours > 0) {
			formattedTime.append(hours).append(hourFormat);
		}
		if (minutes > 0) {
			formattedTime.append(minutes).append(minuteFormat);
		}
		if (remainingSeconds > 0) {
			formattedTime.append(remainingSeconds).append(secondFormat);
		}

		return formattedTime.toString();
	}

	/**
	 * Checks if a specified plugin is enabled on the Bukkit server.
	 *
	 * @param plugin The name of the plugin to check.
	 * @return True if the plugin is enabled, false otherwise.
	 */
	public boolean isHookedPluginEnabled(@NotNull String plugin) {
		return Bukkit.getPluginManager().isPluginEnabled(plugin);
	}

	/**
	 * Outputs a debugging message if the debug mode is enabled.
	 *
	 * @param message The debugging message to be logged.
	 */
	public void debug(@Nullable Object message) {
		if (message == null) {
			return;
		}
		this.debugger.accept(message::toString);
	}

	/**
	 * Outputs a debugging message if the debug mode is enabled.
	 *
	 * @param messageSupplier the supplier for the debugging mode.
	 */
	public void debug(Supplier<String> messageSupplier) {
		this.debugger.accept(messageSupplier);
	}

	/**
	 * Identifies the plugin associated with a given class loader.
	 *
	 * @param classLoader the class loader to identify
	 * @return the name of the plugin associated with the class loader, or null if
	 *         not found
	 * @throws ReflectiveOperationException if reflection fails
	 */
	public @Nullable String identifyClassLoader(ClassLoader classLoader) throws ReflectiveOperationException {
		final Class<?> pluginClassLoaderClass = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
		if (!pluginClassLoaderClass.isInstance(classLoader)) {
			return null;
		}
		final Method getPluginMethod = pluginClassLoaderClass.getDeclaredMethod("getPlugin");
		getPluginMethod.setAccessible(true);
		final JavaPlugin plugin = (JavaPlugin) getPluginMethod.invoke(classLoader);
		return plugin.getName();
	}
}