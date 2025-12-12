package com.swiftlicious.hellblock.listeners.generator;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class GeneratorModeManager {

	protected final HellblockPlugin instance;
	private GenMode generatorMode;

	public GeneratorModeManager(HellblockPlugin plugin) {
		instance = plugin;
		generatorMode = new GenMode(Material.NETHERRACK);
	}

	public void loadFromConfig() {
		final YamlDocument config = instance.getConfigManager().getMainConfig();
		if (!config.contains("netherrack-generator-options.generation")) {
			return;
		}

		final Section section = config.getSection("netherrack-generator-options.generation");
		if (section == null) {
			instance.getPluginLogger().severe("No generation mode section found.");
			return;
		}

		Material fallbackMaterial = null;
		if (section.contains("fallback")) {
			fallbackMaterial = Material.matchMaterial(
					Objects.requireNonNull(section.getString("fallback", "NETHERRACK").toUpperCase(Locale.ROOT)));
			if (fallbackMaterial == null || fallbackMaterial == Material.AIR || !fallbackMaterial.isBlock()) {
				instance.getPluginLogger()
						.severe("%s is not a valid fallback block type.".formatted(section.getString("fallback")));
			}
		}

		final GenMode mode = new GenMode(fallbackMaterial);
		if (section.contains("search-for-players-nearby")) {
			final boolean searchForPlayersNearby = section.getBoolean("search-for-players-nearby", false);
			mode.setSearchForPlayersNearby(searchForPlayersNearby);
		}

		if (section.contains("generation-sound")) {
			final String soundString = section.getString("generation-sound", "minecraft:entity.experience_orb.pickup");
			if (soundString != null && !"none".equalsIgnoreCase(soundString)) {
				mode.setGenSound(soundString);
			}
		}

		if (section.contains("particle-effect")) {
			final String particleString = section.getString("particle-effect", "LARGE_SMOKE");
			if (particleString != null && !"none".equalsIgnoreCase(particleString)) {
				Arrays.stream(Particle.values())
						.filter(particleEffect -> particleEffect.name().equalsIgnoreCase(particleString)).findFirst()
						.ifPresentOrElse(mode::setParticleEffect, () -> instance.getPluginLogger()
								.severe("The particle %s does not exist.".formatted(particleString)));
			}
		}

		if (section.contains("require-sign-to-generate")) {
			final boolean requiresSignToGenerate = section.getBoolean("require-sign-to-generate", false);
			mode.setSignRequirementToGenerate(requiresSignToGenerate);
		}

		if (section.contains("can-generate-while-lava-raining")) {
			final boolean canGenWhileLavaRaining = section.getBoolean("can-generate-while-lava-raining", true);
			mode.setCanGenWhileLavaRaining(canGenWhileLavaRaining);
		}

		this.generatorMode = mode;
	}

	@NotNull
	public GenMode getGenMode() {
		return this.generatorMode;
	}
}