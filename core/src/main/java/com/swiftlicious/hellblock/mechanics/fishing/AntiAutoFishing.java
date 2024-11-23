package com.swiftlicious.hellblock.mechanics.fishing;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class AntiAutoFishing {

	public static void prevent(Player player, FishHook hook) {
		HellblockPlugin.getInstance()
				.debug(String.format("Trying to prevent player %s from auto fishing.", player.getName()));
		HellblockPlugin.getInstance().getAdventureManager().playSound(player,
				net.kyori.adventure.sound.Sound.Source.NEUTRAL,
				net.kyori.adventure.key.Key.key("minecraft:entity.fishing_bobber.splash"), 0F, 1F);
		double motion = -0.4 * RandomUtils.generateRandomDouble(0.6, 1.0);
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().sendClientSideEntityMotion(player, new Vector(0, motion, 0), hook.getEntityId());
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().sendClientSideEntityMotion(player, new Vector(0, 0, 0), hook.getEntityId());
	}
}