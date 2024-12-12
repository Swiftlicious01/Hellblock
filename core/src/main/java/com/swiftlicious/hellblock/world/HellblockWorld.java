package com.swiftlicious.hellblock.world;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.scheduler.SchedulerAdapter;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.world.adapter.WorldAdapter;

public class HellblockWorld<W> implements HellblockWorldInterface<W> {

	private final ConcurrentMap<ChunkPos, HellblockChunk> loadedChunks = new ConcurrentHashMap<>(512);
	private final ConcurrentMap<ChunkPos, HellblockChunk> lazyChunks = new ConcurrentHashMap<>(128);
	private final ConcurrentMap<RegionPos, HellblockRegion> loadedRegions = new ConcurrentHashMap<>(128);
	private final WeakReference<W> world;
	private final WeakReference<World> bukkitWorld;
	private final String worldName;
	private int regionTimer;
	private SchedulerTask tickTask;
	private WorldSetting setting;
	private final WorldAdapter<W> adapter;
	private final WorldExtraData extraData;
	private final WorldScheduler scheduler;

	public HellblockWorld(W world, WorldAdapter<W> adapter) {
		this.world = new WeakReference<>(world);
		this.worldName = adapter.getName(world);
		this.bukkitWorld = new WeakReference<>(Bukkit.getWorld(worldName));
		this.regionTimer = 0;
		this.adapter = adapter;
		this.extraData = adapter.loadExtraData(world);
		this.scheduler = new WorldScheduler(HellblockPlugin.getInstance());
	}

	@NotNull
	@Override
	public WorldAdapter<W> adapter() {
		return adapter;
	}

	@NotNull
	@Override
	public WorldExtraData extraData() {
		return extraData;
	}

