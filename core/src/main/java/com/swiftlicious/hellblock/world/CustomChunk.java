package com.swiftlicious.hellblock.world;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Stream;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

public final class CustomChunk implements CustomChunkInterface {

	private final HellblockWorld<?> world;
	private final ChunkPos chunkPos;
	private final ConcurrentMap<Integer, CustomSection> loadedSections;
	private final PriorityBlockingQueue<DelayedTickTask> queue;
	private final Set<BlockPos> tickedBlocks;
	private long lastUnloadTime;
	private int loadedSeconds;
	private int lazySeconds;
	private boolean notified;
	private boolean isLoaded;
	private boolean forceLoad;

	// new chunk
	protected CustomChunk(HellblockWorld<?> world, ChunkPos chunkPos) {
		this.world = world;
		this.chunkPos = chunkPos;
		this.loadedSections = new ConcurrentHashMap<>(16);
		this.queue = new PriorityBlockingQueue<>();
		this.lazySeconds = 0;
		this.tickedBlocks = Collections.synchronizedSet(new HashSet<>());
		this.notified = true;
		this.isLoaded = false;
		this.updateLastUnloadTime();
	}

	protected CustomChunk(HellblockWorld<?> world, ChunkPos chunkPos, int loadedSeconds, long lastUnloadTime,
			ConcurrentMap<Integer, CustomSection> loadedSections, PriorityBlockingQueue<DelayedTickTask> queue,
			Set<BlockPos> tickedBlocks) {
		this.world = world;
		this.chunkPos = chunkPos;
		this.loadedSections = loadedSections;
		this.lastUnloadTime = lastUnloadTime;
		this.loadedSeconds = loadedSeconds;
		this.queue = queue;
		this.lazySeconds = 0;
		this.tickedBlocks = Collections.synchronizedSet(tickedBlocks);
		this.notified = false;
		this.isLoaded = false;
	}

	@Override
	public void setForceLoaded(boolean forceLoad) {
		this.forceLoad = forceLoad;
	}

	@Override
	public boolean isForceLoaded() {
		return this.forceLoad;
	}

	@Override
	public CompletableFuture<Boolean> load(boolean loadBukkitChunk) {
		if (isLoaded()) {
			return CompletableFuture.completedFuture(true);
		}

		CompletableFuture<Boolean> future = new CompletableFuture<>();

		if (((HellblockWorld<?>) world).loadChunk(this)) {
			if (loadBukkitChunk) {
				final World bukkitWorld = this.world.bukkitWorld();
				final int x = chunkPos.x();
				final int z = chunkPos.z();

				if (!bukkitWorld.isChunkLoaded(x, z)) {
					ChunkUtils.getChunkAtAsync(bukkitWorld, x, z).thenAccept(chunk -> {
						addPluginChunkTicket(bukkitWorld, x, z);
						this.isLoaded = true;
						this.lazySeconds = 0;
						future.complete(true);
					}).exceptionally(ex -> {
						future.completeExceptionally(ex);
						return null;
					});
				} else {
					addPluginChunkTicket(bukkitWorld, x, z);
					this.isLoaded = true;
					this.lazySeconds = 0;
					future.complete(true);
				}
			} else {
				this.isLoaded = true;
				this.lazySeconds = 0;
				future.complete(true);
			}
		} else {
			future.complete(false); // failed to register
		}

		return future;
	}

	@Override
	public void unload(boolean lazy) {
		if (!isLoaded() || isForceLoaded()) {
			return;
		}

		if (((HellblockWorld<?>) world).unloadChunk(this, lazy)) {
			this.isLoaded = false;
			this.notified = false;
			this.lazySeconds = 0;

			// Ticket cleanup
			final World bukkitWorld = world.bukkitWorld();
			final int x = chunkPos.x();
			final int z = chunkPos.z();

			if (VersionHelper.isPaperFork()) {
				// Paper: remove plugin ticket
				try {
					bukkitWorld.getClass().getMethod("removePluginChunkTicket", int.class, int.class, Plugin.class)
							.invoke(bukkitWorld, x, z, HellblockPlugin.getInstance());
				} catch (Exception ignored) {
				}
			}
		}
	}

	@Override
	public void unloadLazy() {
		if (!isLoaded() && isLazy()) {
			((HellblockWorld<?>) world).unloadLazyChunk(chunkPos);

			final World bukkitWorld = world.bukkitWorld();
			final int x = chunkPos.x();
			final int z = chunkPos.z();

			if (VersionHelper.isPaperFork()) {
				try {
					bukkitWorld.getClass().getMethod("removePluginChunkTicket", int.class, int.class, Plugin.class)
							.invoke(bukkitWorld, x, z, HellblockPlugin.getInstance());
				} catch (Exception ignored) {
				}
			}
		}
	}

