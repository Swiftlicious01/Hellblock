package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Shulker;
import org.bukkit.util.BoundingBox;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

/**
 * Manages island backups and snapshots for players. Allows creating, saving,
 * loading, listing, and restoring snapshots.
 */
public class IslandBackupManager {

	protected final HellblockPlugin instance;
	private final File backupFolder;

	private final Map<UUID, Long> lastSnapshotTime = new ConcurrentHashMap<>();
	private static final long SNAPSHOT_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

	public IslandBackupManager(HellblockPlugin plugin, File dataFolder) {
		this.instance = plugin;
		this.backupFolder = new File(dataFolder, "island_backups");
		if (!backupFolder.exists()) {
			backupFolder.mkdirs();
		}
	}

	/**
	 * Create a pre-reset snapshot for the given island owner. Returns the timestamp
	 * of the snapshot, or null if failed. This snapshot is intended to be taken
	 * right before an island reset, so the player can restore it if needed.
	 * 
	 * Note: This runs asynchronously and may take some time to complete.
	 * 
	 * @param ownerId The UUID of the island owner.
	 * 
	 * @return A CompletableFuture that resolves to the timestamp of the snapshot,
	 *         or null if failed.
	 */
	public CompletableFuture<Long> createPreResetSnapshot(UUID ownerId) {
		final long timestamp = System.currentTimeMillis();

		return captureIslandSnapshot(ownerId).thenApply(snapshot -> {
			if (snapshot == null || snapshot.blocks().isEmpty()) {
				instance.getPluginLogger().warn("No blocks found when creating pre-reset snapshot for " + ownerId);
				return null;
			}

			saveSnapshot(ownerId, timestamp, snapshot);
			instance.getPluginLogger().info("Pre-reset snapshot saved for " + ownerId + " at " + timestamp);
			return timestamp;
		});
	}

