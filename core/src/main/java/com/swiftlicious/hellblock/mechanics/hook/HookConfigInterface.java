package com.swiftlicious.hellblock.mechanics.hook;

import java.util.List;

import com.swiftlicious.hellblock.mechanics.hook.HookConfig.Builder;

/**
 * Represents the configuration for a fishing hook.
 */
public interface HookConfigInterface {

	/**
	 * Gets the identifier of the hook.
	 *
	 * @return the identifier of the hook.
	 */
	String id();

	/**
	 * Gets the additional lore of the hook.
	 *
	 * @return a list of additional lore strings for the hook.
	 */
	List<String> lore();

	/**
	 * Gets the max usages of the hook
	 *
	 * @return the max usages
	 */
	int maxUsages();

	/**
	 * Creates a new builder for constructing {@link HookConfig} instances.
	 *
	 * @return a new {@link Builder} instance.
	 */
	static Builder builder() {
		return new HookConfig.Builder();
	}

	/**
	 * Builder interface for constructing {@link HookConfig} instances.
	 */
	interface BuilderInterface {

		/**
		 * Sets the identifier for the hook configuration.
		 *
		 * @param id the identifier of the hook.
		 * @return the current {@link Builder} instance.
		 */
		Builder id(String id);

		/**
		 * Sets the max usages of the hook
		 */
		Builder maxUsages(int maxUsages);

		/**
		 * Sets the lore for the hook configuration.
		 *
		 * @param lore a list of lore strings for the hook.
		 * @return the current {@link Builder} instance.
		 */
		Builder lore(List<String> lore);

		/**
		 * Builds and returns the {@link HookConfig} instance.
		 *
		 * @return the constructed {@link HookConfig} instance.
		 */
		HookConfig build();
	}
}