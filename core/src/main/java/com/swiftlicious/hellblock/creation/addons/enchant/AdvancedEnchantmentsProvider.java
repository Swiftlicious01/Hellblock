package com.swiftlicious.hellblock.creation.addons.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		List<Pair<String, Short>> enchants = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : AEAPI.getEnchantmentsOnItem(itemStack).entrySet()) {
			enchants.add(Pair.of("AE:" + entry.getKey(), entry.getValue().shortValue()));
		}
		return enchants;
	}
}