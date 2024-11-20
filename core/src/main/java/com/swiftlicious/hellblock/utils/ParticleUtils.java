package com.swiftlicious.hellblock.utils;

import org.bukkit.Particle;

import com.swiftlicious.hellblock.HellblockPlugin;

public class ParticleUtils {

	public static Particle getParticle(String particle) {
		if (!HellblockPlugin.getInstance().getVersionManager().isVersionNewerThan1_20_5())
			return Particle.valueOf(particle);
		return switch (particle) {
		case "REDSTONE" -> Particle.valueOf("DUST");
		case "VILLAGER_HAPPY" -> Particle.valueOf("HAPPY_VILLAGER");
		default -> Particle.valueOf(particle);
		};
	}
}