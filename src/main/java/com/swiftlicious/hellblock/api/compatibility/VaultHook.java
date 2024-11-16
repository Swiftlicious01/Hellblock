package com.swiftlicious.hellblock.api.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public class VaultHook {

	private static boolean isHooked = false;

	public static void init() {
		Singleton.initialize();
		VaultHook.isHooked = true;
	}

	public static boolean isHooked() {
		return isHooked;
	}

	public static void deposit(Player player, double amount) {
		Singleton.deposit(player, amount);
	}

	public static void withdraw(OfflinePlayer player, double amount) {
		Singleton.withdraw(player, amount);
	}

	public static double getBalance(OfflinePlayer player) {
		return Singleton.getBalance(player);
	}

	private static class Singleton {
		private static Economy economy;

		private static boolean initialize() {
			RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
			if (rsp == null) {
				return false;
			}
			economy = rsp.getProvider();
			return true;
		}

		private static Economy getEconomy() {
			return economy;
		}

		private static void deposit(OfflinePlayer player, double amount) {
			getEconomy().depositPlayer(player, amount);
		}

		private static void withdraw(OfflinePlayer player, double amount) {
			getEconomy().withdrawPlayer(player, amount);
		}

		private static double getBalance(OfflinePlayer player) {
			return getEconomy().getBalance(player);
		}
	}
}