package com.swiftlicious.hellblock.mechanics.fishing;

import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class AntiAutoFishing {

	public static void prevent(Player player, FishHook hook) {
		Location loc = hook.getLocation();
		HellblockPlugin.getInstance()
				.debug(String.format("Trying to prevent player %s from auto fishing.", player.getName()));
		HellblockPlugin.getInstance().getSenderFactory().getAudience(player).playSound(
				Sound.sound(Key.key("minecraft", "entity.fishing_bobber.splash"), Sound.Source.NEUTRAL, 0f, 1f),
				loc.getX(), loc.getY(), loc.getZ());
		double motion = -0.4 * RandomUtils.generateRandomDouble(0.6, 1.0);
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().sendClientSideEntityMotion(player,
				new Vector(0, motion, 0), hook.getEntityId());
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().sendClientSideEntityMotion(player,
				new Vector(0, 0, 0), hook.getEntityId());
	}
}