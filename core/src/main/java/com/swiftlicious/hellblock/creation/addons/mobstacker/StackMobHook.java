package com.swiftlicious.hellblock.creation.addons.mobstacker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.swiftlicious.hellblock.HellblockPlugin;

public class StackMobHook implements Listener {

	public static void register() {
		Bukkit.getPluginManager().registerEvents(new StackMobHook(), HellblockPlugin.getInstance());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onStackMob(uk.antiperson.stackmob.events.StackMergeEvent event) {
		LivingEntity living = event.getStackEntity().getEntity();
		HellblockPlugin plugin = HellblockPlugin.getInstance();

		// Enhanced Wither
		if (living instanceof Wither wither && plugin.getWitherHandler().isEnhancedWither(wither)) {
			event.setCancelled(true);
			return;
		}

		// Hell Golems
		if (living instanceof Snowman snowman && plugin.getGolemHandler().isHellGolem(snowman)) {
			event.setCancelled(true);
			return;
		}

		// Queen Ghast / Skysiege Ghasts
		if (living instanceof Ghast ghast) {
			if (plugin.getSkysiegeHandler().isQueenGhast(ghast) || plugin.getSkysiegeHandler().isSkysiegeGhast(ghast)) {
				event.setCancelled(true);
				return;
			}
		}

		// Boss Piglin Brute / Invasion Brutes
		if (living instanceof PiglinBrute brute) {
			if (plugin.getInvasionHandler().isBossMob(brute) || plugin.getInvasionHandler().isInvasionMob(brute)
					|| plugin.getInvasionHandler().getInvaderFactory().getBerserkerCreator().isBerserker(brute)) {
				event.setCancelled(true);
				return;
			}
		}

		// Corrupted Hoglin / Invasion Hoglin
		if (living instanceof Hoglin hoglin) {
			if (plugin.getInvasionHandler().isInvasionMob(hoglin) || plugin.getInvasionHandler().getInvaderFactory()
					.getCorruptedHoglinCreator().isCorruptedHoglin(hoglin)) {
				event.setCancelled(true);
				return;
			}
		}

		// Shaman Piglin / Invasion Piglin
		if (living instanceof Piglin piglin) {
			if (plugin.getInvasionHandler().isInvasionMob(piglin)
					|| plugin.getInvasionHandler().getInvaderFactory().getShamanCreator().isShaman(piglin)) {
				event.setCancelled(true);
				return;
			}
		}

		// Invasion Mounts / Wither Minions
		if (living instanceof Mob mob) {
			if (plugin.getMinionHandler().isMinion(mob)
					|| plugin.getInvasionHandler().getInvaderFactory().isMountEntity(mob)) {
				event.setCancelled(true);
				return;
			}
		}
	}
}