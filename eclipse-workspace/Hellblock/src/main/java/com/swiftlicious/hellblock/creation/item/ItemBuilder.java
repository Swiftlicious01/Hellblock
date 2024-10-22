package com.swiftlicious.hellblock.creation.item;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Tuple;
import com.swiftlicious.hellblock.utils.extras.Value;

public interface ItemBuilder extends BuildableItem {

	ItemBuilder customModelData(int value);

	ItemBuilder name(String name);

	ItemBuilder amount(int amount);

	ItemBuilder amount(int min_amount, int max_amount);

	ItemBuilder tag(boolean tag, String type, String id);

	ItemBuilder unbreakable(boolean unbreakable);

	ItemBuilder lore(List<String> lore);

	ItemBuilder nbt(Map<String, Object> nbt);

	ItemBuilder nbt(ConfigurationSection section);

	ItemBuilder enchantment(List<Pair<String, Short>> enchantments, boolean store);

	ItemBuilder randomEnchantments(List<Tuple<Double, String, Short>> enchantments, boolean store);

	ItemBuilder enchantmentPool(List<Pair<Integer, Value>> amountPairs,
			List<Pair<Pair<String, Short>, Value>> enchantments, boolean store);

	ItemBuilder maxDurability(int max);

	ItemBuilder price(float base, float bonus);

	ItemBuilder size(Pair<Float, Float> size);

	ItemBuilder stackable(boolean stackable);

	ItemBuilder preventGrabbing(boolean prevent);

	ItemBuilder head(String base64);

	ItemBuilder randomDamage(boolean damage);

	@NotNull
	String getId();

	@NotNull
	String getLibrary();

	int getAmount();

	Collection<ItemPropertyEditor> getEditors();

	ItemBuilder removeEditor(String type);

	ItemBuilder registerCustomEditor(String type, ItemPropertyEditor editor);

	interface ItemPropertyEditor {

		void edit(Player player, RtagItem rtagItem, Map<String, String> placeholders);

		default void edit(Player player, RtagItem rtagItem) {
			edit(player, rtagItem, null);
		}
	}
}