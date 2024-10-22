package com.swiftlicious.hellblock.gui.icon.property.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.SectionPage;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.ArrayList;

public class Head64Item extends AbstractItem {

	private final SectionPage itemPage;

	public Head64Item(SectionPage itemPage) {
		this.itemPage = itemPage;
	}

	@Override
	public ItemProvider getItemProvider() {
		ItemBuilder itemBuilder = new ItemBuilder(Material.PLAYER_HEAD)
				.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage(HBLocale.GUI_ITEM_HEAD64)));
		if (itemPage.getSection().contains("head64")) {
			itemBuilder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
					.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_CURRENT_VALUE)));
			String head64 = itemPage.getSection().getString("head64", "");
			ArrayList<String> list = new ArrayList<>();
			for (int i = 0; i < head64.length(); i += 16) {
				if (i + 16 > head64.length()) {
					list.add(head64.substring(i));
				} else {
					list.add(head64.substring(i, i + 16));
				}
			}
			for (String line : list) {
				itemBuilder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage("<white>" + line)));
			}
			itemBuilder.addLoreLines("")
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_LEFT_CLICK_EDIT)))
					.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_RIGHT_CLICK_RESET)));
		} else {
			itemBuilder.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
					.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_LEFT_CLICK_EDIT)));
		}
		return itemBuilder;
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		if (clickType.isLeftClick()) {
			player.closeInventory();
			HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
					"Input the head64 value in chat");
			((HellblockPlugin) HellblockPlugin.getInstance()).getChatCatcherManager().catchMessage(player,
					"head64", itemPage);
		} else if (clickType.isRightClick()) {
			itemPage.getSection().set("head64", null);
			itemPage.save();
			itemPage.reOpen();
		}
	}
}
