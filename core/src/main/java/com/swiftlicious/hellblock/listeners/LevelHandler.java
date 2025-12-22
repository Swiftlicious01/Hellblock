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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
import com.swiftlicious.hellblock.events.leaderboard.LeaderboardUpdateEvent;
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

	private final AtomicBoolean isUpdating = new AtomicBoolean(false);

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

		this.updateCacheTask = instance.getScheduler().asyncRepeating(() -> {
			if (isUpdating.get())
				return;

			isUpdating.set(true);
			clearAndUpdateCache().whenComplete((res, ex) -> {
				if (ex != null) {
					instance.getPluginLogger().warn("Cache update failed", ex);
				}
				isUpdating.set(false);
			});
		}, 1, 10, TimeUnit.MINUTES);

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
		this.isUpdating.set(false);
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

	public CompletableFuture<Void> disableSafely() {
		unload();

		List<CompletableFuture<Void>> saveTasks = new ArrayList<>();

		for (int islandId : placedBlockCounts.keySet()) {
			Map<String, Map<String, Integer>> serialized = serializePlacedBlocks(islandId);
			AtomicReference<UUID> lockedUUID = new AtomicReference<>(null);

			CompletableFuture<Void> task = instance.getStorageManager().getOfflineUserDataByIslandId(islandId, true)
					.thenCompose(optData -> {
						if (optData.isEmpty())
							return CompletableFuture.<Void>completedFuture(null);

						UserData userData = optData.get();
						lockedUUID.set(userData.getUUID());

						userData.getLocationCacheData().setPlacedBlocks(serialized);
						return instance.getStorageManager().saveUserData(userData, true).thenApply(x -> null);
					}).handle((result, ex) -> {
						UUID locked = lockedUUID.get();
						if (locked == null)
							return CompletableFuture.<Void>completedFuture(null);

						return instance.getStorageManager().unlockUserData(locked).exceptionally(unlockEx -> {
							instance.getPluginLogger()
									.warn("Failed to unlock user data during shutdown for UUID=" + locked, unlockEx);
							return null;
						});
					}).thenCompose(Function.identity()).exceptionally(ex -> {
						instance.getPluginLogger().severe("disableSafely: Failed to save for islandId=" + islandId, ex);
						return null;
					});

			saveTasks.add(task);
		}

		return CompletableFuture.allOf(saveTasks.toArray(CompletableFuture[]::new));
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

	/**
	 * Returns the level value mapping used for calculating island progression.
	 *
	 * <p>
	 * This map links unique numeric keys (typically representing block/item types)
	 * to a {@link Tuple} consisting of:
	 * <ul>
	 * <li>{@link Material} – the block or item material.</li>
	 * <li>{@link EntityType} – the entity type if applicable (e.g., for spawners),
	 * or {@code null}.</li>
	 * <li>{@link MathValue} – a math expression representing how much this block
	 * contributes to a player's island level.</li>
	 * </ul>
	 *
	 * <p>
	 * This data is used for live level calculation, placement evaluation, and
	 * leaderboard ranking.
	 *
	 * @return a non-null map of all tracked level block values
	 */
	@NotNull
	public Map<Integer, Tuple<Material, EntityType, MathValue<Player>>> getLevelWorthMap() {
		return this.levelSystem;
	}

	/**
	 * Asynchronously serializes and saves all tracked island block data to
	 * persistent storage, then clears the in-memory block placement cache.
	 *
	 * <p>
	 * For each island in the {@code placedBlockCounts} cache:
	 * <ul>
	 * <li>The block data is serialized using
	 * {@link #serializePlacedBlocks(int)}.</li>
	 * <li>The island owner's user data is retrieved and locked via
	 * {@code getOfflineUserDataByIslandId(..., true)}.</li>
	 * <li>If the user is the actual island owner, the serialized data is saved to
	 * their {@code LocationCacheData}.</li>
	 * <li>The user data is then saved and unlocked via {@code saveUserData()} and
	 * {@code unlockUserData()}.</li>
	 * </ul>
	 *
	 * <p>
	 * If any error occurs during the operation, it is logged, and processing
	 * continues for other islands.
	 *
	 * <p>
	 * After initiating all save operations, the method clears the in-memory
	 * {@code placedBlockCounts} cache.
	 *
	 * @return a {@code CompletableFuture<Void>} that completes once all save
	 *         operations and unlocks are finished
	 */
	private CompletableFuture<Void> clearAndUpdateCache() {
		List<CompletableFuture<Boolean>> saveFutures = new ArrayList<>();

		for (int islandId : placedBlockCounts.keySet()) {
			Map<String, Map<String, Integer>> serialized = serializePlacedBlocks(islandId);
			final AtomicReference<UUID> lockedOwnerUUID = new AtomicReference<>(null); // Track what we locked

			CompletableFuture<Boolean> future = instance.getStorageManager()
					.getOfflineUserDataByIslandId(islandId, true).thenCompose(optData -> {
						if (optData.isEmpty()) {
							return CompletableFuture.completedFuture(false);
						}

						UserData ownerData = optData.get();
						HellblockData hellblockData = ownerData.getHellblockData();
						UUID ownerId = hellblockData.getOwnerUUID();
						if (ownerId == null) {
							return CompletableFuture.completedFuture(false);
						}

						lockedOwnerUUID.set(ownerData.getUUID()); // Track who we're locking

						if (hellblockData.isOwner(ownerData.getUUID())) {
							ownerData.getLocationCacheData().setPlacedBlocks(serialized);
							return instance.getStorageManager().saveUserData(ownerData, true);
						}

						return CompletableFuture.completedFuture(false);
					}).handle((result, ex) -> {
						UUID lockedId = lockedOwnerUUID.get(); // Only unlock if we locked
						CompletableFuture<Boolean> unlockFuture;

						if (lockedId != null) {
							unlockFuture = instance.getStorageManager().unlockUserData(lockedId)
									.thenApply(unused -> result != null && result);
						} else {
							unlockFuture = CompletableFuture.completedFuture(result != null && result);
						}

						if (ex != null) {
							instance.getPluginLogger().severe(
									"clearAndUpdateCache: Error clear and updating cache for islandId=" + islandId, ex);
						}

						return unlockFuture;
					}).thenCompose(Function.identity());

			saveFutures.add(future);
		}

		// Clear in-memory cache now or after if you prefer
		placedBlockCounts.clear();

		// Return a future that completes when all saves are done
		return CompletableFuture.allOf(saveFutures.toArray(CompletableFuture[]::new));
	}

	/**
	 * Clears all cached data related to the specified island.
	 *
	 * <p>
	 * This includes:
	 * <ul>
	 * <li>In-memory placed block count data for the island.</li>
	 * <li>The loaded cache state for the placed block map.</li>
	 * <li>The leaderboard rank cache and top leaderboard cache (if the island owner
	 * is found).</li>
	 * </ul>
	 *
	 * <p>
	 * The method:
	 * <ul>
	 * <li>Performs some removals synchronously (e.g., map clearances).</li>
	 * <li>Performs user data lookup asynchronously via
	 * {@code getOwnerUserDataByIslandId()} to remove any associated leaderboard
	 * ranking cache entries.</li>
	 * </ul>
	 *
	 * <p>
	 * If no user data is found, leaderboard caches are skipped. Any exception
	 * during the async portion is logged but will not interrupt the main thread.
	 *
	 * @param islandId the ID of the island whose cache should be cleared
	 * @return a {@code CompletableFuture} that completes when all cache operations
	 *         (including async ones) are finished
	 */
	public CompletableFuture<Void> clearIslandCache(int islandId) {
		// Remove placed block count cache
		placedBlockCounts.remove(islandId);

		// Remove loaded state
		loadedPlacedBlockCaches.remove(islandId);

		// Async: Remove rank cache entries if applicable
		return instance.getCoopManager().getOwnerUserDataByIslandId(islandId).thenAccept(optData -> {
			if (optData.isEmpty())
				return;

			UserData ownerData = optData.get();
			HellblockData data = ownerData.getHellblockData();
			int targetIslandId = data.getIslandId();
			if (targetIslandId > 0) {
				levelRankCache.remove(targetIslandId);
				topCache.remove(targetIslandId);
			}
		}).exceptionally(ex -> {
			instance.getPluginLogger().warn("clearIslandCache: Failed to clear rank cache for islandId=" + islandId,
					ex);
			return null;
		}).thenRun(() -> {
			instance.debug("clearIslandCache: Cleared level cache for islandId= " + islandId);
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onLevelPlace(BlockPlaceEvent event) {
		final Block block = event.getBlockPlaced();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(block).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(null);
			}

			return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return CompletableFuture.completedFuture(null);
				}

				UserData ownerData = optData.get();

				// Now handle block placement and always unlock after
				return handleBlockPlacement(block, ownerData.getHellblockData()).handle((res, ex) -> {
					// Unlock even if an exception occurred
					return instance.getStorageManager().unlockUserData(ownerUUID).thenRun(() -> {
						if (ex != null) {
							instance.getPluginLogger().warn("Failed to handle block place for level updating", ex);
						}
					});
				}).thenCompose(Function.identity()); // Flatten nested future
			});
		}).exceptionally(ex -> {
			// This only catches unexpected top-level errors (e.g., from
			// getHellblockOwnerOfBlock)
			instance.getPluginLogger().warn("Unexpected error in onLevelPlace event", ex);
			return null;
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onLevelBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(block).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(null);
			}

			return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return CompletableFuture.completedFuture(null);
				}

				UserData ownerData = optData.get();

				// Now handle block removal and always unlock after
				return handleBlockRemoval(block, ownerData.getHellblockData()).handle((res, ex) -> {
					// Unlock even if an exception occurred
					return instance.getStorageManager().unlockUserData(ownerUUID).thenRun(() -> {
						if (ex != null) {
							instance.getPluginLogger().warn("Failed to handle block break for level updating", ex);
						}
					});
				}).thenCompose(Function.identity()); // Flatten nested future
			});
		}).exceptionally(ex -> {
			// This only catches unexpected top-level errors (e.g., from
			// getHellblockOwnerOfBlock)
			instance.getPluginLogger().warn("Unexpected error in onLevelBreak event", ex);
			return null;
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onLevelExplode(BlockExplodeEvent event) {
		// Throttle large explosions
		final List<Block> blocks = event.blockList().stream()
				.filter(block -> instance.getHellblockHandler().isInCorrectWorld(block.getWorld())).limit(100).toList();

		if (blocks.isEmpty()) {
			return;
		}

		// Step 1: Map of block -> future<UUID>
		Map<Block, CompletableFuture<UUID>> ownerFutures = new HashMap<>();
		for (Block block : blocks) {
			ownerFutures.put(block, instance.getCoopManager().getHellblockOwnerOfBlock(block));
		}

		// Step 2: Wait for all owner futures to complete
		CompletableFuture.allOf(ownerFutures.values().toArray(CompletableFuture[]::new)).thenCompose(v -> {
			// Step 3: Group blocks by resolved UUID
			Map<UUID, List<Block>> blocksByOwner = new HashMap<>();

			for (Map.Entry<Block, CompletableFuture<UUID>> entry : ownerFutures.entrySet()) {
				try {
					UUID ownerUUID = entry.getValue().getNow(null);
					if (ownerUUID != null) {
						blocksByOwner.computeIfAbsent(ownerUUID, k -> new ArrayList<>()).add(entry.getKey());
					}
				} catch (Exception ex) {
					instance.getPluginLogger().warn("Error resolving block owner for block: " + entry.getKey(), ex);
				}
			}

			// Step 4: Fetch user data + call async handleBlockRemoval per block, then
			// unlock
			List<CompletableFuture<Void>> tasks = new ArrayList<>();

			for (Map.Entry<UUID, List<Block>> entry : blocksByOwner.entrySet()) {
				UUID ownerUUID = entry.getKey();
				List<Block> ownerBlocks = entry.getValue();

				CompletableFuture<Void> task = instance.getStorageManager()
						.getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optData -> {
							if (optData.isEmpty())
								return CompletableFuture.completedFuture(null);

							UserData userData = optData.get();
							HellblockData data = userData.getHellblockData();

							List<CompletableFuture<Boolean>> removals = new ArrayList<>();
							for (Block block : ownerBlocks) {
								try {
									removals.add(handleBlockRemoval(block, data));
								} catch (Exception e) {
									instance.getPluginLogger().warn("Failed to handle block removal in batch", e);
								}
							}

							// Wait for all removals, then unlock
							return CompletableFuture.allOf(removals.toArray(CompletableFuture[]::new))
									.handle((res, ex) -> {
										if (ex != null) {
											instance.getPluginLogger().warn("Error during batch block removal", ex);
										}
										return instance.getStorageManager().unlockUserData(ownerUUID);
									}).thenCompose(Function.identity());
						});

				tasks.add(task);
			}

			// Step 5: Return a future for all user tasks
			return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
		}).exceptionally(ex -> {
			instance.getPluginLogger().warn("Failed to batch process BlockExplodeEvent", ex);
			return null;
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onLevelBurn(BlockBurnEvent event) {
		final Block block = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(block).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(null);
			}

			return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return CompletableFuture.completedFuture(null);
				}

				UserData ownerData = optData.get();

				// Now handle block removal and always unlock after
				return handleBlockRemoval(block, ownerData.getHellblockData()).handle((res, ex) -> {
					// Unlock even if an exception occurred
					return instance.getStorageManager().unlockUserData(ownerUUID).thenRun(() -> {
						if (ex != null) {
							instance.getPluginLogger().warn("Failed to handle block burn for level updating", ex);
						}
					});
				}).thenCompose(Function.identity()); // Flatten nested future
			});
		}).exceptionally(ex -> {
			// This only catches unexpected top-level errors (e.g., from
			// getHellblockOwnerOfBlock)
			instance.getPluginLogger().warn("Unexpected error in onLevelBurn event", ex);
			return null;
		});
	}

	/**
	 * Triggers a leaderboard update event on the main thread using the latest
	 * top-ranked islands. This is performed asynchronously and schedules the event
	 * firing on the sync thread.
	 *
	 * @return A {@code CompletableFuture} that completes when the leaderboard
	 *         update event has been fired.
	 */
	@NotNull
	private CompletableFuture<Boolean> triggerLeaderboardUpdate() {
		return getTopHellblocks(instance.getLeaderboardGUIManager().getTopSlotCount()).thenCompose(topIslands -> {
			return instance.getScheduler().callSyncImmediate(() -> {
				Bukkit.getPluginManager().callEvent(new LeaderboardUpdateEvent(topIslands));
				return true;
			});
		}).exceptionally(ex -> {
			instance.getPluginLogger().warn("Failed to trigger leaderboard update", ex);
			return false;
		});
	}

	/**
	 * Asynchronously retrieves the leaderboard rank of the specified island based
	 * on its level.
	 *
	 * <p>
	 * This method will:
	 * <ul>
	 * <li>Return a cached rank if already computed.</li>
	 * <li>Fetch and filter all cached island owner data to include only those with
	 * valid levels.</li>
	 * <li>Load the target island's user data to determine its current level.</li>
	 * <li>Sort all islands by level in descending order.</li>
	 * <li>Determine the index (rank) of the target island within that sorted
	 * list.</li>
	 * </ul>
	 *
	 * @param islandId the unique ID of the island to rank
	 * @return a {@code CompletableFuture} returning the 1-based rank of the island,
	 *         or -1 if it is not found or does not qualify
	 */
	@NotNull
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

	/**
	 * Retrieves the top N Hellblock islands by level.
	 *
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Returns cached results if available.</li>
	 * <li>Fetches all cached island owner data.</li>
	 * <li>Filters out islands that are abandoned or have the default level.</li>
	 * <li>Sorts the results in descending order by level.</li>
	 * <li>Limits the results to the requested top N entries.</li>
	 * <li>Caches the result for later access.</li>
	 * </ul>
	 *
	 * @param limit the number of top islands to include in the result
	 * @return a {@code CompletableFuture} with a {@code LinkedHashMap} of island
	 *         IDs and levels, ordered descending
	 */
	@NotNull
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
	 * Loads the configuration-defined block values that contribute to island level.
	 *
	 * <p>
	 * This should be called during plugin load or reload to initialize level value
	 * mappings.
	 *
	 * <p>
	 * Supports:
	 * <ul>
	 * <li>Basic block material values (e.g., DIAMOND_BLOCK).</li>
	 * <li>Spawner-specific values using "SPAWNER|ZOMBIE" format.</li>
	 * <li>Aliased material and entity names defined in config for
	 * compatibility.</li>
	 * </ul>
	 *
	 * <p>
	 * Invalid or air blocks, or malformed values, are skipped silently.
	 */
	private void loadLevelBlockValues() {
		this.levelBlockValues = new HashMap<>();

		getLevelWorthMap().forEach((id, tuple) -> {
			final Material mat = tuple.left(); // Material from config
			final EntityType ent = tuple.mid(); // Optional entity
			final MathValue<Player> levelValue = tuple.right(); // Level value
			final float level = ((Number) levelValue.evaluate(Context.playerEmpty())).floatValue();

			if (mat == null || mat.isAir() || !mat.isBlock() || level <= 0.0F) {
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
	 * Retrieves the complete list of valid (Material, EntityType) pairs that
	 * contribute to island level.
	 *
	 * <p>
	 * This reflects the contents of the loaded level block value map, after config
	 * parsing.
	 *
	 * <p>
	 * Each entry may represent:
	 * <ul>
	 * <li>A plain block (e.g., DIAMOND_BLOCK with null entity).</li>
	 * <li>A spawner associated with an entity (e.g., SPAWNER with ZOMBIE).</li>
	 * </ul>
	 *
	 * @return a {@code Set} of {@code Pair<Material, EntityType>} keys used in
	 *         level calculation
	 */
	@NotNull
	public Set<Pair<Material, EntityType>> getLevelBlockList() {
		return new HashSet<>(this.levelBlockValues.keySet());
	}

	/**
	 * Recalculates the island level for the given island ID by scanning all blocks
	 * in its claimed chunks and comparing them to known value mappings.
	 * <p>
	 * This method clears any cached block data for the island, scans the world
	 * chunks, rebuilds the placed block cache, calculates the new level, updates
	 * the user data, clears relevant caches, and triggers the leaderboard update.
	 * </p>
	 *
	 * @param islandId The island ID to recalculate the level for.
	 * @return A {@code CompletableFuture} containing the new level, or -1 if the
	 *         operation fails.
	 */
	@NotNull
	public CompletableFuture<Float> recalculateIslandLevel(int islandId) {
		final AtomicReference<UUID> lockedOwnerUUID = new AtomicReference<>(null); // Track what we locked

		return instance.getStorageManager().getOfflineUserDataByIslandId(islandId, true).thenCompose(optData -> {
			if (optData.isEmpty()) {
				return CompletableFuture.failedFuture(new IllegalStateException(
						"Recalculation failed: No user data found for island ID " + islandId));
			}

			Optional<HellblockWorld<?>> hellblockWorldOpt = instance.getWorldManager()
					.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));

			if (hellblockWorldOpt.isEmpty() || hellblockWorldOpt.get().bukkitWorld() == null) {
				return CompletableFuture.failedFuture(
						new IllegalStateException("Recalculation failed: No world found for island ID " + islandId));
			}

			HellblockWorld<?> world = hellblockWorldOpt.get();

			if (!instance.getHellblockHandler().isInCorrectWorld(world.bukkitWorld())) {
				return CompletableFuture.failedFuture(new IllegalStateException(
						"Recalculation skipped: Not a valid Hellblock world for island " + islandId));
			}

			UserData ownerData = optData.get();
			HellblockData hellblockData = ownerData.getHellblockData();

			if (hellblockData.getOwnerUUID() == null) {
				return CompletableFuture.failedFuture(new IllegalStateException(
						"Recalculation failed: Hellblock owner UUID is null for island ID " + islandId));
			}

			lockedOwnerUUID.set(ownerData.getUUID()); // Track who we're locking

			// Reset
			placedBlockCounts.remove(islandId);
			Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>> newChunkMap = new HashMap<>();

			return instance.getProtectionManager().getHellblockChunks(world, islandId).thenCompose(islandChunks -> {
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

								if (material == null || material.isAir() || !material.isBlock())
									continue;

								EntityType entity = null;
								if (material == Material.SPAWNER) {
									BinaryTag tag = state.get("SpawnData");
									if (tag instanceof CompoundBinaryTag compound
											&& compound.get("id") instanceof StringBinaryTag idTag) {
										try {
											entity = EntityTypeUtils
													.getCompatibleEntityType(idTag.value().toUpperCase(Locale.ROOT));
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
								blockCount.get(blockKey)
										.put(new BlockPosition(absolute.x(), absolute.y(), absolute.z()), false);
							}
						}

						if (!blockCount.isEmpty()) {
							newChunkMap.put(new ChunkCoord(chunkPos.x(), chunkPos.z()), blockCount);
						}
					});

					futures.add(future);
				}

				return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenCompose(v -> {
					// Done loading all chunks
					placedBlockCounts.put(islandId, newChunkMap);

					AtomicReference<Float> newLevel = new AtomicReference<Float>(HellblockData.DEFAULT_LEVEL);
					for (Map<BlockKey, Map<BlockPosition, Boolean>> blockMap : newChunkMap.values()) {
						for (Map.Entry<BlockKey, Map<BlockPosition, Boolean>> entry : blockMap.entrySet()) {
							Float value = levelBlockValues
									.get(Pair.of(entry.getKey().material(), entry.getKey().entity()));
							if (value == null)
								continue;

							for (boolean placed : entry.getValue().values()) {
								if (placed)
									newLevel.updateAndGet(prev -> prev + value);
							}
						}
					}

					hellblockData.setIslandLevel(newLevel.get());
					instance.getPluginLogger()
							.info("Recalculated level for island ID " + islandId + ": " + newLevel.get());
					return instance.getStorageManager().saveUserData(ownerData, true)
							.thenCompose(unused -> clearIslandCache(islandId))
							.thenCompose(unused -> triggerLeaderboardUpdate()).thenApply(vv -> {
								instance.getPluginLogger()
										.info("Recalculated level for island ID " + islandId + ": " + newLevel.get());
								return newLevel.get();
							});
				});
			});
		}).handle((result, ex) -> {
			UUID lockedId = lockedOwnerUUID.get();

			if (lockedId != null) {
				return instance.getStorageManager().unlockUserData(lockedId).handle((unused, unlockEx) -> {
					if (ex != null) {
						instance.getPluginLogger().warn(
								"recalculateIslandLevel: Failed to recalculate island level for islandId=" + islandId,
								ex);
						return -1F; // fallback level
					}
					return result;
				});
			} else {
				// If we didn't lock, just return the result or -1F on failure
				return CompletableFuture.completedFuture(ex != null ? -1F : result);
			}
		}).thenCompose(Function.identity());
	}

	/**
	 * Serializes all tracked placed blocks for the specified island into a
	 * structured map format suitable for storage. This includes chunk coordinates
	 * as keys, and for each chunk, a mapping of block metadata strings
	 * (material|entity|x,y,z) to a flag indicating whether the block was placed by
	 * a player.
	 *
	 * @param islandId The ID of the island whose placed blocks should be
	 *                 serialized.
	 * @return A nested map structure representing placed block data per chunk.
	 */
	@NotNull
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

	/**
	 * Deserializes and restores placed block data from a structured map format back
	 * into the internal placed block cache. This method updates the in-memory
	 * structure used for tracking block placements and player interactions.
	 *
	 * @param islandId   The ID of the island to restore data for.
	 * @param serialized The serialized placed block map (as produced by
	 *                   {@link #serializePlacedBlocks}).
	 */
	public void deserializePlacedBlocks(int islandId, @NotNull Map<String, Map<String, Integer>> serialized) {
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

	/**
	 * Lazily loads the placed block cache for an island from offline user data, if
	 * it hasn't already been loaded. If data exists, it is deserialized and stored
	 * in memory.
	 *
	 * @param islandId The ID of the island to load placed block data for.
	 * @return A {@code CompletableFuture} that completes with {@code true} if data
	 *         was loaded, or {@code false} if already loaded or no data was found.
	 */
	@NotNull
	public CompletableFuture<Boolean> loadIslandPlacedBlocksIfNeeded(int islandId) {
		if (loadedPlacedBlockCaches.contains(islandId))
			return CompletableFuture.completedFuture(false);

		return instance.getStorageManager().getOfflineUserDataByIslandId(islandId, false).thenCompose(optData -> {
			if (optData.isEmpty())
				return CompletableFuture.completedFuture(false);

			UserData ownerData = optData.get();
			Map<String, Map<String, Integer>> placedBlocks = ownerData.getLocationCacheData().getPlacedBlocks();

			if (placedBlocks != null && !placedBlocks.isEmpty()) {
				deserializePlacedBlocks(islandId, placedBlocks);
			}

			instance.debug("Loaded placed blocks for island ID: " + islandId);
			return CompletableFuture.completedFuture(loadedPlacedBlockCaches.add(islandId));
		});
	}

	/**
	 * Updates the island level based on a single block placement or removal.
	 * <p>
	 * This checks whether the block qualifies for level calculation and whether it
	 * was player-placed, then applies the value delta to the island level. Also
	 * clears island-level cache and updates player challenge progression and
	 * activity stats.
	 * </p>
	 * This method is asynchronous and returns a future that completes once all
	 * async operations (such as leaderboard update or cache reloads) are done.
	 *
	 * @param ownerData The owner of the island whose level should be updated.
	 * @param cache     A cache object representing the block and its metadata.
	 * @param placed    True if this was a block placement; false for block removal.
	 * @return A {@code CompletableFuture} that completes when all update tasks have
	 *         finished.
	 */
	@NotNull
	private CompletableFuture<Boolean> updateLevelFromBlockChange(@NotNull HellblockData ownerData,
			@NotNull LevelBlockCache cache, boolean placed) {
		if (!cache.isPlacedByPlayer() || ownerData.isAbandoned()) {
			return CompletableFuture.completedFuture(false);
		}

		final Pair<Material, EntityType> key = Pair.of(cache.getMaterial(), cache.getEntity());
		final Float levelValue = levelBlockValues.get(key);
		if (levelValue == null || levelValue.floatValue() <= 0.0F)
			return CompletableFuture.completedFuture(false);

		int islandId = ownerData.getIslandId();
		BlockPosition pos = new BlockPosition(cache.getX(), cache.getY(), cache.getZ());

		// Step 1: load placed blocks (must complete before other work)
		return loadIslandPlacedBlocksIfNeeded(islandId).thenCompose(v -> {
			// Cache cooldown check
			recentPlacements.putIfAbsent(islandId, new ConcurrentHashMap<>());
			Map<BlockPosition, Long> islandPlacements = recentPlacements.get(islandId);

			long now = System.currentTimeMillis();
			islandPlacements.entrySet().removeIf(e -> now - e.getValue() > PLACEMENT_COOLDOWN_MS);

			if (placed && islandPlacements.containsKey(pos)) {
				return CompletableFuture.completedFuture(false); // skip duplicate
			}
			if (placed) {
				islandPlacements.put(pos, now);
			}

			// Apply level change
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

			// Step 2: clear cache
			return clearIslandCache(islandId).thenCompose(vv -> {
				// Step 3: handle member activity/challenges
				if (placed && cache.isPlacedByPlayer()) {
					for (UUID uuid : ownerData.getPartyPlusOwner()) {
						Player player = Bukkit.getPlayer(uuid);
						if (player != null && player.isOnline()) {
							instance.getStorageManager().getOnlineUser(uuid).ifPresent(memberData -> {
								if (instance.getCooldownManager().shouldUpdateActivity(uuid, 5000)) {
									memberData.getHellblockData().updateLastIslandActivity();
								}
								instance.getChallengeManager().handleChallengeProgression(memberData,
										ActionType.LEVELUP, levelValue, levelValue.intValue());
							});
						}
					}
				}

				// Step 4: trigger leaderboard update
				return triggerLeaderboardUpdate();
			});
		});
	}

	/**
	 * Handles the placement of a block on an island and updates the placed block
	 * cache accordingly. If the block is tracked for level progression, it marks it
	 * as player-placed and triggers level recalculation via
	 * {@code updateLevelFromBlockChange}.
	 *
	 * @param block     The placed block.
	 * @param ownerData The owner of the island where the block was placed.
	 * @return A {@code CompletableFuture} that completes when level updates (if
	 *         any) are done.
	 */
	@NotNull
	private CompletableFuture<Boolean> handleBlockPlacement(@NotNull Block block, @NotNull HellblockData ownerData) {
		final Material material = block.getType();
		final EntityType entity = (material == Material.SPAWNER) ? ((CreatureSpawner) block.getState()).getSpawnedType()
				: null;

		BlockKey key = BlockKey.from(material, entity);

		if (!getLevelBlockList().contains(Pair.of(material, entity))) {
			return CompletableFuture.completedFuture(false);
		}

		int islandId = ownerData.getIslandId();
		ChunkCoord coord = ChunkCoord.fromLocation(block.getLocation());

		placedBlockCounts.putIfAbsent(islandId, new HashMap<>());
		Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>> islandChunks = placedBlockCounts.get(islandId);
		islandChunks.putIfAbsent(coord, new HashMap<>());
		Map<BlockKey, Map<BlockPosition, Boolean>> blockMap = islandChunks.get(coord);

		blockMap.putIfAbsent(key, new HashMap<>());
		blockMap.get(key).put(BlockPosition.fromLocation(block.getLocation()), true); // true = player placed

		Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(block.getWorld());
		if (worldOpt.isEmpty()) {
			return CompletableFuture.completedFuture(false);
		}

		LevelBlockCache cache = new LevelBlockCache(material, entity, worldOpt.get(), block.getX(), block.getY(),
				block.getZ(), true);
		return updateLevelFromBlockChange(ownerData, cache, true);
	}

	/**
	 * Handles the removal of a block on an island and updates the placed block
	 * cache accordingly. If the block was player-placed and is tracked for level
	 * progression, it removes the block from cache and triggers a level
	 * recalculation via {@code updateLevelFromBlockChange}.
	 *
	 * @param block     The removed block.
	 * @param ownerData The owner of the island where the block was removed.
	 * @return A {@code CompletableFuture} that completes when level updates (if
	 *         any) are done.
	 */
	@NotNull
	private CompletableFuture<Boolean> handleBlockRemoval(@NotNull Block block, @NotNull HellblockData ownerData) {
		final Material material = block.getType();
		final EntityType entity = (material == Material.SPAWNER) ? ((CreatureSpawner) block.getState()).getSpawnedType()
				: null;

		BlockKey key = BlockKey.from(material, entity);

		if (!getLevelBlockList().contains(Pair.of(material, entity))) {
			return CompletableFuture.completedFuture(false);
		}

		int islandId = ownerData.getIslandId();
		ChunkCoord coord = ChunkCoord.fromLocation(block.getLocation());

		Map<ChunkCoord, Map<BlockKey, Map<BlockPosition, Boolean>>> islandChunks = placedBlockCounts.get(islandId);
		if (islandChunks == null)
			return CompletableFuture.completedFuture(false);

		Map<BlockKey, Map<BlockPosition, Boolean>> blockMap = islandChunks.get(coord);
		if (blockMap == null)
			return CompletableFuture.completedFuture(false);

		BlockPosition pos = BlockPosition.fromLocation(block.getLocation());
		Map<BlockPosition, Boolean> positions = blockMap.get(key);
		if (positions == null || !positions.containsKey(pos))
			return CompletableFuture.completedFuture(false);

		boolean wasPlacedByPlayer = positions.get(pos);
		positions.remove(pos);
		if (positions.isEmpty()) {
			blockMap.remove(key);
		}

		if (blockMap.isEmpty()) {
			islandChunks.remove(coord);
		}

		if (!wasPlacedByPlayer)
			return CompletableFuture.completedFuture(false);

		Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(block.getWorld());
		if (worldOpt.isEmpty()) {
			return CompletableFuture.completedFuture(false);
		}

		LevelBlockCache cache = new LevelBlockCache(material, entity, worldOpt.get(), block.getX(), block.getY(),
				block.getZ(), true);
		return updateLevelFromBlockChange(ownerData, cache, false);
	}

	/**
	 * Represents a cached block snapshot used for level progression or structure
	 * analysis.
	 * <p>
	 * Contains metadata such as block type, optional entity type (for block-entity
	 * combos), world reference, coordinates, and whether the block was placed by a
	 * player.
	 * <p>
	 * Used to track and analyze placed structures for player island progression or
	 * event triggers.
	 */
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

	/**
	 * Represents the X and Z coordinates of a Minecraft chunk.
	 * <p>
	 * Used to group blocks or cache data at the chunk level for optimization
	 * purposes.
	 */
	private record ChunkCoord(int x, int z) {
		@NotNull
		public static ChunkCoord fromLocation(@NotNull Location location) {
			return new ChunkCoord(location.getChunk().getX(), location.getChunk().getZ());
		}
	}

	/**
	 * Represents a unique identifier for a block, optionally including an
	 * associated entity type.
	 * <p>
	 * Useful for categorizing or counting placed blocks that may have different
	 * variations (e.g., spawners with specific entities).
	 */
	private record BlockKey(@NotNull Material material, @Nullable EntityType entity) {
		@Override
		public String toString() {
			return material.name() + "|" + (entity != null ? entity.name() : "NONE");
		}

		@NotNull
		public static BlockKey from(@NotNull Material material, @Nullable EntityType entity) {
			return new BlockKey(material, entity);
		}
	}

	/**
	 * Represents the X, Y, Z coordinates of a block in the world.
	 * <p>
	 * Can be serialized to and from a string format for storage or comparison
	 * purposes.
	 */
	private record BlockPosition(int x, int y, int z) {
		@NotNull
		public static BlockPosition fromLocation(@NotNull Location location) {
			return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		}

		@Override
		public String toString() {
			return x + "," + y + "," + z;
		}

		@NotNull
		public static BlockPosition fromString(@NotNull String s) {
			String[] parts = s.split(",");
			if (parts.length != 3)
				throw new IllegalArgumentException("Invalid block position: " + s);
			return new BlockPosition(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]));
		}
	}

	/**
	 * Represents the progress of an island from a starting level to the current
	 * level.
	 * <p>
	 * Typically used for measuring how far a player has progressed towards
	 * challenge progression.
	 */
	public record LevelProgressContext(double startLevel, double currentLevel) {
	}
}