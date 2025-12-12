package com.swiftlicious.hellblock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.bukkit.Bukkit;
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
import com.swiftlicious.hellblock.challenges.ProgressBar;
import com.swiftlicious.hellblock.challenges.requirement.FishRequirement;
import com.swiftlicious.hellblock.commands.BukkitCommandManager;
import com.swiftlicious.hellblock.commands.CloudDependencyHelper;
import com.swiftlicious.hellblock.config.ConfigManager;
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
import com.swiftlicious.hellblock.enchantment.CrimsonThorns;
import com.swiftlicious.hellblock.enchantment.LavaVision;
import com.swiftlicious.hellblock.enchantment.MagmaWalker;
import com.swiftlicious.hellblock.enchantment.MoltenCore;
import com.swiftlicious.hellblock.events.HellblockReloadEvent;
import com.swiftlicious.hellblock.generation.BiomeHandler;
import com.swiftlicious.hellblock.generation.BorderHandler;
import com.swiftlicious.hellblock.generation.HellblockHandler;
import com.swiftlicious.hellblock.generation.IslandChoiceConverter;
import com.swiftlicious.hellblock.generation.IslandGenerator;
import com.swiftlicious.hellblock.generation.IslandManager;
import com.swiftlicious.hellblock.generation.IslandPlacementDetector;
import com.swiftlicious.hellblock.gui.biome.BiomeGUIManager;
import com.swiftlicious.hellblock.gui.challenges.ChallengesGUIManager;
import com.swiftlicious.hellblock.gui.choice.IslandChoiceGUIManager;
import com.swiftlicious.hellblock.gui.display.DisplaySettingsGUIManager;
import com.swiftlicious.hellblock.gui.event.EventGUIManager;
import com.swiftlicious.hellblock.gui.flags.FlagsGUIManager;
import com.swiftlicious.hellblock.gui.hellblock.HellblockGUIManager;
import com.swiftlicious.hellblock.gui.invite.InviteGUIManager;
import com.swiftlicious.hellblock.gui.market.MarketManager;
import com.swiftlicious.hellblock.gui.notification.NotificationGUIManager;
import com.swiftlicious.hellblock.gui.party.PartyGUIManager;
import com.swiftlicious.hellblock.gui.reset.ResetConfirmGUIManager;
import com.swiftlicious.hellblock.gui.schematic.SchematicGUIManager;
import com.swiftlicious.hellblock.gui.upgrade.UpgradeGUIManager;
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager;
import com.swiftlicious.hellblock.handlers.AbstractActionManager;
import com.swiftlicious.hellblock.handlers.AbstractRequirementManager;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureDependencyHelper;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.BlockActionManager;
import com.swiftlicious.hellblock.handlers.BlockRequirementManager;
import com.swiftlicious.hellblock.handlers.CoolDownManager;
import com.swiftlicious.hellblock.handlers.EventManager;
import com.swiftlicious.hellblock.handlers.HologramManager;
import com.swiftlicious.hellblock.handlers.IslandActionManager;
import com.swiftlicious.hellblock.handlers.IslandRequirementManager;
import com.swiftlicious.hellblock.handlers.PlayerActionManager;
import com.swiftlicious.hellblock.handlers.PlayerRequirementManager;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.handlers.VisitManager;
import com.swiftlicious.hellblock.handlers.chat.ChatManager;
import com.swiftlicious.hellblock.listeners.ArmorHandler;
import com.swiftlicious.hellblock.listeners.BrewingHandler;
import com.swiftlicious.hellblock.listeners.FarmingHandler;
import com.swiftlicious.hellblock.listeners.GlowTreeHandler;
import com.swiftlicious.hellblock.listeners.GolemHandler;
import com.swiftlicious.hellblock.listeners.HellblockPortalListener;
import com.swiftlicious.hellblock.listeners.HopperHandler;
import com.swiftlicious.hellblock.listeners.InfiniteLavaHandler;
import com.swiftlicious.hellblock.listeners.LevelHandler;
import com.swiftlicious.hellblock.listeners.MinionHandler;
import com.swiftlicious.hellblock.listeners.MobSpawnHandler;
import com.swiftlicious.hellblock.listeners.NetherGeneratorHandler;
import com.swiftlicious.hellblock.listeners.PiglinBarterHandler;
import com.swiftlicious.hellblock.listeners.PiglinInvasionHandler;
import com.swiftlicious.hellblock.listeners.PlayerListener;
import com.swiftlicious.hellblock.listeners.SkysiegeHandler;
import com.swiftlicious.hellblock.listeners.ToolsHandler;
import com.swiftlicious.hellblock.listeners.WitherHandler;
import com.swiftlicious.hellblock.listeners.WraithHandler;
import com.swiftlicious.hellblock.listeners.fishing.FishingManager;
import com.swiftlicious.hellblock.listeners.fishing.HookManager;
import com.swiftlicious.hellblock.listeners.fishing.StatisticsManager;
import com.swiftlicious.hellblock.listeners.weather.NetherWeatherManager;
import com.swiftlicious.hellblock.logging.PlatformLoggerProvider;
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
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.WorldManager;