	private void addPluginChunkTicket(World world, int x, int z) {
		if (VersionHelper.isPaperFork()) {
			try {
				world.getClass().getMethod("addPluginChunkTicket", int.class, int.class, Plugin.class).invoke(world, x,
						z, HellblockPlugin.getInstance());
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public boolean isLazy() {
		return ((HellblockWorld<?>) world).getLazyChunk(chunkPos) == this;
	}

	@Override
	public boolean isLoaded() {
		return this.isLoaded;
	}

	@Override
	public HellblockWorld<?> getWorld() {
		return world;
	}

	@Override
	public ChunkPos chunkPos() {
		return chunkPos;
	}

	@Override
	public void timer() {
		final WorldSetting setting = world.setting();
		final int interval = setting.minTickUnit();
		this.loadedSeconds++;
		// if loadedSeconds reach another recycle, rearrange the tasks
		if (this.loadedSeconds < interval) {
			return;
		}
		this.loadedSeconds = 0;
		this.tickedBlocks.clear();
		this.queue.clear();
		this.arrangeTasks(interval);
	}

	private void arrangeTasks(int unit) {
		loadedSections.values().forEach(section -> section.blockMap().entrySet().forEach(entry -> {
			this.queue.add(new DelayedTickTask(RandomUtils.generateRandomInt(0, unit), entry.getKey()));
			this.tickedBlocks.add(entry.getKey());
		}));
	}

	@Override
	public int lazySeconds() {
		return lazySeconds;
	}

	@Override
	public void lazySeconds(int lazySeconds) {
		this.lazySeconds = lazySeconds;
	}

	@Override
	public long lastLoadedTime() {
		return lastUnloadTime;
	}

	@Override
	public void updateLastUnloadTime() {
		this.lastUnloadTime = System.currentTimeMillis();
	}

	@Override
	public int loadedMilliSeconds() {
		return (int) (System.currentTimeMillis() - lastUnloadTime);
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> getBlockState(Pos3 location) {
		final BlockPos pos = BlockPos.fromPos3(location);
		return getLoadedSection(pos.sectionID()).flatMap(section -> section.getBlockState(pos));
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> removeBlockState(Pos3 location) {
		final BlockPos pos = BlockPos.fromPos3(location);
		final int sectionID = pos.sectionID();

		final Optional<CustomSection> maybeSection = getLoadedSection(sectionID);

		if (maybeSection.isEmpty()) {
			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Attempted to remove block in unloaded or nonexistent section at " + location);
			return Optional.empty();
		}

		CustomSection section = maybeSection.get();
		Optional<CustomBlockState> removed = section.removeBlockState(pos);

		// Optional: log if section is now empty
		if (section.canPrune()) {
			HellblockPlugin.getInstance().debug("Section %d in chunk %s became empty after removing block at %s"
					.formatted(sectionID, chunkPos, location));
		}

		return removed;
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> addBlockState(Pos3 location, CustomBlockState block) {
		final BlockPos pos = BlockPos.fromPos3(location);
		final int sectionID = pos.sectionID();

		// Try to get without creating first, just for logging
		boolean isNewSection = !loadedSections.containsKey(sectionID);

		// Create section if missing
		final CustomSection section = loadedSections.computeIfAbsent(sectionID, CustomSection::new);

		if (isNewSection) {
			HellblockPlugin.getInstance().debug(
					"Created new section %d in chunk %s for block at %s".formatted(sectionID, chunkPos, location));
		}

		arrangeScheduledTickTaskForNewBlock(pos);
		return section.addBlockState(pos, block);
	}

	@NotNull
	@Override
	public Stream<CustomSection> sectionsToSave() {
		return loadedSections.values().stream().filter(section -> !section.canPrune());
	}

	@NotNull
	@Override
	public Optional<CustomSection> getLoadedSection(int sectionID) {
		return Optional.ofNullable(loadedSections.get(sectionID));
	}

	@Override
	public CustomSection getSection(int sectionID) {
		return getLoadedSection(sectionID).orElseGet(() -> {
			final CustomSection section = new CustomSection(sectionID);
			this.loadedSections.put(sectionID, section);
			return section;
		});
	}

	@Override
	public CustomSection[] sections() {
		return loadedSections.values().toArray(new CustomSection[0]);
	}

	@Override
	public Optional<CustomSection> removeSection(int sectionID) {
		return Optional.ofNullable(loadedSections.remove(sectionID));
	}

	@Override
	public boolean canPrune() {
		return loadedSections.isEmpty();
	}

	@Override
	public boolean isOfflineTaskNotified() {
		return notified;
	}

	@Override
	public void notifyOfflineTask() {
		if (isOfflineTaskNotified()) {
			return;
		}
		this.notified = true;
		final long current = System.currentTimeMillis();
		int offlineTimeInSeconds = (int) (current - lastLoadedTime()) / 1000;
		final WorldSetting setting = world.setting();
		offlineTimeInSeconds = Math.min(offlineTimeInSeconds, setting.maxOfflineTime());
		final int minTickUnit = setting.minTickUnit();
		final int threshold = setting.maxLoadingTime();
		int i = 0;
		final long time1 = System.currentTimeMillis();
		while (i < offlineTimeInSeconds) {
			this.loadedSeconds++;
			if (this.loadedSeconds >= minTickUnit) {
				this.loadedSeconds = 0;
				this.tickedBlocks.clear();
				this.queue.clear();
				this.arrangeTasks(minTickUnit);
			}
			i++;
			if (System.currentTimeMillis() - time1 > threshold) {
				break;
			}
		}
	}

	@Override
	public PriorityBlockingQueue<DelayedTickTask> tickTaskQueue() {
		return queue;
	}

	@Override
	public Set<BlockPos> tickedBlocks() {
		return tickedBlocks;
	}

	private void arrangeScheduledTickTaskForNewBlock(BlockPos pos) {
		final WorldSetting setting = world.setting();
		if (tickedBlocks.contains(pos)) {
			return;
		}
		tickedBlocks.add(pos);
		final int random = RandomUtils.generateRandomInt(0, setting.minTickUnit() - 1);
		if (random > loadedSeconds) {
			queue.add(new DelayedTickTask(random, pos));
		}
	}
}