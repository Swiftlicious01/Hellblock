package com.swiftlicious.hellblock.api.compatibility;

import org.bukkit.plugin.RegisteredServiceProvider;

import com.swiftlicious.hellblock.HellblockPlugin;

import net.milkbowl.vault.economy.Economy;

public class VaultHook {

	private static Economy economy;

	public static boolean initialize() {
		RegisteredServiceProvider<Economy> rsp = HellblockPlugin.getInstance().getServer().getServicesManager()
				.getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return true;
	}

	public static Economy getEconomy() {
		return economy;
	}
}