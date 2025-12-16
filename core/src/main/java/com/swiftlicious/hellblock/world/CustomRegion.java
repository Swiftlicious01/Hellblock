package com.swiftlicious.hellblock.world;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;

public class CustomRegion implements CustomRegionInterface {

	private final HellblockWorld<?> world;
	private final RegionPos regionPos;
	private final ConcurrentMap<ChunkPos, byte[]> cachedChunks;
	private AtomicBoolean isLoaded = new AtomicBoolean(false);

	protected CustomRegion(HellblockWorld<?> world, RegionPos regionPos) {
		this.world = world;
		this.cachedChunks = new ConcurrentHashMap<>();
		this.regionPos = regionPos;
	}

	protected CustomRegion(HellblockWorld<?> world, RegionPos regionPos, ConcurrentMap<ChunkPos, byte[]> cachedChunks) {
		this.world = world;
		this.regionPos = regionPos;
		this.cachedChunks = cachedChunks;
	}

	@Override
	public boolean isLoaded() {
		return isLoaded.get();
	}

	@Override
	public void unload() {
		if (this.isLoaded.get() && ((HellblockWorld<?>) world).unloadRegion(this)) {
			this.isLoaded.set(true);
		}
	}

	@Override
	public void load() {
		if (!this.isLoaded.get() && ((HellblockWorld<?>) world).loadRegion(this)) {
			this.isLoaded.set(true);
		}
	}

	@NotNull
	@Override
	public HellblockWorld<?> getWorld() {
		return this.world;
	}

	@Override
	public byte[] getCachedChunkBytes(ChunkPos pos) {
		return this.cachedChunks.get(pos);
	}

	@NotNull
	@Override
	public RegionPos regionPos() {
		return this.regionPos;
	}

	@Override
	public boolean removeCachedChunk(ChunkPos pos) {
		return cachedChunks.remove(pos) != null;
	}

	@Override
	public void setCachedChunk(ChunkPos pos, byte[] data) {
		this.cachedChunks.put(pos, data);
	}

	@Override
	public Map<ChunkPos, byte[]> dataToSave() {
		return new HashMap<>(cachedChunks);
	}

	@Override
	public boolean canPrune() {
		return cachedChunks.isEmpty();
	}
}