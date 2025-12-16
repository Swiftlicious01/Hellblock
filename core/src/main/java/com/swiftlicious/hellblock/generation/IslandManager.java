package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.listeners.AnimalHandler;
//import com.swiftlicious.hellblock.listeners.FortressHandler;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

/**
 * Manages all island-related runtime logic for the Hellblock plugin.
 *
 * <p>
 * This includes:
 * </p>
 * <ul>
 * <li>Tracking players currently on islands</li>
 * <li>Handling player movement between and within islands</li>
 * <li>Scheduling and managing island-specific tasks (e.g., crop growth,
 * animals, fortress events)</li>
 * <li>Resolving island ownership and IDs based on location</li>
 * <li>Lifecycle management for island creation and deletion</li>
 * </ul>
 *
 * <p>
 * This class also integrates with external managers such as storage, world,
 * farming, and piston systems to maintain consistent island state during
 * gameplay.
 * </p>
 */
public class IslandManager implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private static final long MAX_IDLE_TIME_MILLIS = TimeUnit.MINUTES.toMillis(15); // configurable

	private final Map<Integer, Long> lastIslandActivity = new ConcurrentHashMap<>();

	private final Map<Integer, Long> weatherCooldowns = new ConcurrentHashMap<>();

	// Island tracking
	private final Map<Integer, Set<UUID>> activeIslandPlayers = new HashMap<>();
	private final Map<UUID, Integer> lastKnownIsland = new HashMap<>();
	private final Map<UUID, Pos3> lastTrackedMovement = new HashMap<>();

	/**
	 * Distance squared (~4 blocks) a player must move before updating island
	 * activity
	 **/
	private static final double MOVEMENT_THRESHOLD_SQUARED = 16.0;

	private final Map<Integer, SchedulerTask> cropTasks = new HashMap<>();
//	private final Map<Integer, SchedulerTask> fortressTasks = new HashMap<>();
	private final Map<Integer, SchedulerTask> animalTasks = new HashMap<>();

	public IslandManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		monitorIdleIslands();

		// Restore crop/animal/fortress tasks based on current players
		instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer).filter(Objects::nonNull)
				.forEach(player -> resolveIslandId(player.getLocation()).thenAccept(optIslandId -> {
					if (optIslandId.isPresent()) {
						int islandId = optIslandId.get();
						handlePlayerEnterIsland(player, islandId);
					}
				}));
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		lastTrackedMovement.clear();
		cropTasks.values().stream().filter(Objects::nonNull).filter(task -> !task.isCancelled())
				.forEach(SchedulerTask::cancel);
		cropTasks.clear();
