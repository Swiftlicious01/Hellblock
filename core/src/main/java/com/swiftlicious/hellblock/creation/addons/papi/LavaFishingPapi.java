package com.swiftlicious.hellblock.creation.addons.papi;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.RandomUtils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class LavaFishingPapi extends PlaceholderExpansion {

	protected final HellblockPlugin plugin;

	public LavaFishingPapi(HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	public void load() {
		super.register();
	}

	public void unload() {
		super.unregister();
	}

	@Override
	public @NotNull String getIdentifier() {
		return "lavafishing";
	}

	@Override
	public @NotNull String getAuthor() {
		return "Swiftlicious";
	}

	@Override
	public @NotNull String getVersion() {
		return "1.0-ALPHA";
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
		String[] split = params.split("_");
		Player player = offlinePlayer.getPlayer();
		if (player == null)
			return "";
		switch (split[0]) {
		case "random" -> {
			return String.valueOf(RandomUtils.generateRandomDouble(0, 1));
		}
		case "market" -> {
			if (split.length < 2)
				return null;
			switch (split[1]) {
			case "limit" -> {
				if (split.length < 3) {
					return String.format("%.2f", plugin.getMarketManager().earningLimit(Context.player(player)));
				} else {
					Player another = Bukkit.getPlayer(split[2]);
					if (another == null) {
						return "";
					}
					return String.format("%.2f", plugin.getMarketManager().earningLimit(Context.player(another)));
				}
			}
			case "earnings" -> {
				UserData user;
				if (split.length < 3) {
					user = plugin.getStorageManager().getOnlineUser(player.getUniqueId()).orElse(null);
				} else {
					Player another = Bukkit.getPlayer(split[2]);
					if (another == null) {
						return "";
					}
					user = plugin.getStorageManager().getOnlineUser(another.getUniqueId()).orElse(null);
				}
				if (user == null)
					return "";
				return String.format("%.2f", user.getEarningData().getEarnings());
			}
			case "canearn" -> {
				if (split.length < 3) {
					UserData user = plugin.getStorageManager().getOnlineUser(player.getUniqueId()).orElse(null);
					if (user == null)
						return "";
					return String.format("%.2f", plugin.getMarketManager().earningLimit(Context.player(player))
							- user.getEarningData().getEarnings());
				} else {
					Player another = Bukkit.getPlayer(split[2]);
					if (another == null) {
						return "";
					}
					UserData user = plugin.getStorageManager().getOnlineUser(another.getUniqueId()).orElse(null);
					if (user == null)
						return "";
					return String.format("%.2f", plugin.getMarketManager().earningLimit(Context.player(another))
							- user.getEarningData().getEarnings());
				}
			}
			}
		}
		}
		return null;
	}
}
