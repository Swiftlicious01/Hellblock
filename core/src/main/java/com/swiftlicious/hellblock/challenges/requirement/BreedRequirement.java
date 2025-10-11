package com.swiftlicious.hellblock.challenges.requirement;

import java.util.Locale;

import org.bukkit.entity.Breedable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.swiftlicious.hellblock.challenges.ChallengeRequirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class BreedRequirement implements ChallengeRequirement {
	private final EntityType entityType;

	public BreedRequirement(Section data) {
		String e = data.getString("entity");
		if (e == null) {
			throw new IllegalArgumentException("BREED requires 'entity' in data");
		}

		EntityType type;
		try {
			type = EntityType.valueOf(e.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid entity: " + e);
		}

		Class<?> clazz = type.getEntityClass();
		if (clazz == null || !Breedable.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Entity " + e + " is not breedable");
		}

		this.entityType = type;
	}

	@Override
	public boolean matches(Object context) {
		return context instanceof Entity entity && entity.getType() == entityType;
	}
}