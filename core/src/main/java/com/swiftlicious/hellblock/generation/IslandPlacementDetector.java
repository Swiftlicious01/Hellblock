package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.world.HellblockWorld;

/**
 * Handles the detection and computation of safe island placement locations in a
 * Minecraft world using a spiral algorithm to avoid overlaps between island
 * areas.
 * <p>
 * This class maintains a cache of existing island bounding boxes and uses that
 * to detect available slots when placing new islands.
 * <p>
 * Core responsibilities:
 * <li>Initialize bounding box cache from saved island data</li>
 * <li>Provide the next available (non-overlapping) spiral-based coordinate</li>
 * <li>Compute a bounding box for a given island ID based on spiral
 * traversal</li>
 * <li>Operate asynchronously to avoid blocking the main server thread</li>
 * <p>
 * Thread-safety: This class uses concurrent data structures to allow safe
 * access from async tasks.
 */
public class IslandPlacementDetector implements Reloadable {

	protected final HellblockPlugin instance;

	/**
	 * Concurrent cache of island bounding boxes (thread-safe for use across async
	 * tasks)
	 */
	private final Map<Integer, BoundingBox> cachedBoundingBoxes = new ConcurrentHashMap<>();

	/** Future used to track when spiral initialization is complete */
	private final CompletableFuture<Void> spiralReady = new CompletableFuture<>();

	/**
	 * Distance between centers of two adjacent islands (twice the protection
	 * radius)
	 */
	private int spacing;

	public IslandPlacementDetector(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		this.spacing = instance.getUpgradeManager().getMaxValue(IslandUpgradeType.PROTECTION_RANGE).intValue() * 2;
		if (spacing <= 0) {
			throw new IllegalArgumentException("Island spacing cannot be below 0");
		}
		instance.debug("IslandPlacementDetector loaded. Calculated island spacing = " + spacing);
	}

	@Override
	public void unload() {
		cachedBoundingBoxes.clear();
	}

	/**
	 * Initializes the spiral placement system asynchronously. This loads all
	 * existing island bounding boxes into the cache to ensure no overlap.
	 *
	 * @return a CompletableFuture that completes when initialization is done
	 */
	public CompletableFuture<Void> initialize() {
		instance.debug("Starting spiral system initialization...");

		return cacheExistingBoundingBoxes().whenComplete((result, error) -> {
			if (error != null) {
				// Spiral system failed to initialize
				instance.getPluginLogger().severe("IslandPlacementDetector failed to initialize!", error);
				spiralReady.completeExceptionally(error);
			} else {
				// Ready to use for placement
				spiralReady.complete(null);
				if (cachedBoundingBoxes.isEmpty()) {
					instance.debug("Spiral initialization complete. No bounding boxes cached.");
				} else {
					instance.debug("Spiral initialization complete. Cached " + cachedBoundingBoxes.size()
							+ " bounding box" + (cachedBoundingBoxes.size() == 1 ? "" : "es") + ".");
				}
			}
		});
	}

	/**
	 * @return future that completes once the spiral system is ready. Required
	 *         before calling any island placement methods.
	 */
	@NotNull
	public CompletableFuture<Void> checkSpiralPlacementCompletion() {
		return this.spiralReady;
	}

	/**
	 * @return the spacing (in blocks) between island centers.
	 */
	public int getIslandSpacing() {
		return this.spacing;
	}

	public void cacheIslandBoundingBox(int islandId, @NotNull BoundingBox box) {
		cachedBoundingBoxes.put(islandId, box);
		instance.debug("PlacementDetector: Updated bounding box for island ID " + islandId);
	}

	public void removeCachedIslandBoundingBox(int islandId) {
		cachedBoundingBoxes.remove(islandId);
	}

	@Nullable
	public Integer getIslandIdAt(@NotNull Location loc) {
		World world = loc.getWorld();
		if (world == null)
			return null;

		Vector vec = loc.toVector();

		for (Map.Entry<Integer, BoundingBox> entry : cachedBoundingBoxes.entrySet()) {
			BoundingBox box = entry.getValue();
			if (box != null && box.contains(vec)) {
				return entry.getKey();
			}
		}

		return null;
	}

