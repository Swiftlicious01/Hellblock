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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Tuple;

public class LevelHandler implements Listener {

	protected final HellblockPlugin instance;

	private final Map<UUID, Collection<LevelBlockCache>> placedByPlayerCache;

	private final Map<UUID, Integer> levelRankCache;
	private LinkedHashMap<UUID, Float> topCache;

	public LevelHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.placedByPlayerCache = new HashMap<>();
		this.levelRankCache = new HashMap<>();
		this.topCache = new LinkedHashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
		clearCache();
		instance.getScheduler().asyncRepeating(() -> {
			this.levelRankCache.clear();
			this.topCache.clear();
		}, 1, 30, TimeUnit.MINUTES);
	}

	private void clearCache() {
		this.placedByPlayerCache.clear();
	}

	public void saveCache(@NotNull UUID id) {
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
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
				if (onlineUser.isEmpty())
					return;
				onlineUser.get().getLocationCacheData().setLevelBlockLocations(locations);
			}
		}
	}

	public void loadCache(@NotNull UUID id) {
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty())
			return;
		List<String> locations = onlineUser.get().getLocationCacheData().getLevelBlockLocations();
		if (locations != null) {
			Collection<LevelBlockCache> cacheCollection = new HashSet<>();
			for (String cache : locations) {
				String[] cacheString = cache.split(Pattern.quote("|"));
				Material mat = Material.getMaterial(cacheString[0].toUpperCase());
				if (mat == null) {
					instance.getPluginLogger()
							.warn(String.format("Unknown level block material under UUID: %s: ", id) + cacheString[0]);
					continue;
				}
				Location loc = StringUtils.deserializeLoc(cacheString[1]);
				if (loc == null || loc.getWorld() == null) {
					instance.getPluginLogger()
							.warn(String.format("Unknown level block location under UUID: %s: ", id) + cacheString[1]);
					continue;
				}
				Block checkBlock = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
				if (checkBlock.getType() == mat) {
					LevelBlockCache newCache = new LevelBlockCache(mat, loc.getWorld(), loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ());
					cacheCollection.add(newCache);
				}
				onlineUser.get().getLocationCacheData().setLevelBlockLocations(new ArrayList<>());
				this.placedByPlayerCache.put(id, cacheCollection);
			}
		}
	}

	@EventHandler
	public void onLevelPlace(BlockPlaceEvent event) {
		final Block block = event.getBlockPlaced();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null)
				return;
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						if (!offlineUser.getHellblockData().getIslandMembers().contains(id))
							return;

						Material material = block.getType();
						EntityType entity = null;
						if (material == Material.SPAWNER) {
							CreatureSpawner spawner = (CreatureSpawner) block.getState();
							entity = spawner.getSpawnedType() != null ? spawner.getSpawnedType() : null;
						}
						int x = block.getLocation().getBlockX();
						int y = block.getLocation().getBlockY();
						int z = block.getLocation().getBlockZ();
						if (getLevelBlockList().contains(Pair.of(material, entity))) {
							LevelBlockCache levelBlockUpdate = new LevelBlockCache(material, block.getWorld(), x, y, z,
									true);
							this.placedByPlayerCache.putIfAbsent(id, new HashSet<>());
							if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
									&& !this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
								this.placedByPlayerCache.get(id).add(levelBlockUpdate);
							}
							updateLevelFromBlockChange(id, offlineUser.getHellblockData(), levelBlockUpdate, true);
						}
					});
		});
	}

	@EventHandler
	public void onLevelBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null)
				return;
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						if (!offlineUser.getHellblockData().getIslandMembers().contains(id))
							return;

						Material material = block.getType();
						EntityType entity = null;
						if (material == Material.SPAWNER) {
							CreatureSpawner spawner = (CreatureSpawner) block.getState();
							entity = spawner.getSpawnedType() != null ? spawner.getSpawnedType() : null;
						}
						int x = block.getLocation().getBlockX();
						int y = block.getLocation().getBlockY();
						int z = block.getLocation().getBlockZ();
						if (getLevelBlockList().contains(Pair.of(material, entity))) {
							if (!this.placedByPlayerCache.containsKey(id))
								return;
							LevelBlockCache levelBlockUpdate = this.placedByPlayerCache
									.get(id).stream().filter(cache -> cache.getMaterial() == material
											&& cache.getX() == x && cache.getY() == y && cache.getZ() == z)
									.findFirst().orElse(null);
							if (levelBlockUpdate == null)
								return;
							levelBlockUpdate.setIfPlacedByPlayer((this.placedByPlayerCache.containsKey(id)
									&& this.placedByPlayerCache.get(id) != null
									&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
							updateLevelFromBlockChange(id, offlineUser.getHellblockData(), levelBlockUpdate, false);
							if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
									&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
								this.placedByPlayerCache.get(id).remove(levelBlockUpdate);
							}
						}
					});
		});
	}

	@EventHandler
	public void onLevelExplode(BlockExplodeEvent event) {
		final List<Block> blocks = event.blockList();
		for (Block block : blocks) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
				continue;

			Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
					.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
			Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					playersNearby);
			if (player != null) {
				final UUID id = player.getUniqueId();
				instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
					if (ownerUUID == null)
						return;
					instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
							.thenAccept((result) -> {
								if (result.isEmpty())
									return;
								UserData offlineUser = result.get();
								if (!offlineUser.getHellblockData().getIslandMembers().contains(id))
									return;

								Material material = block.getType();
								EntityType entity = null;
								if (material == Material.SPAWNER) {
									CreatureSpawner spawner = (CreatureSpawner) block.getState();
									entity = spawner.getSpawnedType() != null ? spawner.getSpawnedType() : null;
								}
								int x = block.getLocation().getBlockX();
								int y = block.getLocation().getBlockY();
								int z = block.getLocation().getBlockZ();
								if (getLevelBlockList().contains(Pair.of(material, entity))) {
									if (!this.placedByPlayerCache.containsKey(id))
										return;
									LevelBlockCache levelBlockUpdate = this.placedByPlayerCache
											.get(id).stream().filter(cache -> cache.getMaterial() == material
													&& cache.getX() == x && cache.getY() == y && cache.getZ() == z)
											.findFirst().orElse(null);
									if (levelBlockUpdate == null)
										return;
									levelBlockUpdate.setIfPlacedByPlayer((this.placedByPlayerCache.containsKey(id)
											&& this.placedByPlayerCache.get(id) != null
											&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
									updateLevelFromBlockChange(id, offlineUser.getHellblockData(), levelBlockUpdate,
											false);
									if (this.placedByPlayerCache.containsKey(id)
											&& this.placedByPlayerCache.get(id) != null
											&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
										this.placedByPlayerCache.get(id).remove(levelBlockUpdate);
									}
								}
							});
				});
			}
		}
	}

	@EventHandler
	public void onLevelBurn(BlockBurnEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
			return;

		Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25).stream()
				.filter(e -> e.getType() == EntityType.PLAYER).toList();
		Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(), playersNearby);
		if (player != null) {
			final UUID id = player.getUniqueId();
			instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
				if (ownerUUID == null)
					return;
				instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty())
								return;
							UserData offlineUser = result.get();
							if (!offlineUser.getHellblockData().getIslandMembers().contains(id))
								return;

							Material material = block.getType();
							EntityType entity = null;
							if (material == Material.SPAWNER) {
								CreatureSpawner spawner = (CreatureSpawner) block.getState();
								entity = spawner.getSpawnedType() != null ? spawner.getSpawnedType() : null;
							}
							int x = block.getLocation().getBlockX();
							int y = block.getLocation().getBlockY();
							int z = block.getLocation().getBlockZ();
							if (getLevelBlockList().contains(Pair.of(material, entity))) {
								if (!this.placedByPlayerCache.containsKey(id))
									return;
								LevelBlockCache levelBlockUpdate = this.placedByPlayerCache
										.get(id).stream().filter(cache -> cache.getMaterial() == material
												&& cache.getX() == x && cache.getY() == y && cache.getZ() == z)
										.findFirst().orElse(null);
								if (levelBlockUpdate == null)
									return;
								levelBlockUpdate.setIfPlacedByPlayer((this.placedByPlayerCache.containsKey(id)
										&& this.placedByPlayerCache.get(id) != null
										&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)));
								updateLevelFromBlockChange(id, offlineUser.getHellblockData(), levelBlockUpdate, false);
								if (this.placedByPlayerCache.containsKey(id) && this.placedByPlayerCache.get(id) != null
										&& this.placedByPlayerCache.get(id).contains(levelBlockUpdate)) {
									this.placedByPlayerCache.get(id).remove(levelBlockUpdate);
								}
							}
						});
			});
		}
	}

	public Set<Pair<Material, EntityType>> getLevelBlockList() {
		Set<Pair<Material, EntityType>> materialList = new HashSet<>();
		final Map<Integer, Tuple<Material, EntityType, Float>> blockLevelSystem = instance.getConfigManager()
				.levelSystem();
		for (Map.Entry<Integer, Tuple<Material, EntityType, Float>> blockConversion : blockLevelSystem.entrySet()) {
			Material block = blockConversion.getValue().left();
			EntityType entity = blockConversion.getValue().mid();
			if (block != null && block != Material.AIR) {
				materialList.add(Pair.of(block, entity));
			}
		}

		return materialList;
	}

	public int getLevelRank(@NotNull UUID playerID) {
		AtomicInteger rank = new AtomicInteger(-1);
		if (this.levelRankCache.containsKey(playerID)) {
			return this.levelRankCache.get(playerID).intValue();
		}

		Map<UUID, Float> levels = new HashMap<>();
		for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers()) {
			instance.getStorageManager().getOfflineUserData(playerData, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();
						if (ownerUUID != null && playerData.equals(ownerUUID)) {
							float hellblockLevel = offlineUser.getHellblockData().getLevel();
							levels.putIfAbsent(ownerUUID, hellblockLevel);
						}
					});
		}
		LinkedHashMap<UUID, Float> levelsSorted = new LinkedHashMap<>();
		levels.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEach(x -> levelsSorted.put(x.getKey(), x.getValue()));

		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerID);
		if (onlineUser.isEmpty())
			return rank.get();
		if (onlineUser.get().getHellblockData().getOwnerUUID() == null)
			throw new NullPointerException("Owner reference returned null, please report this to the developer.");
		if (instance.getStorageManager().getOnlineUser(onlineUser.get().getHellblockData().getOwnerUUID()) != null
				&& instance.getStorageManager().getOnlineUser(onlineUser.get().getHellblockData().getOwnerUUID()).get()
						.isOnline()) {
			float level = instance.getStorageManager().getOnlineUser(onlineUser.get().getHellblockData().getOwnerUUID())
					.get().getHellblockData().getLevel();
			if (level <= HellblockData.DEFAULT_LEVEL) {
				return rank.get();
			}
		} else {
			instance.getStorageManager().getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
					instance.getConfigManager().lockData()).thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						float level = offlineUser.getHellblockData().getLevel();
						if (level <= HellblockData.DEFAULT_LEVEL) {
							rank.set(-1);
						}
					});
			return rank.get();
		}
		Optional<UUID> position = levelsSorted.reversed().keySet().stream()
				.filter(uuid -> Objects.equals(uuid, onlineUser.get().getHellblockData().getOwnerUUID())).findFirst();
		rank.set(new LinkedList<>(levels.keySet())
				.indexOf(position.orElse(onlineUser.get().getHellblockData().getOwnerUUID())) + 1);
		this.levelRankCache.putIfAbsent(playerID, rank.get());
		return rank.get();
	}

	public LinkedHashMap<UUID, Float> getTopTenHellblocks() {
		if (!this.topCache.isEmpty()) {
			return this.topCache;
		}
		Map<UUID, Float> topHellblocks = new HashMap<>();
		for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers()) {
			instance.getStorageManager().getOfflineUserData(playerData, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();
						float hellblockLevel = offlineUser.getHellblockData().getLevel();
						if (ownerUUID != null && hellblockLevel > HellblockData.DEFAULT_LEVEL) {
							topHellblocks.putIfAbsent(ownerUUID, hellblockLevel);
						}
					});
		}
		LinkedHashMap<UUID, Float> topHellblocksSorted = new LinkedHashMap<>();
		topHellblocks.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEach(x -> topHellblocksSorted.put(x.getKey(), x.getValue()));
		if (this.topCache.isEmpty())
			this.topCache = topHellblocksSorted;
		return topHellblocksSorted;
	}

	public void updateLevelFromBlockChange(@NotNull UUID playerID, @NotNull HellblockData ownerData,
			@NotNull LevelBlockCache cache, boolean placed) {
		if (!cache.isPlacedByPlayer()) {
			return;
		}
		final Map<Integer, Tuple<Material, EntityType, Float>> blockLevelSystem = instance.getConfigManager()
				.levelSystem();
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerID);
		if (onlineUser.isEmpty())
			return;

		if (ownerData.isAbandoned()) {
			instance.getSenderFactory().getAudience(onlineUser.get().getPlayer()).sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
			return;
		}

		for (Map.Entry<Integer, Tuple<Material, EntityType, Float>> blockConversion : blockLevelSystem.entrySet()) {
			Material block = blockConversion.getValue().left();
			EntityType entity = blockConversion.getValue().mid();
			if (block == Material.SPAWNER) {
				if (entity == null)
					return;
			}
			float level = blockConversion.getValue().right();
			if (block != null && level > 0.0F) {
				if (block == cache.getMaterial()) {
					if (level == HellblockData.DEFAULT_LEVEL) {
						if (placed) {
							ownerData.increaseIslandLevel();
						} else {
							ownerData.decreaseIslandLevel();
						}
					} else {
						if (placed) {
							ownerData.addToLevel(level);
						} else {
							ownerData.removeFromLevel(level);
						}
					}
				}
			}
		}
	}

	protected class LevelBlockCache {

		private final Material type;
		private final World world;
		private final int x, y, z;
		private boolean placedByPlayer;

		public LevelBlockCache(@NotNull Material type, @NotNull World world, int x, int y, int z,
				boolean placedByPlayer) {
			this.type = type;
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.placedByPlayer = placedByPlayer;
		}

		public LevelBlockCache(@NotNull Material type, @NotNull World world, int x, int y, int z) {
			this(type, world, x, y, z, false);
		}

		public @NotNull Material getMaterial() {
			return this.type;
		}

		public @NotNull World getWorld() {
			return this.world;
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

		public @NotNull Location getLocation() {
			return new Location(this.world, this.x, this.y, this.z);
		}

		public boolean isPlacedByPlayer() {
			return this.placedByPlayer;
		}

		public void setIfPlacedByPlayer(boolean placedByPlayer) {
			this.placedByPlayer = placedByPlayer;
		}
	}
}