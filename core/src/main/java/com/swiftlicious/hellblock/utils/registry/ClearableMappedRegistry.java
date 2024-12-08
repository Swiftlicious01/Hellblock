package com.swiftlicious.hellblock.utils.registry;

import com.swiftlicious.hellblock.utils.extras.Key;

/**
 * ClearableMappedRegistry is a concrete implementation of the ClearableRegistry
 * interface. It extends MappedRegistry and provides the capability to clear all
 * entries.
 *
 * @param <K> the type of keys maintained by this registry
 * @param <T> the type of mapped values
 */
public class ClearableMappedRegistry<K, T> extends MappedRegistry<K, T> implements ClearableRegistry<K, T> {

	/**
	 * Constructs a new ClearableMappedRegistry with a unique key.
	 *
	 * @param key the unique key for this registry
	 */
	public ClearableMappedRegistry(Key key) {
		super(key);
	}

	/**
	 * Clears all entries from the registry. This operation removes all key-value
	 * mappings from the registry, leaving it empty.
	 */
	@Override
	public void clear() {
		super.byID.clear(); // Clears the list of values indexed by ID
		super.byKey.clear(); // Clears the map of keys to values
		super.byValue.clear(); // Clears the map of values to keys
	}
}