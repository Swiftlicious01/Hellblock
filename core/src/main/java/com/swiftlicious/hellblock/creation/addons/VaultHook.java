package com.swiftlicious.hellblock.creation.addons;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class VaultHook {

	private static boolean isHooked = false;

	public static void init() {
		if (Singleton.initialize()) {
			VaultHook.isHooked = true;
		}
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
	
	public static boolean hasBalance(OfflinePlayer player, double amount) {
		return Singleton.hasBalance(player, amount);
	}

	public static double getBalance(OfflinePlayer player) {
		return Singleton.getBalance(player);
	}

	public static boolean hasPermission(UUID uuid, String permission) {
		return Singleton.hasPermission(uuid, permission);
	}

	private static class Singleton {
		private static Permission permission;
		private static Economy economy;

		private static boolean initialize() {
			final RegisteredServiceProvider<Economy> rspEconomy = Bukkit.getServicesManager()
					.getRegistration(Economy.class);
			final RegisteredServiceProvider<Permission> rspPermission = Bukkit.getServicesManager()
					.getRegistration(Permission.class);
			if (rspEconomy == null || rspPermission == null) {
				return false;
			}
			economy = rspEconomy.getProvider();
			permission = rspPermission.getProvider();
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
		
		private static boolean hasBalance(OfflinePlayer player, double amount) {
			return getEconomy().has(player, amount);
		}

		private static double getBalance(OfflinePlayer player) {
			return getEconomy().getBalance(player);
		}

		private static Permission getPermission() {
			return permission;
		}

		/**
		 * Check if a player (online or offline) has a specific permission.
		 *
		 * @param uuid       UUID of the player
		 * @param permission permission string to check
		 * @return true if the player has the permission, false otherwise
		 */
		public static boolean hasPermission(@NotNull UUID uuid, @NotNull String permission) {
			final Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				// Online player: prefer Vault if available, else Bukkit
				if (permission != null) {
					return getPermission().has(player, permission);
				}
				return player.isOp() || player.hasPermission(permission);
			}

			final OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

			// Offline player: Vault supports checking by name/UUID
			if (getPermission() != null && offline.getName() != null) {
				return getPermission().playerHas(null, offline, permission);
			}

			// Fallback: only check if they're an operator
			return offline.isOp();
		}
	}
}