package com.swiftlicious.hellblock.creation.addons.shop.sign;

import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.creation.addons.ExternalProvider;

/**
 * Represents an external integration point for identifying shop signs within
 * the game world. Implementations of this interface allow detection of custom
 * shop signs provided by external plugins (e.g. QuickShop, TradeShop, etc.).
 * <p>
 * This is part of the external provider system used for interoperability with
 * third-party shop systems.
 */
public interface ShopSignProvider extends ExternalProvider {

	/**
	 * Determines whether the given sign represents a shop sign.
	 *
	 * @param sign the Sign to check, expected to be a sign block
	 * @return true if the block is a shop sign managed by the provider, false
	 *         otherwise
	 */
	boolean isShopSign(@NotNull Sign sign);
}