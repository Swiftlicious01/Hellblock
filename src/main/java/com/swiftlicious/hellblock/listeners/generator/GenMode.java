package com.swiftlicious.hellblock.listeners.generator;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import com.destroystokyo.paper.ParticleBuilder;
import com.swiftlicious.hellblock.utils.LogUtils;

public class GenMode {

	private boolean searchForPlayersNearby = false;
	private boolean canGenWhileLavaRaining = true;

	private Sound genSound = null;
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

	public Sound getGenSound() {
		return this.genSound;
	}

	public void setGenSound(Sound genSound) {
		if (genSound == null) {
			LogUtils.warn("Unknown sound variable defined for netherrack generator settings.");
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
			LogUtils.warn("Unknown particle effect variable defined for netherrack generator settings.");
			return;
		}
		this.particleEffect = particleEffect;
	}

	public void displayGenerationParticles(Location loc) {
		for (int i = 0; i < 10; i++) {
			Random rand = new Random();
			Location tempLoc = loc.clone().add(rand.nextDouble(), 1D, rand.nextDouble());
			float speed = 1 / (i + 1);
			ParticleBuilder builder = new ParticleBuilder(this.particleEffect);
			builder.location(tempLoc).count((int) speed).source(null).spawn();
		}
	}

	public boolean canGenerateWhileLavaRaining() {
		return this.canGenWhileLavaRaining;
	}

	public void setCanGenWhileLavaRaining(boolean canGenWhileLavaRaining) {
		this.canGenWhileLavaRaining = canGenWhileLavaRaining;
	}
}