//		fortressTasks.values().stream().filter(Objects::nonNull).filter(task -> !task.isCancelled())
//				.forEach(SchedulerTask::cancel);
//		fortressTasks.clear();
		animalTasks.values().stream().filter(Objects::nonNull).filter(task -> !task.isCancelled())
				.forEach(SchedulerTask::cancel);
		animalTasks.clear();
	}

	public Map<Integer, Long> getWeatherCooldowns() {
		return this.weatherCooldowns;
	}

	/**
	 * Starts a repeating task that checks for idle islands.
	 *
	 * <p>
	 * If an island has no players and hasn't had recent activity, this method stops
	 * lava rain and clears in-memory caches.
	 * </p>
	 */
	public void monitorIdleIslands() {
		instance.getScheduler().asyncRepeating(() -> {
			long now = System.currentTimeMillis();

			for (Map.Entry<Integer, Long> entry : lastIslandActivity.entrySet()) {
				int islandId = entry.getKey();
				long lastActive = entry.getValue();

				if (now - lastActive >= MAX_IDLE_TIME_MILLIS) {
					Set<UUID> players = activeIslandPlayers.getOrDefault(islandId, Collections.emptySet());
					if (!players.isEmpty()) {
						continue; // island is still active
					}

					// Stop weather event
					instance.getNetherWeatherManager().stopWeather(islandId);

					String worldName = instance.getWorldManager().getHellblockWorldFormat(islandId);
					Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(worldName);
					if (worldOpt.isPresent()) {
						getIslandBoundingBox(islandId)
								.thenAccept(box -> box.ifPresent(bounds -> unloadIslandChunks(worldOpt.get(), bounds)));
					}

					// Cancel crop/animal/fortress tasks
					Optional.ofNullable(cropTasks.remove(islandId)).ifPresent(SchedulerTask::cancel);
					Optional.ofNullable(animalTasks.remove(islandId)).ifPresent(SchedulerTask::cancel);
//					Optional.ofNullable(fortressTasks.remove(islandId)).ifPresent(SchedulerTask::cancel);

					// Clear farming and piston caches
					instance.getIslandLevelManager().clearIslandCache(islandId);
					instance.getNetherrackGeneratorHandler().clearIslandPistonCache(islandId);

					lastIslandActivity.remove(islandId); // stop tracking after cleanup
					activeIslandPlayers.remove(islandId); // optional full reset
				}
			}
		}, 0, 5, TimeUnit.MINUTES); // run every 5 minutes
	}

	/**
	 * Retrieves the set of player UUIDs currently present on the specified island.
	 *
	 * @param islandId the ID of the island to query
	 * @return a set of UUIDs representing players on the island, or an empty set if
	 *         none are present
	 */
	@NotNull
	public Set<UUID> getPlayersOnIsland(int islandId) {
		return activeIslandPlayers.getOrDefault(islandId, Collections.emptySet());
	}

	/**
	 * Asynchronously checks whether the given location is within the specified
	 * island.
	 *
	 * @param location the location to check
	 * @param islandId the ID of the island to compare against
	 * @return a CompletableFuture that completes with {@code true} if the location
	 *         belongs to the island, or {@code false} otherwise
	 */
	@NotNull
	public CompletableFuture<Boolean> isInIsland(@NotNull Location location, int islandId) {
		return resolveIslandId(location).thenApply(optId -> optId.isPresent() && optId.get() == islandId);
	}

	/**
	 * Retrieves the last known island ID a player was tracked on.
	 *
	 * <p>
	 * This is based on the player's most recent movement that resulted in an island
	 * transition or update within the server's island tracking system. This data is
	 * used for context-aware logic such as abuse monitoring, session tracking, or
	 * restoring island state.
	 * </p>
	 *
	 * @param playerId the UUID of the player
	 * @return an {@link Optional} containing the last known island ID, or empty if
	 *         not tracked
	 */
	@Nullable
	public Optional<Integer> getLastTrackedIsland(@NotNull UUID playerId) {
		return Optional.ofNullable(lastKnownIsland.get(playerId));
	}

	/**
	 * Handles logic when a player enters an island.
	 *
	 * <p>
	 * This method registers the player as active on the island, loads any necessary
	 * island data (such as placed blocks and piston states), and starts periodic
	 * farming-related tasks such as crop, animal, and fortress updates.
	 * </p>
	 *
	 * @param player   the player entering the island
	 * @param islandId the ID of the island being entered
	 */
	public void handlePlayerEnterIsland(@NotNull Player player, int islandId) {
		UUID uuid = player.getUniqueId();

		// Ensure world is loaded before any island tasks
		instance.getWorldManager().ensureHellblockWorldLoaded(islandId).thenAccept(hellWorld -> {
			if (hellWorld == null || hellWorld.bukkitWorld() == null) {
				instance.getPluginLogger().warn("Failed to load world for island ID " + islandId);
				return;
			}

			String worldName = hellWorld.worldName();

			instance.getScheduler().executeSync(() -> {
				World world = hellWorld.bukkitWorld();
				if (world == null)
					return;

				activeIslandPlayers.computeIfAbsent(islandId, k -> new HashSet<>()).add(uuid);

				// Optional: teleport player if not already in the world
				if (instance.getConfigManager().perPlayerWorlds() && !player.getWorld().getName().equals(worldName)) {
					Location spawn = world.getSpawnLocation();
					ChunkUtils.teleportAsync(player, spawn);
					instance.debug("Teleported player " + player.getName() + " into reloaded world " + worldName);
				}

				instance.getIslandLevelManager().loadIslandPlacedBlocksIfNeeded(islandId);
				instance.getNetherrackGeneratorHandler().loadIslandPistonsIfNeeded(islandId);

				// Schedule crop/animal/fortress updates
				startIslandTasks(islandId);

				// Mark access
				instance.getWorldManager().markWorldAccess(worldName);
			});
		});
	}

	/**
	 * Handles logic when a player leaves an island.
	 *
	 * <p>
	 * This method removes the player from the island's active player list, and if
	 * no players remain on the island, it performs cleanup: saving piston and
	 * placed block states, cancelling running tasks, and clearing relevant caches.
	 * </p>
	 *
	 * @param player   the player leaving the island
	 * @param islandId the ID of the island being left
	 */
	public void handlePlayerLeaveIsland(@NotNull Player player, int islandId) {
		UUID uuid = player.getUniqueId();

		Set<UUID> players = activeIslandPlayers.get(islandId);
		if (players != null) {
			lastTrackedMovement.remove(uuid);
			players.remove(uuid);

			if (players.isEmpty()) {
				Optional<HellblockWorld<?>> hellworldOpt = instance.getWorldManager()
						.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));
				hellworldOpt.ifPresent(world -> {
					// Save pistons and clear cache
					instance.getNetherrackGeneratorHandler().savePistonsByIsland(islandId, world);
					instance.getNetherrackGeneratorHandler().clearIslandPistonCache(islandId);
					instance.getNetherrackGeneratorHandler().getGeneratorManager()
							.cleanupExpiredPistonsByIsland(islandId, world);

					instance.getNetherrackGeneratorHandler().loadIslandPistonsIfNeeded(islandId);

					getIslandBoundingBox(islandId)
							.thenAccept(box -> box.ifPresent(bounds -> unloadIslandChunks(world, bounds)));
				});

				// Save placed blocks and clear cache
				instance.getIslandLevelManager().serializePlacedBlocks(islandId);
				instance.getIslandLevelManager().clearIslandCache(islandId);

				// Cancel crop task
				SchedulerTask cropTask = cropTasks.remove(islandId);
				if (cropTask != null && !cropTask.isCancelled()) {
					cropTask.cancel();
				}

				// Cancel animal task
				SchedulerTask animalTask = animalTasks.remove(islandId);
				if (animalTask != null && !animalTask.isCancelled()) {
					animalTask.cancel();
				}

//				// Cancel fortress task
//				SchedulerTask fortressTask = fortressTasks.remove(islandId);
//				if (fortressTask != null && !fortressTask.isCancelled()) {
//					fortressTask.cancel();
//				}

				// Stop events
				if (instance.getInvasionHandler().isInvasionRunning(islandId))
					instance.getInvasionHandler().endInvasion(islandId);
				if (instance.getSkysiegeHandler().isSkysiegeRunning(islandId))
					instance.getSkysiegeHandler().getSkysiege(islandId).end(false);
				if (instance.getWitherHandler().getCustomWither().hasActiveWither(islandId))
					instance.getWitherHandler().getCustomWither()
							.removeWither(instance.getWitherHandler().getCustomWither().getEnhancedWither(islandId));

				activeIslandPlayers.remove(islandId);

				if (instance.getConfigManager().perPlayerWorlds()) {
					instance.getScheduler().sync().runLater(() -> {
						World world = Bukkit.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));
						if (world != null && world.getPlayers().isEmpty()) {
							instance.debug("Auto-unloading empty per-player world for island ID: " + islandId);
							instance.getWorldManager().unloadWorld(world, false);
							Bukkit.unloadWorld(world, true);
						}
					}, 20L * 120L, LocationUtils.getAnyLocationInstance()); // 2-minute delay (or configurable)
				}
			}
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		handleIslandMovement(event.getPlayer(), event.getTo());
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		handleIslandMovement(event.getPlayer(), event.getTo());
	}

	/**
	 * Handles player movement between islands and within the same island.
	 *
	 * <p>
	 * This method detects when a player moves to a new island, leaves all islands,
	 * or simply moves within their current island. It triggers the appropriate
	 * enter/leave island logic, updates internal tracking, and conditionally
	 * updates player activity if they've moved far enough within the same island.
	 * </p>
	 *
	 * <p>
	 * Also ensures that movement tracking is skipped if the world is invalid or
	 * unchanged.
	 * </p>
	 *
	 * @param player      the player who moved
	 * @param newLocation the new location the player moved to
	 */
	private void handleIslandMovement(@NotNull Player player, @NotNull Location newLocation) {
		UUID uuid = player.getUniqueId();

		resolveIslandId(newLocation).thenAccept(optNewIslandId -> {
			int previousIsland = lastKnownIsland.getOrDefault(uuid, -1);

			if (optNewIslandId.isPresent()) {
				int newIsland = optNewIslandId.get();

				if (previousIsland != newIsland) {
					// Handle island switching
					if (previousIsland != -1) {
						handlePlayerLeaveIsland(player, previousIsland);
					}
					handlePlayerEnterIsland(player, newIsland);
					lastKnownIsland.put(uuid, newIsland);
					lastIslandActivity.put(newIsland, System.currentTimeMillis());
				} else {
					// Same island → check for meaningful movement
					Pos3 currentPos = Pos3.from(newLocation);
					Pos3 lastPos = lastTrackedMovement.get(uuid);

					boolean movedEnough = false;

					if (lastPos == null) {
						movedEnough = true;
					} else {
						World world = newLocation.getWorld();
						if (world == null)
							return;

						Location lastLoc = lastPos.toLocation(world);

						// Check if world changed or moved enough distance
						if (!Objects.equals(lastLoc.getWorld(), newLocation.getWorld())
								|| lastLoc.distanceSquared(newLocation) > MOVEMENT_THRESHOLD_SQUARED) {
							movedEnough = true;
						}
					}

					if (movedEnough) {
						lastTrackedMovement.put(uuid, currentPos);
						lastIslandActivity.put(newIsland, System.currentTimeMillis());

						Optional<UserData> optional = instance.getStorageManager().getCachedUserData(uuid);
						optional.ifPresent(userData -> {
							if (instance.getCooldownManager().shouldUpdateActivity(uuid, 5000)) {
								userData.getHellblockData().updateLastIslandActivity();
							}
						});
					}
				}
			} else {
				// Left all known islands
				if (previousIsland != -1) {
					handlePlayerLeaveIsland(player, previousIsland);
					lastKnownIsland.remove(uuid);

					lastTrackedMovement.remove(uuid);
				}
			}
		});
	}

	/**
	 * Initializes and activates a newly created island for the given player.
	 *
	 * <p>
	 * This method clears any existing cached data and tasks for the island ID,
	 * registers the player as active, loads relevant data such as placed blocks and
	 * pistons, and starts scheduled tasks for crop, animal, and fortress handling.
	 * </p>
	 *
	 * @param ownerId  the player who created or is assigned to the island
	 * @param islandId the ID of the newly created island
	 */
	public void handleIslandCreation(@NotNull UUID ownerId, int islandId) {
		Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));

		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			instance.getPluginLogger().warn("Island creation skipped: World for island ID " + islandId + " not found.");
			return;
		}

		// Clear old player cache and tasks if present
		activeIslandPlayers.remove(islandId);
		Optional.ofNullable(cropTasks.remove(islandId)).ifPresent(SchedulerTask::cancel);
		Optional.ofNullable(animalTasks.remove(islandId)).ifPresent(SchedulerTask::cancel);
