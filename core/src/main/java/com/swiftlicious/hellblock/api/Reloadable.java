package com.swiftlicious.hellblock.api;

/**
 * The {@code Reloadable} interface defines a lifecycle contract for components
 * that support runtime reloading.
 * 
 * It provides default implementations for common operations such as:
 * <ul>
 * <li>{@code load()} – initialize or load resources</li>
 * <li>{@code unload()} – clean up or release resources</li>
 * <li>{@code reload()} – unload and then load again</li>
 * <li>{@code disable()} – alias for unload-only operation</li>
 * </ul>
 *
 * Implementing classes can override any of these methods to provide custom
 * logic.
 */
public interface Reloadable {

	/**
	 * Reloads the component by first unloading and then loading it again. This
	 * method provides a default full reset mechanism.
	 */
	default void reload() {
		unload();
		load();
	}

	/**
	 * Unloads the component, releasing any allocated resources or state. This
	 * method has an empty default implementation and should be overridden as
	 * needed.
	 */
	default void unload() {
	}

	/**
	 * Loads or initializes the component. This method has an empty default
	 * implementation and should be overridden as needed.
	 */
	default void load() {
	}

	/**
	 * Disables the component by unloading it. This is effectively a one-way
	 * operation as no loading follows.
	 */
	default void disable() {
		unload();
	}
}