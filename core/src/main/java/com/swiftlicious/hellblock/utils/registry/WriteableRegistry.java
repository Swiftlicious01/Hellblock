package com.swiftlicious.hellblock.utils.registry;

/**
 * The WriteableRegistry interface extends the Registry interface, adding the
 * capability to register new key-value pairs. This interface is used to define
 * registries that allow modifications.
 *
 * @param <K> the type of the keys used for lookup
 * @param <T> the type of the values stored in the registry
 */
public interface WriteableRegistry<K, T> extends Registry<K, T> {

	/**
	 * Registers a new key-value pair in the registry. This method allows adding new
	 * entries to the registry dynamically.
	 *
	 * @param key   the key associated with the value
	 * @param value the value to be registered
	 */
	void register(K key, T value);
}