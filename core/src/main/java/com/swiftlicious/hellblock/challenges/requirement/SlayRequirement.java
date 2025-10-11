package com.swiftlicious.hellblock.challenges.requirement;

import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeRequirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class SlayRequirement implements ChallengeRequirement {
	private final EntityType vanillaType;
	private final String customKey;
	private final NamespacedKey pdcKey;

	public SlayRequirement(Section data) {
		String entity = data.getString("entity");
		if (entity == null)
			throw new IllegalArgumentException("SLAY requires 'entity' in data");

		EntityType parsed = null;
		try {
			parsed = EntityType.valueOf(entity.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
		}

		this.vanillaType = parsed;
		this.customKey = parsed == null ? entity.toUpperCase(Locale.ROOT) : null;
		this.pdcKey = new NamespacedKey(HellblockPlugin.getInstance(), entity);
	}

	@Override
	public boolean matches(Object context) {
		if (!(context instanceof Entity entity)) {
			return false;
		}

		// Case 1: Vanilla requirement (e.g., WITHER_SKELETON)
		if (vanillaType != null && customKey == null) {
			if (entity.getType() != vanillaType) {
				return false;
			}

			// Exclude custom-tagged entities
			String stored = entity.getPersistentDataContainer().get(pdcKey, PersistentDataType.STRING);
			return stored == null;
		}

		// Case 2: Custom entity requirement (e.g., WRAITH)
		if (customKey != null) {
			String stored = entity.getPersistentDataContainer().get(pdcKey, PersistentDataType.STRING);
			return stored != null && stored.equalsIgnoreCase(customKey);
		}

		return false;
	}
}