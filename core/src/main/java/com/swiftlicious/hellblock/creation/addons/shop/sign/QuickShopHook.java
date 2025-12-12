package com.swiftlicious.hellblock.creation.addons.shop.sign;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.api.QuickShopAPI;

import com.swiftlicious.hellblock.HellblockPlugin;

public class QuickShopHook implements ShopSignProvider {

	private QuickShopAPI quickShopApi;

	@Override
	public boolean isShopSign(@NotNull Sign sign) {
		if (quickShopApi == null && HellblockPlugin.getInstance().isHookedPluginEnabled("QuickShop")) {
			Plugin plugin = Bukkit.getPluginManager().getPlugin("QuickShop");
			quickShopApi = (QuickShopAPI) plugin;
		}
		
		return Optional.ofNullable(quickShopApi.getShopManager().getShop(sign.getLocation()))
				.map(shop -> shop.isShopSign(sign)).orElse(false);
	}

	@Override
	public String identifier() {
		return "QuickShop";
	}
}