package com.swiftlicious.hellblock.listeners.fishing;

import java.util.UUID;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Reloadable;

public interface FishingManagerInterface extends Reloadable {

	/**
	 * Removes a fishing hook entity associated with a given player's UUID.
	 *
	 * @param uuid The UUID of the player
	 * @return {@code true} if the fishing hook was successfully removed,
	 *         {@code false} otherwise.
	 */
	boolean removeHook(UUID uuid);

	/**
	 * Retrieves a FishHook object associated with the provided player's UUID
	 *
	 * @param uuid The UUID of the player
	 * @return fishhook entity, null if not exists
	 */
	@Nullable
	FishHook getHook(UUID uuid);

	/**
	 * Sets the temporary fishing state for a player.
	 *
	 * @param player           The player for whom to set the temporary fishing
	 *                         state.
	 * @param tempFishingState The temporary fishing state to set for the player.
	 */
	void setTempFishingState(Player player, TempFishingState tempFishingState);

	/**
	 * Gets the {@link TempFishingState} object associated with the given UUID.
	 *
	 * @param uuid The UUID of the player.
	 * @return The {@link TempFishingState} object if found, or {@code null} if not
	 *         found.
	 */
	@Nullable
	TempFishingState getTempFishingState(UUID uuid);

	/**
	 * Removes the temporary fishing state associated with a player.
	 *
	 * @param player The player whose temporary fishing state should be removed.
	 */
	@Nullable
	TempFishingState removeTempFishingState(Player player);

	/**
	 * Checks if a player with the given UUID has cast their fishing hook.
	 *
	 * @param uuid The UUID of the player to check.
	 * @return {@code true} if the player has cast their fishing hook, {@code false}
	 *         otherwise.
	 */
	boolean hasPlayerCastHook(UUID uuid);
}