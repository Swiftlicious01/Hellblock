package com.swiftlicious.hellblock.utils.registry;

/**
 * The ClearableRegistry interface extends WriteableRegistry and provides an
 * additional method to clear all entries from the registry.
 *
 * @param <K> the type of keys maintained by this registry
 * @param <T> the type of mapped values
 */
public interface ClearableRegistry<K, T> extends WriteableRegistry<K, T> {

	/**
	 * Clears all entries from the registry. This operation removes all key-value
	 * mappings from the registry, leaving it empty.
	 */
	void clear();
}