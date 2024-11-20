package com.swiftlicious.hellblock.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class LocationCache {
	/** Limits the size of the cache */
	private static final int MAX_CACHE_SIZE = 50;

	private static final Queue<CachedLocation> locationCacheQueue = new ConcurrentLinkedQueue<CachedLocation>();

	public static Location getCachedLocation() {
		Location cacheLoc = locationCacheQueue.poll();
		return cacheLoc != null ? cacheLoc : new CachedLocation();
	}

	public static Location getCachedLocation(World world, double x, double y, double z) {
		Location cacheLoc = getCachedLocation();

		cacheLoc.setWorld(world);
		cacheLoc.setX(x);
		cacheLoc.setY(y);
		cacheLoc.setZ(z);

		return cacheLoc;
	}

	public static Location getCachedLocation(Location loc) {
		return getCachedLocation(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
	}

	public static Location getCachedLocation(Entity entity) {
		return entity.getLocation(getCachedLocation());
	}

	private static void storeLocationCache(CachedLocation cacheLoc) {
		locationCacheQueue.add(cacheLoc);
	}

	private static class CachedLocation extends Location {

		public CachedLocation() {
			super(null, 0.0, 0.0, 0.0);
		}

		@Override
		public void finalize() throws Exception {
			// Make sure we are not wasting too much memory :3
			if (locationCacheQueue.size() < MAX_CACHE_SIZE) {
				storeLocationCache(this);
				throw new Exception();
			}
		}
	}
}