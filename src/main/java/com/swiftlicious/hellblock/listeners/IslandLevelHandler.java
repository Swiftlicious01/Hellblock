package com.swiftlicious.hellblock.listeners;

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
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.jetbrains.annotations.NotNull;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.OfflineUser;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.StringUtils;

import lombok.NonNull;

public class IslandLevelHandler implements Listener {

	private final HellblockPlugin instance;

	private final Map<UUID, Collection<LevelBlockCache>> placedByPlayerCache;

	private final Map<UUID, Integer> levelRankCache;
	private LinkedHashMap<UUID, Float> topCache;

	public IslandLevelHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.placedByPlayerCache = new HashMap<>();
		this.levelRankCache = new HashMap<>();
		this.topCache = new LinkedHashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
		clearCache();
		instance.getScheduler().runTaskAsyncTimer(() -> {
			this.levelRankCache.clear();
			this.topCache.clear();
		}, 1, 30, TimeUnit.MINUTES);
	}

	private void clearCache() {
		this.placedByPlayerCache.clear();
	}

	public void saveCache(@NonNull UUID id) {
		if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
				&& !this.placedByPlayerCache.get(id).isEmpty()) {
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
			List<String> locations = new ArrayList<>();
			for (LevelBlockCache cache : this.placedByPlayerCache.get(id)) {
				String serializedString = cache.getMaterial().toString().toUpperCase() + "|"
						+ StringUtils.serializeLoc(cache.getLocation());
				if (!locations.contains(serializedString))
					locations.add(serializedString);
			}

			this.placedByPlayerCache.remove(id);
			if (!locations.isEmpty()) {
				OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(id);
				if (onlineUser == null)
					return;
				onlineUser.getPlayerData().setLevelBlockLocations(locations);
			}
		}
	}

	public void loadCache(@NonNull UUID id) {
		OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser == null)
			return;
		List<String> locations = onlineUser.getPlayerData().getLevelBlockLocations();
		if (locations != null) {
			Collection<LevelBlockCache> cacheCollection = new HashSet<>();
			for (String cache : locations) {
				String[] cacheString = cache.split(Pattern.quote("|"));
				Material mat = Material.getMaterial(cacheString[0].toUpperCase());
				if (mat == null) {
					LogUtils.warn(String.format("Unknown level block material under UUID: %s: ", id) + cacheString[0]);
					continue;
				}
				Location loc = StringUtils.deserializeLoc(cacheString[1]);
				if (loc == null) {
					LogUtils.warn(String.format("Unknown level block location under UUID: %s: ", id) + cacheString[1]);
					continue;
				}
				World world = loc.getWorld();
				if (world == null) {
					LogUtils.warn(String.format("Unknown level block world under UUID: %s: ", id) + cacheString[1]);
					continue;
				}
				Block checkBlock = instance.getHellblockHandler().getHellblockWorld().getBlockAt(loc.getBlockX(),
						loc.getBlockY(), loc.getBlockZ());
				if (checkBlock.getType() == mat) {
					LevelBlockCache newCache = new LevelBlockCache(mat, loc.getBlockX(), loc.getBlockY(),
							loc.getBlockZ());
					cacheCollection.add(newCache);
				}
				onlineUser.getPlayerData().setLevelBlockLocations(new ArrayList<>());
				this.placedByPlayerCache.put(id, cacheCollection);
			}
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
			instance.getStorageManager().getOfflineUser(ownerID, HBConfig.lockData).thenAccept((result) -> {
				OfflineUser offlineUser = result.orElseThrow();
				if (!offlineUser.getHellblockData().getEntireParty().getIslandMembers().contains(id))
					return;

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
			});
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
			instance.getStorageManager().getOfflineUser(ownerID, HBConfig.lockData).thenAccept((result) -> {
				OfflineUser offlineUser = result.orElseThrow();
				if (!offlineUser.getHellblockData().getEntireParty().getIslandMembers().contains(id))
					return;

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
			});
		}
	}

	@EventHandler
	public void onLevelExplode(BlockExplodeEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Collection<Player> playersNearby = block.getWorld().getNearbyPlayers(block.getLocation(), 25, 25, 25);
		Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(), playersNearby);
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
				instance.getStorageManager().getOfflineUser(ownerID, HBConfig.lockData).thenAccept((result) -> {
					OfflineUser offlineUser = result.orElseThrow();
					if (!offlineUser.getHellblockData().getEntireParty().getIslandMembers().contains(id))
						return;

					Material material = block.getType();
					int x = block.getLocation().getBlockX();
					int y = block.getLocation().getBlockY();
					int z = block.getLocation().getBlockZ();
					if (getLevelBlockList().contains(material)) {
						if (!this.placedByPlayerCache.containsKey(id))
							return;
						LevelBlockCache levelBlockUpdate = this.placedByPlayerCache.get(id).stream()
								.filter(cache -> cache.getMaterial() == material && cache.getX() == x
										&& cache.getY() == y && cache.getZ() == z)
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
				});
			}
		}
	}

	@EventHandler
	public void onLevelBurn(BlockBurnEvent event) {
		final Block block = event.getBlock();
		if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		Collection<Player> playersNearby = block.getWorld().getNearbyPlayers(block.getLocation(), 25, 25, 25);
		Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(), playersNearby);
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
				instance.getStorageManager().getOfflineUser(ownerID, HBConfig.lockData).thenAccept((result) -> {
					OfflineUser offlineUser = result.orElseThrow();
					if (!offlineUser.getHellblockData().getEntireParty().getIslandMembers().contains(id))
						return;

					Material material = block.getType();
					int x = block.getLocation().getBlockX();
					int y = block.getLocation().getBlockY();
					int z = block.getLocation().getBlockZ();
					if (getLevelBlockList().contains(material)) {
						if (!this.placedByPlayerCache.containsKey(id))
							return;
						LevelBlockCache levelBlockUpdate = this.placedByPlayerCache.get(id).stream()
								.filter(cache -> cache.getMaterial() == material && cache.getX() == x
										&& cache.getY() == y && cache.getZ() == z)
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
				});
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
		RankTracker rank = new RankTracker(-1);
		if (this.levelRankCache.containsKey(playerID)) {
			return this.levelRankCache.get(playerID).intValue();
		}

		Map<UUID, Float> levels = new HashMap<>();
		if (instance.getStorageManager().getDataSource().getUniqueUsers(false).isEmpty()) {
			for (OnlineUser playerData : instance.getStorageManager().getOnlineUsers()) {
				if (playerData == null)
					continue;
				UUID ownerUUID = playerData.getHellblockData().getOwnerUUID();
				if (ownerUUID != null && playerData.getUUID().equals(ownerUUID)) {
					float hellblockLevel = playerData.getHellblockData().getLevel();
					levels.putIfAbsent(ownerUUID, hellblockLevel);
				}
			}
		} else {
			for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers(false)) {
				instance.getStorageManager().getOfflineUser(playerData, HBConfig.lockData).thenAccept((result) -> {
					OfflineUser offlineUser = result.orElseThrow();
					UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();
					if (ownerUUID != null && playerData.equals(ownerUUID)) {
						float hellblockLevel = offlineUser.getHellblockData().getLevel();
						levels.putIfAbsent(ownerUUID, hellblockLevel);
					}
				}).join();
			}
		}
		LinkedHashMap<UUID, Float> levelsSorted = new LinkedHashMap<>();
		levels.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEach(x -> levelsSorted.put(x.getKey(), x.getValue()));

		OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(playerID);
		if (onlineUser == null)
			return rank.getRank();
		if (onlineUser.getHellblockData().getOwnerUUID() == null)
			throw new NullPointerException("Owner reference returned null, please report this to the developer.");
		if (instance.getStorageManager().getOnlineUser(onlineUser.getHellblockData().getOwnerUUID()) != null && instance
				.getStorageManager().getOnlineUser(onlineUser.getHellblockData().getOwnerUUID()).isOnline()) {
			float level = instance.getStorageManager().getOnlineUser(onlineUser.getHellblockData().getOwnerUUID())
					.getHellblockData().getLevel();
			if (level <= HellblockData.DEFAULT_LEVEL) {
				return rank.getRank();
			}
		} else {
			instance.getStorageManager().getOfflineUser(onlineUser.getHellblockData().getOwnerUUID(), HBConfig.lockData)
					.thenAccept((result) -> {
						OfflineUser offlineUser = result.orElseThrow();
						float level = offlineUser.getHellblockData().getLevel();
						if (level <= HellblockData.DEFAULT_LEVEL) {
							rank.setRank(-1);
						}
					}).join();
			return rank.getRank();
		}
		Optional<UUID> position = levelsSorted.reversed().keySet().stream()
				.filter(uuid -> Objects.equals(uuid, onlineUser.getHellblockData().getOwnerUUID())).findFirst();
		rank.setRank(
				new LinkedList<>(levels.keySet()).indexOf(position.orElse(onlineUser.getHellblockData().getOwnerUUID()))
						+ 1);
		this.levelRankCache.putIfAbsent(playerID, rank.getRank());
		return rank.getRank();
	}

	public LinkedHashMap<UUID, Float> getTopTenHellblocks() {
		if (!this.topCache.isEmpty()) {
			return this.topCache;
		}
		Map<UUID, Float> topHellblocks = new HashMap<>();
		if (instance.getStorageManager().getDataSource().getUniqueUsers(false).isEmpty()) {
			for (OnlineUser playerData : instance.getStorageManager().getOnlineUsers()) {
				if (playerData == null)
					continue;
				UUID ownerUUID = playerData.getHellblockData().getOwnerUUID();
				float hellblockLevel = playerData.getHellblockData().getLevel();
				if (ownerUUID != null && hellblockLevel > HellblockData.DEFAULT_LEVEL) {
					topHellblocks.putIfAbsent(ownerUUID, hellblockLevel);
				}
			}
		} else {
			for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers(false)) {
				instance.getStorageManager().getOfflineUser(playerData, HBConfig.lockData).thenAccept((result) -> {
					OfflineUser offlineUser = result.orElseThrow();
					UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();
					float hellblockLevel = offlineUser.getHellblockData().getLevel();
					if (ownerUUID != null && hellblockLevel > HellblockData.DEFAULT_LEVEL) {
						topHellblocks.putIfAbsent(ownerUUID, hellblockLevel);
					}
				}).join();
			}
		}
		LinkedHashMap<UUID, Float> topHellblocksSorted = new LinkedHashMap<>();
		topHellblocks.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEach(x -> topHellblocksSorted.put(x.getKey(), x.getValue()));
		if (this.topCache.isEmpty())
			this.topCache = topHellblocksSorted;
		return topHellblocksSorted;
	}

	public void updateLevelFromBlockChange(@NotNull UUID id, @NotNull LevelBlockCache cache, boolean placed) {
		if (!cache.isPlacedByPlayer()) {
			return;
		}
		final List<String> blockLevelSystem = instance.getHellblockHandler().getBlockLevelSystem();
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(id);
			if (onlineUser == null)
				return;
			ProtectedRegion region = null;
			if (onlineUser.getHellblockData().getOwnerUUID() != null
					&& onlineUser.getHellblockData().getOwnerUUID().equals(id)) {
				// is owner updating the island so get based off original id
				region = instance.getWorldGuardHandler().getRegion(id, onlineUser.getHellblockData().getID());
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
					instance.getStorageManager().getOfflineUser(ownerUUID, HBConfig.lockData).thenAccept((result) -> {
						OfflineUser offlineUser = result.orElseThrow();
						if (offlineUser.getHellblockData().isAbandoned()) {
							if (Bukkit.getPlayer(id) != null)
								instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getPlayer(id),
										HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}

						for (String blockConversion : blockLevelSystem) {
							String[] split = blockConversion.split(":");
							Material block = Material.getMaterial(split[0].toUpperCase());
							float level = 0.0F;
							try {
								level = Float.parseFloat(split[1]);
							} catch (NumberFormatException ex) {
								LogUtils.warn(String.format("The level defined for the block %s is not a valid number",
										block.toString()), ex);
								continue;
							}
							if (block != null && level > 0.0F) {
								if (block == cache.getMaterial()) {
									if (level == HellblockData.DEFAULT_LEVEL) {
										if (placed) {
											offlineUser.getHellblockData().increaseIslandLevel();
										} else {
											offlineUser.getHellblockData().decreaseIslandLevel();
										}
									} else {
										if (placed) {
											offlineUser.getHellblockData().addToLevel(level);
										} else {
											offlineUser.getHellblockData().removeFromLevel(level);
										}
									}
								}
							}
						}
					});
				}
			}
		} else {
			// TODO: using plugin protection
		}
	}

	private class RankTracker {
		private int rank;

		public RankTracker(int rank) {
			this.rank = rank;
		}

		public int getRank() {
			return this.rank;
		}

		public void setRank(int rank) {
			this.rank = rank;
		}
	}

	private class LevelBlockCache {

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

		public @NonNull Location getLocation() {
			return new Location(instance.getHellblockHandler().getHellblockWorld(), this.x, this.y, this.z);
		}

		public boolean isPlacedByPlayer() {
			return this.placedByPlayer;
		}

		public void setIfPlacedByPlayer(boolean placedByPlayer) {
			this.placedByPlayer = placedByPlayer;
		}
	}
}
