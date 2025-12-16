package com.swiftlicious.hellblock.world;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.infernalsuite.asp.api.world.SlimeWorld;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.ConfigManager;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandPlacementDetector;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.FileUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.world.adapter.BukkitWorldAdapter;
import com.swiftlicious.hellblock.world.adapter.SlimeWorldAdapter;
import com.swiftlicious.hellblock.world.adapter.WorldAdapter;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Handles the lifecycle, management, and configuration of plugin-controlled
 * worlds for the Hellblock plugin.
 *
 * <p>
 * This class is the central authority for interacting with all "Hellblock"
 * worlds—either shared or per-player— and supports multiple backend
 * implementations via {@link WorldAdapter}s (e.g., Bukkit, SlimeWorldManager).
 *
 * <p>
 * Main responsibilities include:
 * <ul>
 * <li>Loading and unloading worlds based on configuration and usage</li>
 * <li>Tracking world access times for idle unloading and long-term purging</li>
 * <li>Adapting Bukkit worlds into {@link HellblockWorld} wrappers</li>
 * <li>Dispatching tick-related and chunk-related operations for managed
 * worlds</li>
 * <li>Handling player events to track world usage and persistence</li>
 * <li>Supporting dynamic loading from disk or memory through the configured
 * adapter</li>
 * <li>Applying world-specific settings parsed from YAML configuration</li>
 * </ul>
 *
 * <p>
 * It supports automatic discovery of worlds from disk, scheduled cleanup of
 * unused or abandoned worlds, and runtime unloading of idle worlds to minimize
 * resource usage.
 *
 * <p>
 * Integrates tightly with Bukkit's world and chunk events to ensure accurate
 * bookkeeping and custom mechanics. Also compatible with async world systems
 * like SlimeWorldManager or AdvancedSlimePaper, if present.
 *
 * <p>
 * This manager implements {@link WorldManagerInterface} and is registered as a
 * Bukkit event listener. It should be loaded early during plugin startup to
 * ensure world access is ready for gameplay logic.
 *
 * <p>
 * World names are filtered and validated based on the {@link MatchRule} and
 * regex/world list from config.
 *
 * @see WorldAdapter
 * @see HellblockWorld
 * @see WorldSetting
 * @see MatchRule
 */
public class WorldManager implements WorldManagerInterface, Listener {

	protected final HellblockPlugin instance;
	private final TreeSet<WorldAdapter<?>> adapters = new TreeSet<>();
	private final ConcurrentMap<String, HellblockWorld<?>> worlds = new ConcurrentHashMap<>();
	private final Map<String, WorldSetting> worldSettings = new HashMap<>();
	private WorldAdapter<?> adapter;
	private WorldSetting defaultWorldSetting;
	private MatchRule matchRule;
	private Set<String> worldList;

	private SchedulerTask idleTask = null;

	private Listener entitiesLoadListener;

	private CompletableFuture<Void> loadComplete = new CompletableFuture<>();

	public static final String WORLD_PREFIX = "hellblock_world_";

	public static final int CURRENT_WORLD_VERSION = 1;

	private Set<Pattern> compiledPatterns;

	public WorldManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		if (loadComplete.isDone()) {
			// reset the future if reloading again
			this.loadComplete = new CompletableFuture<>();
		}

