package com.swiftlicious.hellblock.effects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.utils.extras.Key;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public interface EffectManagerInterface extends Reloadable {

	/**
	 * Registers an EffectCarrier with a unique Key.
	 *
	 * @param key    The unique Key associated with the EffectCarrier.
	 * @param effect The EffectCarrier to be registered.
	 * @return True if the registration was successful, false if the Key already
	 *         exists.
	 */
	boolean registerEffectCarrier(Key key, EffectCarrier effect);

	/**
	 * Unregisters an EffectCarrier associated with the specified Key.
	 *
	 * @param key The unique Key of the EffectCarrier to unregister.
	 * @return True if the EffectCarrier was successfully unregistered, false if the
	 *         Key does not exist.
	 */
	boolean unregisterEffectCarrier(Key key);

	/**
	 * Checks if an EffectCarrier with the specified namespace and id exists.
	 *
	 * @param namespace The namespace of the EffectCarrier.
	 * @param id        The unique identifier of the EffectCarrier.
	 * @return True if an EffectCarrier with the given namespace and id exists,
	 *         false otherwise.
	 */
	boolean hasEffectCarrier(String namespace, String id);

	/**
	 * Retrieves an EffectCarrier with the specified namespace and id.
	 *
	 * @param namespace The namespace of the EffectCarrier.
	 * @param id        The unique identifier of the EffectCarrier.
	 * @return The EffectCarrier with the given namespace and id, or null if it
	 *         doesn't exist.
	 */
	@Nullable
	EffectCarrier getEffectCarrier(String namespace, String id);

	/**
	 * Parses a Section to create an EffectCarrier based on the specified key and
	 * configuration.
	 * <p>
	 * xxx_item: <- section effects: ... events: ...
	 *
	 * @param key     The key that uniquely identifies the EffectCarrier.
	 * @param section The Section containing the EffectCarrier configuration.
	 * @return An EffectCarrier instance based on the key and configuration, or null
	 *         if the section is null.
	 */
	EffectCarrier getEffectCarrierFromSection(Key key, Section section);

	/**
	 * Retrieves the initial FishingEffect that represents no special effects.
	 *
	 * @return The initial FishingEffect.
	 */
	@NotNull
	FishingEffect getInitialEffect();

	/**
	 * Parses a Section to retrieve an array of EffectModifiers.
	 * <p>
	 * effects: <- section effect_1: type: xxx value: xxx
	 *
	 * @param section The Section to parse.
	 * @return An array of EffectModifiers based on the values found in the section.
	 */
	@NotNull
	EffectModifier[] getEffectModifiers(Section section);

	BaseEffect getBaseEffect(Section section);

	/**
	 * Parses a Section to create an EffectModifier based on the specified type and
	 * configuration.
	 * <p>
	 * effects: effect_1: <- section type: xxx value: xxx
	 *
	 * @param section The Section containing the effect modifier configuration.
	 * @return An EffectModifier instance based on the type and configuration.
	 */
	@Nullable
	EffectModifier getEffectModifier(Section section);
}