	@Override
	public boolean testChunkLimitation(Pos3 pos3, Class<? extends HellblockBlock> clazz, int amount) {
		Optional<HellblockChunk> optional = getChunk(pos3.toChunkPos());
		if (optional.isPresent()) {
			int i = 0;
			HellblockChunk chunk = optional.get();
			for (HellblockSection section : chunk.sections()) {
				for (HellblockBlockState state : section.blockMap().values()) {
					if (clazz.isAssignableFrom(state.type().getClass())) {
						i++;
						if (i >= amount) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean doesChunkHaveBlock(Pos3 pos3, Class<? extends HellblockBlock> clazz) {
		Optional<HellblockChunk> optional = getChunk(pos3.toChunkPos());
		if (optional.isPresent()) {
			HellblockChunk chunk = optional.get();
			for (HellblockSection section : chunk.sections()) {
				for (HellblockBlockState state : section.blockMap().values()) {
					if (clazz.isAssignableFrom(state.type().getClass())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public int getChunkBlockAmount(Pos3 pos3, Class<? extends HellblockBlock> clazz) {
		Optional<HellblockChunk> optional = getChunk(pos3.toChunkPos());
		if (optional.isPresent()) {
			int i = 0;
			HellblockChunk chunk = optional.get();
			for (HellblockSection section : chunk.sections()) {
				for (HellblockBlockState state : section.blockMap().values()) {
					if (clazz.isAssignableFrom(state.type().getClass())) {
						i++;
					}
				}
			}
			return i;
		} else {
			return 0;
		}
	}

	@Override
	public HellblockChunk[] loadedChunks() {
		return loadedChunks.values().toArray(new HellblockChunk[0]);
	}

	@Override
	public HellblockChunk[] lazyChunks() {
		return lazyChunks.values().toArray(new HellblockChunk[0]);
	}

	@Override
	public HellblockRegion[] loadedRegions() {
		return loadedRegions.values().toArray(new HellblockRegion[0]);
	}

	@Override
	public @NotNull Optional<HellblockBlockState> getLoadedBlockState(Pos3 location) {
		ChunkPos pos = location.toChunkPos();
		Optional<HellblockChunk> chunk = getLoadedChunk(pos);
		if (chunk.isEmpty()) {
			return Optional.empty();
		} else {
			HellblockChunk customChunk = chunk.get();
			return customChunk.getBlockState(location);
		}
	}

	@NotNull
	@Override
	public Optional<HellblockBlockState> getBlockState(Pos3 location) {
		ChunkPos pos = location.toChunkPos();
		Optional<HellblockChunk> chunk = getChunk(pos);
		if (chunk.isEmpty()) {
			return Optional.empty();
		} else {
			HellblockChunk customChunk = chunk.get();
			// to let the bukkit system trigger the ChunkUnloadEvent later
			customChunk.load(true);
			return customChunk.getBlockState(location);
		}
	}

	@NotNull
	@Override
	public Optional<HellblockBlockState> removeBlockState(Pos3 location) {
		ChunkPos pos = location.toChunkPos();
		Optional<HellblockChunk> chunk = getChunk(pos);
		if (chunk.isEmpty()) {
			return Optional.empty();
		} else {
			HellblockChunk customChunk = chunk.get();
			// to let the bukkit system trigger the ChunkUnloadEvent later
			customChunk.load(true);
			return customChunk.removeBlockState(location);
		}
	}

	@NotNull
	@Override
	public Optional<HellblockBlockState> addBlockState(Pos3 location, HellblockBlockState block) {
		ChunkPos pos = location.toChunkPos();
		HellblockChunk chunk = getOrCreateChunk(pos);
		return chunk.addBlockState(location, block);
	}

	@Override
	public void save(boolean async, boolean disabling) {
		if (async && !disabling) {
			this.scheduler.async().execute(this::save);
		} else {
			if (disabling) {
				save();
			} else {
				HellblockPlugin.getInstance().getScheduler().sync().run(this::save, null);
			}
		}
	}

	private void save() {
		long time1 = System.currentTimeMillis();
		for (HellblockChunk chunk : loadedChunks.values()) {
			this.adapter.saveChunk(this, chunk);
		}
		for (HellblockChunk chunk : lazyChunks.values()) {
			this.adapter.saveChunk(this, chunk);
		}
		for (HellblockRegion region : loadedRegions.values()) {
			this.adapter.saveRegion(this, region);
		}
		long time2 = System.currentTimeMillis();
		HellblockPlugin.getInstance().debug(() -> "Took " + (time2 - time1) + "ms to save world " + worldName
				+ ". Saved " + (lazyChunks.size() + loadedChunks.size()) + " chunks.");
	}

	@Override
	public void setTicking(boolean tick) {
		if (tick) {
			if (this.tickTask == null || this.tickTask.isCancelled())
				this.tickTask = this.scheduler.asyncRepeating(this::timer, 1, 1, TimeUnit.SECONDS);
		} else {
			if (this.tickTask != null && !this.tickTask.isCancelled())
				this.tickTask.cancel();
		}
	}

	private void timer() {
		saveLazyChunks();
		saveLazyRegions();
		if (setting().enableScheduler()) {
			tickChunks();
		}
	}

	private void tickChunks() {
		if (VersionHelper.isFolia()) {
			SchedulerAdapter<Location, World> scheduler = HellblockPlugin.getInstance().getScheduler();
			for (HellblockChunk chunk : loadedChunks.values()) {
				scheduler.sync().run(chunk::timer, bukkitWorld(), chunk.chunkPos().x(), chunk.chunkPos().z());
			}
		} else {
			for (HellblockChunk chunk : loadedChunks.values()) {
				chunk.timer();
			}
		}
	}

	private void saveLazyRegions() {
		this.regionTimer++;
		// To avoid the same timing as saving
		if (this.regionTimer >= 666) {
			this.regionTimer = 0;
			ArrayList<HellblockRegion> removed = new ArrayList<>();
			for (Map.Entry<RegionPos, HellblockRegion> entry : loadedRegions.entrySet()) {
				if (shouldUnloadRegion(entry.getKey())) {
					removed.add(entry.getValue());
				}
			}
			for (HellblockRegion region : removed) {
				region.unload();
			}
		}
	}

	private void saveLazyChunks() {
		ArrayList<HellblockChunk> chunksToSave = new ArrayList<>();
		for (Map.Entry<ChunkPos, HellblockChunk> lazyEntry : this.lazyChunks.entrySet()) {
			HellblockChunk chunk = lazyEntry.getValue();
			int sec = chunk.lazySeconds() + 1;
			if (sec >= 30) {
				chunksToSave.add(chunk);
			} else {
				chunk.lazySeconds(sec);
			}
		}
		for (HellblockChunk chunk : chunksToSave) {
			unloadLazyChunk(chunk.chunkPos());
		}
	}

	@Override
	public W world() {
		return world.get();
	}

	@Override
	public World bukkitWorld() {
		return bukkitWorld.get();
	}

	@Override
	public String worldName() {
		return worldName;
	}

	@Override
	public @NotNull WorldSetting setting() {
		return setting;
	}

	@Override
	public void setting(WorldSetting setting) {
		this.setting = setting;
	}

	@Nullable
	public HellblockChunk removeLazyChunk(ChunkPos chunkPos) {
		return this.lazyChunks.remove(chunkPos);
	}

	public void deleteChunk(ChunkPos chunkPos) {
		this.lazyChunks.remove(chunkPos);
		this.loadedChunks.remove(chunkPos);
		getRegion(RegionPos.getByChunkPos(chunkPos)).ifPresent(region -> region.removeCachedChunk(chunkPos));
	}

	@Nullable
	public HellblockChunk getLazyChunk(ChunkPos chunkPos) {
		return this.lazyChunks.get(chunkPos);
	}

	@Override
	public boolean isChunkLoaded(ChunkPos pos) {
		return this.loadedChunks.containsKey(pos);
	}

	public boolean loadChunk(HellblockChunk chunk) {
		Optional<HellblockChunk> previousChunk = getLoadedChunk(chunk.chunkPos());
		if (previousChunk.isPresent()) {
			HellblockPlugin.getInstance().debug(() -> "Chunk " + chunk.chunkPos() + " already loaded.");
			if (previousChunk.get() != chunk) {
				HellblockPlugin.getInstance().getPluginLogger().severe(
						"Failed to load the chunk. There is already a different chunk instance with the same coordinates in the cache. "
								+ chunk.chunkPos());
				return false;
			}
			return true;
		}
		this.loadedChunks.put(chunk.chunkPos(), chunk);
		this.lazyChunks.remove(chunk.chunkPos());
		return true;
	}

	@ApiStatus.Internal
	public boolean unloadChunk(HellblockChunk chunk, boolean lazy) {
		ChunkPos pos = chunk.chunkPos();
		Optional<HellblockChunk> previousChunk = getLoadedChunk(chunk.chunkPos());
		if (previousChunk.isPresent()) {
			if (previousChunk.get() != chunk) {
				HellblockPlugin.getInstance().getPluginLogger().severe(
						"Failed to remove the chunk. The provided chunk instance is inconsistent with the one in the cache. "
								+ chunk.chunkPos());
				return false;
			}
		} else {
			return false;
		}
		this.loadedChunks.remove(chunk.chunkPos());
		chunk.updateLastUnloadTime();
		if (lazy) {
			this.lazyChunks.put(pos, chunk);
		} else {
			this.adapter.saveChunk(this, chunk);
		}
		return true;
	}

	@ApiStatus.Internal
	public boolean unloadChunk(ChunkPos pos, boolean lazy) {
		HellblockChunk removed = this.loadedChunks.remove(pos);
		if (removed != null) {
			removed.updateLastUnloadTime();
			if (lazy) {
				this.lazyChunks.put(pos, removed);
			} else {
				this.adapter.saveChunk(this, removed);
			}
			return true;
		}
		return false;
	}

	@ApiStatus.Internal
	public boolean unloadLazyChunk(ChunkPos pos) {
		HellblockChunk removed = this.lazyChunks.remove(pos);
		if (removed != null) {
			this.adapter.saveChunk(this, removed);
			return true;
		}
		return false;
	}

	@NotNull
	@Override
	public Optional<HellblockChunk> getLoadedChunk(ChunkPos chunkPos) {
		return Optional.ofNullable(this.loadedChunks.get(chunkPos));
	}

	@NotNull
	@Override
	public Optional<HellblockChunk> getChunk(ChunkPos chunkPos) {
		return Optional.ofNullable(getLoadedChunk(chunkPos).orElseGet(() -> {
			HellblockChunk chunk = getLazyChunk(chunkPos);
			if (chunk != null) {
				return chunk;
			}
			return this.adapter.loadChunk(this, chunkPos, false);
		}));
	}

	@NotNull
	@Override
	public HellblockChunk getOrCreateChunk(ChunkPos chunkPos) {
		return Objects.requireNonNull(getLoadedChunk(chunkPos).orElseGet(() -> {
			HellblockChunk chunk = getLazyChunk(chunkPos);
			if (chunk != null) {
				return chunk;
			}
			chunk = this.adapter.loadChunk(this, chunkPos, true);
			// to let the bukkit system trigger the ChunkUnloadEvent later
			chunk.load(true);
			return chunk;
		}));
	}

	/*
	 * Regions
	 */
	@Override
	public boolean isRegionLoaded(RegionPos pos) {
		return this.loadedRegions.containsKey(pos);
	}

	@ApiStatus.Internal
	public boolean loadRegion(HellblockRegion region) {
		Optional<HellblockRegion> previousRegion = getLoadedRegion(region.regionPos());
		if (previousRegion.isPresent()) {
			HellblockPlugin.getInstance().debug(() -> "Region " + region.regionPos() + " already loaded.");
			if (previousRegion.get() != region) {
				HellblockPlugin.getInstance().getPluginLogger().severe(
						"Failed to load the region. There is already a different region instance with the same coordinates in the cache. "
								+ region.regionPos());
				return false;
			}
			return true;
		}
		this.loadedRegions.put(region.regionPos(), region);
		return true;
	}

	@NotNull
	@Override
	public Optional<HellblockRegion> getLoadedRegion(RegionPos regionPos) {
		return Optional.ofNullable(loadedRegions.get(regionPos));
	}

	@NotNull
	@Override
	public Optional<HellblockRegion> getRegion(RegionPos regionPos) {
		return Optional.ofNullable(getLoadedRegion(regionPos).orElse(adapter.loadRegion(this, regionPos, false)));
	}

	@NotNull
	@Override
	public HellblockRegion getOrCreateRegion(RegionPos regionPos) {
		return Objects.requireNonNull(getLoadedRegion(regionPos).orElse(adapter.loadRegion(this, regionPos, true)));
	}

	private boolean shouldUnloadRegion(RegionPos regionPos) {
		World bukkitWorld = bukkitWorld();
		for (int chunkX = regionPos.x() * 32; chunkX < regionPos.x() * 32 + 32; chunkX++) {
			for (int chunkZ = regionPos.z() * 32; chunkZ < regionPos.z() * 32 + 32; chunkZ++) {
				// if a chunk is unloaded, then it should not be in the loaded chunks map
				ChunkPos pos = ChunkPos.of(chunkX, chunkZ);
				if (isChunkLoaded(pos) || this.lazyChunks.containsKey(pos)
						|| bukkitWorld.isChunkLoaded(chunkX, chunkZ)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean unloadRegion(HellblockRegion region) {
		Optional<HellblockRegion> previousRegion = getLoadedRegion(region.regionPos());
		if (previousRegion.isPresent()) {
			if (previousRegion.get() != region) {
				HellblockPlugin.getInstance().getPluginLogger().severe(
						"Failed to remove the region. The provided region instance is inconsistent with the one in the cache. "
								+ region.regionPos());
				return false;
			}
		} else {
			return false;
		}
		RegionPos regionPos = region.regionPos();
		for (int chunkX = regionPos.x() * 32; chunkX < regionPos.x() * 32 + 32; chunkX++) {
			for (int chunkZ = regionPos.z() * 32; chunkZ < regionPos.z() * 32 + 32; chunkZ++) {
				ChunkPos pos = ChunkPos.of(chunkX, chunkZ);
				if (!unloadLazyChunk(pos)) {
					unloadChunk(pos, false);
				}
			}
		}
		this.adapter.saveRegion(this, region);
		this.loadedRegions.remove(region.regionPos());
		HellblockPlugin.getInstance()
				.debug(() -> "[" + worldName + "] " + "Region " + region.regionPos() + " unloaded.");
		return true;
	}

	@Override
	public WorldScheduler scheduler() {
		return scheduler;
	}
}