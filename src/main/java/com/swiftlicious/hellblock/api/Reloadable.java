package com.swiftlicious.hellblock.api;

public interface Reloadable {

	default void reload() {
		unload();
		load();
	}

	default void unload() {
	}

	default void load() {
	}

	default void disable() {
		unload();
	}
}