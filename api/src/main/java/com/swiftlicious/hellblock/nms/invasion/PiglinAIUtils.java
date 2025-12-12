package com.swiftlicious.hellblock.nms.invasion;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface PiglinAIUtils {

	/**
	 * Removes passive goals from the given entity.
	 */
	abstract void stripAllPassiveGoals(@NotNull LivingEntity bukkitEntity);

	/**
	 * Adds a "look at nearest player" goal to the entity.
	 */
	abstract void addFollowPlayerGoal(@NotNull LivingEntity bukkitEntity);

	/**
	 * Restores default goal selector for Striders.
	 */
	abstract void restoreStriderGoals(@NotNull org.bukkit.entity.Strider bukkitStrider);

	/**
	 * Restores default goal selector for Magma Cubes.
	 */
	abstract void restoreMagmaCubeGoals(@NotNull org.bukkit.entity.MagmaCube magma);
}