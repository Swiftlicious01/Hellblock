package com.swiftlicious.hellblock.challenges.requirement;

import java.util.Locale;

import org.bukkit.entity.Breedable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.challenges.ChallengeRequirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where a player must breed a specific type
 * of animal.
 * <p>
 * This requirement is triggered when two compatible {@link Breedable} entities
 * successfully create offspring of the configured {@link EntityType}.
 * </p>
 *
 * <p>
 * <b>Example configuration:</b>
 * </p>
 * 
 * <pre>
 *   BREED_COWS:
 *     needed-amount: 10
 *     action: BREED
 *     data:
 *       entity: COW
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: WHEAT
 * </pre>
 *
 * <p>
 * This challenge requires the player to breed cows ten times.
 * </p>
 */
public class BreedRequirement implements ChallengeRequirement {

	private final EntityType entityType;

	public BreedRequirement(@NotNull Section data) {
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

	/**
	 * Checks whether the given context represents a {@link Breedable} entity that
	 * matches the required {@link EntityType}.
	 *
	 * @param context The event context (expected to be an {@link Entity}).
	 * @return {@code true} if the entity matches and is breedable.
	 */
	@Override
	public boolean matches(@NotNull Object context) {
		return context instanceof Entity entity && entity.getType() == entityType;
	}
}