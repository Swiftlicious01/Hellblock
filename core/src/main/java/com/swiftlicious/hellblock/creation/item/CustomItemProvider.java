package com.swiftlicious.hellblock.creation.item;

import static java.util.Objects.requireNonNull;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;

public class CustomItemProvider implements ItemProvider {

	@NotNull
	@Override
	public ItemStack buildItem(@NotNull Context<Player> player, @NotNull String id) {
		final String[] split = id.split(":", 2);
		// CUSTOMITEM:ID or CUSTOMITEM:ID:TYPE
		final String finalID = split.length == 1 ? split[0] : split[1];
		final ItemStack itemStack = HellblockPlugin.getInstance().getItemManager()
				.buildInternal(player.arg(ContextKeys.ID, finalID), finalID);
		return requireNonNull(itemStack);
	}

	@Override
	public String itemID(@NotNull ItemStack itemStack) {
		return HellblockPlugin.getInstance().getItemManager().getCustomItemID(itemStack);
	}

	@Override
	public String identifier() {
		return "CustomItem";
	}
}