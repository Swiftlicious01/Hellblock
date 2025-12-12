package com.swiftlicious.hellblock.challenges.requirement;

import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeRequirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where the player must slay a specific
 * type of entity, either vanilla or custom (via PDC tags).
 * <p>
 * This requirement supports both built-in {@link EntityType} values and
 * custom entities identified by persistent data tags.
 * </p>
 *
 * <p><b>Example configuration:</b></p>
 * <pre>
 *   SLAY_WITHER_SKELETONS:
 *     needed-amount: 25
 *     action: SLAY
 *     data:
 *       entity: WITHER_SKELETON
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: NETHERITE_SCRAP
 * </pre>
 *
 * <p>Players must slay 25 Wither Skeletons to complete this challenge.</p>
 */
public class SlayRequirement implements ChallengeRequirement {

	private final EntityType vanillaType;
	private final String customKey;
	private final NamespacedKey pdcKey;

	public SlayRequirement(@NotNull Section data) {
		String entity = data.getString("entity");
		if (entity == null)
			throw new IllegalArgumentException("SLAY requires 'entity' in data");

		EntityType parsed = null;
		try {
			parsed = EntityType.valueOf(entity.toUpperCase(Locale.ROOT));
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}

		this.vanillaType = parsed;
		this.customKey = parsed == null ? entity.toUpperCase(Locale.ROOT) : null;
		this.pdcKey = new NamespacedKey(HellblockPlugin.getInstance(), entity);
	}

	/**
	 * Checks whether the provided context represents a slain entity that matches
	 * this requirement — either vanilla or custom.
	 *
	 * @param context The event context (expected to be an {@link Entity}).
	 * @return {@code true} if the slain entity fulfills the requirement.
	 */
	@Override
	public boolean matches(@NotNull Object context) {
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