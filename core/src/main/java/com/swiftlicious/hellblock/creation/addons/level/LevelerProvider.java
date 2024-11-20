package com.swiftlicious.hellblock.creation.addons.level;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.creation.addons.ExternalProvider;

/**
 * The LevelerProvider interface defines methods to interact with external
 * leveling systems, allowing the management of experience points (XP) and
 * levels for various skills or jobs. Implementations of this interface should
 * provide the logic for adding XP to players and retrieving their levels in
 * specific skills or jobs.
 */
public interface LevelerProvider extends ExternalProvider {

	/**
	 * Add exp to a certain skill or job
	 *
	 * @param player player
	 * @param target the skill or job, for instance "Fishing" "fisherman"
	 * @param amount the exp amount
	 */
	void addXp(@NotNull Player player, @NotNull String target, double amount);

	/**
	 * Get a player's skill or job's level
	 *
	 * @param player player
	 * @param target the skill or job, for instance "Fishing" "fisherman"
	 * @return level
	 */
	int getLevel(@NotNull Player player, @NotNull String target);
}