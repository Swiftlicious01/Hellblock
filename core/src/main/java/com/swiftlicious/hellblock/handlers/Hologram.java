package com.swiftlicious.hellblock.handlers;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.nms.entity.FakeNamedEntity;

public class Hologram {

	private final FakeNamedEntity entity;
	private Set<Player> set1 = new HashSet<>();
	private int ticksRemaining = 0;

	public Hologram(FakeNamedEntity entity) {
		this.entity = entity;
	}

	public void name(String json) {
		entity.name(json);
	}

	public void destroy() {
		for (Player player : set1) {
			entity.destroy(player);
		}
		set1.clear();
	}

	public void setTicksRemaining(int ticks) {
		ticksRemaining = ticks;
	}

	public boolean reduceTicks() {
		ticksRemaining--;
		return ticksRemaining < 0;
	}

	public void updateNearbyPlayers(Set<Player> set2) {
		Set<Player> intersectionSet = new HashSet<>(set1);
		intersectionSet.retainAll(set2);
		Set<Player> uniqueToSet1 = new HashSet<>(set1);
		uniqueToSet1.removeAll(set2);
		Set<Player> uniqueToSet2 = new HashSet<>(set2);
		uniqueToSet2.removeAll(set1);
		for (Player p : uniqueToSet1) {
			entity.destroy(p);
		}
		for (Player p : uniqueToSet2) {
			entity.spawn(p);
		}
		for (Player p : intersectionSet) {
			entity.updateMetaData(p);
		}
		set1 = set2;
	}
}