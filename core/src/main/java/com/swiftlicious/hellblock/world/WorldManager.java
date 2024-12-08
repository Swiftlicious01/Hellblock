/*
 *  Copyright (C) <2024> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.swiftlicious.hellblock.world;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.*;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.world.adapter.BukkitWorldAdapter;
import com.swiftlicious.hellblock.world.adapter.SlimeWorldAdapter;
import com.swiftlicious.hellblock.world.adapter.WorldAdapter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WorldManager implements WorldManagerInterface, Listener {

	private final HellblockPlugin instance;
	private final TreeSet<WorldAdapter<?>> adapters = new TreeSet<>();
	private final ConcurrentMap<String, HellblockWorld<?>> worlds = new ConcurrentHashMap<>();
	private final Map<String, WorldSetting> worldSettings = new HashMap<>();
	private WorldAdapter<?> adapter;
	private WorldSetting defaultWorldSetting;
	private MatchRule matchRule;
	private Set<String> worldList;

	public WorldManager(HellblockPlugin plugin) {
		this.instance = plugin;
		if (plugin.getConfigManager().perPlayerWorlds()) {
			try {
				Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
				SlimeWorldAdapter adapter = new SlimeWorldAdapter(1);
				adapters.add(adapter);
				Bukkit.getPluginManager().registerEvents(adapter, plugin);
				instance.getIntegrationManager().isHooked("SlimeWorldManager");
			} catch (ClassNotFoundException ignored) {
			}
			if (Bukkit.getPluginManager().isPluginEnabled("SlimeWorldPlugin")) {
				SlimeWorldAdapter adapter = new SlimeWorldAdapter(2);
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
		for (World world : Bukkit.getWorlds()) {
			if (isMechanicEnabled(world)) {
				loadWorld(world);
			} else {
				unloadWorld(world, false);
			}
		}
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.worldSettings.clear();
	}

	@Override
	public void disable() {
		this.unload();
		for (World world : Bukkit.getWorlds()) {
			unloadWorld(world, true);
		}
		for (WorldAdapter<?> adapter : this.adapters) {
			if (adapter instanceof Listener listener) {
				HandlerList.unregisterAll(listener);
			}
		}
	}

	private void loadWorldsFromConfig() {
		YamlDocument config = instance.getConfigManager().getMainConfig();

		Section section = config.getSection("general.worlds");
		if (section == null) {
			instance.getPluginLogger().warn("worlds section should not be null");
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

		Section settingSection = section.getSection("settings");
		if (settingSection == null) {
			instance.getPluginLogger().warn("worlds.settings section should not be null");
			return;
		}

		Section defaultSection = settingSection.getSection("_DEFAULT_");
		if (defaultSection == null) {
			instance.getPluginLogger().warn("worlds.settings._DEFAULT_ section should not be null");
			return;
		}

		this.defaultWorldSetting = sectionToWorldSetting(defaultSection);

		Section worldSection = settingSection.getSection("_WORLDS_");
		if (worldSection != null) {
			for (Map.Entry<String, Object> entry : worldSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section inner) {
					this.worldSettings.put(entry.getKey(), sectionToWorldSetting(inner));
				}
			}
		}
	}

	public String getHellblockWorldFormat(int id) {
		if (instance.getConfigManager().perPlayerWorlds())
			return "hellblock_world_" + id;
		else
			return instance.getConfigManager().worldName();
	}

	@Override
	public HellblockWorld<?> loadWorld(World world) {
		Optional<HellblockWorld<?>> optionalWorld = getWorld(world);
		if (optionalWorld.isPresent()) {
			HellblockWorld<?> hellblockWorld = optionalWorld.get();
			hellblockWorld.setting(Optional.ofNullable(worldSettings.get(world.getName())).orElse(defaultWorldSetting));
			return hellblockWorld;
		}
		HellblockWorld<?> adaptedWorld = adapt(world);
		adaptedWorld.setting(Optional.ofNullable(worldSettings.get(world.getName())).orElse(defaultWorldSetting));
		adaptedWorld.setTicking(true);
		this.worlds.put(world.getName(), adaptedWorld);
		for (Chunk chunk : world.getLoadedChunks()) {
			ChunkPos pos = ChunkPos.fromBukkitChunk(chunk);
			loadLoadedChunk(adaptedWorld, pos);
			notifyOfflineUpdates(adaptedWorld, pos);
		}
		return adaptedWorld;
	}

	// Before using the method, make sure that the bukkit chunk is loaded
	public void loadLoadedChunk(HellblockWorld<?> world, ChunkPos pos) {
		if (world.isChunkLoaded(pos))
			return;
		Optional<HellblockChunk> customChunk = world.getChunk(pos);
		// don't load bukkit chunk again since it has been loaded
		customChunk.ifPresent(hellblockChunk -> hellblockChunk.load(false));
	}

	public void notifyOfflineUpdates(HellblockWorld<?> world, ChunkPos pos) {
		world.getLoadedChunk(pos).ifPresent(HellblockChunk::notifyOfflineTask);
	}

	@Override
	public boolean unloadWorld(World world, boolean disabling) {
		HellblockWorld<?> removedWorld = worlds.remove(world.getName());
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
		World world = event.getWorld();
		if (!isMechanicEnabled(world))
			return;
		loadWorld(world);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onWorldUnload(WorldUnloadEvent event) {
		World world = event.getWorld();
		if (!isMechanicEnabled(world))
			return;
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
			ChunkPos pos = ChunkPos.fromBukkitChunk(chunk);
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
		this.getWorld(world).ifPresent(customWorld -> {
			if (customWorld.setting().offlineTick()) {
				notifyOfflineUpdates(customWorld, ChunkPos.fromBukkitChunk(chunk));
			}
		});
	}

	@Override
	public boolean isMechanicEnabled(World world) {
		if (world == null)
			return false;
		if (matchRule == MatchRule.WHITELIST) {
			return worldList.contains(world.getName());
		} else if (matchRule == MatchRule.BLACKLIST) {
			return !worldList.contains(world.getName());
		} else {
			for (String regex : worldList) {
				if (world.getName().matches(regex)) {
					return true;
				}
			}
			return false;
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
			Object world = adapter.getWorld(name);
			if (world != null) {
				return adapter.adapt(world);
			}
		}
		throw new RuntimeException("Unable to adapt world " + name);
	}

	public enum MatchRule {
		BLACKLIST, WHITELIST, REGEX
	}

	private static WorldSetting sectionToWorldSetting(Section section) {
		return WorldSetting.of(section.getBoolean("enable", true), section.getInt("min-tick-unit", 300),
				getRandomTickModeByString(section.getString("farming.mode")),
				section.getInt("farming.tick-interval", 1), section.getBoolean("offline-tick.enable", false),
				section.getInt("offline-tick.max-offline-seconds", 1200),
				section.getInt("offline-tick.max-loading-time", 100), section.getInt("farming.max-per-chunk", 128),
				section.getInt("random-tick-speed", 0));
	}

	private static int getRandomTickModeByString(String str) {
		if (str == null) {
			return 1;
		}
		return TickMode.valueOf(str.toUpperCase(Locale.ENGLISH)).mode();
	}
}