package com.swiftlicious.hellblock.gui.icon.property.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.SectionPage;
import com.swiftlicious.hellblock.gui.page.property.DisplayNameEditor;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class DisplayNameItem extends AbstractItem {

	private final SectionPage itemPage;

	public DisplayNameItem(SectionPage itemPage) {
		this.itemPage = itemPage;
	}

	@Override
	public ItemProvider getItemProvider() {
		ItemBuilder itemBuilder = new ItemBuilder(Material.NAME_TAG)
				.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
						.getComponentFromMiniMessage(HBLocale.GUI_ITEM_DISPLAY_NAME)));
		if (itemPage.getSection().contains("display.name")) {
			itemBuilder
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									HBLocale.GUI_CURRENT_VALUE + itemPage.getSection().getString("display.name"))))
					.addLoreLines("");
			itemBuilder
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
			new DisplayNameEditor(player, itemPage);
		} else if (clickType.isRightClick()) {
			itemPage.getSection().set("display.name", null);
			itemPage.save();
			itemPage.reOpen();
		}
	}
}