		this.setupWorldAdapter();
		// Delay loadWorldsFromConfig until after shared world is created
		if (!instance.getConfigManager().perPlayerWorlds()) {
			final String sharedWorldName = instance.getConfigManager().worldName();
			WorldAdapter<World> bukkitAdapter = getAdapterByType(BukkitWorldAdapter.class);
			bukkitAdapter.createWorld(sharedWorldName).thenAccept(ignored -> {
				// now run loading logic after world exists
				this.loadWorldsFromConfig();
				this.finalizeWorldLoading(); // optional cleanup / debug logging
				loadComplete.complete(null); // Mark world manager as loaded
			}).exceptionally(ex -> {
				instance.getPluginLogger().severe("Failed to create world: " + ex.getMessage(), ex);
				loadComplete.completeExceptionally(ex);
				return null;
			});
		} else {
			// Per-player mode doesn't need world pre-creation
			this.loadWorldsFromConfig();
			this.finalizeWorldLoading();
			loadComplete.complete(null); // Mark world manager as loaded
		}
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.worldSettings.clear();
		if (this.idleTask != null && !this.idleTask.isCancelled()) {
			this.idleTask.cancel();
			this.idleTask = null;
		}
		if (entitiesLoadListener != null) {
			HandlerList.unregisterAll(entitiesLoadListener);
			entitiesLoadListener = null;
		}
	}

	@Override
	public void disable() {
		unload();
		if (!worlds.isEmpty()) {
			instance.debug("Saving " + worlds.size() + " hellblock world" + (worlds.size() == 1 ? "" : "s") + ".");
		}
		// Remove plugin chunk tickets (only for our worlds)
		if (VersionHelper.isPaperFork()) {
			for (HellblockWorld<?> hellblockWorld : worlds.values()) {
				World world = hellblockWorld.bukkitWorld();
				if (world != null) {
					try {
						Method removeTickets = World.class.getMethod("removePluginChunkTickets", Plugin.class);
						removeTickets.invoke(world, instance);
						instance.debug(() -> "Cleared plugin chunk tickets for world: " + world.getName());
					} catch (Exception e) {
						instance.getPluginLogger().warn("Failed to clear chunk tickets for " + world.getName(), e);
					}
				}
			}
		}

		// Unload only plugin-managed worlds
		if (!worlds.isEmpty())
			instance.debug(() -> "Unloaded all hellblock worlds");
		for (HellblockWorld<?> hellblockWorld : worlds.values()) {
			World world = hellblockWorld.bukkitWorld();
			if (world != null) {
				instance.debug(() -> "Unloading " + world.getName());
				unloadWorld(world, true);
				instance.debug(() -> "Unloaded " + world.getName());
			}
		}
		worlds.clear();

		if (!this.adapters.isEmpty())
			instance.debug(() -> "Unloaded hellblock world adapters");
		// Unregister any listener adapters
		for (WorldAdapter<?> adapter : this.adapters) {
			if (adapter instanceof Listener listener) {
				HandlerList.unregisterAll(listener);
			}
		}
	}

	@Override
	public void reloadWorlds() {
		// load and unload worlds
		for (HellblockWorld<?> wrapper : worlds.values()) {
			World bukkitWorld = wrapper.bukkitWorld();
			if (bukkitWorld != null && isMechanicEnabled(bukkitWorld)) {
				loadWorld(bukkitWorld);
			} else {
				unloadWorld(bukkitWorld, false);
			}
		}
	}

	public CompletableFuture<Void> getWorldLoadingCompletion() {
		return loadComplete;
	}

	private void finalizeWorldLoading() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		this.unloadMissingWorldFolders();
		this.purgeOldUnusedWorlds();

		this.idleTask = instance.getScheduler().sync().runRepeating(() -> {
			try {
				tryUnloadIdleWorlds();
			} catch (Exception e) {
				instance.getPluginLogger().warn("Error while unloading idle worlds: " + e.getMessage());
			}
		}, TimeUnit.MINUTES.toMinutes(5), TimeUnit.MINUTES.toMinutes(5), LocationUtils.getAnyLocationInstance());

		if (worlds.isEmpty()) {
			instance.debug("WorldManager loaded with no worlds.");
		} else {
			instance.debug(
					"WorldManager loaded with " + worlds.size() + " world" + (worlds.size() == 1 ? "" : "s") + ".");
		}
		registerEntitiesLoadListener();
		reapplyFortressBiomes();
	}

	/**
	 * Reapplies fake fortress structures for any islands whose biome is
	 * NETHER_FORTRESS. This ensures fortress data persists after restarts or
	 * reloads.
	 */
	private void reapplyFortressBiomes() {
		instance.getCoopManager().getCachedIslandOwnerData().thenAccept(owners -> {
			for (UserData owner : owners) {
				HellblockData data = owner.getHellblockData();
				if (!data.hasHellblock() || data.getBiome() == null || data.getBiome() != HellBiome.NETHER_FORTRESS)
					continue;

				String worldName = getHellblockWorldFormat(data.getIslandId());
				Optional<HellblockWorld<?>> worldOpt = getWorld(worldName);
				if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null)
					continue;

				World world = worldOpt.get().bukkitWorld();
				BoundingBox bounds = data.getBoundingBox();
				if (bounds == null)
					continue;

				VersionHelper.getNMSManager().injectFakeFortress(world, bounds);

				instance.debug("[FortressInjector] Reapplied fortress for " + worldName);
			}
		});
	}

	private void registerEntitiesLoadListener() {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Event> typedEventClass = (Class<? extends Event>) Class
					.forName("org.bukkit.event.world.EntitiesLoadEvent");
			Method getChunkMethod = typedEventClass.getMethod("getChunk");
			Method getWorldMethod = typedEventClass.getMethod("getWorld");

			this.entitiesLoadListener = new Listener() {
			}; // Still needed for context

			EventExecutor executor = (listenerInstance, event) -> {
				if (!typedEventClass.isInstance(event))
					return;

				try {
					Chunk chunk = (Chunk) getChunkMethod.invoke(event);
					World world = (World) getWorldMethod.invoke(event);

					getWorld(world).filter(customWorld -> customWorld.setting().offlineTick()).ifPresent(
							customWorld -> notifyOfflineUpdates(customWorld, ChunkPos.fromBukkitChunk(chunk)));

				} catch (Throwable t) {
					t.printStackTrace();
				}
			};

			Bukkit.getPluginManager().registerEvent(typedEventClass, this.entitiesLoadListener, EventPriority.NORMAL,
					executor, instance, true // ignoreCancelled
			);

		} catch (ClassNotFoundException ignored) {
			// EntitiesLoadEvent not available in this version — skip
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Initializes and registers the appropriate {@link WorldAdapter}
	 * implementations based on plugin configuration and available dependencies.
	 *
	 * <p>
	 * If per-player worlds are enabled in the configuration, this method attempts
	 * to detect and register compatible SlimeWorldManager or
	 * AdvancedSlimePaper-based adapters. If the required classes or plugins are not
	 * present, they are skipped silently.
	 *
	 * <p>
	 * Regardless of configuration, the {@link BukkitWorldAdapter} is always
	 * registered as a fallback or for use in shared world mode.
	 *
	 * <p>
	 * Adapters are also registered as event listeners where applicable.
	 */
	private void setupWorldAdapter() {
		if (instance.getConfigManager().perPlayerWorlds()) {
			try {
				Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
				final SlimeWorldAdapter adapter = new SlimeWorldAdapter(instance, 1);
				adapters.add(adapter);
				Bukkit.getPluginManager().registerEvents(adapter, instance);
				instance.getIntegrationManager().isHooked("SlimeWorldManager");
			} catch (ClassNotFoundException ignored) {
			}
			if (instance.isHookedPluginEnabled("SlimeWorldPlugin")) {
				final SlimeWorldAdapter adapter = new SlimeWorldAdapter(instance, 2);
				adapters.add(adapter);
				Bukkit.getPluginManager().registerEvents(adapter, instance);
				instance.getIntegrationManager().isHooked("AdvancedSlimePaper");
			}
		}
		this.adapters.add(new BukkitWorldAdapter(instance));
	}

	/**
	 * Loads hellblock world configuration from the main plugin configuration file.
	 *
	 * <p>
	 * Sets the active {@link WorldAdapter} based on whether per-player worlds are
	 * enabled and what adapters were registered. If no adapter is found, logs a
	 * severe error and halts world loading.
	 *
	 * <p>
	 * Reads and parses world inclusion/exclusion rules from the
	 * {@code general.worlds} section, using a match rule ("blacklist" or
	 * "whitelist") and a list of regex patterns for world names.
	 *
	 * <p>
	 * Also loads default and per-world settings from
	 * {@code general.worlds.settings}, and stores them for later use in world
	 * handling.
	 *
	 * <p>
	 * After configuration is processed, attempts to discover and load all matching
	 * hellblock worlds using the selected world adapter.
	 */
	private void loadWorldsFromConfig() {
		final YamlDocument config = instance.getConfigManager().getMainConfig();

		final Section section = config.getSection("general.worlds");
		if (section == null) {
			instance.getPluginLogger().warn("general.worlds section should not be null");
			return;
		}

		if (instance.getConfigManager().perPlayerWorlds()) {
			for (WorldAdapter<?> adapter : this.adapters) {
				if (adapter instanceof SlimeWorldAdapter slime) {
					this.adapter = slime;
					instance.debug("Registered SlimeWorldAdapter for hellblock worlds (per player worlds enabled)");
					break;
				}
			}
		} else {
			for (WorldAdapter<?> adapter : this.adapters) {
				if (adapter instanceof BukkitWorldAdapter bukkit) {
					this.adapter = bukkit;
					instance.debug("Registered BukkitWorldAdapter for the hellblock world (shared world enabled)");
					break;
				}
			}
		}

		if (adapter() == null) {
			instance.getPluginLogger().severe("No world adapter was set — skipping world loading.");
			instance.getPluginLogger().severe("Available adapters: "
					+ adapters.stream().map(Object::getClass).map(Class::getSimpleName).toList());
			return;
		}

		this.matchRule = MatchRule.valueOf(section.getString("mode", "blacklist").toUpperCase(Locale.ENGLISH));
		this.worldList = new HashSet<>(section.getStringList("list"));

		this.compiledPatterns = worldList.stream().map(Pattern::compile).collect(Collectors.toSet());

		final Section settingSection = section.getSection("settings");
		if (settingSection == null) {
			instance.getPluginLogger().warn("general.worlds.settings section should not be null");
			return;
		}

		final Section defaultSection = settingSection.getSection("_DEFAULT_");
		if (defaultSection == null) {
			instance.getPluginLogger().warn("general.worlds.settings._DEFAULT_ section should not be null");
			return;
		}

		this.defaultWorldSetting = sectionToWorldSetting(defaultSection);

		final Section worldSection = settingSection.getSection("_WORLDS_");
		if (worldSection != null) {
			worldSection.getStringRouteMappedValues(false).entrySet().stream()
					.filter(entry -> entry.getValue() instanceof Section).forEach(entry -> {
						final Section inner = (Section) entry.getValue();
						this.worldSettings.put(entry.getKey(), sectionToWorldSetting(inner));
					});
		}

		List<String> discoveredWorlds = discoverHellblockWorlds();
		discoveredWorlds.forEach(this::loadOrAdaptWorld);
		if (worlds.isEmpty()) {
			instance.debug("No hellblock worlds discovered to load.");
		} else {
			instance.debug("Discovered and attempted to load " + worlds.size() + " hellblock world"
					+ (worlds.size() == 1 ? "" : "s") + ".");
		}
	}

	/**
	 * Returns a list of Bukkit {@link World} instances that are currently managed
	 * by the plugin.
	 * <p>
	 * This excludes vanilla worlds (e.g., {@code world}, {@code world_nether}) and
	 * any other unrelated worlds not loaded or tracked by the plugin's
	 * {@link WorldManager}.
	 *
	 * @return a list of plugin-owned, loaded Bukkit worlds
	 */
	public List<World> getManagedBukkitWorlds() {
		return new ArrayList<>(
				worlds.values().stream().map(HellblockWorld::bukkitWorld).filter(Objects::nonNull).toList());
	}

	/**
	 * Returns a snapshot of all plugin-managed Bukkit {@link World} instances that
	 * are currently loaded on the server.
	 * <p>
	 * This excludes worlds that have been unloaded but still exist in memory in the
	 * world cache.
	 *
	 * @return a thread-safe list of currently loaded Bukkit worlds managed by the
	 *         plugin
	 */
	public List<World> getLoadedManagedBukkitWorlds() {
		return new ArrayList<>(worlds.values().stream().map(HellblockWorld::bukkitWorld).filter(Objects::nonNull)
				.filter(world -> Bukkit.getWorld(world.getName()) != null).toList());
	}

	/**
	 * Returns a snapshot of all currently loaded plugin-managed worlds, including
	 * both their Bukkit {@link World} instances and their {@link HellblockWorld}
	 * wrappers.
	 * <p>
	 * This method is thread-safe and returns a new list snapshot for async-safe
	 * iteration.
	 *
	 * @return a list of pairs containing the plugin's managed
	 *         {@link HellblockWorld} wrappers and their corresponding Bukkit
	 *         {@link World} objects
	 */
	public List<Pair<HellblockWorld<?>, World>> getLoadedManagedWorldPairs() {
		List<Pair<HellblockWorld<?>, World>> result = new ArrayList<>();
		for (HellblockWorld<?> wrapper : worlds.values()) {
			World bukkitWorld = wrapper.bukkitWorld();
			if (bukkitWorld != null) {
				result.add(Pair.of(wrapper, bukkitWorld));
			}
		}
		return result;
	}

	/**
	 * Returns a collection of all plugin-managed {@link HellblockWorld} instances.
	 *
	 * @return all active plugin world wrappers
	 */
	public Collection<HellblockWorld<?>> getManagedWorlds() {
		return Collections.unmodifiableCollection(worlds.values());
	}

	/**
	 * Discovers all plugin-managed world folders stored in the server's world
	 * container.
	 * <p>
	 * If per-player worlds are enabled, this scans the root world directory for
	 * folders beginning with {@link #WORLD_PREFIX} (e.g.,
	 * {@code hellblock_world_42}) and containing a valid {@code level.dat} file.
	 * Malformed or incomplete folders are logged and skipped.
	 * <p>
	 * If per-player worlds are disabled, this simply returns the shared world name
	 * defined in the plugin configuration.
	 *
	 * @return a list of valid plugin world names detected on disk
	 */
	private List<String> discoverHellblockWorlds() {
		if (instance.getConfigManager().perPlayerWorlds()) {
			// Scan for player-created worlds
			File[] files = Bukkit.getWorldContainer().listFiles();
			if (files == null)
				return List.of();

			return Arrays.stream(files).filter(File::isDirectory).filter(this::isValidWorldFolder).map(File::getName)
					.filter(name -> name.startsWith(WORLD_PREFIX)).peek(name -> {
						if (!name.matches(WORLD_PREFIX + "\\d+")) {
							instance.debug("Skipping malformed plugin world folder: " + name);
						}
					}).toList();
		} else {
			// Single static world from config
			return List.of(instance.getConfigManager().worldName());
		}
	}

	/**
	 * Unloads and removes references to any plugin-managed worlds whose
	 * corresponding folders no longer exist on disk.
	 * <p>
	 * This ensures the internal world cache ({@link #worlds}) and access tracking
	 * map remain synchronized with the actual file system. Useful in cases where an
	 * admin manually deletes a world folder while the server is offline.
	 */
	private void unloadMissingWorldFolders() {
		List<String> toRemove = new ArrayList<>();

		worlds.keySet().forEach(worldName -> {
			File folder = new File(Bukkit.getWorldContainer(), worldName);
			if (!folder.exists()) {
				World world = Bukkit.getWorld(worldName);
				unloadWorld(world, false);
				toRemove.add(worldName);
				instance.debug("Unloaded and removed stale reference to missing world: " + worldName);
			}
		});

		toRemove.forEach(worlds::remove);
	}

	/**
	 * Purges old or inactive plugin worlds that have not been accessed for a
	 * configured amount of time (currently 30 days).
	 * <p>
	 * This method checks the {@link #lastAccess} timestamps and queries the
	 * associated {@link HellblockData} for each world to determine if it is
	 * abandoned. If so, the world is unloaded, its folder is deleted, and
	 * references are removed from memory.
	 * <p>
	 * This feature only applies when per-player worlds are enabled.
	 */
	private void purgeOldUnusedWorlds() {
		if (!instance.getConfigManager().perPlayerWorlds()) {
			return;
		}

		long now = System.currentTimeMillis();
		long threshold = TimeUnit.DAYS.toMillis(instance.getConfigManager().abandonAfterDays()); // 30 days

		for (String worldName : worlds.keySet()) {
			Optional<UserData> ownerData = instance.getCoopManager().getCachedIslandOwnerDataNow(worldName);
			if (ownerData.isEmpty())
				continue;

			HellblockData islandData = ownerData.get().getHellblockData();
			long lastUsed = islandData.getLastWorldAccess();
			if (lastUsed == 0L)
				continue;

			if ((now - lastUsed) > threshold && islandData.isAbandoned()) {
				try {
					World world = Bukkit.getWorld(worldName);
					unloadWorld(world, true);
					File folder = new File(Bukkit.getWorldContainer(), worldName);
					FileUtils.deleteDirectory(folder.toPath());

					worlds.remove(worldName);
					instance.getPluginLogger().info("Purged unused world: " + worldName);
				} catch (IOException e) {
					instance.getPluginLogger().warn("Failed to delete world folder for " + worldName, e);
				}
			}
		}
	}

	/**
	 * Loads or adapts a plugin-managed world into the server.
	 * <p>
	 * If the world is already loaded by Bukkit, it is verified via
	 * {@link #isMechanicEnabled(World)} and passed to {@link #loadWorld(World)}.
	 * Otherwise, the current world adapter is used to attempt asynchronous loading
	 * from disk (e.g., SlimeWorldManager or BukkitWorldAdapter).
	 * <p>
	 * Successfully loaded worlds are registered into the internal {@link #worlds}
	 * cache and their access time is recorded via {@link #markWorldAccess(String)}.
	 *
	 * @param worldName the name of the plugin-managed world
	 */
	private void loadOrAdaptWorld(@NotNull String worldName) {
		World bukkitWorld = Bukkit.getWorld(worldName);
		if (bukkitWorld != null && isMechanicEnabled(bukkitWorld)) {
			loadWorld(bukkitWorld);
		} else {
			// Attempt to load via adapter if available
			adapter().getOrLoadIslandWorld(worldName).thenAccept(loaded -> {
				if (loaded != null && isMechanicEnabled(loaded.bukkitWorld())) {
					markWorldAccess(worldName);
					WorldSetting setting = Optional.ofNullable(worldSettings.get(worldName))
							.orElse(defaultWorldSetting);
					loaded.setting(setting);
					worlds.put(worldName, loaded);
					instance.debug("Loaded world via adapter: " + worldName);
				} else if (bukkitWorld != null) {
					unloadWorld(bukkitWorld, false);
				}
			}).exceptionally(ex -> {
				instance.getPluginLogger().warn("World " + worldName + " failed to load via loadOrAdaptWorld.", ex);
				return null;
			});
		}
	}

	/**
	 * Generates the formatted world name used internally by the plugin based on the
	 * given island ID.
	 * <p>
	 * When per-player worlds are enabled, this returns a name such as
	 * {@code hellblock_world_<id>}. Otherwise, it returns the shared world name
	 * from the plugin configuration.
	 *
	 * @param islandId the island ID
	 * @return the formatted world name corresponding to the island
	 */
	@NotNull
	public String getHellblockWorldFormat(int islandId) {
		if (instance.getConfigManager().perPlayerWorlds()) {
			return WORLD_PREFIX + islandId;
		} else {
			return instance.getConfigManager().worldName();
		}
	}

	/**
	 * Marks the given plugin-managed world as recently accessed by updating the
	 * associated island's {@link HellblockData} with the current timestamp.
	 *
	 * <p>
	 * This is used to track world activity for idle-unload and purging logic. It
	 * should be called whenever a player enters or interacts with a world.
	 *
	 * @param worldName the name of the plugin-managed world to update
	 */
	public void markWorldAccess(@NotNull String worldName) {
		instance.getCoopManager().getCachedIslandOwnerDataNow(worldName)
				.ifPresent(data -> data.getHellblockData().updateLastWorldAccess());
	}

	/**
	 * Checks whether a given folder represents a valid world directory.
	 * <p>
	 * This validation currently checks only for the presence of a {@code level.dat}
	 * file inside the folder.
	 *
	 * @param worldFolder the folder to validate
	 * @return {@code true} if the folder contains a {@code level.dat} file,
	 *         indicating a valid world directory; otherwise {@code false}
	 */
	private boolean isValidWorldFolder(@NotNull File worldFolder) {
		return new File(worldFolder, "level.dat").exists();
	}

	/**
	 * Attempts to unload plugin-managed worlds that have been idle for more than a
	 * configured amount of time (currently 10 minutes) and have no online players.
	 * <p>
	 * This helps reduce memory usage by unloading inactive per-player worlds while
	 * keeping track of their last access times in {@link #lastAccess}.
	 */
	private void tryUnloadIdleWorlds() {
		if (!instance.getConfigManager().perPlayerWorlds()) {
			return;
		}

		long now = System.currentTimeMillis();
		for (String worldName : worlds.keySet()) {
			Optional<UserData> ownerData = instance.getCoopManager().getCachedIslandOwnerDataNow(worldName);
			if (ownerData.isEmpty())
				continue;

			HellblockData islandData = ownerData.get().getHellblockData();
			long lastUsed = islandData.getLastWorldAccess();

			if ((now - lastUsed) > TimeUnit.MINUTES.toMillis(10)) {
				World world = Bukkit.getWorld(worldName);
				if (world != null && world.getPlayers().isEmpty()) {
					unloadWorld(world, false);
					boolean unloaded = Bukkit.unloadWorld(world, true);
					if (unloaded) {
						instance.debug("Unloaded idle world: " + worldName);
					}
				}
			}
		}
	}

	/**
	 * Converts all islands between shared world format and per-player world format.
	 *
	 * <p>
	 * If converting to per-player format, this will extract each island from the
	 * shared world and place it into its own world. If converting to shared format,
	 * it will merge all per-player islands into a single shared world at spaced
	 * coordinates.
	 * </p>
	 *
	 * @param toPerPlayerWorlds true to convert to per-player worlds, false for
	 *                          shared world
	 * @return a {@link CompletableFuture} that completes when the conversion
	 *         finishes
	 */
	public CompletableFuture<Void> convertWorldFormat(boolean toPerPlayerWorlds) {
		return instance.getCoopManager().getCachedIslandOwnerData()
				.thenCompose(userDataList -> CompletableFuture.runAsync(() -> {
					instance.getPluginLogger().info("Starting world format conversion to "
							+ (toPerPlayerWorlds ? "per-player worlds..." : "shared world..."));

					IslandPlacementDetector placementDetector = instance.getPlacementDetector();
					ConfigManager config = instance.getConfigManager();

					// Retrieve both adapters explicitly
					WorldAdapter<SlimeWorld> slimeAdapter = getAdapterByType(SlimeWorldAdapter.class);
					WorldAdapter<World> bukkitAdapter = getAdapterByType(BukkitWorldAdapter.class);

					if (toPerPlayerWorlds) {
						// Convert from shared → per-player
						String sharedWorldName = config.worldName();
						HellblockWorld<World> sharedWorld = bukkitAdapter.getLoadedHellblockWorld(sharedWorldName);
						if (sharedWorld == null)
							return;

						for (UserData userData : userDataList) {
							HellblockData islandData = userData.getHellblockData();
							if (islandData.getHellblockLocation() == null)
								continue;

							String perPlayerWorldName = WORLD_PREFIX + islandData.getIslandId();
							HellblockWorld<?> perPlayerWorld = slimeAdapter.createWorld(perPlayerWorldName).join();

							Location newOrigin = new Location(perPlayerWorld.bukkitWorld(), 0, config.height(), 0);

							copyWorldData(sharedWorld, perPlayerWorld, newOrigin, islandData);
							instance.getPluginLogger()
									.info("Migrated island " + islandData.getIslandId() + " to per-player world.");
						}

					} else {
						// Convert from per-player → shared
						String sharedWorldName = config.worldName();
						HellblockWorld<World> sharedWorld = bukkitAdapter.createWorld(sharedWorldName).join();

						for (UserData userData : userDataList) {
							HellblockData islandData = userData.getHellblockData();

							String perPlayerWorldName = WORLD_PREFIX + islandData.getIslandId();
							HellblockWorld<SlimeWorld> perPlayerWorld = slimeAdapter
									.getLoadedHellblockWorld(perPlayerWorldName);
							if (perPlayerWorld == null)
								continue;

							Location destOrigin = placementDetector.findNextIslandLocation().join();
							islandData.setHellblockLocation(destOrigin);

							copyWorldData(perPlayerWorld, sharedWorld, destOrigin, islandData);
							instance.getPluginLogger().info("Merged " + perPlayerWorldName + " into shared world.");
						}
					}

					instance.getPluginLogger().info("World format conversion complete.");
				}));
	}

	/**
	 * Copies world data (chunks, blocks, and metadata) from one Hellblock world to
	 * another, applying a coordinate offset so that the island is placed at a new
	 * destination origin.
	 *
	 * <p>
	 * This method is used during conversion between shared-world and
	 * per-player-world formats, ensuring the island data is relocated properly
	 * within the new world.
	 * </p>
	 *
	 * @param from      The source world containing the island data
	 * @param to        The destination world to copy data into
	 * @param newOrigin The new center {@link Location} where the island should be
	 *                  placed
	 * @param data      The {@link HellblockData} that provides the original island
	 *                  center location
	 */
	private void copyWorldData(@NotNull HellblockWorld<?> from, @NotNull HellblockWorld<?> to,
			@NotNull Location newOrigin, @NotNull HellblockData data) {
		// Compute chunk and block offsets between source and target locations
		ChunkPos chunkOffset = computeOffset(data, from, newOrigin);
		BlockPos blockOffset = chunkOffset.toBlockOffset(); // Offset in block units

		for (CustomChunk chunk : from.loadedChunks()) {
			ChunkPos newChunkPos = chunk.chunkPos().add(chunkOffset);

			// Ensure the target chunk is created/initialized before adding blocks
			to.getOrCreateChunk(newChunkPos);

			// Copy block states with coordinate translation
			for (CustomSection section : chunk.sections()) {
				section.blockMap().entrySet().forEach(entry -> {
					BlockPos oldBlockPos = entry.getKey();
					CustomBlockState state = entry.getValue();

					BlockPos newBlockPos = oldBlockPos.add(blockOffset);
					to.addBlockState(Pos3.fromBlockPos(newBlockPos), state);
				});
			}
		}

		// Copy extra metadata (e.g. island flags, biome settings, etc.)
		to.extraData().copyFrom(from.extraData());

		// Save the destination world after migration
		to.save(false, false);
	}

	/**
	 * Computes the chunk offset required to translate all blocks and chunks from a
	 * source island world to a new destination origin.
	 *
	 * <p>
	 * This ensures that world data (chunks, blocks, etc.) is relocated correctly
	 * when converting between shared-world and per-player-world formats.
	 * </p>
	 *
	 * <p>
	 * The offset is calculated using the island's true center location, obtained
	 * from {@link HellblockData#getLocation()}, rather than assuming an origin.
	 * </p>
	 *
	 * @param hellblockData The island's {@link HellblockData} containing its
	 *                      current location
	 * @param source        The source {@link HellblockWorld<?>} instance
	 * @param newOrigin     The target origin {@link Location} where the island
	 *                      should be placed
	 * @return The {@link ChunkPos} offset between the source and destination
	 *         origins
	 */
	@NotNull
	private ChunkPos computeOffset(@NotNull HellblockData hellblockData, @NotNull HellblockWorld<?> source,
			@NotNull Location newOrigin) {
		World sourceWorld = source.bukkitWorld();
		if (sourceWorld == null)
			throw new IllegalStateException("Source world is not loaded for " + source.worldName());

		Location sourceCenter = hellblockData.getHellblockLocation();
		if (sourceCenter == null)
			throw new IllegalStateException("HellblockData has no center location defined for this island.");

		// Calculate block-space deltas
		int dx = newOrigin.getBlockX() - sourceCenter.getBlockX();
		int dz = newOrigin.getBlockZ() - sourceCenter.getBlockZ();

		// Convert block-space offset into chunk-space offset (divide by 16)
		return ChunkPos.of(dx >> 4, dz >> 4);
	}

	@Override
	public CompletableFuture<HellblockWorld<?>> ensureHellblockWorldLoaded(int islandId) {
		String worldName = getHellblockWorldFormat(islandId);
		HellblockWorld<?> existing = adapter().getLoadedHellblockWorld(worldName); // from cache or adapter

		if (existing != null) {
			markWorldAccess(worldName);
			return CompletableFuture.completedFuture(existing);
		}

		return adapter().createWorld(worldName).thenApply(world -> {
			markWorldAccess(worldName);
			return world;
		});
	}

	@Override
	public HellblockWorld<?> loadWorld(HellblockWorld<?> world) {
		Optional<HellblockWorld<?>> optionalWorld = getWorld(world.worldName());
		if (optionalWorld.isPresent()) {
			HellblockWorld<?> hellblockWorld = optionalWorld.get();
			hellblockWorld
					.setting(Optional.ofNullable(worldSettings.get(world.worldName())).orElse(defaultWorldSetting));
			return hellblockWorld;
		}
		world.setting(Optional.ofNullable(worldSettings.get(world.worldName())).orElse(defaultWorldSetting));
		world.setTicking(true);
		this.worlds.put(world.worldName(), world);
		for (Chunk chunk : world.bukkitWorld().getLoadedChunks()) {
			ChunkPos pos = ChunkPos.fromBukkitChunk(chunk);
			loadLoadedChunk(world, pos).thenRun(() -> notifyOfflineUpdates(world, pos));
		}
		return world;
	}

	@Override
	public HellblockWorld<?> loadWorld(World world) {
		final Optional<HellblockWorld<?>> optionalWorld = getWorld(world);
		if (optionalWorld.isPresent()) {
			final HellblockWorld<?> hellblockWorld = optionalWorld.get();
			hellblockWorld.setting(Optional.ofNullable(worldSettings.get(world.getName())).orElse(defaultWorldSetting));
			return hellblockWorld;
		}
		final HellblockWorld<?> adaptedWorld = adapt(world);
		adaptedWorld.setting(Optional.ofNullable(worldSettings.get(world.getName())).orElse(defaultWorldSetting));
		adaptedWorld.setTicking(true);
		this.worlds.put(world.getName(), adaptedWorld);
		for (Chunk chunk : world.getLoadedChunks()) {
			final ChunkPos pos = ChunkPos.fromBukkitChunk(chunk);
			loadLoadedChunk(adaptedWorld, pos).thenRun(() -> notifyOfflineUpdates(adaptedWorld, pos));
		}
		return adaptedWorld;
	}

	/**
	 * Loads a {@link CustomChunk} into the plugin's logic layer if the
	 * corresponding Bukkit chunk is already loaded.
	 *
	 * <p>
	 * This method ensures the chunk is initialized in the plugin's world system
	 * without reloading it through Bukkit, preventing duplicate operations or chunk
	 * inconsistencies.
	 *
	 * <p>
	 * <strong>Note:</strong> Ensure the Bukkit chunk is already loaded before
	 * calling this method.
	 *
	 * @param world the {@link HellblockWorld} instance containing the chunk
	 * @param pos   the position of the chunk to load
	 */
	public CompletableFuture<Void> loadLoadedChunk(HellblockWorld<?> world, ChunkPos pos) {
		if (world.isChunkLoaded(pos)) {
			return CompletableFuture.completedFuture(null);
		}

		Optional<CustomChunk> customChunk = world.getChunk(pos);
		if (customChunk.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		return customChunk.get().load(false).thenAccept(success -> {
			if (!success) {
				instance.getPluginLogger().warn("Chunk " + pos + " failed to load via loadLoadedChunk.");
			}
		});
	}

	/**
	 * Triggers any queued offline tasks for a loaded {@link CustomChunk} at the
	 * given position.
	 *
	 * <p>
	 * This is typically used after a chunk becomes active (e.g., when entities are
	 * loaded) to resume or finalize logic that was deferred while the chunk was
	 * offline.
	 *
	 * @param world the {@link HellblockWorld} instance containing the chunk
	 * @param pos   the position of the chunk to update
	 */
	public void notifyOfflineUpdates(HellblockWorld<?> world, ChunkPos pos) {
		world.getLoadedChunk(pos).ifPresent(CustomChunk::notifyOfflineTask);
	}

	@Override
	public boolean unloadWorld(World world, boolean disabling) {
		final HellblockWorld<?> removedWorld = worlds.remove(world.getName());
		if (removedWorld == null) {
			return false;
		}
		removedWorld.setTicking(false);
		instance.debug(() -> "[" + world.getName() + "] Unloading -> Saving");
		removedWorld.save(false, disabling);
		instance.debug(() -> "[" + world.getName() + "] Saving -> Shutdown");
		removedWorld.scheduler().shutdownScheduler();
		removedWorld.scheduler().shutdownExecutor();
		instance.debug(() -> "[" + world.getName() + "] Finished Shutdown");
		return true;
	}

	@EventHandler
	public void onWorldSave(WorldSaveEvent event) {
		final World world = event.getWorld();
		getWorld(world)
				.ifPresent(customWorld -> customWorld.save(instance.getConfigManager().asyncWorldSaving(), false));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onWorldLoad(WorldLoadEvent event) {
		final World world = event.getWorld();
		if (!isMechanicEnabled(world)) {
			return;
		}
		loadWorld(world);

		// After hellblock world is loaded, reapply fake fortress if needed
		instance.getCoopManager().getCachedIslandOwnerDataNow(world.getName()).ifPresent(userData -> {
			HellblockData data = userData.getHellblockData();
			if (data.getBiome() != null && data.getBiome() == HellBiome.NETHER_FORTRESS
					&& data.getBoundingBox() != null) {
				VersionHelper.getNMSManager().injectFakeFortress(world, data.getBoundingBox());
				instance.debug("[FortressInjector] Reinjected fortress for world: " + world.getName());
			}
		});
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onWorldUnload(WorldUnloadEvent event) {
		final World world = event.getWorld();
		if (!isMechanicEnabled(world)) {
			return;
		}
		unloadWorld(world, false);

		instance.getCoopManager().getCachedIslandOwnerDataNow(world.getName()).ifPresent(userData -> {
			HellblockData data = userData.getHellblockData();
			if (data.getBiome() != null && data.getBiome() == HellBiome.NETHER_FORTRESS
					&& data.getBoundingBox() != null) {
				VersionHelper.getNMSManager().removeFakeFortress(world, data.getBoundingBox());
				instance.debug("[FortressInjector] Removed fake fortress for world unload: " + world.getName());
			}
		});
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		final Chunk chunk = event.getChunk();
		final World world = event.getWorld();
		this.getWorld(world).flatMap(customWorld -> customWorld.getLoadedChunk(ChunkPos.fromBukkitChunk(chunk)))
				.ifPresent(customChunk -> customChunk.unload(true));
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		final Chunk chunk = event.getChunk();
		final World world = event.getWorld();
		this.getWorld(world).ifPresent(customWorld -> {
			final ChunkPos pos = ChunkPos.fromBukkitChunk(chunk);
			loadLoadedChunk(customWorld, pos).thenRun(() -> {
				if (ChunkUtils.isEntitiesLoaded(chunk) && customWorld.setting().offlineTick()) {
					notifyOfflineUpdates(customWorld, pos);
				}
			});
		});
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!instance.getConfigManager().perPlayerWorlds()) {
			return;
		}
		World toWorld = event.getTo().getWorld();
		if (toWorld != null) {
			markWorldAccess(toWorld.getName());
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		if (!instance.getConfigManager().perPlayerWorlds()) {
			return;
		}
		World toWorld = event.getPlayer().getWorld();
		markWorldAccess(toWorld.getName());
	}

	@Override
	public boolean isMechanicEnabled(World world) {
		if (world == null)
			return false;

		String worldName = world.getName();

		return switch (matchRule) {
		case WHITELIST -> worldList.contains(worldName);
		case BLACKLIST -> !worldList.contains(worldName);
		case REGEX -> compiledPatterns.stream().anyMatch(p -> p.matcher(worldName).matches());
		};
	}

	@Override
	public Optional<HellblockWorld<?>> getWorld(World world) {
		return getWorld(world.getName());
	}

	@Override
	public Optional<HellblockWorld<?>> getWorld(String worldName) {
		return Optional.ofNullable(worlds.get(worldName));
	}

	@Override
	public boolean isWorldLoaded(World world) {
		return worlds.containsKey(world.getName());
	}

	@Override
	public TreeSet<WorldAdapter<?>> adapters() {
		return adapters;
	}

	@Override
	public WorldAdapter<?> adapter() {
		if (this.adapter == null) {
			throw new IllegalStateException("WorldAdapter not initialized — did you forget setupWorldAdapter()?");
		}
		return adapter;
	}

	@Override
	public HellblockWorld<?> adapt(World world) {
		return adapt(world.getName());
	}

	@Override
	public HellblockWorld<?> adapt(String worldName) {
		for (WorldAdapter<?> adapter : adapters) {
			HellblockWorld<?> adapted = tryAdapt(adapter, worldName);
			if (adapted != null) {
				WorldSetting setting = Optional.ofNullable(worldSettings.get(worldName)).orElse(defaultWorldSetting);
				adapted.setting(setting);
				return adapted;
			}
		}
		throw new RuntimeException("Unable to adapt world " + worldName);
	}

	@Override
	public <T extends WorldAdapter<?>> T getAdapterByType(Class<T> type) {
		for (WorldAdapter<?> adapter : adapters) {
			if (type.isInstance(adapter)) {
				return type.cast(adapter);
			}
		}
		throw new IllegalStateException("No adapter registered for type: " + type.getSimpleName());
	}

	/**
	 * Attempts to adapt a world using the given {@link WorldAdapter}.
	 *
	 * <p>
	 * Checks if the adapter has a world loaded or accessible with the specified
	 * name. If found, it uses the adapter to wrap the world into a
	 * {@link HellblockWorld} instance.
	 *
	 * @param adapter   the {@link WorldAdapter} implementation used for adaptation
	 * @param worldName the name of the world to adapt
	 * @return the adapted {@link HellblockWorld}, or {@code null} if not available
	 */
	private <T> HellblockWorld<?> tryAdapt(WorldAdapter<T> adapter, String worldName) {
		return adapter.getWorld(worldName).map(adapter::adapt).orElse(null);
	}

	/**
	 * Defines how world names are matched against a configured list from the plugin
	 * configuration.
	 *
	 * <p>
	 * Used to determine whether a world is considered "managed" or not, based on
	 * its name.
	 */
	public enum MatchRule {

		/** World is excluded if listed (default behavior). */
		BLACKLIST,

		/** World is included only if listed. */
		WHITELIST,

		/** World is included if its name matches any regex in the list. */
		REGEX;
	}

	/**
	 * Parses a {@link Section} from the YAML configuration into a
	 * {@link WorldSetting} object.
	 *
	 * <p>
	 * This includes settings for ticking behavior and offline processing of worlds.
	 * All values have fallbacks if not explicitly defined in the config.
	 *
	 * @param section the configuration section containing world settings
	 * @return a constructed {@link WorldSetting} with the parsed values
	 */
	private WorldSetting sectionToWorldSetting(Section section) {
		return WorldSetting.of(section.getBoolean("enable", true), section.getInt("min-tick-unit", 300),
				section.getBoolean("offline-tick.enable", false),
				section.getInt("offline-tick.max-offline-seconds", 1200),
				section.getInt("offline-tick.max-loading-time", 100));
	}

	@Nullable
	public WorldSetting getWorldSetting(String worldName) {
		return worldSettings.get(worldName);
	}

	@NotNull
	public WorldSetting getDefaultWorldSetting() {
		return defaultWorldSetting;
	}
}