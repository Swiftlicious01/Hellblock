package com.swiftlicious.hellblock.utils;

import org.bukkit.Particle;

import com.swiftlicious.hellblock.handlers.VersionHelper;

public class ParticleUtils {

	public static Particle getParticle(String particle) {
		if (!VersionHelper.isVersionNewerThan1_20_5())
			return Particle.valueOf(particle);
		return switch (particle) {
		case "REDSTONE" -> Particle.valueOf("DUST");
		case "VILLAGER_HAPPY" -> Particle.valueOf("HAPPY_VILLAGER");
		default -> Particle.valueOf(particle);
		};
	}
}