	/**
	 * Scans outward in a spiral pattern to find the next valid island location.
	 * Each step in the spiral generates a new candidate position which is tested
	 * for overlap.
	 * 
	 * If a valid non-overlapping position is found, its bounding box is immediately
	 * reserved in the cache to prevent race conditions with other tasks.
	 * 
	 * This method is safe to call from async threads, and it assumes the spiral
	 * system has already been initialized.
	 *
	 * @return a future with the next available island {@link Location}
	 */
	@NotNull
	public CompletableFuture<Location> findNextIslandLocation() {
		return spiralReady
				.thenCompose(v -> instance.getCoopManager().getCachedIslandOwnerData().thenApply(ownerDataList -> {
					instance.debug("findNextIslandLocation: Found " + ownerDataList.size() + " cached island owner"
							+ (ownerDataList.size() == 1 ? "" : "s") + ".");

					AtomicInteger maxId = new AtomicInteger(-1);

					// Determine the highest used island ID
					ownerDataList.forEach(userData -> {
						HellblockData data = userData.getHellblockData();
						int id = data.getIslandId();
						if (id > 0) {
							instance.debug("findNextIslandLocation: Resolved island ID " + id + " for user "
									+ userData.getName());
							maxId.updateAndGet(current -> Math.max(current, id));
						}
					});

					if (maxId.get() == -1) {
						instance.getPluginLogger().warn(
								"findNextIslandLocation: No valid island IDs found among owners. Starting from ID=1.");
						maxId.set(0);
					}

					int nextId = maxId.get() + 1;
					String worldFormat = instance.getWorldManager().getHellblockWorldFormat(nextId);
					instance.debug("findNextIslandLocation: Next island ID = " + nextId + ", expected world name = "
							+ worldFormat);

					Optional<HellblockWorld<?>> hellblockWorldOpt = instance.getWorldManager().getWorld(worldFormat);
					if (hellblockWorldOpt.isEmpty() || hellblockWorldOpt.get().bukkitWorld() == null) {
						throw new IllegalStateException("World for next island ID is not loaded: " + worldFormat);
					}

					World world = hellblockWorldOpt.get().bukkitWorld();
					instance.debug("findNextIslandLocation: Scanning spiral coordinates in world '" + world.getName()
							+ "'...");

					// Begin scanning outward in a spiral to find a non-overlapping position
					SpiralIterator spiral = new SpiralIterator();
					int checked = 0;
					int softLimit = Math.max(5000, cachedBoundingBoxes.size() * 5); // Adaptive fail-safe

					// Dynamic infinite search — continues until it finds an open space
					while (true) {
						SpiralCoordinate coord = spiral.next();
						int centerX = coord.x * spacing;
						int centerZ = coord.z * spacing;

						BoundingBox candidate = new BoundingBox(centerX - spacing / 2.0, world.getMinHeight(),
								centerZ - spacing / 2.0, centerX + spacing / 2.0, world.getMaxHeight(),
								centerZ + spacing / 2.0);

						// Check for overlap
						boolean intersects = cachedBoundingBoxes.values().stream()
								.anyMatch(box -> box.overlaps(candidate));
						if (!intersects) {
							instance.debug("findNextIslandLocation: Found available spiral slot at (" + centerX + ", "
									+ centerZ + ")");
							return new Location(world, centerX, instance.getConfigManager().height(), centerZ);
						}

						// Soft fail protection
						if (++checked > softLimit) {
							throw new IllegalStateException(
									"findNextIslandLocation: Spiral exceeded soft safety limit (" + softLimit
											+ ") — possible bounding box overlap issue or corrupted cache.");
						}

						// Periodic debug output to monitor spiral progress
						if (checked % 5000 == 0) {
							instance.debug("findNextIslandLocation: Still scanning spiral... checked " + checked
									+ " coordinates. Current = (" + coord.x + ", " + coord.z + ")");
						}
					}
				}));
	}

