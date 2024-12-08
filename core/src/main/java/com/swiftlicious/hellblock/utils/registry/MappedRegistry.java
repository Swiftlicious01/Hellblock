package com.swiftlicious.hellblock.utils.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.utils.extras.Key;

/**
 * A registry implementation that supports mapping keys to values and provides
 * methods for registration, lookup, and iteration.
 *
 * @param <K> the type of the keys used for lookup
 * @param <T> the type of the values stored in the registry
 */
public class MappedRegistry<K, T> implements WriteableRegistry<K, T> {

	protected final Map<K, T> byKey = new HashMap<>(1024);
	protected final Map<T, K> byValue = new IdentityHashMap<>(1024);
	protected final List<T> byID = new ArrayList<>(1024);
	private final Key key;

	/**
	 * Constructs a new MappedRegistry with a given unique key.
	 *
	 * @param key the unique key for this registry
	 */
	public MappedRegistry(Key key) {
		this.key = key;
	}

	/**
	 * Registers a new key-value pair in the registry.
	 *
	 * @param key   the key associated with the value
	 * @param value the value to be registered
	 */
	@Override
	public void register(K key, T value) {
		if (byKey.containsKey(key))
			return;
		byKey.put(key, value);
		byValue.put(value, key);
		byID.add(value);
	}

	/**
	 * Gets the unique key identifier for this registry.
	 *
	 * @return the key of the registry
	 */
	@Override
	public Key key() {
		return key;
	}

	/**
	 * Retrieves the index (ID) of a given value in the registry.
	 *
	 * @param value the value to look up
	 * @return the index of the value, or -1 if not found
	 */
	@Override
	public int getId(@Nullable T value) {
		return byID.indexOf(value);
	}

	/**
	 * Retrieves a value from the registry by its index (ID).
	 *
	 * @param index the index of the value
	 * @return the value at the specified index, or null if out of bounds
	 */
	@Nullable
	@Override
	public T byId(int index) {
		return byID.get(index);
	}

	/**
	 * Gets the number of entries in the registry.
	 *
	 * @return the size of the registry
	 */
	@Override
	public int size() {
		return byKey.size();
	}

	/**
	 * Retrieves a value from the registry by its key.
	 *
	 * @param key the key of the value
	 * @return the value associated with the key, or null if not found
	 */
	@Nullable
	@Override
	public T get(@Nullable K key) {
		return byKey.get(key);
	}

	/**
	 * Checks if the registry contains a given key.
	 *
	 * @param key the key to check
	 * @return true if the key exists, false otherwise
	 */
	@Override
	public boolean containsKey(@Nullable K key) {
		return byKey.containsKey(key);
	}

	/**
	 * Checks if the registry contains a given value.
	 *
	 * @param value the value to check
	 * @return true if the value exists, false otherwise
	 */
	@Override
	public boolean containsValue(@Nullable T value) {
		return byValue.containsKey(value);
	}

	/**
	 * Provides an iterator over the values in the registry.
	 *
	 * @return an iterator for the registry values
	 */
	@NotNull
	@Override
	public Iterator<T> iterator() {
		return this.byKey.values().iterator();
	}
}