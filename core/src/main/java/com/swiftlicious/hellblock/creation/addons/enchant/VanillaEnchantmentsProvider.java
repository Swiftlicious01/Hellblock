package com.swiftlicious.hellblock.creation.addons.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.utils.extras.Pair;

public class VanillaEnchantmentsProvider implements EnchantmentProvider {

	@Override
	public List<Pair<String, Short>> getEnchants(@NotNull ItemStack itemStack) {
		final Map<Enchantment, Integer> enchantments = itemStack.getEnchantments();
		final List<Pair<String, Short>> enchants = new ArrayList<>(enchantments.size());
		enchantments.entrySet()
				.forEach(en -> enchants.add(Pair.of(en.getKey().getKey().toString(), en.getValue().shortValue())));
		return enchants;
	}

	@Override
	public String identifier() {
		return "vanilla";
	}
}