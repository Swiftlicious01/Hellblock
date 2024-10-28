package com.swiftlicious.hellblock.listeners;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.jetbrains.annotations.NotNull;

import com.google.common.io.Files;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class IslandLevelHandler implements Listener {

	private final HellblockPlugin instance;

	private final Collection<LevelBlockCache> blockCache;
	private final Map<UUID, Collection<LevelBlockCache>> placedByPlayerCache;

	private final Map<UUID, Integer> levelRankCache;

	public IslandLevelHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.blockCache = new HashSet<>();
		this.placedByPlayerCache = new HashMap<>();
		this.levelRankCache = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
		clearCache();
		instance.getScheduler().runTaskAsyncTimer(() -> this.levelRankCache.clear(), 1, 30, TimeUnit.MINUTES);
		instance.getScheduler().runTaskAsyncTimer(
				() -> instance.getHellblockHandler().getActivePlayers().keySet().forEach(id -> saveCache(id)), 1, 2,
				TimeUnit.HOURS);
	}

	private void clearCache() {
		this.blockCache.clear();
		this.placedByPlayerCache.clear();
	}

	public void saveCache(@NonNull UUID id) {
		if (this.placedByPlayerCache.containsKey(id) && !this.placedByPlayerCache.get(id).isEmpty()) {
			File playerFile = new File(
					instance.getHellblockHandler().getPlayersDirectory() + File.separator + id.toString() + ".yml");
			YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
			int i = 0;
			for (LevelBlockCache cache : this.placedByPlayerCache.get(id)) {
				instance.getAdventureManager().sendMessage(null, cache.toString());
				++i;
				playerConfig.set("player.level-block-cache." + i + ".material",
						cache.getMaterial().toString().toUpperCase());
				playerConfig.set("player.level-block-cache." + i + ".x", cache.getLocation().getBlockX());
				playerConfig.set("player.level-block-cache." + i + ".y", cache.getLocation().getBlockY());
				playerConfig.set("player.level-block-cache." + i + ".z", cache.getLocation().getBlockZ());
				playerConfig.set("player.level-block-cache." + i + ".action",
						cache.getBlockAction().toString().toUpperCase());
			}
			try {
				playerConfig.save(playerFile);
			} catch (IOException ex) {
				LogUtils.severe(String.format("Unable to save player file for %s!", id), ex);
			}
			this.placedByPlayerCache.remove(id);
		}
	}

	public void loadCache(@NonNull UUID id) {
		File playerFile = new File(
				instance.getHellblockHandler().getPlayersDirectory() + File.separator + id.toString() + ".yml");
		YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
		if (playerConfig.contains("player.level-block-cache")) {
			Collection<LevelBlockCache> cacheCollection = new HashSet<>();
			for (String cacheData : playerConfig.getConfigurationSection("player.level-block-cache").getKeys(false)) {
				int i = Integer.parseInt(cacheData);
				Material type = Material
						.getMaterial(playerConfig.getString("player.level-block-cache." + i + ".material"));
				int x = playerConfig.getInt("player.level-block-cache." + i + ".x");
				int y = playerConfig.getInt("player.level-block-cache." + i + ".y");
				int z = playerConfig.getInt("player.level-block-cache." + i + ".z");
				BlockAction action = BlockAction
						.valueOf(playerConfig.getString("player.level-block-cache." + i + ".action"));
				LevelBlockCache newCache = new LevelBlockCache(type, x, y, z, action);
				cacheCollection.add(newCache);
				playerConfig.set("player.level-block-cache." + i, null);
			}
			try {
				playerConfig.save(playerFile);
			} catch (IOException ex) {
				LogUtils.severe(String.format("Unable to save player file for %s!", id), ex);
			}
			this.placedByPlayerCache.put(id, cacheCollection);
		}
	}

	@EventHandler
	public void onLevelPlace(BlockPlaceEvent event) {
		final Block block = event.getBlockPlaced();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		Material material = block.getType();
		int x = block.getLocation().getBlockX();
		int y = block.getLocation().getBlockY();
		int z = block.getLocation().getBlockZ();
		if (getLevelBlockList().contains(material)) {
			LevelBlockCache levelBlockUpdate = new LevelBlockCache(material, x, y, z, BlockAction.PLACE, true);
			this.placedByPlayerCache.putIfAbsent(id, new HashSet<>());
			if (this.placedByPlayerCache.containsKey(id)
					&& !this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
				this.placedByPlayerCache.get(id).add(levelBlockUpdate);
			}
			updateLevelFromBlockChange(id, levelBlockUpdate);
		}
	}

	@EventHandler
	public void onLevelBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		Material material = block.getType();
		int x = block.getLocation().getBlockX();
		int y = block.getLocation().getBlockY();
		int z = block.getLocation().getBlockZ();
		if (getLevelBlockList().contains(material)) {
			LevelBlockCache levelBlockUpdate = new LevelBlockCache(material, x, y, z, BlockAction.BREAK, false);
			levelBlockUpdate.setIfPlacedByPlayer((this.placedByPlayerCache.containsKey(id)
					&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
			updateLevelFromBlockChange(id, levelBlockUpdate);
			if (this.placedByPlayerCache.containsKey(id)
					&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
				this.placedByPlayerCache.get(id).remove(levelBlockUpdate);
			}
		}
	}

	@EventHandler
	public void onLevelExplode(BlockExplodeEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Collection<Entity> entitiesNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25);
		Player player = instance.getNetherrackGenerator().getClosestPlayer(block.getLocation(), entitiesNearby);
		if (player != null) {
			final UUID id = player.getUniqueId();
			Material material = block.getType();
			int x = block.getLocation().getBlockX();
			int y = block.getLocation().getBlockY();
			int z = block.getLocation().getBlockZ();
			if (getLevelBlockList().contains(material)) {
				LevelBlockCache levelBlockUpdate = new LevelBlockCache(material, x, y, z, BlockAction.BREAK, false);
				levelBlockUpdate.setIfPlacedByPlayer((this.placedByPlayerCache.containsKey(id)
						&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
				updateLevelFromBlockChange(id, levelBlockUpdate);
				if (this.placedByPlayerCache.containsKey(id)
						&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
					this.placedByPlayerCache.get(id).remove(levelBlockUpdate);
				}
			}
		}
	}

	@EventHandler
	public void onLevelBurn(BlockBurnEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Collection<Entity> entitiesNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25);
		Player player = instance.getNetherrackGenerator().getClosestPlayer(block.getLocation(), entitiesNearby);
		if (player != null) {
			final UUID id = player.getUniqueId();
			Material material = block.getType();
			int x = block.getLocation().getBlockX();
			int y = block.getLocation().getBlockY();
			int z = block.getLocation().getBlockZ();
			if (getLevelBlockList().contains(material)) {
				LevelBlockCache levelBlockUpdate = new LevelBlockCache(material, x, y, z, BlockAction.BREAK, false);
				levelBlockUpdate.setIfPlacedByPlayer((this.placedByPlayerCache.containsKey(id)
						&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
				updateLevelFromBlockChange(id, levelBlockUpdate);
				if (this.placedByPlayerCache.containsKey(id)
						&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
					this.placedByPlayerCache.get(id).remove(levelBlockUpdate);
				}
			}
		}
	}

	public Set<Material> getLevelBlockList() {
		Set<Material> materialList = new HashSet<>();
		final List<String> blockLevelSystem = instance.getHellblockHandler().getBlockLevelSystem();
		for (String blockConversion : blockLevelSystem) {
			String[] split = blockConversion.split(":");
			Material block = Material.getMaterial(split[0].toUpperCase());
			if (block != null && !Tag.AIR.isTagged(block)) {
				materialList.add(block);
			}
		}

		return materialList;
	}

	public int getLevelRank(@NonNull UUID playerID) {
		int rank = -1;
		if (this.levelRankCache.containsKey(playerID)) {
			return this.levelRankCache.get(playerID).intValue();
		}

		Map<UUID, Float> levels = new HashMap<>();
		for (File playerData : HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory().listFiles()) {
			if (!playerData.isFile() || !playerData.getName().endsWith(".yml"))
				continue;
			String uuid = Files.getNameWithoutExtension(playerData.getName());
			UUID id = null;
			try {
				id = UUID.fromString(uuid);
			} catch (IllegalArgumentException ignored) {
				// ignored
				continue;
			}
			if (id == null)
				continue;
			YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerData);
			if (playerConfig.getKeys(true).size() == 0)
				continue;
			String ownerID = playerConfig.getString("player.owner");
			if (ownerID == null)
				continue;
			UUID ownerUUID = null;
			try {
				ownerUUID = UUID.fromString(ownerID);
			} catch (IllegalArgumentException ignored) {
				// ignored
				continue;
			}
			if (ownerUUID == null)
				continue;
			if (id.equals(ownerUUID)) {
				if (!playerConfig.contains("player.hellblock-level"))
					continue;
				float hellblockLevel = (float) playerConfig.getDouble("player.hellblock-level",
						HellblockPlayer.DEFAULT_LEVEL);
				levels.putIfAbsent(ownerUUID, hellblockLevel);
			}
		}
		LinkedHashMap<UUID, Float> levelsSorted = new LinkedHashMap<>();
		levels.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEach(x -> levelsSorted.put(x.getKey(), x.getValue()));

		Optional<UUID> position = levelsSorted.reversed().keySet().stream().filter(uuid -> Objects.equals(uuid,
				instance.getHellblockHandler().getActivePlayer(playerID).getHellblockOwner())).findFirst();
		rank = new LinkedList<>(levels.keySet()).indexOf(
				position.orElse(instance.getHellblockHandler().getActivePlayer(playerID).getHellblockOwner())) + 1;
		this.levelRankCache.putIfAbsent(playerID, rank);
		return rank;
	}

	public LinkedHashMap<UUID, Float> getTopTenHellblocks() {
		Map<UUID, Float> topHellblocks = new HashMap<>();
		for (File playerData : HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory().listFiles()) {
			if (!playerData.isFile() || !playerData.getName().endsWith(".yml"))
				continue;
			String uuid = Files.getNameWithoutExtension(playerData.getName());
			UUID id = null;
			try {
				id = UUID.fromString(uuid);
			} catch (IllegalArgumentException ignored) {
				// ignored
				continue;
			}
			if (id == null)
				continue;
			YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerData);
			if (playerConfig.getKeys(true).size() == 0)
				continue;
			String ownerID = playerConfig.getString("player.owner");
			if (ownerID == null)
				continue;
			UUID ownerUUID = null;
			try {
				ownerUUID = UUID.fromString(ownerID);
			} catch (IllegalArgumentException ignored) {
				// ignored
				continue;
			}
			if (ownerUUID == null)
				continue;
			if (!playerConfig.contains("player.hellblock-level"))
				continue;
			float hellblockLevel = (float) playerConfig.getDouble("player.hellblock-level",
					HellblockPlayer.DEFAULT_LEVEL);
			if (hellblockLevel <= 1.0F)
				continue;

			topHellblocks.putIfAbsent(ownerUUID, hellblockLevel);
		}
		LinkedHashMap<UUID, Float> topHellblocksSorted = new LinkedHashMap<>();
		topHellblocks.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEach(x -> topHellblocksSorted.put(x.getKey(), x.getValue()));
		return topHellblocksSorted;
	}

	public void updateLevelFromBlockChange(@NotNull UUID id, @NotNull LevelBlockCache cache) {
		if (this.blockCache.contains(cache)) {
			return;
		}
		if (!cache.isPlacedByPlayer()) {
			return;
		}
		final List<String> blockLevelSystem = instance.getHellblockHandler().getBlockLevelSystem();
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			HellblockPlayer pi;
			if (instance.getHellblockHandler().getActivePlayers().get(id) != null) {
				pi = instance.getHellblockHandler().getActivePlayers().get(id);
			} else {
				pi = new HellblockPlayer(id);
			}
			ProtectedRegion region = null;
			if (pi.getHellblockOwner() != null && pi.getHellblockOwner().equals(id)) {
				// is owner updating the island so get based off original id
				region = instance.getWorldGuardHandler().getRegion(id);
			} else {
				// else is another player so just get the region they're in
				ProtectedRegion defRegion = null;
				for (Iterator<ProtectedRegion> regions = instance.getWorldGuardHandler().getRegions(id)
						.iterator(); regions.hasNext();) {
					defRegion = regions.next();
					break;
				}
				if (defRegion != null) {
					region = defRegion;
				}
			}
			if (region != null) {
				Set<UUID> owners = region.getOwners().getUniqueIds();
				UUID ownerUUID = null;
				for (Iterator<UUID> uuids = owners.iterator(); uuids.hasNext();) {
					ownerUUID = uuids.next();
					break;
				}
				if (ownerUUID != null) {
					HellblockPlayer ti;
					if (instance.getHellblockHandler().getActivePlayers().get(ownerUUID) != null) {
						ti = instance.getHellblockHandler().getActivePlayers().get(ownerUUID);
					} else {
						ti = new HellblockPlayer(ownerUUID);
					}

					for (String blockConversion : blockLevelSystem) {
						String[] split = blockConversion.split(":");
						Material block = Material.getMaterial(split[0].toUpperCase());
						float level = 0.0F;
						try {
							level = Float.parseFloat(split[1]);
						} catch (NumberFormatException ex) {
							LogUtils.warn(
									String.format("The level defined for the block %s is not a valid number", block),
									ex);
							continue;
						}
						if (block != null && level > 0.0F) {
							if (block == cache.getMaterial()) {
								if (level == 1.0F) {
									if (cache.getBlockAction() == BlockAction.PLACE) {
										ti.increaseIslandLevel();
										ti.saveHellblockPlayer();
										instance.getCoopManager().updateParty(ownerUUID, "leveladd", 1.0F);
									} else {
										ti.decreaseIslandLevel();
										ti.saveHellblockPlayer();
										instance.getCoopManager().updateParty(ownerUUID, "levelremove", 1.0F);
									}
								} else {
									if (cache.getBlockAction() == BlockAction.PLACE) {
										ti.addToLevel(level);
										ti.saveHellblockPlayer();
										instance.getCoopManager().updateParty(ownerUUID, "leveladd", level);
									} else {
										ti.removeFromLevel(level);
										ti.saveHellblockPlayer();
										instance.getCoopManager().updateParty(ownerUUID, "levelremove", level);
									}
								}
								this.blockCache.add(cache);
							}
						}
					}
				}
			}
		} else {
			// TODO: using plugin protection
		}
	}

	public class LevelBlockCache {

		private final Material type;
		private final int x, y, z;
		private final BlockAction action;
		private boolean placedByPlayer;

		public LevelBlockCache(@NotNull Material type, int x, int y, int z, @NotNull BlockAction action,
				boolean placedByPlayer) {
			this.type = type;
			this.x = x;
			this.y = y;
			this.z = z;
			this.action = action;
			this.placedByPlayer = placedByPlayer;
		}

		public LevelBlockCache(@NotNull Material type, int x, int y, int z, @NotNull BlockAction action) {
			this(type, x, y, z, action, false);
		}

		public @NotNull Material getMaterial() {
			return this.type;
		}

		public @NotNull Location getLocation() {
			return new Location(instance.getHellblockHandler().getHellblockWorld(), this.x, this.y, this.z);
		}

		public @NotNull BlockAction getBlockAction() {
			return this.action;
		}

		public boolean isPlacedByPlayer() {
			return this.placedByPlayer;
		}

		public void setIfPlacedByPlayer(boolean placedByPlayer) {
			this.placedByPlayer = placedByPlayer;
		}
	}

	public enum BlockAction {
		BREAK, PLACE;
	}
}
