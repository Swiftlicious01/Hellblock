package com.swiftlicious.hellblock.listeners.invasion;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Strider;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.VersionHelper;

public final class InvasionGoalHelper {

	public static void updateRideableMobGoals(@NotNull LivingEntity entity) {
		if (!(entity instanceof Mob mob))
			return;

		boolean hasRider = isMountedByInvasionMob(mob);

		if (hasRider) {
			VersionHelper.getInvasionAIManager().stripAllPassiveGoals(mob);

			if (mob instanceof Strider || mob instanceof MagmaCube) {
				VersionHelper.getInvasionAIManager().addFollowPlayerGoal(mob);
			}
		} else {
			restoreGoalsIfNotRidden(mob);
		}
	}

	public static void restoreGoalsIfNotRidden(@NotNull Mob mob) {
		if (mob instanceof Strider strider) {
			VersionHelper.getInvasionAIManager().restoreStriderGoals(strider);
		} else if (mob instanceof MagmaCube cube) {
			VersionHelper.getInvasionAIManager().restoreMagmaCubeGoals(cube);
		}
	}

	public static boolean isMountedByInvasionMob(@NotNull LivingEntity entity) {
		if (!(entity instanceof Mob mob))
			return false;

		return mob.getPassengers().stream().anyMatch(passenger -> passenger instanceof Piglin pig
				&& HellblockPlugin.getInstance().getInvasionHandler().isInvasionMob(pig));
	}
}