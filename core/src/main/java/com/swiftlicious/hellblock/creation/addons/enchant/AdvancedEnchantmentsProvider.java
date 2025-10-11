package com.swiftlicious.hellblock.creation.addons.enchant;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.utils.extras.Pair;

import net.advancedplugins.ae.api.AEAPI;

public class AdvancedEnchantmentsProvider implements EnchantmentProvider {

	@Override
	public String identifier() {
		return "AdvancedEnchantments";
	}

	@Override
	public List<Pair<String, Short>> getEnchants(@NotNull ItemStack itemStack) {
		final List<Pair<String, Short>> enchants = new ArrayList<>();
		AEAPI.getEnchantmentsOnItem(itemStack).entrySet()
				.forEach(entry -> enchants.add(Pair.of("AE:" + entry.getKey(), entry.getValue().shortValue())));
		return enchants;
	}
}