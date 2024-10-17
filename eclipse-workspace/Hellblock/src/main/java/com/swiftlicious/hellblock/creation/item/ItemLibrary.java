package com.swiftlicious.hellblock.creation.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ItemLibrary {

	String identification();

	ItemStack buildItem(Player player, String id);

	String getItemID(ItemStack itemStack);
}
