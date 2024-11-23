package com.swiftlicious.hellblock.listeners.generator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class GenMode {

	private boolean searchForPlayersNearby = false;
	private boolean canGenWhileLavaRaining = true;

	private String genSound = null;
	private Particle particleEffect = null;
	private Material fallbackMaterial = null;

	public GenMode(Material fallbackMaterial) {
		this(true, fallbackMaterial);
	}

	public GenMode(boolean searchForPlayersNearby, Material fallbackMaterial) {
		this.setSearchForPlayersNearby(searchForPlayersNearby);
		this.setFallbackMaterial(fallbackMaterial);
	}

	public boolean isSearchingForPlayersNearby() {
		return this.searchForPlayersNearby;
	}

	public void setSearchForPlayersNearby(boolean searchForPlayersNearby) {
		this.searchForPlayersNearby = searchForPlayersNearby;
	}

	public boolean hasFallBackMaterial() {
		return this.getFallbackMaterial() != null;
	}

	public Material getFallbackMaterial() {
		return this.fallbackMaterial;
	}

	public void setFallbackMaterial(Material fallbackMaterial) {
		this.fallbackMaterial = fallbackMaterial;
	}

	public boolean hasGenSound() {
		return this.getGenSound() != null;
	}

	public String getGenSound() {
		return this.genSound;
	}

	public void setGenSound(String genSound) {
		if (genSound == null) {
			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Unknown sound variable defined for netherrack generator settings.");
			return;
		}
		this.genSound = genSound;
	}

	public boolean hasParticleEffect() {
		return this.getParticleEffect() != null;
	}

	public Particle getParticleEffect() {
		return this.particleEffect;
	}

	public void setParticleEffect(Particle particleEffect) {
		if (particleEffect == null) {
			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Unknown particle effect variable defined for netherrack generator settings.");
			return;
		}
		this.particleEffect = particleEffect;
	}

	public void displayGenerationParticles(Location loc) {
		if (loc.getWorld() == null)
			return;
		for (int i = 0; i < 10; i++) {
			Location tempLoc = loc.clone().add(RandomUtils.generateRandomDouble(), 1D,
					RandomUtils.generateRandomDouble());
			float speed = 1 / (i + 1);
			loc.getWorld().spawnParticle(this.particleEffect, tempLoc, (int) speed);
		}
	}

	public boolean canGenerateWhileLavaRaining() {
		return this.canGenWhileLavaRaining;
	}

	public void setCanGenWhileLavaRaining(boolean canGenWhileLavaRaining) {
		this.canGenWhileLavaRaining = canGenWhileLavaRaining;
	}
}