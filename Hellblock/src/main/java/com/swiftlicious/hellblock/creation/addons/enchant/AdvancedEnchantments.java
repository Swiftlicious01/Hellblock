package com.swiftlicious.hellblock.creation.addons.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.creation.addons.EnchantmentInterface;

import net.advancedplugins.ae.api.AEAPI;

public class AdvancedEnchantments implements EnchantmentInterface {

	@Override
	public List<String> getEnchants(ItemStack itemStack) {
		List<String> enchants = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : AEAPI.getEnchantmentsOnItem(itemStack).entrySet()) {
			enchants.add("AE:" + entry.getKey() + ":" + entry.getValue());
		}
		return enchants;
	}
}
