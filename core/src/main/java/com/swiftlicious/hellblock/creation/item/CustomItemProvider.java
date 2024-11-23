package com.swiftlicious.hellblock.creation.item;

import static java.util.Objects.requireNonNull;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;

public class CustomItemProvider implements ItemProvider {

	@Override
	public String identifier() {
		return "CustomItem";
	}

	@NotNull
	@Override
	public ItemStack buildItem(@NotNull Player player, @NotNull String id) {
		String[] split = id.split(":", 2);
		String finalID;
		if (split.length == 1) {
			// CustomItem:ID
			finalID = split[0];
		} else {
			// CustomItem:TYPE:ID
			finalID = split[1];
		}
		ItemStack itemStack = HellblockPlugin.getInstance().getItemManager()
				.buildInternal(Context.player(player).arg(ContextKeys.ID, finalID), finalID);
		return requireNonNull(itemStack);
	}

	@Override
	public String itemID(@NotNull ItemStack itemStack) {
		return HellblockPlugin.getInstance().getItemManager().getCustomItemID(itemStack);
	}
}