package com.swiftlicious.hellblock.listeners.generator;

import java.util.Arrays;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class GeneratorModeManager {

	private GenMode generatorMode;

	public GeneratorModeManager(HellblockPlugin plugin) {
		generatorMode = new GenMode(Material.NETHERRACK);
	}

	public void loadFromConfig() {
		YamlDocument config = HellblockPlugin.getInstance().getConfigManager().getMainConfig();
		if (config.contains("netherrack-generator-options.generation")) {

			Section section = config.getSection("netherrack-generator-options.generation");
			if (section == null) {
				HellblockPlugin.getInstance().getPluginLogger().severe("No generation mode section found.");
				return;
			}
			Material fallbackMaterial = null;
			if (section.contains("fallback")) {
				fallbackMaterial = Material
						.getMaterial(Objects.requireNonNull(section.getString("fallback", "NETHERRACK").toUpperCase()));
				if (fallbackMaterial == null || fallbackMaterial == Material.AIR) {
					HellblockPlugin.getInstance().getPluginLogger().severe(
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
							.findFirst().ifPresentOrElse(mode::setParticleEffect,
									() -> HellblockPlugin.getInstance().getPluginLogger()
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

	public @NotNull GenMode getGenMode() {
		return this.generatorMode;
	}
}