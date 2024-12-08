package com.swiftlicious.hellblock.utils.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.utils.extras.Key;

/**
 * The Registry interface defines a structure for a key-value mapping system
 * that supports efficient retrieval and management of entries.
 *
 * @param <K> the type of keys maintained by this registry
 * @param <T> the type of values that the keys map to
 */
public interface Registry<K, T> extends IdMap<T> {

	/**
	 * Retrieves the unique key associated with this registry.
	 *
	 * @return the unique {@link Key} of this registry
	 */
	Key key();

	/**
	 * Retrieves the unique identifier associated with a value.
	 *
	 * @param value the value whose identifier is to be retrieved
	 * @return the unique identifier of the value, or -1 if not present
	 */
	@Override
	int getId(@NotNull T value);

	/**
	 * Retrieves a value mapped to the specified key.
	 *
	 * @param key the key associated with the value to be retrieved
	 * @return the value mapped to the specified key, or null if no mapping exists
	 */
	@Nullable
	T get(@NotNull K key);

	/**
	 * Checks if the registry contains a mapping for the specified key.
	 *
	 * @param key the key to check for existence
	 * @return true if the registry contains a mapping for the key, false otherwise
	 */
	boolean containsKey(@NotNull K key);

	/**
	 * Checks if the registry contains the specified value.
	 *
	 * @param value the value to check for existence
	 * @return true if the registry contains the specified value, false otherwise
	 */
	boolean containsValue(@NotNull T value);
}