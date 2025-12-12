package com.swiftlicious.hellblock.creation.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.context.Context;

import io.lumine.mythic.bukkit.MythicBukkit;

public class MythicMobsItemProvider implements ItemProvider {

	private MythicBukkit mythicBukkit;

	@NotNull
	@Override
	public ItemStack buildItem(@NotNull Context<Player> player, @NotNull String id) {
		if (mythicBukkit == null || mythicBukkit.isClosed()) {
			this.mythicBukkit = MythicBukkit.inst();
		}
		return mythicBukkit.getItemManager().getItemStack(id);
	}

	@Override
	public String itemID(@NotNull ItemStack itemStack) {
		if (mythicBukkit == null || mythicBukkit.isClosed()) {
			this.mythicBukkit = MythicBukkit.inst();
		}
		return mythicBukkit.getItemManager().getMythicTypeFromItem(itemStack);
	}

	@Override
	public String identifier() {
		return "MythicMobs";
	}
}