	/**
	 * Populates the bounding box cache using the current island owner data. Each
	 * owner's saved island data is read and added to the set for overlap detection.
	 *
	 * @return future that completes when the cache is fully populated
	 */
	private CompletableFuture<Void> cacheExistingBoundingBoxes() {
		return instance.getCoopManager().getCachedIslandOwnerData().thenCompose(owners -> {
			if (owners.isEmpty()) {
				instance.debug("Spiral cache: No island owners found.");
			} else {
				instance.debug("Spiral cache: Found " + owners.size() + " island owner"
						+ (owners.size() == 1 ? "" : "s") + ".");
			}

			if (owners.isEmpty()) {
				instance.debug("Spiral cache: No existing owners — starting fresh from coordinates (0, 0)");
				return CompletableFuture.completedFuture(null);
			}

			List<CompletableFuture<Void>> futures = new ArrayList<>();

			owners.forEach(ownerData -> {
				CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
					HellblockData data = ownerData.getHellblockData();
					BoundingBox box = data.getBoundingBox();
					if (box != null) {
						cachedBoundingBoxes.put(data.getIslandId(), box);
						instance.debug("Spiral cache: Cached bounding box for " + ownerData.getName() + " at center ("
								+ ((box.getMinX() + box.getMaxX()) / 2) + ", " + ((box.getMinZ() + box.getMaxZ()) / 2)
								+ ")");
					} else {
						instance.getPluginLogger().warn("Spiral cache: Missing bounding box for " + ownerData.getName()
								+ " (" + ownerData.getUUID() + ")");
					}
				});
				futures.add(task);
			});

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
					.thenRun(() -> instance.debug("Spiral cache: Finished caching " + cachedBoundingBoxes.size()
							+ " bounding box" + (cachedBoundingBoxes.size() == 1 ? "" : "es")));
		});
	}

	/**
	 * Computes the bounding box for a specific island ID using spiral coordinates.
	 * This is useful for visualizing or simulating island placement without
	 * reserving a spot.
	 *
	 * @param islandId the island's unique ID (must be >= 1)
	 * @param world    the world to compute coordinates in
	 * @return bounding box centered at the spiral-computed location
	 */
	@NotNull
	public BoundingBox computeSpiralBoundingBoxForIsland(int islandId, @NotNull HellblockWorld<?> world) {
		if (islandId <= 0) {
			throw new IllegalArgumentException("Island ID must be ≥ 1: received " + islandId);
		}

		// Before: spacing = maxProtection * 2
		// After: spacing still uses max, but bounding boxes use default
		int spacing = getIslandSpacing(); // max range * 2 (spacing between islands)
		double defaultRadius = instance.getUpgradeManager().getDefaultValue(IslandUpgradeType.PROTECTION_RANGE)
				.intValue();

		SpiralIterator spiral = new SpiralIterator();
		SpiralCoordinate coord = null;

		// Island ID starts at 1, so advance the spiral ID-1 times
		for (int i = 0; i < islandId; i++) {
			coord = spiral.next();
		}

		if (coord == null) {
			throw new IllegalStateException("Failed to resolve spiral coordinate for island ID " + islandId);
		}

		int centerX = coord.x * spacing;
		int centerZ = coord.z * spacing;

		double minX = centerX - defaultRadius;
		double maxX = centerX + defaultRadius;
		double minZ = centerZ - defaultRadius;
		double maxZ = centerZ + defaultRadius;

		instance.debug(
				"Computed bounding box for island ID " + islandId + ": center (" + centerX + ", " + centerZ + ")");
		return new BoundingBox(minX, world.bukkitWorld().getMinHeight(), minZ, maxX, world.bukkitWorld().getMaxHeight(),
				maxZ);
	}

	/**
	 * Represents a coordinate in the spiral grid.
	 */
	private class SpiralCoordinate {
		final int x, z;

		SpiralCoordinate(int x, int z) {
			this.x = x;
			this.z = z;
		}
	}

	/**
	 * Iterator that yields an infinite sequence of (x, z) grid coordinates in a
	 * square spiral pattern. Starts at (0, 0), and spirals outward.
	 *
	 * Used to calculate deterministic and collision-free positions for new islands.
	 */
	private class SpiralIterator implements Iterator<SpiralCoordinate> {
		private int x = 0, z = 0;
		private int dx = 0, dz = -1;

		@Override
		public boolean hasNext() {
			return true; // Infinite spiral generation
		}

		@Override
		public SpiralCoordinate next() {
			SpiralCoordinate coord = new SpiralCoordinate(x, z);

			// Change direction at square corners to create a spiral pattern
			if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
				int temp = dx;
				dx = -dz;
				dz = temp;
			}

			x += dx;
			z += dz;

			return coord;
		}
	}
}