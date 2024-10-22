package com.swiftlicious.hellblock.creation.addons;

import org.bukkit.entity.Player;

public interface LevelInterface {

	/**
	 * Add exp to a certain skill or job
	 *
	 * @param player player
	 * @param target the skill or job, for instance "Fishing" "fisherman"
	 * @param amount the exp amount
	 */
	void addXp(Player player, String target, double amount);

	/**
	 * Get a player's skill or job's level
	 *
	 * @param player player
	 * @param target the skill or job, for instance "Fishing" "fisherman"
	 * @return level
	 */
	int getLevel(Player player, String target);
}