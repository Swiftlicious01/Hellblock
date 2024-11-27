package com.swiftlicious.hellblock.handlers;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.nms.entity.FakeNamedEntity;

public class Hologram {

	private final FakeNamedEntity entity;
	private Set<Player> set = new HashSet<>();
	private int ticksRemaining = 0;

	public Hologram(FakeNamedEntity entity) {
		this.entity = entity;
	}

	public void name(String json) {
		entity.name(json);
	}

	public void destroy() {
		for (Player player : set) {
			entity.destroy(player);
		}
		set.clear();
	}

	public void setTicksRemaining(int ticks) {
		ticksRemaining = ticks;
	}

	public boolean reduceTicks() {
		ticksRemaining--;
		return ticksRemaining < 0;
	}

	public void updateNearbyPlayers(Set<Player> nearSet) {
		Set<Player> intersectionSet = new HashSet<>(set);
		intersectionSet.retainAll(nearSet);
		Set<Player> uniqueToSet = new HashSet<>(set);
		uniqueToSet.removeAll(nearSet);
		Set<Player> uniqueToNearSet = new HashSet<>(nearSet);
		uniqueToNearSet.removeAll(set);
		for (Player p : uniqueToSet) {
			entity.destroy(p);
		}
		for (Player p : uniqueToNearSet) {
			entity.spawn(p);
		}
		for (Player p : intersectionSet) {
			entity.updateMetaData(p);
		}
		set = nearSet;
	}
}