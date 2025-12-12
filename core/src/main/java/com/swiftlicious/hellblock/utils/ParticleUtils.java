package com.swiftlicious.hellblock.utils;

import java.util.Locale;

import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.handlers.VersionHelper;

public class ParticleUtils {

	private ParticleUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Resolves a particle name safely across multiple Minecraft versions.
	 * <p>
	 * This method accounts for renamed or missing particles between versions. It
	 * also safely handles cases where a particle does not exist (e.g. SCULK_SOUL
	 * before 1.19).
	 *
	 * @param particle the particle name (case-insensitive)
	 * @return a valid {@link Particle}, or a safe fallback (SMOKE) if unavailable
	 */
	public static Particle getParticle(@NotNull String particle) {
		String name = particle.toUpperCase(Locale.ROOT);

		// --- Handle pre-1.19 SCULK_SOUL gracefully ---
		if ("SCULK_SOUL".equals(name)) {
			if (!EnumUtils.isValidEnum(Particle.class, "SCULK_SOUL")) {
				// particle doesn't exist before 1.19
				return safeParticle("SMOKE");
			}
			return Particle.valueOf("SCULK_SOUL");
		}

		// --- Handle renamed particles for 1.20.5+ ---
		if (VersionHelper.isVersionNewerThan1_20_5()) {
			switch (name) {
			case "REDSTONE" -> name = "DUST";
			case "VILLAGER_HAPPY" -> name = "HAPPY_VILLAGER";
			case "VILLAGER_ANGRY" -> name = "ANGRY_VILLAGER";
			case "BLOCK_CRACK" -> name = "BLOCK_CRUMBLE";
			case "BLOCK_DUST" -> name = "BLOCK";
			case "SPELL_WITCH" -> name = "WITCH";
			case "SPELL_INSTANT" -> name = "INSTANT_EFFECT";
			case "SPELL_MOB" -> name = "ENTITY_EFFECT";
			case "ENCHANTMENT_TABLE" -> name = "ENCHANT";
			case "DRIP_LAVA" -> name = "DRIPPING_LAVA";
			case "SMOKE_LARGE" -> name = "LARGE_SMOKE";
			case "SMOKE_NORMAL" -> name = "SMOKE";
			case "EXPLOSION_LARGE" -> name = "EXPLOSION";
			case "EXPLOSION_HUGE" -> name = "EXPLOSION_EMITTER";
			case "TOTEM" -> name = "TOTEM_OF_UNDYING";
			case "FIREWORKS_SPARK" -> name = "FIREWORK";
			}
		}

		return safeParticle(name);
	}

	/**
	 * Attempts to resolve a particle name safely. Returns {@code WHITE_ASH} as a
	 * fallback if the particle does not exist.
	 */
	@NotNull
	private static Particle safeParticle(@NotNull String name) {
		if (EnumUtils.isValidEnum(Particle.class, name)) {
			return Particle.valueOf(name);
		}
		return Particle.WHITE_ASH;
	}
}