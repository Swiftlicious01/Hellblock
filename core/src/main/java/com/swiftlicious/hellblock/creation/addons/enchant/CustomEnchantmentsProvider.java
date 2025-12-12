package com.swiftlicious.hellblock.creation.addons.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.utils.extras.Pair;

public class CustomEnchantmentsProvider implements EnchantmentProvider {

	private final HellblockPlugin plugin;

	// Map<namespacedKey, metadata>
	private static final Map<String, CustomEnchantMetadata> CUSTOM_ENCHANTS = Map.of("custom:magma_walker",
			new CustomEnchantMetadata("custom", "magma_walker_boots", "level"), "custom:crimson_thorns",
			new CustomEnchantMetadata("custom", "crimson_thorns_chestplate", "level"), "custom:lava_vision",
			new CustomEnchantMetadata("custom", "lava_vision_helmet", "level"), "custom:molten_core",
			new CustomEnchantMetadata("custom", "molten_core_leggings", "level"));

	public CustomEnchantmentsProvider(@NotNull HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public List<Pair<String, Short>> getEnchants(@NotNull ItemStack itemStack) {
		Item<ItemStack> wrapped = plugin.getItemManager().wrap(itemStack);
		List<Pair<String, Short>> result = new ArrayList<>();

		CUSTOM_ENCHANTS.entrySet().forEach(entry -> {
			String key = entry.getKey();
			CustomEnchantMetadata meta = entry.getValue();

			Optional<Object> levelOpt = wrapped.getTag((Object[]) meta.path());
			levelOpt.filter(v -> v instanceof Integer).map(v -> (Integer) v).filter(v -> v > 0)
					.ifPresent(v -> result.add(Pair.of(key, (short) (int) v)));
		});

		return result;
	}

	@Override
	public String identifier() {
		return "custom";
	}

	private record CustomEnchantMetadata(String... path) {
	}
}