package com.swiftlicious.hellblock.listeners.fishing;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.mechanics.fishing.CustomFishingHook;

/**
 * Interface for managing fishing.
 */
public interface FishingManagerInterface extends Reloadable {

	/**
	 * Retrieves the custom fishing hook associated with the specified player.
	 *
	 * @param player the player.
	 * @return an Optional containing the custom fishing hook if found, or an empty
	 *         Optional if not found.
	 */
	Optional<CustomFishingHook> getFishHook(Player player);

	/**
	 * Retrieves the custom fishing hook associated with the specified player UUID.
	 *
	 * @param player the UUID of the player.
	 * @return an Optional containing the custom fishing hook if found, or an empty
	 *         Optional if not found.
	 */
	Optional<CustomFishingHook> getFishHook(UUID player);

	/**
	 * Retrieves the owner of the specified fish hook.
	 *
	 * @param hook the fish hook.
	 * @return an Optional containing the owner if found, or an empty Optional if
	 *         not found.
	 */
	Optional<Player> getOwner(FishHook hook);

	/**
	 * Destroys the custom fishing hook associated with the specified player UUID.
	 *
	 * @param player the UUID of the player.
	 */
	void destroyHook(UUID player);
}