//		Optional.ofNullable(fortressTasks.remove(islandId)).ifPresent(SchedulerTask::cancel);

		activeIslandPlayers.computeIfAbsent(islandId, k -> new HashSet<>()).add(ownerId);

		instance.getIslandLevelManager().loadIslandPlacedBlocksIfNeeded(islandId);
		instance.getNetherrackGeneratorHandler().loadIslandPistonsIfNeeded(islandId);

		// Add weather cooldown (5–10 minutes)
		long now = System.currentTimeMillis();
		long delayMinutes = RandomUtils.generateRandomLong(5, 10);
		long nextAllowed = now + TimeUnit.MINUTES.toMillis(delayMinutes);
		weatherCooldowns.put(islandId, nextAllowed);

		getIslandBoundingBox(islandId).thenAccept(box -> {
			if (box.isEmpty())
				return;

			loadExistingIslandChunks(worldOpt.get(), box.get()).thenRun(() -> {
				startIslandTasks(islandId); // only after chunks are loaded
				instance.getNetherWeatherManager().scheduleRandomWeatherForIsland(islandId);
				instance.debug("Restarted all tasks and repopulated caches for created island ID: " + islandId);
			});
		});
	}

	/**
	 * Loads all existing chunks within the specified island bounding box.
	 *
	 * <p>
	 * This method attempts to retrieve and load only the chunks that already exist
	 * in the {@link HellblockWorld}, skipping any missing ones to avoid unintended
	 * generation.
	 * </p>
	 *
	 * <p>
	 * Useful for when a player enters an island and chunk content (e.g., custom
	 * blocks) must be present, but the island should not create new chunks if they
	 * were never generated (e.g., during resets).
	 * </p>
	 *
	 * @param world  the Hellblock world containing the island
	 * @param bounds the bounding box of the island
	 */
	public CompletableFuture<Void> loadExistingIslandChunks(HellblockWorld<?> world, BoundingBox bounds) {
		int minChunkX = (int) Math.floor(bounds.getMinX() / 16.0);
		int maxChunkX = (int) Math.ceil(bounds.getMaxX() / 16.0);
		int minChunkZ = (int) Math.floor(bounds.getMinZ() / 16.0);
		int maxChunkZ = (int) Math.ceil(bounds.getMaxZ() / 16.0);

		List<CompletableFuture<Boolean>> futures = new ArrayList<>();

		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				ChunkPos chunkPos = new ChunkPos(x, z);
				world.getChunk(chunkPos).ifPresent(chunk -> futures.add(chunk.load(true)));
			}
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	/**
	 * Starts scheduled farming-related tasks for the specified island.
	 *
	 * <p>
	 * Tasks include:
	 * </p>
	 * <ul>
	 * <li>Crop updates (asynchronous)</li>
	 * <li>Animal updates (asynchronous)</li>
	 * <li>Fortress structure handling (asynchronous)</li>
	 * </ul>
	 *
	 * <p>
	 * If tasks for the island already exist, they will not be restarted.
	 * </p>
	 *
	 * @param islandId the ID of the island
	 */
	private void startIslandTasks(int islandId) {
		if (!cropTasks.containsKey(islandId)) {
			SchedulerTask cropTask = instance.getScheduler().asyncRepeating(() -> {
				Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
						.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));
				worldOpt.ifPresent(world -> instance.getFarmingManager().updateCrops(world, islandId));
			}, 0, 3, TimeUnit.MINUTES);
			cropTasks.put(islandId, cropTask);
		}

		animalTasks.computeIfAbsent(islandId, id -> instance.getScheduler()
				.asyncRepeating(new AnimalHandler(instance, islandId), 0, 3, TimeUnit.MINUTES));
