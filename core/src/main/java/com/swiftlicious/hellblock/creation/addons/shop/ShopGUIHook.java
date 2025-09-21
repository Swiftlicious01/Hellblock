package com.swiftlicious.hellblock.creation.addons.shop;

import org.bukkit.Bukkit;

import com.swiftlicious.hellblock.HellblockPlugin;

public class ShopGUIHook {

	public static void register() {
		Bukkit.getPluginManager().registerEvents(new ShopGUIItemProvider(HellblockPlugin.getInstance()),
				HellblockPlugin.getInstance());
	}
}