package com.swiftlicious.hellblock.world;

import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.world.adapter.WorldAdapter;

/**
 * WorldManager is responsible for managing the lifecycle and state of worlds in
 * the Hellblock plugin. It provides methods to load, unload, and adapt worlds
 * and interact with different world adapters.
 */
public interface WorldManagerInterface extends Reloadable {

	/**
	 * Reload all worlds.
	 */
	void reloadWorlds();

	/**
	 * Loads a Hellblock world based on the specified Hellblock world.
	 *
	 * @param world The Hellblock world to load as a Hellblock world.
	 * @return The loaded HellblockWorld instance.
	 */
	HellblockWorld<?> loadWorld(HellblockWorld<?> world);

	/**
	 * Loads a Hellblock world based on the specified Bukkit world.
	 *
	 * @param world The Bukkit world to load as a Hellblock world.
	 * @return The loaded HellblockWorld instance.
	 */
	HellblockWorld<?> loadWorld(World world);

	/**
	 * Ensures that the Hellblock world for the specified island ID is loaded. If
	 * the world is not already loaded, it will be loaded asynchronously.
	 *
	 * @param islandId The ID of the island to ensure is loaded.
	 * @return A CompletableFuture that completes with the HellblockWorld instance
	 *         once it is loaded.
	 */
	CompletableFuture<HellblockWorld<?>> ensureHellblockWorldLoaded(int islandId);

	/**
	 * Unloads the Hellblock world associated with the specified Bukkit world.
	 *
	 * @param world     The Bukkit world to unload.
	 * @param disabling
	 * @return True if the world was successfully unloaded, false otherwise.
	 */
	boolean unloadWorld(World world, boolean disabling);

	/**
	 * Checks if mechanism is enabled for a certain world
	 *
	 * @param world world
	 * @return enabled or not
	 */
	boolean isMechanicEnabled(World world);

	/**
	 * Retrieves a Hellblock world based on the specified Bukkit world, if loaded.
	 *
	 * @param world The Bukkit world to retrieve the Hellblock world for.
	 * @return An Optional containing the HellblockWorld instance if loaded,
	 *         otherwise empty.
	 */
	Optional<HellblockWorld<?>> getWorld(World world);

	/**
	 * Retrieves a Hellblock world based on the world name, if loaded.
	 *
	 * @param world The name of the world to retrieve.
	 * @return An Optional containing the HellblockWorld instance if loaded,
	 *         otherwise empty.
	 */
	Optional<HellblockWorld<?>> getWorld(String world);

	/**
	 * Checks if a given Bukkit world is currently loaded as a Hellblock world.
	 *
	 * @param world The Bukkit world to check.
	 * @return True if the world is loaded, false otherwise.
	 */
	boolean isWorldLoaded(World world);

	/**
	 * Retrieves all available world adapters.
	 *
	 * @return A set of WorldAdapter instances.
	 */
	TreeSet<WorldAdapter<?>> adapters();

	/**
	 * Retrieves the currently used world adapter.
	 *
	 * @return The WorldAdapter instance.
	 */
	WorldAdapter<?> adapter();

	/**
	 * Adapts a Bukkit world into a Hellblock world.
	 *
	 * @param world The Bukkit world to adapt.
	 * @return The adapted HellblockWorld instance.
	 */
	HellblockWorld<?> adapt(World world);

	/**
	 * Adapts a world by its name into a Hellblock world.
	 *
	 * @param world The name of the world to adapt.
	 * @return The adapted HellblockWorld instance.
	 */
	HellblockWorld<?> adapt(String world);

	/**
	 * Retrieves the registered {@link WorldAdapter} instance that matches the
	 * specified type.
	 *
	 * <p>
	 * This is used when accessing a specific world adapter implementation, such as
	 * {@code SlimeWorldAdapter} or {@code BukkitWorldAdapter}, which may be needed
	 * for format-specific operations like world conversion or direct chunk access.
	 * </p>
	 *
	 * <p>
	 * If no adapter of the given type is registered, this method throws an
	 * {@link IllegalStateException}.
	 * </p>
	 *
	 * @param <T>  the type of the world adapter to retrieve
	 * @param type the class of the desired world adapter (e.g.,
	 *             {@code SlimeWorldAdapter.class})
	 * @return the matching {@link WorldAdapter} instance
	 * @throws IllegalStateException if no adapter of the specified type is
	 *                               registered
	 */
	<T extends WorldAdapter<?>> T getAdapterByType(Class<T> type);
}