//		fortressTasks.computeIfAbsent(islandId, id -> instance.getScheduler()
//				.asyncRepeating(new FortressHandler(instance, islandId), 0, 3, TimeUnit.MINUTES));
	}

	/**
	 * Cleans up all data, tasks, and cache related to a deleted island.
	 *
	 * <p>
	 * This includes:
	 * </p>
	 * <ul>
	 * <li>Cancelling crop, animal, and fortress tasks</li>
	 * <li>Removing active players</li>
	 * <li>Ending running events like invasions, skysieges, and withers</li>
	 * <li>Clearing farming, piston, and placed block caches</li>
	 * <li>Clearing lava-grown mushroom and crop data from memory</li>
	 * </ul>
	 *
	 * @param islandId the ID of the island being deleted
	 */
	public void handleIslandDeletion(int islandId) {
		// Cancel and remove crop task
		SchedulerTask cropTask = cropTasks.remove(islandId);
		if (cropTask != null && !cropTask.isCancelled()) {
			cropTask.cancel();
		}

		// Cancel and remove animal task
		SchedulerTask animalTask = animalTasks.remove(islandId);
		if (animalTask != null && !animalTask.isCancelled()) {
			animalTask.cancel();
		}

//		// Cancel and remove fortress task
//		SchedulerTask fortressTask = fortressTasks.remove(islandId);
//		if (fortressTask != null && !fortressTask.isCancelled()) {
//			fortressTask.cancel();
//		}

		// Remove any tracked players
		activeIslandPlayers.remove(islandId);

		instance.getNetherWeatherManager().stopWeather(islandId);

		// Stop events
		if (instance.getInvasionHandler().isInvasionRunning(islandId))
			instance.getInvasionHandler().endInvasion(islandId);
		if (instance.getSkysiegeHandler().isSkysiegeRunning(islandId))
			instance.getSkysiegeHandler().getSkysiege(islandId).end(false);
		if (instance.getWitherHandler().getCustomWither().hasActiveWither(islandId))
			instance.getWitherHandler().getCustomWither()
					.removeWither(instance.getWitherHandler().getCustomWither().getEnhancedWither(islandId));

		// Optional: clear level/farm block caches
		instance.getIslandLevelManager().clearIslandCache(islandId);
		instance.getNetherrackGeneratorHandler().clearIslandPistonCache(islandId);

		Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));

		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			instance.getPluginLogger().warn("Island deletion skipped: World for island ID " + islandId + " not found.");
			return;
		}

		final HellblockWorld<?> world = worldOpt.get();

		instance.getFarmingManager().clearIslandFarmCache(world, islandId);

		instance.debug("Cleaned up all tasks and caches for deleted island ID: " + islandId);
	}

	/**
	 * Unloads all currently loaded chunks within the specified island bounding box.
	 *
	 * <p>
	 * This is typically called when the last player leaves the island or when the
	 * island is considered idle. It only unloads chunks that are already loaded and
	 * skips forced or persistent ones.
	 * </p>
	 *
	 * <p>
	 * This helps reduce memory usage while ensuring persistent data (e.g.,
	 * villagers, pets) remains safe if implemented properly in your chunk system.
	 * </p>
	 *
	 * @param world  the Hellblock world containing the island
	 * @param bounds the bounding box of the island
	 */
	public void unloadIslandChunks(@NotNull HellblockWorld<?> world, @NotNull BoundingBox bounds) {
		int minChunkX = (int) Math.floor(bounds.getMinX() / 16.0);
		int maxChunkX = (int) Math.ceil(bounds.getMaxX() / 16.0);
		int minChunkZ = (int) Math.floor(bounds.getMinZ() / 16.0);
		int maxChunkZ = (int) Math.ceil(bounds.getMaxZ() / 16.0);

		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				ChunkPos chunkPos = new ChunkPos(x, z);
				world.getLoadedChunk(chunkPos).ifPresent(chunk -> chunk.unload(false)); // Do not lazy-unload
			}
		}
	}

	/**
	 * Asynchronously retrieves the {@link BoundingBox} of a specific island.
	 *
	 * <p>
	 * This method searches cached island owner data to find the bounding box
	 * associated with the given island ID. If no match is found or if the bounding
	 * box is missing, the returned {@link Optional} will be empty.
	 * </p>
	 *
	 * @param islandId the ID of the island to retrieve the bounding box for
	 * @return a {@link CompletableFuture} containing the {@link BoundingBox}, or an
	 *         empty {@link Optional} if not found
	 */
	@NotNull
	public CompletableFuture<Optional<BoundingBox>> getIslandBoundingBox(int islandId) {
		return instance.getCoopManager().getCachedIslandOwnerData().thenApply(users -> {
			return users.stream().map(user -> user.getHellblockData())
					.filter(data -> data.getIslandId() == islandId && data.getBoundingBox() != null)
					.map(HellblockData::getBoundingBox).findFirst();
		});
	}

	/**
	 * Resolves the island ID associated with a given location, if any.
	 *
	 * <p>
	 * This method asynchronously checks who owns the location via the CoopManager,
	 * then retrieves their Hellblock user data to obtain the corresponding island
	 * ID.
	 * </p>
	 *
	 * @param location the location to resolve
	 * @return a CompletableFuture that completes with an {@link Optional} island
	 *         ID; empty if no island is associated with the location
	 */
	@NotNull
	public CompletableFuture<Optional<Integer>> resolveIslandId(@NotNull Location location) {
		return instance.getCoopManager().getHellblockOwnerOfLocation(location).thenCompose(ownerUUID -> {
			if (ownerUUID == null)
				return CompletableFuture.completedFuture(Optional.empty());

			return instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenApply(data -> data.map(user -> user.getHellblockData().getIslandId()));
		});
	}

	/**
	 * Asynchronously resolves the island ID associated with a given block position
	 * within a Hellblock world.
	 *
	 * <p>
	 * This method converts the provided {@link Pos3} coordinates into a Bukkit
	 * {@link Location} and delegates to {@link #resolveIslandId(Location)} to
	 * determine which island (if any) the position belongs to.
	 * </p>
	 *
	 * <p>
	 * This is a convenience wrapper that allows systems using internal coordinate
	 * representations ({@link Pos3}) to interact seamlessly with the asynchronous
	 * island resolution logic without needing to manually handle Bukkit
	 * {@link Location} objects.
	 * </p>
	 *
	 * <p>
	 * The returned {@link CompletableFuture} will complete with an {@link Optional}
	 * containing the island ID if one exists at the given position, or an empty
	 * value if the position does not fall within any island boundary.
	 * </p>
	 *
	 * @param world the {@link HellblockWorld} context representing the world the
	 *              position belongs to
	 * @param pos   the {@link Pos3} block position to resolve
	 * @return a {@link CompletableFuture} that completes with an {@link Optional}
	 *         containing the island ID, or an empty {@link Optional} if the
	 *         position is not within an island
	 *
	 * @see #resolveIslandId(Location)
	 * @see Pos3#toLocation(World)
	 */
	@NotNull
	public CompletableFuture<Optional<Integer>> resolveIslandId(@NotNull HellblockWorld<?> world, @NotNull Pos3 pos) {
		Vector vector = pos.toLocation(world.bukkitWorld()).toVector();

		// Load all island owners -> filter by bounding box containing this pos
		return instance.getCoopManager().getCachedIslandOwnerData()
				.thenApply(users -> users.stream().filter(userData -> {
					var data = userData.getHellblockData();
					return data.getIslandId() > 0 && data.getHellblockLocation() != null
							&& data.getHellblockLocation().getWorld().getName().equals(world.worldName())
							&& data.getBoundingBox() != null && data.getBoundingBox().contains(vector);
				}).map(user -> user.getHellblockData().getIslandId()).findFirst());
	}

	/**
	 * Asynchronously retrieves all unique island IDs from cached island owner data.
	 *
	 * <p>
	 * This method uses {@code getCachedIslandOwnerData()} to resolve all user-owned
	 * islands and extracts the unique island IDs. It is typically used for
	 * initializing per-island tasks like lava rain scheduling.
	 * </p>
	 *
	 * @return a {@link CompletableFuture} containing a list of unique island IDs
	 */
	@NotNull
	public CompletableFuture<List<Integer>> getAllIslandIds() {
		return instance.getCoopManager().getCachedIslandOwnerData().thenApply(userDataList -> userDataList.stream()
				.map(user -> user.getHellblockData().getIslandId()).distinct().toList());
	}

	/**
	 * Asynchronously retrieves a list of island IDs that exist in the specified
	 * world.
	 *
	 * <p>
	 * This method filters through all known cached island owners and checks if
	 * their island location is in the target world. It does not block the main
	 * thread.
	 * </p>
	 *
	 * @param worldName the name of the world to check
	 * @return a CompletableFuture containing a list of matching island IDs
	 */
	@NotNull
	public CompletableFuture<List<Integer>> getIslandIdsForWorld(@NotNull String worldName) {
		return instance.getCoopManager().getCachedIslandOwnerData()
				.thenApply(users -> users.stream().filter(userData -> {
					Location loc = userData.getHellblockData().getHellblockLocation();
					return loc != null && loc.getWorld() != null
							&& loc.getWorld().getName().equalsIgnoreCase(worldName);
				}).map(userData -> userData.getHellblockData().getIslandId()).distinct().toList());
	}

	/**
	 * Asynchronously resolves the {@link HellblockWorld} that a given island ID
	 * belongs to.
	 *
	 * <p>
	 * This searches all cached island owner data to find a match for the given
	 * island ID, and retrieves the associated {@link HellblockWorld} via the user's
	 * island location.
	 * </p>
	 *
	 * @param islandId the ID of the island
	 * @return a {@link CompletableFuture} with the resolved {@link HellblockWorld},
	 *         or empty if not found
	 */
	@NotNull
	public CompletableFuture<Optional<HellblockWorld<?>>> getWorldForIsland(int islandId) {
		return instance.getCoopManager().getCachedIslandOwnerData().thenApply(userDataList -> {
			for (UserData userData : userDataList) {
				if (userData.getHellblockData().getIslandId() == islandId) {
					String worldName = userData.getHellblockData().getHellblockLocation().getWorld().getName();
					Optional<?> worldOpt = instance.getWorldManager().getWorld(worldName);

					if (worldOpt.isPresent() && worldOpt.get() instanceof HellblockWorld<?> hellblockWorld) {
						return Optional.of(hellblockWorld);
					}
				}
			}
			return Optional.empty();
		});
	}

	/**
	 * Asynchronously retrieves the center location of the specified island ID.
	 *
	 * <p>
	 * This method scans cached island owners, filters those that match the island
	 * ID, and returns the first valid center location from their bounding box data.
	 * </p>
	 *
	 * @param islandId the ID of the island to find the center for
	 * @return a CompletableFuture containing the island center Location
	 * @throws IllegalStateException if no valid location is found
	 */
	@NotNull
	public CompletableFuture<Location> getIslandCenterLocation(int islandId) {
		return instance.getCoopManager().getCachedIslandOwnerData()
				.thenApply(users -> users.stream().filter(userData -> {
					var data = userData.getHellblockData();
					return data.getIslandId() == islandId && data.getBoundingBox() != null
							&& data.getHellblockLocation() != null && data.getHellblockLocation().getWorld() != null;
				}).map(userData -> {
					var data = userData.getHellblockData();
					return data.getBoundingBox().getCenter().toLocation(data.getHellblockLocation().getWorld());
				}).findFirst().orElseThrow(
						() -> new IllegalStateException("No valid center location found for island ID: " + islandId)));
	}
}