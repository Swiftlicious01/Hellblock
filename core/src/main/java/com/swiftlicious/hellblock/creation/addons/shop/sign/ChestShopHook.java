package com.swiftlicious.hellblock.creation.addons.shop.sign;

import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import com.Acrobot.ChestShop.Signs.ChestShopSign;

public class ChestShopHook implements ShopSignProvider {

	@Override
	public boolean isShopSign(@NotNull Sign sign) {
		return ChestShopSign.isValid(sign) && ChestShopSign.isShopBlock(sign.getBlock());
	}

	@Override
	public String identifier() {
		return "ChestShop";
	}
}