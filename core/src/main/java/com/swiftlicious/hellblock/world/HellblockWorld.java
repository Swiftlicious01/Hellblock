package com.swiftlicious.hellblock.world;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
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

public class HellblockWorld<W> implements CustomWorldInterface<W> {

	private final ConcurrentMap<ChunkPos, CustomChunk> loadedChunks = new ConcurrentHashMap<>(512);
	private final ConcurrentMap<ChunkPos, CustomChunk> lazyChunks = new ConcurrentHashMap<>(128);
	private final ConcurrentMap<RegionPos, CustomRegion> loadedRegions = new ConcurrentHashMap<>(128);
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
	public boolean testChunkLimitation(Pos3 pos3, Class<? extends CustomBlock> clazz, int amount) {
		final Optional<CustomChunk> optional = getChunk(pos3.toChunkPos());
		if (optional.isPresent()) {
			int i = 0;
			final CustomChunk chunk = optional.get();
			for (CustomSection section : chunk.sections()) {
				for (CustomBlockState state : section.blockMap().values()) {
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
	public boolean doesChunkHaveBlock(Pos3 pos3, Class<? extends CustomBlock> clazz) {
		final Optional<CustomChunk> optional = getChunk(pos3.toChunkPos());
		if (optional.isPresent()) {
			final CustomChunk chunk = optional.get();
			for (CustomSection section : chunk.sections()) {
				for (CustomBlockState state : section.blockMap().values()) {
					if (clazz.isAssignableFrom(state.type().getClass())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public int getChunkBlockAmount(Pos3 pos3, Class<? extends CustomBlock> clazz) {
		final Optional<CustomChunk> optional = getChunk(pos3.toChunkPos());
		if (!optional.isPresent()) {
			return 0;
		}
		int i = 0;
		final CustomChunk chunk = optional.get();
		for (CustomSection section : chunk.sections()) {
			for (CustomBlockState state : section.blockMap().values()) {
				if (clazz.isAssignableFrom(state.type().getClass())) {
					i++;
				}
			}
		}
		return i;
	}

	@Override
	public CustomChunk[] loadedChunks() {
		return loadedChunks.values().toArray(new CustomChunk[0]);
	}

	@Override
	public CustomChunk[] lazyChunks() {
		return lazyChunks.values().toArray(new CustomChunk[0]);
	}

	@Override
	public CustomRegion[] loadedRegions() {
		return loadedRegions.values().toArray(new CustomRegion[0]);
	}

	@Override
	public @NotNull Optional<CustomBlockState> getLoadedBlockState(Pos3 location) {
		final ChunkPos pos = location.toChunkPos();
		final Optional<CustomChunk> chunk = getLoadedChunk(pos);
		if (chunk.isEmpty()) {
			return Optional.empty();
		} else {
			final CustomChunk customChunk = chunk.get();
			return customChunk.getBlockState(location);
		}
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> getBlockState(Pos3 location) {
		final ChunkPos pos = location.toChunkPos();
		final Optional<CustomChunk> chunk = getChunk(pos);
		if (chunk.isEmpty()) {
			return Optional.empty();
		} else {
			final CustomChunk customChunk = chunk.get();
			// to let the bukkit system trigger the ChunkUnloadEvent later
			customChunk.load(true);
			return customChunk.getBlockState(location);
		}
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> removeBlockState(Pos3 location) {
		final ChunkPos pos = location.toChunkPos();
		final Optional<CustomChunk> chunk = getChunk(pos);
		if (chunk.isEmpty()) {
			return Optional.empty();
		} else {
			final CustomChunk customChunk = chunk.get();
			// to let the bukkit system trigger the ChunkUnloadEvent later
			customChunk.load(true);
			return customChunk.removeBlockState(location);
		}
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> addBlockState(Pos3 location, CustomBlockState block) {
		final ChunkPos pos = location.toChunkPos();
		final CustomChunk chunk = getOrCreateChunk(pos);
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
		final long time1 = System.currentTimeMillis();
		loadedChunks.values().forEach(chunk -> this.adapter.saveChunk(this, chunk));
		lazyChunks.values().forEach(chunk -> this.adapter.saveChunk(this, chunk));
		loadedRegions.values().forEach(region -> this.adapter.saveRegion(this, region));
		final long time2 = System.currentTimeMillis();
		HellblockPlugin.getInstance().debug(() -> "Took " + (time2 - time1) + "ms to save world " + worldName
				+ ". Saved " + (lazyChunks.size() + loadedChunks.size()) + " chunks.");
	}

	@Override
	public void setTicking(boolean tick) {
		if (tick) {
			if (this.tickTask == null || this.tickTask.isCancelled()) {
				this.tickTask = this.scheduler.asyncRepeating(this::timer, 1, 1, TimeUnit.SECONDS);
			}
		} else {
			if (this.tickTask != null && !this.tickTask.isCancelled()) {
				this.tickTask.cancel();
				this.tickTask = null;
			}
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
			final SchedulerAdapter<Location, World> scheduler = HellblockPlugin.getInstance().getScheduler();
			loadedChunks.values().forEach(chunk -> scheduler.sync().run(chunk::timer, bukkitWorld(),
					chunk.chunkPos().x(), chunk.chunkPos().z()));
		} else {
			loadedChunks.values().forEach(CustomChunk::timer);
		}
	}

	private void saveLazyRegions() {
		this.regionTimer++;
		// To avoid the same timing as saving
		if (this.regionTimer < 666) {
			return;
		}
		this.regionTimer = 0;
		final List<CustomRegion> removed = new ArrayList<>();
		loadedRegions.entrySet().stream().filter(entry -> shouldUnloadRegion(entry.getKey()))
				.forEach(entry -> removed.add(entry.getValue()));
		removed.forEach(CustomRegion::unload);
	}

	private void saveLazyChunks() {
		final List<CustomChunk> chunksToSave = new ArrayList<>();
		this.lazyChunks.entrySet().stream().map(Map.Entry::getValue).forEach(chunk -> {
			final int sec = chunk.lazySeconds() + 1;
			if (sec >= 30) {
				chunksToSave.add(chunk);
			} else {
				chunk.lazySeconds(sec);
			}
		});
		chunksToSave.forEach(chunk -> unloadLazyChunk(chunk.chunkPos()));
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
	public CustomChunk removeLazyChunk(ChunkPos chunkPos) {
		return this.lazyChunks.remove(chunkPos);
	}

	public void deleteChunk(ChunkPos chunkPos) {
		this.lazyChunks.remove(chunkPos);
		this.loadedChunks.remove(chunkPos);
		getRegion(RegionPos.getByChunkPos(chunkPos)).ifPresent(region -> region.removeCachedChunk(chunkPos));
	}

	@Nullable
	public CustomChunk getLazyChunk(ChunkPos chunkPos) {
		return this.lazyChunks.get(chunkPos);
	}

	@Override
	public boolean isChunkLoaded(ChunkPos pos) {
		return this.loadedChunks.containsKey(pos);
	}

	public boolean loadChunk(CustomChunk chunk) {
		final Optional<CustomChunk> previousChunk = getLoadedChunk(chunk.chunkPos());
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
	public boolean unloadChunk(CustomChunk chunk, boolean lazy) {
		final ChunkPos pos = chunk.chunkPos();
		final Optional<CustomChunk> previousChunk = getLoadedChunk(chunk.chunkPos());
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
		final CustomChunk removed = this.loadedChunks.remove(pos);
		if (removed == null) {
			return false;
		}
		removed.updateLastUnloadTime();
		if (lazy) {
			this.lazyChunks.put(pos, removed);
		} else {
			this.adapter.saveChunk(this, removed);
		}
		return true;
	}

	@ApiStatus.Internal
	public boolean unloadLazyChunk(ChunkPos pos) {
		final CustomChunk removed = this.lazyChunks.remove(pos);
		if (removed == null) {
			return false;
		}
		this.adapter.saveChunk(this, removed);
		return true;
	}

	@NotNull
	@Override
	public Optional<CustomChunk> getLoadedChunk(ChunkPos chunkPos) {
		return Optional.ofNullable(this.loadedChunks.get(chunkPos));
	}

	@NotNull
	@Override
	public Optional<CustomChunk> getChunk(ChunkPos chunkPos) {
		return Optional.ofNullable(getLoadedChunk(chunkPos).orElseGet(() -> {
			final CustomChunk chunk = getLazyChunk(chunkPos);
			if (chunk != null) {
				return chunk;
			}
			return this.adapter.loadChunk(this, chunkPos, false);
		}));
	}

	@NotNull
	@Override
	public CustomChunk getOrCreateChunk(ChunkPos chunkPos) {
		return Objects.requireNonNull(getLoadedChunk(chunkPos).orElseGet(() -> {
			CustomChunk chunk = getLazyChunk(chunkPos);
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
	public boolean loadRegion(CustomRegion region) {
		final Optional<CustomRegion> previousRegion = getLoadedRegion(region.regionPos());
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
	public Optional<CustomRegion> getLoadedRegion(RegionPos regionPos) {
		return Optional.ofNullable(loadedRegions.get(regionPos));
	}

	@NotNull
	@Override
	public Optional<CustomRegion> getRegion(RegionPos regionPos) {
		return Optional.ofNullable(getLoadedRegion(regionPos).orElse(adapter.loadRegion(this, regionPos, false)));
	}

	@NotNull
	@Override
	public CustomRegion getOrCreateRegion(RegionPos regionPos) {
		return Objects.requireNonNull(getLoadedRegion(regionPos).orElse(adapter.loadRegion(this, regionPos, true)));
	}

	private boolean shouldUnloadRegion(RegionPos regionPos) {
		final World bukkitWorld = bukkitWorld();
		for (int chunkX = regionPos.x() * 32; chunkX < regionPos.x() * 32 + 32; chunkX++) {
			for (int chunkZ = regionPos.z() * 32; chunkZ < regionPos.z() * 32 + 32; chunkZ++) {
				// if a chunk is unloaded, then it should not be in the loaded chunks map
				final ChunkPos pos = ChunkPos.of(chunkX, chunkZ);
				if (isChunkLoaded(pos) || this.lazyChunks.containsKey(pos)
						|| bukkitWorld.isChunkLoaded(chunkX, chunkZ)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean unloadRegion(CustomRegion region) {
		final Optional<CustomRegion> previousRegion = getLoadedRegion(region.regionPos());
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
		final RegionPos regionPos = region.regionPos();
		for (int chunkX = regionPos.x() * 32; chunkX < regionPos.x() * 32 + 32; chunkX++) {
			for (int chunkZ = regionPos.z() * 32; chunkZ < regionPos.z() * 32 + 32; chunkZ++) {
				final ChunkPos pos = ChunkPos.of(chunkX, chunkZ);
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