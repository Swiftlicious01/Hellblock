package com.swiftlicious.hellblock.listeners.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.LogUtils;

public class GeneratorModeManager {

	private final HellblockPlugin instance;

	private List<GenMode> generatorModes;
	private final GenMode defaultGenMode;

	public GeneratorModeManager(HellblockPlugin plugin) {
		instance = plugin;
		this.generatorModes = new ArrayList<>();
		this.defaultGenMode = new GenMode(Material.NETHERRACK);
	}

	public void loadFromConfig() {
		this.generatorModes = new ArrayList<>();
		if (instance.getConfig("config.yml").contains("netherrack-generator-options.generation")) {

			ConfigurationSection section = instance.getConfig("config.yml")
					.getConfigurationSection("netherrack-generator-options.generation");
			if (section == null) {
				LogUtils.severe("No generation mode section found");
				return;
			}
			Material fallbackMaterial = null;
			if (section.contains("fallback")) {
				fallbackMaterial = Material
						.getMaterial(Objects.requireNonNull(section.getString("fallback")).toUpperCase());
				if (fallbackMaterial == null) {
					LogUtils.severe(
							String.format("%s is not a valid fallback material", section.getString("fallback")));
				}
			}
			GenMode mode = new GenMode(fallbackMaterial);
			if (section.contains("searchForPlayersNearby")) {
				mode.setSearchForPlayersNearby(section.getBoolean("searchForPlayersNearby", false));
			}
			if (section.contains("generationSound")) {
				String soundString = section.getString("generationSound");
				if (soundString != null && !soundString.equalsIgnoreCase("none")) {
					Arrays.stream(Sound.values()).filter(sound -> sound.name().equalsIgnoreCase(soundString))
							.findFirst().ifPresentOrElse(mode::setGenSound,
									() -> LogUtils.severe(String.format("The sound %s does not exist.", soundString)));
				}
			}
			if (section.contains("particleEffect")) {
				String particle = section.getString("particleEffect");
				if (particle != null) {
					Particle[] effects = Particle.values();
					Arrays.stream(effects).filter(particleEffect -> particleEffect.name().equalsIgnoreCase(particle))
							.findFirst().ifPresentOrElse(mode::setParticleEffect,
									() -> LogUtils.severe(String.format("The particle %s does not exist.", particle)));
				}
			}

			if (section.contains("canGenerateWhileLavaRaining")) {
				boolean canGenWhileLavaRaining = section.getBoolean("canGenerateWhileLavaRaining");
				mode.setCanGenWhileLavaRaining(canGenWhileLavaRaining);
			}
			if (mode.isValid()) {
				this.generatorModes.add(mode);
			}
		}

		if (this.generatorModes.isEmpty()) {
			LogUtils.severe("COULD NOT FIND ANY GENERATION MODES IN CONFIG. USING DEFAULT INSTEAD!");
			this.generatorModes.add(this.defaultGenMode);
		}

	}

	public boolean isSupportedBlockFace(BlockFace blockFace) {
		if (blockFace == null)
			return false;
		return blockFace.equals(BlockFace.DOWN) || blockFace.equals(BlockFace.UP) || blockFace.equals(BlockFace.WEST)
				|| blockFace.equals(BlockFace.NORTH) || blockFace.equals(BlockFace.EAST)
				|| blockFace.equals(BlockFace.SOUTH);
	}

	public List<GenMode> getModes() {
		return this.generatorModes;
	}
}