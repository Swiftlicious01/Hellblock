package com.swiftlicious.hellblock.schematic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Shulker;
import org.bukkit.util.BoundingBox;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;

/**
 * Island snapshot storing serializable block snapshots and entity snapshots.
 */
public record IslandSnapshot(List<IslandSnapshotBlock> blocks, List<EntitySnapshot> entities) {

	private static final int BATCH_SIZE = 500; // adjust as needed

	/**
	 * Create a snapshot of blocks + entities within a bounding box. Runs in batches
	 * so it won’t lag the server.
	 */
	public static CompletableFuture<IslandSnapshot> captureAsync(HellblockPlugin plugin, BoundingBox box, World world) {
		final CompletableFuture<IslandSnapshot> future = new CompletableFuture<>();

		// Collect all block locations first
		final List<Location> locations = new ArrayList<>();
		for (int x = (int) box.getMinX(); x <= (int) box.getMaxX(); x++) {
			for (int y = (int) box.getMinY(); y <= (int) box.getMaxY(); y++) {
				for (int z = (int) box.getMinZ(); z <= (int) box.getMaxZ(); z++) {
					locations.add(new Location(world, x, y, z));
				}
			}
		}

		final Iterator<Location> it = locations.iterator();
		final List<IslandSnapshotBlock> blocks = Collections.synchronizedList(new ArrayList<>());
		final List<EntitySnapshot> entities = Collections.synchronizedList(new ArrayList<>());

		// Capture global entities first (excluding block-attached)
		world.getEntities().stream().filter(entity -> box.contains(entity.getLocation().toVector())).forEach(entity -> {
			if (isBlockAttachedEntity(entity)) {
				return;
			}
			entities.add(EntitySnapshot.fromEntity(entity));
		});

		// Process blocks in batches (sync)
		final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();
		final SchedulerTask task = plugin.getScheduler().sync().runRepeating(() -> {
			int processed = 0;

			while (processed < BATCH_SIZE && it.hasNext()) {
				final Location loc = it.next();
				final Block block = loc.getBlock();

				if (!block.getType().isAir()) {
					// Capture attached entities for this block (e.g., item frames, shulkers)
					final List<EntitySnapshot> nearby = findAttachedEntities(loc);
					blocks.add(IslandSnapshotBlock.fromBlockState(block.getState(), nearby));
				}
				processed++;
			}

			// Finish when no more blocks
			if (!it.hasNext()) {
				final SchedulerTask scheduled = taskRef.get();
				if (scheduled != null && !scheduled.isCancelled()) {
					scheduled.cancel();
				}
				future.complete(new IslandSnapshot(blocks, entities));
			}
		}, 1L, 1L, LocationUtils.getAnyLocationInstance());

		taskRef.set(task);
		return future;
	}

	/**
	 * Restore a snapshot into the given world at the original positions. Runs in
	 * sync batches.
	 */
	public CompletableFuture<Void> restoreAsync(HellblockPlugin plugin, World world) {
		final CompletableFuture<Void> future = new CompletableFuture<>();

		final Iterator<IslandSnapshotBlock> blockIt = blocks.iterator();
		final Iterator<EntitySnapshot> entityIt = entities.iterator();

		final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();
		final SchedulerTask task = plugin.getScheduler().sync().runRepeating(() -> {
			int processed = 0;

			// Restore blocks first (IslandSnapshotBlock.restore returns BlockState)
			while (processed < BATCH_SIZE && blockIt.hasNext()) {
				final IslandSnapshotBlock snapshotBlock = blockIt.next();
				snapshotBlock.restore(world);
				processed++;
			}

			// Once blocks are done, restore free entities
			if (!blockIt.hasNext()) {
				while (processed < BATCH_SIZE && entityIt.hasNext()) {
					entityIt.next().spawn(world);
					processed++;
				}
			}

			if (!blockIt.hasNext() && !entityIt.hasNext()) {
				final SchedulerTask scheduled = taskRef.get();
				if (scheduled != null && !scheduled.isCancelled()) {
					scheduled.cancel();
				}
				future.complete(null);
			}
		}, 1L, 1L, LocationUtils.getAnyLocationInstance());

		taskRef.set(task);
		return future;
	}

	/**
	 * Converts runtime snapshot into serializable snapshot for saving. (blocks
	 * already are IslandSnapshotBlock, so just wrap)
	 */
	public SerializableIslandSnapshot toSerializable() {
		return new SerializableIslandSnapshot(blocks, entities);
	}

	/**
	 * Converts serializable snapshot back into runtime snapshot for restoring.
	 * (blocks already are IslandSnapshotBlock, so just wrap)
	 * 
	 * @param serializable the serializable snapshot
	 * @return the runtime snapshot
	 * @throws IllegalArgumentException if the serializable snapshot is null
	 */
	private static boolean isBlockAttachedEntity(Entity entity) {
		return entity instanceof Hanging || entity instanceof Shulker
				|| (entity instanceof ArmorStand stand && stand.isVisible() && stand.isSmall());
	}

	/**
	 * Find entities that are attached to a block at the given location.
	 * 
	 * @param loc the block location
	 * @return list of attached entity snapshots
	 */
	private static List<EntitySnapshot> findAttachedEntities(Location loc) {
		final World world = loc.getWorld();
		if (world == null) {
			return List.of();
		}

		final Block block = loc.getBlock();
		final BoundingBox box = block.getBoundingBox();

		return world.getNearbyEntities(box).stream().filter(IslandSnapshot::isBlockAttachedEntity)
				.map(EntitySnapshot::fromEntity).toList();
	}
}