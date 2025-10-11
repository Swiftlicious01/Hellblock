package com.swiftlicious.hellblock.generation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.world.HellblockWorld;

public class IslandPlacementDetector implements Reloadable {

	private final HellblockPlugin instance;

	private final Set<BoundingBox> cachedBoundingBoxes = ConcurrentHashMap.newKeySet();
	private final Queue<SpiralCoordinate> spiralQueue = new ArrayDeque<>();

	private int spacing;

	public IslandPlacementDetector(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		this.spacing = instance.getUpgradeManager().getMaxValue(IslandUpgradeType.PROTECTION_RANGE).intValue() * 2;
	}

	@Override
	public void unload() {
		cachedBoundingBoxes.clear();
		spiralQueue.clear();
	}

	public void initialize() {
		cacheExistingBoundingBoxesAndPrepareSpiral();
	}

	/**
	 * Find the next available island location in a spiral pattern, ensuring no
	 * overlap with existing islands.
	 * 
	 * @return A CompletableFuture that resolves to the next available Location.
	 */
	public CompletableFuture<Location> findNextIslandLocation() {
		return instance.getCoopManager().getAllIslandOwners().thenCompose(owners -> {
			List<CompletableFuture<Optional<UserData>>> futures = owners.stream().map(uuid -> instance
					.getStorageManager().getOfflineUserData(uuid, instance.getConfigManager().lockData()))
					.collect(Collectors.toList());

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(voided -> {
				AtomicInteger maxId = new AtomicInteger(-1);

				futures.forEach(future -> {
					Optional<UserData> userOpt = future.join();
					if (userOpt.isPresent()) {
						HellblockData data = userOpt.get().getHellblockData();
						if (data != null) {
							maxId.updateAndGet(current -> Math.max(current, data.getID()));
						}
					}
				});

				if (maxId.get() == -1) {
					throw new IllegalStateException("No valid Hellblock IDs found.");
				}

				int nextId = maxId.get() + 1;
				String worldFormat = instance.getWorldManager().getHellblockWorldFormat(nextId);
				Optional<HellblockWorld<?>> hellblockWorld = instance.getWorldManager().getWorld(worldFormat);

				if (hellblockWorld.isEmpty()) {
					throw new IllegalStateException("World for next island ID is not loaded: " + worldFormat);
				}

				World world = hellblockWorld.get().bukkitWorld();

				// Now that we have the world, scan for the next available spiral location
				while (!spiralQueue.isEmpty()) {
					SpiralCoordinate coord = spiralQueue.poll();
					int centerX = coord.x * spacing;
					int centerZ = coord.z * spacing;

					BoundingBox candidate = new BoundingBox(centerX - spacing / 2.0, world.getMinHeight(),
							centerZ - spacing / 2.0, centerX + spacing / 2.0, world.getMaxHeight(),
							centerZ + spacing / 2.0);

					boolean intersects = cachedBoundingBoxes.stream().anyMatch(box -> box.overlaps(candidate));
					if (!intersects) {
						cachedBoundingBoxes.add(candidate); // optimistic add
						return new Location(world, centerX, instance.getConfigManager().height(), centerZ);
					}
				}

				throw new IllegalStateException("No available island location found.");
			});
		});
	}

	/**
	 * Cache existing bounding boxes of all islands and prepare the spiral queue
	 * starting from the next free coordinate.
	 */
	private void cacheExistingBoundingBoxesAndPrepareSpiral() {
		instance.getCoopManager().getAllIslandOwners().thenAccept(owners -> {
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			List<SpiralCoordinate> usedCoords = new ArrayList<>();

			owners.forEach(owner -> {
				CompletableFuture<Void> task = instance.getStorageManager()
						.getOfflineUserData(owner, instance.getConfigManager().lockData())
						.thenAccept(optional -> optional.ifPresent(userData -> {
							HellblockData data = userData.getHellblockData();
							if (data != null && data.getBoundingBox() != null) {
								BoundingBox box = data.getBoundingBox();
								cachedBoundingBoxes.add(box);

								int centerX = (int) ((box.getMinX() + box.getMaxX()) / 2);
								int centerZ = (int) ((box.getMinZ() + box.getMaxZ()) / 2);
								usedCoords.add(new SpiralCoordinate(centerX / spacing, centerZ / spacing));
							}
						}));
				futures.add(task);
			});

			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
				SpiralCoordinate start = computeNextFreeSpiralCoord(usedCoords);
				preloadSpiralFrom(start);
			});
		});
	}

	/**
	 * Compute the next free spiral coordinate that is not in the used list.
	 * 
	 * @param used List of already used spiral coordinates.
	 * @return The next free spiral coordinate.
	 */
	private SpiralCoordinate computeNextFreeSpiralCoord(List<SpiralCoordinate> used) {
		Set<String> usedKeys = used.stream().map(coord -> coord.x + "," + coord.z).collect(Collectors.toSet());

		SpiralIterator iterator = new SpiralIterator(10000);
		while (iterator.hasNext()) {
			SpiralCoordinate candidate = iterator.next();
			String key = candidate.x + "," + candidate.z;
			if (!usedKeys.contains(key)) {
				return candidate;
			}
		}

		throw new IllegalStateException("No free spiral coordinate found.");
	}

	/**
	 * Preload a large number of spiral coordinates starting from a given
	 * coordinate. This helps in quickly finding the next available location later.
	 * 
	 * @param start The starting spiral coordinate.
	 */
	private void preloadSpiralFrom(SpiralCoordinate start) {
		int x = start.x, z = start.z;
		int dx = 0, dz = -1;

		for (int i = 0; i < 10000; i++) {
			spiralQueue.add(new SpiralCoordinate(x, z));

			if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
				int temp = dx;
				dx = -dz;
				dz = temp;
			}

			x += dx;
			z += dz;
		}
	}

	private static class SpiralCoordinate {
		final int x, z;

		SpiralCoordinate(int x, int z) {
			this.x = x;
			this.z = z;
		}
	}

	private static class SpiralIterator implements Iterator<SpiralCoordinate> {
		private int x = 0, z = 0, dx = 0, dz = -1;
		private int steps;
		private final int maxSteps;

		SpiralIterator(int maxSteps) {
			this.maxSteps = maxSteps;
		}

		@Override
		public boolean hasNext() {
			return steps < maxSteps;
		}

		@Override
		public SpiralCoordinate next() {
			SpiralCoordinate coord = new SpiralCoordinate(x, z);
			steps++;

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