	/**
	 * Save snapshot to disk as a serialized file. Filename format:
	 * {islandOwner}_{timestamp}.dat
	 * 
	 * @param islandOwner the island owner's UUID
	 * @param timestamp   the timestamp of the snapshot
	 * @param snapshot    the snapshot to save
	 */
	public void saveSnapshot(UUID islandOwner, long timestamp, IslandSnapshot snapshot) {
		final File file = new File(backupFolder, islandOwner.toString() + "_" + timestamp + ".dat");

		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
			oos.writeObject(snapshot.toSerializable());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load snapshot from disk. Filename format: {islandOwner}_{timestamp}.dat
	 * 
	 * @param islandOwner the island owner's UUID
	 * @param timestamp   the timestamp of the snapshot
	 * @return the loaded snapshot, or null if not found or failed
	 */
	public SerializableIslandSnapshot loadSnapshot(UUID islandOwner, long timestamp) {
		final File file = new File(backupFolder, islandOwner.toString() + "_" + timestamp + ".dat");
		if (!file.exists()) {
			return null;
		}

		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
			return (SerializableIslandSnapshot) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates a snapshot for the given island owner if the cooldown has passed.
	 * 
	 * @param ownerId The UUID of the island owner.
	 * @return A CompletableFuture that completes when the snapshot is taken or
	 *         skipped.
	 */
	public CompletableFuture<Void> maybeSnapshot(UUID ownerId) {
		final long now = System.currentTimeMillis();
		final long last = lastSnapshotTime.getOrDefault(ownerId, 0L);

		if (now - last < SNAPSHOT_COOLDOWN_MS) {
			return CompletableFuture.completedFuture(null);
		}

		lastSnapshotTime.put(ownerId, now);

		return captureIslandSnapshot(ownerId).thenAccept(snapshot -> {
			if (snapshot.blocks().isEmpty() && snapshot.entities().isEmpty()) {
				return;
			}

			final long timestamp = System.currentTimeMillis();
			saveSnapshot(ownerId, timestamp, snapshot);

			// Purge older snapshots (keep last 5, for example)
			purgeOldSnapshots(ownerId, 5);

			instance.getPluginLogger()
					.info("Snapshot saved for " + ownerId + " at " + timestamp + " (older snapshots pruned)");
		});
	}

	/**
	 * Lists available snapshot timestamps for the given island owner.
	 * 
	 * @param islandOwner the island owner's UUID
	 * @return list of snapshot timestamps (sorted ascending)
	 */
	public List<Long> listSnapshots(UUID islandOwner) {
		final File[] files = backupFolder
				.listFiles((dir, name) -> name.startsWith(islandOwner.toString() + "_") && name.endsWith(".dat"));
		if (files == null) {
			return Collections.emptyList();
		}

		return Arrays.stream(files).map(file -> {
			final String timestamp = file.getName().replace(islandOwner.toString() + "_", "").replace(".dat", "");
			return Long.parseLong(timestamp);
		}).sorted().toList();
	}

	/**
	 * Purges old snapshots for the given island owner, keeping only the most recent
	 * 'keepLast' snapshots.
	 * 
	 * @param islandOwner the island owner's UUID
	 * @param keepLast    number of most recent snapshots to keep
	 */
	public void purgeOldSnapshots(UUID islandOwner, int keepLast) {
		final List<Long> snapshots = listSnapshots(islandOwner);
		if (snapshots.size() <= keepLast) {
			return;
		}

		final List<Long> toDelete = snapshots.subList(0, snapshots.size() - keepLast);
		toDelete.stream().map(ts -> new File(backupFolder, islandOwner.toString() + "_" + ts + ".dat"))
				.filter(File::exists).forEach(File::delete);
	}

	/**
	 * Captures the current state of the island owned by the given player. This
	 * includes all blocks and entities within the island's bounding box.
	 * 
	 * @param ownerId The UUID of the island owner.
	 * @return A CompletableFuture that resolves to the captured IslandSnapshot, or
	 *         null if the island data or world is not found.
	 */
	private CompletableFuture<IslandSnapshot> captureIslandSnapshot(UUID ownerId) {
		final CompletableFuture<IslandSnapshot> future = new CompletableFuture<>();

		instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
				.thenAccept(optionalUserData -> {
					if (optionalUserData.isEmpty()) {
						future.complete(null);
						return;
					}

					final HellblockData hb = optionalUserData.get().getHellblockData();
					final Optional<HellblockWorld<?>> optWorld = instance.getWorldManager()
							.getWorld(instance.getWorldManager().getHellblockWorldFormat(hb.getID()));

					if (optWorld.isEmpty() || optWorld.get().bukkitWorld() == null) {
						future.complete(null);
						return;
					}

					final World world = optWorld.get().bukkitWorld();
					final BoundingBox box = hb.getBoundingBox();

					final List<IslandSnapshotBlock> blocks = Collections.synchronizedList(new ArrayList<>());
					final List<EntitySnapshot> entities = Collections.synchronizedList(new ArrayList<>());

					// Collect all entities inside bounding box
					world.getEntities().forEach(entity -> {
						if (box.contains(entity.getLocation().toVector())) {
							entities.add(EntitySnapshot.fromEntity(entity));
						}
					});

					// Collect block states in batches
					final int BATCH_SIZE = 500;
					final List<Location> locations = new ArrayList<>();

					for (int x = (int) box.getMinX(); x <= (int) box.getMaxX(); x++) {
						for (int y = (int) box.getMinY(); y <= (int) box.getMaxY(); y++) {
							for (int z = (int) box.getMinZ(); z <= (int) box.getMaxZ(); z++) {
								locations.add(new Location(world, x, y, z));
							}
						}
					}

					final Iterator<Location> it = locations.iterator();

					final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

					final SchedulerTask task = instance.getScheduler().sync().runRepeating(() -> {
						int processed = 0;
						while (processed < BATCH_SIZE && it.hasNext()) {
							final Location loc = it.next();
							final Block block = loc.getBlock();
							if (!block.getType().isAir()) {
								// Capture attached entities near this block (optional, can be empty list)
								final List<EntitySnapshot> nearby = findAttachedEntities(loc);
								blocks.add(IslandSnapshotBlock.fromBlockState(block.getState(), nearby));
							}
							processed++;
						}

						if (!it.hasNext()) {
							final SchedulerTask t = taskRef.get();
							if (t != null) {
								t.cancel();
							}
							future.complete(new IslandSnapshot(blocks, entities));
						}
					}, 1L, 1L, LocationUtils.getAnyLocationInstance());

					// assign after scheduling so it's available inside the lambda
					taskRef.set(task);
				});

		return future;
	}

	/**
	 * Restores a previously saved snapshot into the given world at the island's
	 * bounding box. This runs in batches to avoid server lag.
	 * 
	 * @param ownerId    The UUID of the island owner.
	 * @param timestamp  The timestamp of the snapshot to restore.
	 * @param world      The world to restore into.
	 * @param whenDone   A runnable to execute when the restore is complete (can be
	 *                   null).
	 * @param onProgress A consumer to receive progress updates (0.0 to 1.0), can be
	 *                   null.
	 */
	public void restoreSnapshot(UUID ownerId, long timestamp, World world, Runnable whenDone,
			Consumer<Double> onProgress) {
		final SerializableIslandSnapshot snapshot = loadSnapshot(ownerId, timestamp);
		if (snapshot == null) {
			instance.getPluginLogger().warn("No snapshot found for " + ownerId + " at " + timestamp);
			return;
		}

		// Get bounding box from island data
		instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
				.thenAccept(optionalUserData -> {
					if (optionalUserData.isEmpty()) {
						instance.getPluginLogger()
								.warn("No island data found for " + ownerId + " when restoring snapshot");
						return;
					}

					final HellblockData hb = optionalUserData.get().getHellblockData();
					final BoundingBox box = hb.getBoundingBox();

					// Run batched restore
					snapshot.restoreIntoWorldBatched(world, box, instance, () -> {
						instance.getPluginLogger().info("Snapshot restored for " + ownerId + " at " + timestamp);

						if (whenDone != null) {
							whenDone.run();
						}
					}, onProgress);
				});
	}

	/**
	 * Finds entities that are attached to the block at the given location. These
	 * include item frames, paintings, shulkers, and small armor stands.
	 * 
	 * @param loc The location of the block.
	 * @return A list of EntitySnapshot representing the attached entities.
	 */
	private List<EntitySnapshot> findAttachedEntities(Location loc) {
		final World world = loc.getWorld();
		if (world == null) {
			return List.of();
		}

		final Block block = loc.getBlock();
		final BoundingBox box = block.getBoundingBox();

		// Capture nearby entities anchored to this block
		return world.getNearbyEntities(box).stream().filter(this::isBlockAttachedEntity).map(EntitySnapshot::fromEntity)
				.toList();
	}

	/**
	 * Checks if the given entity is one that is typically attached to a block, such
	 * as item frames, paintings, shulkers, or small visible armor stands.
	 * 
	 * @param entity The entity to check.
	 * @return True if the entity is block-attached, false otherwise.
	 */
	private boolean isBlockAttachedEntity(Entity entity) {
		// Hanging = item frames, paintings
		// Shulker = mob that acts like a block
		// ArmorStand = small decorative stands
		return entity instanceof Hanging || entity instanceof Shulker
				|| (entity instanceof ArmorStand stand && stand.isVisible() && stand.isSmall());
	}
}