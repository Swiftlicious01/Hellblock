package com.swiftlicious.hellblock.listeners.generator;

import java.util.Arrays;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.LogUtils;

public class GeneratorModeManager {

	private final HellblockPlugin instance;
	private GenMode generatorMode;

	public GeneratorModeManager(HellblockPlugin plugin) {
		instance = plugin;
		generatorMode = new GenMode(Material.NETHERRACK);
	}

	public void loadFromConfig() {
		if (instance.getConfig("config.yml").contains("netherrack-generator-options.generation")) {

			ConfigurationSection section = instance.getConfig("config.yml")
					.getConfigurationSection("netherrack-generator-options.generation");
			if (section == null) {
				LogUtils.severe("No generation mode section found.");
				return;
			}
			Material fallbackMaterial = null;
			if (section.contains("fallback")) {
				fallbackMaterial = Material
						.getMaterial(Objects.requireNonNull(section.getString("fallback", "NETHERRACK").toUpperCase()));
				if (fallbackMaterial == null || fallbackMaterial == Material.AIR) {
					LogUtils.severe(
							String.format("%s is not a valid fallback material.", section.getString("fallback")));
				}
			}
			GenMode mode = new GenMode(fallbackMaterial);
			if (section.contains("search-for-players-nearby")) {
				boolean searchForPlayersNearby = section.getBoolean("search-for-players-nearby", false);
				mode.setSearchForPlayersNearby(searchForPlayersNearby);
			}
			if (section.contains("generation-sound")) {
				String soundString = section.getString("generation-sound", "minecraft:entity.experience_orb.pickup");
				if (soundString != null && !soundString.equalsIgnoreCase("none")) {
					mode.setGenSound(soundString);
				}
			}
			if (section.contains("particle-effect")) {
				String particleString = section.getString("particle-effect", "LARGE_SMOKE");
				if (particleString != null && !particleString.equalsIgnoreCase("none")) {
					Arrays.stream(Particle.values())
							.filter(particleEffect -> particleEffect.name().equalsIgnoreCase(particleString))
							.findFirst().ifPresentOrElse(mode::setParticleEffect, () -> LogUtils
									.severe(String.format("The particle %s does not exist.", particleString)));
				}
			}
			if (section.contains("can-generate-while-lava-raining")) {
				boolean canGenWhileLavaRaining = section.getBoolean("can-generate-while-lava-raining", true);
				mode.setCanGenWhileLavaRaining(canGenWhileLavaRaining);
			}

			this.generatorMode = mode;
		}
	}

	public GenMode getGenMode() {
		return this.generatorMode;
	}
}