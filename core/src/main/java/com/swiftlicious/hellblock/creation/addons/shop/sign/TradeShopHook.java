package com.swiftlicious.hellblock.creation.addons.shop.sign;

import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;
import org.shanerx.tradeshop.shop.ShopType;

public class TradeShopHook implements ShopSignProvider {

	@Override
	public boolean isShopSign(@NotNull Sign sign) {
		return ShopType.isShop(sign);
	}

	@Override
	public String identifier() {
		return "TradeShop";
	}
}