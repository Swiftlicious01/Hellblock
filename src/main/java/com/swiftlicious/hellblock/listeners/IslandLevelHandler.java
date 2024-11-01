package com.swiftlicious.hellblock.listeners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
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
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer.HellblockData;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class IslandLevelHandler implements Listener {

	private final HellblockPlugin instance;

	private final Map<UUID, Collection<LevelBlockCache>> placedByPlayerCache;

	private final Map<UUID, Integer> levelRankCache;

	public IslandLevelHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.placedByPlayerCache = new HashMap<>();
		this.levelRankCache = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
		clearCache();
		instance.getScheduler().runTaskAsyncTimer(() -> this.levelRankCache.clear(), 1, 30, TimeUnit.MINUTES);
		instance.getScheduler().runTaskAsyncTimer(
				() -> instance.getHellblockHandler().getActivePlayers().keySet().forEach(this::saveCache), 1, 2,
				TimeUnit.HOURS);
	}

	private void clearCache() {
		this.placedByPlayerCache.clear();
	}

	public void saveCache(@NonNull UUID id) {
		if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
				&& !this.placedByPlayerCache.get(id).isEmpty()) {
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
			File playerFile = pi.getPlayerFile();
			YamlConfiguration playerConfig = pi.getHellblockPlayer();
			ConfigurationSection section = playerConfig.getConfigurationSection("player.level-block-cache");
			if (section == null)
				section = playerConfig.createSection("player.level-block-cache");
			List<LevelBlockCache> copy = new ArrayList<>(this.placedByPlayerCache.get(id));
			for (int i = 0; i < copy.size(); i++) {
				for (int j = 0; j < copy.size(); j++) {
					LevelBlockCache cacheCopyOne = copy.get(i);
					LevelBlockCache cacheCopyTwo = copy.get(j);
					if (cacheCopyOne.getX() == cacheCopyTwo.getX() && cacheCopyOne.getY() == cacheCopyTwo.getY()
							&& cacheCopyOne.getZ() == cacheCopyTwo.getZ()) {
						this.placedByPlayerCache.remove(id, cacheCopyOne);
					}
				}
			}
			for (LevelBlockCache cache : this.placedByPlayerCache.get(id)) {
				int lastCacheNumber = section.getKeys(false).size();
				int counter = lastCacheNumber + 1;
				section.set(counter + ".material", cache.getMaterial().toString().toUpperCase());
				section.set(counter + ".x", cache.getX());
				section.set(counter + ".y", cache.getY());
				section.set(counter + ".z", cache.getZ());
			}
			try {
				playerConfig.save(playerFile);
			} catch (IOException ex) {
				LogUtils.severe(String.format("Unable to save player file for %s!", pi.getPlayer().getName()), ex);
			}
			this.placedByPlayerCache.remove(id);
		}
	}

	public void loadCache(@NonNull UUID id) {
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
		File playerFile = pi.getPlayerFile();
		YamlConfiguration playerConfig = pi.getHellblockPlayer();
		if (playerConfig.contains("player.level-block-cache")) {
			Collection<LevelBlockCache> cacheCollection = new HashSet<>();
			;
			ConfigurationSection section = playerConfig.getConfigurationSection("player.level-block-cache");
			for (String cache : section.getKeys(false)) {
				Material type = Material.getMaterial(section.getString(cache + ".material").toUpperCase());
				int x = section.getInt(cache + ".x");
				int y = section.getInt(cache + ".y");
				int z = section.getInt(cache + ".z");
				Block checkBlock = instance.getHellblockHandler().getHellblockWorld().getBlockAt(x, y, z);
				if (checkBlock.getType() == type) {
					LevelBlockCache newCache = new LevelBlockCache(type, x, y, z);
					cacheCollection.add(newCache);
				}
				section.set(cache, null);
			}
			playerConfig.set("player.level-block-cache", null);
			try {
				playerConfig.save(playerFile);
			} catch (IOException ex) {
				LogUtils.severe(String.format("Unable to save player file for %s!", pi.getPlayer().getName()), ex);
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
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegions(id).stream().findFirst().orElse(null);
			if (region == null)
				return;
			UUID ownerID = region.getOwners().getUniqueIds().stream().findFirst().orElse(null);
			if (ownerID == null)
				return;
			final HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(ownerID);
			if (!ti.getParty().getIslandMembers().contains(id)) {
				return;
			}
		}
		Material material = block.getType();
		int x = block.getLocation().getBlockX();
		int y = block.getLocation().getBlockY();
		int z = block.getLocation().getBlockZ();
		if (getLevelBlockList().contains(material)) {
			LevelBlockCache levelBlockUpdate = new LevelBlockCache(material, x, y, z, true);
			this.placedByPlayerCache.putIfAbsent(id, new HashSet<>());
			if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
					&& !this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
				this.placedByPlayerCache.get(id).add(levelBlockUpdate);
			}
			updateLevelFromBlockChange(id, levelBlockUpdate, true);
		}
	}

	@EventHandler
	public void onLevelBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegions(id).stream().findFirst().orElse(null);
			if (region == null)
				return;
			UUID ownerID = region.getOwners().getUniqueIds().stream().findFirst().orElse(null);
			if (ownerID == null)
				return;
			final HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(ownerID);
			if (!ti.getParty().getIslandMembers().contains(id)) {
				return;
			}
		}
		Material material = block.getType();
		int x = block.getLocation().getBlockX();
		int y = block.getLocation().getBlockY();
		int z = block.getLocation().getBlockZ();
		if (getLevelBlockList().contains(material)) {
			if (!this.placedByPlayerCache.containsKey(id))
				return;
			LevelBlockCache levelBlockUpdate = this.placedByPlayerCache.get(id).stream()
					.filter(cache -> cache.getMaterial() == material && cache.getX() == x && cache.getY() == y
							&& cache.getZ() == z)
					.findFirst().orElse(null);
			if (levelBlockUpdate == null)
				return;
			levelBlockUpdate.setIfPlacedByPlayer(
					(this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
							&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
			updateLevelFromBlockChange(id, levelBlockUpdate, false);
			if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
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

		Collection<LivingEntity> entitiesNearby = block.getWorld().getNearbyLivingEntities(block.getLocation(), 25, 25,
				25);
		Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(), entitiesNearby);
		if (player != null) {
			final UUID id = player.getUniqueId();
			if (instance.getHellblockHandler().isWorldguardProtected()) {
				ProtectedRegion region = instance.getWorldGuardHandler().getRegions(id).stream().findFirst()
						.orElse(null);
				if (region == null)
					return;
				UUID ownerID = region.getOwners().getUniqueIds().stream().findFirst().orElse(null);
				if (ownerID == null)
					return;
				final HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(ownerID);
				if (!ti.getParty().getIslandMembers().contains(id)) {
					return;
				}
			}
			Material material = block.getType();
			int x = block.getLocation().getBlockX();
			int y = block.getLocation().getBlockY();
			int z = block.getLocation().getBlockZ();
			if (getLevelBlockList().contains(material)) {
				if (!this.placedByPlayerCache.containsKey(id))
					return;
				LevelBlockCache levelBlockUpdate = this.placedByPlayerCache.get(id).stream()
						.filter(cache -> cache.getMaterial() == material && cache.getX() == x && cache.getY() == y
								&& cache.getZ() == z)
						.findFirst().orElse(null);
				if (levelBlockUpdate == null)
					return;
				levelBlockUpdate.setIfPlacedByPlayer(
						(this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
								&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
				updateLevelFromBlockChange(id, levelBlockUpdate, false);
				if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
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

		Collection<LivingEntity> entitiesNearby = block.getWorld().getNearbyLivingEntities(block.getLocation(), 25, 25,
				25);
		Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(), entitiesNearby);
		if (player != null) {
			final UUID id = player.getUniqueId();
			if (instance.getHellblockHandler().isWorldguardProtected()) {
				ProtectedRegion region = instance.getWorldGuardHandler().getRegions(id).stream().findFirst()
						.orElse(null);
				if (region == null)
					return;
				UUID ownerID = region.getOwners().getUniqueIds().stream().findFirst().orElse(null);
				if (ownerID == null)
					return;
				final HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(ownerID);
				if (!ti.getParty().getIslandMembers().contains(id)) {
					return;
				}
			}
			Material material = block.getType();
			int x = block.getLocation().getBlockX();
			int y = block.getLocation().getBlockY();
			int z = block.getLocation().getBlockZ();
			if (getLevelBlockList().contains(material)) {
				if (!this.placedByPlayerCache.containsKey(id))
					return;
				LevelBlockCache levelBlockUpdate = this.placedByPlayerCache.get(id).stream()
						.filter(cache -> cache.getMaterial() == material && cache.getX() == x && cache.getY() == y
								&& cache.getZ() == z)
						.findFirst().orElse(null);
				if (levelBlockUpdate == null)
					return;
				levelBlockUpdate.setIfPlacedByPlayer(
						(this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
								&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
				updateLevelFromBlockChange(id, levelBlockUpdate, false);
				if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
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
		int rank = 9999;
		if (this.levelRankCache.containsKey(playerID)) {
			return this.levelRankCache.get(playerID).intValue();
		}

		Map<UUID, Float> levels = new HashMap<>();
		for (File playerData : instance.getHellblockHandler().getPlayersDirectory().listFiles()) {
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
		for (File playerData : instance.getHellblockHandler().getPlayersDirectory().listFiles()) {
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
			if (hellblockLevel <= HellblockPlayer.DEFAULT_LEVEL)
				continue;

			topHellblocks.putIfAbsent(ownerUUID, hellblockLevel);
		}
		LinkedHashMap<UUID, Float> topHellblocksSorted = new LinkedHashMap<>();
		topHellblocks.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEach(x -> topHellblocksSorted.put(x.getKey(), x.getValue()));
		return topHellblocksSorted;
	}

	public void updateLevelFromBlockChange(@NotNull UUID id, @NotNull LevelBlockCache cache, boolean placed) {
		if (!cache.isPlacedByPlayer()) {
			return;
		}
		final List<String> blockLevelSystem = instance.getHellblockHandler().getBlockLevelSystem();
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
			ProtectedRegion region = null;
			if (pi.getHellblockOwner() != null && pi.getHellblockOwner().equals(id)) {
				// is owner updating the island so get based off original id
				region = instance.getWorldGuardHandler().getRegion(id, pi.getID());
			} else {
				// else is another player so just get the region they're in
				ProtectedRegion defRegion = instance.getWorldGuardHandler().getRegions(id).stream().findAny()
						.orElse(null);
				if (defRegion != null) {
					region = defRegion;
				}
			}
			if (region != null) {
				UUID ownerUUID = region.getOwners().getUniqueIds().stream().findAny().orElse(null);
				if (ownerUUID != null) {
					HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(ownerUUID);
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
									if (placed) {
										ti.increaseIslandLevel();
										ti.saveHellblockPlayer();
										instance.getCoopManager().updateParty(ownerUUID, HellblockData.LEVEL_ADDITION,
												1.0F);
									} else {
										ti.decreaseIslandLevel();
										ti.saveHellblockPlayer();
										instance.getCoopManager().updateParty(ownerUUID, HellblockData.LEVEL_REMOVAL,
												1.0F);
									}
								} else {
									if (placed) {
										ti.addToLevel(level);
										ti.saveHellblockPlayer();
										instance.getCoopManager().updateParty(ownerUUID, HellblockData.LEVEL_ADDITION,
												level);
									} else {
										ti.removeFromLevel(level);
										ti.saveHellblockPlayer();
										instance.getCoopManager().updateParty(ownerUUID, HellblockData.LEVEL_REMOVAL,
												level);
									}
								}
							}
						}

						if (ti.getLevel() >= ChallengeType.ISLAND_LEVEL_CHALLENGE.getNeededAmount()) {
							if (!ti.isChallengeActive(ChallengeType.ISLAND_LEVEL_CHALLENGE)
									&& !ti.isChallengeCompleted(ChallengeType.ISLAND_LEVEL_CHALLENGE)) {
								ti.beginChallengeProgression(ChallengeType.ISLAND_LEVEL_CHALLENGE);
							} else {
								ti.updateChallengeProgression(ChallengeType.ISLAND_LEVEL_CHALLENGE,
										ChallengeType.ISLAND_LEVEL_CHALLENGE.getNeededAmount());
								if (ti.isChallengeCompleted(ChallengeType.ISLAND_LEVEL_CHALLENGE)
										&& (int) ti.getLevel() >= ChallengeType.ISLAND_LEVEL_CHALLENGE
												.getNeededAmount()) {
									ti.completeChallenge(ChallengeType.ISLAND_LEVEL_CHALLENGE);
								}
							}
						}
						if (pi.getLevel() >= ChallengeType.ISLAND_LEVEL_CHALLENGE.getNeededAmount()) {
							if (!pi.isChallengeActive(ChallengeType.ISLAND_LEVEL_CHALLENGE)
									&& !pi.isChallengeCompleted(ChallengeType.ISLAND_LEVEL_CHALLENGE)) {
								pi.beginChallengeProgression(ChallengeType.ISLAND_LEVEL_CHALLENGE);
							} else {
								pi.updateChallengeProgression(ChallengeType.ISLAND_LEVEL_CHALLENGE,
										ChallengeType.ISLAND_LEVEL_CHALLENGE.getNeededAmount());
								if (pi.isChallengeCompleted(ChallengeType.ISLAND_LEVEL_CHALLENGE)
										&& (int) pi.getLevel() >= ChallengeType.ISLAND_LEVEL_CHALLENGE
												.getNeededAmount()) {
									pi.completeChallenge(ChallengeType.ISLAND_LEVEL_CHALLENGE);
								}
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
		private boolean placedByPlayer;

		public LevelBlockCache(@NotNull Material type, int x, int y, int z, boolean placedByPlayer) {
			this.type = type;
			this.x = x;
			this.y = y;
			this.z = z;
			this.placedByPlayer = placedByPlayer;
		}

		public LevelBlockCache(@NotNull Material type, int x, int y, int z) {
			this(type, x, y, z, false);
		}

		public @NotNull Material getMaterial() {
			return this.type;
		}

		public int getX() {
			return this.x;
		}

		public int getY() {
			return this.y;
		}

		public int getZ() {
			return this.z;
		}

		public boolean isPlacedByPlayer() {
			return this.placedByPlayer;
		}

		public void setIfPlacedByPlayer(boolean placedByPlayer) {
			this.placedByPlayer = placedByPlayer;
		}
	}
}
