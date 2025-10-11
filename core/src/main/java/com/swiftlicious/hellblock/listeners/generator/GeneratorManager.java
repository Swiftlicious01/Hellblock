package com.swiftlicious.hellblock.listeners.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.swiftlicious.hellblock.listeners.NetherGeneratorHandler.LocationKey;

public class GeneratorManager {

	private final List<LocationKey> knownGenLocations = new ArrayList<>();
	private final Map<LocationKey, GenBlock> genBreaks = new HashMap<>();
	private Map<LocationKey, GenPiston> knownGenPistons = new HashMap<>();

	public List<LocationKey> getKnownGenLocations() {
		return knownGenLocations;
	}

	public boolean isGenLocationKnown(LocationKey l) {
		return this.getKnownGenLocations().contains(l);
	}

	public void addKnownGenLocation(LocationKey lk) {
		if (this.isGenLocationKnown(lk)) {
			return;
		}
		this.getKnownGenLocations().add(lk);
	}

	public void removeKnownGenLocation(LocationKey l) {
		if (this.isGenLocationKnown(l)) {
			this.getKnownGenLocations().remove(l);
		}
		this.genBreaks.remove(l);
	}

	public void setPlayerForLocation(UUID uuid, LocationKey l, boolean pistonPowered) {
		this.addKnownGenLocation(l);
		this.getGenBreaks().remove(l);

		// Create a new GenBlock object to track the player+timestamp and add it to the
		// genBreaks map
		final GenBlock gb = new GenBlock(l, uuid, pistonPowered);
		this.getGenBreaks().put(l, gb);
	}

	public Map<LocationKey, GenBlock> getGenBreaks() {
		return genBreaks;
	}

	public void cleanupExpiredLocations() {
		// Remove all expired GenBlock entries
		final Set<Map.Entry<LocationKey, GenBlock>> entrySet = genBreaks.entrySet();
		if (entrySet.isEmpty()) {
			return;
		}
		final List<GenBlock> expiredBlocks = new ArrayList<>();
		entrySet.stream().map(Map.Entry::getValue).filter(GenBlock::hasExpired).forEach(expiredBlocks::add);
		expiredBlocks.forEach(genBlock -> removeKnownGenLocation(genBlock.getLocation()));
	}

	public void cleanupExpiredPistons(UUID uuid) {
		// Remove all expired GenBlock entries
		if (knownGenPistons == null) {
			return;
		}
		final Set<Map.Entry<LocationKey, GenPiston>> entrySet = knownGenPistons.entrySet();
		if (entrySet.isEmpty()) {
			return;
		}
		final List<GenPiston> expiredPistons = new ArrayList<>();
		for (Map.Entry<LocationKey, GenPiston> entry : entrySet) {
			final GenPiston piston = entry.getValue();
			final Block block = piston.getLoc().getBlock();
			if (block != null && block.getType() == Material.PISTON) {
				expiredPistons.add(piston);
				continue;
			}
			if (piston.getUUID().equals(uuid) && !piston.hasBeenUsed()) {
				expiredPistons.add(piston);
			}
		}
		expiredPistons.forEach(this::removeKnownGenPiston);
	}

	public Map<LocationKey, GenPiston> getKnownGenPistons() {
		return knownGenPistons;
	}

	public void setKnownGenPistons(Map<LocationKey, GenPiston> knownGenPistons) {
		this.knownGenPistons = knownGenPistons;
	}

	public void addKnownGenPiston(GenPiston piston) {
		final LocationKey location = piston.getLoc();
		this.getKnownGenPistons().remove(location);
		this.getKnownGenPistons().put(location, piston);
	}

	public void removeKnownGenPiston(GenPiston piston) {
		final LocationKey location = piston.getLoc();
		this.getKnownGenPistons().remove(location);
	}

	public GenPiston[] getGenPistonsByUUID(UUID uuid) {
		return this.getKnownGenPistons() != null ? this.getKnownGenPistons().values().stream()
				.filter(piston -> piston.getUUID() != null && piston.getUUID().equals(uuid)).toArray(GenPiston[]::new)
				: new GenPiston[0];
	}
}