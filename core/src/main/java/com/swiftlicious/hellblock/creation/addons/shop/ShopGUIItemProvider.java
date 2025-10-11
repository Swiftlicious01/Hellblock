package com.swiftlicious.hellblock.creation.addons.shop;

import java.util.Objects;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent;
import net.brcdev.shopgui.provider.item.ItemProvider;

public class ShopGUIItemProvider extends ItemProvider implements Listener {

	private final HellblockPlugin plugin;

	public ShopGUIItemProvider(HellblockPlugin plugin) {
		super("Hellblock");
		this.plugin = plugin;
	}

	@Override
	public boolean isValidItem(ItemStack itemStack) {
		return plugin.getItemManager().getCustomItemID(itemStack) != null;
	}

	@Override
	public ItemStack loadItem(ConfigurationSection configurationSection) {
		final String id = configurationSection.getString("hellblock");
		if (id == null) {
			return null;
		}
		return plugin.getItemManager().buildInternal(Context.player(null).arg(ContextKeys.ID, id), id);
	}

	@Override
	public boolean compare(ItemStack i1, ItemStack i2) {
		return Objects.equals(plugin.getItemManager().getCustomItemID(i1), plugin.getItemManager().getCustomItemID(i2));
	}

	@EventHandler
	public void onShopGUIPlusPostEnable(ShopGUIPlusPostEnableEvent event) {
		ShopGuiPlusApi.registerItemProvider(this);
	}
}