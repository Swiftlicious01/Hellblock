package com.swiftlicious.hellblock.listeners.fishing;

import java.util.Optional;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.mechanics.hook.HookConfig;

/**
 * Interface for managing hooks.
 */
public interface HookManagerInterface extends Reloadable {

	/**
	 * Registers a new hook configuration.
	 *
	 * @param hook the {@link HookConfig} to be registered
	 * @return true if the hook was successfully registered, false otherwise
	 */
	boolean registerHook(HookConfig hook);

	/**
	 * Retrieves a hook configuration by its ID.
	 *
	 * @param id the ID of the hook
	 * @return an {@link Optional} containing the {@link HookConfig} if found, or an
	 *         empty {@link Optional} if not
	 */
	@NotNull
	Optional<HookConfig> getHook(String id);

	/**
	 * Retrieves the hook ID associated with a given fishing rod.
	 *
	 * @param rod the {@link ItemStack} representing the fishing rod
	 * @return an {@link Optional} containing the hook ID if found, or an empty
	 *         {@link Optional} if not
	 */
	Optional<String> getHookID(ItemStack rod);
}