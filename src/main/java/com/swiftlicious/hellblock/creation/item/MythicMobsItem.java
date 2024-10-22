package com.swiftlicious.hellblock.creation.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.bukkit.MythicBukkit;

public class MythicMobsItem implements ItemLibrary {

	private MythicBukkit mythicBukkit;

	public MythicMobsItem() {
		this.mythicBukkit = MythicBukkit.inst();
	}

	@Override
	public String identification() {
		return "MythicMobs";
	}

	@Override
	public ItemStack buildItem(Player player, String id) {
		if (mythicBukkit == null || mythicBukkit.isClosed()) {
			this.mythicBukkit = MythicBukkit.inst();
		}
		return mythicBukkit.getItemManager().getItemStack(id);
	}

	@Override
	public String getItemID(ItemStack itemStack) {
		if (mythicBukkit == null || mythicBukkit.isClosed()) {
			this.mythicBukkit = MythicBukkit.inst();
		}
		return mythicBukkit.getItemManager().getMythicTypeFromItem(itemStack);
	}
}