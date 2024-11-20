package com.swiftlicious.hellblock.effects;

import java.util.Optional;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.mechanics.MechanicType;

/**
 * Interface for managing effect modifiers.
 */
public interface EffectManagerInterface extends Reloadable {

	/**
	 * Registers an effect modifier for a specific mechanic type.
	 *
	 * @param effect the effect modifier to register.
	 * @param type   the type of mechanic.
	 * @return true if registration is successful, false otherwise.
	 */
	boolean registerEffectModifier(EffectModifier effect, MechanicType type);

	/**
	 * Retrieves an effect modifier by its ID and mechanic type.
	 *
	 * @param id   the ID of the effect modifier.
	 * @param type the type of mechanic.
	 * @return an Optional containing the effect modifier if found, otherwise empty.
	 */
	Optional<EffectModifier> getEffectModifier(String id, MechanicType type);
}
