package com.swiftlicious.hellblock.protection;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sk89q.worldguard.WorldGuard;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

/**
 * Handles all logic related to island protection, including flag changes,
 * entity cleanup, island restoration, and boundary queries. Supports both
 * external and internal protection systems and dynamically switches based on
 * configuration and available integrations.
 * <p>
 * Implements {@link ProtectionManagerInterface} for standardized protection
 * operations and {@link Reloadable} for lifecycle management.
 */
public class ProtectionManager implements ProtectionManagerInterface, Reloadable {

	protected final HellblockPlugin instance;

	public final TreeMap<String, IslandProtection<?>> availableProtection = new TreeMap<>();

	private IslandProtection<?> islandProtection;
	private ProtectionEvents protectionEvents;

	private final static int BATCH_SIZE = 500;

	private final Map<UUID, CompletableFuture<Set<Pos3>>> activeBlockScans = new ConcurrentHashMap<>();
	private final Map<UUID, Set<ChunkPos>> skippedChunksPerIsland = new ConcurrentHashMap<>();

	private final Cache<Integer, Set<ChunkPos>> islandChunksCache = Caffeine.newBuilder()
			.expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(5000).build();

	private boolean worldGuard;

	public ProtectionManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		this.worldGuard = instance.isHookedPluginEnabled("WorldGuard");
		setProtectionFromConfig();
	}

	@Override
	public void unload() {
		this.worldGuard = false;
		activeBlockScans.clear();
		skippedChunksPerIsland.clear();
		islandChunksCache.cleanUp();

	}

	/**
	 * Configures and initializes the island protection system based on the plugin
	 * configuration and available integrations.
	 * <p>
	 * Attempts to use WorldGuard if available and compatible. Falls back to
	 * internal protection if necessary. Also manages the lifecycle of
	 * {@link ProtectionEvents} based on the selected protection method.
	 */
	private void setProtectionFromConfig() {
		instance.debug("Loading protection systems...");

		availableProtection.clear();

		// Try to register WorldGuard protection
		if (worldGuard && instance.getConfigManager().worldguardProtect()) {
			if (WorldGuardHook.isWorking()) {
				if (instance.getIntegrationManager().isHooked("WorldGuard", "7")) {
					instance.debug("Registering WorldGuard island protection.");
					availableProtection.put("worldguard", new WorldGuardHook(instance));
				} else {
					final String version = WorldGuard.getVersion();
					if (!version.startsWith("7.")) {
						instance.getPluginLogger().warn("WorldGuard version must be 7.0 or higher to be usable.");
					}
				}
			} else {
				instance.getPluginLogger().warn("WorldGuard is incompatible with this Minecraft version.");
			}
		}

		// If no external protection was added, register the internal fallback
		if (availableProtection.isEmpty()) {
			instance.debug(
					"External island protection unavailable (disabled in config or unsupported WorldGuard version). Using internal protection.");
			availableProtection.put("internal", new DefaultProtection(instance));
		}

		// Select the active protection
		islandProtection = availableProtection.firstEntry().getValue();
		instance.debug("Active island protection set to: " + islandProtection.getClass().getSimpleName());

		// Only register ProtectionEvents if internal protection is active
		if (islandProtection instanceof DefaultProtection) {
			if (getProtectionEvents() == null) {
				// Only create once, if not already created
				ProtectionEvents events = new ProtectionEvents(instance);
				setProtectionEvents(events);
			}
			getProtectionEvents().reload();
		} else {
			// If not using internal protection, unregister and clean up
			if (getProtectionEvents() != null) {
				getProtectionEvents().unload();
			}
			setProtectionEvents(null);
		}
	}

	/**
	 * Returns the currently active island protection system. This could be an
	 * external integration (like WorldGuard) or the internal fallback.
	 *
	 * @return the active {@link IslandProtection} implementation
	 */
	@NotNull
	public IslandProtection<?> getIslandProtection() {
		return this.islandProtection;
	}

	/**
	 * Returns the currently registered {@link ProtectionEvents} handler. Only used
	 * if internal protection is active.
	 *
	 * @return the current {@link ProtectionEvents} instance, or {@code null} if not
	 *         used
	 */
	@Nullable
	public ProtectionEvents getProtectionEvents() {
		return protectionEvents;
	}

	/**
	 * Sets the {@link ProtectionEvents} instance to be used for internal
	 * protection. This should only be set when internal protection is selected.
	 *
	 * @param events the new {@link ProtectionEvents} instance
	 */
	public void setProtectionEvents(@Nullable ProtectionEvents events) {
		this.protectionEvents = events;
	}

	@Override
	public CompletableFuture<Void> changeProtectionFlag(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId,
			@NotNull HellblockFlag flag) {
		return validateUser(ownerId).thenAccept(userOpt -> {
			userOpt.ifPresent(ownerData -> {
				ownerData.getHellblockData().setProtectionValue(flag);
				islandProtection.changeHellblockFlag(world, ownerData, flag);
			});
		});
	}

	@Override
	public CompletableFuture<Void> changeLockStatus(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		return validateUser(ownerId).thenAccept(userOpt -> {
			userOpt.ifPresent(ownerData -> {
				final HellblockData hellblockData = ownerData.getHellblockData();
				final boolean locked = hellblockData.isLocked();

				final HellblockFlag flag = new HellblockFlag(HellblockFlag.FlagType.ENTRY,
						locked ? HellblockFlag.AccessType.DENY : HellblockFlag.AccessType.ALLOW);

				hellblockData.setProtectionValue(flag);
				instance.debug("Island Lock status changed for user " + ownerData.getName() + " to " + locked);
				islandProtection.lockHellblock(world, ownerData);
			});
		});
	}

	@Override
	public void restoreIsland(@NotNull HellblockData data) {
		final UUID ownerUUID = data.getOwnerUUID();
		if (ownerUUID == null) {
			instance.getPluginLogger()
					.severe("Tried to restore island with null owner UUID (ID: " + data.getIslandId() + ")");
			throw new IllegalStateException("Cannot restore island without a valid owner UUID.");
		}

		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(data.getIslandId()));

		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			throw new IllegalStateException(
					"Could not restore island because its world is missing (ID: " + data.getIslandId() + ")");
		}

		final HellblockWorld<?> world = worldOpt.get();

		// Update abandoned flag in data
		data.setAsAbandoned(false);

		// Reapply protection regions/messages
		islandProtection.updateHellblockMessages(world, ownerUUID);

		// Re-register island ownership with protection handlers
		islandProtection.restoreFlags(world, ownerUUID);

		instance.debug("Island for " + ownerUUID + " restored successfully.");
	}

	/**
	 * Validates that the given UUID corresponds to a valid, non-abandoned Hellblock
	 * island owner.
	 *
	 * @param ownerId the UUID of the potential island owner
	 * @return a CompletableFuture with the validated UserData, or an empty Optional
	 *         if invalid
	 */
	private CompletableFuture<Optional<UserData>> validateUser(@NotNull UUID ownerId) {
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false)
				.thenApplyAsync(userOpt -> userOpt.filter(userData -> {
					HellblockData data = userData.getHellblockData();
					UUID owner = data.getOwnerUUID();

					return data.hasHellblock() && !data.isAbandoned() && owner != null && owner.equals(ownerId);
				}));
	}

	@Override
	public void clearHellblockEntities(@NotNull World world, @NotNull BoundingBox bounds) {
		instance.getScheduler().executeSync(() -> world
				.getEntities().stream().filter(Objects::nonNull).filter(entity -> entity.getType() != EntityType.PLAYER
						&& entity.isValid() && !entity.isDead() && bounds.contains(entity.getLocation().toVector()))
				.forEach(Entity::remove));
	}

	@Nullable
	@Override
	public CompletableFuture<BoundingBox> getHellblockBounds(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		if (world.bukkitWorld() == null || !instance.getHellblockHandler().isInCorrectWorld(world.bukkitWorld())) {
			return CompletableFuture.completedFuture(null);
		}

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).orTimeout(5, TimeUnit.SECONDS)
				.exceptionally(ex -> {
					instance.getPluginLogger()
							.warn("Failed to get bounds for island with owner " + ownerId + ": " + ex.getMessage());
					return Optional.empty();
				}).thenApply(result -> {
					if (result.isEmpty()) {
						return null;
					}

					UserData userData = result.get();
					BoundingBox bounds = userData.getHellblockData().getBoundingBox();

					if (bounds == null || instance.getIslandGenerator().isAnimating(userData.getUUID())
							|| instance.getHellblockHandler().creationProcessing(userData.getUUID())
							|| instance.getHellblockHandler().resetProcessing(userData.getUUID())
							|| instance.getIslandGenerator().isGenerating(userData.getUUID())
							|| instance.getIslandChoiceGUIManager().isGeneratingIsland(userData.getUUID())
							|| instance.getSchematicGUIManager().isGeneratingSchematic(userData.getUUID())) {
						return null;
					}

					return bounds;
				});
	}

	@Override
	public CompletableFuture<Boolean> isInsideIsland(@NotNull UUID ownerId, @NotNull Location location) {
		final World bukkitWorld = location.getWorld();

		// Fail early if world isn't valid
		if (bukkitWorld == null || !instance.getHellblockHandler().isInCorrectWorld(bukkitWorld)) {
			return CompletableFuture.completedFuture(false);
		}

		Optional<HellblockWorld<?>> hellWorld = instance.getWorldManager().getWorld(bukkitWorld);
		if (hellWorld.isEmpty() || hellWorld.get().bukkitWorld() == null) {
			return CompletableFuture.completedFuture(false);
		}

		HellblockWorld<?> world = hellWorld.get();
		return getHellblockBounds(world, ownerId).thenApply(bounds -> {
			if (bounds == null)
				return false;

			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();

			return bounds.contains(x, y, z);
		});
	}

	@Override
	public CompletableFuture<Boolean> isInsideIsland2D(@NotNull UUID ownerId, @NotNull Location location) {
		final World bukkitWorld = location.getWorld();

		// Fail early if world isn't valid
		if (bukkitWorld == null || !instance.getHellblockHandler().isInCorrectWorld(bukkitWorld)) {
			return CompletableFuture.completedFuture(false);
		}

		Optional<HellblockWorld<?>> hellWorld = instance.getWorldManager().getWorld(bukkitWorld);
		if (hellWorld.isEmpty() || hellWorld.get().bukkitWorld() == null) {
			return CompletableFuture.completedFuture(false);
		}

		HellblockWorld<?> world = hellWorld.get();
		return getHellblockBounds(world, ownerId).thenApply(bounds -> {
			if (bounds == null)
				return false;

			double x = location.getX();
			double z = location.getZ();

			return bounds.getMinX() <= x && x <= bounds.getMaxX() && bounds.getMinZ() <= z && z <= bounds.getMaxZ();
		});
	}

	@NotNull
	@Override
	public CompletableFuture<Set<ChunkPos>> getHellblockChunks(@NotNull HellblockWorld<?> world, int islandId) {
		CompletableFuture<Set<ChunkPos>> chunkFuture = new CompletableFuture<>();

		Set<ChunkPos> cached = islandChunksCache.getIfPresent(islandId);
		if (cached != null) {
			chunkFuture.complete(cached);
			return chunkFuture;
		}

		instance.getStorageManager().getOfflineUserDataByIslandId(islandId, false).orTimeout(5, TimeUnit.SECONDS)
				.exceptionally(ex -> {
					instance.getPluginLogger()
							.warn("Failed to get chunks for island " + islandId + ": " + ex.getMessage());
					return Optional.empty();
				}).thenAccept(result -> {
					if (result.isEmpty()) {
						chunkFuture.complete(Collections.emptySet());
						return;
					}

					UserData userData = result.get();
					BoundingBox bounds = userData.getHellblockData().getBoundingBox();
					if (bounds == null || instance.getIslandGenerator().isAnimating(userData.getUUID())
							|| instance.getHellblockHandler().creationProcessing(userData.getUUID())
							|| instance.getHellblockHandler().resetProcessing(userData.getUUID())
							|| instance.getIslandGenerator().isGenerating(userData.getUUID())
							|| instance.getIslandChoiceGUIManager().isGeneratingIsland(userData.getUUID())
							|| instance.getSchematicGUIManager().isGeneratingSchematic(userData.getUUID())) {
						chunkFuture.complete(Collections.emptySet());
						return;
					}

					int minChunkX = (int) Math.floor(bounds.getMinX() / 16.0);
					int maxChunkX = (int) Math.ceil(bounds.getMaxX() / 16.0);
					int minChunkZ = (int) Math.floor(bounds.getMinZ() / 16.0);
					int maxChunkZ = (int) Math.ceil(bounds.getMaxZ() / 16.0);

					preloadIslandBounds(world, bounds, 10, null, 600L, false, 3, 2, () -> {
						Set<ChunkPos> chunkPositions = new HashSet<>();
						for (int cx = minChunkX; cx <= maxChunkX; cx++) {
							for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
								chunkPositions.add(ChunkPos.of(cx, cz));
							}
						}

						islandChunksCache.put(islandId, chunkPositions);
						chunkFuture.complete(chunkPositions);
					});
				});

		return chunkFuture;
	}

	@NotNull
	@Override
	public CompletableFuture<Set<ChunkPos>> getHellblockChunks(@NotNull HellblockWorld<?> world,
			@NotNull UUID ownerId) {
		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).orTimeout(5, TimeUnit.SECONDS)
				.exceptionally(ex -> {
					instance.getPluginLogger()
							.warn("Failed to get chunks for island with owner " + ownerId + ": " + ex.getMessage());
					return Optional.empty();
				}).thenCompose(result -> {
					if (result.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptySet());
					}
					int islandId = result.get().getHellblockData().getIslandId();
					if (islandId <= 0) {
						return CompletableFuture.completedFuture(Collections.emptySet());
					}
					return getHellblockChunks(world, islandId);
				});
	}

	@NotNull
	@Override
	public CompletableFuture<Set<Pos3>> getHellblockBlocks(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId) {
		cancelBlockScan(ownerId);

		final CompletableFuture<Set<Pos3>> blockSupplier = new CompletableFuture<>();
		activeBlockScans.put(ownerId, blockSupplier);
		skippedChunksPerIsland.put(ownerId, ConcurrentHashMap.newKeySet());

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).orTimeout(5, TimeUnit.SECONDS)
				.exceptionally(ex -> {
					instance.getPluginLogger()
							.warn("Failed to get blocks for island with owner " + ownerId + ": " + ex.getMessage());
					return Optional.empty();
				}).thenAccept(result -> {
					if (result.isEmpty()) {
						blockSupplier.complete(Collections.emptySet());
						activeBlockScans.remove(ownerId);
						return;
					}

					final UserData userData = result.get();
					final BoundingBox bounds = userData.getHellblockData().getBoundingBox();
					if (bounds == null || instance.getIslandGenerator().isAnimating(userData.getUUID())
							|| instance.getHellblockHandler().creationProcessing(userData.getUUID())
							|| instance.getHellblockHandler().resetProcessing(userData.getUUID())
							|| instance.getIslandGenerator().isGenerating(userData.getUUID())
							|| instance.getIslandChoiceGUIManager().isGeneratingIsland(userData.getUUID())
							|| instance.getSchematicGUIManager().isGeneratingSchematic(userData.getUUID())) {
						blockSupplier.complete(Collections.emptySet());
						activeBlockScans.remove(ownerId);
						return;
					}

					final int minX = (int) Math.floor(bounds.getMinX());
					final int minY = (int) Math.floor(bounds.getMinY());
					final int minZ = (int) Math.floor(bounds.getMinZ());
					final int maxX = (int) Math.ceil(bounds.getMaxX());
					final int maxY = (int) Math.ceil(bounds.getMaxY());
					final int maxZ = (int) Math.ceil(bounds.getMaxZ());

					Queue<Pos3> positions = new ConcurrentLinkedQueue<>();
					final Set<Pos3> collected = new HashSet<>();

					// Call updated batch method
					preloadIslandBounds(world, bounds, 10, null, 600L, false, 3, 2,
							() -> instance.getScheduler().executeAsync(() -> {
								for (int x = minX; x <= maxX; x++) {
									for (int y = minY; y <= maxY; y++) {
										for (int z = minZ; z <= maxZ; z++) {
											positions.add(new Pos3(x, y, z));
										}
									}
								}
								instance.getScheduler().executeSync(
										() -> processBatch(ownerId, world, positions, collected, blockSupplier));
							}));
				});

		return blockSupplier;
	}

	@NotNull
	public CompletableFuture<List<ChunkPos>> preloadIslandBounds(@NotNull HellblockWorld<?> world,
			@NotNull BoundingBox bounds, int chunksPerTick, @Nullable Consumer<Double> progressCallback,
			long timeoutTicks, boolean verboseLogging, int maxRetries, int retryDelayTicks,
			@Nullable Runnable onCompleteCallback) {

		if (chunksPerTick <= 0) {
			throw new IllegalArgumentException("chunksPerTick must be > 0");
		}

		int minChunkX = (int) Math.floor(bounds.getMinX() / 16.0);
		int maxChunkX = (int) Math.ceil(bounds.getMaxX() / 16.0);
		int minChunkZ = (int) Math.floor(bounds.getMinZ() / 16.0);
		int maxChunkZ = (int) Math.ceil(bounds.getMaxZ() / 16.0);

		Set<ChunkPos> allChunkPositions = new HashSet<>();

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				ChunkPos pos = ChunkPos.of(chunkX, chunkZ);
				if (!world.isChunkLoaded(pos)) {
					allChunkPositions.add(pos);
				}
			}
		}

		if (allChunkPositions.isEmpty()) {
			instance.debug(() -> "preloadIslandBounds: all chunks already loaded for bounds " + bounds);

			if (onCompleteCallback != null) {
				try {
					onCompleteCallback.run();
				} catch (Exception ignored) {
				}
			}

			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		instance.debug(
				() -> "preloadIslandBounds: " + allChunkPositions.size() + " chunks to load for bounds " + bounds);

		Deque<ChunkPos> queue = new ConcurrentLinkedDeque<>(allChunkPositions);
		int totalChunks = queue.size();
		AtomicInteger loadedChunks = new AtomicInteger(0);
		Map<ChunkPos, Integer> failedAttempts = new ConcurrentHashMap<>();
		List<ChunkPos> finalFailures = new CopyOnWriteArrayList<>();
		Map<Class<? extends Throwable>, Integer> failureReasons = new ConcurrentHashMap<>();
		Set<ChunkPos> loadingChunks = ConcurrentHashMap.newKeySet();

		CompletableFuture<List<ChunkPos>> future = new CompletableFuture<>();
		AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

		AtomicBoolean completed = new AtomicBoolean(false);

		Runnable checkCompletion = () -> {
			if (loadedChunks.get() + finalFailures.size() >= totalChunks) {
				if (completed.getAndSet(true)) {
					return; // Already completed once, do nothing
				}

				SchedulerTask task = taskRef.get();
				if (task != null && !task.isCancelled()) {
					task.cancel();
				}

				if (!failureReasons.isEmpty()) {
					instance.debug("preloadIslandBounds: Failure summary:");
					failureReasons
							.forEach((type, count) -> instance.debug("  - " + type.getSimpleName() + ": " + count));
				}

				if (onCompleteCallback != null) {
					try {
						onCompleteCallback.run();
					} catch (Exception ignored) {
					}
				}

				future.complete(finalFailures);
			}
		};

		AtomicInteger lastProgress = new AtomicInteger(-1);

		Runnable tickTask = () -> {
			int processed = 0;

			while (!queue.isEmpty() && processed < chunksPerTick) {
				ChunkPos chunkPos = queue.poll();
				if (chunkPos == null || !loadingChunks.add(chunkPos)) {
					continue;
				}

				try {
					world.getOrCreateChunk(chunkPos).thenCompose(chunk -> chunk.load(true)).thenAccept(success -> {
						if (success) {
							loadedChunks.incrementAndGet();
							if (verboseLogging) {
								instance.debug(() -> "preloadIslandBounds: Loaded chunk " + chunkPos);
							}
						} else {
							instance.debug(() -> "preloadIslandBounds: Failed to load chunk " + chunkPos);
							finalFailures.add(chunkPos);
						}

						loadingChunks.remove(chunkPos);

						if (progressCallback != null) {
							double progress = loadedChunks.get() / (double) totalChunks;
							int newProgress = (int) (progress * 100);
							if (newProgress != lastProgress.getAndSet(newProgress)) {
								progressCallback.accept(progress);
							}
						}

						checkCompletion.run();

					}).exceptionally(ex -> {
						int attempts = failedAttempts.getOrDefault(chunkPos, 0) + 1;
						failedAttempts.put(chunkPos, attempts);
						failureReasons.merge(ex.getClass(), 1, Integer::sum);

						if (attempts <= maxRetries) {
							instance.debug(() -> "Retrying chunk " + chunkPos + " (attempt " + attempts + ")");
							instance.getScheduler().sync().runLater(() -> queue.addLast(chunkPos), retryDelayTicks,
									null);
						} else {
							finalFailures.add(chunkPos);
							instance.getPluginLogger().warn("Chunk permanently failed after retries: " + chunkPos, ex);
						}

						loadingChunks.remove(chunkPos);
						checkCompletion.run();
						return null;
					});

				} catch (Throwable t) {
					finalFailures.add(chunkPos);
					failureReasons.merge(t.getClass(), 1, Integer::sum);
					loadingChunks.remove(chunkPos);
					checkCompletion.run();
				}

				processed++;
			}

			// Optional progress update even if no chunks were processed this tick
			if (progressCallback != null) {
				double progress = loadedChunks.get() / (double) totalChunks;
				int newProgress = (int) (progress * 100);
				if (newProgress != lastProgress.getAndSet(newProgress)) {
					progressCallback.accept(progress);
				}
			}
		};

		SchedulerTask scheduled = instance.getScheduler().sync().runRepeating(tickTask, 0L, 1L, null);
		taskRef.set(scheduled);

		// Timeout
		instance.getScheduler().sync().runLater(() -> {
			if (!future.isDone() && completed.getAndSet(true)) {
				SchedulerTask task = taskRef.get();
				if (task != null && !task.isCancelled()) {
					task.cancel();
				}
				future.completeExceptionally(
						new TimeoutException("Chunk preload exceeded timeout of " + timeoutTicks + " ticks"));
			}
		}, timeoutTicks, null);

		return future;
	}

	/**
	 * Processes a batch of block positions for a Hellblock island to collect
	 * non-air blocks. Operates in batches to avoid overloading the main thread, and
	 * schedules itself until complete.
	 *
	 * @param islandOwnerId   the UUID of the island owner
	 * @param world           the world to scan in
	 * @param positions       the queue of block coordinates to scan (as int[3]
	 *                        arrays)
	 * @param collectedBlocks the set of collected non-air blocks
	 * @param future          the CompletableFuture to complete when the scan is
	 *                        done
	 */
	private void processBatch(UUID ownerId, HellblockWorld<?> world, Queue<Pos3> positions, Set<Pos3> collectedBlocks,
			CompletableFuture<Set<Pos3>> future) {
		if (!activeBlockScans.containsKey(ownerId) || future.isCancelled()) {
			skippedChunksPerIsland.remove(ownerId);
			return;
		}

		int processed = 0;
		AtomicInteger remaining = new AtomicInteger(BATCH_SIZE);

		while (processed < BATCH_SIZE && !positions.isEmpty()) {
			final Pos3 pos = positions.poll();
			world.getBlockState(pos).thenAccept(stateOpt -> {
				if (stateOpt.isPresent() && !stateOpt.get().isAir()) {
					collectedBlocks.add(pos);
				}

				if (!stateOpt.isPresent()) {
					ChunkPos chunkPos = ChunkPos.fromPos3(pos);
					if (!world.isChunkLoaded(chunkPos)) {
						Set<ChunkPos> skippedChunks = skippedChunksPerIsland.get(ownerId);
						if (skippedChunks != null && skippedChunks.add(chunkPos)) {
							instance.debug(() -> "Skipping blocks in chunk " + chunkPos + " because it is not loaded");
						}
					}
				}

				// After processing, schedule next batch if needed
				if (remaining.decrementAndGet() == 0) {
					if (positions.isEmpty()) {
						instance.debug(() -> "Finished block scan for island " + ownerId + ". Collected "
								+ collectedBlocks.size() + " blocks.");
						future.complete(collectedBlocks);
						activeBlockScans.remove(ownerId);
						skippedChunksPerIsland.remove(ownerId);
					} else {
						instance.getScheduler()
								.executeSync(() -> processBatch(ownerId, world, positions, collectedBlocks, future));
					}
				}
			});
		}
	}

	/**
	 * Cancels an in-progress block scan for the given island owner.
	 *
	 * @param islandOwnerId the UUID of the island owner whose scan should be
	 *                      cancelled
	 */
	public void cancelBlockScan(@NotNull UUID ownerId) {
		final CompletableFuture<Set<Pos3>> future = activeBlockScans.remove(ownerId);
		if (future != null && !future.isDone()) {
			// Gracefully complete the future instead of cancelling
			future.complete(Collections.emptySet());
			instance.debug(() -> "cancelBlockScan: gracefully completed block scan for " + ownerId);
		}
	}

	/**
	 * Invalidates the cached chunk data for the specified island. Used when changes
	 * are made to the island that require chunk recalculation.
	 *
	 * @param islandId the island ID to invalidate from the chunk cache
	 */
	public void invalidateIslandChunkCache(int islandId) {
		islandChunksCache.invalidate(islandId);
	}
}