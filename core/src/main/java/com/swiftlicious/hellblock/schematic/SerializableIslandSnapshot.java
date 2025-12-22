package com.swiftlicious.hellblock.schematic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;

/**
 * A serializable snapshot of an island — for saving/loading from JSON.
 */
public record SerializableIslandSnapshot(@JsonProperty("blocks") List<IslandSnapshotBlock> blocks,
		@JsonProperty("entities") List<EntitySnapshot> entities) implements Serializable {

	public static final int MIN_BATCH_SIZE = 50;
	public static final int MAX_BATCH_SIZE = 2000;

	@JsonCreator
	public SerializableIslandSnapshot {
		// Jackson/Gson will auto-wire the values
	}

	/**
	 * Convert this serializable snapshot back into a runtime IslandSnapshot.
	 * 
	 * @param world World to associate with (for relative locations)
	 * @return Runtime IslandSnapshot
	 * @throws IllegalArgumentException if world is null
	 */
	public IslandSnapshot toRuntime(World world) {
		return new IslandSnapshot(new ArrayList<>(blocks), entities);
	}

	/**
	 * Restore this snapshot into the world immediately (may lag on large islands).
	 * 
	 * @param world World to restore into
	 * @param box   Bounding box to clear before restoring
	 * @throws IllegalArgumentException if box is null
	 */
	public void restoreIntoWorld(World world, BoundingBox box) {
		if (box == null) {
			throw new IllegalArgumentException("BoundingBox required for restoration");
		}

		clearAreaImmediate(world, box);

		// Place blocks
		blocks.stream().map(block -> block.restore(world)).filter(restored -> restored != null)
				.forEach(restored -> restored.update(true, false));

		// Spawn entities
		entities.forEach(entitySnapshot -> entitySnapshot.spawn(world));
	}

	/**
	 * Restore this snapshot into the world in small batches (safe for large
	 * islands).
	 * 
	 * @param plugin     Your HellblockPlugin instance
	 * @param world      World to restore into
	 * @param box        Bounding box to clear before restoring
	 * @param onProgress Consumer to accept progress updates (0.0 to 1.0), may be
	 *                   null
	 */
	public CompletableFuture<Boolean> restoreIntoWorldBatched(HellblockPlugin plugin, World world, BoundingBox box,
			@Nullable Consumer<Double> onProgress) {
		final CompletableFuture<Boolean> future = new CompletableFuture<>();

		// Collect all blocks inside the bounding box
		final List<Block> toRemove = collectBlocksInside(world, box);
		final List<IslandSnapshotBlock> blocksToRestore = new ArrayList<>(blocks);
		final List<EntitySnapshot> entitiesToSpawn = new ArrayList<>(entities);

		final int totalWork = toRemove.size() + blocksToRestore.size() + entitiesToSpawn.size();
		final AtomicInteger done = new AtomicInteger(0);

		// Aim to spread over ~200 ticks (10s)
		final AtomicInteger batchSize = new AtomicInteger(Math.max(MIN_BATCH_SIZE, totalWork / 200));

		final Iterator<Block> removeIt = toRemove.iterator();
		final Iterator<IslandSnapshotBlock> blockIt = blocksToRestore.iterator();
		final Iterator<EntitySnapshot> entityIt = entitiesToSpawn.iterator();

		final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

		final SchedulerTask task = plugin.getScheduler().sync().runRepeating(() -> {
			try {
				// Dynamic batch scaling – optional: tie to your TPS monitor
				final double tps = getRecentTps(plugin);
				if (tps < 18) {
					batchSize.set(Math.max(MIN_BATCH_SIZE, batchSize.get() / 2));
				} else if (tps > 19.8) {
					batchSize.set(Math.min(MAX_BATCH_SIZE, batchSize.get() * 2));
				}

				int processed = 0;
				while (processed < batchSize.get() && (removeIt.hasNext() || blockIt.hasNext() || entityIt.hasNext())) {
					if (removeIt.hasNext()) {
						removeIt.next().setType(Material.AIR, false);
					} else if (blockIt.hasNext()) {
						blockIt.next().restore(world);
					} else if (entityIt.hasNext()) {
						entityIt.next().spawn(world);
					}
					done.incrementAndGet();
					processed++;
				}

				// fire progress update async
				if (onProgress != null && totalWork > 0) {
					final double progress = (double) done.get() / totalWork;
					plugin.getScheduler().async().execute(() -> onProgress.accept(progress));
				}

				// finish
				if (!removeIt.hasNext() && !blockIt.hasNext() && !entityIt.hasNext()) {
					final SchedulerTask scheduled = taskRef.get();
					if (scheduled != null && !scheduled.isCancelled()) {
						scheduled.cancel();
					}
					future.complete(true);
				}
			} catch (Exception ex) {
				final SchedulerTask scheduled = taskRef.get();
				if (scheduled != null && !scheduled.isCancelled()) {
					scheduled.cancel();
				}
				future.completeExceptionally(ex);
			}
		}, 1L, 1L, LocationUtils.getAnyLocationInstance());

		// assign after scheduling so it's available inside the lambda
		taskRef.set(task);
		return future;
	}

	/**
	 * Collect all blocks inside the bounding box.
	 * 
	 * @param world World to collect from
	 * @param box   Bounding box to collect within
	 * @return List of blocks inside the box
	 */
	private List<Block> collectBlocksInside(World world, BoundingBox box) {
		final List<Block> blocks = new ArrayList<>();
		for (int x = (int) box.getMinX(); x <= (int) box.getMaxX(); x++) {
			for (int y = (int) box.getMinY(); y <= (int) box.getMaxY(); y++) {
				for (int z = (int) box.getMinZ(); z <= (int) box.getMaxZ(); z++) {
					blocks.add(world.getBlockAt(x, y, z));
				}
			}
		}
		return blocks;
	}

	/**
	 * Gets recent TPS if your plugin has a monitor. If not, just return 20.0 so
	 * batch size stays steady.
	 * 
	 * @param plugin Your HellblockPlugin instance
	 * @return Recent TPS or 20.0 if no monitor
	 */
	private double getRecentTps(HellblockPlugin plugin) {
		try {
			return plugin.getTpsMonitor().getRecentTps();
		} catch (Exception e) {
			return 20.0;
		}
	}

	/**
	 * Clears all blocks and entities inside the bounding box immediately (unsafe
	 * for large areas).
	 * 
	 * @param world World to clear
	 * @param box   Bounding box to clear
	 */
	private void clearAreaImmediate(World world, BoundingBox box) {
		world.getEntities().stream().filter(Objects::nonNull)
				.filter(entity -> entity.isValid() && !entity.isDead() && box.contains(entity.getLocation().toVector()))
				.forEach(Entity::remove);

		for (int x = (int) box.getMinX(); x <= (int) box.getMaxX(); x++) {
			for (int y = (int) box.getMinY(); y <= (int) box.getMaxY(); y++) {
				for (int z = (int) box.getMinZ(); z <= (int) box.getMaxZ(); z++) {
					final Block block = world.getBlockAt(x, y, z);
					if (!block.getType().isAir()) {
						block.setType(Material.AIR, false);
					}
				}
			}
		}
	}
}