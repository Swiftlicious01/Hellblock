package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;

public class LevelHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final Map<UUID, Set<LevelBlockCache>> placedByPlayerCache = new HashMap<>();

	private final Map<UUID, Integer> levelRankCache = new HashMap<>();
	private LinkedHashMap<UUID, Float> topCache = new LinkedHashMap<>();

	// Cached lookup for block -> level value
	private Map<Pair<Material, EntityType>, Float> levelBlockValues = new HashMap<>();

	private static final Map<String, String> MATERIAL_ALIASES = Map.ofEntries(Map.entry("GRASS", "GRASS_BLOCK"),
			Map.entry("PIG_ZOMBIE_SPAWNER", "ZOMBIFIED_PIGLIN_SPAWNER"), Map.entry("PIG_ZOMBIE", "ZOMBIFIED_PIGLIN"));

	private static final Map<String, String> ENTITY_ALIASES = Map.ofEntries(Map.entry("PIG_ZOMBIE", "ZOMBIFIED_PIGLIN"),
			Map.entry("OCELOT", "CAT") // old to new
	);

	public LevelHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		clearCache();
		loadLevelBlockValues();
		instance.getScheduler().asyncRepeating(() -> {
			this.levelRankCache.clear();
			this.topCache.clear();
		}, 1, 30, TimeUnit.MINUTES);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	private void clearCache() {
		this.placedByPlayerCache.clear();
	}

	public void saveCache(@NotNull UUID id) {
		final Set<LevelBlockCache> cacheSet = this.placedByPlayerCache.get(id);
		if (cacheSet == null || cacheSet.isEmpty()) {
			return;
		}

		// Deduplicate by material+entity+coords+placement
		final List<String> locations = cacheSet.stream().map(this::serializeCache).distinct().toList();

		this.placedByPlayerCache.remove(id);
		if (locations.isEmpty()) {
			return;
		}

		instance.getStorageManager().getOnlineUser(id)
				.ifPresent(user -> user.getLocationCacheData().setLevelBlockLocations(locations));
	}

	public void loadCache(@NotNull UUID id) {
		final Optional<UserData> onlineUserOpt = instance.getStorageManager().getOnlineUser(id);
		if (onlineUserOpt.isEmpty()) {
			return;
		}

		final UserData onlineUser = onlineUserOpt.get();
		final List<String> locations = onlineUser.getLocationCacheData().getLevelBlockLocations();
		if (locations == null || locations.isEmpty()) {
			return;
		}

		final Set<LevelBlockCache> cacheCollection = new HashSet<>();
		for (String raw : locations) {
			final LevelBlockCache cache = deserializeCache(raw);
			if (cache == null) {
				instance.getPluginLogger().warn("Failed to deserialize cache for UUID: %s -> %s".formatted(id, raw));
				continue;
			}

			final Block checkBlock = cache.getWorld().getBlockAt(cache.getX(), cache.getY(), cache.getZ());
			final EntityType checkEntity = (checkBlock.getType() == Material.SPAWNER)
					? ((CreatureSpawner) checkBlock.getState()).getSpawnedType()
					: null;
			if (checkBlock.getType() == cache.getMaterial() && ((checkEntity == null && cache.getEntity() == null)
					|| (checkEntity != null && cache.getEntity() != null && cache.getEntity() == checkEntity))) {
				cacheCollection.add(cache);
			}
		}

		onlineUser.getLocationCacheData().setLevelBlockLocations(new ArrayList<>()); // clear old
		this.placedByPlayerCache.put(id, cacheCollection);
	}

	private @NotNull String serializeCache(@NotNull LevelBlockCache cache) {
		return cache.getMaterial().toString().toUpperCase() + "|"
				+ (cache.getEntity() != null ? cache.getEntity().toString().toUpperCase() : "NONE") + "|"
				+ StringUtils.serializeLoc(cache.getLocation()) + "|" + cache.isPlacedByPlayer();
	}

	private @Nullable LevelBlockCache deserializeCache(@NotNull String cacheString) {
		final String[] parts = cacheString.split(Pattern.quote("|"));
		if (parts.length != 4) {
			return null;
		}
		final Material mat = Material.matchMaterial(parts[0].toUpperCase(Locale.ROOT));
		if (mat == null) {
			return null;
		}

		EntityType ent = null;
		if (!("NONE".equalsIgnoreCase(parts[1].toUpperCase()))) {
			ent = EntityType.fromName(parts[1].toUpperCase());
			if (ent == null) {
				return null;
			}
		}

		final Location loc = StringUtils.deserializeLoc(parts[2]);
		if (loc == null || loc.getWorld() == null) {
			return null;
		}

		final boolean placed = Boolean.parseBoolean(parts[3]);

		return new LevelBlockCache(mat, ent, loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), placed);
	}

	@EventHandler
	public void onLevelPlace(BlockPlaceEvent event) {
		final Block block = event.getBlockPlaced();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}

						final UserData offlineUser = result.get();
						if (!offlineUser.getHellblockData().getIslandMembers().contains(id)) {
							return;
						}

						handleBlockPlacement(id, block, offlineUser);
					});
		});
	}

	@EventHandler
	public void onLevelBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}

						final UserData offlineUser = result.get();
						if (!offlineUser.getHellblockData().getIslandMembers().contains(id)) {
							return;
						}

						handleBlockRemoval(id, block, offlineUser);
					});
		});
	}

	@EventHandler
	public void onLevelExplode(BlockExplodeEvent event) {
		for (Block block : event.blockList()) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
				continue;
			}

			final Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
					.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();

			final Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					playersNearby);
			if (player == null) {
				continue;
			}

			final UUID id = player.getUniqueId();
			instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
				if (ownerUUID == null) {
					return;
				}

				instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
						.thenAccept(result -> {
							if (result.isEmpty()) {
								return;
							}

							final UserData offlineUser = result.get();
							if (!offlineUser.getHellblockData().getIslandMembers().contains(id)) {
								return;
							}

							handleBlockRemoval(id, block, offlineUser);
						});
			});
		}
	}

	@EventHandler
	public void onLevelBurn(BlockBurnEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		final Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25)
				.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();

		final Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
				playersNearby);
		if (player == null) {
			return;
		}

		final UUID id = player.getUniqueId();
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}

						final UserData offlineUser = result.get();
						if (!offlineUser.getHellblockData().getIslandMembers().contains(id)) {
							return;
						}

						handleBlockRemoval(id, block, offlineUser);
					});
		});
	}

	public CompletableFuture<Integer> getLevelRank(@NotNull UUID playerID) {
		final CompletableFuture<Integer> futureRank = new CompletableFuture<>();

		if (this.levelRankCache.containsKey(playerID)) {
			futureRank.complete(this.levelRankCache.get(playerID));
			return futureRank;
		}

		Set<UUID> allUsers = instance.getStorageManager().getDataSource().getUniqueUsers();
		List<CompletableFuture<Optional<UserData>>> futures = allUsers.stream().map(
				uuid -> instance.getStorageManager().getOfflineUserData(uuid, instance.getConfigManager().lockData()))
				.toList();

		CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));

		return allDone.thenApply(__ -> {
			Map<UUID, Float> levels = new HashMap<>();
			futures.forEach(future -> future.join().ifPresent(user -> {
				UUID ownerUUID = user.getHellblockData().getOwnerUUID();
				float level = user.getHellblockData().getLevel();
				if (ownerUUID != null) {
					levels.put(ownerUUID, level);
				}
			}));
			return levels;
		}).thenApply(levels -> {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerID);
			if (onlineUser.isEmpty() || onlineUser.get().getHellblockData().getOwnerUUID() == null) {
				return -1;
			}

			UUID owner = onlineUser.get().getHellblockData().getOwnerUUID();
			Float ownerLevel = levels.get(owner);

			if (ownerLevel == null || ownerLevel <= HellblockData.DEFAULT_LEVEL) {
				return -1;
			}

			List<Map.Entry<UUID, Float>> sorted = levels.entrySet().stream()
					.sorted(Map.Entry.<UUID, Float>comparingByValue().reversed()).toList();

			for (int i = 0; i < sorted.size(); i++) {
				if (sorted.get(i).getKey().equals(owner)) {
					int rank = i + 1;
					this.levelRankCache.put(playerID, rank);
					return rank;
				}
			}
			return -1;
		});
	}

	public CompletableFuture<LinkedHashMap<UUID, Float>> getTopHellblocks(int limit) {
		if (!this.topCache.isEmpty()) {
			return CompletableFuture.completedFuture(this.topCache);
		}

		Set<UUID> allUsers = instance.getStorageManager().getDataSource().getUniqueUsers();
		List<CompletableFuture<Optional<UserData>>> futures = allUsers.stream().map(
				uuid -> instance.getStorageManager().getOfflineUserData(uuid, instance.getConfigManager().lockData()))
				.toList();

		CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));

		return allDone.thenApply(__ -> {
			Map<UUID, Float> levels = new HashMap<>();

			futures.forEach(future -> future.join().ifPresent(user -> {
				UUID ownerUUID = user.getHellblockData().getOwnerUUID();
				float level = user.getHellblockData().getLevel();
				if (ownerUUID != null && level > HellblockData.DEFAULT_LEVEL) {
					levels.put(ownerUUID, level);
				}
			}));

			return levels.entrySet().stream().sorted(Map.Entry.<UUID, Float>comparingByValue().reversed()).limit(limit)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
		}).thenApply(sorted -> {
			this.topCache = sorted;
			return sorted;
		});
	}

	/**
	 * Loads the level system from config into a quick lookup map. Should be called
	 * once on plugin startup / reload. Keep first if duplicate
	 */
	private void loadLevelBlockValues() {
		this.levelBlockValues = new HashMap<>();

		instance.getConfigManager().levelSystem().forEach((id, tuple) -> {
			final Material mat = tuple.left(); // Material from config
			final EntityType ent = tuple.mid(); // Optional entity
			final MathValue<Player> levelValue = tuple.right(); // Level value
			final float level = ((Number) levelValue.evaluate(Context.empty())).floatValue();

			if (mat == null || !mat.isBlock() || level <= 0.0F) {
				return;
			}

			// Normalize Material name
			String matName = mat.name().toUpperCase(Locale.ROOT);

			// Apply alias for materials
			matName = MATERIAL_ALIASES.getOrDefault(matName, matName);

			// Case 1: explicit SPAWNER with entity
			if (mat == Material.SPAWNER && ent != null) {
				final String entName = ENTITY_ALIASES.getOrDefault(ent.name().toUpperCase(Locale.ROOT), ent.name());
				final EntityType resolvedEnt = EntityType.fromName(entName);
				if (resolvedEnt != null) {
					levelBlockValues.put(Pair.of(Material.SPAWNER, resolvedEnt), level);
				}
				return;
			}

			// Case 2: shorthand ENTITY_SPAWNER via material name
			if (matName.endsWith("_SPAWNER") && ent == null) {
				final String entityPart = matName.replace("_SPAWNER", "");
				final String entName = ENTITY_ALIASES.getOrDefault(entityPart, entityPart);
				final EntityType resolvedEnt = EntityType.fromName(entName);
				if (resolvedEnt != null) {
					levelBlockValues.put(Pair.of(Material.SPAWNER, resolvedEnt), level);
					return;
				}
			}

			// Case 3: plain material
			final Material resolvedMat = Material.matchMaterial(matName.toLowerCase(Locale.ROOT));
			if (resolvedMat != null) {
				levelBlockValues.put(Pair.of(resolvedMat, null), level);
				return;
			}

			// Case 4: fallback entity-only
			if (ent != null) {
				final String entName = ENTITY_ALIASES.getOrDefault(ent.name().toUpperCase(Locale.ROOT), ent.name());
				final EntityType resolvedEnt = EntityType.fromName(entName);
				if (resolvedEnt != null) {
					levelBlockValues.put(Pair.of(Material.SPAWNER, resolvedEnt), level);
					return;
				}
			}

			instance.getPluginLogger().warn("Unknown block/entity in level-system entry id=" + id);
		});
	}

	/**
	 * Returns all valid block/entity pairs that count toward island level.
	 */
	public Set<Pair<Material, EntityType>> getLevelBlockList() {
		return new HashSet<>(this.levelBlockValues.keySet());
	}

	public void updateLevelFromBlockChange(@NotNull UUID playerID, @NotNull HellblockData ownerData,
			@NotNull LevelBlockCache cache, boolean placed) {
		if (!cache.isPlacedByPlayer()) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerID);
		if (onlineUser.isEmpty()) {
			return;
		}

		final Player player = onlineUser.get().getPlayer();

		if (ownerData.isAbandoned()) {
			if (player != null) {
				instance.getSenderFactory().wrap(player).sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
			}
			return;
		}

		// Direct lookup instead of looping
		// adjust if spawners use entity
		final Pair<Material, EntityType> key = Pair.of(cache.getMaterial(), cache.getEntity());
		final Float levelValue = levelBlockValues.get(key);

		if (levelValue == null) {
			return; // not a level block
		}

		if (levelValue == HellblockData.DEFAULT_LEVEL) {
			if (placed) {
				ownerData.increaseIslandLevel();
			} else {
				ownerData.decreaseIslandLevel();
			}
		} else {
			if (placed) {
				ownerData.addToLevel(levelValue);
			} else {
				ownerData.removeFromLevel(levelValue);
			}
		}

		if (player != null) {
			instance.getChallengeManager().handleChallengeProgression(player, ActionType.LEVELUP, levelValue);
		}
	}

	private void handleBlockPlacement(UUID id, Block block, UserData offlineUser) {
		final Material material = block.getType();
		final EntityType entity = (material == Material.SPAWNER) ? ((CreatureSpawner) block.getState()).getSpawnedType()
				: null;

		if (!getLevelBlockList().contains(Pair.of(material, entity))) {
			return;
		}

		final LevelBlockCache newCache = new LevelBlockCache(material, entity, block.getWorld(), block.getX(),
				block.getY(), block.getZ(), true // placedByPlayer
		);

		this.placedByPlayerCache.putIfAbsent(id, new HashSet<>());
		final Set<LevelBlockCache> cacheSet = this.placedByPlayerCache.get(id);

		if (!(cacheSet != null && !cacheSet.contains(newCache))) {
			return;
		}
		cacheSet.add(newCache);
		updateLevelFromBlockChange(id, offlineUser.getHellblockData(), newCache, true);
	}

	private void handleBlockRemoval(UUID id, Block block, UserData offlineUser) {
		final Material material = block.getType();
		final EntityType entity = (material == Material.SPAWNER) ? ((CreatureSpawner) block.getState()).getSpawnedType()
				: null;

		if (!getLevelBlockList().contains(Pair.of(material, entity))) {
			return;
		}

		final Set<LevelBlockCache> cacheSet = this.placedByPlayerCache.get(id);
		if (cacheSet == null) {
			return;
		}

		final LevelBlockCache match = cacheSet.stream()
				.filter(c -> c.getMaterial() == material
						&& (entity == null || (entity != null && c.getEntity() != null && c.getEntity() == entity))
						&& c.getX() == block.getX() && c.getY() == block.getY() && c.getZ() == block.getZ())
				.findFirst().orElse(null);

		if (match == null) {
			return;
		}
		updateLevelFromBlockChange(id, offlineUser.getHellblockData(), match, false);
		cacheSet.remove(match);
	}

	protected class LevelBlockCache {

		private final Material type;
		private final EntityType entity;
		private final World world;
		private final int x;
		private final int y;
		private final int z;
		private boolean placedByPlayer;

		public LevelBlockCache(@NotNull Material type, @Nullable EntityType entity, @NotNull World world, int x, int y,
				int z, boolean placedByPlayer) {
			this.type = type;
			this.entity = entity;
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.placedByPlayer = placedByPlayer;
		}

		public LevelBlockCache(@NotNull Material type, @NotNull World world, int x, int y, int z) {
			this(type, null, world, x, y, z, false);
		}

		public @NotNull Material getMaterial() {
			return this.type;
		}

		public @Nullable EntityType getEntity() {
			return this.entity;
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