package com.swiftlicious.hellblock.listeners;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
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
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.EntityTypeUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Tuple;
import com.swiftlicious.hellblock.world.BlockPos;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomChunk;
import com.swiftlicious.hellblock.world.CustomSection;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public class LevelHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private YamlDocument levelWorthConfig;

	protected Map<Integer, Tuple<Material, EntityType, MathValue<Player>>> levelSystem = new HashMap<>();

	private SchedulerTask updateCacheTask;
	private SchedulerTask clearCacheTask;

	private final Map<Integer, Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>>> placedBlockCounts = new HashMap<>();
	private final Set<Integer> loadedPlacedBlockCaches = ConcurrentHashMap.newKeySet();

	// Tracks recently counted block positions to prevent place/break exploits
	private final Map<Integer, Map<BlockPosition, Long>> recentPlacements = new ConcurrentHashMap<>();
	private static final long PLACEMENT_COOLDOWN_MS = 30_000; // 30 seconds

	// Mapping Island Id -> Level Rank Placement
	private final Map<Integer, Integer> levelRankCache = new HashMap<>();
	// Mapping Island Id -> Island Level Placement
	private LinkedHashMap<Integer, Float> topCache = new LinkedHashMap<>();

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
		loadLevelWorthConfig();

		final Section levelSystemSection = getLevelWorthConfig().getSection("level-system.blocks");
		if (levelSystemSection != null) {
			int i = 0;
			int spawnerCount = 0;
			int normalCount = 0;
			final Set<String> spawnerEntities = new HashSet<>();

			for (Map.Entry<String, Object> entry : levelSystemSection.getStringRouteMappedValues(false).entrySet()) {
				if (!(entry.getValue() instanceof Number value)) {
					return;
				}
				if (value.doubleValue() <= 0) {
					instance.getPluginLogger().warn("Invalid level worth for " + entry.getKey() + ": must be > 0");
					return;
				}

				final String key = entry.getKey();
				final MathValue<Player> level = MathValue.auto(value);
				final Material material;
				EntityType entity = null;

				if (key.contains(":")) {
					final String[] split = key.split(":");
					material = Material.matchMaterial(split[0].toUpperCase(Locale.ROOT));
					if (material != null && material == Material.SPAWNER) {
						entity = EntityTypeUtils.getCompatibleEntityType(split[1].toUpperCase(Locale.ROOT));
					}
				} else {
					material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
				}

				if (material != null && material.isBlock()) {
					this.levelSystem.putIfAbsent(i++, Tuple.of(material, entity, level));

					if (material == Material.SPAWNER) {
						spawnerCount++;
						if (entity != null) {
							spawnerEntities.add(entity.name());
						}
					} else {
						normalCount++;
					}
				}
			}

			// Build debug message dynamically
			final int total = normalCount + spawnerCount;
			final StringBuilder debugMessage = new StringBuilder(
					"Loaded " + total + " block" + (total == 1 ? "" : "s"));

			final List<String> details = new ArrayList<>();
			if (normalCount > 0) {
				details.add(normalCount + " normal block" + (normalCount == 1 ? "" : "s"));
			}
			if (spawnerCount > 0) {
				String spawnerText = spawnerCount + " spawner" + (spawnerCount == 1 ? "" : "s");
				if (!spawnerEntities.isEmpty()) {
					spawnerText += " [" + String.join(", ", spawnerEntities) + "]";
				}
				details.add(spawnerText);
			}

			if (!details.isEmpty()) {
				debugMessage.append(" (").append(String.join(", ", details)).append(")");
			}

			debugMessage.append(" from level-worth.yml.");

			instance.debug(debugMessage.toString());
		}

		Bukkit.getPluginManager().registerEvents(this, instance);

		this.updateCacheTask = instance.getScheduler().asyncRepeating(this::clearAndUpdateCache, 1, 10,
				TimeUnit.MINUTES);

		loadLevelBlockValues();

		// Preload placed block caches for all existing islands
		instance.getCoopManager().getCachedIslandOwnerData().thenAcceptAsync(allOwners -> {
			if (allOwners == null || allOwners.isEmpty())
				return;

			allOwners.forEach(ownerData -> {
				int islandId = ownerData.getHellblockData().getIslandId();
				if (islandId <= 0)
					return;

				loadIslandPlacedBlocksIfNeeded(islandId);
			});
		});

		this.clearCacheTask = instance.getScheduler().asyncRepeating(() -> {
			this.levelRankCache.clear();
			this.topCache.clear();
		}, 30, 30, TimeUnit.MINUTES);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.levelSystem.clear();
		this.loadedPlacedBlockCaches.clear();
		this.levelRankCache.clear();
		this.topCache.clear();
		this.recentPlacements.clear();
		this.placedBlockCounts.clear();
		if (this.updateCacheTask != null && !this.updateCacheTask.isCancelled()) {
			this.updateCacheTask.cancel();
			this.updateCacheTask = null;
		}
		if (this.clearCacheTask != null && !this.clearCacheTask.isCancelled()) {
			this.clearCacheTask.cancel();
			this.clearCacheTask = null;
		}
	}

	@Override
	public void disable() {
		unload();
		// Save all currently tracked islands
		placedBlockCounts.keySet().forEach(islandId -> {
			Map<String, Map<String, Integer>> serialized = serializePlacedBlocks(islandId);

			instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isPresent()) {
							UserData userData = result.get();
							userData.getLocationCacheData().setPlacedBlocks(serialized);
						}
					});
		});
	}

	@NotNull
	public YamlDocument getLevelWorthConfig() {
		return this.levelWorthConfig;
	}

	private void loadLevelWorthConfig() {
		try (InputStream inputStream = new FileInputStream(
				instance.getConfigManager().resolveConfig("level-worth.yml").toFile())) {
			levelWorthConfig = YamlDocument.create(inputStream,
					instance.getConfigManager().getResourceMaybeGz("level-worth.yml"),
					GeneralSettings.builder().setRouteSeparator('.').setUseDefaults(false).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder().setScalarFormatter((tag, value, role, def) -> {
						if (role == NodeRole.KEY) {
							return ScalarStyle.PLAIN;
						} else {
							return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
						}
					}).build());
			levelWorthConfig.save(instance.getConfigManager().resolveConfig("level-worth.yml").toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Map<Integer, Tuple<Material, EntityType, MathValue<Player>>> getLevelWorthMap() {
		return levelSystem;
	}

	private void clearAndUpdateCache() {
		placedBlockCounts.entrySet().stream().mapToInt(Map.Entry::getKey).forEach(islandId -> {
			Map<String, Map<String, Integer>> serialized = serializePlacedBlocks(islandId);
			// Save to storage — only for island owner
			instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isPresent()) {
							UserData userData = result.get();
							HellblockData hellblockData = userData.getHellblockData();
							UUID ownerId = hellblockData.getOwnerUUID();

							if (ownerId != null && ownerId.equals(userData.getUUID())) {
								userData.getLocationCacheData().setPlacedBlocks(serialized);
							}
						}
					});
		});

		placedBlockCounts.clear();
	}

	public void clearIslandCache(int islandId) {
		// Remove placed block count cache
		placedBlockCounts.remove(islandId);

		// Remove loaded state
		loadedPlacedBlockCaches.remove(islandId);

		// Remove rank cache entries
		instance.getCoopManager().getOwnerUserDataByIslandId(islandId).thenAccept(ownerDataOpt -> {
			if (ownerDataOpt.isEmpty()) {
				return;
			}

			UserData ownerData = ownerDataOpt.get();
			HellblockData data = ownerData.getHellblockData();
			int targetIslandId = data.getIslandId();
			if (targetIslandId > 0) {
				levelRankCache.remove(targetIslandId);
				topCache.remove(targetIslandId);
			}
		});

		instance.debug("Cleared level cache for island ID= " + islandId);
	}

	@EventHandler
	public void onLevelPlace(BlockPlaceEvent event) {
		final Block block = event.getBlockPlaced();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(block).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}

						final UserData ownerData = result.get();
						handleBlockPlacement(block, ownerData.getHellblockData());
					});
		});
	}

	@EventHandler
	public void onLevelBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(block).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}

						final UserData ownerData = result.get();
						handleBlockRemoval(block, ownerData.getHellblockData());
					});
		});
	}

	@EventHandler
	public void onLevelExplode(BlockExplodeEvent event) {
		for (Block block : event.blockList()) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
				continue;
			}

			instance.getCoopManager().getHellblockOwnerOfBlock(block).thenAccept(ownerUUID -> {
				if (ownerUUID == null) {
					return;
				}

				instance.getStorageManager()
						.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
						.thenAccept(result -> {
							if (result.isEmpty()) {
								return;
							}

							final UserData ownerData = result.get();
							handleBlockRemoval(block, ownerData.getHellblockData());
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

		instance.getCoopManager().getHellblockOwnerOfBlock(block).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}

						final UserData ownerData = result.get();
						handleBlockRemoval(block, ownerData.getHellblockData());
					});
		});
	}

	public CompletableFuture<Integer> getLevelRank(int islandId) {
		// Use cache if available
		if (this.levelRankCache.containsKey(islandId)) {
			return CompletableFuture.completedFuture(this.levelRankCache.get(islandId));
		}

		// Step 1: Get all cached island owner data
		return instance.getCoopManager().getCachedIslandOwnerData().thenCompose(allOwners -> {
			if (allOwners == null || allOwners.isEmpty()) {
				return CompletableFuture.completedFuture(-1);
			}

			// Step 2: Collect all valid island levels by island ID
			Map<Integer, Float> levels = new HashMap<>();
			allOwners.forEach(owner -> {
				int id = owner.getHellblockData().getIslandId();
				float level = owner.getHellblockData().getIslandLevel();

				if (level > HellblockData.DEFAULT_LEVEL) {
					levels.put(id, level);
				}
			});

			// Step 3: Get the island's own data from storage
			return instance.getStorageManager().getOfflineUserDataByIslandId(islandId, false).thenApply(userData -> {
				if (userData.isEmpty() || !userData.get().getHellblockData().hasHellblock()
						|| userData.get().getHellblockData().isAbandoned()) {
					return -1;
				}

				int targetIslandId = userData.get().getHellblockData().getIslandId();
				Float targetLevel = levels.get(targetIslandId);

				if (targetLevel == null) {
					return -1;
				}

				// Step 4: Sort all island levels in descending order
				List<Map.Entry<Integer, Float>> sorted = levels.entrySet().stream()
						.sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).toList();

				// Step 5: Find the rank of the target island
				for (int i = 0; i < sorted.size(); i++) {
					if (sorted.get(i).getKey().equals(targetIslandId)) {
						int rank = i + 1;
						this.levelRankCache.put(targetIslandId, rank);
						return rank;
					}
				}

				return -1;
			});
		});
	}

	public CompletableFuture<LinkedHashMap<Integer, Float>> getTopHellblocks(int limit) {
		if (!this.topCache.isEmpty()) {
			return CompletableFuture.completedFuture(this.topCache);
		}

		return instance.getCoopManager().getCachedIslandOwnerData().thenApply(allOwners -> {
			if (allOwners == null || allOwners.isEmpty()) {
				return new LinkedHashMap<>();
			}

			// Collect valid island levels by islandId
			Map<Integer, Float> levels = new HashMap<>();

			allOwners.forEach(owner -> {
				HellblockData data = owner.getHellblockData();
				int islandId = data.getIslandId();
				float level = data.getIslandLevel();
				boolean abandoned = data.isAbandoned();

				if (!abandoned && level > HellblockData.DEFAULT_LEVEL) {
					levels.put(islandId, level);
				}
			});

			// Sort by level descending and limit results
			LinkedHashMap<Integer, Float> sorted = levels.entrySet().stream()
					.sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).limit(limit)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

			// Cache result
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

		getLevelWorthMap().forEach((id, tuple) -> {
			final Material mat = tuple.left(); // Material from config
			final EntityType ent = tuple.mid(); // Optional entity
			final MathValue<Player> levelValue = tuple.right(); // Level value
			final float level = ((Number) levelValue.evaluate(Context.playerEmpty())).floatValue();

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

	public CompletableFuture<Float> recalculateIslandLevel(int islandId) {
		CompletableFuture<Float> recalcFuture = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						recalcFuture.completeExceptionally(new IllegalStateException(
								"Recalculation failed: No user data found for island ID " + islandId));
					}

					Optional<HellblockWorld<?>> hellblockWorldOpt = instance.getWorldManager()
							.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));

					if (hellblockWorldOpt.isEmpty() || hellblockWorldOpt.get().bukkitWorld() == null) {
						recalcFuture.completeExceptionally(new IllegalStateException(
								"Recalculation failed: No world found for island ID " + islandId));
					}

					HellblockWorld<?> world = hellblockWorldOpt.get();

					if (!instance.getHellblockHandler().isInCorrectWorld(world.bukkitWorld())) {
						recalcFuture.completeExceptionally(new IllegalStateException(
								"Recalculation skipped: Not a valid Hellblock world for island " + islandId));
					}

					UserData data = result.get();
					HellblockData hellblockData = data.getHellblockData();

					if (hellblockData.getOwnerUUID() == null) {
						recalcFuture.completeExceptionally(new IllegalStateException(
								"Recalculation failed: Hellblock owner UUID is null for island ID " + islandId));
					}

					// Reset
					placedBlockCounts.remove(islandId);
					Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>> newChunkMap = new HashMap<>();

					instance.getProtectionManager().getHellblockChunks(world, islandId).thenAccept(islandChunks -> {
						List<CompletableFuture<Void>> futures = new ArrayList<>();

						for (ChunkPos chunkPos : islandChunks) {
							Optional<CustomChunk> opt = world.getChunk(chunkPos);
							if (opt.isEmpty())
								continue;

							CustomChunk chunk = opt.get();

							CompletableFuture<Void> future = chunk.load(true).thenAccept(success -> {
								if (!success)
									return;

								Map<BlockKey, Map<BlockPosition, Boolean>> blockCount = new HashMap<>();

								for (CustomSection section : chunk.sections()) {
									for (Map.Entry<BlockPos, CustomBlockState> entry : section.blockMap().entrySet()) {
										BlockPos blockPos = entry.getKey();
										CustomBlockState state = entry.getValue();

										String key = state.type().type().value();
										Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));

										if (material == null || !material.isBlock())
											continue;

										EntityType entity = null;
										if (material == Material.SPAWNER) {
											BinaryTag tag = state.get("SpawnData");
											if (tag instanceof CompoundBinaryTag compound
													&& compound.get("id") instanceof StringBinaryTag idTag) {
												try {
													entity = EntityTypeUtils.getCompatibleEntityType(
															idTag.value().toUpperCase(Locale.ROOT));
												} catch (Exception ignored) {
												}
											}
										}

										Pair<Material, EntityType> keyPair = Pair.of(material, entity);
										if (!levelBlockValues.containsKey(keyPair))
											continue;

										BlockKey blockKey = BlockKey.from(material, entity);
										blockCount.putIfAbsent(blockKey, new HashMap<>());

										Pos3 absolute = blockPos.toPos3(chunkPos);
										// not player placed
										blockCount.get(blockKey).put(
												new BlockPosition(absolute.x(), absolute.y(), absolute.z()), false);
									}
								}

								if (!blockCount.isEmpty()) {
									newChunkMap.put(new ChunkCoord(chunkPos.x(), chunkPos.z()), blockCount);
								}
							});

							futures.add(future);
						}

						CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
							// Done loading all chunks
							placedBlockCounts.put(islandId, newChunkMap);

							float newLevel = 0.0F;
							for (Map<BlockKey, Map<BlockPosition, Boolean>> blockMap : newChunkMap.values()) {
								for (Map.Entry<BlockKey, Map<BlockPosition, Boolean>> entry : blockMap.entrySet()) {
									Float value = levelBlockValues
											.get(Pair.of(entry.getKey().material(), entry.getKey().entity()));
									if (value == null)
										continue;

									for (boolean placed : entry.getValue().values()) {
										if (placed)
											newLevel += value;
									}
								}
							}

							hellblockData.setIslandLevel(newLevel);
							clearIslandCache(islandId);
							instance.getPluginLogger()
									.info("Recalculated level for island ID " + islandId + ": " + newLevel);
							recalcFuture.complete(newLevel);

						}).exceptionally(ex -> {
							recalcFuture.completeExceptionally(ex);
							return null;
						});
					});
				});
		return recalcFuture;
	}

	public Map<String, Map<String, Integer>> serializePlacedBlocks(int islandId) {
		Map<String, Map<String, Integer>> data = new HashMap<>();

		Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>> chunkMap = placedBlockCounts.get(islandId);
		if (chunkMap == null)
			return data;

		chunkMap.entrySet().forEach(chunkEntry -> {
			String chunkKey = chunkEntry.getKey().x() + "," + chunkEntry.getKey().z();
			Map<String, Integer> blockData = new HashMap<>();

			chunkEntry.getValue().entrySet().forEach(blockEntry -> {
				BlockKey key = blockEntry.getKey();
				blockEntry.getValue().entrySet().forEach(posEntry -> {
					// e.g., "IRON_BLOCK|NONE|12,65,9"
					String fullKey = key.toString() + "|" + posEntry.getKey().toString();
					blockData.put(fullKey, posEntry.getValue() ? 1 : 0); // 1 = player placed
				});
			});
			data.put(chunkKey, blockData);
		});

		return data;
	}

	public void deserializePlacedBlocks(int islandId, Map<String, Map<String, Integer>> serialized) {
		Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>> chunkMap = new HashMap<>();

		for (Map.Entry<String, Map<String, Integer>> chunkEntry : serialized.entrySet()) {
			String[] chunkParts = chunkEntry.getKey().split(",");
			if (chunkParts.length != 2)
				continue;

			int chunkX;
			int chunkZ;

			try {
				chunkX = Integer.parseInt(chunkParts[0]);
				chunkZ = Integer.parseInt(chunkParts[1]);
			} catch (NumberFormatException ex) {
				instance.getPluginLogger().warn("Invalid chunk key format: " + chunkEntry.getKey());
				continue; // Skip malformed chunk keys
			}

			ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);

			Map<BlockKey, Map<BlockPosition, Boolean>> blockMap = new HashMap<>();

			for (Map.Entry<String, Integer> blockEntry : chunkEntry.getValue().entrySet()) {
				String[] split = blockEntry.getKey().split("\\|");
				// must include material|entity|x,y,z
				if (split.length < 3)
					continue;

				Material material = Material.matchMaterial(split[0].toUpperCase(Locale.ROOT));
				EntityType entity = (!"NONE".equals(split[1])) ? EntityType.fromName(split[1]) : null;
				BlockPosition pos = BlockPosition.fromString(split[2]);

				boolean placedByPlayer = blockEntry.getValue() == 1;

				if (material != null && pos != null) {
					BlockKey blockKey = new BlockKey(material, entity);
					blockMap.putIfAbsent(blockKey, new HashMap<>());
					blockMap.get(blockKey).put(pos, placedByPlayer);
				}
			}

			chunkMap.put(coord, blockMap);
		}

		placedBlockCounts.put(islandId, chunkMap);
	}

	public void loadIslandPlacedBlocksIfNeeded(int islandId) {
		if (loadedPlacedBlockCaches.contains(islandId))
			return;

		instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
				.thenAccept(optUser -> {
					if (optUser.isEmpty())
						return;

					UserData data = optUser.get();
					Map<String, Map<String, Integer>> placedBlocks = data.getLocationCacheData().getPlacedBlocks();

					if (placedBlocks != null && !placedBlocks.isEmpty()) {
						deserializePlacedBlocks(islandId, placedBlocks);
					}

					loadedPlacedBlockCaches.add(islandId);
					instance.debug("Loaded placed blocks for island ID: " + islandId);
				});
	}

	public void updateLevelFromBlockChange(@NotNull HellblockData ownerData, @NotNull LevelBlockCache cache,
			boolean placed) {
		if (!cache.isPlacedByPlayer()) {
			return;
		}

		if (ownerData.isAbandoned()) {
			return;
		}

		final Pair<Material, EntityType> key = Pair.of(cache.getMaterial(), cache.getEntity());
		final Float levelValue = levelBlockValues.get(key);

		if (levelValue == null)
			return;

		// Ensure cache consistency for this island
		loadIslandPlacedBlocksIfNeeded(ownerData.getIslandId());

		int islandId = ownerData.getIslandId();
		BlockPosition pos = new BlockPosition(cache.getX(), cache.getY(), cache.getZ());

		// get or create island map
		recentPlacements.putIfAbsent(islandId, new ConcurrentHashMap<>());
		Map<BlockPosition, Long> islandPlacements = recentPlacements.get(islandId);

		// clean up old entries
		long now = System.currentTimeMillis();
		islandPlacements.entrySet().removeIf(e -> now - e.getValue() > PLACEMENT_COOLDOWN_MS);

		// if recently placed, ignore this to prevent duping
		if (placed && islandPlacements.containsKey(pos)) {
			return;
		}

		// record new placement
		if (placed) {
			islandPlacements.put(pos, now);
		}

		if (levelValue == HellblockData.DEFAULT_LEVEL) {
			if (placed) {
				ownerData.increaseIslandLevel();
			} else {
				ownerData.decreaseIslandLevel();
			}
		} else {
			if (placed) {
				ownerData.addToIslandLevel(levelValue);
			} else {
				ownerData.removeFromIslandLevel(levelValue);
			}
		}
		clearIslandCache(ownerData.getIslandId());

		if (placed && cache.isPlacedByPlayer()) {
			// Only award progression for a new placement of a player-placed block
			ownerData.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
					.filter(Player::isOnline).forEach(member -> instance.getStorageManager()
							.getOnlineUser(member.getUniqueId()).ifPresent(memberData -> {
								if (instance.getCooldownManager().shouldUpdateActivity(member.getUniqueId(), 5000)) {
									memberData.getHellblockData().updateLastIslandActivity();
								}

								// only trigger challenge progression if this is a new placement event
								instance.getChallengeManager().handleChallengeProgression(memberData,
										ActionType.LEVELUP, levelValue, levelValue.intValue());
							}));
		}
	}

	private void handleBlockPlacement(Block block, HellblockData ownerData) {
		final Material material = block.getType();
		final EntityType entity = (material == Material.SPAWNER) ? ((CreatureSpawner) block.getState()).getSpawnedType()
				: null;

		BlockKey key = BlockKey.from(material, entity);

		if (!getLevelBlockList().contains(Pair.of(material, entity))) {
			return;
		}

		int islandId = ownerData.getIslandId();
		ChunkCoord coord = ChunkCoord.fromLocation(block.getLocation());

		placedBlockCounts.putIfAbsent(islandId, new HashMap<>());
		Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>> islandChunks = placedBlockCounts.get(islandId);
		islandChunks.putIfAbsent(coord, new HashMap<>());
		Map<BlockKey, Map<BlockPosition, Boolean>> blockMap = islandChunks.get(coord);

		blockMap.putIfAbsent(key, new HashMap<>());
		blockMap.get(key).put(BlockPosition.fromLocation(block.getLocation()), true); // true = player placed

		instance.getWorldManager().getWorld(block.getWorld()).ifPresent(world -> updateLevelFromBlockChange(ownerData,
				new LevelBlockCache(material, entity, world, block.getX(), block.getY(), block.getZ(), true), true));
	}

	private void handleBlockRemoval(Block block, HellblockData ownerData) {
		final Material material = block.getType();
		final EntityType entity = (material == Material.SPAWNER) ? ((CreatureSpawner) block.getState()).getSpawnedType()
				: null;

		BlockKey key = BlockKey.from(material, entity);

		if (!getLevelBlockList().contains(Pair.of(material, entity))) {
			return;
		}

		int islandId = ownerData.getIslandId();
		ChunkCoord coord = ChunkCoord.fromLocation(block.getLocation());

		Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>> islandChunks = placedBlockCounts.get(islandId);
		if (islandChunks == null)
			return;

		Map<BlockKey, Map<BlockPosition, Boolean>> blockMap = islandChunks.get(coord);
		if (blockMap == null)
			return;

		BlockPosition pos = BlockPosition.fromLocation(block.getLocation());
		Map<BlockPosition, Boolean> positions = blockMap.get(key);
		if (positions == null || !positions.containsKey(pos))
			return;

		boolean wasPlacedByPlayer = positions.get(pos);
		positions.remove(pos);
		if (positions.isEmpty()) {
			blockMap.remove(key);
		}
		if (blockMap.isEmpty()) {
			islandChunks.remove(coord);
		}

		if (wasPlacedByPlayer) {
			instance.getWorldManager().getWorld(block.getWorld()).ifPresent(world -> updateLevelFromBlockChange(
					ownerData,
					new LevelBlockCache(material, entity, world, block.getX(), block.getY(), block.getZ(), true),
					false));
		}
	}

	protected class LevelBlockCache {
		private final Material type;
		private final EntityType entity;
		private final HellblockWorld<?> world;
		private final int x;
		private final int y;
		private final int z;
		private boolean placedByPlayer;

		public LevelBlockCache(@NotNull Material type, @Nullable EntityType entity, @NotNull HellblockWorld<?> world,
				int x, int y, int z, boolean placedByPlayer) {
			this.type = type;
			this.entity = entity;
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.placedByPlayer = placedByPlayer;
		}

		public LevelBlockCache(@NotNull Material type, @NotNull HellblockWorld<?> world, int x, int y, int z) {
			this(type, null, world, x, y, z, false);
		}

		@NotNull
		public Material getMaterial() {
			return this.type;
		}

		@Nullable
		public EntityType getEntity() {
			return this.entity;
		}

		@NotNull
		public HellblockWorld<?> getWorld() {
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

		@NotNull
		public Pos3 getPosition() {
			return new Pos3(this.x, this.y, this.z);
		}

		public boolean isPlacedByPlayer() {
			return this.placedByPlayer;
		}

		public void setIfPlacedByPlayer(boolean placedByPlayer) {
			this.placedByPlayer = placedByPlayer;
		}
	}

	public record ChunkCoord(int x, int z) {
		public static ChunkCoord fromLocation(Location loc) {
			return new ChunkCoord(loc.getChunk().getX(), loc.getChunk().getZ());
		}
	}

	public record BlockKey(Material material, @Nullable EntityType entity) {
		@Override
		public String toString() {
			return material.name() + "|" + (entity != null ? entity.name() : "NONE");
		}

		public static BlockKey from(Material material, @Nullable EntityType entity) {
			return new BlockKey(material, entity);
		}
	}

	public record BlockPosition(int x, int y, int z) {
		public static BlockPosition fromLocation(Location loc) {
			return new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		}

		@Override
		public String toString() {
			return x + "," + y + "," + z;
		}

		public static BlockPosition fromString(String s) {
			String[] parts = s.split(",");
			if (parts.length != 3)
				throw new IllegalArgumentException("Invalid block position: " + s);
			return new BlockPosition(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]));
		}
	}

	public record LevelProgressContext(double startLevel, double currentLevel) {
	}
}