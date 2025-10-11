package com.swiftlicious.hellblock.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.nms.entity.FakeNamedEntity;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.extras.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class HologramManager implements Listener, Reloadable {

	private final ConcurrentMap<UUID, HologramCache> hologramMap = new ConcurrentHashMap<>();
	protected final HellblockPlugin instance;
	private SchedulerTask cacheCheckTask;

	public HologramManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		this.cacheCheckTask = instance.getScheduler().asyncRepeating(() -> {
			final List<UUID> removed = new ArrayList<>();
			final long current = System.currentTimeMillis();
			hologramMap.entrySet().forEach(entry -> {
				final Player player = Bukkit.getPlayer(entry.getKey());
				if (player == null || !player.isOnline()) {
					removed.add(entry.getKey());
				} else {
					entry.getValue().removeOutDated(current, player);
				}
			});
			removed.forEach(hologramMap::remove);
		}, 100, 100, TimeUnit.MILLISECONDS);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		hologramMap.entrySet().forEach(entry -> {
			final Player player = Bukkit.getPlayer(entry.getKey());
			if (player != null && player.isOnline()) {
				entry.getValue().removeAll(player);
			}
		});
		if (cacheCheckTask != null && !cacheCheckTask.isCancelled()) {
			cacheCheckTask.cancel();
			cacheCheckTask = null;
		}
		this.hologramMap.clear();
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.hologramMap.remove(event.getPlayer().getUniqueId());
	}

	public void showHologram(Player player, Location location, String json, int millis) {
		HologramCache hologramCache = hologramMap.get(player.getUniqueId());
		if (hologramCache != null) {
			hologramCache.showHologram(player, location, json, millis);
		} else {
			hologramCache = new HologramCache();
			hologramCache.showHologram(player, location, json, millis);
			hologramMap.put(player.getUniqueId(), hologramCache);
		}
	}

	public static class HologramCache {

		private final ConcurrentMap<Location, Pair<FakeNamedEntity, Long>> cache = new ConcurrentHashMap<>();

		public void removeOutDated(long current, Player player) {
			final List<Location> removed = new ArrayList<>();
			cache.entrySet().stream().filter(entry -> entry.getValue().right() < current).forEach(entry -> {
				entry.getValue().left().destroy(player);
				removed.add(entry.getKey());
			});
			removed.forEach(cache::remove);
		}

		public void showHologram(Player player, Location location, String json, int millis) {
			final Pair<FakeNamedEntity, Long> pair = cache.get(location);
			if (pair != null) {
				pair.left().name(json);
				pair.left().updateMetaData(player);
				pair.right(System.currentTimeMillis() + millis);
			} else {
				final long removeTime = System.currentTimeMillis() + millis;
				if (VersionHelper.isVersionNewerThan1_19_4()) {
					final FakeTextDisplay fakeEntity = VersionHelper.getNMSManager()
							.createFakeTextDisplay(location.clone().add(0, 1.25, 0));
					fakeEntity.name(json);
					fakeEntity.rgba(0, 0, 0, 0);
					fakeEntity.spawn(player);
					this.cache.put(location, Pair.of(fakeEntity, removeTime));
				} else {
					final FakeArmorStand fakeEntity = VersionHelper.getNMSManager().createFakeArmorStand(location);
					fakeEntity.name(json);
					fakeEntity.small(true);
					fakeEntity.invisible(true);
					fakeEntity.spawn(player);
					this.cache.put(location, Pair.of(fakeEntity, removeTime));
				}
			}
		}

		public void removeAll(Player player) {
			this.cache.entrySet().forEach(entry -> entry.getValue().left().destroy(player));
			cache.clear();
		}
	}
}