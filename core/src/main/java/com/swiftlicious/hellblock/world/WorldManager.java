package com.swiftlicious.hellblock.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.adapter.BukkitWorldAdapter;
import com.swiftlicious.hellblock.world.adapter.SlimeWorldAdapter;
import com.swiftlicious.hellblock.world.adapter.WorldAdapter;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class WorldManager implements WorldManagerInterface, Listener {

	protected final HellblockPlugin instance;
	private final TreeSet<WorldAdapter<?>> adapters = new TreeSet<>();
	private final ConcurrentMap<String, HellblockWorld<?>> worlds = new ConcurrentHashMap<>();
	private final Map<String, WorldSetting> worldSettings = new HashMap<>();
	private WorldAdapter<?> adapter;
	private WorldSetting defaultWorldSetting;
	private MatchRule matchRule;
	private Set<String> worldList;

	private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();
	private SchedulerTask idleTask = null;

	public WorldManager(HellblockPlugin plugin) {
		this.instance = plugin;
		if (plugin.getConfigManager().perPlayerWorlds()) {
			try {
				Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
				final SlimeWorldAdapter adapter = new SlimeWorldAdapter(1);
				adapters.add(adapter);
				Bukkit.getPluginManager().registerEvents(adapter, plugin);
				instance.getIntegrationManager().isHooked("SlimeWorldManager");
			} catch (ClassNotFoundException ignored) {
			}
			if (Bukkit.getPluginManager().isPluginEnabled("SlimeWorldPlugin")) {
				final SlimeWorldAdapter adapter = new SlimeWorldAdapter(2);
				adapters.add(adapter);
				Bukkit.getPluginManager().registerEvents(adapter, plugin);
				instance.getIntegrationManager().isHooked("AdvancedSlimePaper");
			}
		}
		this.adapters.add(new BukkitWorldAdapter());
	}

	@Override
	public void load() {
		this.loadWorldsFromConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
		// load and unload worlds
		Bukkit.getWorlds().forEach(world -> {
			if (isMechanicEnabled(world)) {
				loadWorld(world);
			} else {
				unloadWorld(world, false);
			}
		});
		this.idleTask = instance.getScheduler().sync().runRepeating(() -> {
			try {
				tryUnloadIdleWorlds();
			} catch (Exception e) {
				instance.getPluginLogger().warn("Error while unloading idle worlds: " + e.getMessage());
			}
		}, TimeUnit.MINUTES.toMinutes(5), TimeUnit.MINUTES.toMinutes(5), LocationUtils.getAnyLocationInstance());
		instance.debug("WorldManager loaded with " + worlds.size() + " worlds.");
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.worldSettings.clear();
		this.lastAccess.clear();
		if (this.idleTask != null && !this.idleTask.isCancelled()) {
			this.idleTask.cancel();
			this.idleTask = null;
		}
	}

	@Override
	public void disable() {
		unload();
		Bukkit.getWorlds().forEach(world -> unloadWorld(world, true));
		for (WorldAdapter<?> adapter : this.adapters) {
			if (adapter instanceof Listener listener) {
				HandlerList.unregisterAll(listener);
			}
		}
	}

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
					break;
				}
			}
		} else {
			for (WorldAdapter<?> adapter : this.adapters) {
				if (adapter instanceof BukkitWorldAdapter bukkit) {
					this.adapter = bukkit;
					break;
				}
			}
		}

		this.matchRule = MatchRule.valueOf(section.getString("mode", "blacklist").toUpperCase(Locale.ENGLISH));
		this.worldList = new HashSet<>(section.getStringList("list"));

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
	}

	public String getHellblockWorldFormat(int id) {
		if (instance.getConfigManager().perPlayerWorlds()) {
			return "hellblock_world_" + id;
		} else {
			return instance.getConfigManager().worldName();
		}
	}

	public void markWorldAccess(String worldName) {
		this.lastAccess.put(worldName, System.currentTimeMillis());
	}

	public void tryUnloadIdleWorlds() {
		long now = System.currentTimeMillis();
		this.lastAccess.entrySet().forEach(entry -> {
			String worldName = entry.getKey();
			long lastUsed = entry.getValue();

			if ((now - lastUsed) > TimeUnit.MINUTES.toMillis(10)) {
				World world = Bukkit.getWorld(worldName);
				if (world != null && world.getPlayers().isEmpty()) {
					boolean unloaded = Bukkit.unloadWorld(world, true);
					if (unloaded) {
						HellblockPlugin.getInstance().debug("Unloaded idle world: " + worldName);
						this.lastAccess.remove(worldName);
					}
				}
			}
		});
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
			loadLoadedChunk(adaptedWorld, pos);
			notifyOfflineUpdates(adaptedWorld, pos);
		}
		return adaptedWorld;
	}

	// Before using the method, make sure that the bukkit chunk is loaded
	public void loadLoadedChunk(HellblockWorld<?> world, ChunkPos pos) {
		if (world.isChunkLoaded(pos)) {
			return;
		}
		final Optional<CustomChunk> customChunk = world.getChunk(pos);
		// don't load bukkit chunk again since it has been loaded
		customChunk.ifPresent(hellblockChunk -> hellblockChunk.load(false));
	}

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
		removedWorld.save(false, disabling);
		removedWorld.scheduler().shutdownScheduler();
		removedWorld.scheduler().shutdownExecutor();
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
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onWorldUnload(WorldUnloadEvent event) {
		final World world = event.getWorld();
		if (!isMechanicEnabled(world)) {
			return;
		}
		unloadWorld(world, false);
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
			loadLoadedChunk(customWorld, pos);
			if (chunk.isEntitiesLoaded() && customWorld.setting().offlineTick()) {
				notifyOfflineUpdates(customWorld, pos);
			}
		});
	}

	@EventHandler
	public void onEntitiesLoad(EntitiesLoadEvent event) {
		final Chunk chunk = event.getChunk();
		final World world = event.getWorld();
		this.getWorld(world).filter(customWorld -> customWorld.setting().offlineTick())
				.ifPresent(customWorld -> notifyOfflineUpdates(customWorld, ChunkPos.fromBukkitChunk(chunk)));
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
		if (world == null) {
			return false;
		}
		if (matchRule == MatchRule.WHITELIST) {
			return worldList.contains(world.getName());
		} else if (matchRule == MatchRule.BLACKLIST) {
			return !worldList.contains(world.getName());
		} else {
			return worldList.stream().filter(regex -> world.getName().matches(regex)).findFirst().map(regex -> true)
					.orElse(false);
		}
	}

	@Override
	public Optional<HellblockWorld<?>> getWorld(World world) {
		return getWorld(world.getName());
	}

	@Override
	public Optional<HellblockWorld<?>> getWorld(String world) {
		return Optional.ofNullable(worlds.get(world));
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
		return adapter;
	}

	@Override
	public HellblockWorld<?> adapt(World world) {
		return adapt(world.getName());
	}

	@Override
	public HellblockWorld<?> adapt(String name) {
		for (WorldAdapter<?> adapter : adapters) {
			HellblockWorld<?> adapted = tryAdapt(adapter, name);
			if (adapted != null) {
				return adapted;
			}
		}
		throw new RuntimeException("Unable to adapt world " + name);
	}

	private <T> HellblockWorld<?> tryAdapt(WorldAdapter<T> adapter, String name) {
		return adapter.getWorld(name).map(adapter::adapt).orElse(null);
	}

	public enum MatchRule {
		BLACKLIST, WHITELIST, REGEX
	}

	private static WorldSetting sectionToWorldSetting(Section section) {
		return WorldSetting.of(section.getBoolean("enable", true), section.getInt("min-tick-unit", 300),
				section.getBoolean("offline-tick.enable", false),
				section.getInt("offline-tick.max-offline-seconds", 1200),
				section.getInt("offline-tick.max-loading-time", 100));
	}
}