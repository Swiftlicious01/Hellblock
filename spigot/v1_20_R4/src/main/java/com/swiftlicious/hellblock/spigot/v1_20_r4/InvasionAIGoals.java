package com.swiftlicious.hellblock.spigot.v1_20_r4;

import java.util.Set;

import org.bukkit.craftbukkit.v1_20_R4.entity.CraftMagmaCube;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftMob;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftStrider;
import org.bukkit.entity.LivingEntity;

import com.swiftlicious.hellblock.nms.invasion.PiglinAIUtils;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class InvasionAIGoals implements PiglinAIUtils {

	private static final Set<String> PASSIVE_GOAL_KEYWORDS = Set.of("admire", "barter", "avoid", "flee", "interact",
			"celebrate", "look", "wander", "random", "panic", "dance", "observe", "play", "tempt", "float", "stroll",
			"strider", "fungus", "restrict", "idle", "roam", "swim", "move", "ride", "parent", "restriction");

	@Override
	public void stripAllPassiveGoals(LivingEntity bukkitEntity) {
		if (!(bukkitEntity instanceof org.bukkit.entity.Mob mob))
			return;

		Mob nmsEntity = (Mob) ((CraftMob) mob).getHandle();

		removePassiveGoals(nmsEntity.goalSelector);
		removePassiveGoals(nmsEntity.targetSelector);
	}

	private void removePassiveGoals(GoalSelector selector) {
		for (WrappedGoal wrapped : selector.getAvailableGoals()) {
			Goal goal = wrapped.getGoal();
			String name = goal.getClass().getSimpleName().toLowerCase();

			for (String keyword : PASSIVE_GOAL_KEYWORDS) {
				if (name.contains(keyword)) {
					selector.removeGoal(goal);
					break;
				}
			}
		}
	}

	@Override
	public void addFollowPlayerGoal(LivingEntity bukkitEntity) {
		if (!(bukkitEntity instanceof org.bukkit.entity.Mob mob))
			return;

		Mob nmsEntity = (Mob) ((CraftMob) mob).getHandle();

		Goal lookAtPlayerGoal = new LookAtPlayerGoal(nmsEntity, Player.class, 12.0F);
		nmsEntity.goalSelector.addGoal(2, lookAtPlayerGoal);
	}

	@Override
	public void restoreStriderGoals(org.bukkit.entity.Strider bukkitStrider) {
		Strider strider = ((CraftStrider) bukkitStrider).getHandle();

		strider.goalSelector.removeAllGoals(goal -> true);
		strider.targetSelector.removeAllGoals(goal -> true);

		// Re-add only relevant default goals for Strider
		strider.goalSelector.addGoal(1, new FloatGoal(strider));
		strider.goalSelector.addGoal(2, new PanicGoal(strider, 1.25D));
		strider.goalSelector.addGoal(3, new TemptGoal(strider, 1.0D, Ingredient.of(Items.WARPED_FUNGUS), false));
		strider.goalSelector.addGoal(4, new FollowParentGoal(strider, 1.1D));
		strider.goalSelector.addGoal(5, new RandomStrollGoal(strider, 1.0D));
		strider.goalSelector.addGoal(6, new LookAtPlayerGoal(strider, Player.class, 8.0F));
		strider.goalSelector.addGoal(7, new RandomLookAroundGoal(strider));
	}

	public void restoreMagmaCubeGoals(org.bukkit.entity.MagmaCube bukkitMagma) {
		// Get the NMS entity
		MagmaCube cube = ((CraftMagmaCube) bukkitMagma).getHandle();

		// Since MagmaCube extends Slime -> Mob -> PathfinderMob, we can now use it
		cube.goalSelector.removeAllGoals(goal -> true);
		cube.targetSelector.removeAllGoals(goal -> true);

		// Add goals using the NMS entity, which is a PathfinderMob
		cube.goalSelector.addGoal(1, new FloatGoal(cube));
		cube.goalSelector.addGoal(2, new MeleeAttackGoal(asPathfinderMob(cube), 1.0D, false));
		cube.goalSelector.addGoal(3, new RandomStrollGoal(asPathfinderMob(cube), 1.0D));
		cube.goalSelector.addGoal(4, new LookAtPlayerGoal(cube, Player.class, 8.0F));
		cube.goalSelector.addGoal(5, new RandomLookAroundGoal(cube));

		cube.targetSelector.addGoal(1, new HurtByTargetGoal(asPathfinderMob(cube)));
		cube.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(cube, Player.class, true));
	}

	private PathfinderMob asPathfinderMob(Mob mob) {
		if (mob instanceof PathfinderMob pathfinder)
			return pathfinder;
		throw new IllegalArgumentException("Not a PathfinderMob: " + mob.getClass().getName());
	}
}