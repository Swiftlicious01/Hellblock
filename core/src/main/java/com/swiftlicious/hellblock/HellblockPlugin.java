package com.swiftlicious.hellblock;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Metrics;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.ChallengeManager;
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
import com.swiftlicious.hellblock.events.HellblockReloadEvent;
import com.swiftlicious.hellblock.generation.BiomeHandler;
import com.swiftlicious.hellblock.generation.HellblockHandler;
import com.swiftlicious.hellblock.generation.IslandChoiceConverter;
import com.swiftlicious.hellblock.generation.IslandGenerator;
import com.swiftlicious.hellblock.gui.biome.BiomeGUIManager;
import com.swiftlicious.hellblock.gui.challenges.ChallengesGUIManager;
import com.swiftlicious.hellblock.gui.choice.IslandChoiceGUIManager;
import com.swiftlicious.hellblock.gui.flags.FlagsGUIManager;
import com.swiftlicious.hellblock.gui.hellblock.HellblockGUIManager;
import com.swiftlicious.hellblock.gui.invite.InviteGUIManager;
import com.swiftlicious.hellblock.gui.market.MarketManager;
import com.swiftlicious.hellblock.gui.party.PartyGUIManager;
import com.swiftlicious.hellblock.gui.reset.ResetConfirmGUIManager;
import com.swiftlicious.hellblock.gui.schematic.SchematicGUIManager;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.BlockActionManager;
import com.swiftlicious.hellblock.handlers.PlayerActionManager;
import com.swiftlicious.hellblock.handlers.CoolDownManager;
import com.swiftlicious.hellblock.handlers.EventManager;
import com.swiftlicious.hellblock.handlers.HologramManager;
import com.swiftlicious.hellblock.handlers.BlockRequirementManager;
import com.swiftlicious.hellblock.handlers.PlayerRequirementManager;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.GlowTreeHandler;
import com.swiftlicious.hellblock.listeners.InfiniteLavaHandler;
import com.swiftlicious.hellblock.listeners.LevelHandler;
import com.swiftlicious.hellblock.listeners.ArmorHandler;
import com.swiftlicious.hellblock.listeners.BrewingHandler;
import com.swiftlicious.hellblock.listeners.FarmingHandler;
import com.swiftlicious.hellblock.listeners.GolemHandler;
import com.swiftlicious.hellblock.listeners.ToolsHandler;
import com.swiftlicious.hellblock.listeners.NetherGeneratorHandler;
import com.swiftlicious.hellblock.listeners.PiglinBarterHandler;
import com.swiftlicious.hellblock.listeners.PlayerListener;
import com.swiftlicious.hellblock.listeners.WitherHandler;
import com.swiftlicious.hellblock.listeners.fishing.FishingManager;
import com.swiftlicious.hellblock.listeners.fishing.HookManager;
import com.swiftlicious.hellblock.listeners.fishing.StatisticsManager;
import com.swiftlicious.hellblock.listeners.rain.RainHandler;
import com.swiftlicious.hellblock.logging.JavaPluginLogger;
import com.swiftlicious.hellblock.logging.PluginLogger;
import com.swiftlicious.hellblock.loot.LootManager;
import com.swiftlicious.hellblock.placeholders.PlaceholderManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.ProtectionManager;
import com.swiftlicious.hellblock.scheduler.BukkitSchedulerAdapter;
import com.swiftlicious.hellblock.schematic.SchematicManager;
import com.swiftlicious.hellblock.sender.SenderFactory;
import com.swiftlicious.hellblock.sender.BukkitSenderFactory;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.world.HellblockBlockState;
import com.swiftlicious.hellblock.world.HellblockWorld;
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
	protected GolemHandler netherSnowGolemHandler;
	protected WitherHandler witherBossHandler;
	protected PlayerListener playerListener;
	protected NetherGeneratorHandler netherrackGeneratorHandler;
	protected IslandGenerator islandGenerator;
	protected ProtectionManager protectionManager;
	protected HellblockHandler hellblockHandler;
	protected BiomeHandler biomeHandler;
	protected IslandChoiceConverter islandChoiceConverter;
	protected CoopManager coopManager;
	protected LevelHandler islandLevelManager;
	protected SchematicManager schematicManager;
	protected WorldManager worldManager;
	protected ChallengeManager challengeManager;
	protected ConfigManager configManager;

	protected PluginLogger pluginLogger;
	protected SenderFactory<HellblockPlugin, CommandSender> senderFactory;
	protected BukkitSchedulerAdapter scheduler;

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
	protected PartyGUIManager partyGUIManager;
	protected InviteGUIManager inviteGUIManager;
	protected ChallengesGUIManager challengesGUIManager;
	protected IslandChoiceGUIManager islandChoiceGUIManager;
	protected SchematicGUIManager schematicGUIManager;
	protected ResetConfirmGUIManager resetConfirmGUIManager;
	protected StorageManager storageManager;
	protected DependencyManager dependencyManager;
	protected TranslationManager translationManager;

	protected final Map<Class<?>, ActionManager<?>> actionManagers = new HashMap<>();
	protected final Map<Class<?>, RequirementManager<?>> requirementManagers = new HashMap<>();

	protected boolean updateAvailable = false;
	protected boolean isReloading = false;
	protected Consumer<Supplier<String>> debugger = (supplier -> {
	});

	@Override
	public void onLoad() {
		instance = this;
		this.pluginLogger = new JavaPluginLogger(this.getLogger());
		VersionHelper.init(Bukkit.getBukkitVersion().split("-")[0]);
		this.scheduler = new BukkitSchedulerAdapter(this);
		this.dependencyManager = new DependencyManager(this, new ReflectionClassPathAppender(this.getClassLoader()));
		Set<Dependency> dependencies = new HashSet<>(Arrays.asList(Dependency.values()));
		dependencies.removeAll(RelocationHandler.DEPENDENCIES);
		getPluginLogger().info(String.format("Preloading %s dependencies...", dependencies.size()));
		this.dependencyManager.loadDependencies(dependencies);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		double startTime = System.currentTimeMillis();

		// TODO: test support from 1.17.1 to 1.21.8+
		if (!VersionHelper.getSupportedVersions().contains(VersionHelper.getServerVersion())) {
			getPluginLogger().severe(
					"Hellblock only supports legacy versions down to 1.17.1. Please update your server to be able to properly use this plugin.");
			getPluginLogger().severe("Disabling plugin...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// TODO: test spigot
		if (!VersionHelper.isPaper()) {
			getPluginLogger().severe("Hellblock is more suited towards Paper, but will still work with Spigot.");
			PaperLib.suggestPaper(this);
		}

		this.configManager = new ConfigManager(this);
		this.configManager.load();
		// after ConfigManager
		this.debugger = getConfigManager().debug() ? (s) -> pluginLogger.info("[DEBUG] " + s.get()) : (s) -> {
		};

		this.netherrackGeneratorHandler = new NetherGeneratorHandler(this);
		this.lavaRainHandler = new RainHandler(this);
		this.farmingManager = new FarmingHandler(this);
		this.islandGenerator = new IslandGenerator(this);
		this.glowstoneTreeHandler = new GlowTreeHandler(this);
		this.infiniteLavaHandler = new InfiniteLavaHandler(this);
		this.witherBossHandler = new WitherHandler(this);
		this.biomeHandler = new BiomeHandler(this);
		this.islandChoiceConverter = new IslandChoiceConverter(this);
		this.coopManager = new CoopManager(this);
		this.islandLevelManager = new LevelHandler(this);
		this.protectionManager = new ProtectionManager(this);
		this.challengeManager = new ChallengeManager(this);
		this.senderFactory = new BukkitSenderFactory(this);
		this.blockManager = new BlockManager(this);
		this.effectManager = new EffectManager(this);
		this.fishingManager = new FishingManager(this);
		this.eventManager = new EventManager(this);
		this.itemManager = new ItemManager(this);
		this.lootManager = new LootManager(this);
		this.marketManager = new MarketManager(this);
		this.hellblockGUIManager = new HellblockGUIManager(this);
		this.biomeGUIManager = new BiomeGUIManager(this);
		this.flagsGUIManager = new FlagsGUIManager(this);
		this.partyGUIManager = new PartyGUIManager(this);
		this.challengesGUIManager = new ChallengesGUIManager(this);
		this.islandChoiceGUIManager = new IslandChoiceGUIManager(this);
		this.schematicGUIManager = new SchematicGUIManager(this);
		this.resetConfirmGUIManager = new ResetConfirmGUIManager(this);
		this.entityManager = new EntityManager(this);
		this.placeholderManager = new PlaceholderManager(this);
		this.cooldownManager = new CoolDownManager(this);
		this.playerListener = new PlayerListener(this);
		this.integrationManager = new IntegrationManager(this);
		this.hellblockHandler = new HellblockHandler(this);
		this.storageManager = new StorageManager(this);
		this.statisticsManager = new StatisticsManager(this);
		this.hologramManager = new HologramManager(this);
		this.hookManager = new HookManager(this);
		this.schematicManager = new SchematicManager(this);
		this.worldManager = new WorldManager(this);
		this.translationManager = new TranslationManager(this);
		this.commandManager = new BukkitCommandManager(this);
		this.commandManager.registerDefaultFeatures();
		this.netherBrewingHandler = new BrewingHandler(this);
		this.netherToolsHandler = new ToolsHandler(this);
		this.netherArmorHandler = new ArmorHandler(this);
		this.piglinBarterHandler = new PiglinBarterHandler(this);
		this.netherSnowGolemHandler = new GolemHandler(this);

		this.requirementManagers.put(Player.class, new PlayerRequirementManager(this));
		this.requirementManagers.put(HellblockBlockState.class, new BlockRequirementManager(this));
		this.actionManagers.put(Player.class, new PlayerActionManager(this));
		this.actionManagers.put(HellblockBlockState.class, new BlockActionManager(this));

		reload();

		if (getConfigManager().metrics())
			new Metrics(this, 23739);

		if (getConfigManager().checkUpdate()) {
			VersionHelper.checkUpdate.apply(this).thenAccept(result -> {
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
			onlineUser.startSpawningFortressMobs();
			getFarmingManager().updateCrops(player.getWorld(), player);
			getIslandLevelManager().loadCache(id);
			getNetherrackGeneratorHandler().loadPistons(id);
			getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
				if (ownerUUID == null)
					return;
				getCoopManager().trackBannedPlayer(ownerUUID, id).thenAccept((status) -> {
					if (status) {
						if (onlineUser.getHellblockData().hasHellblock()) {
							if (onlineUser.getHellblockData().getOwnerUUID() == null) {
								throw new NullPointerException(
										"Owner reference returned null, please report this to the developer.");
							}
							getStorageManager().getOfflineUserData(onlineUser.getHellblockData().getOwnerUUID(),
									getConfigManager().lockData()).thenAccept((owner) -> {
										if (owner.isEmpty())
											return;
										UserData bannedOwner = owner.get();
										getCoopManager().makeHomeLocationSafe(bannedOwner, onlineUser);
									});
						} else {
							getHellblockHandler().teleportToSpawn(player, true);
						}
					}
				});
			});
		}

		int purgeDays = getConfigManager().abandonAfterDays();
		if (purgeDays > 0) {
			AtomicInteger purgeCount = new AtomicInteger(0);
			for (UUID id : getStorageManager().getDataSource().getUniqueUsers()) {
				if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
					continue;

				OfflinePlayer player = Bukkit.getOfflinePlayer(id);
				if (player.getLastPlayed() == 0)
					continue;
				long millisSinceLastLogin = (System.currentTimeMillis() - player.getLastPlayed()) -
				// Account for a timezone difference
						TimeUnit.MILLISECONDS.toHours(19);
				if (millisSinceLastLogin > TimeUnit.DAYS.toMillis(purgeDays)) {
					getStorageManager().getOfflineUserData(id, getConfigManager().lockData()).thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						if (offlineUser.getHellblockData().hasHellblock()
								&& offlineUser.getHellblockData().getOwnerUUID() != null) {
							if (id.equals(offlineUser.getHellblockData().getOwnerUUID())) {
								float level = offlineUser.getHellblockData().getLevel();
								if (level == HellblockData.DEFAULT_LEVEL) {
									Optional<HellblockWorld<?>> world = getWorldManager().getWorld(getWorldManager()
											.getHellblockWorldFormat(offlineUser.getHellblockData().getID()));
									if (world.isEmpty() || world.get() == null)
										throw new NullPointerException(
												"World returned null, please try to regenerate the world before reporting this issue.");
									World bukkitWorld = world.get().bukkitWorld();
									offlineUser.getHellblockData().setAsAbandoned(true);
									getProtectionManager().getIslandProtection().updateHellblockMessages(bukkitWorld,
											offlineUser.getHellblockData().getOwnerUUID());
									getProtectionManager().getIslandProtection().abandonIsland(bukkitWorld,
											offlineUser.getHellblockData().getOwnerUUID());
									purgeCount.getAndIncrement();
								}
							}
						}
					});
				}
			}
			if (purgeCount.get() > 0)
				getPluginLogger()
						.info(String.format("A total of %s hellblocks have been set as abandoned.", purgeCount.get()));
		}
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
				onlineUser.stopSpawningFortressMobs();
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
			this.lavaRainHandler.disable();
		if (this.playerListener != null)
			this.playerListener.disable();
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
		if (this.biomeGUIManager != null)
			this.biomeGUIManager.disable();
		if (this.flagsGUIManager != null)
			this.flagsGUIManager.disable();
		if (this.partyGUIManager != null)
			this.partyGUIManager.disable();
		if (this.inviteGUIManager != null)
			this.inviteGUIManager.disable();
		if (this.challengesGUIManager != null)
			this.challengesGUIManager.disable();
		if (this.islandChoiceGUIManager != null)
			this.islandChoiceGUIManager.disable();
		if (this.schematicGUIManager != null)
			this.schematicGUIManager.disable();
		if (this.resetConfirmGUIManager != null)
			this.resetConfirmGUIManager.disable();
		if (this.entityManager != null)
			this.entityManager.disable();
		if (this.integrationManager != null)
			this.integrationManager.disable();
		if (this.storageManager != null)
			this.storageManager.disable();
		if (this.worldManager != null)
			this.worldManager.disable();
		if (this.placeholderManager != null)
			this.placeholderManager.disable();
		if (this.hookManager != null)
			this.hookManager.disable();
		if (this.cooldownManager != null)
			this.cooldownManager.disable();
		if (this.commandManager != null) {
			if (!Bukkit.getServer().isStopping()) {
				this.commandManager.unregisterFeatures();
			}
		}
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
		this.isReloading = true;
		this.lavaRainHandler.reload();
		this.configManager.reload();
		this.debugger = getConfigManager().debug() ? (s) -> pluginLogger.info("[DEBUG] " + s.get()) : (s) -> {
		};
		this.actionManagers.values().forEach(Reloadable::reload);
		this.requirementManagers.values().forEach(Reloadable::reload);
		this.playerListener.reload();
		this.challengeManager.reload();
		this.netherToolsHandler.reload();
		this.netherArmorHandler.reload();
		this.netherBrewingHandler.reload();
		this.farmingManager.reload();
		this.statisticsManager.reload();
		this.itemManager.reload();
		this.lootManager.reload();
		this.cooldownManager.reload();
		this.fishingManager.reload();
		this.effectManager.reload();
		this.marketManager.reload();
		this.hellblockGUIManager.reload();
		this.biomeGUIManager.reload();
		this.flagsGUIManager.reload();
		this.partyGUIManager.reload();
		this.inviteGUIManager.reload();
		this.challengesGUIManager.reload();
		this.islandChoiceGUIManager.reload();
		this.schematicGUIManager.reload();
		this.resetConfirmGUIManager.reload();
		this.blockManager.reload();
		this.entityManager.reload();
		this.storageManager.reload();
		this.hookManager.reload();
		this.hologramManager.reload();
		this.schematicManager.reload();
		this.worldManager.reload();
		this.protectionManager.reload();
		this.translationManager.reload();
		EventUtils.fireAndForget(new HellblockReloadEvent(this));
		this.isReloading = false;
	}

	public static HellblockPlugin getInstance() {
		if (instance == null) {
			throw new IllegalArgumentException("Plugin not initialized");
		}
		return instance;
	}

	@NotNull
	public ConfigManager getConfigManager() {
		return this.configManager;
	}

	@NotNull
	public TranslationManager getTranslationManager() {
		return this.translationManager;
	}

	@NotNull
	public StorageManager getStorageManager() {
		return this.storageManager;
	}

	@NotNull
	public SchematicManager getSchematicManager() {
		return this.schematicManager;
	}

	@NotNull
	public WorldManager getWorldManager() {
		return this.worldManager;
	}

	@NotNull
	public DependencyManager getDependencyManager() {
		return this.dependencyManager;
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

	@NotNull
	public FishingManager getFishingManager() {
		return this.fishingManager;
	}

	@NotNull
	public HookManager getHookManager() {
		return this.hookManager;
	}

	@NotNull
	public LootManager getLootManager() {
		return this.lootManager;
	}

	@NotNull
	public EffectManager getEffectManager() {
		return this.effectManager;
	}

	@NotNull
	public ItemManager getItemManager() {
		return this.itemManager;
	}

	@NotNull
	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	@NotNull
	public BlockManager getBlockManager() {
		return this.blockManager;
	}

	@NotNull
	public BukkitCommandManager getCommandManager() {
		return this.commandManager;
	}

	@NotNull
	public PlaceholderManager getPlaceholderManager() {
		return this.placeholderManager;
	}

	@NotNull
	public IntegrationManager getIntegrationManager() {
		return this.integrationManager;
	}

	@NotNull
	public MarketManager getMarketManager() {
		return this.marketManager;
	}

	@NotNull
	public StatisticsManager getStatisticsManager() {
		return this.statisticsManager;
	}

	@NotNull
	public EventManager getEventManager() {
		return this.eventManager;
	}

	@NotNull
	public HologramManager getHologramManager() {
		return this.hologramManager;
	}

	@NotNull
	public CoolDownManager getCooldownManager() {
		return this.cooldownManager;
	}

	@NotNull
	public HellblockGUIManager getHellblockGUIManager() {
		return this.hellblockGUIManager;
	}

	@NotNull
	public BiomeGUIManager getBiomeGUIManager() {
		return this.biomeGUIManager;
	}

	@NotNull
	public FlagsGUIManager getFlagsGUIManager() {
		return this.flagsGUIManager;
	}

	@NotNull
	public PartyGUIManager getPartyGUIManager() {
		return this.partyGUIManager;
	}

	@NotNull
	public InviteGUIManager getInviteGUIManager() {
		return this.inviteGUIManager;
	}

	@NotNull
	public ChallengesGUIManager getChallengesGUIManager() {
		return this.challengesGUIManager;
	}

	@NotNull
	public IslandChoiceGUIManager getIslandChoiceGUIManager() {
		return this.islandChoiceGUIManager;
	}

	@NotNull
	public SchematicGUIManager getSchematicGUIManager() {
		return this.schematicGUIManager;
	}

	@NotNull
	public ResetConfirmGUIManager getResetConfirmGUIManager() {
		return this.resetConfirmGUIManager;
	}

	@NotNull
	public CoopManager getCoopManager() {
		return this.coopManager;
	}

	@NotNull
	public IslandGenerator getIslandGenerator() {
		return this.islandGenerator;
	}

	@NotNull
	public LevelHandler getIslandLevelManager() {
		return this.islandLevelManager;
	}

	@NotNull
	public IslandChoiceConverter getIslandChoiceConverter() {
		return this.islandChoiceConverter;
	}

	@NotNull
	public GlowTreeHandler getGlowstoneTreeHandler() {
		return this.glowstoneTreeHandler;
	}

	@NotNull
	public InfiniteLavaHandler getInfiniteLavaHandler() {
		return this.infiniteLavaHandler;
	}

	@NotNull
	public NetherGeneratorHandler getNetherrackGeneratorHandler() {
		return this.netherrackGeneratorHandler;
	}

	@NotNull
	public RainHandler getLavaRainHandler() {
		return this.lavaRainHandler;
	}

	@NotNull
	public PiglinBarterHandler getPiglinBarterHandler() {
		return this.piglinBarterHandler;
	}

	@NotNull
	public WitherHandler getWitherBossHandler() {
		return this.witherBossHandler;
	}

	@NotNull
	public BrewingHandler getNetherBrewingHandler() {
		return this.netherBrewingHandler;
	}

	@NotNull
	public FarmingHandler getFarmingManager() {
		return this.farmingManager;
	}

	@NotNull
	public ArmorHandler getNetherArmorHandler() {
		return this.netherArmorHandler;
	}

	@NotNull
	public ToolsHandler getNetherToolsHandler() {
		return this.netherToolsHandler;
	}

	@NotNull
	public ChallengeManager getChallengeManager() {
		return this.challengeManager;
	}

	@NotNull
	public ProtectionManager getProtectionManager() {
		return this.protectionManager;
	}

	@NotNull
	public HellblockHandler getHellblockHandler() {
		return this.hellblockHandler;
	}

	@NotNull
	public BiomeHandler getBiomeHandler() {
		return this.biomeHandler;
	}

	@NotNull
	public GolemHandler getNetherSnowGolemHandler() {
		return this.netherSnowGolemHandler;
	}

	@NotNull
	public PlayerListener getPlayerListener() {
		return this.playerListener;
	}

	@NotNull
	public BukkitSchedulerAdapter getScheduler() {
		return this.scheduler;
	}

	@NotNull
	public SenderFactory<HellblockPlugin, CommandSender> getSenderFactory() {
		return this.senderFactory;
	}

	@NotNull
	public PluginLogger getPluginLogger() {
		return this.pluginLogger;
	}

	@NotNull
	public Consumer<Supplier<String>> getDebugger() {
		return this.debugger;
	}

	public boolean isUpdateAvailable() {
		return this.updateAvailable;
	}

	public boolean isReloading() {
		return this.isReloading;
	}

	public @NotNull String getFormattedCooldown(long seconds) {
		long hours = (seconds - seconds % 3600) / 3600;
		long minutes = (seconds % 3600 - seconds % 3600 % 60) / 60;
		seconds = seconds % 3600 % 60;
		String hourFormat = getTranslationManager().miniMessageTranslation(MessageConstants.FORMAT_HOUR.build().key());
		String minuteFormat = getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_MINUTE.build().key());
		String secondFormat = getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_SECOND.build().key());
		StringBuilder formattedTime = new StringBuilder();
		if (hours > 0)
			formattedTime.append(hours + hourFormat);
		if (minutes > 0)
			formattedTime.append(minutes + minuteFormat);
		if (seconds > 0)
			formattedTime.append(seconds + secondFormat);
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
	public void debug(Object message) {
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