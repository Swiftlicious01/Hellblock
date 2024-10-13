package com.swiftlicious.hellblock.creation.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;

public class LavaFishingItem implements ItemLibrary {

	@Override
	public String identification() {
		return "LavaFishing";
	}

	@Override
	public ItemStack buildItem(Player player, String id) {
		String[] split = id.split(":", 2);
		return HellblockPlugin.getInstance().getItemManager().build(player, split[0], split[1]);
	}

	@Override
	public String getItemID(ItemStack itemStack) {
		return HellblockPlugin.getInstance().getItemManager().getLavaFishingItemID(itemStack);
	}
}