package com.swiftlicious.hellblock.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public sealed interface HellblockRecipe permits HellblockRecipe.Enabled, HellblockRecipe.Disabled {

	record Enabled(NamespacedKey key, ShapedRecipe recipe) implements HellblockRecipe {
	}

	record Disabled(NamespacedKey key) implements HellblockRecipe {
	}
}