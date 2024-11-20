package com.swiftlicious.hellblock.utils.extras;

public interface TriConsumer<K, V, S> {
	void accept(K k, V v, S s);
}