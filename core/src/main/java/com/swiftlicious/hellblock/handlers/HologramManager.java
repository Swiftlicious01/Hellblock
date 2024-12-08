package com.swiftlicious.hellblock.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.nms.entity.FakeNamedEntity;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HologramManager implements Reloadable {

	protected final HellblockPlugin instance;
	private final ConcurrentMap<Location, Hologram> holograms = new ConcurrentHashMap<>();
	private SchedulerTask task;

	public HologramManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		this.task = instance.getScheduler().sync().runRepeating(() -> {
			List<Location> toRemove = new ArrayList<>();
			for (Map.Entry<Location, Hologram> entry : holograms.entrySet()) {
				if (entry.getValue().reduceTicks()) {
					toRemove.add(entry.getKey());
					entry.getValue().destroy();
				}
			}
			for (Location location : toRemove) {
				holograms.remove(location);
			}
		}, 1, 1, null);
	}

	@Override
	public void unload() {
		if (this.task.isCancelled()) {
			this.task.cancel();
		}
		for (Hologram hologram : holograms.values()) {
			hologram.destroy();
		}
		holograms.clear();
	}

	public void createHologram(Location location, String json, int ticks, boolean displayEntity, int[] rgba,
			Set<Player> viewers) {
		Hologram hologram = holograms.get(location);
		if (hologram == null) {
			FakeNamedEntity fakeNamedEntity;
			if (displayEntity && instance.getVersionManager().isVersionNewerThan1_19_4()) {
				FakeTextDisplay textDisplay = instance.getVersionManager().getNMSManager()
						.createFakeTextDisplay(location);
				textDisplay.rgba(rgba[0], rgba[1], rgba[2], rgba[3]);
				fakeNamedEntity = textDisplay;
			} else {
				FakeArmorStand armorStand = instance.getVersionManager().getNMSManager().createFakeArmorStand(location);
				armorStand.small(true);
				armorStand.invisible(true);
				fakeNamedEntity = armorStand;
			}
			hologram = new Hologram(fakeNamedEntity);
			hologram.name(json);
			hologram.updateNearbyPlayers(viewers);
			hologram.setTicksRemaining(ticks);
			holograms.put(location, hologram);
		} else {
			hologram.name(json);
			hologram.updateNearbyPlayers(viewers);
			hologram.setTicksRemaining(ticks);
		}
	}
}