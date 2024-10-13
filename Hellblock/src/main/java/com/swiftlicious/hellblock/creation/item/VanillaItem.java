package com.swiftlicious.hellblock.creation.item;

import java.util.Locale;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VanillaItem implements ItemLibrary {

	@Override
	public String identification() {
		return "vanilla";
	}

	@Override
	public ItemStack buildItem(Player player, String id) {
		try {
			return new ItemStack(Material.valueOf(id.toUpperCase(Locale.ENGLISH)));
		} catch (IllegalArgumentException e) {
			return new ItemStack(Objects.requireNonNull(
					Registry.MATERIAL.get(new NamespacedKey("minecraft", id.toLowerCase(Locale.ENGLISH)))));
		}
	}

	@Override
	public String getItemID(ItemStack itemStack) {
		return itemStack.getType().name();
	}
}