public class HellblockPlugin extends JavaPlugin {

	protected static HellblockPlugin instance;

	protected GlowTreeHandler glowstoneTreeHandler;
	protected NetherWeatherManager netherWeatherManager;
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
	protected PiglinInvasionHandler invasionHandler;
	protected SkysiegeHandler skysiegeHandler;
	protected MobSpawnHandler mobSpawnHandler;
	protected PlayerListener playerListener;
	protected HellblockPortalListener portalListener;
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
	protected IslandManager islandManager;
	protected IslandBackupManager backupManager;
	protected ChallengeManager challengeManager;
	protected BorderHandler borderHandler;
	protected MailboxManager mailboxManager;
	protected ConfigManager configManager;

	protected PluginLogger pluginLogger;
	protected SenderFactory<HellblockPlugin, CommandSender> senderFactory;
	protected BukkitSchedulerAdapter scheduler;

	protected VisitManager visitManager;
	protected ChatManager chatManager;
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
	protected EventGUIManager eventGUIManager;
	protected NotificationGUIManager notificationGUIManager;
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
	protected AdventureDependencyHelper adventureDependencyHelper;

	protected LavaVision lavaVision;
	protected CrimsonThorns crimsonThorns;
	protected MoltenCore moltenCore;
	protected MagmaWalker magmaWalker;

	protected TpsMonitor tpsMonitor;
	protected Metrics bStats;

	protected final Map<Class<?>, ActionManager<?>> actionManagers = new HashMap<>();
	protected final Map<Class<?>, RequirementManager<?>> requirementManagers = new HashMap<>();

	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_RED = "\u001B[31m";
	private static final String ANSI_GREEN = "\u001B[32m";
	private static final String ANSI_YELLOW = "\u001B[33m";
	private static final String ANSI_CYAN = "\u001B[36m";

	protected long startTime = 0L;
	protected boolean initialStartup = false;
	protected boolean updateAvailable = false;
	protected boolean isReloading = false;
	protected boolean isLoaded = false;

	protected Consumer<Supplier<String>> debugger = (supplier -> {
	});

