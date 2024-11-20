package com.swiftlicious.hellblock.effects;

import java.util.Objects;

/**
 * Represents properties for effects.
 *
 * @param <T> the type of the property value.
 */
public class EffectProperties<T> {

	public static final EffectProperties<Boolean> LAVA_FISHING = of("lava", Boolean.class);
	public static final EffectProperties<Boolean> WATER_FISHING = of("water", Boolean.class);

	private final String key;
	private final Class<T> type;

	private EffectProperties(String key, Class<T> type) {
		this.key = key;
		this.type = type;
	}

	/**
	 * Gets the key of the property.
	 *
	 * @return the key.
	 */
	public String key() {
		return key;
	}

	/**
	 * Gets the type of the property value.
	 *
	 * @return the type.
	 */
	public Class<T> type() {
		return type;
	}

	/**
	 * Creates a new effect property.
	 *
	 * @param key  the key of the property.
	 * @param type the type of the property value.
	 * @param <T>  the type of the property value.
	 * @return a new EffectProperties instance.
	 */
	public static <T> EffectProperties<T> of(String key, Class<T> type) {
		return new EffectProperties<T>(key, type);
	}

	@Override
	public final boolean equals(final Object other) {
		if (this == other) {
			return true;
		} else if (other != null && this.getClass() == other.getClass()) {
			EffectProperties<?> that = (EffectProperties<?>) other;
			return Objects.equals(this.key, that.key);
		} else {
			return false;
		}
	}

	@Override
	public final int hashCode() {
		return Objects.hashCode(this.key);
	}

	@Override
	public String toString() {
		return "EffectProperties{" + "key='" + key + '\'' + ", type=" + type.getSimpleName() + '}';
	}
}