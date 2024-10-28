package com.swiftlicious.hellblock.listeners.generator;

import java.util.Arrays;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
						.getMaterial(Objects.requireNonNull(section.getString("fallback", "NETHERRACK")).toUpperCase());
				if (fallbackMaterial == null) {
					LogUtils.severe(
							String.format("%s is not a valid fallback material.", section.getString("fallback")));
				}
			}
			GenMode mode = new GenMode(fallbackMaterial);
			if (section.contains("searchForPlayersNearby")) {
				boolean searchForPlayersNearby = section.getBoolean("searchForPlayersNearby", false);
				mode.setSearchForPlayersNearby(searchForPlayersNearby);
			}
			if (section.contains("generationSound")) {
				String soundString = section.getString("generationSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
				if (soundString != null && !soundString.equalsIgnoreCase("none")) {
					Arrays.stream(Sound.values()).filter(sound -> sound.name().equalsIgnoreCase(soundString))
							.findFirst().ifPresentOrElse(mode::setGenSound,
									() -> LogUtils.severe(String.format("The sound %s does not exist.", soundString)));
				}
			}
			if (section.contains("particleEffect")) {
				String particleString = section.getString("particleEffect", "LARGE_SMOKE");
				if (particleString != null && !particleString.equalsIgnoreCase("none")) {
					Arrays.stream(Particle.values())
							.filter(particleEffect -> particleEffect.name().equalsIgnoreCase(particleString))
							.findFirst().ifPresentOrElse(mode::setParticleEffect, () -> LogUtils
									.severe(String.format("The particle %s does not exist.", particleString)));
				}
			}
			if (section.contains("canGenerateWhileLavaRaining")) {
				boolean canGenWhileLavaRaining = section.getBoolean("canGenerateWhileLavaRaining", true);
				mode.setCanGenWhileLavaRaining(canGenWhileLavaRaining);
			}
			
			this.generatorMode = mode;
		}
	}

	public GenMode getGenMode() {
		return this.generatorMode;
	}
}