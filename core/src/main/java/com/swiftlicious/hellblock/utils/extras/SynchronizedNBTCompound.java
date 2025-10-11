package com.swiftlicious.hellblock.utils.extras;

import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

/**
 * A thread-safe wrapper around a CompoundMap that provides synchronized access
 * to the underlying map for reading and writing operations.
 */
public class SynchronizedNBTCompound {

	private CompoundBinaryTag compound;
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Lock readLock = rwLock.readLock();
	private final Lock writeLock = rwLock.writeLock();

	public SynchronizedNBTCompound(CompoundBinaryTag initial) {
		this.compound = initial;
	}

	/**
	 * Returns the original CompoundBinaryTag. Modifications to the returned tag
	 * will not be thread-safe.
	 *
	 * @return the original CompoundBinaryTag
	 */
	public CompoundBinaryTag original() {
		readLock.lock();
		try {
			return compound;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Returns a copy of the current CompoundBinaryTag. Modifications to the
	 * returned tag will not affect the original.
	 *
	 * @return a copy of the current CompoundBinaryTag
	 */
	public BinaryTag get(String key) {
		readLock.lock();
		try {
			return compound.get(key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Adds or updates an entry in the compound tag with the specified key and
	 * value.
	 *
	 * @param key the key of the entry to add or update
	 * @param tag the value of the entry to add or update
	 */
	public void put(String key, BinaryTag tag) {
		writeLock.lock();
		try {
			compound = CompoundBinaryTag.builder().put(key, tag).build();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Removes the entry with the specified key from the compound tag.
	 *
	 * @param key the key of the entry to remove
	 */
	public void remove(String key) {
		writeLock.lock();
		try {
			compound = CompoundBinaryTag.builder().remove(key).build();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public int hashCode() {
		readLock.lock();
		try {
			return compound.hashCode();
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SynchronizedNBTCompound that)) {
			return false;
		}

		readLock.lock();
		try {
			return this.compound.equals(that.original());
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public String toString() {
		return asString();
	}

	/**
	 * Returns a string representation of the compound tag in a human-readable
	 * format.
	 *
	 * @return a string representation of the compound tag
	 */
	public String asString() {
		readLock.lock();
		try {
			return compoundToString("", compound);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Recursively converts a CompoundBinaryTag to a string representation.
	 *
	 * @param key the key associated with the compound tag
	 * @param tag the CompoundBinaryTag to convert
	 * @return a string representation of the compound tag
	 */
	private String compoundToString(String key, CompoundBinaryTag tag) {
		final StringJoiner joiner = new StringJoiner(", ");
		for (Entry<String, ? extends BinaryTag> entry : tag) {
			final BinaryTag value = entry.getValue();
			final String tagValue;
			if (value instanceof CompoundBinaryTag compound) {
				tagValue = compoundToString(entry.getKey(), compound);
			} else if (value instanceof ListBinaryTag list) {
				final StringJoiner listJoiner = new StringJoiner(", ");
				for (BinaryTag element : list) {
					listJoiner.add(element.toString());
				}
				tagValue = "[" + listJoiner + "]";
			} else {
				tagValue = value.toString();
			}
			joiner.add(entry.getKey() + "=" + tagValue);
		}
		return key.isEmpty() ? "{" + joiner + "}" : key + "={" + joiner + "}";
	}
}