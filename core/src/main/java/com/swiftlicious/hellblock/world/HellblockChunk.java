package com.swiftlicious.hellblock.world;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.utils.RandomUtils;

public class HellblockChunk implements HellblockChunkInterface {

	private final HellblockWorld<?> world;
	private final ChunkPos chunkPos;
	private final ConcurrentMap<Integer, HellblockSection> loadedSections;
	private final PriorityBlockingQueue<DelayedTickTask> queue;
	private final Set<BlockPos> tickedBlocks;
	private long lastUnloadTime;
	private int loadedSeconds;
	private int lazySeconds;
	private boolean notified;
	private boolean isLoaded;
	private boolean forceLoad;

	// new chunk
	protected HellblockChunk(HellblockWorld<?> world, ChunkPos chunkPos) {
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

	protected HellblockChunk(HellblockWorld<?> world, ChunkPos chunkPos, int loadedSeconds, long lastUnloadTime,
			ConcurrentMap<Integer, HellblockSection> loadedSections, PriorityBlockingQueue<DelayedTickTask> queue,
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
	public void load(boolean loadBukkitChunk) {
		if (!isLoaded()) {
			if (((HellblockWorld<?>) world).loadChunk(this)) {
				this.isLoaded = true;
				this.lazySeconds = 0;
			}
			if (loadBukkitChunk && !this.world.bukkitWorld().isChunkLoaded(chunkPos.x(), chunkPos.z())) {
				this.world.bukkitWorld().getChunkAt(chunkPos.x(), chunkPos.z());
			}
		}
	}

	@Override
	public void unload(boolean lazy) {
		if (isLoaded() && !isForceLoaded()) {
			if (((HellblockWorld<?>) world).unloadChunk(this, lazy)) {
				this.isLoaded = false;
				this.notified = false;
				this.lazySeconds = 0;
			}
		}
	}

	@Override
	public void unloadLazy() {
		if (!isLoaded() && isLazy()) {
			((HellblockWorld<?>) world).unloadLazyChunk(chunkPos);
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
		WorldSetting setting = world.setting();
		int interval = setting.minTickUnit();
		this.loadedSeconds++;
		// if loadedSeconds reach another recycle, rearrange the tasks
		if (this.loadedSeconds >= interval) {
			this.loadedSeconds = 0;
			this.tickedBlocks.clear();
			this.queue.clear();
			this.arrangeTasks(interval);
		}
		scheduledTick(false);
		randomTick(setting.randomTickSpeed(), false);
	}

	private void arrangeTasks(int unit) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (HellblockSection section : loadedSections.values()) {
			for (Map.Entry<BlockPos, HellblockBlockState> entry : section.blockMap().entrySet()) {
				this.queue.add(new DelayedTickTask(random.nextInt(0, unit), entry.getKey()));
				this.tickedBlocks.add(entry.getKey());
			}
		}
	}

	private void scheduledTick(boolean offline) {
		while (!queue.isEmpty() && queue.peek().getTime() <= loadedSeconds) {
			DelayedTickTask task = queue.poll();
			if (task != null) {
				BlockPos pos = task.blockPos();
				HellblockSection section = loadedSections.get(pos.sectionID());
				if (section != null) {
					Optional<HellblockBlockState> block = section.getBlockState(pos);
					block.ifPresent(state -> state.type().scheduledTick(state, world, pos.toPos3(chunkPos), offline));
				}
			}
		}
	}

	private void randomTick(int randomTickSpeed, boolean offline) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (HellblockSection section : loadedSections.values()) {
			int sectionID = section.getSectionID();
			int baseY = sectionID * 16;
			for (int i = 0; i < randomTickSpeed; i++) {
				int x = random.nextInt(16);
				int y = random.nextInt(16) + baseY;
				int z = random.nextInt(16);
				BlockPos pos = new BlockPos(x, y, z);
				Optional<HellblockBlockState> block = section.getBlockState(pos);
				block.ifPresent(state -> state.type().randomTick(state, world, pos.toPos3(chunkPos), offline));
			}
		}
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
	public Optional<HellblockBlockState> getBlockState(Pos3 location) {
		BlockPos pos = BlockPos.fromPos3(location);
		return getLoadedSection(pos.sectionID()).flatMap(section -> section.getBlockState(pos));
	}

	@NotNull
	@Override
	public Optional<HellblockBlockState> removeBlockState(Pos3 location) {
		BlockPos pos = BlockPos.fromPos3(location);
		return getLoadedSection(pos.sectionID()).flatMap(section -> section.removeBlockState(pos));
	}

	@NotNull
	@Override
	public Optional<HellblockBlockState> addBlockState(Pos3 location, HellblockBlockState block) {
		BlockPos pos = BlockPos.fromPos3(location);
		HellblockSection section = getSection(pos.sectionID());
		this.arrangeScheduledTickTaskForNewBlock(pos);
		return section.addBlockState(pos, block);
	}

	@NotNull
	@Override
	public Stream<HellblockSection> sectionsToSave() {
		return loadedSections.values().stream().filter(section -> !section.canPrune());
	}

	@NotNull
	@Override
	public Optional<HellblockSection> getLoadedSection(int sectionID) {
		return Optional.ofNullable(loadedSections.get(sectionID));
	}

	@Override
	public HellblockSection getSection(int sectionID) {
		return getLoadedSection(sectionID).orElseGet(() -> {
			HellblockSection section = new HellblockSection(sectionID);
			this.loadedSections.put(sectionID, section);
			return section;
		});
	}

	@Override
	public HellblockSection[] sections() {
		return loadedSections.values().toArray(new HellblockSection[0]);
	}

	@Override
	public Optional<HellblockSection> removeSection(int sectionID) {
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
		if (isOfflineTaskNotified())
			return;
		this.notified = true;
		long current = System.currentTimeMillis();
		int offlineTimeInSeconds = (int) (current - lastLoadedTime()) / 1000;
		WorldSetting setting = world.setting();
		offlineTimeInSeconds = Math.min(offlineTimeInSeconds, setting.maxOfflineTime());
		int minTickUnit = setting.minTickUnit();
		int randomTickSpeed = setting.randomTickSpeed();
		int threshold = setting.maxLoadingTime();
		int i = 0;
		long time1 = System.currentTimeMillis();
		while (i < offlineTimeInSeconds) {
			this.loadedSeconds++;
			if (this.loadedSeconds >= minTickUnit) {
				this.loadedSeconds = 0;
				this.tickedBlocks.clear();
				this.queue.clear();
				this.arrangeTasks(minTickUnit);
			}
			scheduledTick(true);
			randomTick(randomTickSpeed, true);
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
		WorldSetting setting = world.setting();
		if (!tickedBlocks.contains(pos)) {
			tickedBlocks.add(pos);
			int random = RandomUtils.generateRandomInt(0, setting.minTickUnit() - 1);
			if (random > loadedSeconds) {
				queue.add(new DelayedTickTask(random, pos));
			}
		}
	}
}