	/**
	 * Preloads the plugins and all of its dependencies.
	 */
	@Override
	public void onLoad() {
		instance = this;
		this.pluginLogger = PlatformLoggerProvider.forCurrentPlatform(this);

		// --- Detect version & runtime target ---
		String bukkitVersion = Bukkit.getBukkitVersion().split("-")[0];
		boolean isPaper = VersionHelper.isPaperForkPreInit(); // lightweight check
		String internalVersion = VersionHelper.resolveInternalVersion(bukkitVersion);
		String platform = isPaper ? "paper" : "spigot";

		// e.g. paper-v1_21_R6.jar
		String artifactName = String.format("%s-v%s.jar", platform, internalVersion);
		String resourcePath = "runtime/" + artifactName + ".gz";

		File runtimeDir = new File(getDataFolder(), "runtime");
		File target = new File(runtimeDir, artifactName);
		File hashFile = new File(runtimeDir, artifactName + ".sha1");

		try {
			// --- Ensure runtime directory exists ---
			if (!runtimeDir.exists() && !runtimeDir.mkdirs()) {
				getPluginLogger().severe("Failed to create runtime directory: " + runtimeDir);
				return;
			}

			// --- Compute hash of embedded gzipped resource ---
			String newHash;
			try (InputStream rawIn = getResource(resourcePath)) {
				if (rawIn == null) {
					getPluginLogger().severe("Missing gzipped runtime jar inside plugin resources: " + resourcePath);
					return;
				}
				try (GZIPInputStream gzIn = new GZIPInputStream(rawIn)) {
					newHash = computeSHA1(gzIn);
				}
			}

			// --- Compare hashes to decide extraction ---
			String oldHash = hashFile.exists() ? Files.readString(hashFile.toPath()).trim() : null;
			boolean shouldExtract = !target.exists() || !newHash.equals(oldHash);

			if (shouldExtract) {
				getPluginLogger().info("Extracting runtime jar: " + artifactName);

				try (InputStream rawIn = getResource(resourcePath)) {
					if (rawIn == null) {
						getPluginLogger()
								.severe("Missing gzipped runtime jar inside plugin resources: " + resourcePath);
						return;
					}

					try (GZIPInputStream gzIn = new GZIPInputStream(rawIn)) {
						Files.copy(gzIn, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				}

				Files.writeString(hashFile.toPath(), newHash, StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING);
				getPluginLogger().info("Extracted updated runtime jar: " + artifactName);
			}

			// --- Add runtime jar to classpath BEFORE initializing VersionHelper ---
			ReflectionClassPathAppender appender = new ReflectionClassPathAppender(this.getClassLoader());
			appender.addJarToClasspath(target.toPath());
			getPluginLogger().info("Injected runtime jar: " + artifactName);

			// --- Initialize version helpers (now runtime classes are available) ---
			VersionHelper.init(bukkitVersion);

			// --- Dependency setup ---
			this.scheduler = new BukkitSchedulerAdapter(this);
			this.dependencyManager = new DependencyManager(this, appender);

			final Set<Dependency> dependencies = EnumSet.allOf(Dependency.class);
			dependencies.removeAll(Stream
					.of(RelocationHandler.RELOCATION_DEPENDENCIES, CloudDependencyHelper.CLOUD_DEPENDENCIES,
							AdventureDependencyHelper.ADVENTURE_DEPENDENCIES)
					.flatMap(Set::stream).collect(Collectors.toSet()));

			getPluginLogger().info("Preloading %s dependencies...".formatted(dependencies.size()));
			this.dependencyManager.loadDependencies(dependencies);

			// Initialize Cloud & Adventure helpers (which use isolated runtime)
			this.cloudDependencyHelper = new CloudDependencyHelper(this);
			this.cloudDependencyHelper.load();

			this.adventureDependencyHelper = new AdventureDependencyHelper(this);
			this.adventureDependencyHelper.load();

			this.isLoaded = true;

		} catch (Throwable t) {
			getPluginLogger().severe("Failed during runtime jar injection or dependency loading: " + artifactName);
			t.printStackTrace();
		}
	}

	/**
	 * Computes SHA-1 hash for identifying runtime updates.
	 */
	private static String computeSHA1(InputStream in) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		try (DigestInputStream dis = new DigestInputStream(in, digest)) {
			dis.transferTo(OutputStream.nullOutputStream());
		}
		byte[] hashBytes = digest.digest();
		StringBuilder sb = new StringBuilder();
		for (byte b : hashBytes)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	/**
	 * Enables the plugin and initializes its components.
	 */
	@Override
	public void onEnable() {
		if (!this.isLoaded) {
			getPluginLogger().severe("Hellblock was unable to setup correctly.");
			getPluginLogger().severe("Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		this.startTime = System.currentTimeMillis();

		if (!VersionHelper.getSupportedVersions().contains(VersionHelper.getServerVersion())) {
			getPluginLogger().severe(
					"Hellblock only supports legacy versions down to 1.17. Please update your server to be able to properly use this plugin.");
			getPluginLogger().severe("Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		this.tpsMonitor = new TpsMonitor();
		// Schedule a repeating task every tick to record TPS samples
		getScheduler().sync().runRepeating(getTpsMonitor()::onTick, 1L, 1L, LocationUtils.getAnyLocationInstance());

		this.translationManager = new TranslationManager(this);
		registerDataManager(Player.class, new PlayerRequirementManager(this), new PlayerActionManager(this));
		registerDataManager(CustomBlockState.class, new BlockRequirementManager(this), new BlockActionManager(this));
		registerDataManager(Integer.class, new IslandRequirementManager(this), new IslandActionManager(this));
		this.placeholderManager = new PlaceholderManager(this);
		this.lootManager = new LootManager(this);
		this.effectManager = new EffectManager(this);
		this.eventManager = new EventManager(this);
		this.itemManager = new ItemManager(this);
		this.entityManager = new EntityManager(this);
		this.blockManager = new BlockManager(this);
		this.hookManager = new HookManager(this);
		this.configManager = new ConfigManager(this);
		this.itemManager.registerProviders();
		this.entityManager.registerProviders();
		this.blockManager.registerProviders();
		// after ConfigManager
		this.debugger = getConfigManager().debug() ? (s) -> getPluginLogger().info("[DEBUG] " + s.get()) : (s) -> {
		};
		this.coopManager = new CoopManager(this);
		this.islandManager = new IslandManager(this);
		this.storageManager = new StorageManager(this);
		this.storageManager.initialize();
		this.storageManager.reload();

		this.magmaWalker = new MagmaWalker(this);
		this.moltenCore = new MoltenCore(this);
		this.crimsonThorns = new CrimsonThorns(this);
		this.lavaVision = new LavaVision(this);
		this.visitManager = new VisitManager(this);
		this.chatManager = new ChatManager(this);
		this.mailboxManager = new MailboxManager(this);
		this.netherrackGeneratorHandler = new NetherGeneratorHandler(this);
		this.netherWeatherManager = new NetherWeatherManager(this);
		this.farmingManager = new FarmingHandler(this);
		this.islandGenerator = new IslandGenerator(this);
		this.netherBrewingHandler = new BrewingHandler(this);
		this.glowstoneTreeHandler = new GlowTreeHandler(this);
		this.infiniteLavaHandler = new InfiniteLavaHandler(this);
		this.witherHandler = new WitherHandler(this);
		this.wraithHandler = new WraithHandler(this);
		this.minionHandler = new MinionHandler(this);
		this.skysiegeHandler = new SkysiegeHandler(this);
		this.invasionHandler = new PiglinInvasionHandler(this);
		this.biomeHandler = new BiomeHandler(this);
		this.borderHandler = new BorderHandler(this);
		this.mobSpawnHandler = new MobSpawnHandler(this);
		this.islandChoiceConverter = new IslandChoiceConverter(this);
		this.placementDetector = new IslandPlacementDetector(this);
		this.hopperHandler = new HopperHandler(this);
		this.islandLevelManager = new LevelHandler(this);
		this.protectionManager = new ProtectionManager(this);
		this.challengeManager = new ChallengeManager(this);
		this.adventureDependencyHelper.createBukkitAudiences();
		this.senderFactory = new BukkitSenderFactory(this);
		this.fishingManager = new FishingManager(this);
		this.backupManager = new IslandBackupManager(this);
		this.hellblockGUIManager = new HellblockGUIManager(this);
		this.biomeGUIManager = new BiomeGUIManager(this);
		this.flagsGUIManager = new FlagsGUIManager(this);
		this.upgradeGUIManager = new UpgradeGUIManager(this);
		this.partyGUIManager = new PartyGUIManager(this);
		this.notificationGUIManager = new NotificationGUIManager(this);
		this.visitGUIManager = new VisitGUIManager(this);
		this.displaySettingsGUIManager = new DisplaySettingsGUIManager(this);
		this.challengesGUIManager = new ChallengesGUIManager(this);
		this.eventGUIManager = new EventGUIManager(this);
		this.islandChoiceGUIManager = new IslandChoiceGUIManager(this);
		this.schematicGUIManager = new SchematicGUIManager(this);
		this.inviteGUIManager = new InviteGUIManager(this);
		this.resetConfirmGUIManager = new ResetConfirmGUIManager(this);
		this.cooldownManager = new CoolDownManager(this);
		this.playerListener = new PlayerListener(this);
		this.portalListener = new HellblockPortalListener(this);
		this.integrationManager = new IntegrationManager(this);
		this.integrationManager.initialize();
		this.upgradeManager = new UpgradeManager(this);
		this.hellblockHandler = new HellblockHandler(this);
		this.marketManager = new MarketManager(this);
		this.statisticsManager = new StatisticsManager(this);
		this.hologramManager = new HologramManager(this);
		this.schematicManager = new SchematicManager(this);
		this.worldManager = new WorldManager(this);
		this.commandManager = new BukkitCommandManager(this);
		// Run after plugin fully loads, ensures main loader context
		getScheduler().executeSync(getCommandManager()::registerDefaultFeatures);
		this.netherToolsHandler = new ToolsHandler(this);
		this.netherArmorHandler = new ArmorHandler(this);
		this.piglinBarterHandler = new PiglinBarterHandler(this);
		this.hellGolemHandler = new GolemHandler(this);
		this.initialStartup = true;

		// Use scheduler to allow other plugins to load before reloading to prevent
		// conflicts
		runReloadTask(this, this::reload);
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
				if (getIslandManager() != null)
					getIslandManager().resolveIslandId(onlineUser.getPlayer().getLocation())
							.thenAccept(optIslandId -> optIslandId.ifPresent(islandId -> getIslandManager()
									.handlePlayerLeaveIsland(onlineUser.getPlayer(), islandId)));

				final Player player = onlineUser.getPlayer();
				if ((onlineUser.hasGlowstoneToolEffect() || onlineUser.hasGlowstoneArmorEffect())
						&& player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					onlineUser.isHoldingGlowstoneTool(false);
					onlineUser.isWearingGlowstoneArmor(false);
				}
			}
		}

		if (getCommandManager() != null && getCommandManager().getWorldTileHelper() != null) {
			getCommandManager().getWorldTileHelper().stopCachingTileEntities();
		}

		if (this.netherWeatherManager != null) {
			this.netherWeatherManager.disable();
		}
		if (this.bStats != null) {
			this.bStats.shutdown();
		}
		if (this.magmaWalker != null) {
			this.magmaWalker.disable();
		}
		if (this.moltenCore != null) {
			this.moltenCore.disable();
		}
		if (this.crimsonThorns != null) {
			this.crimsonThorns.disable();
		}
		if (this.lavaVision != null) {
			this.lavaVision.disable();
		}
		if (this.schematicManager != null) {
			this.schematicManager.disable();
		}
		if (this.protectionManager != null) {
			this.protectionManager.disable();
		}
		if (this.coopManager != null) {
			this.coopManager.disable();
		}
		if (this.portalListener != null) {
			this.portalListener.disable();
		}
		if (this.challengeManager != null) {
			this.challengeManager.disable();
		}
		if (this.farmingManager != null) {
			this.farmingManager.disable();
		}
		if (this.hopperHandler != null) {
			this.hopperHandler.disable();
		}
		if (this.upgradeManager != null) {
			this.upgradeManager.disable();
		}
		if (this.netherrackGeneratorHandler != null) {
			this.netherrackGeneratorHandler.disable();
		}
		if (this.glowstoneTreeHandler != null) {
			this.glowstoneTreeHandler.disable();
		}
		if (this.infiniteLavaHandler != null) {
			this.infiniteLavaHandler.disable();
		}
		if (this.piglinBarterHandler != null) {
			this.piglinBarterHandler.disable();
		}
		if (this.hellGolemHandler != null) {
			this.hellGolemHandler.disable();
		}
		if (this.minionHandler != null) {
			this.minionHandler.disable();
		}
		if (this.skysiegeHandler != null) {
			this.skysiegeHandler.disable();
		}
		if (this.invasionHandler != null) {
			this.invasionHandler.disable();
		}
		if (this.witherHandler != null) {
			this.witherHandler.disable();
		}
		if (this.chatManager != null) {
			this.chatManager.disable();
		}
		if (this.mobSpawnHandler != null) {
			this.mobSpawnHandler.disable();
		}
		if (this.wraithHandler != null) {
			this.wraithHandler.disable();
		}
		if (this.borderHandler != null) {
			this.borderHandler.disable();
		}
		if (this.placementDetector != null) {
			this.placementDetector.disable();
		}
		if (this.islandGenerator != null) {
			this.islandGenerator.disable();
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
		if (this.backupManager != null) {
			this.backupManager.disable();
		}
		if (this.eventManager != null) {
			this.eventManager.disable();
		}
		if (this.marketManager != null) {
			this.marketManager.disable();
		}
		if (this.islandManager != null) {
			this.islandManager.disable();
		}
		if (this.visitManager != null) {
			this.visitManager.disable();
		}
		if (this.hellblockGUIManager != null) {
			this.hellblockGUIManager.disable();
		}
		if (this.visitGUIManager != null) {
			this.visitGUIManager.disable();
		}
		if (this.eventGUIManager != null) {
			this.eventGUIManager.disable();
		}
		if (this.notificationGUIManager != null) {
			this.notificationGUIManager.disable();
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
		if (this.cooldownManager != null) {
			this.cooldownManager.disable();
		}
		if (this.worldManager != null) {
			this.worldManager.disable();
		}
		if (this.islandLevelManager != null) {
			this.islandLevelManager.disable();
		}
		if (this.hologramManager != null) {
			this.hologramManager.disable();
		}
		if (this.statisticsManager != null) {
			this.statisticsManager.disable();
		}
		if (this.netherToolsHandler != null) {
			this.netherToolsHandler.disable();
		}
		if (this.netherArmorHandler != null) {
			this.netherArmorHandler.disable();
		}
		if (this.netherBrewingHandler != null) {
			this.netherBrewingHandler.disable();
		}
		if (this.hellblockHandler != null) {
			this.hellblockHandler.disable();
		}
		if (this.senderFactory != null) {
			this.senderFactory.close();
		}
		if (this.actionManagers != null) {
			this.actionManagers.values().stream().filter(Objects::nonNull).forEach(Reloadable::disable);
		}
		if (this.requirementManagers != null) {
			this.requirementManagers.values().stream().filter(Objects::nonNull).forEach(Reloadable::disable);
		}
		if (this.storageManager != null) {
			this.storageManager.shutdownPreloadExecutor();
			this.storageManager.disable();
		}
		if (this.placeholderManager != null) {
			this.placeholderManager.disable();
		}
		if (this.hookManager != null) {
			this.hookManager.disable();
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

		AdventureHelper.resetInstance();
		MechanicType.reset();
		ProgressBar.clearColorCache();
		getCommandManager().getWorldTileHelper().stopCachingTileEntities();
		this.itemManager.unload();
		this.eventManager.unload();
		this.entityManager.unload();
		this.lootManager.unload();
		this.blockManager.unload();
		this.effectManager.unload();
		this.hookManager.unload();
		this.worldManager.unload();
		this.actionManagers.values().forEach(Reloadable::reload);
		this.requirementManagers.values().forEach(Reloadable::reload);

		// Reload early components
		this.placeholderManager.reload();
		this.configManager.reload();

		// Re-assign debugger
		this.debugger = getConfigManager().debug() ? (s) -> getPluginLogger().info("[DEBUG] " + s.get()) : (s) -> {
		};

		if (getConfigManager().metrics()) {
			this.bStats = new Metrics(this, 23739);
		}

		this.translationManager.reload();
		this.netherWeatherManager.reload();
		this.cooldownManager.reload();
		this.visitManager.reload();
		this.islandManager.reload();
		this.marketManager.reload();
		this.statisticsManager.reload();
		this.storageManager.reload();
		this.fishingManager.reload();
		this.hologramManager.reload();
		this.coopManager.reload();
		this.worldManager.load();

		getWorldManager().getWorldLoadingCompletion().thenRun(() -> {
			try {
				this.protectionManager.reload();
				this.schematicManager.reload();
				this.backupManager.reload();
				this.upgradeManager.reload();
				this.placementDetector.reload();
				this.placementDetector.initialize();
				this.hellblockHandler.reload();
				this.itemManager.load();
				this.eventManager.load();
				this.entityManager.load();
				this.lootManager.load();
				this.blockManager.load();
				this.effectManager.load();
				this.hookManager.load();
				this.magmaWalker.reload();
				this.moltenCore.reload();
				this.lavaVision.reload();
				this.crimsonThorns.reload();
				this.mobSpawnHandler.reload();
				this.playerListener.reload();
				this.portalListener.reload();
				this.chatManager.reload();
				this.islandGenerator.reload();
				this.hellGolemHandler.reload();
				this.witherHandler.reload();
				this.minionHandler.reload();
				this.skysiegeHandler.reload();
				this.borderHandler.reload();
				this.glowstoneTreeHandler.reload();
				this.infiniteLavaHandler.reload();
				this.piglinBarterHandler.reload();
				this.netherrackGeneratorHandler.reload();
				this.wraithHandler.reload();
				this.invasionHandler.reload();
				this.islandLevelManager.reload();
				this.hopperHandler.reload();
				this.farmingManager.reload();

				FishRequirement.reloadContents(this);

				this.challengeManager.reload();
				this.netherToolsHandler.reload();
				this.netherArmorHandler.reload();
				this.netherBrewingHandler.reload();
				this.hellblockGUIManager.reload();
				this.biomeGUIManager.reload();
				this.upgradeGUIManager.reload();
				this.flagsGUIManager.reload();
				this.partyGUIManager.reload();
				this.inviteGUIManager.reload();
				this.challengesGUIManager.reload();
				this.islandChoiceGUIManager.reload();
				this.notificationGUIManager.reload();
				this.displaySettingsGUIManager.reload();
				this.schematicGUIManager.reload();
				this.visitGUIManager.reload();
				this.eventGUIManager.reload();
				this.resetConfirmGUIManager.reload();

				getCommandManager().getWorldTileHelper().startCachingTileEntities(200L);
				getCommandManager().getBioPatternHelper()
						.precompileBannedWordPatterns(getConfigManager().bannedWords());

				// Handle online users
				for (UserData onlineUser : getStorageManager().getOnlineUsers()) {
					final Player player = onlineUser.getPlayer();
					if (player != null && player.isOnline()) {
						getIslandManager().resolveIslandId(player.getLocation()).thenAccept(optIslandId -> optIslandId
								.ifPresent(islandId -> getIslandManager().handlePlayerEnterIsland(player, islandId)));
						getHellblockHandler().handleVisitingIsland(player, onlineUser);
					}
				}

				// Purge inactive Hellblocks
				getHellblockHandler().purgeInactiveHellblocks().thenRun(() -> {
					if (this.initialStartup) {
						double finalTime = (System.currentTimeMillis() - this.startTime) / 1000.0;
						getPluginLogger().info("Took " + finalTime + " second" + (finalTime == 1.0 ? "" : "s")
								+ " to setup Hellblock!");
						this.initialStartup = false;
					}
					EventUtils.fireAndForget(new HellblockReloadEvent(this));
					this.isReloading = false;
				}).exceptionally(ex -> {
					getPluginLogger().severe("HellblockHandler failed to purge inactive hellblocks: " + ex.getMessage(),
							ex);
					this.isReloading = false;
					return null;
				});
			} catch (Exception ex) {
				getPluginLogger().severe("Exception during plugin reload after world loading: " + ex.getMessage(), ex);
				this.isReloading = false;
			}
		}).exceptionally(ex -> {
			getPluginLogger().severe("WorldManager failed to load worlds: " + ex.getMessage(), ex);
			this.isReloading = false;
			return null;
		}).thenRun(() -> {
			if (getConfigManager().checkUpdate()) {
				VersionHelper.checkUpdate.apply(this).thenAccept(result -> {
					getPluginLogger().info(colorLog(ANSI_CYAN, "Checking for updates..."));
					final String link = "https://github.com/Swiftlicious01/Hellblock/releases";
					final String current = VersionHelper.getCurrentVersion();
					final String latest = VersionHelper.getLatestVersion();

					if (!result) {
						getPluginLogger().info(
								colorLog(ANSI_GREEN, "You are using the latest version (v%s).".formatted(current)));
					} else {
						String message = String.format(
								"%s===============================================\n"
										+ "      A new version of %sHellblock%s is available!\n"
										+ "      Current version: %s\n" + "      Latest version:  %s\n"
										+ "      Download: %s\n" + "===============================================%s",
								ANSI_YELLOW, ANSI_RED, ANSI_YELLOW, current, latest, link, ANSI_RESET);

						getPluginLogger().warn(colorLog(ANSI_YELLOW, message));
						this.updateAvailable = true;
					}
				}).exceptionally(ex -> {
					getPluginLogger().severe("VersionHelper failed to check for updates: " + ex.getMessage(), ex);
					return null;
				});
			}
		});
	}

	/**
	 * Executes a plugin reload task using the appropriate scheduler for the current
	 * server platform (Folia, Paper, or Spigot).
	 * <p>
	 * This method automatically detects whether the server is running on Folia and,
	 * if so, uses the {@code GlobalRegionScheduler} to execute the task in a
	 * thread-safe manner. When running on Spigot or other non-Folia platforms, it
	 * gracefully falls back to the standard Bukkit scheduler.
	 * <p>
	 * Using this method allows plugin developers to safely execute reload or
	 * synchronous tasks without introducing compile-time dependencies on Folia
	 * APIs.
	 *
	 * @param plugin the plugin instance requesting the reload operation
	 * @param task   the task to execute (typically {@code this::reload})
	 */
	private void runReloadTask(@NotNull Plugin plugin, @NotNull Runnable task) {
		if (VersionHelper.isPaperFork()) {
			try {
				Method method = Bukkit.class.getMethod("getGlobalRegionScheduler");
				Object scheduler = method.invoke(null);

				Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class);
				runMethod.invoke(scheduler, plugin, (Consumer<Object>) (scheduledTask) -> task.run());
				return;
			} catch (ReflectiveOperationException e) {
				getPluginLogger().warn("Failed to use Folia global region scheduler: " + e.getMessage());
			}
		}

		// Fallback to standard Spigot scheduler
		Bukkit.getScheduler().runTask(plugin, task);
	}

	/**
	 * Gets the instance of the hellblock plugin for anything related to the plugin.
	 *
	 * @return the hellblock plugin instance
	 */
	@NotNull
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
	 * Gets the island manager for handling island-related functionality.
	 *
	 * @return the island manager
	 */
	@NotNull
	public IslandManager getIslandManager() {
		return this.islandManager;
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
	 * Gets the chat manager for handling chat-related functionality.
	 *
	 * @return the chat manager
	 */
	@NotNull
	public ChatManager getChatManager() {
		return this.chatManager;
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

	/**
	 * Gets the mailbox manager for handling mailbox-related functionality.
	 *
	 * @return the mailbox manager
	 */
	@NotNull
	public MailboxManager getMailboxManager() {
		return this.mailboxManager;
	}

	/**
	 * Gets the cloud dependency helper for handling cloud dependency-related
	 * functionality.
	 *
	 * @return the cloud dependency helper
	 */
	@NotNull
	public CloudDependencyHelper getCloudDependencyHelper() {
		return this.cloudDependencyHelper;
	}

	/**
	 * Gets the adventure dependency helper for handling adventure
	 * dependency-related functionality.
	 *
	 * @return the adventure dependency helper
	 */
	@NotNull
	public AdventureDependencyHelper getAdventureDependencyHelper() {
		return this.adventureDependencyHelper;
	}

	/**
	 * Gets the mob spawn handler for handling mob spawn-related functionality.
	 *
	 * @return the mob spawn handler
	 */
	@NotNull
	public MobSpawnHandler getMobSpawnHandler() {
		return this.mobSpawnHandler;
	}

	/**
	 * Gets the piglin invasion handler for handling piglin invasion-related
	 * functionality.
	 *
	 * @return the piglin invasion handler
	 */
	@NotNull
	public PiglinInvasionHandler getInvasionHandler() {
		return this.invasionHandler;
	}

	/**
	 * Gets the skysiege handler for handling skysiege-related functionality.
	 *
	 * @return the skysiege handler
	 */
	@NotNull
	public SkysiegeHandler getSkysiegeHandler() {
		return this.skysiegeHandler;
	}

	/**
	 * Retrieves an ActionManager for a specific type.
	 *
	 * @param type the class type of the action
	 * @return the {@link ActionManager} for the specified type
	 * @throws IllegalArgumentException if the type is null
	 */
	@SuppressWarnings("unchecked")
	@NotNull
	public <T> ActionManager<T> getActionManager(@NotNull Class<T> type) {
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
	@NotNull
	public <T> RequirementManager<T> getRequirementManager(@NotNull Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("Type cannot be null");
		}
		return (RequirementManager<T>) requirementManagers.get(type);
	}

	private <T> void registerDataManager(@NotNull Class<T> type,
			@NotNull AbstractRequirementManager<T> requirementManager,
			@NotNull AbstractActionManager<T> actionManager) {
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
	 * Gets the manager for handling portal-related functionality.
	 *
	 * @return the portal manager
	 */
	@NotNull
	public HellblockPortalListener getPortalHandler() {
		return this.portalListener;
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
	 * Gets the manager for handling notification-related GUI functionality.
	 *
	 * @return the notification GUI manager
	 */
	@NotNull
	public NotificationGUIManager getNotificationGUIManager() {
		return this.notificationGUIManager;
	}

	/**
	 * Gets the manager for handling event-related GUI functionality.
	 *
	 * @return the event GUI manager
	 */
	@NotNull
	public EventGUIManager getEventGUIManager() {
		return this.eventGUIManager;
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
	 * Gets the handler for managing nether weather-related functionality.
	 *
	 * @return the nether weather manager
	 */
	@NotNull
	public NetherWeatherManager getNetherWeatherManager() {
		return this.netherWeatherManager;
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
	 * Gets the handler for handling the magma walker enchantment.
	 *
	 * @return the magma walker handler
	 */
	@NotNull
	public MagmaWalker getMagmaWalkerHandler() {
		return this.magmaWalker;
	}

	/**
	 * Gets the handler for handling the lava vision enchantment.
	 *
	 * @return the lava vision handler
	 */
	@NotNull
	public LavaVision getLavaVisionHandler() {
		return this.lavaVision;
	}

	/**
	 * Gets the handler for handling the molten core enchantment.
	 *
	 * @return the molten core handler
	 */
	@NotNull
	public MoltenCore getMoltenCoreHandler() {
		return this.moltenCore;
	}

	/**
	 * Gets the handler for handling the crimson thorns enchantment.
	 *
	 * @return the crimson thorns handler
	 */
	@NotNull
	public CrimsonThorns getCrimsonThornsHandler() {
		return this.crimsonThorns;
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
	public void debug(@NotNull Supplier<String> messageSupplier) {
		this.debugger.accept(messageSupplier);
	}

	/**
	 * Wraps the given message with ANSI color codes for colored console output.
	 * <p>
	 * This method is intended for use in terminals or environments that support
	 * ANSI escape codes, such as local development consoles or when using a custom
	 * console appender like Aikar's TerminalConsoleAppender. It appends an ANSI
	 * reset code at the end to prevent color bleed into subsequent log lines.
	 *
	 * @param ansi    The ANSI color code to apply (e.g. {@code "\u001B[31m"} for
	 *                red).
	 * @param message The plain text message to colorize.
	 * @return The message wrapped in the given ANSI color code and reset code.
	 */
	@NotNull
	private String colorLog(@NotNull String ansi, @NotNull String message) {
		return ansi + message + ANSI_RESET;
	}

	/**
	 * Identifies the plugin associated with a given class loader.
	 *
	 * @param classLoader the class loader to identify
	 * @return the name of the plugin associated with the class loader, or null if
	 *         not found
	 * @throws ReflectiveOperationException if reflection fails
	 */
	@Nullable
	public String identifyClassLoader(@NotNull ClassLoader classLoader) throws ReflectiveOperationException {
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