package com.swiftlicious.hellblock.creation.addons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class VanillaEnchantment implements EnchantmentInterface {

	@Override
	public List<String> getEnchants(ItemStack itemStack) {
		Map<Enchantment, Integer> enchantments = itemStack.getEnchantments();
		List<String> enchants = new ArrayList<>(enchantments.size());
		for (Map.Entry<Enchantment, Integer> en : enchantments.entrySet()) {
			String key = en.getKey().getKey() + ":" + en.getValue();
			enchants.add(key);
		}
		return enchants;
	}
}
